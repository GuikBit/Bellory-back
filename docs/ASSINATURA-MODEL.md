# Modelo de Assinatura - Bellory API

## Visao Geral

O sistema de assinatura gerencia o ciclo de vida completo da relacao comercial entre a plataforma Bellory e as organizacoes (saloes/barbearias). Utiliza o **Asaas** como gateway de pagamento para cobranças recorrentes.

**Relacao:** `Organizacao` 1:1 `Assinatura` (cada org tem exatamente uma assinatura).

---

## Entidades

### 1. `Assinatura` (tabela: `admin.assinatura`)

Entidade central. Vincula uma organizacao a um plano.

| Campo | Tipo | Descricao |
|---|---|---|
| `id` | Long | PK |
| `organizacao` | Organizacao | FK OneToOne - a org dona da assinatura |
| `planoBellory` | PlanoBellory | FK ManyToOne - plano atual |
| `status` | StatusAssinatura | Estado atual (TRIAL, ATIVA, VENCIDA, CANCELADA, SUSPENSA) |
| `cicloCobranca` | CicloCobranca | MENSAL ou ANUAL |
| `dtInicioTrial` | LocalDateTime | Quando o trial comecou |
| `dtFimTrial` | LocalDateTime | Quando o trial expira |
| `dtTrialNotificado` | LocalDateTime | Se ja enviou email de trial expirando |
| `dtInicio` | LocalDateTime | Inicio da assinatura paga |
| `dtProximoVencimento` | LocalDateTime | Proxima data de vencimento |
| `dtCancelamento` | LocalDateTime | Data do cancelamento |
| `valorMensal` / `valorAnual` | BigDecimal | Valores do plano na assinatura |
| `formaPagamento` | FormaPagamentoPlataforma | PIX, BOLETO, CARTAO_CREDITO |
| `cupom` / `valorDesconto` / `cupomCodigo` | - | Cupom de desconto aplicado |
| `assasCustomerId` | String | ID do cliente no Asaas |
| `assasSubscriptionId` | String | ID da assinatura recorrente no Asaas |

**Metodos de negocio:**
- `isTrialExpirado()` - true se status=TRIAL e `dtFimTrial` ja passou
- `isBloqueada()` - true se trial expirado, VENCIDA, SUSPENSA, ou CANCELADA com periodo expirado
- `isPlanoGratuito()` - true se precoMensal do plano = 0

### 2. `PlanoBellory` (tabela: `admin.plano_bellory`)

Plano da plataforma (ex: Gratuito, Basico, Plus, Premium).

| Campo | Tipo | Descricao |
|---|---|---|
| `codigo` | String | Codigo unico (ex: "gratuito", "basico", "plus", "premium") |
| `nome` | String | Nome de exibicao |
| `precoMensal` / `precoAnual` | BigDecimal | Precos do plano |
| `descontoPercentualAnual` | Double | Ex: 20.0 (20%) |
| `promoMensalAtiva/Preco/Texto` | - | Promocao mensal (ex: Black Friday) |
| `features` | JSONB | Lista de features do plano |
| `limites` | PlanoLimitesBellory | Limites (max funcionarios, servicos, etc.) |
| `ativo` | boolean | Se o plano esta disponivel |
| `popular` | boolean | Se e o plano "mais popular" |
| `ordemExibicao` | Integer | Ordem na tela |
| UI: `tagline`, `cta`, `badge`, `icone`, `cor`, `gradiente` | - | Campos visuais do plano |
| `cartaoCredito` | CartaoCredito | **PROBLEMA**: FK OneToOne para CartaoCredito (nao faz sentido semantico aqui) |

### 3. `CobrancaPlataforma` (tabela: `admin.cobranca_plataforma`)

Cada cobranca gerada (uma por periodo de pagamento).

| Campo | Tipo | Descricao |
|---|---|---|
| `assinatura` | Assinatura | FK - assinatura relacionada |
| `organizacao` | Organizacao | FK - organizacao (redundancia com assinatura) |
| `valor` | BigDecimal | Valor cobrado (ja com desconto se aplicavel) |
| `dtVencimento` | LocalDate | Data de vencimento |
| `dtPagamento` | LocalDateTime | Data efetiva do pagamento |
| `status` | StatusCobrancaPlataforma | PENDENTE, PAGA, VENCIDA, CANCELADA, ESTORNADA |
| `formaPagamento` | FormaPagamentoPlataforma | Forma usada nesta cobranca |
| Cupom: `cupom`, `valorOriginal`, `valorDescontoAplicado`, `cupomCodigo` | - | Dados do desconto |
| Asaas: `assasPaymentId`, `assasInvoiceUrl`, `assasBankSlipUrl`, `assasPixQrCode/CopiaCola` | - | Dados do Asaas |
| `referenciaMes` / `referenciaAno` | Integer | Mes/ano de referencia |

### 4. `PagamentoPlataforma` (tabela: `admin.pagamento_plataforma`)

Registro de pagamento efetivado (vinculado a uma cobranca).

| Campo | Tipo | Descricao |
|---|---|---|
| `cobranca` | CobrancaPlataforma | FK - cobranca paga |
| `valor` | BigDecimal | Valor pago |
| `status` | StatusPagamentoPlataforma | PENDENTE, CONFIRMADO, RECUSADO, ESTORNADO |
| `formaPagamento` | FormaPagamentoPlataforma | Forma usada |
| `assasPaymentId` / `assasTransactionId` | String | IDs do Asaas |
| `comprovanteUrl` | String | URL do comprovante |
| `dtPagamento` | LocalDateTime | Data/hora do pagamento |

### 5. `CupomDesconto` (tabela: `admin.cupom_desconto`)

Cupom de desconto com regras granulares.

| Campo | Tipo | Descricao |
|---|---|---|
| `codigo` | String | Codigo unico (ex: "WELCOME50") |
| `tipoDesconto` | TipoDesconto | PERCENTUAL ou VALOR_FIXO |
| `valorDesconto` | BigDecimal | Valor ou percentual do desconto |
| `tipoAplicacao` | TipoAplicacaoCupom | PRIMEIRA_COBRANCA ou RECORRENTE |
| `dtInicio` / `dtFim` | LocalDateTime | Periodo de vigencia |
| `maxUtilizacoes` / `maxUtilizacoesPorOrg` | Integer | Limites de uso |
| `planosPermitidos` | JSONB | Lista de codigos de planos validos |
| `segmentosPermitidos` | JSONB | Segmentos permitidos |
| `organizacoesPermitidas` | JSONB | IDs de orgs especificas |
| `cicloCobranca` | String | Se aplica apenas a MENSAL ou ANUAL |

### 6. `WebhookLog` (tabela: `admin.webhook_log`)

Log de eventos recebidos do Asaas (idempotencia).

| Campo | Tipo | Descricao |
|---|---|---|
| `assinatura` | Assinatura | FK - assinatura relacionada (pode ser null) |
| `evento` | String | Tipo do evento (ex: "PAYMENT_RECEIVED") |
| `assasPaymentId` | String | ID do pagamento no Asaas |
| `assasSubscriptionId` | String | ID da subscription no Asaas |
| `valor` | BigDecimal | Valor do pagamento |
| `statusPagamento` | String | Status reportado pelo Asaas |
| `payloadResumo` | TEXT | Resumo do payload |

---

## Enums

| Enum | Valores | Uso |
|---|---|---|
| `StatusAssinatura` | TRIAL, ATIVA, VENCIDA, CANCELADA, SUSPENSA | Estado interno da assinatura |
| `SituacaoAssinatura` | TRIAL_ATIVO, TRIAL_EXPIRADO, PLANO_GRATUITO, ATIVA, PAGAMENTO_PENDENTE, PAGAMENTO_ATRASADO, CANCELADA_COM_ACESSO, CANCELADA_SEM_ACESSO, SUSPENSA, SEM_ASSINATURA | Estado semantico para o frontend (derivado do status + regras) |
| `CicloCobranca` | MENSAL, ANUAL | Periodo de cobranca |
| `FormaPagamentoPlataforma` | PIX, BOLETO, CARTAO_CREDITO | Forma de pagamento |
| `StatusCobrancaPlataforma` | PENDENTE, PAGA, VENCIDA, CANCELADA, ESTORNADA | Status de uma cobranca |
| `StatusPagamentoPlataforma` | PENDENTE, CONFIRMADO, RECUSADO, ESTORNADO | Status de um pagamento |
| `TipoDesconto` | PERCENTUAL, VALOR_FIXO | Tipo do cupom |
| `TipoAplicacaoCupom` | PRIMEIRA_COBRANCA, RECORRENTE | Quando o cupom se aplica |

---

## Endpoints

### AssinaturaController (`/api/v1/assinatura`) - Uso da Organizacao

#### `GET /status`
**O que faz:** Retorna o status completo da assinatura da organizacao logada.

**Logica detalhada:**
1. Pega `organizacaoId` do `TenantContext` (JWT)
2. Busca a `Assinatura` pelo `organizacaoId`
3. Se nao existe: retorna `SEM_ASSINATURA` com `bloqueado=false` (!)
4. Se existe: chama `determinarSituacao()` que mapeia `StatusAssinatura` -> `SituacaoAssinatura`:
   - TRIAL -> verifica `isTrialExpirado()` -> TRIAL_ATIVO ou TRIAL_EXPIRADO
   - ATIVA -> verifica `isPlanoGratuito()` -> PLANO_GRATUITO; se nao, verifica cobranças pendentes -> PAGAMENTO_PENDENTE ou ATIVA
   - VENCIDA -> PAGAMENTO_ATRASADO
   - CANCELADA -> verifica `dtProximoVencimento` -> CANCELADA_COM_ACESSO ou CANCELADA_SEM_ACESSO
   - SUSPENSA -> SUSPENSA
5. Anexa informacoes de cobrancas pendentes (valor total, proximo vencimento)
6. Retorna `AssinaturaStatusDTO` com: bloqueado, statusAssinatura, situacao, mensagem, dados do plano, trial, cobranca

**Response:** `AssinaturaStatusDTO`

---

#### `GET /cobrancas?status={status}`
**O que faz:** Lista as cobranças da organizacao logada.

**Logica detalhada:**
1. Pega `organizacaoId` do TenantContext
2. **Tenta buscar do Asaas primeiro** via `assasClient.buscarPagamentosAssinatura(subscriptionId)`
3. Se o Asaas responder: converte cada `AssasPaymentResponse` para `CobrancaPlataformaDTO` com:
   - Mapeamento de status Asaas -> local (PENDING->PENDENTE, RECEIVED/CONFIRMED->PAGA, OVERDUE->VENCIDA, etc.)
   - Busca PIX QR Code do Asaas para pagamentos PIX pendentes
   - Enriquece com dados locais (cupom, valorOriginal) se encontrar `CobrancaPlataforma` correspondente via `assasPaymentId`
4. Se Asaas falhar: **fallback** para dados locais (tabela `cobranca_plataforma`)
5. Filtra por `status` se informado

**Filtro opcional:** `status` = PENDENTE | PAGA | VENCIDA | CANCELADA

**Response:** `List<CobrancaPlataformaDTO>`

---

#### `POST /validar-cupom`
**O que faz:** Valida um cupom de desconto ANTES de escolher o plano (preview no frontend).

**Logica detalhada:**
1. Recebe `ValidarCupomDTO` com: `codigoCupom`, `planoCodigo`, `cicloCobranca`
2. Busca a organizacao e o plano
3. Calcula o valor original baseado no ciclo
4. Delega para `CupomDescontoService.validarCupom()` que verifica:
   - Cupom existe e esta ativo
   - Vigencia (dtInicio/dtFim)
   - Limite global e por org nao atingido
   - Plano permitido, segmento, org especifica
   - Ciclo de cobranca permitido
5. Retorna: valido, mensagem, tipoDesconto, tipoAplicacao, valorOriginal, valorDesconto, valorComDesconto

**Body:** `ValidarCupomDTO`
**Response:** `CupomValidacaoResponseDTO`

---

#### `POST /escolher-plano`
**O que faz:** Escolhe um plano pela primeira vez ou apos o trial expirar. Cria a assinatura recorrente no Asaas.

**Logica detalhada:**
1. Recebe `EscolherPlanoDTO` com: `planoCodigo`, `cicloCobranca`, `formaPagamento`, `codigoCupom?`, `creditCard?`, `creditCardToken?`
2. Busca assinatura existente da org (deve existir - criada no onboarding como TRIAL)
3. Busca e valida o plano (existe e ativo)
4. Calcula valor baseado no ciclo + aplica cupom se informado
5. **Integracao Asaas:**
   - Cria cliente no Asaas se `assasCustomerId` ainda nao existe
   - Se ja existe `assasSubscriptionId` antiga: cancela no Asaas
   - Valida dados do cartao se `formaPagamento = CARTAO_CREDITO`
   - Cria nova assinatura recorrente no Asaas via `criarAssinatura()` com nextDueDate = amanha
6. **Atualiza assinatura local:**
   - `status` -> ATIVA
   - `planoBellory` -> novo plano
   - `cicloCobranca`, `formaPagamento`, `dtInicio` -> now()
   - `dtProximoVencimento` -> now() + 1 mes ou 1 ano
   - `valorMensal/valorAnual` -> precos do plano
   - `dtCancelamento` -> null
   - Campos de cupom se aplicavel
7. Registra utilizacao do cupom se usado

**Body:** `EscolherPlanoDTO`
**Response:** `AssinaturaResponseDTO`

**NOTA:** Nao verifica o status atual da assinatura antes de escolher o plano. Qualquer status (TRIAL, ATIVA, VENCIDA, etc.) pode chamar este endpoint.

---

#### `POST /trocar-plano`
**O que faz:** Faz upgrade ou downgrade de plano. Cancela a assinatura antiga no Asaas e cria uma nova.

**Logica detalhada:**
1. Recebe `EscolherPlanoDTO` (mesmo DTO do escolher-plano)
2. **Validacao:** assinatura deve estar ATIVA
3. Verifica que nao e o mesmo plano+ciclo
4. Calcula valor do novo plano + aplica cupom se informado
5. **Integracao Asaas:**
   - **Cancela** a assinatura antiga no Asaas (se falhar, lanca erro e para)
   - Se cupom RECORRENTE: calcula valor recorrente com desconto
   - Cria nova assinatura no Asaas
6. Atualiza assinatura local (mesma logica do escolher-plano)
7. **NOTA:** `dtInicio` e resetado para now(), `dtProximoVencimento` e recalculado do zero (nao faz pro-rata apesar do summary dizer "calculo pro-rata")

**Body:** `EscolherPlanoDTO`
**Response:** `AssinaturaResponseDTO`

---

#### `POST /cancelar`
**O que faz:** Cancela a assinatura ativa. O acesso e mantido ate o fim do periodo pago (`dtProximoVencimento`).

**Logica detalhada:**
1. Assinatura deve estar ATIVA
2. Cancela no Asaas (ignora erros do Asaas com warn)
3. `status` -> CANCELADA
4. `dtCancelamento` -> now()
5. Cancela cobranças locais PENDENTES -> CANCELADA
6. **Grace period:** acesso mantido ate `dtProximoVencimento`

**Response:** `AssinaturaResponseDTO`

---

#### `POST /reativar`
**O que faz:** Reativa uma assinatura CANCELADA ou VENCIDA.

**Logica detalhada:**
1. Recebe `EscolherPlanoDTO` (permite escolher novo plano/ciclo/forma)
2. Assinatura deve estar CANCELADA ou VENCIDA
3. Logica identica ao `escolherPlano`: cria nova assinatura no Asaas, atualiza local
4. `status` -> ATIVA, `dtCancelamento` -> null

**Body:** `EscolherPlanoDTO`
**Response:** `AssinaturaResponseDTO`

---

### AdminAssinaturaController (`/api/v1/admin/assinaturas`) - Uso do Admin

#### `GET /` - Listar todas as assinaturas
Filtros: `status`, `planoCodigo`. Retorna lista de `AssinaturaResponseDTO`.

#### `GET /dashboard` - Metricas de billing
Retorna `AdminBillingDashboardDTO` com metricas agregadas.

#### `GET /{id}` - Detalhe de uma assinatura por ID

#### `GET /{id}/cobrancas` - Cobranças de uma assinatura

#### `GET /organizacao/{orgId}` - Assinatura por organizacao

#### `GET /organizacao/{orgId}/cobrancas` - Cobranças por organizacao

#### `GET /organizacao/{orgId}/pagamentos` - Pagamentos por organizacao

#### `GET /cobrancas/{cobrancaId}/pagamentos` - Pagamentos de uma cobranca

#### `POST /{id}/cancelar` - Admin cancela assinatura

#### `POST /{id}/suspender` - Admin suspende assinatura

#### `POST /{id}/reativar` - Admin reativa assinatura

---

## Interceptor de Assinatura (`AssinaturaInterceptor`)

Executa antes de cada request (exceto admin/publico). Verifica se a org esta bloqueada via `verificarAcessoPermitido()`.

- Se `organizacaoId` == null (admin/publico): libera
- Se bloqueada: retorna **403 Forbidden** com `AcessoDTO` contendo mensagem e status
- Liberacao do bloqueio depende de `Assinatura.isBloqueada()`

---

## Jobs Agendados (`AssinaturaSchedulerService`)

| Job | Cron | Descricao |
|---|---|---|
| `bloquearCancelamentoExpirado` | 02:00 diario | Canceladas com `dtProximoVencimento` passado -> migra para plano gratuito (ATIVA com valorMensal=0) |
| `verificarInadimplentes` | 03:00 diario | Consulta Asaas para cada assinatura ATIVA com subscription -> se tem OVERDUE, marca como VENCIDA |
| `sincronizarComAsaas` | A cada 2h | Backup dos webhooks: reconcilia status e `dtProximoVencimento` com Asaas |
| `notificarTrialsExpirando` | 09:00 diario | Envia email para orgs com trial expirando nos proximos 6 dias (1 vez por assinatura) |

**NOTA:** Os jobs de `expirarTrials` e `gerarCobrancas` foram removidos do scheduler mas o metodo `expirarTrials()` ainda existe no service (migra para plano gratuito se existir, senao marca VENCIDA).

---

## Webhooks do Asaas

Endpoint (nao documentado nos controllers acima, mas existe no service):

Eventos tratados:
| Evento Asaas | Acao |
|---|---|
| `PAYMENT_RECEIVED` / `PAYMENT_CONFIRMED` | Marca cobranca como PAGA, cria PagamentoPlataforma, se assinatura VENCIDA->ATIVA, renova dtProximoVencimento. Se cupom PRIMEIRA_COBRANCA: atualiza Asaas para valor cheio e remove cupom |
| `PAYMENT_OVERDUE` | Marca cobranca como VENCIDA, assinatura ATIVA->VENCIDA |
| `PAYMENT_REFUNDED` | Marca cobranca como ESTORNADA |
| `PAYMENT_RESTORED` | Se assinatura VENCIDA->ATIVA |
| `PAYMENT_DELETED` | Apenas log |

**Idempotencia:** verifica em `WebhookLog` se ja processou (paymentId + evento).

---

## Fluxo de Vida da Assinatura

```
Onboarding Org -> criarAssinaturaTrial() -> status=TRIAL, plano=escolhido no onboarding
     |
     |  (14 dias depois)
     v
Trial Expirado -> isBloqueada()=true -> Interceptor bloqueia acesso (403)
     |
     |  [Org escolhe plano]
     v
escolherPlano() -> status=ATIVA, cria subscription no Asaas
     |
     +-- Pagamento confirmado (webhook) -> renova dtProximoVencimento
     |
     +-- trocarPlano() -> cancela assinatura Asaas, cria nova
     |
     +-- cancelarAssinatura() -> status=CANCELADA, grace period ate dtProximoVencimento
     |       |
     |       +-- (bloquearCancelamentoExpirado job) -> plano gratuito
     |       +-- reativarAssinatura() -> status=ATIVA, nova subscription Asaas
     |
     +-- Pagamento atrasado (webhook/job) -> status=VENCIDA -> bloqueado
             |
             +-- Pagamento confirmado (webhook) -> status=ATIVA
             +-- reativarAssinatura() -> status=ATIVA
```

---

## Problemas e Pontos de Atencao Identificados

### 1. Duplicacao de Logica Massiva
Os metodos `escolherPlano()`, `trocarPlano()` e `reativarAssinatura()` tem ~80% do codigo identico:
- Validacao de cupom
- Criacao de cliente/assinatura no Asaas
- Atualizacao da entidade local
- Registro de cupom

### 2. `escolherPlano()` nao valida status
Nao verifica se a assinatura esta em TRIAL, ATIVA, CANCELADA, etc. Qualquer status pode chamar. Deveria validar (ex: so TRIAL ou TRIAL_EXPIRADO).

### 3. Pro-rata inexistente no `trocarPlano()`
O endpoint documenta "calculo pro-rata" no summary, mas na realidade simplesmente reseta `dtInicio` e `dtProximoVencimento` do zero, sem considerar dias restantes.

### 4. `PlanoBellory.cartaoCredito` - FK sem sentido
A entidade `PlanoBellory` tem um `@OneToOne CartaoCredito` que nao faz sentido semantico. Um plano nao deveria ter cartao de credito.

### 5. Cobrancas locais vs Asaas - duplicidade de fonte de verdade
O `getMinhasCobrancas()` tenta buscar do Asaas primeiro e faz fallback para dados locais. Isso cria duas fontes de verdade que podem divergir. As cobranças locais so existem se foram criadas manualmente ou via webhook, mas o Asaas gera automaticamente.

### 6. `SEM_ASSINATURA` com `bloqueado=false`
Se uma org nao tem assinatura, retorna `bloqueado=false`, mas o interceptor (`verificarAcessoPermitido`) tambem retorna `bloqueado=false` para `SEM_ASSINATURA`. Isso permite acesso irrestrito a orgs sem assinatura.

### 7. Grace period de cancelamento -> Plano Gratuito
Quando o cancelamento expira, o job migra para plano gratuito (ATIVA com valor 0). Isso pode nao ser o comportamento desejado - talvez devesse bloquear completamente.

### 8. Erro no Asaas ao cancelar e tratado como warn
No `cancelarAssinatura()`, se o Asaas falhar ao cancelar, apenas loga warn e continua. A assinatura e marcada como CANCELADA localmente mas pode continuar cobrando no Asaas.

No `trocarPlano()`, por outro lado, se o cancelamento da antiga falhar no Asaas, lanca excecao (comportamento correto).

### 9. Valor no Asaas vs Valor local
Em `escolherPlano()`, se o cupom e PRIMEIRA_COBRANCA, o valor enviado ao Asaas e o valor com desconto. Depois, no webhook de PAYMENT_CONFIRMED, atualiza a subscription no Asaas para o valor cheio. Mas em `trocarPlano()`, trata cupom RECORRENTE separadamente calculando `valorRecorrente`. A logica de quando aplicar desconto na subscription do Asaas vs na primeira cobranca e inconsistente entre os metodos.

### 10. Service muito grande (God Class)
O `AssinaturaService` tem ~1400 linhas com responsabilidades mistas:
- CRUD de assinatura
- Integracao com Asaas
- Processamento de webhooks
- Jobs agendados
- Conversao de DTOs
- Logica de cupom
- Envio de email
