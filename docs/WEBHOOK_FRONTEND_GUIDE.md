# Webhook Payment API — Guia de Integração Frontend

## Visao Geral

O modulo de Webhook recebe eventos da Payment API (cobrancas, pagamentos, troca de plano, etc.) e executa acoes automaticas: push notification para admins, email para a organizacao e invalidacao de cache. O painel admin permite configurar quais eventos disparam quais acoes e visualizar o historico completo.

---

## Arquitetura

```
Payment API ──POST──▶ /api/v1/webhook/payment (publico, token proprio)
                         │
                         ├─ Valida token Bearer (comparacao tempo-constante)
                         ├─ Verifica idempotencia (eventId duplicado = ignora)
                         ├─ Resolve organizacaoId (customerId → Assinatura → Org)
                         ├─ Salva no webhook_event_log (status: RECEIVED)
                         ├─ Retorna 200 OK imediato
                         │
                         └─ Async ──▶ WebhookEventProcessor
                                       │
                                       ├─ Busca webhook_event_config do evento
                                       ├─ Se invalidarCache = true → refresh cache Redis
                                       ├─ Se pushEnabled = true → push para ROLE_ADMIN da org
                                       ├─ Se emailEnabled = true → email para emailPrincipal da org
                                       └─ Atualiza log (status: PROCESSED ou FAILED)
```

---

## Endpoints da API Admin

**Base URL**: `/api/v1/admin/webhook`
**Autenticacao**: JWT token com role `PLATFORM_ADMIN`, `SUPERADMIN` ou `ADMIN`

---

### 1. Configuracao do Token

O token e gerado na Payment API ao criar o webhook. Voce salva esse token no Bellory para validar as requisicoes recebidas.

#### GET /config

Retorna a configuracao ativa do token.

**Response 200:**
```json
{
  "id": 1,
  "token": "whk_abc123...",
  "ativo": true,
  "descricao": "Token webhook producao"
}
```

**Response 204:** Nenhuma configuracao cadastrada.

---

#### POST /config

Cria ou atualiza o token do webhook. Se ja existir uma config ativa, atualiza.

**Request:**
```json
{
  "token": "whk_abc123...",
  "descricao": "Token webhook producao"
}
```

**Response 200:**
```json
{
  "id": 1,
  "token": "whk_abc123...",
  "ativo": true,
  "descricao": "Token webhook producao"
}
```

---

### 2. Configuracao de Eventos

Cada tipo de evento tem toggles independentes para push, email, cache e ativo/inativo.

#### GET /eventos/config

Lista todos os 14 tipos de evento e suas configuracoes.

**Response 200:**
```json
[
  {
    "id": 1,
    "eventType": "ChargeCreatedEvent",
    "descricao": "Cobranca criada",
    "pushEnabled": true,
    "emailEnabled": true,
    "invalidarCache": false,
    "ativo": true
  },
  {
    "id": 2,
    "eventType": "ChargePaidEvent",
    "descricao": "Pagamento confirmado",
    "pushEnabled": true,
    "emailEnabled": true,
    "invalidarCache": true,
    "ativo": true
  }
  // ... 12 mais
]
```

---

#### PUT /eventos/config/{id}

Atualiza a configuracao de um evento especifico (toggles).

**Request:**
```json
{
  "pushEnabled": true,
  "emailEnabled": false,
  "invalidarCache": true,
  "ativo": true,
  "descricao": "Cobranca criada"
}
```

**Response 200:** Retorna o objeto atualizado.

**Response 404:** ID nao encontrado.

---

### 3. Historico de Eventos (Log)

#### GET /eventos/log

Lista os eventos recebidos com paginacao e filtros.

**Query Parameters:**

| Param | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| `eventType` | String | Nao | Filtrar por tipo (ex: `ChargePaidEvent`) |
| `organizacaoId` | Long | Nao | Filtrar por organizacao |
| `page` | int | Nao | Pagina (default: 0) |
| `size` | int | Nao | Itens por pagina (default: 20) |
| `sort` | String | Nao | Ordenacao (default: `dtRecebido,desc`) |

**Response 200:**
```json
{
  "content": [
    {
      "id": 42,
      "eventId": "evt_4821",
      "deliveryId": "dlv_9001",
      "eventType": "ChargePaidEvent",
      "companyId": 4,
      "organizacaoId": 3,
      "resourceType": "Charge",
      "resourceId": "58",
      "payload": "{\"id\":\"evt_4821\",\"type\":\"ChargePaidEvent\",...}",
      "status": "PROCESSED",
      "errorMessage": null,
      "dtRecebido": "2026-04-19T15:30:00",
      "dtProcessado": "2026-04-19T15:30:01"
    }
  ],
  "totalElements": 156,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

---

#### GET /eventos/log/{id}

Retorna um evento especifico com o payload completo.

**Response 200:** Objeto WebhookEventLog completo.

**Response 404:** Evento nao encontrado.

---

## Tipos de Evento

### Tabela completa

| Evento | Grupo | Descricao | Push | Email | Cache | Ativo |
|--------|-------|-----------|------|-------|-------|-------|
| `ChargeCreatedEvent` | Cobranca | Cobranca criada (PIX/Boleto/Cartao) | ✅ | ✅ | ❌ | ✅ |
| `ChargePaidEvent` | Cobranca | Pagamento confirmado | ✅ | ✅ | ✅ | ✅ |
| `ChargeCanceledEvent` | Cobranca | Cobranca cancelada | ✅ | ❌ | ❌ | ✅ |
| `ChargeRefundedEvent` | Cobranca | Estorno realizado | ✅ | ✅ | ❌ | ✅ |
| `SubscriptionCreatedEvent` | Assinatura | Assinatura criada | ❌ | ❌ | ❌ | ✅ |
| `SubscriptionPausedEvent` | Assinatura | Assinatura pausada | ✅ | ✅ | ✅ | ✅ |
| `SubscriptionResumedEvent` | Assinatura | Assinatura retomada | ✅ | ✅ | ✅ | ✅ |
| `SubscriptionSuspendedEvent` | Assinatura | Suspensa por inadimplencia | ✅ | ✅ | ✅ | ✅ |
| `SubscriptionCanceledEvent` | Assinatura | Cancelamento definitivo | ✅ | ✅ | ✅ | ✅ |
| `PlanChangeScheduledEvent` | Plano | Troca agendada (fim do ciclo) | ✅ | ❌ | ❌ | ✅ |
| `PlanChangePendingPaymentEvent` | Plano | Troca aguardando pagamento | ✅ | ❌ | ❌ | ✅ |
| `PlanChangedEvent` | Plano | Troca efetivada | ✅ | ✅ | ✅ | ✅ |
| `CustomerCreatedEvent` | Cliente | Cliente criado na Payment API | ❌ | ❌ | ❌ | ✅ |
| `WebhookTestEvent` | Sistema | Evento de teste (ping) | ❌ | ❌ | ❌ | ✅ |

> Esses sao os valores **padrao** do seed. O admin pode alterar qualquer toggle via `PUT /eventos/config/{id}`.

### Agrupamento sugerido para o frontend

```
Cobrancas
  ├─ ChargeCreatedEvent
  ├─ ChargePaidEvent
  ├─ ChargeCanceledEvent
  └─ ChargeRefundedEvent

Assinatura
  ├─ SubscriptionCreatedEvent
  ├─ SubscriptionPausedEvent
  ├─ SubscriptionResumedEvent
  ├─ SubscriptionSuspendedEvent
  └─ SubscriptionCanceledEvent

Troca de Plano
  ├─ PlanChangeScheduledEvent
  ├─ PlanChangePendingPaymentEvent
  └─ PlanChangedEvent

Sistema
  ├─ CustomerCreatedEvent
  └─ WebhookTestEvent
```

---

## Status do Evento (ciclo de vida)

```
RECEIVED ──▶ PROCESSING ──▶ PROCESSED
                         └──▶ FAILED
```

| Status | Descricao | Cor sugerida |
|--------|-----------|-------------|
| `RECEIVED` | Evento recebido, aguardando processamento | Azul / Info |
| `PROCESSING` | Em processamento async | Amarelo / Warning |
| `PROCESSED` | Processado com sucesso | Verde / Success |
| `FAILED` | Erro no processamento (ver `errorMessage`) | Vermelho / Error |

---

## Guia de Implementacao do Frontend

### Tela 1: Configuracao do Webhook Token

**Rota sugerida**: `/admin/webhook/config`

**Comportamento:**
1. Ao carregar, chamar `GET /api/v1/admin/webhook/config`
2. Se 204 (sem config), mostrar formulario vazio com botao "Salvar"
3. Se 200, mostrar token atual (mascarado: `whk_abc1****`) com opcao de editar
4. Ao salvar, chamar `POST /api/v1/admin/webhook/config`

**Campos do formulario:**

| Campo | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| Token | text/password | Sim | Token gerado na Payment API |
| Descricao | text | Nao | Descricao livre (ex: "Producao", "Staging") |

**Dicas de UX:**
- Mostrar o token mascarado por padrao, com botao de "olho" para revelar
- Adicionar indicador visual de status (verde = ativo, vermelho = sem config)
- Mostrar instrucoes: "Gere o webhook na Payment API apontando para `https://api.bellory.com.br/api/v1/webhook/payment` e cole o token aqui"

---

### Tela 2: Configuracao de Eventos

**Rota sugerida**: `/admin/webhook/eventos`

**Comportamento:**
1. Carregar `GET /api/v1/admin/webhook/eventos/config`
2. Renderizar lista agrupada por categoria (Cobrancas, Assinatura, Plano, Sistema)
3. Cada evento mostra 4 toggles: Push, Email, Cache, Ativo
4. Ao alterar qualquer toggle, chamar `PUT /api/v1/admin/webhook/eventos/config/{id}`

**Layout sugerido:**

```
┌─────────────────────────────────────────────────────────────────┐
│ Configuracao de Eventos                                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ COBRANCAS                                                       │
│ ┌─────────────────────────┬──────┬───────┬───────┬──────┐      │
│ │ Evento                  │ Push │ Email │ Cache │ Ativo│      │
│ ├─────────────────────────┼──────┼───────┼───────┼──────┤      │
│ │ Cobranca criada         │  ●   │   ●   │   ○   │  ●   │      │
│ │ Pagamento confirmado    │  ●   │   ●   │   ●   │  ●   │      │
│ │ Cobranca cancelada      │  ●   │   ○   │   ○   │  ●   │      │
│ │ Estorno realizado       │  ●   │   ●   │   ○   │  ●   │      │
│ └─────────────────────────┴──────┴───────┴───────┴──────┘      │
│                                                                 │
│ ASSINATURA                                                      │
│ ┌─────────────────────────┬──────┬───────┬───────┬──────┐      │
│ │ Assinatura criada       │  ○   │   ○   │   ○   │  ●   │      │
│ │ Assinatura pausada      │  ●   │   ●   │   ●   │  ●   │      │
│ │ ...                     │      │       │       │      │      │
│ └─────────────────────────┴──────┴───────┴───────┴──────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Legenda dos toggles:**
- **Push**: Envia notificacao push para todos os admins (ROLE_ADMIN) da organizacao afetada
- **Email**: Envia email para o emailPrincipal da organizacao afetada
- **Cache**: Invalida o cache Redis da assinatura (forca refresh dos dados de plano/status)
- **Ativo**: Se desativado, o evento e logado mas nenhuma acao e executada

---

### Tela 3: Historico de Eventos

**Rota sugerida**: `/admin/webhook/log`

**Comportamento:**
1. Carregar `GET /api/v1/admin/webhook/eventos/log?page=0&size=20`
2. Mostrar tabela paginada com filtros
3. Clicar em um evento abre detalhe com payload completo

**Filtros:**

| Filtro | Param | Tipo | Opcoes |
|--------|-------|------|--------|
| Tipo de evento | `eventType` | select | Lista dos 14 tipos |
| Organizacao | `organizacaoId` | number/select | ID ou dropdown |

**Colunas da tabela:**

| Coluna | Campo | Formato |
|--------|-------|---------|
| Data/Hora | `dtRecebido` | `dd/MM/yyyy HH:mm:ss` |
| Tipo | `eventType` | Badge com cor por grupo |
| Organizacao | `organizacaoId` | ID ou nome (se disponivel) |
| Recurso | `resourceType` + `resourceId` | Ex: "Charge #58" |
| Status | `status` | Badge colorido (ver tabela de status) |
| Processado em | `dtProcessado` | `dd/MM/yyyy HH:mm:ss` ou "-" |

**Detalhe do evento (modal ou pagina):**

```
┌──────────────────────────────────────────────┐
│ Evento: ChargePaidEvent                       │
├──────────────────────────────────────────────┤
│ Event ID:      evt_4821                       │
│ Delivery ID:   dlv_9001                       │
│ Tipo:          ChargePaidEvent                │
│ Organizacao:   #3 - Bellory Salon             │
│ Recurso:       Charge #58                     │
│ Status:        PROCESSED ✅                   │
│ Recebido:      19/04/2026 15:30:00            │
│ Processado:    19/04/2026 15:30:01            │
│ Erro:          -                              │
│                                               │
│ Payload:                                      │
│ ┌────────────────────────────────────────┐    │
│ │ {                                      │    │
│ │   "id": "evt_4821",                    │    │
│ │   "type": "ChargePaidEvent",           │    │
│ │   "companyId": 4,                      │    │
│ │   "data": {                            │    │
│ │     "id": 58,                          │    │
│ │     "value": 99.90,                    │    │
│ │     "status": "CONFIRMED",             │    │
│ │     "billingType": "PIX",              │    │
│ │     ...                                │    │
│ │   }                                    │    │
│ │ }                                      │    │
│ └────────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

**Cores sugeridas por grupo de evento:**

| Grupo | Cor | Eventos |
|-------|-----|---------|
| Cobranca | Azul | `Charge*Event` |
| Assinatura | Roxo | `Subscription*Event` |
| Plano | Laranja | `PlanChange*Event` |
| Sistema | Cinza | `Customer*Event`, `WebhookTestEvent` |

---

## Setup Inicial (Passo a Passo)

### 1. Criar o webhook na Payment API

Na Payment API, criar um webhook apontando para:
```
URL: https://api.bellory.com.br/api/v1/webhook/payment
```

A Payment API vai gerar um **token** para autenticacao.

### 2. Salvar o token no Bellory

```http
POST /api/v1/admin/webhook/config
Authorization: Bearer {jwt-admin}
Content-Type: application/json

{
  "token": "whk_token_gerado_pela_payment_api",
  "descricao": "Webhook producao"
}
```

### 3. Verificar configuracao de eventos

```http
GET /api/v1/admin/webhook/eventos/config
Authorization: Bearer {jwt-admin}
```

Os 14 eventos ja vem pre-configurados com valores padrao sensatos. Ajuste conforme necessario.

### 4. Testar

Na Payment API, usar o botao "Ping" para disparar um `WebhookTestEvent`. Verificar no log:

```http
GET /api/v1/admin/webhook/eventos/log?eventType=WebhookTestEvent
Authorization: Bearer {jwt-admin}
```

---

## Notificacoes Geradas

### Push Notifications (para admins da organizacao)

| Evento | Titulo | Prioridade |
|--------|--------|------------|
| `ChargeCreatedEvent` | "Nova cobranca gerada R$ 99,90" | BAIXA |
| `ChargePaidEvent` | "Pagamento confirmado R$ 99,90" | MEDIA |
| `ChargeCanceledEvent` | "Cobranca cancelada R$ 99,90" | BAIXA |
| `ChargeRefundedEvent` | "Estorno realizado R$ 99,90" | BAIXA |
| `SubscriptionPausedEvent` | "Assinatura pausada" | BAIXA |
| `SubscriptionResumedEvent` | "Assinatura retomada" | BAIXA |
| `SubscriptionSuspendedEvent` | "Assinatura suspensa" | ALTA |
| `SubscriptionCanceledEvent` | "Assinatura cancelada" | ALTA |
| `PlanChangeScheduledEvent` | "Troca de plano agendada" | BAIXA |
| `PlanChangePendingPaymentEvent` | "Troca de plano aguardando pagamento" | BAIXA |
| `PlanChangedEvent` | "Plano alterado para Premium" | MEDIA |

- **Destino**: Todos os usuarios com `ROLE_ADMIN` da organizacao afetada
- **Categoria**: `PAGAMENTO` (icone: DollarSign)
- **Origem**: "Payment API"

### Emails (para emailPrincipal da organizacao)

| Evento | Assunto |
|--------|---------|
| `ChargeCreatedEvent` | "Bellory — Nova cobranca gerada R$ 99,90" |
| `ChargePaidEvent` | "Bellory — Pagamento confirmado R$ 99,90" |
| `ChargeRefundedEvent` | "Bellory — Estorno realizado R$ 99,90" |
| `SubscriptionPausedEvent` | "Bellory — Assinatura pausada" |
| `SubscriptionResumedEvent` | "Bellory — Assinatura retomada" |
| `SubscriptionSuspendedEvent` | "Bellory — Atencao: assinatura suspensa" |
| `SubscriptionCanceledEvent` | "Bellory — Assinatura cancelada" |
| `PlanChangedEvent` | "Bellory — Plano alterado com sucesso" |

- **Destino**: `emailPrincipal` da organizacao
- **Formato**: Texto simples com saudacao usando `nomeFantasia` da org

---

## Campos dos DTOs

### WebhookConfigDTO

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `id` | Long | ID no banco |
| `token` | String | Token Bearer para validacao |
| `ativo` | boolean | Se esta ativo |
| `descricao` | String | Descricao livre |

### WebhookEventConfigDTO

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `id` | Long | ID no banco |
| `eventType` | String | Identificador do evento (ex: `ChargePaidEvent`) |
| `descricao` | String | Descricao legivel |
| `pushEnabled` | boolean | Envia push notification |
| `emailEnabled` | boolean | Envia email |
| `invalidarCache` | boolean | Invalida cache Redis da assinatura |
| `ativo` | boolean | Evento ativo (se false, loga mas nao executa acoes) |

### WebhookEventLog

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `id` | Long | ID no banco |
| `eventId` | String | ID unico do evento na Payment API |
| `deliveryId` | String | ID da tentativa de entrega |
| `eventType` | String | Tipo do evento |
| `companyId` | Long | Company ID na Payment API |
| `organizacaoId` | Long | ID da organizacao no Bellory (resolvido automaticamente) |
| `resourceType` | String | Tipo do recurso (Charge, Subscription, PlanChange) |
| `resourceId` | String | ID do recurso |
| `payload` | String | JSON bruto completo do evento |
| `status` | String | `RECEIVED`, `PROCESSING`, `PROCESSED`, `FAILED` |
| `errorMessage` | String | Mensagem de erro (se FAILED) |
| `dtRecebido` | datetime | Data/hora de recebimento |
| `dtProcessado` | datetime | Data/hora de conclusao do processamento |

---

## Exemplos de Chamadas (cURL)

### Salvar token
```bash
curl -X POST https://api.bellory.com.br/api/v1/admin/webhook/config \
  -H "Authorization: Bearer {jwt}" \
  -H "Content-Type: application/json" \
  -d '{"token":"whk_meu_token","descricao":"Producao"}'
```

### Listar configuracao de eventos
```bash
curl https://api.bellory.com.br/api/v1/admin/webhook/eventos/config \
  -H "Authorization: Bearer {jwt}"
```

### Desativar email para ChargeCreatedEvent
```bash
curl -X PUT https://api.bellory.com.br/api/v1/admin/webhook/eventos/config/1 \
  -H "Authorization: Bearer {jwt}" \
  -H "Content-Type: application/json" \
  -d '{"pushEnabled":true,"emailEnabled":false,"invalidarCache":false,"ativo":true}'
```

### Listar log com filtro
```bash
curl "https://api.bellory.com.br/api/v1/admin/webhook/eventos/log?eventType=ChargePaidEvent&page=0&size=10" \
  -H "Authorization: Bearer {jwt}"
```
