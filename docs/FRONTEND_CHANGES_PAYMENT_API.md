# Mudanças para o Front-end — Migração Payment API

> Referência de contrato: o que o frontend precisa mudar após a migração do Bellory para a Payment API externa.

---

## 1. Endpoints removidos

Todos os endpoints abaixo **foram deletados** — qualquer chamada retornará 404:

| Método | Path | Substituto |
|---|---|---|
| GET | `/api/v1/assinatura/status` | Dados vêm no `/auth/login` e `/auth/me` em `organizacao.assinatura` |
| POST | `/api/v1/assinatura/escolher-plano` | Chamar direto na Payment API (ver §6) |
| POST | `/api/v1/assinatura/forma-pagamento` | Chamar `PATCH /api/v1/subscriptions/{id}/payment-method` na Payment API |
| GET | `/api/v1/assinatura/cobrancas` | `GET /api/v1/subscriptions/{id}/charges` na Payment API |
| POST | `/api/v1/assinatura/cupom` | `POST /api/v1/coupons/validate` na Payment API |
| GET | `/api/v1/public/planos` | `GET /api/v1/plans` na Payment API (com `X-API-Key` pública) |
| POST | `/api/v1/public/validar-cupom` | `POST /api/v1/coupons/validate/public` na Payment API |
| POST | `/api/v1/webhook/assas` | Payment API trata webhooks internamente |
| GET/POST | `/api/v1/admin/assinaturas/**` | Listagem/detalhe via endpoints da Payment API |
| GET/POST | `/api/v1/admin/planos/**` | `GET/POST /api/v1/plans` na Payment API |
| GET/POST | `/api/v1/admin/cupons/**` | `/api/v1/coupons` na Payment API |
| GET | `/api/v1/admin/metricas/planos` | Reports da Payment API (`/api/v1/reports/*`) |

---

## 2. Endpoint novo

### `POST /api/v1/assinatura/refresh-cache`

- **Auth**: JWT Bearer (usuário autenticado no Bellory).
- **Body**: nenhum.
- **Response 200**: `AssinaturaStatusDTO` (ver §3).

**Quando chamar**: sempre que o usuário executar alguma ação na Payment API que afete status/plano/limites (trocar plano, pagar cobrança, inserir cartão, etc.). Sem essa chamada, o Bellory continua servindo o status antigo até expirar o TTL de 5 min.

---

## 3. DTOs alterados

### `AssinaturaStatusDTO` (em `organizacao.assinatura` no login e `user.assinatura` no /me)

**Campos preenchidos agora** (após migração):

```typescript
interface AssinaturaStatusDTO {
  bloqueado: boolean;
  statusAssinatura: string;        // "ACTIVE"|"PAUSED"|"SUSPENDED"|"CANCELED"|"EXPIRED"|"INDISPONIVEL"|"NAO_MIGRADO"|"SEM_ASSINATURA"
  situacao: string;                // "ATIVA"|"PLANO_GRATUITO"|"PAGAMENTO_ATRASADO"|"SUSPENSA"|"CANCELADA_SEM_ACESSO"|"VENCIDA"|"SEM_ASSINATURA"
  mensagem: string | null;         // só preenchido quando bloqueado=true
  planoCodigo: string | null;      // "gratuito" | "basico" | "plus" | "premium"
  planoNome: string | null;
  planoGratuito: boolean;
  cicloCobranca: string | null;    // "MONTHLY" | "SEMIANNUALLY" | "YEARLY"  ⚠️ mudou de "MENSAL"/"ANUAL"
  dtProximoVencimento: string | null; // ISO date
}
```

**⚠️ Campos que NÃO vêm mais** (se o front lê, vão vir `null`):
- `diasRestantesTrial`, `dtFimTrial` — trial agora é controle da Payment API, front busca direto lá se precisar.
- `temCobrancaPendente`, `valorPendente`, `dtVencimentoProximaCobranca` — usar `GET /api/v1/subscriptions/{id}/charges` da Payment API.
- `dtAcessoAte` — não usado mais; se bloqueado, usar `mensagem`.
- `planoAgendadoCodigo`, `planoAgendadoNome`, `cicloAgendado` — troca agendada é responsabilidade da Payment API (`GET /api/v1/subscriptions/{subscriptionId}/plan-changes`).

**⚠️ Atenção ao valor de `cicloCobranca`**: antes vinha `MENSAL`/`ANUAL`, agora vem `MONTHLY`/`SEMIANNUALLY`/`YEARLY` (valores da Payment API). Ajustar enums/traduções no front.

### `OrganizacaoInfoDTO` (em `organizacao` no login)

**Removidos**:
- `plano: PlanoBellory` — não vem mais. Use `organizacao.assinatura.planoCodigo` + `planoNome`.
- `limitesPersonalizados: PlanoLimitesBellory` — não vem mais. Front consulta limites via Payment API (ver §5).

### `OrganizacaoResponseDTO` (resposta do signup e update)

**Removidos**: `planoId`, `planoNome`, `plano`, `limitesPersonalizados`.

### `UpdateOrganizacaoDTO` (request do update)

**Removidos**: `plano`, `limitesPersonalizados`.

---

## 4. Novo código HTTP: 422 — Limite do plano excedido

Quando o usuário tenta criar um recurso que ultrapassa o limite do plano, o Bellory agora responde **422**. Afeta os endpoints:

- `POST /api/v1/cliente`
- `POST /api/v1/agendamento` (valida agendamentos do mês corrente)
- `POST /api/v1/funcionario`
- `POST /api/v1/instances` (quando `interno=false`)
- `POST /api/v1/landing-pages`

**Body da resposta**:
```json
{
  "success": false,
  "message": "Limite de clientes excedido. Plano basico permite ate 100 (tentativa: 101). Faca upgrade para ampliar.",
  "errorCode": 422,
  "dados": {
    "limitKey": "cliente",
    "limiteMaximo": 100,
    "usoAtual": 101
  }
}
```

**Ação no front**: tratar 422 nesses endpoints exibindo a `message` e, opcionalmente, redirecionando para a tela de upgrade de plano. Os campos em `dados` permitem personalizar a UI (ex.: "Seu plano permite até **100** clientes — faça upgrade").

---

## 5. Limites granulares — como consultar no front

Antes os limites vinham embutidos em `organizacao.plano.limites` e `organizacao.limitesPersonalizados`. Agora você tem **duas opções**:

### Opção A — Consultar direto na Payment API (recomendado)
```
GET /api/v1/plans/{planId}/limits
Headers: X-API-Key: <chave>, X-Company-Id: 4
```

Retorna:
```json
[
  {"key": "cliente", "label": "Cliente", "type": "NUMBER", "value": 100},
  {"key": "funcionario", "label": "Funcionário", "type": "NUMBER", "value": 3},
  {"key": "api", "label": "API", "type": "BOOLEAN", "enabled": false},
  {"key": "agendamento", "label": "Agendamento", "type": "UNLIMITED"}
]
```

Tipo `NUMBER`: compara com `value`. `BOOLEAN`: feature on/off via `enabled`. `UNLIMITED`: sempre libera.

### Opção B — Check direto de um limite específico
```
GET /api/v1/plans/{planId}/limits/{key}?usage=101
```

Retorna já decidido: `allowed: true|false`. Útil pra guard-clauses antes de submeter um form.

---

## 6. Fluxo de troca de plano / pagamento

Antes o Bellory orquestrava tudo. Agora o frontend fala **direto** com a Payment API:

### Trocar plano
```
POST https://pay-api.bellory.com.br/api/v1/subscriptions/{subscriptionId}/change-plan
```
(Preview antes: `POST /subscriptions/{id}/preview-change`)

### Atualizar forma de pagamento
```
PATCH https://pay-api.bellory.com.br/api/v1/subscriptions/{subscriptionId}/payment-method
```

### Depois de qualquer ação na Payment API
**Chame `POST /api/v1/assinatura/refresh-cache` no Bellory** para que o interceptor e o LimiteValidator enxerguem o novo estado imediatamente. Sem isso, o Bellory continua servindo status antigo por até 5 min.

### Auth na Payment API
Para operações de usuário final (troca de plano etc.), o frontend precisa de JWT da Payment API. Opções:
- **API Key pública para leitura** (pricing, planos públicos) — enviar `X-API-Key` direto do front (plano básico, pois é exposto).
- **JWT de usuário Payment API** — login em `POST /api/v1/auth/login` da Payment API usando as credenciais do tenant.
- **Proxy via Bellory** — NÃO implementado ainda; se preferir, o backend pode expor endpoints proxy que reassinam com a API Key sistema-a-sistema.

> Recomendo alinhar com a equipe da Payment API qual estratégia de auth usar pelo front antes do cutover.

---

## 7. Checklist de mudanças no código do front

- [ ] Substituir consumo de `GET /api/v1/assinatura/status` por `organizacao.assinatura` do `/auth/login` e `user.assinatura` do `/auth/me`.
- [ ] Atualizar valor de `cicloCobranca`: `MENSAL` → `MONTHLY`, `ANUAL` → `YEARLY`.
- [ ] Remover uso de `organizacao.plano` e `organizacao.limitesPersonalizados` — buscar na Payment API.
- [ ] Remover campos de trial/cobrança-pendente/plano-agendado do DTO de assinatura (hoje vêm `null`, amanhã podem sumir de vez).
- [ ] Adicionar tratamento de HTTP **422** nos POSTs de `cliente`, `agendamento`, `funcionario`, `instance`, `landing-page` — exibindo `message` + link pra upgrade.
- [ ] Tela de planos públicos: trocar fonte de `GET /api/v1/public/planos` → `GET <payment-api>/api/v1/plans`.
- [ ] Tela de trocar plano: integrar com `<payment-api>/api/v1/subscriptions/{id}/change-plan`.
- [ ] Tela de pagamento: integrar com `<payment-api>/api/v1/subscriptions/{id}/payment-method` e `/charges/credit-card|pix|boleto`.
- [ ] Depois de qualquer ação na Payment API → chamar `POST /api/v1/assinatura/refresh-cache` no Bellory.
- [ ] Painel admin: remover views que dependiam de `/admin/assinaturas`, `/admin/planos`, `/admin/cupons`, `/admin/metricas/planos`. Se precisar admin global dessas infos, criar telas consumindo a Payment API.
- [ ] Remover componentes de webhook Asaas (se houver — geralmente não há no front).
