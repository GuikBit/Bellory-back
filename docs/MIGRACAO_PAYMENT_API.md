# Plano de Migração — Assinatura para Payment API

> Criado em 2026-04-16. Migração da lógica de assinatura/cobrança atualmente acoplada ao Asaas dentro do Bellory-API para uma **Payment API** externa que centraliza pagamentos/planos. O Bellory vira consumidor.

---

## Premissas confirmadas

1. **Signup com plano gratuito transparente**: Payment API cria assinatura gratuita sem tocar no Asaas. Asaas só é acionado no upgrade para plano pago. Elimina o risco de orfãos no signup.
2. **Latência extra no login é aceitável**. Cache Redis mitiga; primeira request de cada cliente paga o custo.
3. **Limites centralizados** em `LimiteValidatorService`.
4. **Cache invalidado via endpoint explícito** (`POST /assinatura/refresh-cache`) chamado pelo frontend após ações na Payment API. Sem webhook.
5. **Superadmin do Bellory mantém bypass** (já existe no `AuthController` linhas 117-129).
6. **Migração de dados**: clientes existentes importados via `POST /customers/adopt` na Payment API.
7. **TTL cache fresh**: 5 min. Stale: 24h (fail-open).
8. **Plano escolhido no form de cadastro** (não é sempre gratuito por default — cliente seleciona no signup).

---

## FASE 0 — Preparação

- Adicionar `spring-boot-starter-data-redis`
- Configurar Redis em `application-dev.properties` e `application.properties`
- Confirmar contrato OpenAPI da Payment API:
  - `POST /customers` — signup (aceita plano escolhido, incluindo gratuito)
  - `POST /customers/adopt` — migração de clientes existentes (recebe `assasCustomerId` se houver)
  - `GET /customers/{id}/status` — retorna `{plano, limites, bloqueado, mensagem, situacao}`
  - `DELETE /customers/{id}` — compensação em falha de signup
  - `POST /customers/{id}/change-plan` — upgrade/downgrade (opcional, pode ficar no frontend)
- Branch: `feat/payment-api-migration`

## FASE 1 — PaymentApiClient + Cache

Criar:
- `client/payment/PaymentApiClient.java` — RestClient com `@Retryable`, timeout 2-3s
- `client/payment/dto/` — `CreateCustomerRequest`, `AdoptCustomerRequest`, `CustomerStatusResponse`
- `service/assinatura/AssinaturaCacheService.java` — fresh 5min + stale 24h
- `config/RedisConfig.java` — `RedisTemplate` com `ObjectMapper` + `JavaTimeModule`
- `exception/PaymentApiException.java`

### Estratégia fail-open do cache

```
get(customerId) →
  fresh hit? return
  call PaymentApi → ok? grava fresh+stale, retorna
  call falhou + tem stale? retorna stale + log alerta
  call falhou + sem stale? throw PaymentApiException
```

## FASE 2 — Entity `Assinatura` + Flyway (só ADD)

**Estratégia incremental**: nesta fase apenas adiciona as colunas novas. Drops de colunas obsoletas (status, ciclo, trial, cupom, pro-rata, FKs plano_bellory/cupom/plano_agendado) ficam para a Fase 8 junto com a deleção dos services Asaas. Isso garante que o projeto continua compilando em toda fase intermediária.

Adicionar ao entity:
- `paymentApiCustomerId` (Long)
- `paymentApiSubscriptionId` (Long)

Flyway `V58__assinatura_payment_api_ids.sql`:
- ADD `payment_api_customer_id BIGINT`
- ADD `payment_api_subscription_id BIGINT`
- Índices sobre ambos

Entity final (pós Fase 8): `id`, `organizacao` (1:1), `paymentApiCustomerId`, `paymentApiSubscriptionId`, `dtCriacao`. Drops em migration futura.

## FASE 3 — Refatorar Signup (`OrganizacaoService.create`)

Fluxo simplificado (Payment API não toca Asaas no plano gratuito):

1. Validações locais (CNPJ, email, etc.)
2. `paymentApiClient.createCustomer(...)` recebendo **plano escolhido no form**
3. Falhou? → 400 antes de criar qualquer coisa local
4. Sucesso? → cria `Organizacao` + `ConfigSistema` + `Cargo` + `User` + `Assinatura` (vínculo) em `@Transactional`
5. **Falha local rara após passo 2**: compensação async enfileira `paymentApiClient.deleteCustomer()` + log de alerta

## FASE 4 — Refatorar Login (`AuthController` login / `/me` / `/validate`)

- Substituir `assinaturaService.getStatusAssinatura()` por `assinaturaCacheService.get(paymentApiCustomerId)`
- `AssinaturaStatusDTO` alinhado ao payload da Payment API
- Manter bypass superadmin
- **Novo**: `POST /assinatura/refresh-cache` — invalida cache após ações no Payment API

## FASE 5 — Refatorar `AssinaturaInterceptor`

- Lê cache local (rápido)
- `bloqueado=true` → 403 com mensagem da Payment API
- Cache miss + Payment API offline + sem stale → fail-open + log crítico (decisão: permissivo)

## FASE 6 — `LimiteValidatorService`

`service/plano/LimiteValidatorService.java`:

```java
validarLimite(organizacaoId, TipoLimite.CLIENTE, countAtual)
validarLimite(organizacaoId, TipoLimite.AGENDAMENTO, countAtual)
validarLimite(organizacaoId, TipoLimite.COLABORADOR, countAtual)
podeUsarLandingPage(organizacaoId)
```

Lê internamente `cache.get(customerId).plano.limites.*`.

Substituir validações em:
- `ClienteService`
- `AgendamentoService`
- `FuncionarioService`
- `InstanceService`
- `LandingPageEditorService`

## FASE 7 — Migração de dados (adopt)

`CommandLineRunner` acionável por flag (ex.: `--migrate-assinaturas`):

1. Lê todas as `Assinatura` existentes
2. Para cada uma: `POST /customers/adopt` enviando dados da org + `assasCustomerId` (se existir) + plano atual + status
3. Grava `paymentApiCustomerId` retornado
4. Rodar em staging primeiro. Gerar relatório de falhas.

## FASE 8 — Limpeza (dividida em 8A / 8B / 8C)

Divisão proposta para evitar deixar o projeto em estado não-compilável entre passos:

### 8A (concluída 2026-04-16) — Desacoplar e deletar periféricos
- `OrganizacaoService.create` cria a `Assinatura` direto (sem `AssinaturaService.criarAssinaturaTrial`) com defaults `ATIVA` + ciclo derivado do DTO.
- Deletado: `AssinaturaSchedulerService`, `AssasWebhookController`.
- Properties `assas.*` e `bellory.trial.dias` já haviam sido removidas na Fase 0.

### 8B (concluída 2026-04-16) — Deletar services, controllers e DTOs legados
- Deletados: `AssinaturaService`, `AdminAssinaturaService`, `CupomDescontoService`, `AdminCupomDescontoService`, `AssinaturaController`, `AdminAssinaturaController`, `PublicPlanosController`, `AdminCupomDescontoController`, `AssasClient`, `AssasApiException`, pasta `dto/assinatura/assas/`.
- `DatabaseSeederService`: imports Asaas removidos, `criarAssinaturaNoAsaas`/`cancelarAssinaturaNoAsaas` viraram no-op.
- `CacheConfig`: removidas constantes `CACHE_ASSAS_*`.

### 8C (concluída 2026-04-16) — Drop de schema + enxugar entities
Executado:
1. Refatorar/limpar `DatabaseSeederService` removendo seeds de planos/cupom/cobrança
2. Enxugar entity `Assinatura` (drop dos campos `planoBellory`, `status`, `cicloCobranca`, trial, cupom, pro-rata, Asaas, plano agendado). Manter: `id`, `organizacao`, `paymentApiCustomerId`, `paymentApiSubscriptionId`, `dtCriacao`.
3. Remover FKs legadas em `Organizacao` (plano, limitesPersonalizados) + ajustar `OrganizacaoInfoDTO`/mappers.
4. Deletar entities: `PlanoBellory`, `PlanoLimites`, `PlanoLimitesBellory` (se não usada), `Plano`, `CobrancaPlataforma`, `PagamentoPlataforma`, `WebhookLog`, `CupomDesconto`, `CupomUtilizacao`. Repos correspondentes.
5. Deletar enums: `StatusAssinatura`, `SituacaoAssinatura`, `CicloCobranca`, `FormaPagamentoPlataforma`, `StatusCobrancaPlataforma`, `StatusPagamentoPlataforma`.
6. Nova Flyway migration (V59) dropa colunas legadas de `admin.assinatura` + tabelas `cobranca_plataforma`, `pagamento_plataforma`, `webhook_log`, `plano_bellory`, `plano_limites_bellory`, `cupom_desconto`, `cupom_utilizacao`, FK `plano_id` de `organizacao`.


### Deletar
- `service/assinatura/AssasClient.java`
- `service/assinatura/AssinaturaSchedulerService.java`
- `controller/app/AssinaturaController.java`
- `controller/app/AssasWebhookController.java`
- `controller/app/PublicPlanosController.java`
- `controller/admin/AdminAssinaturaController.java`
- `service/admin/AdminAssinaturaService.java` (se existir)
- `dto/assinatura/assas/` (pasta inteira)
- `entity/assinatura/CobrancaPlataforma.java` + repo
- `entity/assinatura/WebhookLog.java` + repo
- `entity/plano/PlanoBellory.java` + repo + service
- `entity/plano/PlanoLimites.java` + repo
- Enums: `StatusAssinatura`, `SituacaoAssinatura`, `CicloCobranca`, `FormaPagamentoPlataforma`, `StatusCobrancaPlataforma`
- DTOs: `EscolherPlanoDTO`, `ProRataPreviewDTO`, `CobrancaPlataformaDTO`, `FormaPagamentoResponseDTO`, `PlanoBelloryPublicDTO`, `AdminBillingDashboardDTO`
- Properties: `assas.api.url`, `assas.api.key`, `assas.webhook.token`
- Caches: `CACHE_ASSAS_SUBSCRIPTION`, `CACHE_ASSAS_PAYMENTS`

### Alterar
- `entity/assinatura/Assinatura.java` — enxugar
- `service/assinatura/AssinaturaService.java` — fachada do `PaymentApiClient` + cache
- `controller/auth/AuthController.java` — consumir cache no login/me/validate
- `service/organizacao/OrganizacaoService.java` — chamar Payment API no create
- `config/AssinaturaInterceptor.java` — ler do cache
- `config/WebMvcConfig.java` — revisar paths excluídos
- `dto/auth/AssinaturaStatusDTO.java` — alinhar com payload da Payment API
- Services que validam limite — usar `LimiteValidatorService`
- `application.properties` + `application-dev.properties` — adicionar `payment.api.url`, `payment.api.key`, configs Redis

## FASE 9 — Testes + Cutover

- E2E: signup com plano X → login → criar entidades até limite → upgrade → refresh-cache → novo limite aplicado (com mock da Payment API)
- Staging: deploy com Payment API real, rodar script de migração em subset
- Cutover: deploy Bellory novo + Payment API + script de migração completo → switch DNS/feature flag

---

## Checklist antes de começar a Fase 0

- [x] TTL cache fresh: 5 min
- [x] Plano padrão no signup: escolhido pelo cliente no form
- [x] Compensação no signup: `DELETE /customers/{id}` async
- [x] Migração: `POST /customers/adopt`
- [ ] Definir contrato OpenAPI final da Payment API
- [ ] Redis disponível em staging/prod
