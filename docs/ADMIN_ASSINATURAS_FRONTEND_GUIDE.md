# Admin Assinaturas — Guia de Integracao Frontend

## Visao Geral

O modulo de assinaturas do admin e um proxy para a Payment API. O admin visualiza, pausa, retoma, cancela e gerencia assinaturas e customers. Inclui historico de cobrancas e trocas de plano.

**Base URL:** `/api/v1/admin/assinaturas`
**Autenticacao:** JWT com role `PLATFORM_ADMIN`, `SUPERADMIN` ou `ADMIN`

---

## Endpoints

### ASSINATURAS

#### 1. Listar Assinaturas

```
GET /api/v1/admin/assinaturas?page=0&size=20
```

**Query Params:**

| Param | Tipo | Default | Descricao |
|-------|------|---------|-----------|
| `status` | String | — | Filtrar: `ACTIVE`, `PAUSED`, `SUSPENDED`, `CANCELED`, `EXPIRED` |
| `customerId` | Long | — | Filtrar por customer |
| `page` | int | 0 | Pagina |
| `size` | int | 20 | Itens por pagina |

**Response 200:**
```json
{
  "content": [
    {
      "id": 58,
      "companyId": 4,
      "customerId": 12,
      "planId": 3,
      "planName": "Plus",
      "billingType": "PIX",
      "effectivePrice": 99.90,
      "cycle": "MONTHLY",
      "currentPeriodStart": "2026-04-01T00:00:00",
      "currentPeriodEnd": "2026-04-30T23:59:59",
      "nextDueDate": "2026-05-01",
      "status": "ACTIVE",
      "couponCode": "BEMVINDO20",
      "couponDiscountAmount": 19.98,
      "couponUsesRemaining": 0,
      "createdAt": "2026-01-15T10:30:00",
      "updatedAt": "2026-04-01T00:00:00"
    }
  ],
  "totalElements": 45,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

---

#### 2. Buscar Assinatura por ID

```
GET /api/v1/admin/assinaturas/{id}
```

**Response 200:** Objeto `SubscriptionResponse` completo.

---

#### 3. Atualizar Assinatura

```
PUT /api/v1/admin/assinaturas/{id}
Content-Type: application/json
```

**Request Body (todos opcionais):**
```json
{
  "billingType": "BOLETO",
  "nextDueDate": "2026-06-01",
  "description": "Assinatura corporativa",
  "externalReference": "org-3"
}
```

**Response 200:** Objeto atualizado.

---

#### 4. Cancelar Assinatura

```
DELETE /api/v1/admin/assinaturas/{id}
```

**Response 200:** Objeto com `status: "CANCELED"`.

---

#### 5. Pausar Assinatura

```
POST /api/v1/admin/assinaturas/{id}/pausar?confirmCouponRemoval=false
```

| Param | Tipo | Default | Descricao |
|-------|------|---------|-----------|
| `confirmCouponRemoval` | boolean | false | Se true, confirma remocao do cupom ao pausar |

**Regras:**
- Se a assinatura tem cupom e `confirmCouponRemoval=false`, retorna 400.
- O cupom e **removido permanentemente** ao pausar.

**Response 200:** Objeto com `status: "PAUSED"`.

---

#### 6. Retomar Assinatura

```
POST /api/v1/admin/assinaturas/{id}/retomar
```

So pode ser chamado se `status = PAUSED`. O cupom **nao** e restaurado.

**Response 200:** Objeto com `status: "ACTIVE"`.

---

#### 7. Atualizar Forma de Pagamento

```
PATCH /api/v1/admin/assinaturas/{id}/forma-pagamento
Content-Type: application/json
```

**PIX/Boleto:**
```json
{
  "billingType": "PIX"
}
```

**Cartao:**
```json
{
  "billingType": "CREDIT_CARD",
  "creditCard": {
    "holderName": "JOAO DA SILVA",
    "number": "5162306219378829",
    "expiryMonth": "12",
    "expiryYear": "2028",
    "ccv": "318"
  },
  "creditCardHolderInfo": {
    "name": "Joao da Silva",
    "email": "joao@email.com",
    "cpfCnpj": "24971563792",
    "postalCode": "01310100",
    "addressNumber": "100",
    "phone": "11999999999"
  }
}
```

**Response 200:** Objeto atualizado.

---

### COBRANCAS

#### 8. Listar Cobrancas da Assinatura

```
GET /api/v1/admin/assinaturas/{id}/cobrancas
```

**Response 200:** Lista de `ChargeResponse`:
```json
[
  {
    "id": 245,
    "customerId": 12,
    "subscriptionId": 58,
    "billingType": "PIX",
    "value": 99.90,
    "dueDate": "2026-05-01",
    "status": "PENDING",
    "origin": "RECURRING",
    "pixQrcode": "data:image/png;base64,...",
    "pixCopyPaste": "00020126...",
    "boletoUrl": null,
    "invoiceUrl": "https://...",
    "couponCode": "BEMVINDO20",
    "discountAmount": 19.98,
    "originalValue": 119.88,
    "createdAt": "2026-04-15T00:00:00"
  }
]
```

---

### TROCAS DE PLANO

#### 9. Historico de Trocas de Plano

```
GET /api/v1/admin/assinaturas/{id}/trocas-plano?page=0&size=20
```

**Response 200:**
```json
{
  "content": [
    {
      "id": 5,
      "subscriptionId": 58,
      "previousPlanId": 2,
      "previousPlanName": "Basico",
      "requestedPlanId": 3,
      "requestedPlanName": "Plus",
      "changeType": "UPGRADE",
      "policy": "IMMEDIATE_PRORATA",
      "deltaAmount": 49.90,
      "prorationCredit": 25.00,
      "prorationCharge": 74.90,
      "status": "COMPLETED",
      "chargeId": 250,
      "effectiveAt": "2026-03-15T14:30:00",
      "requestedBy": "admin",
      "requestedAt": "2026-03-15T14:30:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

#### 10. Cancelar Troca de Plano Pendente

```
DELETE /api/v1/admin/assinaturas/{subscriptionId}/trocas-plano/{changeId}
```

So funciona para trocas com status `PENDING` ou `SCHEDULED`. Trocas `COMPLETED` nao podem ser canceladas (retorna 409).

**Response 204:** Sem corpo.

---

### CUSTOMERS

#### 11. Listar Customers

```
GET /api/v1/admin/assinaturas/customers?search=bellory&page=0&size=20
```

| Param | Tipo | Descricao |
|-------|------|-----------|
| `search` | String | Busca por nome, email ou documento (case-insensitive) |
| `page` | int | Pagina |
| `size` | int | Itens por pagina |

**Response 200:**
```json
{
  "content": [
    {
      "id": 12,
      "companyId": 4,
      "name": "Bellory Salon & Spa LTDA",
      "document": "12345678000195",
      "email": "contato@bellorysalon.com.br",
      "phone": "(11) 3000-1000",
      "creditBalance": 25.00,
      "createdAt": "2026-01-15T10:30:00"
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

#### 12. Buscar Customer por ID

```
GET /api/v1/admin/assinaturas/customers/{id}
```

**Response 200:** Objeto `CustomerResponse` completo com endereco.

---

#### 13. Verificar Acesso do Customer

```
GET /api/v1/admin/assinaturas/customers/{id}/access-status
```

**Response 200:**
```json
{
  "customerId": 12,
  "customerName": "Bellory Salon & Spa LTDA",
  "allowed": true,
  "reasons": [],
  "customBlockMessage": null,
  "summary": {
    "activeSubscriptions": 1,
    "suspendedSubscriptions": 0,
    "overdueCharges": 0,
    "totalOverdueValue": 0.00,
    "oldestOverdueDays": 0,
    "creditBalance": 25.00
  },
  "checkedAt": "2026-04-19T15:30:00"
}
```

---

## Campos dos DTOs

### SubscriptionResponse

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `id` | Long | ID da assinatura |
| `customerId` | Long | ID do customer |
| `planId` | Long | ID do plano atual |
| `planName` | String | Nome do plano |
| `billingType` | String | `PIX`, `BOLETO`, `CREDIT_CARD`, `DEBIT_CARD`, `UNDEFINED` |
| `effectivePrice` | BigDecimal | Preco efetivo (com desconto de cupom se aplicavel) |
| `cycle` | String | `MONTHLY`, `SEMIANNUALLY`, `YEARLY` |
| `currentPeriodStart` | datetime | Inicio do periodo atual |
| `currentPeriodEnd` | datetime | Fim do periodo atual |
| `nextDueDate` | date | Proximo vencimento |
| `status` | String | `ACTIVE`, `PAUSED`, `SUSPENDED`, `CANCELED`, `EXPIRED` |
| `couponCode` | String | Codigo do cupom aplicado (null se nenhum) |
| `couponDiscountAmount` | BigDecimal | Valor do desconto do cupom |
| `couponUsesRemaining` | Integer | Usos restantes do cupom nesta assinatura |
| `createdAt` | datetime | Data de criacao |
| `updatedAt` | datetime | Ultima atualizacao |

### ChargeResponse

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `id` | Long | ID da cobranca |
| `customerId` | Long | ID do customer |
| `subscriptionId` | Long | ID da assinatura (null se avulsa) |
| `billingType` | String | Forma de pagamento |
| `value` | BigDecimal | Valor da cobranca |
| `dueDate` | date | Data de vencimento |
| `status` | String | `PENDING`, `CONFIRMED`, `RECEIVED`, `OVERDUE`, `CANCELED`, `REFUNDED` |
| `origin` | String | `API`, `RECURRING`, `PLAN_CHANGE` |
| `pixQrcode` | String | QR code PIX (base64 ou URL) |
| `pixCopyPaste` | String | Codigo copia-e-cola PIX |
| `boletoUrl` | String | URL do boleto |
| `invoiceUrl` | String | URL da fatura |
| `couponCode` | String | Cupom aplicado |
| `discountAmount` | BigDecimal | Desconto aplicado |
| `originalValue` | BigDecimal | Valor original (antes do desconto) |

### PlanChangeResponse

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `id` | Long | ID da troca |
| `subscriptionId` | Long | ID da assinatura |
| `previousPlanId` | Long | Plano anterior |
| `previousPlanName` | String | Nome do plano anterior |
| `requestedPlanId` | Long | Novo plano |
| `requestedPlanName` | String | Nome do novo plano |
| `changeType` | String | `UPGRADE`, `DOWNGRADE`, `SIDEGRADE` |
| `policy` | String | `IMMEDIATE_PRORATA`, `END_OF_CYCLE`, `IMMEDIATE_NO_PRORATA` |
| `deltaAmount` | BigDecimal | Diferenca de valor |
| `prorationCredit` | BigDecimal | Credito proporcional |
| `prorationCharge` | BigDecimal | Cobranca proporcional |
| `status` | String | `COMPLETED`, `SCHEDULED`, `FAILED`, `CANCELED` |
| `chargeId` | Long | ID da cobranca gerada (se houver) |
| `scheduledFor` | datetime | Agendado para (se END_OF_CYCLE) |
| `effectiveAt` | datetime | Quando foi efetivado |
| `requestedBy` | String | Quem solicitou |
| `requestedAt` | datetime | Quando solicitou |
| `failureReason` | String | Motivo da falha (se FAILED) |

---

## Guia de Implementacao do Frontend

### Tela 1: Listagem de Assinaturas

**Rota sugerida:** `/admin/assinaturas`

**Colunas da tabela:**

| Coluna | Campo | Formato |
|--------|-------|---------|
| Customer | `customerId` | Link para customer |
| Plano | `planName` | Badge |
| Valor | `effectivePrice` | R$ |
| Ciclo | `cycle` | Mensal/Semestral/Anual |
| Pagamento | `billingType` | Badge com icone |
| Vencimento | `nextDueDate` | dd/MM/yyyy |
| Status | `status` | Badge colorido |
| Cupom | `couponCode` | Badge ou "—" |
| Acoes | — | Menu dropdown |

**Status visual:**

| Status | Label | Cor | Acoes disponiveis |
|--------|-------|-----|-------------------|
| `ACTIVE` | Ativa | Verde | Pausar, Cancelar, Alterar pagamento |
| `PAUSED` | Pausada | Amarelo | Retomar, Cancelar |
| `SUSPENDED` | Suspensa | Laranja | Cancelar |
| `CANCELED` | Cancelada | Vermelho | — (somente leitura) |
| `EXPIRED` | Expirada | Cinza | — (somente leitura) |

**Filtros:**

| Filtro | Tipo | Opcoes |
|--------|------|--------|
| Status | Select | Todos, Ativa, Pausada, Suspensa, Cancelada, Expirada |
| Customer | Busca | Campo de busca que filtra por customerId |

---

### Tela 2: Detalhes da Assinatura

**Rota sugerida:** `/admin/assinaturas/{id}`

**Comportamento:**
1. Carregar `GET /api/v1/admin/assinaturas/{id}`
2. Carregar `GET /api/v1/admin/assinaturas/customers/{customerId}` (dados do customer)
3. Carregar `GET /api/v1/admin/assinaturas/{id}/cobrancas`
4. Carregar `GET /api/v1/admin/assinaturas/{id}/trocas-plano`

**Layout:**

```
┌──────────────────────────────────────────────────────────────┐
│ Assinatura #58                          ● Ativa             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ ── Dados ──                                                  │
│ Customer:    #12 - Bellory Salon & Spa LTDA                  │
│ Plano:       Plus (v2)                                       │
│ Valor:       R$ 99,90 /mes                                   │
│ Pagamento:   PIX                                             │
│ Vencimento:  01/05/2026                                      │
│ Periodo:     01/04/2026 — 30/04/2026                         │
│ Cupom:       BEMVINDO20 (-R$ 19,98, 0 usos restantes)       │
│                                                              │
│ [ Pausar ] [ Alterar Pagamento ] [ Cancelar ]               │
│                                                              │
│ ── Cobrancas ──                                              │
│ ┌───────────┬────────┬──────────┬─────────┬────────┐        │
│ │   Data    │ Valor  │  Forma   │ Status  │ Origem │        │
│ ├───────────┼────────┼──────────┼─────────┼────────┤        │
│ │01/05/2026 │ R$99,90│   PIX    │ PENDING │RECURRING│       │
│ │01/04/2026 │ R$99,90│   PIX    │CONFIRMED│RECURRING│       │
│ │15/03/2026 │ R$49,90│   PIX    │CONFIRMED│PLAN_CHANGE│     │
│ └───────────┴────────┴──────────┴─────────┴────────┘        │
│                                                              │
│ ── Trocas de Plano ──                                        │
│ ┌───────────┬──────────┬─────────┬───────┬─────────┐        │
│ │   Data    │ De → Para│  Tipo   │ Valor │ Status  │        │
│ ├───────────┼──────────┼─────────┼───────┼─────────┤        │
│ │15/03/2026 │Basico→Plus│UPGRADE │+R$49,90│COMPLETED│       │
│ └───────────┴──────────┴─────────┴───────┴─────────┘        │
└──────────────────────────────────────────────────────────────┘
```

---

### Tela 3: Listagem de Customers

**Rota sugerida:** `/admin/assinaturas/customers`

**Colunas:**

| Coluna | Campo | Formato |
|--------|-------|---------|
| ID | `id` | # |
| Nome | `name` | Texto |
| Documento | `document` | CPF/CNPJ formatado |
| Email | `email` | Texto |
| Telefone | `phone` | Texto |
| Credito | `creditBalance` | R$ |
| Criado em | `createdAt` | dd/MM/yyyy |
| Acoes | — | Ver detalhes, Ver acesso |

**Campo de busca:** Pesquisa por nome, email ou documento.

---

### Tela 4: Detalhes do Customer

**Rota sugerida:** `/admin/assinaturas/customers/{id}`

**Comportamento:**
1. `GET /api/v1/admin/assinaturas/customers/{id}` — dados do customer
2. `GET /api/v1/admin/assinaturas/customers/{id}/access-status` — status de acesso
3. `GET /api/v1/admin/assinaturas?customerId={id}` — assinaturas do customer

**Layout:**

```
┌──────────────────────────────────────────────────────────────┐
│ Customer #12                                                 │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ Nome:       Bellory Salon & Spa LTDA                          │
│ CNPJ:       12.345.678/0001-95                                │
│ Email:      contato@bellorysalon.com.br                       │
│ Telefone:   (11) 3000-1000                                    │
│ Credito:    R$ 25,00                                          │
│                                                              │
│ ── Status de Acesso ──                                       │
│ Acesso:         ● Liberado                                    │
│ Assin. ativas:  1                                             │
│ Assin. suspensas: 0                                           │
│ Cobr. atrasadas:  0                                           │
│                                                              │
│ ── Assinaturas ──                                            │
│ (tabela com assinaturas do customer)                          │
└──────────────────────────────────────────────────────────────┘
```

---

## Status de Cobranca

| Status | Label | Cor |
|--------|-------|-----|
| `PENDING` | Pendente | Amarelo |
| `CONFIRMED` | Confirmado | Verde |
| `RECEIVED` | Recebido | Verde |
| `OVERDUE` | Atrasado | Vermelho |
| `CANCELED` | Cancelado | Cinza |
| `REFUNDED` | Estornado | Azul |

## Origem da Cobranca

| Origin | Label | Descricao |
|--------|-------|-----------|
| `RECURRING` | Recorrente | Cobranca automatica da assinatura |
| `PLAN_CHANGE` | Troca de plano | Pro-rata de upgrade |
| `API` | Manual | Criada via API |

## Status de Troca de Plano

| Status | Label | Cor | Acao |
|--------|-------|-----|------|
| `COMPLETED` | Efetivada | Verde | — |
| `SCHEDULED` | Agendada | Azul | Pode cancelar |
| `PENDING` | Pendente | Amarelo | Pode cancelar |
| `FAILED` | Falhou | Vermelho | — |
| `CANCELED` | Cancelada | Cinza | — |

---

## Tratamento de Erros

| Codigo | Significado | Quando |
|--------|-------------|--------|
| 200 | OK | GET/PUT/PATCH/POST/DELETE com sucesso |
| 204 | Sem conteudo | Cancelar troca de plano |
| 400 | Bad Request | Assinatura cancelada, cupom sem confirmacao |
| 404 | Nao encontrado | ID inexistente |
| 409 | Conflito | Cancelar troca ja completada |
| 502 | Bad Gateway | Payment API indisponivel |

**Mensagens sugeridas:**

| Erro | Mensagem |
|------|----------|
| 400 (cancelada) | "Nao e possivel alterar uma assinatura cancelada." |
| 400 (cupom) | "Esta assinatura possui cupom. Confirme a remocao para pausar." |
| 409 | "Esta troca de plano ja foi efetivada e nao pode ser cancelada." |

---

## Fluxos Comuns

### Pausar assinatura com cupom
```
POST /api/v1/admin/assinaturas/58/pausar?confirmCouponRemoval=true
```
Frontend deve mostrar modal de confirmacao: "O cupom BEMVINDO20 sera removido permanentemente ao pausar. Deseja continuar?"

### Retomar assinatura pausada
```
POST /api/v1/admin/assinaturas/58/retomar
```

### Alterar para PIX
```json
PATCH /api/v1/admin/assinaturas/58/forma-pagamento
{ "billingType": "PIX" }
```

### Cancelar troca agendada
```
DELETE /api/v1/admin/assinaturas/58/trocas-plano/5
```
