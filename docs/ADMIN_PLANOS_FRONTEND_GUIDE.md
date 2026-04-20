# Admin Planos — Guia de Integracao Frontend

## Visao Geral

O modulo de planos do admin e um proxy para a Payment API. Nenhum dado e armazenado localmente. O admin cria, edita, versiona, ativa/desativa planos e gerencia limites, features e promocoes.

**Base URL:** `/api/v1/admin/planos`
**Autenticacao:** JWT com role `PLATFORM_ADMIN`, `SUPERADMIN` ou `ADMIN`

---

## Endpoints

### 1. Listar Planos

```
GET /api/v1/admin/planos
```

**Response 200:** Lista de `PlanResponse` (ordenados por `tierOrder` ascendente).

```json
[
  {
    "id": 1,
    "companyId": 4,
    "name": "Gratuito",
    "description": "Plano gratuito com recursos basicos",
    "codigo": "gratuito",
    "precoMensal": 0.00,
    "precoAnual": 0.00,
    "precoSemestral": 0.00,
    "descontoPercentualAnual": null,
    "promoMensalAtiva": false,
    "promoAnualAtiva": false,
    "active": true,
    "version": 1,
    "trialDays": 0,
    "setupFee": 0.00,
    "tierOrder": 0,
    "isFree": true,
    "limits": [...],
    "features": [...],
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  },
  {
    "id": 2,
    "name": "Basico",
    "codigo": "basico",
    "precoMensal": 49.90,
    "precoAnual": 478.80,
    "descontoPercentualAnual": 20,
    "promoMensalAtiva": true,
    "promoMensalPreco": 29.90,
    "promoMensalTexto": "Lancamento: 40% off!",
    "promoMensalInicio": "2026-04-01T00:00:00",
    "promoMensalFim": "2026-06-30T23:59:59",
    "active": true,
    "version": 1,
    "tierOrder": 1,
    "isFree": false,
    "limits": [
      { "key": "funcionario", "label": "Funcionarios", "type": "NUMBER", "value": 5 },
      { "key": "cliente", "label": "Clientes", "type": "NUMBER", "value": 100 }
    ],
    "features": [
      { "key": "agendamento_online", "label": "Agendamento Online", "type": "BOOLEAN", "enabled": true },
      { "key": "relatorios", "label": "Relatorios", "type": "BOOLEAN", "enabled": false }
    ]
  }
]
```

---

### 2. Buscar Plano por ID

```
GET /api/v1/admin/planos/{id}
```

**Response 200:** Objeto `PlanResponse` completo.

---

### 3. Buscar Plano por Codigo

```
GET /api/v1/admin/planos/codigo/{codigo}
```

**Exemplo:** `GET /api/v1/admin/planos/codigo/premium`

---

### 4. Criar Plano

```
POST /api/v1/admin/planos
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Plus",
  "description": "Plano intermediario com recursos avancados",
  "codigo": "plus",
  "precoMensal": 99.90,
  "precoAnual": 958.80,
  "descontoPercentualAnual": 20,
  "trialDays": 7,
  "tierOrder": 2,
  "isFree": false,
  "limits": [
    { "key": "funcionario", "label": "Funcionarios", "type": "NUMBER", "value": 15 },
    { "key": "cliente", "label": "Clientes", "type": "NUMBER", "value": 500 },
    { "key": "servicos", "label": "Servicos", "type": "UNLIMITED" },
    { "key": "agendamento", "label": "Agendamentos/mes", "type": "NUMBER", "value": 1000 }
  ],
  "features": [
    { "key": "agendamento_online", "label": "Agendamento Online", "type": "BOOLEAN", "enabled": true },
    { "key": "relatorios", "label": "Relatorios", "type": "BOOLEAN", "enabled": true },
    { "key": "site_externo", "label": "Site Externo", "type": "BOOLEAN", "enabled": true },
    { "key": "api", "label": "API", "type": "BOOLEAN", "enabled": false }
  ]
}
```

**Response 201:** Objeto `PlanResponse` criado.

---

### 5. Atualizar Plano

```
PUT /api/v1/admin/planos/{id}
Content-Type: application/json
```

**Campo imutavel:** `codigo` (nao pode ser alterado).

**Request Body (todos opcionais):**
```json
{
  "name": "Plus Pro",
  "precoMensal": 119.90,
  "promoMensalAtiva": true,
  "promoMensalPreco": 89.90,
  "promoMensalTexto": "Promocao de lancamento!",
  "promoMensalInicio": "2026-05-01T00:00:00",
  "promoMensalFim": "2026-07-31T23:59:59"
}
```

**Response 200:** Objeto atualizado.

---

### 6. Criar Nova Versao

```
POST /api/v1/admin/planos/{id}/nova-versao
Content-Type: application/json
```

Cria uma nova versao do plano mantendo o mesmo `codigo`. A versao anterior e desativada automaticamente. Assinaturas existentes continuam na versao antiga.

**Quando usar:** Mudancas de preco que nao devem afetar assinaturas existentes.

**Request Body:** Mesmo formato do `PUT` (UpdatePlanRequest).

```json
{
  "precoMensal": 149.90,
  "precoAnual": 1438.80
}
```

**Response 201:** Nova versao com `version` incrementado.

---

### 7. Ativar Plano

```
PATCH /api/v1/admin/planos/{id}/ativar
```

**Response 200:** Plano com `active=true`.

---

### 8. Desativar Plano

```
PATCH /api/v1/admin/planos/{id}/desativar
```

Impede novas assinaturas. Assinaturas existentes continuam funcionando.

**Response 200:** Plano com `active=false`.

---

### 9. Deletar Plano

```
DELETE /api/v1/admin/planos/{id}
```

**Bloqueado** se o plano tiver assinaturas ativas (retorna 400).

**Response 204:** Sem corpo.

---

### 10. Pricing (precos efetivos com promocoes)

```
GET /api/v1/admin/planos/{id}/pricing
```

**Response 200:**
```json
{
  "id": 2,
  "codigo": "basico",
  "name": "Basico",
  "precoMensal": 29.90,
  "precoSemestral": 179.40,
  "precoAnual": 478.80,
  "descontoPercentualAnual": 20,
  "promoMensal": {
    "ativa": true,
    "preco": 29.90,
    "texto": "Lancamento: 40% off!",
    "validaAte": "2026-06-30T23:59:59"
  },
  "promoAnual": {
    "ativa": false,
    "preco": null,
    "texto": null,
    "validaAte": null
  },
  "limits": [...],
  "features": [...]
}
```

---

### 11. Limites do Plano

```
GET /api/v1/admin/planos/{id}/limites
```

**Response 200:**
```json
[
  { "key": "funcionario", "label": "Funcionarios", "type": "NUMBER", "value": 15 },
  { "key": "cliente", "label": "Clientes", "type": "NUMBER", "value": 500 },
  { "key": "servicos", "label": "Servicos", "type": "UNLIMITED" },
  { "key": "relatorios", "label": "Relatorios", "type": "BOOLEAN", "enabled": true }
]
```

---

### 12. Features do Plano

```
GET /api/v1/admin/planos/{id}/features
```

Mesmo formato dos limites.

---

### 13. Verificar Limite Especifico

```
GET /api/v1/admin/planos/{id}/limites/{key}?usage=26
```

**Response 200:**
```json
{
  "key": "funcionario",
  "label": "Funcionarios",
  "type": "NUMBER",
  "value": 25,
  "unlimited": false,
  "found": true,
  "currentUsage": 26,
  "allowed": false
}
```

---

## Campos do Plano

### PlanResponse (leitura)

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `id` | Long | ID na Payment API |
| `name` | String | Nome do plano |
| `description` | String | Descricao |
| `codigo` | String | Slug unico e imutavel (ex: `plus`) |
| `precoMensal` | BigDecimal | Preco mensal base |
| `precoAnual` | BigDecimal | Preco anual base |
| `precoSemestral` | BigDecimal | Preco semestral (6x mensal) |
| `descontoPercentualAnual` | BigDecimal | Desconto anual (%) |
| `promoMensalAtiva` | Boolean | Promo mensal ativa |
| `promoMensalPreco` | BigDecimal | Preco da promo mensal |
| `promoMensalTexto` | String | Texto da promo mensal |
| `promoMensalInicio` | datetime | Inicio da promo mensal |
| `promoMensalFim` | datetime | Fim da promo mensal |
| `promoAnualAtiva` | Boolean | Promo anual ativa |
| `promoAnualPreco` | BigDecimal | Preco da promo anual |
| `promoAnualTexto` | String | Texto da promo anual |
| `promoAnualInicio` | datetime | Inicio da promo anual |
| `promoAnualFim` | datetime | Fim da promo anual |
| `active` | Boolean | Se esta ativo |
| `version` | Integer | Versao do plano |
| `trialDays` | Integer | Dias de trial |
| `setupFee` | BigDecimal | Taxa de setup |
| `tierOrder` | Integer | Ordem de exibicao |
| `isFree` | Boolean | Se e gratuito |
| `limits` | List | Limites estruturados |
| `features` | List | Features estruturadas |

### CreatePlanRequest (criacao)

| Campo | Tipo | Obrigatorio | Regras |
|-------|------|-------------|--------|
| `name` | String | Sim | Nome do plano |
| `codigo` | String | Sim | Apenas `a-z`, `0-9`, `-`. Unico. Imutavel. |
| `precoMensal` | BigDecimal | Sim | Positivo |
| `description` | String | Nao | |
| `precoAnual` | BigDecimal | Nao | Validado ±5% da margem anualizada |
| `precoSemestral` | BigDecimal | Nao | Calculado automaticamente se omitido |
| `descontoPercentualAnual` | BigDecimal | Nao | |
| `promoMensalAtiva` | Boolean | Nao | Se true, preco/texto/inicio/fim obrigatorios |
| `promoMensalPreco` | BigDecimal | Cond. | Menor que `precoMensal` |
| `promoMensalTexto` | String | Cond. | |
| `promoMensalInicio` | datetime | Cond. | |
| `promoMensalFim` | datetime | Cond. | Apos `promoMensalInicio` |
| `promoAnualAtiva` | Boolean | Nao | Mesmas regras da promo mensal |
| `trialDays` | Integer | Nao | Default: 0 |
| `setupFee` | BigDecimal | Nao | Default: 0 |
| `tierOrder` | Integer | Nao | Ordem na exibicao |
| `isFree` | Boolean | Nao | |
| `limits` | List | Nao | Array de PlanLimitDto |
| `features` | List | Nao | Array de PlanLimitDto |

### PlanLimitDto (limite/feature)

| Campo | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| `key` | String | Sim | Slug snake_case (ex: `funcionario`) |
| `label` | String | Sim | Texto exibido (ex: "Funcionarios") |
| `type` | String | Sim | `NUMBER`, `BOOLEAN` ou `UNLIMITED` |
| `value` | Long | Se NUMBER | Quantidade maxima |
| `enabled` | Boolean | Se BOOLEAN | Habilitado/desabilitado |

---

## Guia de Implementacao do Frontend

### Tela 1: Listagem de Planos

**Rota sugerida:** `/admin/planos`

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│ Planos                                         [ + Novo Plano ] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ ┌────────┬──────────┬──────────┬──────────┬────────┬─────────┐ │
│ │  Ordem │   Nome   │  Codigo  │  Mensal  │ Status │  Acoes  │ │
│ ├────────┼──────────┼──────────┼──────────┼────────┼─────────┤ │
│ │   0    │Gratuito  │ gratuito │ R$ 0,00  │ ● Ativo│ ⚙ 👁 🗑│ │
│ │   1    │ Basico   │ basico   │ R$ 49,90 │ ● Ativo│ ⚙ 👁 🗑│ │
│ │   2    │  Plus    │ plus     │ R$ 99,90 │ ● Ativo│ ⚙ 👁 🗑│ │
│ │   3    │ Premium  │ premium  │ R$199,90 │ ○ Inativo│ ⚙ 👁│ │
│ └────────┴──────────┴──────────┴──────────┴────────┴─────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

**Colunas:**

| Coluna | Campo | Formato |
|--------|-------|---------|
| Ordem | `tierOrder` | Numero |
| Nome | `name` | Texto + badge se `isFree` |
| Codigo | `codigo` | Monospace |
| Mensal | `precoMensal` | R$ + badge promo se `promoMensalAtiva` |
| Anual | `precoAnual` | R$ |
| Versao | `version` | v1, v2... |
| Status | `active` | Badge verde/cinza |
| Acoes | — | Editar, Ver, Ativar/Desativar, Deletar |

---

### Tela 2: Criar/Editar Plano

**Rota sugerida:** `/admin/planos/novo` e `/admin/planos/{id}/editar`

**Secoes do formulario:**

#### Dados Basicos
```
Nome *              [ Plus                      ]
Codigo *            [ plus                      ] (so criacao, read-only na edicao)
Descricao           [ Plano intermediario...    ]
Gratuito            [ ] (checkbox)
Ordem exibicao      [ 2 ]
```

#### Precos
```
Mensal *            [ 99,90  ] R$
Semestral           [ 539,40 ] R$ (sugestao: 6x mensal)
Anual               [ 958,80 ] R$
Desconto anual (%)  [ 20     ] %
Trial (dias)        [ 7      ]
Taxa setup          [ 0,00   ] R$
```

#### Promocao Mensal
```
Ativar promo mensal [ ] (toggle)
  Preco promo       [ 49,90  ] R$ (menor que mensal)
  Texto             [ 50% off no primeiro mes   ]
  Inicio            [ 01/05/2026 ]
  Fim               [ 31/07/2026 ]
```

#### Promocao Anual
```
Ativar promo anual  [ ] (toggle)
  (mesmos campos)
```

#### Limites
```
[ + Adicionar Limite ]

┌──────────────┬──────────────────┬──────────┬────────┐
│     Key      │      Label       │   Tipo   │ Valor  │
├──────────────┼──────────────────┼──────────┼────────┤
│ funcionario  │ Funcionarios     │ NUMBER   │ 15     │
│ cliente      │ Clientes         │ NUMBER   │ 500    │
│ servicos     │ Servicos         │UNLIMITED │ —      │
│ agendamento  │ Agendamentos/mes │ NUMBER   │ 1000   │
│ [x remover]  │                  │          │        │
└──────────────┴──────────────────┴──────────┴────────┘
```

#### Features
```
[ + Adicionar Feature ]

┌──────────────────┬──────────────────────┬─────────┬────────┐
│       Key        │        Label         │  Tipo   │  On/Off│
├──────────────────┼──────────────────────┼─────────┼────────┤
│agendamento_online│ Agendamento Online   │ BOOLEAN │  ●     │
│ relatorios       │ Relatorios           │ BOOLEAN │  ●     │
│ site_externo     │ Site Externo         │ BOOLEAN │  ●     │
│ api              │ API                  │ BOOLEAN │  ○     │
│ agente_virtual   │ Agente Virtual       │ BOOLEAN │  ○     │
└──────────────────┴──────────────────────┴─────────┴────────┘
```

**Keys conhecidas (sugestoes para autocomplete):**

| Key | Label sugerido | Tipo usual |
|-----|---------------|------------|
| `funcionario` | Funcionarios | NUMBER |
| `cliente` | Clientes | NUMBER |
| `servicos` | Servicos | NUMBER/UNLIMITED |
| `agendamento` | Agendamentos/mes | NUMBER/UNLIMITED |
| `agente_virtual` | Agente Virtual | BOOLEAN/NUMBER |
| `site_externo` | Site Externo | BOOLEAN |
| `api` | API | BOOLEAN |
| `relatorios` | Relatorios | BOOLEAN |
| `arquivos` | Arquivos | NUMBER |
| `unidade` | Unidades/Filiais | NUMBER |

---

### Tela 3: Detalhes do Plano

**Rota sugerida:** `/admin/planos/{id}`

**Comportamento:**
1. Carregar `GET /api/v1/admin/planos/{id}`
2. Carregar `GET /api/v1/admin/planos/{id}/pricing` (precos efetivos)
3. Carregar `GET /api/v1/admin/planos/{id}/limites`
4. Carregar `GET /api/v1/admin/planos/{id}/features`

**Layout:**

```
┌──────────────────────────────────────────────────────────────┐
│ Plus (v2)                   ● Ativo    [ Editar ] [ ⚙ ]    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ Codigo:     plus                                             │
│ Descricao:  Plano intermediario com recursos avancados       │
│ Trial:      7 dias                                           │
│                                                              │
│ ── Precos ──                                                 │
│ Mensal:     R$ 99,90                                         │
│ Semestral:  R$ 539,40                                        │
│ Anual:      R$ 958,80 (20% desconto)                         │
│                                                              │
│ ── Promo Mensal ── 🏷                                       │
│ Status:     ● Ativa ate 30/06/2026                           │
│ Preco:      R$ 49,90 (50% off)                               │
│ Texto:      "Lancamento: 50% off!"                           │
│                                                              │
│ ── Limites ──                                                │
│ Funcionarios:       15                                       │
│ Clientes:           500                                      │
│ Servicos:           Ilimitado                                │
│ Agendamentos/mes:   1.000                                    │
│                                                              │
│ ── Features ──                                               │
│ Agendamento Online: ✅                                       │
│ Relatorios:         ✅                                       │
│ Site Externo:       ✅                                       │
│ API:                ❌                                       │
│ Agente Virtual:     ❌                                       │
│                                                              │
│ [ Editar ] [ Nova Versao ] [ Desativar ] [ Deletar ]        │
└──────────────────────────────────────────────────────────────┘
```

---

## Versionamento de Planos

### Quando usar "Editar" vs "Nova Versao"

| Acao | Quando usar | O que acontece |
|------|-------------|---------------|
| **Editar** (`PUT`) | Corrigir descricao, nome, promo | Altera o plano in-place. Afeta assinaturas existentes. |
| **Nova Versao** (`POST /nova-versao`) | Mudar precos | Cria novo plano (v2) com mesmo codigo. Versao anterior desativada. Assinaturas existentes ficam na v1. |

**Fluxo de nova versao no frontend:**
1. Admin clica "Nova Versao" no plano `plus` (v1, R$ 99,90)
2. Formulario pre-preenchido com dados atuais
3. Admin altera preco para R$ 119,90
4. `POST /api/v1/admin/planos/{id}/nova-versao` com `{ "precoMensal": 119.90 }`
5. Resultado: `plus` v1 desativado, `plus` v2 ativo com R$ 119,90
6. Novas assinaturas pagam R$ 119,90, existentes continuam em R$ 99,90

---

## Tratamento de Erros

| Codigo | Significado | Quando |
|--------|-------------|--------|
| 201 | Criado | POST com sucesso |
| 200 | OK | GET/PUT/PATCH com sucesso |
| 204 | Sem conteudo | DELETE com sucesso |
| 400 | Bad Request | Codigo duplicado, validacao de precos, promo invalida, plano com assinaturas ativas (delete) |
| 404 | Nao encontrado | ID ou codigo inexistente |
| 409 | Conflito | Codigo ja existe |
| 502 | Bad Gateway | Payment API indisponivel |

**Mensagens sugeridas:**

| Erro | Mensagem |
|------|----------|
| 400 (assinaturas ativas) | "Este plano possui assinaturas ativas e nao pode ser deletado. Desative-o primeiro." |
| 400 (promo preco) | "O preco promocional deve ser menor que o preco base." |
| 409 | "Ja existe um plano com o codigo informado." |

---

## Fluxos Comuns

### Criar plano com limites e features
```json
POST /api/v1/admin/planos
{
  "name": "Premium",
  "codigo": "premium",
  "precoMensal": 199.90,
  "precoAnual": 1918.80,
  "descontoPercentualAnual": 20,
  "tierOrder": 3,
  "isFree": false,
  "limits": [
    { "key": "funcionario", "label": "Funcionarios", "type": "UNLIMITED" },
    { "key": "cliente", "label": "Clientes", "type": "UNLIMITED" },
    { "key": "servicos", "label": "Servicos", "type": "UNLIMITED" },
    { "key": "agendamento", "label": "Agendamentos", "type": "UNLIMITED" }
  ],
  "features": [
    { "key": "agendamento_online", "label": "Agendamento Online", "type": "BOOLEAN", "enabled": true },
    { "key": "relatorios", "label": "Relatorios", "type": "BOOLEAN", "enabled": true },
    { "key": "site_externo", "label": "Site Externo", "type": "BOOLEAN", "enabled": true },
    { "key": "api", "label": "API", "type": "BOOLEAN", "enabled": true },
    { "key": "agente_virtual", "label": "Agente Virtual", "type": "BOOLEAN", "enabled": true }
  ]
}
```

### Adicionar promocao a um plano existente
```json
PUT /api/v1/admin/planos/2
{
  "promoMensalAtiva": true,
  "promoMensalPreco": 24.90,
  "promoMensalTexto": "Black Friday: 50% off!",
  "promoMensalInicio": "2026-11-20T00:00:00",
  "promoMensalFim": "2026-11-30T23:59:59"
}
```

### Reajustar preco sem afetar assinaturas
```json
POST /api/v1/admin/planos/2/nova-versao
{
  "precoMensal": 59.90,
  "precoAnual": 574.80
}
```

### Desativar e reativar
```
PATCH /api/v1/admin/planos/5/desativar  → 200 (active=false)
PATCH /api/v1/admin/planos/5/ativar     → 200 (active=true)
```
