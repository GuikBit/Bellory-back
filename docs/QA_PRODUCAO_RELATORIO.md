# Relatório QA — Prontidão para Produção (Bellory-API)

> **Status: NÃO PRONTO PARA PRODUÇÃO.**
>
> Auditoria realizada por 9 análises especializadas paralelas cobrindo: segurança, configuração, multi-tenancy, banco de dados, endpoints públicos, errors/logs, código pendente, performance e integrações externas.
>
> **Total identificado:** ~30 BLOCKERS · ~40 HIGH · ~40 MEDIUM · ~30 LOW.
> **Prazo estimado para resolver bloqueadores:** 5–10 dias úteis (sem incluir HIGH).

---

## ⛔ TOP 10 — Não suba para produção sem corrigir

| # | Problema | Onde | Impacto |
|---|---|---|---|
| 1 | **Secrets hardcoded no git** (JWT signing-key, Evolution API, Gmail SMTP, VAPID, Payment API key, DB pass) | `application*.properties` (várias linhas) | Anyone com acesso ao repo forja JWT, envia WhatsApp/email, decripta push. |
| 2 | **JWT property name mismatch** — `TokenService` lê `api.security.token.secret:my-secret-key`, properties define `security.jwt.signing-key` | `TokenService.java:23` | Token assinado com **literal `"my-secret-key"`**, forge trivial de qualquer usuário. |
| 3 | **`/api/v1/agendamento/**`, `/cliente/**`, `/funcionario/**`, `/servico/**`, `/produto/**`, `/dashboard/**` em `permitAll()`** | `SecurityConfig.java:88-145` | PII (CPF/email/telefone), agenda, dashboard financeiro de TODOS tenants leitura/escrita sem auth. |
| 4 | **`@EnableMethodSecurity` ausente** — `@PreAuthorize` em todo projeto é decoração inerte | `SecurityConfig.java` | Endpoints admin sem proteção real. |
| 5 | **`AgendamentoService` 6 métodos sem validação de tenant** (update, reagendar, concluir, listar por status/funcionário, filtrar) | `AgendamentoService.java:531, 1064, 1498, 1526, 1543, 1569, 1607` | Tenant A altera/lê agenda de tenant B. |
| 6 | **Webhook Payment sem HMAC** — só Bearer estático | `PaymentWebhookService.java:45-61` | Replay attacks; quem vaze o token confirma pagamentos falsos. |
| 7 | **`/api/v1/webhook/confirmacao*` e `/fila-espera/*` sem validação de origem** | Controllers respectivos | Qualquer JWT confirma agendamentos de tenant alheio. |
| 8 | **`/api/v1/email/teste` aberto sem auth/rate-limit** | `EmailController.java:27` | SMTP corporativa como open-relay. |
| 9 | **`/api/v1/instances/by-name/{name}` aberto vaza configuração WhatsApp** (knowledgeBase, webhook URL, tools) | `InstanceController.java:508` | Reverse-engineer do agente IA + exfiltração de prompts. |
| 10 | **`/api/v1/organizacao` POST sem CAPTCHA/rate-limit** | `OrganizacaoController.java:47` | Atacante cria milhares de tenants, exaure recursos. |

---

## 1. Segredos comprometidos — ROTACIONAR JÁ

Listas em `application.properties` e `application-dev.properties` que **estão commitados no git**:

| Chave | Linha | Ação |
|---|---|---|
| `security.jwt.signing-key=...0830939a` (default) | `application.properties:59` | Rotacionar + remover default + alinhar nome com `TokenService` |
| `evolution.api.key=0626f19f09bd...` | `application.properties:112` | Rotacionar na Evolution + usar `${EVOLUTION_API_KEY}` |
| `EMAIL_PASSWORD:dvlublkyeehifhth` | `application.properties:121` | Revogar app-password Gmail + gerar nova |
| `vapid.private-key=...` | `application.properties:163-164` | Rotacionar par VAPID + remover default |
| `payment.api.key=pk_F9vtY0M_NVmjV1zLnmD3...` | `application-dev.properties:66-67` | Rotacionar com Payment API |
| `DB_DEV_PASSWORD=mB6EI3IH4OtYnY2` | `application-dev.properties:35` | Trocar senha + remover default |

**Após rotacionar, scrubar histórico do git** com `git filter-repo` (ou aceitar que o repo fica para sempre comprometido).

---

## 2. Autenticação / Autorização quebrada

### 2.1. Whitelists abertas demais
`SecurityConfig.java:88-145` libera para acesso público:
- `/api/v1/agendamento/**` → CRUD de agenda
- `/api/v1/cliente/**` → CPF/email/telefone
- `/api/v1/funcionario/**` → cadastro de funcionários
- `/api/v1/servico/**` + `/produto/**` → catálogo
- `/api/v1/dashboard/**` → métricas financeiras
- `/api/v1/test/**` + `/api/v1/email/teste` → endpoints debug
- `/api/v1/instances/by-name/**` → instâncias WhatsApp

**Fix:** mover para `authenticated()` ou para `/api/v1/public/**` (já restrito por `PublicSiteGuard`).

### 2.2. JWT `TokenService` quebrado
- Property name não bate: lê `api.security.token.secret`, properties define `security.jwt.signing-key`. Em prod, assina com fallback `"my-secret-key"`.
- Sem refresh-token / blacklist (já reconhecido em `AuthController.logout`).
- BCrypt strength=10 (default) — aceitável; considerar 12.

### 2.3. `@EnableMethodSecurity` ausente
`@PreAuthorize` em vários controllers (ex.: `OrganizacaoController`, todos os admin) é **inerte**. Fix:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ← adicionar
public class SecurityConfig { ... }
```
**Mas atenção:** habilitar pode quebrar comportamento existente em controllers que dependem disso. Auditar caso a caso.

### 2.4. Forgot password vulnerável
- `ForgotPasswordService.java:43,90` → enumera usuários (lança `"Usuário não encontrado"`).
- `ForgotPasswordRateLimiterService.java:12,21` → 3 req/5min por IP, key em `X-Forwarded-For` (spoofável).
- Código 6 dígitos, sem lockout por usuário → brute-force viável.

---

## 3. Vazamento cross-tenant (BLOCKER)

`AgendamentoService` tem **6 métodos sem `validarOrganizacao()`**:

| Método | Linha | Risco |
|---|---|---|
| `updateAgendamento` | 531 | Tenant A altera agenda de B |
| `reagendarAgendamento` | 1569 | Move agendamento de outro tenant |
| `concluirAgendamento` | 1064 | Marca como concluído |
| `getAgendamentosByStatus` | 1498 | Lista global cross-tenant |
| `getAgendamentosProximos` | 1511 | Idem |
| `getEstatisticasAgendamentos` | 1526 | Métricas globais |
| `getAgendamentosByFuncionarioAndData` | 1543 | Idem |
| `filtrarAgendamentos` | 1607 | findAll() em memória, sem filtro org |
| `consultarFuncionariosPorServicos` | 1738 | `funcionarioRepository.findAll()` global |

`ProdutoService.findAll()` e `findById()` também ignoram tenant.

**Admin endpoints:** sem `@PreAuthorize` (inerte), confiam só em `SecurityConfig` que aceita `ADMIN` (role tenant comum). Tenant admin pode acessar `/admin/organizacoes/{any-id}`.

---

## 4. Endpoints públicos perigosos

Além dos já listados em §2.1:
- `OrganizacaoController.verificar-cnpj/email/username/slug` → user enumeration (B2B PII).
- `/uploads/**` (`WebMvcConfig.java:31`) sirva arquivos sem ACL → IDOR por adivinhação de UUID.
- `ArquivoStorageService` aceita SVG → XSS armazenado quando renderizado em browser.
- `navegarSubpastaSistema` → path traversal possível em `caminhoCompleto = "organizacao/../../"`.
- CORS dual: `WebMvcConfig.addCorsMappings` aceita `*` e sobrescreve `SecurityConfig`. Combina com `@CrossOrigin("https://*.vercel.app")` em `AuthController`/`ForgotPasswordController` → CSRF possível.
- `multipart.max-request-size=100MB` → DoS via upload.

---

## 5. Performance — bloqueadores que aparecem com volume real

### 5.1. `Thread.sleep` em scheduler
`NotificacaoSchedulerService.java:121-191` → `DELAY_FIXO_MS = 180000` (3 min) dentro de loop. 100 notificações = 5h de execução. `@Scheduled fixedRate=5min` causa sobreposição. **Mover para fila assíncrona com workers.**

### 5.2. `findAll()` + filtro em memória
`AgendamentoService.filtrarAgendamentos` (1608) carrega TODA a tabela e filtra em Java Streams, sem `organizacaoId`. **A 10k agendamentos a query trava; 100k = OOM.**

### 5.3. N+1 em `AgendamentoDTO`
Toca `cliente`, `cobrancas`, `servicos`, `funcionarios`, `questionarios` (todos LAZY) → 1 + N×5 queries. 200 agendamentos = 1000 queries por request.

### 5.4. EAGER em coleções
- `Servico.categoria` EAGER (`Servico.java:51`)
- `Page.componentes` EAGER OneToMany (`Page.java:66`) — catastrófico
- `Produto.imagens` EAGER (`Produto.java:40`)
- `AgendamentoQuestionario.questionario` EAGER (`AgendamentoQuestionario.java:35`)

### 5.5. Listagens sem paginação
- `AgendamentoRepository.findByOrganizacaoId(Long)` retorna `List`
- `ClienteRepository.findAllByOrganizacao_Id`
- `findTopClientesByOrganizacao` (sem LIMIT no SQL)
- `OrganizacaoController.findAll()`

### 5.6. Hikari pool subdimensionado
Prod = 20 conexões. Schedulers + webhooks + tracking saturam. **Subir para 30–50 + monitorar.**

---

## 6. Configuração de produção

### 6.1. Profile default frágil
`application.properties:107` → `spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}`. Se var não setada em deploy, sobe **com perfil DEV em produção**.

### 6.2. Logs DEV expõem TUDO
`application-dev.properties:51-53` → `hibernate.SQL=DEBUG` + `BasicBinder=TRACE` (loga **todos** os parâmetros bindados — senhas hash, CPF, tokens). Se prod virar dev acidental → vazamento massivo LGPD.

### 6.3. Actuator + Swagger expostos
`SecurityConfig.java:123-129` libera `/actuator/**` e `/swagger-ui/**`/`/v3/api-docs/**`. Por default só `/health` e `/info` são expostos pelo actuator, mas reconfigurar em prop expõe `env`/`heapdump`. Swagger expõe toda a API publicamente.

### 6.4. Dockerfile inadequado
- Roda como **root**.
- Sem `HEALTHCHECK`.
- Sem multi-stage build.
- `EXPOSE 8081` (compose prod usa 8080).
- Imagem base sem digest fixado.
- `--enable-preview` Java 21 em prod.

### 6.5. CI/CD prod faz `docker system prune -f` antes do deploy
Apaga cache de build → deploy lento + janela de downtime.

### 6.6. Sem rotação de logs
Sem `logging.logback.rollingpolicy.*` nem `json-file` driver com `max-size`/`max-file` no `docker-compose.yml`.

### 6.7. CORS aceita `localhost:*` em prod
`SecurityConfig.java:48,52` + `setAllowCredentials(true)` → captura credencial via localhost local.

---

## 7. Banco de dados

### 7.1. Cliente sem soft-delete
`Cliente.java` não tem `is_deletado`. Hard-delete via `cascade=ALL` derruba histórico de cobranças/agendamentos. **Conflita com retenção LGPD/fiscal.**

### 7.2. `Cliente.cascade=ALL` em coleções históricas
`Cliente.java:50,53,56,59` → deletar cliente apaga `agendamentos`, `compras`, `cobrancas`, `pagamentos`. **Remover cascade.**

### 7.3. Tabelas sem retenção
- `notificacao_enviada` (1+ linha por agendamento × tipo)
- `webhook_event_log` (V60) com `payload TEXT` grande
- `tracking_*` (page views, sessions, visitors)

→ todas crescem infinitamente. Adicionar archive/partição.

### 7.4. CHECKs dropados sem replacement
- V56 droppou `chk_notif_status`
- V65 droppou `chk_notif_tipo`

Agora qualquer string entra. Recriar com novos valores ou enum tipo PG.

### 7.5. `aviso_dispensado.usuario_id` sem FK
`V61__aviso_dispensado.sql:4` declara `BIGINT NOT NULL` sem FOREIGN KEY → registros órfãos.

### 7.6. `aceitou_termo` sem default
`V68:8` adiciona `BOOLEAN` nullable. Em queries `WHERE aceitou_termo = true`, NULL é excluído. **Adicionar `DEFAULT FALSE`.**

### 7.7. Índices faltando
- `notificacao_enviada(agendamento_id, tipo)` — usado em `findConfirmacoesPendentes` com NOT EXISTS sem índice → seq scan.
- `notificacao_enviada(status, dt_envio)` — pickup de jobs.

---

## 8. Tratamento de erros / logs

### 8.1. `SecurityException` cai em handler genérico → 500
`config/GlobalExceptionHandler.java` não trata `SecurityException`. 13+ pontos lançam essa exceção. Acesso negado vira 500 + log error. **Adicionar handler que retorne 403.**

### 8.2. Stacktrace no body
- `TenantPageController.java:89,128` → `"Erro interno: " + e.getMessage()`. Se `e` for SQL exception, vaza schema.
- Vários controllers retornam `e.getMessage()` em 500 (`OrganizacaoController:65,511`, `EmailController:59`, `InstanceController:76`).

### 8.3. PII em logs INFO
- `AnamneseWhatsAppService.java:134` → telefone completo
- `FilaEsperaDispatchService.java:128` → idem
- `PaymentApiClient.java:59-60` → CPF + email + nome

### 8.4. `e.printStackTrace()` em produção
- `AuthController.java:188-189`
- `ClienteController.java:176-177`
- `FileStorageService.java:181-185, 252-255` (10+ `System.out.println` com emojis)

### 8.5. `RuntimeException(e.getMessage())` perde causa raiz
`InstanceService.java:190, 403, 579, 708, 743` + `BloqueioOrganizacaoService.java:266`. Stacktrace original sumiu — debugging cego.

### 8.6. Rawpayload de webhook com PII no banco
`WebhookEventLog.payload` salva payload integral (valores, CPF, dados de cartão potencialmente). Se DB comprometido → vazamento. **Mascarar ou criptografar coluna.**

---

## 9. Integrações externas

### 9.1. Sem retry / circuit breaker
- `RestTemplate` global, `connectTimeout=5s`, `readTimeout=30s`.
- `InstanceService.createInstance` (síncrono) → Evolution lento → request usuário pendura 30s.
- Sem `@Retryable` (mesmo `@EnableRetry` declarado).
- Sem Resilience4j.

### 9.2. `N8nWebhookClient.block()` sem retry
`service/notificacao/N8nWebhookClient.java:51` → bloqueia thread se n8n estiver fora.

### 9.3. SMTP sem timeout
`application.properties:118-125` → falta `mail.smtp.connectiontimeout`, `.timeout`, `.writetimeout`. Default Java é INFINITO. `@Async` no email pode esgotar pool.

### 9.4. Webhook reentrância
`PaymentWebhookService.java:68-98` → `existsByEventId` + `save` sem lock. Mesmo eventId em paralelo passa o check 2×. **Adicionar `UNIQUE` constraint em `event_id` + try/catch `DataIntegrityViolationException`.**

### 9.5. `FilaEsperaDispatchService.dispatch` marca FALHA permanente em telefone inválido
Sem distinção transiente vs definitivo. **Adicionar contador de retries.**

---

## 10. Código pendente / sujo

### 10.1. `UserInfoService` retorna estatísticas zeradas
12 TODOs em `UserInfoService.java:97-101, 191-222`. `buildClienteInfo` retorna `0L`/`null` em totalAgendamentos, totalCompras, valorGasto, etc. **Front recebe estats zeradas no `/login` e `/validate`.** Implementar ou remover do DTO.

### 10.2. `TestController` em prod
`/api/v1/test` retorna `"Olá! Esta é uma mensagem pública..."`. Whitelisted. **Remover.**

### 10.3. `DataInitializer` no-op
`DataInitializer.java:17` → `// seeder.seedDatabase();` comentado. Bean inteiro é no-op. **Remover ou reativar.**

### 10.4. Endpoints comentados
- `ProdutoController.java:59-74` (versão paginada de listar)
- `OrganizacaoController.java:183-209` (`buscarPorId`)
- `AgendamentoService.java:972, 1832-1894` (concluir/disponibilidade)

### 10.5. `System.out.println` em produção
- `FileStorageService.java` 20+ ocorrências com emojis
- `DatabaseSeederService` dezenas (apenas dev profile, OK)

---

## Plano de ação faseado

### 🚨 FASE 0 — Pré-deploy (bloqueia o lançamento)

1. **Rotacionar TODOS os secrets** (§1) e remover defaults dos properties.
2. **Corrigir `TokenService` property name** (§2.2).
3. **Adicionar `@EnableMethodSecurity`** + auditar `@PreAuthorize` em controllers admin (§2.3).
4. **Remover whitelists perigosas** do `SecurityConfig` — endpoints `/agendamento`, `/cliente`, `/funcionario`, `/servico`, `/produto`, `/dashboard`, `/test`, `/email/teste`, `/instances/by-name` (§2.1).
5. **Adicionar `validarOrganizacao()`** nos 6+ métodos do `AgendamentoService` (§3).
6. **HMAC no webhook Payment** (§9.4 e §6 da audit de segurança).
7. **Auth nos webhooks confirmacao/fila-espera** (§4 endpoints públicos).
8. **CAPTCHA + rate-limit em `POST /organizacao`** e `verificar-*` (§4).
9. **Travar `SPRING_PROFILES_ACTIVE=prod`** no deploy + travar root logger em INFO.
10. **`GlobalExceptionHandler` para `SecurityException`** → 403 (§8.1).
11. **Remover `printStackTrace`/`System.out.println`** em controllers/services produtivos.
12. **Limitar payload upload** (`multipart.max-request-size=10MB`).
13. **Configurar SMTP timeouts**.
14. **`UserInfoService`**: implementar estatísticas ou remover do DTO de auth.

### 📋 FASE 1 — Primeiras 2 semanas após go-live

15. **Cliente soft-delete** + remover `cascade=ALL` em coleções financeiras.
16. **Particionar/archive** `notificacao_enviada`, `webhook_event_log`, `tracking_*`.
17. **`@Retryable`** em integrações (Evolution, n8n, Payment).
18. **Refatorar `NotificacaoSchedulerService`** (Thread.sleep → fila async).
19. **`AgendamentoService.filtrarAgendamentos`** com `Specification` + `Pageable`.
20. **Paginação** em listagens críticas (`AgendamentoRepository`, `ClienteRepository`).
21. **Hikari pool=30-50** + leak detection.
22. **Healthcheck no Dockerfile** + non-root user.
23. **Mascarar PII em logs** (telefone, CPF, email).
24. **Rotação de logs** Docker (`json-file` com max-size/max-file).
25. **Recriar CHECKs** em `notificacao_enviada` (status, tipo).
26. **CSV/HMAC validation** em todos webhooks externos.

### 🔧 FASE 2 — Hardening contínuo (1-3 meses)

27. Circuit breaker (Resilience4j) em integrações.
28. Health indicators custom (Evolution/Payment/n8n/SMTP).
29. Reduzir EAGER fetches em entities críticas (`Page`, `Produto`).
30. JOIN FETCH ou `@EntityGraph` em queries de listagem.
31. Migration `WITH TIME ZONE` em colunas de eventos (multi-fuso futuro).
32. `Set` vs `List` em `@ManyToMany` (`Agendamento.servicos`/`funcionarios`).
33. Trigger SQL de imutabilidade em campos de prova legal (termo).
34. Endpoint admin de purga LGPD (respostas vencidas).
35. Limpar endpoints comentados em controllers/services.

---

## Arquivos a editar (consolidado)

### Crítico (FASE 0)
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`
- `src/main/java/org/exemplo/bellory/config/SecurityConfig.java`
- `src/main/java/org/exemplo/bellory/service/TokenService.java`
- `src/main/java/org/exemplo/bellory/service/AgendamentoService.java`
- `src/main/java/org/exemplo/bellory/service/ProdutoService.java`
- `src/main/java/org/exemplo/bellory/service/webhook/PaymentWebhookService.java`
- `src/main/java/org/exemplo/bellory/controller/app/EmailController.java`
- `src/main/java/org/exemplo/bellory/controller/app/InstanceController.java`
- `src/main/java/org/exemplo/bellory/controller/app/OrganizacaoController.java`
- `src/main/java/org/exemplo/bellory/controller/app/TestController.java` (remover)
- `src/main/java/org/exemplo/bellory/exception/GlobalExceptionHandler.java`
- `src/main/java/org/exemplo/bellory/service/UserInfoService.java`
- `src/main/java/org/exemplo/bellory/service/ForgotPasswordService.java`
- `src/main/java/org/exemplo/bellory/service/ForgotPasswordRateLimiterService.java`
- `Dockerfile`
- `docker-compose.yml`

### Importante (FASE 1)
- `src/main/java/org/exemplo/bellory/model/entity/users/Cliente.java`
- `src/main/java/org/exemplo/bellory/service/notificacao/NotificacaoSchedulerService.java`
- `src/main/java/org/exemplo/bellory/client/payment/PaymentApiClient.java`
- `src/main/java/org/exemplo/bellory/service/notificacao/N8nWebhookClient.java`
- `src/main/java/org/exemplo/bellory/service/InstanceService.java`
- `src/main/java/org/exemplo/bellory/service/FileStorageService.java`
- Migrations novas: `V70__cliente_soft_delete.sql`, `V71__notificacao_recriar_check.sql`, etc.

---

## Métricas finais

| Severidade | Quantidade aproximada | Tempo estimado |
|---|---|---|
| BLOCKER | ~30 | 5–10 dias |
| HIGH | ~40 | 2–4 semanas |
| MEDIUM | ~40 | 1–2 meses |
| LOW | ~30 | hardening contínuo |

**Recomendação final:** **NÃO subir para produção** sem completar Fase 0. Priorizar resolução em sprints de 1 semana, com foco em segredos + auth + cross-tenant nas duas primeiras semanas.

---

> **Observação:** este relatório foi gerado por análise estática automatizada. Todos os achados precisam de validação humana antes de fix. Alguns podem ser falsos positivos. Recomendo testar cada correção em ambiente de staging.
