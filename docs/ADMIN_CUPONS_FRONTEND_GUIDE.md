# Admin Cupons — Guia de Integracao Frontend

## Visao Geral

O modulo de cupons do admin e um proxy para a Payment API. Nenhum dado e armazenado localmente — tudo vive na Payment API. O admin cria, edita, desativa e monitora cupons de desconto que sao aplicados automaticamente nas assinaturas e cobrancas dos clientes.

**Base URL:** `/api/v1/admin/cupons`
**Autenticacao:** JWT com role `PLATFORM_ADMIN`, `SUPERADMIN` ou `ADMIN`

---

## Endpoints

### 1. Listar Todos os Cupons

```
GET /api/v1/admin/cupons?page=0&size=20
```

**Query Params:**
| Param | Tipo | Default | Descricao |
|-------|------|---------|-----------|
| `page` | int | 0 | Pagina (base 0) |
| `size` | int | 20 | Itens por pagina |

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "companyId": 4,
      "code": "BEMVINDO20",
      "description": "20% de desconto para novos clientes",
      "discountType": "PERCENTAGE",
      "discountValue": 20.00,
      "scope": "SUBSCRIPTION",
      "applicationType": "FIRST_CHARGE",
      "recurrenceMonths": null,
      "validFrom": "2026-01-01T00:00:00",
      "validUntil": "2026-12-31T23:59:59",
      "maxUses": 100,
      "maxUsesPerCustomer": 1,
      "usageCount": 23,
      "allowedPlans": "basico,plus,premium",
      "allowedCustomers": null,
      "allowedCycle": null,
      "active": true,
      "currentlyValid": true,
      "createdAt": "2026-01-15T10:30:00",
      "updatedAt": "2026-03-20T14:15:00"
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### 2. Listar Cupons Ativos

```
GET /api/v1/admin/cupons/ativos?page=0&size=20
```

Retorna apenas cupons onde `active=true` E dentro da validade (`validFrom <= agora <= validUntil`).

---

### 3. Buscar Cupom por ID

```
GET /api/v1/admin/cupons/{id}
```

**Response 200:** Objeto `CouponResponse` completo.

---

### 4. Buscar Cupom por Codigo

```
GET /api/v1/admin/cupons/code/{code}
```

**Exemplo:** `GET /api/v1/admin/cupons/code/BEMVINDO20`

O codigo e case-insensitive (normalizado para UPPER internamente).

---

### 5. Criar Cupom

```
POST /api/v1/admin/cupons
Content-Type: application/json
```

**Request Body:**
```json
{
  "code": "PROMO50",
  "description": "Promocao de lancamento - 50% off primeiro mes",
  "discountType": "PERCENTAGE",
  "discountValue": 50,
  "scope": "SUBSCRIPTION",
  "applicationType": "FIRST_CHARGE",
  "validFrom": "2026-04-01T00:00:00",
  "validUntil": "2026-06-30T23:59:59",
  "maxUses": 200,
  "maxUsesPerCustomer": 1,
  "allowedPlans": "plus,premium",
  "allowedCycle": "MONTHLY"
}
```

**Response 201:** Objeto `CouponResponse` criado.

---

### 6. Atualizar Cupom

```
PUT /api/v1/admin/cupons/{id}
Content-Type: application/json
```

**Campos imutaveis (nao podem ser alterados):** `code`, `scope`, `discountType`

**Request Body (todos os campos sao opcionais):**
```json
{
  "description": "Descricao atualizada",
  "discountValue": 30,
  "validUntil": "2026-09-30T23:59:59",
  "maxUses": 500
}
```

**Response 200:** Objeto `CouponResponse` atualizado.

---

### 7. Desativar Cupom (Soft Delete)

```
DELETE /api/v1/admin/cupons/{id}
```

**Response 204:** Sem corpo. O cupom fica com `active=false`.

---

### 8. Reativar Cupom

```
PATCH /api/v1/admin/cupons/{id}/ativar
```

**Response 200:** Objeto `CouponResponse` com `active=true`.

---

### 9. Historico de Uso

```
GET /api/v1/admin/cupons/{id}/usos?page=0&size=50
```

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "couponId": 5,
      "couponCode": "BEMVINDO20",
      "customerId": 12,
      "subscriptionId": 58,
      "chargeId": null,
      "originalValue": 99.90,
      "discountAmount": 19.98,
      "finalValue": 79.92,
      "planCode": "plus",
      "cycle": "MONTHLY",
      "usedAt": "2026-03-15T14:30:00"
    }
  ],
  "totalElements": 23,
  "totalPages": 1,
  "number": 0,
  "size": 50
}
```

---

### 10. Validar Cupom

```
POST /api/v1/admin/cupons/validar
Content-Type: application/json
```

**Request Body:**
```json
{
  "couponCode": "PROMO50",
  "scope": "SUBSCRIPTION",
  "planCode": "premium",
  "cycle": "MONTHLY",
  "value": 199.90
}
```

**Response 200:**
```json
{
  "valid": true,
  "message": null,
  "discountType": "PERCENTAGE",
  "applicationType": "FIRST_CHARGE",
  "percentualDiscount": 50.00,
  "discountAmount": 99.95,
  "originalValue": 199.90,
  "finalValue": 99.95
}
```

**Cupom invalido:**
```json
{
  "valid": false,
  "message": "Cupom expirado",
  "discountType": null,
  "applicationType": null,
  "percentualDiscount": null,
  "discountAmount": null,
  "originalValue": null,
  "finalValue": null
}
```

---

## Campos do Cupom

### CouponResponse (leitura)

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `id` | Long | ID na Payment API |
| `code` | String | Codigo unico (ex: PROMO50) |
| `description` | String | Descricao livre |
| `discountType` | String | `PERCENTAGE` ou `FIXED_AMOUNT` |
| `discountValue` | BigDecimal | Valor do desconto (% ou R$) |
| `scope` | String | `SUBSCRIPTION` ou `CHARGE` |
| `applicationType` | String | `FIRST_CHARGE` ou `RECURRING` |
| `recurrenceMonths` | Integer | Meses de recorrencia (null = ilimitado) |
| `validFrom` | datetime | Inicio da validade |
| `validUntil` | datetime | Fim da validade |
| `maxUses` | Integer | Limite global de usos |
| `maxUsesPerCustomer` | Integer | Limite por cliente |
| `usageCount` | Integer | Total de usos ate agora |
| `allowedPlans` | String | Planos permitidos (CSV: "basico,plus") |
| `allowedCustomers` | String | Customers permitidos (CSV de IDs) |
| `allowedCycle` | String | Ciclo permitido (MONTHLY/QUARTERLY/YEARLY) |
| `active` | Boolean | Se esta ativo |
| `currentlyValid` | Boolean | Se esta valido agora (ativo + dentro da validade + usos < max) |
| `createdAt` | datetime | Data de criacao |
| `updatedAt` | datetime | Ultima atualizacao |

### CreateCouponRequest (criacao)

| Campo | Tipo | Obrigatorio | Regras |
|-------|------|-------------|--------|
| `code` | String | Sim | Apenas `A-Z`, `0-9`, `_`, `-`. Unico. |
| `description` | String | Nao | Descricao livre |
| `discountType` | String | Sim | `PERCENTAGE` ou `FIXED_AMOUNT` |
| `discountValue` | BigDecimal | Sim | Positivo. Max 90 se PERCENTAGE. |
| `scope` | String | Sim | `SUBSCRIPTION` ou `CHARGE` |
| `applicationType` | String | Nao | Default: `FIRST_CHARGE`. Ou `RECURRING`. |
| `recurrenceMonths` | Integer | Nao | So se RECURRING. null = ilimitado. |
| `validFrom` | datetime | Nao | Inicio da validade |
| `validUntil` | datetime | Nao | Fim da validade |
| `maxUses` | Integer | Nao | Limite global |
| `maxUsesPerCustomer` | Integer | Nao | Limite por cliente |
| `allowedPlans` | String | Nao | CSV: "basico,plus,premium" |
| `allowedCustomers` | String | Nao | CSV de IDs: "12,34,56" |
| `allowedCycle` | String | Nao | `MONTHLY`, `QUARTERLY` ou `YEARLY` |

### UpdateCouponRequest (edicao)

Mesmos campos do Create, **exceto** `code`, `scope` e `discountType` que sao **imutaveis**.

---

## Enums

### discountType
| Valor | Descricao | Exemplo |
|-------|-----------|---------|
| `PERCENTAGE` | Desconto percentual (max 90%) | 20% de R$ 100 = R$ 20 desconto |
| `FIXED_AMOUNT` | Valor fixo em R$ | R$ 30 de desconto em qualquer valor |

### scope
| Valor | Descricao |
|-------|-----------|
| `SUBSCRIPTION` | Aplicado na criacao/renovacao de assinatura |
| `CHARGE` | Aplicado em cobranças avulsas |

### applicationType
| Valor | Descricao |
|-------|-----------|
| `FIRST_CHARGE` | So aplica na primeira cobranca |
| `RECURRING` | Aplica por N meses (`recurrenceMonths`). null = para sempre. |

### allowedCycle
| Valor | Descricao |
|-------|-----------|
| `MONTHLY` | So para assinaturas mensais |
| `QUARTERLY` | So para assinaturas trimestrais |
| `YEARLY` | So para assinaturas anuais |

---

## Guia de Implementacao do Frontend

### Tela 1: Listagem de Cupons

**Rota sugerida:** `/admin/cupons`

**Comportamento:**
1. Carregar `GET /api/v1/admin/cupons?page=0&size=20`
2. Mostrar tabela com paginacao
3. Botao "Novo Cupom" abre formulario de criacao
4. Cada linha tem acoes: Ver, Editar, Desativar/Reativar

**Colunas da tabela:**

| Coluna | Campo | Formato |
|--------|-------|---------|
| Codigo | `code` | Badge/monospace |
| Desconto | `discountType` + `discountValue` | "20%" ou "R$ 30,00" |
| Escopo | `scope` | Badge: Assinatura / Cobranca |
| Validade | `validFrom` — `validUntil` | dd/MM/yyyy |
| Usos | `usageCount` / `maxUses` | "23/100" ou "23/∞" |
| Status | `active` + `currentlyValid` | Ver tabela abaixo |
| Acoes | — | Botoes |

**Status visual:**

| Condicao | Label | Cor |
|----------|-------|-----|
| `active=true` + `currentlyValid=true` | Ativo | Verde |
| `active=true` + `currentlyValid=false` | Expirado | Amarelo |
| `active=false` | Desativado | Cinza |

**Filtros sugeridos:**
- Toggle: "Mostrar todos" / "Apenas ativos" (alterna entre `/cupons` e `/cupons/ativos`)

---

### Tela 2: Criar/Editar Cupom

**Rota sugerida:** `/admin/cupons/novo` e `/admin/cupons/{id}/editar`

**Layout do formulario:**

```
┌─────────────────────────────────────────────────────────────┐
│ Novo Cupom de Desconto                                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Codigo *          [ PROMO50          ]  (so criacao)        │
│                   Apenas letras, numeros, _ e -             │
│                                                             │
│ Descricao         [ Promocao de lancamento           ]     │
│                                                             │
│ ── Desconto ──                                              │
│                                                             │
│ Tipo *            (●) Percentual  ( ) Valor fixo           │
│ Valor *           [ 50    ] %  (ou R$)                      │
│                   Maximo 90% para percentual                │
│                                                             │
│ ── Escopo ──                                                │
│                                                             │
│ Escopo *          (●) Assinatura  ( ) Cobranca avulsa      │
│                   (so criacao)                              │
│                                                             │
│ Aplicacao         (●) Primeira cobranca  ( ) Recorrente    │
│ Meses recorrentes [ 3  ] (so se recorrente, vazio=sempre)  │
│                                                             │
│ ── Validade ──                                              │
│                                                             │
│ De                [ 01/04/2026 ]                            │
│ Ate               [ 30/06/2026 ]                            │
│                                                             │
│ ── Limites ──                                               │
│                                                             │
│ Max usos (total)  [ 200 ]                                   │
│ Max usos/cliente  [ 1   ]                                   │
│                                                             │
│ ── Restricoes (opcional) ──                                 │
│                                                             │
│ Planos permitidos [ basico, plus, premium ] (tags/chips)   │
│ Ciclo permitido   [▼ Todos ] (dropdown: Todos/Mensal/...)  │
│                                                             │
│                        [ Cancelar ]  [ Salvar ]             │
└─────────────────────────────────────────────────────────────┘
```

**Regras do formulario:**
- `code`: so na criacao, read-only na edicao. Uppercase automatico.
- `scope` e `discountType`: so na criacao, read-only na edicao (imutaveis).
- `discountValue`: se PERCENTAGE, validar max 90.
- `recurrenceMonths`: so aparece se `applicationType=RECURRING`.
- `allowedPlans`: campo de tags/chips. Buscar planos via `GET /api/v1/planos` para sugestoes.

**Ao salvar:**
- Criacao: `POST /api/v1/admin/cupons`
- Edicao: `PUT /api/v1/admin/cupons/{id}`

---

### Tela 3: Detalhes do Cupom

**Rota sugerida:** `/admin/cupons/{id}`

**Comportamento:**
1. Carregar `GET /api/v1/admin/cupons/{id}`
2. Mostrar dados do cupom
3. Carregar `GET /api/v1/admin/cupons/{id}/usos?page=0&size=50`
4. Mostrar historico de uso em tabela

**Layout:**

```
┌─────────────────────────────────────────────────────────────┐
│ Cupom: PROMO50                          [ Editar ] [ ✕ ]   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Descricao:    Promocao de lancamento                        │
│ Desconto:     50% (Percentual)                              │
│ Escopo:       Assinatura                                    │
│ Aplicacao:    Primeira cobranca                              │
│ Validade:     01/04/2026 — 30/06/2026                       │
│ Usos:         23 / 200                      [████░░░] 11%  │
│ Status:       ● Ativo                                       │
│ Planos:       basico, plus, premium                         │
│ Ciclo:        Mensal                                        │
│                                                             │
│ ── Historico de Uso ──                                      │
│ ┌──────────┬────────┬──────────┬───────────┬───────────┐   │
│ │   Data   │Cliente │  Plano   │  Original │ Desconto  │   │
│ ├──────────┼────────┼──────────┼───────────┼───────────┤   │
│ │15/03/2026│ #12    │ plus     │ R$ 99,90  │ R$ 49,95  │   │
│ │18/03/2026│ #34    │ premium  │ R$ 199,90 │ R$ 99,95  │   │
│ │ ...      │        │          │           │           │   │
│ └──────────┴────────┴──────────┴───────────┴───────────┘   │
│                                                             │
│                  [ < 1 2 3 > ]                              │
└─────────────────────────────────────────────────────────────┘
```

**Colunas do historico de uso:**

| Coluna | Campo | Formato |
|--------|-------|---------|
| Data | `usedAt` | dd/MM/yyyy HH:mm |
| Cliente | `customerId` | ID ou link |
| Assinatura | `subscriptionId` | ID |
| Plano | `planCode` | Badge |
| Ciclo | `cycle` | Mensal/Trimestral/Anual |
| Original | `originalValue` | R$ |
| Desconto | `discountAmount` | R$ (vermelho) |
| Final | `finalValue` | R$ (verde) |

---

## Tratamento de Erros

Todos os endpoints retornam erros no formato:

```json
{
  "success": false,
  "message": "Payment API erro em POST /coupons (HTTP 409)",
  "details": "{\"error\":\"Coupon code already exists\"}"
}
```

**Codigos HTTP comuns:**

| Codigo | Significado | Quando |
|--------|-------------|--------|
| 201 | Criado | POST com sucesso |
| 200 | OK | GET/PUT/PATCH com sucesso |
| 204 | Sem conteudo | DELETE com sucesso |
| 400 | Bad Request | Campos invalidos |
| 404 | Nao encontrado | ID ou codigo inexistente |
| 409 | Conflito | Codigo de cupom ja existe |
| 422 | Nao processavel | Percentual > 90% |
| 502 | Bad Gateway | Payment API indisponivel |

**Mensagens sugeridas para o frontend:**

| Erro | Mensagem amigavel |
|------|--------------------|
| 409 | "Ja existe um cupom com este codigo" |
| 422 | "Desconto percentual nao pode ser maior que 90%" |
| 404 | "Cupom nao encontrado" |
| 502 | "Servico de pagamento indisponivel. Tente novamente." |

---

## Fluxos Comuns

### Criar cupom percentual para primeiro mes
```json
POST /api/v1/admin/cupons
{
  "code": "LANCAMENTO30",
  "description": "30% off no primeiro mes",
  "discountType": "PERCENTAGE",
  "discountValue": 30,
  "scope": "SUBSCRIPTION",
  "applicationType": "FIRST_CHARGE",
  "validFrom": "2026-04-01T00:00:00",
  "validUntil": "2026-12-31T23:59:59",
  "maxUses": 500,
  "maxUsesPerCustomer": 1
}
```

### Criar cupom de valor fixo recorrente por 3 meses
```json
POST /api/v1/admin/cupons
{
  "code": "FIXO50_3M",
  "description": "R$ 50 off por 3 meses",
  "discountType": "FIXED_AMOUNT",
  "discountValue": 50,
  "scope": "SUBSCRIPTION",
  "applicationType": "RECURRING",
  "recurrenceMonths": 3,
  "maxUsesPerCustomer": 1
}
```

### Criar cupom exclusivo para plano premium, ciclo anual
```json
POST /api/v1/admin/cupons
{
  "code": "VIP_ANUAL",
  "description": "Desconto especial para plano anual premium",
  "discountType": "PERCENTAGE",
  "discountValue": 25,
  "scope": "SUBSCRIPTION",
  "applicationType": "RECURRING",
  "allowedPlans": "premium",
  "allowedCycle": "YEARLY",
  "maxUses": 50
}
```

### Desativar e reativar cupom
```
DELETE /api/v1/admin/cupons/5        → 204 (desativado)
PATCH  /api/v1/admin/cupons/5/ativar → 200 (reativado)
```

### Testar se um cupom e valido antes de compartilhar
```json
POST /api/v1/admin/cupons/validar
{
  "couponCode": "LANCAMENTO30",
  "scope": "SUBSCRIPTION",
  "planCode": "plus",
  "cycle": "MONTHLY",
  "value": 99.90
}
```
