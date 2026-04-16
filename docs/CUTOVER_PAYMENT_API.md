# Cutover — Migração Bellory → Payment API

> Plano operacional para executar a migração em staging/produção. Cada etapa tem pré-condição, ação e verificação. Se alguma verificação falhar, **pare** e siga o rollback da etapa correspondente antes de continuar.

---

## Pré-flight — tudo pronto ANTES de começar

### Infra
- [ ] Redis instalado e acessível no host/porta configurados (`spring.data.redis.host/port`). Comando de sanidade: `redis-cli -h $HOST -p $PORT PING` → `PONG`.
- [ ] Payment API rodando em `payment.api.url` e respondendo em `GET <url>/actuator/health` → `{"status":"UP"}`.
- [ ] `payment.api.key` (`pk_...`) válida: `curl -H "X-API-Key: $PK" -H "X-Company-Id: 4" <url>/api/v1/plans/codigo/gratuito` → 200 com o JSON do plano.

### Planos padronizados
- [ ] Keys de limits consistentes nos 4 planos da Payment API (basico/plus/premium/gratuito):
  - `cliente`, `funcionario`, `agendamento`, `servicos`, `unidade`, `arquivos`, `relatorios`, `api`, `site_externo`, `agente_virtual`
- [ ] Validação: `GET /api/v1/plans/codigo/basico` → verificar cada key presente no array `limits`. Mesma checagem pros outros 3 planos.

### Código Bellory
- [ ] Branch `feat/payment-api-migration` com build verde no IntelliJ.
- [ ] Migrations V58 + V59 presentes em `src/main/resources/db/migration/`.
- [ ] `application-dev.properties` com `payment.api.*` e `spring.data.redis.*` preenchidos.

### Dados
- [ ] Backup do banco de produção antes da migração. Comando:
  ```bash
  pg_dump -h $HOST -U $USER -d $DB > bellory_pre_migration_$(date +%Y%m%d_%H%M).sql
  ```

---

## Etapa 1 — Staging: validar build + schema

**Pré-condição:** pre-flight ok.

**Ação:**
1. Deploy do Bellory branch `feat/payment-api-migration` em staging.
2. Deixar Flyway aplicar automaticamente V58 + V59.

**Verificação:**
- Logs Flyway: `Successfully applied 2 migrations to schema "admin"` (ou 1 se V58 já estava aplicada).
- `psql -c "\d admin.assinatura"`: tabela com apenas `id`, `organizacao_id`, `payment_api_customer_id`, `payment_api_subscription_id`, `dt_criacao`.
- `psql -c "\dt admin.cobranca_plataforma"`: → `Did not find any relation` (confirmação que foi dropada).
- Aplicação sobe sem erros no log.

**Rollback:**
- Restaurar backup do banco.
- Reverter deploy para a tag anterior.

---

## Etapa 2 — Staging: migrar assinaturas existentes (se houver)

> **Observação:** o `MigrateAssinaturasToPaymentApiRunner` foi removido na Fase 8 porque dependia de campos que foram dropados. Em staging, a base provavelmente é limpa ou só com dados de teste. Se houver organizações reais em staging com assinatura legada:
> - **Opção A (recomendada)**: limpe os dados de staging e recrie manualmente via signup usando a nova rota.
> - **Opção B**: use o endpoint `POST /api/v1/import/asaas` da Payment API — ele importa customers/subscriptions diretamente do Asaas (requer a `asaasApiKey` configurada na Company 4 da Payment API).

**Verificação:**
- `SELECT COUNT(*) FROM admin.assinatura WHERE payment_api_customer_id IS NULL;` → deve ser 0.

---

## Etapa 3 — Staging: smoke tests manuais

Executar cada cenário pelo frontend e pelo `curl`. Cada caixa marcada = passou.

### 3.1 — Signup (fluxo completo)
- [ ] `POST /api/v1/organizacao` com payload válido + `plano.id = "basico"`.
- [ ] Resposta 201.
- [ ] No banco: `SELECT payment_api_customer_id, payment_api_subscription_id FROM admin.assinatura WHERE organizacao_id = <novo_id>` — ambos preenchidos.
- [ ] Na Payment API: `GET /api/v1/customers/<customerId>` retorna 200 com os dados da org.
- [ ] Na Payment API: `GET /api/v1/subscriptions?customerId=<customerId>` retorna 1 subscription ACTIVE com `planId` correto.

### 3.2 — Signup com falha na Payment API (compensação)
Simule derrubando a Payment API temporariamente antes de chamar `POST /organizacao`.
- [ ] Resposta do Bellory: 500 com mensagem `Falha criando assinatura na Payment API`.
- [ ] No banco Bellory: organização **não** foi persistida (transação revertida).

### 3.3 — Login
- [ ] Login com credenciais do admin criado no signup 3.1.
- [ ] Resposta 200 contendo `organizacao.assinatura` com `situacao=ATIVA`, `planoCodigo=basico`, `planoNome`, `cicloCobranca=MONTHLY`.

### 3.4 — Redis cache — hit/miss
- [ ] Primeira request após login: log do `AssinaturaCacheService` indica fetch da Payment API.
- [ ] Segunda request (mesmo customer, <5min): log indica hit fresh (sem chamada).
- [ ] `redis-cli KEYS "payment:status:*"` → chave presente.

### 3.5 — Refresh-cache
- [ ] `POST /api/v1/assinatura/refresh-cache` autenticado → 200 com `AssinaturaStatusDTO` atualizado.
- [ ] Após chamar, nova request de `GET /me` reflete mudanças vindas da Payment API.

### 3.6 — Interceptor — bloqueio por assinatura
- [ ] Na Payment API, simular bloqueio do customer (ex.: criar cobrança vencida se possível, ou ajustar `access-policy` para bloquear).
- [ ] `POST /refresh-cache` → retorna `bloqueado=true`.
- [ ] Próxima request em `/api/v1/cliente/...` → 403 com `mensagem` vinda da Payment API.
- [ ] `/api/v1/assinatura/refresh-cache` continua respondendo (path excluído).

### 3.7 — LimiteValidator — cliente/agendamento/funcionário/site
Plano `gratuito` tem limites: 100 clientes, 50 agendamentos/mês, 1 funcionário, 10 serviços.

- [ ] Criar organização com plano `gratuito`.
- [ ] Criar 1 funcionário (via `POST /api/v1/funcionario`) → 201.
- [ ] Criar 2º funcionário → **422** com `limitKey=funcionario`, `limiteMaximo=1`, `usoAtual=2`.
- [ ] Mudar plano pra `basico` na Payment API (aguardar ou chamar refresh-cache).
- [ ] Criar 2º funcionário → 201. Criar 4º → **422** (limite `basico` é 3).

### 3.8 — Fail-open do interceptor
- [ ] Derrubar a Payment API (ou cortar rede).
- [ ] Esperar `payment.cache.fresh-ttl-seconds` passar.
- [ ] Requests continuam 200 (stale servido). Log: `Payment API indisponivel, servindo STALE`.
- [ ] Após `stale-ttl-seconds` (24h default), requests retornariam `INDISPONIVEL` + `bloqueado=false` (fail-open).

### 3.9 — Fail-open do LimiteValidator
- [ ] Com Payment API fora e sem cache, tentar criar recurso.
- [ ] Deve permitir (log: `LimiteValidator fail-open: sem cache`).

---

## Etapa 4 — Produção: cutover

**Pré-condição:** todas as caixas da Etapa 3 marcadas em staging.

**Janela de manutenção sugerida:** 30min (pior caso).

1. **Anúncio** — avisar clientes da janela de manutenção (se política da empresa).
2. **Backup** — `pg_dump` antes de tocar em qualquer coisa.
3. **Deploy** do Bellory em produção.
4. **Monitorar** logs da aplicação nos primeiros 5min:
   - Procurar por `PaymentApiException`, `RedisConnectionFailureException` — se aparecerem, investigar antes de liberar tráfego.
5. **Smoke test em produção** — repetir 3.1 + 3.3 + 3.6 com uma conta de teste.
6. Se ok, anúncio de encerramento da janela.

---

## Rollback de produção (plano de contingência)

Se depois do deploy algo crítico quebrar:

1. **Deploy da versão anterior** (último release estável).
2. **Restaurar banco** do backup (`pg_restore` ou `psql < backup.sql`).
3. Abrir issue no repositório com:
   - Logs do incidente
   - Request/response que falhou
   - Horário exato
4. Investigar fora da janela, corrigir, repetir cutover.

**Atenção:** uma vez que V58 + V59 foram aplicadas, reverter exige restaurar o banco do backup. Não adianta só deployar a versão antiga — ela espera colunas que a V59 dropou.

---

## Pós-cutover — monitoramento primeiras 48h

- [ ] Grafana/Prometheus: latência p95 do endpoint `POST /api/v1/auth/login` comparado com baseline pré-cutover. Espera-se incremento de +50–200ms na 1ª request de cada cliente (cache miss), +0ms nas subsequentes.
- [ ] Taxa de erro 5xx: não deve subir.
- [ ] Logs `WARN` com "Payment API indisponivel": se aumentar, sinalizar instabilidade do lado Payment API.
- [ ] Contagem de `LimitePlanoExcedidoException`: se muitos clientes começarem a receber 422, é sinal que os limites dos planos podem estar muito apertados — ajustar na Payment API.
- [ ] Redis: uso de memória e hit rate via `redis-cli INFO stats`.

---

## Limpeza pós-cutover (dias+)

Depois de 30 dias estáveis em produção:
- [ ] Remover backup antigo
- [ ] Arquivar branch de feature
- [ ] Atualizar `README`/`CLAUDE.md` do projeto refletindo o novo fluxo
- [ ] Fechar tickets relacionados à migração
