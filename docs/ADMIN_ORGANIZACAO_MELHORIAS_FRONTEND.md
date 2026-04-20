# Admin Organizacao — Melhorias do Modulo Frontend

## Situacao Atual

O endpoint `GET /api/v1/admin/organizacoes/{id}` retorna dados basicos da organizacao com metricas e instancias. Porem **muitos dados existentes nao sao expostos** e **dados da Payment API nao sao integrados**.

### O que ja e retornado (5 secoes atuais)
1. **Informacoes** — nome, CNPJ, email, telefones, slug, responsavel
2. **Metricas** — contadores de agendamentos, clientes, funcionarios, servicos, faturamento
3. **Plano** — campos existem no DTO mas estao **sempre null** (nao populados)
4. **Assinatura** — nao exposta
5. **Instancias** — lista de agentes virtuais

### O que esta escondido (existe na entidade mas nao no DTO)
- Endereco completo
- Redes sociais
- Logo e banner URLs
- Inscricao estadual
- Publico alvo
- Dados de faturamento (conta bancaria)
- ConfigSistema (flags de modulos)
- Tema/cores

---

## Proposta de Melhoria — Nova Estrutura de Abas

### Aba 1: Informacoes Gerais

**Dados do Bellory (ja existem na entidade):**

| Campo | Fonte | Status |
|-------|-------|--------|
| `nomeFantasia` | Organizacao | ✅ Ja exposto |
| `razaoSocial` | Organizacao | ✅ Ja exposto |
| `cnpj` | Organizacao | ✅ Ja exposto |
| `inscricaoEstadual` | Organizacao | ❌ Falta expor |
| `emailPrincipal` | Organizacao | ✅ Ja exposto |
| `telefone1`, `telefone2` | Organizacao | ✅ Ja exposto |
| `whatsapp` | Organizacao | ✅ Ja exposto |
| `slug` | Organizacao | ✅ Ja exposto |
| `publicoAlvo` | Organizacao | ❌ Falta expor |
| `logoUrl` | Organizacao | ❌ Falta expor |
| `bannerUrl` | Organizacao | ❌ Falta expor |
| `ativo` | Organizacao | ✅ Ja exposto |
| `dtCadastro` | Organizacao | ✅ Ja exposto |

**Responsavel:**

| Campo | Fonte | Status |
|-------|-------|--------|
| `responsavelNome` | Organizacao.responsavel | ✅ Ja exposto |
| `responsavelEmail` | Organizacao.responsavel | ✅ Ja exposto |
| `responsavelTelefone` | Organizacao.responsavel | ✅ Ja exposto |

**Endereco (NOVO — nao exposto hoje):**

| Campo | Fonte | Status |
|-------|-------|--------|
| `logradouro` | Organizacao.enderecoPrincipal | ❌ Falta expor |
| `numero` | Organizacao.enderecoPrincipal | ❌ Falta expor |
| `complemento` | Organizacao.enderecoPrincipal | ❌ Falta expor |
| `bairro` | Organizacao.enderecoPrincipal | ❌ Falta expor |
| `cidade` | Organizacao.enderecoPrincipal | ❌ Falta expor |
| `uf` | Organizacao.enderecoPrincipal | ❌ Falta expor |
| `cep` | Organizacao.enderecoPrincipal | ❌ Falta expor |

**Redes Sociais (NOVO — nao exposto hoje):**

| Campo | Fonte | Status |
|-------|-------|--------|
| `instagram` | Organizacao.redesSociais | ❌ Falta expor |
| `facebook` | Organizacao.redesSociais | ❌ Falta expor |
| `whatsapp` | Organizacao.redesSociais | ❌ Falta expor |
| `linkedin` | Organizacao.redesSociais | ❌ Falta expor |
| `youtube` | Organizacao.redesSociais | ❌ Falta expor |
| `site` | Organizacao.redesSociais | ❌ Falta expor |

**Layout sugerido:**

```
┌──────────────────────────────────────────────────────────────┐
│ Bellory Salon & Spa                    ● Ativa   [logo.png] │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ ── Dados da Empresa ──                                       │
│ Razao Social:   Bellory Salon & Spa LTDA                     │
│ CNPJ:           12.345.678/0001-95                           │
│ Inscr. Estadual: 110.042.490.114                             │
│ Slug:           bellory-salon                                │
│ Publico Alvo:   Feminino, 25-45 anos                         │
│ Cadastro:       15/01/2026                                   │
│                                                              │
│ ── Contato ──                                                │
│ Email:          contato@bellorysalon.com.br                  │
│ Telefone 1:     (11) 3000-1000                               │
│ Telefone 2:     (11) 3000-1001                               │
│ WhatsApp:       (11) 99000-1000                              │
│                                                              │
│ ── Responsavel ──                                            │
│ Nome:           Admin do Sistema                             │
│ Email:          admin@bellory.com                             │
│ Telefone:       (11) 99999-0001                              │
│                                                              │
│ ── Endereco ──                                               │
│ Rua Diomar Monteiro, 1509                                    │
│ Grama — Juiz de Fora/MG                                     │
│ CEP: 36048-310                                               │
│                                                              │
│ ── Redes Sociais ──                                          │
│ Instagram:  @bellorysalon                                    │
│ Facebook:   facebook.com/bellorysalon                        │
│ Site:       www.bellorysalon.com.br                          │
└──────────────────────────────────────────────────────────────┘
```

---

### Aba 2: Metricas

**Sem mudancas necessarias** — ja retorna dados completos.

| Metrica | Status |
|---------|--------|
| Total/mes agendamentos | ✅ |
| Agendamentos por status | ✅ |
| Total/ativos clientes | ✅ |
| Total/ativos funcionarios | ✅ |
| Total/ativos servicos | ✅ |
| Faturamento total/mes | ✅ |
| Cobrancas pagas/pendentes/vencidas | ✅ |

---

### Aba 3: Plano e Assinatura (NOVO — dados da Payment API)

**Dados vem da Payment API via customerId/subscriptionId da tabela Assinatura.**

**Bloco: Assinatura Atual**

| Campo | Fonte | Descricao |
|-------|-------|-----------|
| `planName` | SubscriptionResponse | Nome do plano atual |
| `effectivePrice` | SubscriptionResponse | Valor efetivo |
| `cycle` | SubscriptionResponse | Mensal/Semestral/Anual |
| `billingType` | SubscriptionResponse | PIX/Boleto/Cartao |
| `status` | SubscriptionResponse | Ativa/Pausada/Suspensa/Cancelada |
| `nextDueDate` | SubscriptionResponse | Proximo vencimento |
| `currentPeriodStart` / `End` | SubscriptionResponse | Periodo atual |
| `couponCode` | SubscriptionResponse | Cupom aplicado |
| `couponDiscountAmount` | SubscriptionResponse | Valor do desconto |

**Bloco: Status de Acesso**

| Campo | Fonte | Descricao |
|-------|-------|-----------|
| `allowed` | AccessStatusResponse | Acesso liberado/bloqueado |
| `reasons` | AccessStatusResponse | Motivos do bloqueio |
| `activeSubscriptions` | AccessSummary | Assinaturas ativas |
| `overdueCharges` | AccessSummary | Cobrancas atrasadas |
| `totalOverdueValue` | AccessSummary | Valor total atrasado |
| `creditBalance` | AccessSummary | Saldo de credito |

**Bloco: Detalhes do Plano**

| Campo | Fonte | Descricao |
|-------|-------|-----------|
| `name` | PlanResponse | Nome do plano |
| `codigo` | PlanResponse | Codigo do plano |
| `precoMensal` | PlanResponse | Preco mensal base |
| `precoAnual` | PlanResponse | Preco anual |
| `isFree` | PlanResponse | Se e gratuito |
| `limits` | PlanResponse.limits | Limites (NUMBER/BOOLEAN/UNLIMITED) |
| `features` | PlanResponse.features | Features (toggles) |

**Layout sugerido:**

```
┌──────────────────────────────────────────────────────────────┐
│ ── Assinatura ──                                             │
│                                                              │
│ Plano:        Plus (v2)              Status: ● Ativa         │
│ Valor:        R$ 99,90/mes           Pagamento: PIX          │
│ Vencimento:   01/05/2026             Periodo: 01/04 - 30/04  │
│ Cupom:        BEMVINDO20 (-R$ 19,98)                         │
│                                                              │
│ ── Acesso ──                                                 │
│ Status:       ● Liberado                                     │
│ Assin. ativas: 1   Cobr. atrasadas: 0   Credito: R$ 25,00   │
│                                                              │
│ ── Limites do Plano ──                                       │
│ ┌──────────────────┬──────────┬──────────┬─────────────────┐ │
│ │ Recurso          │ Limite   │ Uso Atual│    Disponivel   │ │
│ ├──────────────────┼──────────┼──────────┼─────────────────┤ │
│ │ Funcionarios     │    15    │     5    │  10 restantes   │ │
│ │ Clientes         │   500    │    42    │ 458 restantes   │ │
│ │ Servicos         │ Ilimitado│    30    │       —         │ │
│ │ Agendamentos/mes │  1.000   │    87    │ 913 restantes   │ │
│ └──────────────────┴──────────┴──────────┴─────────────────┘ │
│                                                              │
│ ── Features ──                                               │
│ Agendamento Online: ✅   Relatorios: ✅   Site: ✅           │
│ API: ❌   Agente Virtual: ❌                                 │
└──────────────────────────────────────────────────────────────┘
```

> **Nota:** O "Uso Atual" dos limites vem do endpoint `GET /api/v1/assinatura/uso` que ja existe no Bellory. Basta chamar passando o `organizacaoId`.

---

### Aba 4: Historico de Cobrancas (NOVO — dados da Payment API)

**Fonte:** `GET /api/v1/admin/assinaturas/customers/{customerId}` para dados do customer + `GET /api/v1/admin/assinaturas/{subscriptionId}/cobrancas` para cobrancas da assinatura + cobrancas avulsas do customer.

**Como obter o customerId:** A tabela `admin.assinatura` tem `payment_api_customer_id` vinculado ao `organizacao_id`. O backend deve incluir esse dado no DTO de detalhe.

**Colunas da tabela de cobrancas:**

| Coluna | Campo | Formato |
|--------|-------|---------|
| Data | `dueDate` | dd/MM/yyyy |
| Valor | `value` | R$ |
| Original | `originalValue` | R$ (se tem desconto) |
| Desconto | `discountAmount` | R$ |
| Forma | `billingType` | Badge: PIX/Boleto/Cartao |
| Status | `status` | Badge colorido |
| Origem | `origin` | RECURRING/PLAN_CHANGE/API |
| Cupom | `couponCode` | Badge ou "—" |
| Pagamento | `pixQrcode`/`boletoUrl`/`invoiceUrl` | Link/botao |

**Status das cobrancas:**

| Status | Cor | Icone |
|--------|-----|-------|
| `PENDING` | Amarelo | Relogio |
| `CONFIRMED` | Verde | Check |
| `RECEIVED` | Verde | Check duplo |
| `OVERDUE` | Vermelho | Alerta |
| `CANCELED` | Cinza | X |
| `REFUNDED` | Azul | Seta voltar |

**Layout sugerido:**

```
┌──────────────────────────────────────────────────────────────┐
│ ── Historico de Cobrancas ──                     Total: 24   │
│                                                              │
│ Resumo: 20 pagas · 2 pendentes · 1 atrasada · 1 cancelada   │
│                                                              │
│ ┌──────────┬─────────┬────────┬──────────┬────────┬────────┐│
│ │   Data   │  Valor  │ Forma  │  Status  │ Origem │  Cupom ││
│ ├──────────┼─────────┼────────┼──────────┼────────┼────────┤│
│ │01/05/2026│ R$99,90 │  PIX   │ PENDING  │RECURRING│  —    ││
│ │01/04/2026│ R$99,90 │  PIX   │CONFIRMED │RECURRING│  —    ││
│ │15/03/2026│ R$49,90 │  PIX   │CONFIRMED │PLAN_CHG │  —    ││
│ │01/03/2026│ R$79,92 │  PIX   │CONFIRMED │RECURRING│BV20   ││
│ │01/02/2026│ R$79,92 │  PIX   │CONFIRMED │RECURRING│BV20   ││
│ └──────────┴─────────┴────────┴──────────┴────────┴────────┘│
│                      [ < 1 2 3 > ]                           │
└──────────────────────────────────────────────────────────────┘
```

---

### Aba 5: Trocas de Plano (NOVO — dados da Payment API)

**Fonte:** `GET /api/v1/admin/assinaturas/{subscriptionId}/trocas-plano`

**Colunas:**

| Coluna | Campo | Formato |
|--------|-------|---------|
| Data | `requestedAt` | dd/MM/yyyy HH:mm |
| De | `previousPlanName` | Texto |
| Para | `requestedPlanName` | Texto |
| Tipo | `changeType` | Badge: UPGRADE/DOWNGRADE/SIDEGRADE |
| Valor | `deltaAmount` | R$ (+/-) |
| Status | `status` | Badge: COMPLETED/SCHEDULED/FAILED |
| Solicitado por | `requestedBy` | Texto |
| Acao | — | Cancelar (se SCHEDULED/PENDING) |

---

### Aba 6: Instancias (sem mudancas)

Ja retorna os dados necessarios.

---

### Aba 7: Configuracoes (NOVO — dados do Bellory)

**Fonte:** `ConfigSistema` da organizacao (nao exposto atualmente).

**Secoes:**

| Secao | Dados |
|-------|-------|
| Agendamento | tolerancia, min/max dias, cancelamento cliente, modo visualizacao |
| Servico | mostrar valor, unico servico, avaliacao |
| Cliente | cadastro obrigatorio, fidelidade, valor ponto |
| Colaborador | selecionar no agendamento, notas, comissao |
| Notificacao | confirmacao/lembrete WhatsApp, SMS, email, templates |
| Modulos | ecommerce, gestao produtos, planos clientes, push |

---

## Endpoints Necessarios no Backend

### Novos endpoints ou modificacoes

#### 1. Atualizar `GET /api/v1/admin/organizacoes/{id}`

O DTO `AdminOrganizacaoDetalheDTO` precisa ser enriquecido com:

**Campos locais (Bellory) que faltam:**

```java
// Adicionar ao DTO
private String inscricaoEstadual;
private String publicoAlvo;
private String logoUrl;
private String bannerUrl;

// Endereco
private EnderecoDTO endereco;  // { logradouro, numero, complemento, bairro, cidade, uf, cep }

// Redes Sociais
private RedesSociaisDTO redesSociais;  // { instagram, facebook, whatsapp, linkedin, youtube, site }
```

**Campos da Payment API que faltam:**

```java
// IDs de vinculo
private Long paymentApiCustomerId;
private Long paymentApiSubscriptionId;

// Assinatura ativa (da Payment API)
private SubscriptionResponse assinaturaAtiva;

// Status de acesso (da Payment API)
private AccessStatusResponse accessStatus;

// Plano detalhado (da Payment API) — substituir PlanoInfo por PlanResponse completo
private PlanResponse planoDetalhado;
```

#### 2. Novo endpoint: Cobrancas da organizacao

```
GET /api/v1/admin/organizacoes/{id}/cobrancas?page=0&size=20
```

Internamente: busca `Assinatura` pela org → usa `paymentApiCustomerId` → chama `listChargesByCustomer`.

**Response:** Paginado de `ChargeResponse`.

#### 3. Novo endpoint: Trocas de plano da organizacao

```
GET /api/v1/admin/organizacoes/{id}/trocas-plano?page=0&size=20
```

Internamente: busca `Assinatura` pela org → usa `paymentApiSubscriptionId` → chama `getPlanChangeHistory`.

**Response:** Paginado de `PlanChangeResponse`.

#### 4. Novo endpoint: Uso do plano da organizacao

```
GET /api/v1/admin/organizacoes/{id}/uso-plano
```

Internamente: chama `PlanoUsoService.getUso(organizacaoId)`.

**Response:** `PlanoUsoDTO` (ja existe).

---

## Resumo de Abas para o Frontend

| # | Aba | Dados | Fonte |
|---|-----|-------|-------|
| 1 | **Informacoes** | Empresa + contato + responsavel + endereco + redes sociais | Bellory DB |
| 2 | **Metricas** | Contadores e faturamento | Bellory DB (AdminQueryRepository) |
| 3 | **Plano e Assinatura** | Assinatura ativa + acesso + limites/uso + features | Payment API + Bellory |
| 4 | **Cobrancas** | Historico completo de cobrancas | Payment API |
| 5 | **Trocas de Plano** | Historico de upgrades/downgrades | Payment API |
| 6 | **Instancias** | Agentes virtuais (WhatsApp) | Bellory DB |
| 7 | **Configuracoes** | Flags de modulos e configs do sistema | Bellory DB |

---

## Fluxo de Dados por Aba

### Aba 1 — Informacoes
```
GET /api/v1/admin/organizacoes/{id}
→ Campos ja existentes + novos (endereco, redesSociais, logoUrl, etc.)
→ Tudo do Bellory DB, sem chamada a Payment API
```

### Aba 2 — Metricas
```
GET /api/v1/admin/organizacoes/{id}
→ Campo "metricas" ja existente
→ Tudo do Bellory DB
```

### Aba 3 — Plano e Assinatura
```
GET /api/v1/admin/organizacoes/{id}
→ Campos "assinaturaAtiva", "accessStatus", "planoDetalhado" (enriquecidos da Payment API)

GET /api/v1/admin/organizacoes/{id}/uso-plano
→ PlanoUsoDTO com limites vs uso atual
```

### Aba 4 — Cobrancas
```
GET /api/v1/admin/organizacoes/{id}/cobrancas?page=0&size=20
→ Lista paginada de ChargeResponse da Payment API
```

### Aba 5 — Trocas de Plano
```
GET /api/v1/admin/organizacoes/{id}/trocas-plano?page=0&size=20
→ Lista paginada de PlanChangeResponse da Payment API
```

### Aba 6 — Instancias
```
GET /api/v1/admin/organizacoes/{id}
→ Campo "instancias" ja existente
```

### Aba 7 — Configuracoes
```
GET /api/v1/admin/organizacoes/{id}
→ Campo "configSistema" (NOVO — precisa expor)
```

---

## Checklist de Implementacao Backend

| # | Tarefa | Tipo |
|---|--------|------|
| 1 | Adicionar `inscricaoEstadual`, `publicoAlvo`, `logoUrl`, `bannerUrl` ao DTO | Expor campo existente |
| 2 | Criar `EnderecoDTO` e popular no detalhe | Expor entidade existente |
| 3 | Criar `RedesSociaisDTO` e popular no detalhe | Expor embedded existente |
| 4 | Adicionar `paymentApiCustomerId`, `paymentApiSubscriptionId` ao DTO | Expor da tabela Assinatura |
| 5 | Enriquecer com `SubscriptionResponse` da Payment API (fail-safe) | Integracao Payment API |
| 6 | Enriquecer com `AccessStatusResponse` da Payment API (fail-safe) | Integracao Payment API |
| 7 | Enriquecer com `PlanResponse` da Payment API (fail-safe) | Integracao Payment API |
| 8 | Criar endpoint `GET /{id}/cobrancas` | Novo endpoint proxy |
| 9 | Criar endpoint `GET /{id}/trocas-plano` | Novo endpoint proxy |
| 10 | Criar endpoint `GET /{id}/uso-plano` | Novo endpoint proxy |
| 11 | Expor `ConfigSistema` no DTO | Expor entidade existente |

> **Importante:** Todas as chamadas a Payment API devem ser **fail-safe** (try-catch). Se a Payment API estiver fora, retornar os dados do Bellory normalmente com os campos da Payment API como `null`. Nunca bloquear a resposta por falha da Payment API.
