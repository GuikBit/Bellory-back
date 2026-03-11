# Remodelagem do Sistema de Assinaturas - Bellory API

**Data**: 2026-03-09
**Objetivo**: Reestruturar o sistema de assinaturas para que o Asaas seja a fonte principal de verdade (source of truth) para cobranças e pagamentos, salvando o mínimo necessário localmente.

---

## 1. Diagnóstico do Sistema Atual

### 1.1. O que existe hoje

O sistema atual mantém **duplicação massiva de dados** entre o Bellory e o Asaas:

| Dado | Bellory (local) | Asaas | Duplicado? |
|------|----------------|-------|------------|
| Cliente | Organizacao (completo) | Customer (name, cpfCnpj, email, phone) | Sim |
| Assinatura | Assinatura (completo) | Subscription (value, cycle, status) | Sim |
| Cobrança | CobrancaPlataforma (completo) | Payment (gerado automaticamente) | Sim |
| Pagamento | PagamentoPlataforma (completo) | Payment (status, comprovante) | Sim |
| Status | StatusAssinatura (local) | Subscription status (no Asaas) | Sim - podem divergir |
| Valores | valorMensal, valorAnual na Assinatura | value na Subscription | Sim |
| Vencimento | dtProximoVencimento (local) | nextDueDate (no Asaas) | Sim |

### 1.2. Problemas Identificados

#### P1: Risco de Divergência de Status
- O status da assinatura é mantido localmente (`StatusAssinatura`) e no Asaas separadamente.
- Se o webhook falhar ou atrasar, o sistema local pode ficar em estado inconsistente.
- Exemplo: Asaas marca como `overdue`, mas o webhook não chega → localmente continua `ATIVA`.

#### P2: Geração Local de Cobranças (Desnecessária)
- O scheduler `gerarCobrancasMensais()` e `gerarCobrancasAnuais()` criam cobranças localmente.
- **O Asaas já gera cobranças automaticamente** quando se usa o recurso de assinatura.
- Isso resulta em cobranças locais que podem não ter correspondência 1:1 com as do Asaas.

#### P3: Dados de Pagamento Salvos Localmente sem Necessidade
- `CobrancaPlataforma` e `PagamentoPlataforma` armazenam dados que o Asaas já mantém.
- Campos como `assas_invoice_url`, `assas_bank_slip_url`, `assas_pix_qr_code`, `assas_pix_copia_cola` são cópias estáticas que podem ficar desatualizadas.

#### P4: Webhook Limitado
- Só processa 3 eventos: `PAYMENT_RECEIVED`, `PAYMENT_CONFIRMED`, `PAYMENT_OVERDUE`.
- Não trata: `PAYMENT_DELETED`, `PAYMENT_REFUNDED`, `PAYMENT_RESTORED`, `SUBSCRIPTION_DELETED`, `SUBSCRIPTION_UPDATED`, etc.
- Não usa `PAYMENT_CREATED` para vincular o `assas_payment_id` à cobrança local.

#### P5: Falta de Consulta ao Asaas para Status Real
- O endpoint `GET /assinatura/status` retorna dados locais sem consultar o Asaas.
- O `AssinaturaInterceptor` bloqueia/libera com base no status local, que pode estar desatualizado.

#### P6: Trial Desabilitado em Código
- O `criarAssinaturaTrial()` está em modo teste - cria direto como ATIVA com assinatura no Asaas.
- Isso gera cobrança imediata para um cliente que deveria estar em trial.

#### P7: Excesso de Dados Locais
- `valorMensal`, `valorAnual` na entidade Assinatura são cópias do plano.
- Campos de cupom na Assinatura (`cupom_id`, `valor_desconto`, `cupom_codigo`) duplicam o que já está em CupomUtilizacao.
- `forma_pagamento` na Assinatura duplica o que está na subscription do Asaas.

#### P8: Sem Sincronização Periódica
- Não existe job que consulte o Asaas para reconciliar status.
- Se um pagamento for feito manualmente no painel do Asaas, o sistema local não saberá.

#### P9: AssasClient Silencia Erros
- Todos os métodos retornam `null` em caso de erro, sem lançar exceção.
- O sistema continua o fluxo normalmente, resultando em assinaturas sem `assasCustomerId` ou `assasSubscriptionId`.

#### P10: Cobrança Local Sem Vínculo com Asaas
- Cobranças geradas pelos schedulers (`gerarCobrancasMensais`, `gerarCobrancasAnuais`) **não têm `assas_payment_id`**.
- Quando o webhook chega, `findByAssasPaymentId()` não encontra a cobrança → pagamento perdido.

---

## 2. Arquitetura Proposta (Asaas como Source of Truth)

### 2.1. Princípio Fundamental

> **Salvar localmente apenas o que é necessário para controle de acesso e auditoria.**
> **Para tudo mais, consultar o Asaas sob demanda.**

### 2.2. O que MANTER no banco local

| Dado | Tabela | Justificativa |
|------|--------|---------------|
| ID do cliente no Asaas | `assinatura.assas_customer_id` | Referência para API calls |
| ID da subscription no Asaas | `assinatura.assas_subscription_id` | Referência para API calls |
| Status da assinatura | `assinatura.status` | Controle de acesso (interceptor) |
| Plano atual | `assinatura.plano_bellory_id` | Limites de recursos |
| Ciclo de cobrança | `assinatura.ciclo_cobranca` | Informativo |
| Datas de trial | `assinatura.dt_inicio_trial`, `dt_fim_trial` | Controle de trial |
| Data próximo vencimento | `assinatura.dt_proximo_vencimento` | Controle de acesso / grace period |
| Data cancelamento | `assinatura.dt_cancelamento` | Grace period |
| Cupom ativo | `assinatura.cupom_id` | Para aplicar desconto recorrente |
| Auditoria de cupons | `cupom_utilizacao` | Histórico/controle de uso |

### 2.3. O que REMOVER / SIMPLIFICAR do banco local

| Dado | Ação | Motivo |
|------|------|--------|
| `valor_mensal`, `valor_anual` na Assinatura | **Remover** | Já existe no PlanoBellory. Consultar de lá. |
| `forma_pagamento` na Assinatura | **Remover** | Consultar do Asaas (`subscription.billingType`) |
| `CobrancaPlataforma` (tabela inteira) | **Simplificar drasticamente** | Asaas já gerencia cobranças. Manter apenas como cache/log de webhooks recebidos |
| `PagamentoPlataforma` (tabela inteira) | **Remover ou transformar em log** | Asaas é o dono desse dado. Consultar via API quando necessário |
| Schedulers de geração de cobrança | **Remover** | Asaas gera cobranças automaticamente na assinatura |
| `assas_invoice_url`, `assas_bank_slip_url`, etc. | **Não salvar** | Consultar do Asaas via `GET /v3/payments/{id}` quando necessário |

### 2.4. O que ADICIONAR

| Funcionalidade | Descrição |
|----------------|-----------|
| Consulta de status ao Asaas | Endpoint que consulta `GET /v3/subscriptions/{id}` para status real |
| Consulta de cobranças ao Asaas | Endpoint que consulta `GET /v3/subscriptions/{id}/payments` |
| Sincronização periódica | Job que reconcilia status local vs Asaas |
| Mais eventos de webhook | Tratar todos os eventos relevantes |
| Tratamento de erros no AssasClient | Lançar exceções ao invés de retornar null |
| Cache com TTL | Cache de consultas ao Asaas para não sobrecarregar a API |

---

## 3. Novo Fluxo Proposto

### 3.1. Cadastro do Cliente (Registro)

```
[Cliente se cadastra no Bellory]
    |
    v
[1. Cria Organizacao no banco local]
    |
    v
[2. Cria Customer no Asaas]
    POST /v3/customers { name, cpfCnpj, email, phone }
    -> Salva assas_customer_id na Assinatura
    |
    v
[3. Cria Assinatura LOCAL com status TRIAL]
    - assas_customer_id = id retornado
    - assas_subscription_id = null (ainda não tem assinatura no Asaas)
    - status = TRIAL
    - plano = plano trial (ou premium trial)
    - dt_inicio_trial = agora
    - dt_fim_trial = agora + 14 dias
    |
    v
[4. NÃO cria subscription no Asaas ainda]
    - Trial é 100% local, sem cobrança no Asaas
```

**Diferença do atual**: Hoje está criando assinatura no Asaas imediatamente (modo teste). Proposta separa claramente trial (local) de assinatura paga (Asaas).

### 3.2. Escolha do Plano (Pós-Trial ou Durante Trial)

```
[Cliente escolhe plano pago]
    |
    v
[1. Validações locais]
    - Plano existe e está ativo
    - Cupom válido (se informado)
    |
    v
[2. Criar/Verificar Customer no Asaas]
    - Se assas_customer_id == null → POST /v3/customers
    - Se já existe → opcionalmente PUT /v3/customers/{id} para atualizar dados
    |
    v
[3. Cancelar subscription antiga no Asaas (se existir)]
    - DELETE /v3/subscriptions/{assas_subscription_id}
    |
    v
[4. Criar Subscription no Asaas]
    POST /v3/subscriptions {
        customer: assas_customer_id,
        billingType: PIX | BOLETO | CREDIT_CARD,
        value: valor_final (com desconto se cupom aplicado),
        cycle: MONTHLY | YEARLY,
        nextDueDate: D+1,
        description: "Bellory - Plano Plus",
        discount: {  // <-- NOVO: usar recurso nativo do Asaas para cupom
            value: valor_desconto,
            dueDateLimitDays: 0,
            type: PERCENTAGE | FIXED
        }
    }
    -> Retorna: id, status, nextDueDate, etc.
    |
    v
[5. Atualizar Assinatura LOCAL (mínimo)]
    - assas_subscription_id = id retornado
    - status = ATIVA
    - plano_bellory_id = novo plano
    - ciclo_cobranca = MENSAL ou ANUAL
    - dt_inicio = agora
    - dt_proximo_vencimento = baseado no ciclo
    - cupom_id (se aplicado)
    |
    v
[6. NÃO criar CobrancaPlataforma local]
    - O Asaas gera a cobrança automaticamente
    - Quando pagar, o webhook notifica
```

### 3.3. Processamento de Pagamentos (Webhook)

```
[Asaas envia webhook]
    |
    v
[Validar token de autenticação]
    |
    v
[Identificar subscription pelo payment.subscription]
    - Buscar Assinatura local por assas_subscription_id
    |
    +-- PAYMENT_CREATED
    |       - Log: nova cobrança gerada pelo Asaas
    |       - Opcionalmente salvar referência mínima local
    |
    +-- PAYMENT_CONFIRMED / PAYMENT_RECEIVED
    |       - Assinatura.status = ATIVA (garante)
    |       - Atualizar dt_proximo_vencimento
    |       - Se cupom PRIMEIRA_COBRANCA:
    |           → PUT /v3/subscriptions/{id} com valor cheio
    |           → Limpar cupom da assinatura local
    |       - Log do pagamento (opcional)
    |
    +-- PAYMENT_OVERDUE
    |       - Assinatura.status = VENCIDA (ou manter ATIVA com flag)
    |       - Asaas gerencia retentativas automaticamente
    |       - Opcionalmente: notificar cliente por email
    |
    +-- PAYMENT_DELETED
    |       - Log
    |
    +-- PAYMENT_REFUNDED
    |       - Avaliar: reverter status? Notificar admin?
    |
    +-- SUBSCRIPTION_DELETED / SUBSCRIPTION_INACTIVATED
            - Assinatura.status = CANCELADA
            - Iniciar grace period
```

### 3.4. Consulta de Status (Novo Fluxo)

```
[GET /api/v1/assinatura/status]
    |
    v
[1. Buscar Assinatura local]
    - status, plano, assas_subscription_id
    |
    v
[2. Se tem assas_subscription_id → consultar Asaas]
    GET /v3/subscriptions/{id}
    -> Retorna: status, value, cycle, nextDueDate, etc.
    |
    v
[3. Reconciliar]
    - Se status Asaas diverge do local → atualizar local
    - Ex: Asaas diz "INACTIVE" mas local diz "ATIVA" → marcar CANCELADA
    |
    v
[4. Retornar status consolidado ao frontend]
    - Status real (baseado no Asaas)
    - Plano e limites (local)
    - Dados de cobrança (do Asaas, não de tabela local)
```

### 3.5. Consulta de Cobranças (Novo Fluxo)

```
[GET /api/v1/assinatura/cobrancas]
    |
    v
[1. Buscar assas_subscription_id da assinatura]
    |
    v
[2. Consultar Asaas]
    GET /v3/subscriptions/{id}/payments
    ou
    GET /v3/payments?subscription={id}
    |
    v
[3. Retornar lista de cobranças do Asaas ao frontend]
    - id, value, status, dueDate, paymentDate
    - invoiceUrl, bankSlipUrl, pixQrCode (links diretos do Asaas)
    - Sem salvar nada localmente
```

### 3.6. Bloqueio de Acesso

```
[AssinaturaInterceptor.preHandle()]
    |
    v
[1. Buscar status LOCAL da assinatura]
    - Rápido: não faz API call em cada request
    |
    v
[2. Regras de bloqueio (mantém igual)]
    - TRIAL expirado → bloqueado
    - VENCIDA → bloqueado
    - SUSPENSA → bloqueado
    - CANCELADA + grace expirado → bloqueado
    |
    v
[3. Job de sincronização garante que o status local está correto]
    - Roda a cada 1h (ou configurável)
    - Consulta subscriptions ativas no Asaas
    - Atualiza status local conforme resposta
```

---

## 4. Novo Modelo de Dados (Simplificado)

### 4.1. Tabela `assinatura` (Revisada)

```sql
-- Campos que MANTÊM:
id                      BIGSERIAL PRIMARY KEY
organizacao_id          BIGINT NOT NULL UNIQUE   -- FK organizacao
plano_bellory_id        BIGINT NOT NULL          -- FK plano_bellory
status                  VARCHAR(20) NOT NULL     -- TRIAL, ATIVA, VENCIDA, CANCELADA, SUSPENSA
ciclo_cobranca          VARCHAR(10) NOT NULL     -- MENSAL, ANUAL

-- Trial (100% local):
dt_inicio_trial         TIMESTAMP
dt_fim_trial            TIMESTAMP
dt_trial_notificado     TIMESTAMP

-- Controle de acesso:
dt_inicio               TIMESTAMP                -- Quando começou a pagar
dt_proximo_vencimento   TIMESTAMP                -- Para grace period e bloqueio
dt_cancelamento         TIMESTAMP                -- Quando cancelou

-- Asaas (referências):
assas_customer_id       VARCHAR(100)             -- ID do customer no Asaas
assas_subscription_id   VARCHAR(100)             -- ID da subscription no Asaas

-- Cupom ativo (para recorrente):
cupom_id                BIGINT                   -- FK cupom_desconto (se recorrente ativo)
cupom_codigo            VARCHAR(50)              -- Código para referência rápida

-- Auditoria:
dt_criacao              TIMESTAMP NOT NULL
dt_atualizacao          TIMESTAMP

-- Campos REMOVIDOS:
-- valor_mensal          → consultar de plano_bellory.preco_mensal
-- valor_anual           → consultar de plano_bellory.preco_anual
-- valor_desconto        → consultar do cupom ou do Asaas
-- forma_pagamento       → consultar do Asaas (subscription.billingType)
```

### 4.2. Tabela `cobranca_plataforma` → `webhook_log` (Renomear/Simplificar)

```sql
-- Opção A: Transformar em log de webhooks (mínimo)
CREATE TABLE admin.webhook_log (
    id                  BIGSERIAL PRIMARY KEY,
    assinatura_id       BIGINT NOT NULL,
    evento              VARCHAR(50) NOT NULL,     -- PAYMENT_CONFIRMED, PAYMENT_OVERDUE, etc.
    assas_payment_id    VARCHAR(100),
    assas_subscription_id VARCHAR(100),
    valor               DECIMAL(10,2),
    status_pagamento    VARCHAR(30),
    payload_resumo      JSONB,                    -- Dados relevantes do webhook
    dt_recebimento      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Opção B: Manter CobrancaPlataforma mas sem schedulers de geração
-- Só criar registro quando webhook PAYMENT_CREATED chegar
```

### 4.3. Tabela `pagamento_plataforma` → REMOVER

Não é mais necessária. O Asaas tem todos os dados de pagamento. Consultar via API quando precisar.

### 4.4. Tabelas que NÃO MUDAM

- `plano_bellory` - mantém igual
- `plano_limites_bellory` - mantém igual
- `cupom_desconto` - mantém igual
- `cupom_utilizacao` - mantém igual

---

## 5. Novos Endpoints Asaas a Utilizar

### 5.1. Endpoints que já usamos

| Método | Endpoint | Uso |
|--------|----------|-----|
| POST | `/v3/customers` | Criar cliente |
| POST | `/v3/subscriptions` | Criar assinatura |
| PUT | `/v3/subscriptions/{id}` | Atualizar valor (pós cupom primeira cobrança) |
| DELETE | `/v3/subscriptions/{id}` | Cancelar assinatura |
| GET | `/v3/payments/{id}` | Buscar pagamento específico |

### 5.2. Endpoints NOVOS a implementar

| Método | Endpoint | Uso Proposto |
|--------|----------|--------------|
| GET | `/v3/subscriptions/{id}` | **Consultar status real da assinatura** |
| GET | `/v3/subscriptions/{id}/payments` | **Listar cobranças da assinatura** |
| PUT | `/v3/customers/{id}` | Atualizar dados do cliente |
| GET | `/v3/customers/{id}` | Consultar dados do cliente |
| GET | `/v3/payments/{id}/pixQrCode` | Obter QR code PIX sob demanda |
| GET | `/v3/payments/{id}/identificationField` | Obter linha digitável do boleto |
| POST | `/v3/payments/{id}/refund` | Estorno (admin) |

### 5.3. Webhooks a Tratar (Novos)

| Evento | Ação Local |
|--------|------------|
| `PAYMENT_CREATED` | Log + opcionalmente criar referência local |
| `PAYMENT_UPDATED` | Log |
| `PAYMENT_CONFIRMED` | ✅ Já trata - manter e melhorar |
| `PAYMENT_RECEIVED` | ✅ Já trata - manter |
| `PAYMENT_OVERDUE` | ✅ Já trata - melhorar (notificar cliente) |
| `PAYMENT_DELETED` | Log + verificar se impacta status |
| `PAYMENT_REFUNDED` | Marcar assinatura conforme regra de negócio |
| `PAYMENT_RESTORED` | Reverter status para ATIVA se necessário |
| `SUBSCRIPTION_DELETED` | Assinatura → CANCELADA |
| `SUBSCRIPTION_INACTIVATED` | Assinatura → SUSPENSA ou VENCIDA |

---

## 6. Schedulers (Revisados)

### 6.1. Schedulers que MANTÊM

| Job | Horário | Descrição |
|-----|---------|-----------|
| `expirarTrials()` | Diário 2:00 AM | Migra trials expirados para plano gratuito |
| `notificarTrialsExpirando()` | Diário 9:00 AM | Email de aviso 6 dias antes |
| `bloquearCancelamentoExpirado()` | Diário 2:00 AM | Cancelados com grace expirado → gratuito |

### 6.2. Schedulers que REMOVEM

| Job | Motivo da Remoção |
|-----|-------------------|
| `gerarCobrancasMensais()` | Asaas gera automaticamente |
| `gerarCobrancasAnuais()` | Asaas gera automaticamente |
| `marcarCobrancasVencidas()` | Asaas gerencia status de pagamento |

### 6.3. Schedulers NOVOS

| Job | Horário | Descrição |
|-----|---------|-----------|
| `sincronizarComAsaas()` | A cada 2h | Consulta subscriptions ativas no Asaas e reconcilia status local |
| `verificarInadimplentes()` | Diário 3:00 AM | Consulta assinaturas ATIVAS locais e verifica no Asaas se há pagamentos overdue |
| `notificarInadimplentes()` | Diário 10:00 AM | Email/WhatsApp para clientes com pagamento atrasado |

---

## 7. Controle de Acesso (Melhorado)

### 7.1. Interceptor (Rápido - Sem API Call)

O `AssinaturaInterceptor` continua usando **apenas dados locais** para não adicionar latência em cada request:

```
Bloqueado se:
  - status == TRIAL && dtFimTrial < agora
  - status == VENCIDA
  - status == SUSPENSA
  - status == CANCELADA && dtProximoVencimento < agora
  - assas_subscription_id == null && status != TRIAL (sem assinatura no Asaas)
```

### 7.2. Sincronização Garante Consistência

O job `sincronizarComAsaas()` atualiza o status local periodicamente:

```
Para cada assinatura ATIVA ou VENCIDA com assas_subscription_id:
    1. GET /v3/subscriptions/{id}
    2. Se Asaas status == "INACTIVE" ou "EXPIRED":
        → status local = VENCIDA
    3. Se Asaas status == "ACTIVE" e local != ATIVA:
        → status local = ATIVA (pagamento pode ter sido feito no painel)
    4. Atualizar dt_proximo_vencimento com nextDueDate do Asaas
```

### 7.3. Webhook como Atualização em Tempo Real

O webhook continua sendo o mecanismo **primário** de atualização:
- Pagamento confirmado → ATIVA imediatamente
- Pagamento vencido → VENCIDA (ou aguardar retentativas do Asaas)

O job de sincronização é apenas **backup** para quando webhooks falharem.

---

## 8. Fluxos do Admin

### 8.1. Dashboard (Consultar Asaas)

```
[GET /api/v1/admin/assinaturas/dashboard]
    |
    v
[Dados locais]
    - Total de assinaturas por status (query local)
    - Total por plano (query local)
    |
    v
[Dados do Asaas (opcionalmente)]
    - MRR (Monthly Recurring Revenue): somar values das subscriptions ativas
    - Churn: assinaturas canceladas no mês
    - Inadimplência: pagamentos overdue
```

### 8.2. Detalhes de um Cliente (Consultar Asaas)

```
[GET /api/v1/admin/assinaturas/{id}]
    |
    v
[Dados locais]
    - Assinatura: status, plano, ciclo, datas
    |
    v
[Dados do Asaas]
    GET /v3/subscriptions/{assas_subscription_id}
    → status, value, cycle, nextDueDate, billingType

    GET /v3/subscriptions/{id}/payments
    → Lista de cobranças com status, valores, links de pagamento
```

### 8.3. Ações Admin

| Ação | Implementação |
|------|---------------|
| Suspender | Status local → SUSPENSA + DELETE /v3/subscriptions/{id} |
| Reativar | Status local → ATIVA + POST /v3/subscriptions (nova) |
| Cancelar | Status local → CANCELADA + DELETE /v3/subscriptions/{id} |
| Estornar pagamento | POST /v3/payments/{id}/refund |
| Alterar plano de cliente | PUT /v3/subscriptions/{id} com novo valor |

---

## 9. Tratamento de Erros (Melhorado)

### 9.1. AssasClient - Parar de Silenciar Erros

**Atual (problemático)**:
```java
// Retorna null silenciosamente
catch (Exception e) {
    log.error("Erro ao criar cliente no Assas: {}", e.getMessage());
    return null;
}
```

**Proposto**:
```java
// Lançar exceção para que o chamador decida
catch (HttpClientErrorException e) {
    log.error("Erro Asaas [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
    throw new AssasApiException("Falha ao criar cliente: " + e.getMessage(), e);
}
```

### 9.2. Nova Exceção: `AssasApiException`

- Extend `RuntimeException`
- Carry HTTP status code e response body do Asaas
- Controller advice retorna mensagem amigável ao frontend

### 9.3. Retry com Resiliência

- Usar `@Retryable` (Spring Retry) ou Resilience4j para chamadas ao Asaas
- 3 tentativas com backoff exponencial
- Circuit breaker para evitar cascading failure

---

## 10. Cache (Novo)

### 10.1. Cache de Consultas ao Asaas

Para evitar excesso de chamadas à API do Asaas:

| Consulta | TTL | Invalidação |
|----------|-----|-------------|
| `GET /v3/subscriptions/{id}` | 5 min | Invalidar quando webhook recebido |
| `GET /v3/subscriptions/{id}/payments` | 5 min | Invalidar quando webhook de payment recebido |
| `GET /v3/customers/{id}` | 30 min | Invalidar quando dados do cliente mudam |

Implementar com Spring Cache + Caffeine (in-memory).

---

## 11. Resumo das Mudanças

### 11.1. O que SIMPLIFICA

| Item | Antes | Depois |
|------|-------|--------|
| Tabelas de billing | 3 (cobranca, pagamento, assinatura) | 1 (assinatura) + 1 log opcional |
| Schedulers | 6 jobs | 3 mantidos + 2 novos (sincronização) |
| Dados locais | ~30 campos de billing | ~15 campos essenciais |
| Geração de cobrança | Manual por scheduler | Automática pelo Asaas |

### 11.2. O que MELHORA

| Item | Antes | Depois |
|------|-------|--------|
| Status de pagamento | Local (pode divergir) | Asaas (source of truth) + sync |
| Links de pagamento | Salvos localmente (estáticos) | Consultados sob demanda (sempre atuais) |
| Webhook | 3 eventos | 10+ eventos |
| Tratamento de erro | Silencioso (null) | Exceções + retry |
| Consulta de cobranças | Tabela local | API Asaas em tempo real |
| Reconciliação | Não existe | Job automático a cada 2h |

### 11.3. O que ADICIONA

| Item | Descrição |
|------|-----------|
| Novos endpoints no AssasClient | GET subscription, GET payments, PUT customer, refund |
| Job de sincronização | Reconcilia status local vs Asaas |
| Cache | Evita excesso de calls à API |
| Mais webhooks | Cobertura completa de eventos |
| Tratamento de erros robusto | Exceções, retry, circuit breaker |

---

## 12. Ordem de Implementação Sugerida

### Fase 1: Fundação (Prioridade Alta)
1. Melhorar `AssasClient` com tratamento de erros e novos endpoints
2. Implementar novos webhooks (PAYMENT_CREATED, SUBSCRIPTION_DELETED, etc.)
3. Restaurar trial correto (remover bloco de teste)
4. Criar job de sincronização (`sincronizarComAsaas()`)

### Fase 2: Simplificação (Prioridade Alta)
5. Remover schedulers de geração de cobrança
6. Remover `valor_mensal`/`valor_anual`/`forma_pagamento` da entidade Assinatura
7. Criar endpoints que consultam cobranças do Asaas
8. Simplificar/remover tabela `pagamento_plataforma`

### Fase 3: Robustez (Prioridade Média)
9. Implementar cache (Spring Cache + Caffeine)
10. Implementar retry/circuit breaker (Resilience4j)
11. Melhorar webhook controller (idempotência, deduplicação)
12. Criar endpoint de reconciliação manual (admin)

### Fase 4: Otimização (Prioridade Baixa)
13. Dashboard admin consultando métricas do Asaas
14. Notificações automáticas de inadimplência
15. Transformar `cobranca_plataforma` em `webhook_log`
16. Migração de dados históricos

---

## 13. Validação de Assinatura no Login

### 13.1. Como Funciona Hoje

No `AuthController.login()` (linhas 96-102), após autenticar o usuário:

```java
// Buscar status da assinatura
try {
    AssinaturaStatusDTO assinaturaStatus = assinaturaService.getStatusAssinatura(org.getId());
    organizacaoInfo.setAssinatura(assinaturaStatus);
} catch (Exception e) {
    // Nao bloqueia login se falhar ao buscar assinatura
    System.err.println("Erro ao buscar status da assinatura: " + e.getMessage());
}
```

**O que retorna hoje** (`AssinaturaStatusDTO`):
- `bloqueado` (boolean) - se o acesso está bloqueado
- `statusAssinatura` (String) - TRIAL, ATIVA, VENCIDA, CANCELADA, SUSPENSA, SEM_ASSINATURA
- `diasRestantesTrial` (Integer) - dias restantes do trial (só para TRIAL)
- `mensagem` (String) - mensagem amigável
- `planoCodigo` / `planoNome` (String) - plano atual
- `temCobrancaPendente` (Boolean) - se há cobrança pendente
- `valorPendente` (BigDecimal) - valor total pendente
- `dtVencimentoProximaCobranca` (LocalDate) - próximo vencimento

### 13.2. Problemas Identificados no Login

#### P1: Não bloqueia login quando deveria alertar
O login **sempre retorna sucesso** com status 200, mesmo quando a assinatura está bloqueada. O frontend recebe `bloqueado: true` mas cabe a ele decidir o que fazer. Isso é válido, porém **faltam informações** para o frontend tomar a decisão correta:
- Não retorna o **tipo de pendência** de forma clara (enum)
- Não retorna **links de pagamento** para cobranças pendentes
- Não diferencia entre "trial ativo" e "plano gratuito" (ambos aparecem como ATIVA)

#### P2: Silencia erro com System.err.println
Se `getStatusAssinatura()` falhar, o login retorna `organizacaoInfo.assinatura = null`, e o frontend não sabe se não tem assinatura ou se houve erro.

#### P3: Não consulta o Asaas
O status vem 100% do banco local. Se o Asaas já confirmou um pagamento mas o webhook não chegou, o login mostra "vencida" mesmo já estando paga.

#### P4: OrganizacaoInfoDTO retorna `PlanoBellory` como entidade JPA
`organizacaoInfo.setPlano(org.getPlano())` retorna a entidade JPA inteira, incluindo dados internos. Deveria ser um DTO.

#### P5: Faltam informações úteis para o frontend
O frontend precisa saber **exatamente** o que mostrar ao usuário no login:
- Se está em trial: quantos dias restam, data de expiração
- Se está no plano gratuito: limites do plano
- Se tem cobrança pendente: link para pagar (PIX, boleto)
- Se está cancelada: data de encerramento do acesso
- Se está vencida: o que fazer (regularizar/escolher plano)

### 13.3. Proposta: Novo `AssinaturaStatusDTO` para Login

```java
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssinaturaStatusDTO {

    // === Status principal ===
    private boolean bloqueado;
    private String statusAssinatura;       // TRIAL, ATIVA, VENCIDA, CANCELADA, SUSPENSA, SEM_ASSINATURA
    private String situacao;               // NOVO CAMPO - enum claro para o frontend (ver abaixo)
    private String mensagem;

    // === Plano ===
    private String planoCodigo;
    private String planoNome;
    private boolean planoGratuito;         // NOVO - flag direto para o frontend

    // === Trial ===
    private Integer diasRestantesTrial;
    private LocalDate dtFimTrial;          // NOVO - data exata do fim do trial

    // === Cobrança pendente ===
    private Boolean temCobrancaPendente;
    private BigDecimal valorPendente;
    private LocalDate dtVencimentoProximaCobranca;

    // === Cancelamento ===
    private LocalDate dtAcessoAte;         // NOVO - até quando pode usar (grace period)

    // === Ciclo ===
    private String cicloCobranca;          // NOVO - MENSAL ou ANUAL
    private LocalDate dtProximoVencimento; // NOVO - próxima renovação
}
```

### 13.4. Novo Enum: `SituacaoAssinatura` (para o frontend)

O campo `situacao` retorna um valor semântico claro que o frontend usa diretamente para decidir qual tela/modal mostrar:

| Situacao | Quando | O que o Frontend faz |
|----------|--------|---------------------|
| `TRIAL_ATIVO` | Status TRIAL + dtFimTrial > agora | Mostra banner "X dias restantes de trial" |
| `TRIAL_EXPIRADO` | Status TRIAL + dtFimTrial < agora | Mostra tela de escolha de plano (bloqueado) |
| `PLANO_GRATUITO` | Status ATIVA + plano gratuito | Mostra banner "Upgrade para desbloquear recursos" |
| `ATIVA` | Status ATIVA + plano pago | Acesso normal, sem banners |
| `PAGAMENTO_PENDENTE` | Status ATIVA + temCobrancaPendente | Mostra alerta "Pagamento pendente de R$X" |
| `PAGAMENTO_ATRASADO` | Status VENCIDA | Mostra tela de regularização (bloqueado) |
| `CANCELADA_COM_ACESSO` | Status CANCELADA + dtAcessoAte > agora | Mostra banner "Acesso até dd/mm/yyyy" |
| `CANCELADA_SEM_ACESSO` | Status CANCELADA + dtAcessoAte < agora | Mostra tela para reativar (bloqueado) |
| `SUSPENSA` | Status SUSPENSA | Mostra tela "Conta suspensa, contate suporte" (bloqueado) |
| `SEM_ASSINATURA` | Não encontrou assinatura | Mostra tela de escolha de plano |

### 13.5. Novo Método `getStatusAssinatura()` (Proposto)

```
[getStatusAssinatura(organizacaoId)]
    |
    v
[1. Buscar Assinatura local]
    - Se não existe → retorna SEM_ASSINATURA
    |
    v
[2. Determinar SITUACAO (lógica semântica)]
    |
    +-- Status == TRIAL
    |       +-- dtFimTrial > agora → TRIAL_ATIVO (diasRestantes, dtFimTrial)
    |       +-- dtFimTrial < agora → TRIAL_EXPIRADO (bloqueado=true)
    |
    +-- Status == ATIVA
    |       +-- plano gratuito → PLANO_GRATUITO
    |       +-- tem cobrança pendente vencida → PAGAMENTO_PENDENTE
    |       +-- normal → ATIVA
    |
    +-- Status == VENCIDA → PAGAMENTO_ATRASADO (bloqueado=true, valorPendente)
    |
    +-- Status == CANCELADA
    |       +-- dtProximoVencimento > agora → CANCELADA_COM_ACESSO (dtAcessoAte)
    |       +-- dtProximoVencimento < agora → CANCELADA_SEM_ACESSO (bloqueado=true)
    |
    +-- Status == SUSPENSA → SUSPENSA (bloqueado=true)
    |
    v
[3. Se assas_subscription_id existe → opcionalmente consultar Asaas]
    GET /v3/subscriptions/{id}
    - Reconciliar status se divergente
    - Atualizar dtProximoVencimento com nextDueDate do Asaas
    |
    v
[4. Montar e retornar AssinaturaStatusDTO]
```

### 13.6. Validação no `/auth/validate` e `/auth/me`

Hoje o `POST /auth/validate` e `GET /auth/me` **não retornam status de assinatura**.
Eles deveriam, porque o frontend pode chamar esses endpoints ao recarregar a página.

**Proposta**:
- `/auth/validate` → adicionar `assinatura: AssinaturaStatusDTO` no `TokenValidationResponseDTO`
- `/auth/me` → adicionar `assinatura: AssinaturaStatusDTO` no response (via `UserInfoDTO` ou diretamente)

Isso garante que **toda vez que o frontend validar o token**, ele recebe o status atualizado da assinatura.

### 13.7. Fluxo Completo Login → Validação de Acesso

```
[Login / Validate / Me]
    |
    v
[1. Autenticar/Validar token]
    |
    v
[2. Buscar status da assinatura (getStatusAssinatura)]
    → Retorna situacao + bloqueado + dados complementares
    |
    v
[3. Retornar no response]
    {
        success: true,
        token: "...",
        user: { ... },
        organizacao: {
            id: 1,
            nome: "Salão X",
            assinatura: {
                bloqueado: false,
                statusAssinatura: "ATIVA",
                situacao: "PAGAMENTO_PENDENTE",  // ← Frontend usa isso
                planoCodigo: "plus",
                planoNome: "Plano Plus",
                planoGratuito: false,
                cicloCobranca: "MENSAL",
                temCobrancaPendente: true,
                valorPendente: 79.90,
                dtVencimentoProximaCobranca: "2026-03-15",
                mensagem: "Voce tem um pagamento pendente de R$ 79,90"
            }
        }
    }
    |
    v
[4. Frontend decide o que mostrar baseado em 'situacao']
    - TRIAL_ATIVO → banner de trial
    - PLANO_GRATUITO → banner de upgrade
    - PAGAMENTO_PENDENTE → alerta de pagamento
    - PAGAMENTO_ATRASADO → tela de bloqueio + regularizar
    - CANCELADA_COM_ACESSO → banner de cancelamento
    - etc.
```

### 13.8. Resumo das Mudanças no Login

| Item | Antes | Depois |
|------|-------|--------|
| Status retornado | 6 valores genéricos (TRIAL, ATIVA, ...) | 10 situações semânticas claras |
| Plano gratuito | Aparece como "ATIVA" | Aparece como "PLANO_GRATUITO" |
| Cobrança pendente | `temCobrancaPendente: true` genérico | `situacao: PAGAMENTO_PENDENTE` com valor e data |
| Cancelamento | `bloqueado: true` sem contexto | `CANCELADA_COM_ACESSO` ou `CANCELADA_SEM_ACESSO` com data |
| Trial | Dias restantes | Dias restantes + data exata do fim |
| Validação de token | Não retorna assinatura | Retorna assinatura atualizada |
| `/auth/me` | Não retorna assinatura | Retorna assinatura atualizada |
| Tratamento de erro | `System.err.println` silencioso | Log adequado + retornar situação de fallback |

---

## 14. Riscos e Considerações

| Risco | Mitigação |
|-------|-----------|
| Asaas fora do ar | Cache local + status local como fallback. Interceptor usa dados locais. |
| Rate limit da API Asaas | Cache + batch queries na sincronização |
| Webhook não chega | Job de sincronização a cada 2h como backup |
| Migração de dados | Manter tabelas antigas por 90 dias durante transição |
| Webhook duplicado | Implementar idempotência (verificar se já processou o payment_id) |
| Consulta lenta ao Asaas | Cache com TTL de 5 min + interceptor usa apenas dados locais |
