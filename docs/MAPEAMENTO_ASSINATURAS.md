# Mapeamento do Sistema de Assinaturas - Bellory API

**Data**: 2026-03-05
**Versao**: 2.0 (atualizado com ajustes 9.1 a 9.9)

---

## 1. Visao Geral da Arquitetura

O sistema de assinaturas gerencia o ciclo de vida completo de um cliente desde o cadastro ate a cobranca recorrente, integrado com o gateway de pagamento **Assas**.

### Entidades Principais (Schema: `admin`)

| Tabela | Descricao |
|--------|-----------|
| `assinatura` | Assinatura do cliente (1:1 com organizacao) |
| `plano_bellory` | Planos disponiveis (gratuito, basico, plus, premium) |
| `cobranca_plataforma` | Registros de cobranca gerados |
| `pagamento_plataforma` | Registros de pagamento confirmados |
| `cupom_desconto` | Cupons de desconto configurados |
| `cupom_utilizacao` | Historico/auditoria de uso de cupons |

---

## 2. Fluxo Completo do Ciclo de Vida

### 2.1. Cadastro e Trial (14 dias)

```
[Cliente se cadastra]
    |
    v
[Assinatura criada com status TRIAL]
    - dt_inicio_trial = agora
    - dt_fim_trial = agora + 14 dias
    - Configurado via: bellory.trial.dias=14
    |
    v
[Notificacao 6 dias antes do fim do trial] (scheduler 9h)
    - Email "trial-expirando.html" enviado
    - dt_trial_notificado marcado para evitar duplicatas
    |
    v
[Trial expira] (scheduler 2h da manha)
    - Cliente migra automaticamente para plano GRATUITO
    - Status -> ATIVA com plano gratuito (limites do plano gratuito aplicados)
    - Se plano gratuito nao existe no banco: status -> VENCIDA (fallback)
    |
    v
[Cliente loga com plano gratuito]
    - isBloqueada() agora inclui isTrialExpirado()
    - Cliente ve tela para upgrade de plano
    - Limites do plano gratuito sao aplicados automaticamente
```

**Regras implementadas**:
- `isTrialExpirado()` verifica `LocalDateTime.now().isAfter(dtFimTrial)`
- `isBloqueada()` retorna true para VENCIDA, CANCELADA, SUSPENSA e TRIAL_EXPIRADO
- Trial expirado automaticamente migra para plano gratuito (nao fica bloqueado)

### 2.2. Escolha do Plano (pos-trial ou durante trial)

```
[Cliente escolhe plano via EscolherPlanoDTO]
    - planoCodigo (obrigatorio)
    - cicloCobranca: MENSAL ou ANUAL (obrigatorio)
    - formaPagamento: PIX, BOLETO ou CARTAO_CREDITO (obrigatorio)
    - codigoCupom (opcional)
    |
    v
[Validacoes]
    1. Plano existe e esta ativo
    2. Cupom valido (se informado) - ver secao 4
    3. Calculo do valor final
    |
    v
[Integracao Assas]
    1. Criar cliente no Assas (se nao existir) -> assas_customer_id
    2. Se existia assinatura Assas antiga -> cancela
    3. Criar assinatura no Assas -> assas_subscription_id
       - billingType: PIX | BOLETO | CREDIT_CARD
       - value: valor com desconto (cupom PRIMEIRA_COBRANCA ou RECORRENTE)
       - cycle: MONTHLY | YEARLY
       - nextDueDate: amanha (D+1)
    |
    v
[Atualizacao Local]
    - Status: TRIAL/CANCELADA/VENCIDA -> ATIVA
    - dt_inicio = agora
    - dt_cancelamento = null (limpa cancelamento anterior)
    - dt_proximo_vencimento = calculado por ciclo
    - valor_mensal / valor_anual = preco do plano
    - forma_pagamento = preferencia salva
    - cupom_id, valor_desconto, cupom_codigo (se cupom aplicado)
    |
    v
[Criar Cobranca]
    - CobrancaPlataforma criada com status PENDENTE
    - Registro de CupomUtilizacao (se cupom usado)
```

### 2.3. Ciclo de Cobranca

#### MENSAL
- **Primeira cobranca**: vencimento D+1 apos escolha do plano
- **Cobrancas recorrentes**: geradas no dia 1 de cada mes, as 6h (scheduler)
- **Proximo vencimento**: data atual + 1 mes
- **Valor**: `plano.precoMensal` (ou `promoMensalPreco` se promocao ativa)
- **Cupom RECORRENTE**: aplicado em cada cobranca mensal enquanto vigente

#### ANUAL
- **Primeira cobranca**: vencimento D+1 apos escolha do plano
- **Valor cobrado**: `plano.precoAnual` (valor integral de uma vez)
- **Proximo vencimento**: data atual + 1 ano
- **Cobranca de renovacao**: gerada 30 dias antes do vencimento (scheduler diario 3h)
- **Cupom RECORRENTE**: aplicado na renovacao anual se cupom ainda vigente

### 2.4. Processamento de Pagamento (Webhooks Assas)

```
[Assas envia webhook]
    |
    +-- PAYMENT_RECEIVED / PAYMENT_CONFIRMED
    |       - CobrancaPlataforma.status -> PAGA
    |       - PagamentoPlataforma criado (status: CONFIRMADO)
    |       - Assinatura garantida como ATIVA
    |       - Proximo vencimento renovado (+ 1 mes ou + 1 ano)
    |       - Se cupom PRIMEIRA_COBRANCA:
    |           -> Atualiza assinatura no Assas para valor CHEIO
    |           -> Remove cupom da assinatura local
    |
    +-- PAYMENT_OVERDUE
            - CobrancaPlataforma.status -> VENCIDA
            (retentativa fica a cargo do Assas)
```

### 2.5. Upgrade / Downgrade de Plano

```
[Cliente troca de plano via trocarPlano()]
    |
    v
[Validacoes]
    1. Assinatura deve estar ATIVA
    2. Novo plano diferente do atual
    3. Plano existe e esta ativo
    |
    v
[Calculo Pro-Rata]
    - Dias restantes do periodo atual
    - Credito = (valor_atual * dias_restantes) / dias_totais_ciclo
    - Valor primeira cobranca = novo_valor - credito_pro_rata
    - Se resultado negativo -> R$ 0,00
    |
    v
[Assas]
    - Cancela assinatura antiga
    - Cria nova assinatura com novo valor
    |
    v
[Cobranca]
    - Gera cobranca com valor pro-rata (D+1)
    - Proximo vencimento recalculado
```

### 2.6. Cancelamento

```
[Cliente cancela assinatura]
    |
    v
[Regras]
    - Apenas assinaturas ATIVAS podem ser canceladas
    - Status -> CANCELADA
    - dt_cancelamento = agora
    - Assas: cancela assinatura (bloqueia renovacao)
    - Cobrancas PENDENTES -> CANCELADAS
    |
    v
[Periodo de Graca]
    - Cliente continua usando ate dt_proximo_vencimento
    - buildStatusDTO() mostra data de acesso restante
    - Apos dt_proximo_vencimento expirar:
        -> Scheduler bloquearCancelamentoExpirado() migra para plano gratuito
    |
    v
[Reativacao]
    - Cliente pode reativar via reativarAssinatura()
    - Data de inicio = agora, proximo vencimento = +30 dias ou +1 ano
    - Cobranca gerada imediatamente (D+1)
    - Nova assinatura criada no Assas
```

---

## 3. Status e Transicoes

### 3.1. StatusAssinatura

```
TRIAL -------> ATIVA (trial expira -> plano gratuito automatico)
TRIAL -------> ATIVA (cliente escolhe plano pago)
TRIAL -------> VENCIDA (trial expira sem plano gratuito disponivel - fallback)
ATIVA -------> VENCIDA (cobranca nao paga / webhook PAYMENT_OVERDUE)
ATIVA -------> CANCELADA (cancelamento pelo cliente/admin)
ATIVA -------> SUSPENSA (suspensao pelo admin)
CANCELADA ---> ATIVA (reativacao - cliente assina novamente)
VENCIDA -----> ATIVA (pagamento confirmado via webhook / reativacao)
SUSPENSA ----> ATIVA (reativacao pelo admin)
CANCELADA ---> ATIVA (periodo expirou -> migra para plano gratuito)
```

### 3.2. StatusCobrancaPlataforma

```
PENDENTE --> PAGA (webhook PAYMENT_CONFIRMED)
PENDENTE --> VENCIDA (webhook PAYMENT_OVERDUE / scheduler diario)
PENDENTE --> CANCELADA (cancelamento de assinatura / manual)
PAGA -----> ESTORNADA (estorno)
```

### 3.3. StatusPagamentoPlataforma

```
PENDENTE --> CONFIRMADO
PENDENTE --> RECUSADO
CONFIRMADO -> ESTORNADO
```

---

## 4. Regras de Cupom de Desconto

### 4.1. Validacoes na Aplicacao do Cupom

| # | Validacao | Campo | Regra |
|---|-----------|-------|-------|
| 1 | Cupom existe | `codigo` | Busca por codigo ativo |
| 2 | Cupom ativo | `ativo` | Deve ser `true` |
| 3 | Vigencia | `dtInicio`, `dtFim` | `dtInicio <= agora <= dtFim` |
| 4 | Limite global | `maxUtilizacoes` | Total de usos < maxUtilizacoes |
| 5 | Limite por org | `maxUtilizacoesPorOrg` | Usos da org < maxUtilizacoesPorOrg |
| 6 | Plano compativel | `planosPermitidos` | Plano selecionado esta na lista (JSONB) |
| 7 | Segmento compativel | `segmentosPermitidos` | publicoAlvo da org esta na lista (JSONB) |
| 8 | Org permitida | `organizacoesPermitidas` | ID da org esta na lista (JSONB) |
| 9 | Ciclo compativel | `cicloCobranca` | MENSAL, ANUAL ou null (ambos) |

### 4.2. Tipos de Desconto

| Tipo | Calculo | Exemplo |
|------|---------|---------|
| `PERCENTUAL` | `desconto = valorOriginal * (valorDesconto / 100)` | Cupom 20% em plano R$100 = R$20 desconto |
| `VALOR_FIXO` | `desconto = min(valorDesconto, valorOriginal)` | Cupom R$50 em plano R$100 = R$50 desconto; Cupom R$150 em plano R$100 = R$100 desconto (cap) |

**Nota sobre desconto anual fixo**: Um cupom VALOR_FIXO aplica o valor exato do desconto sobre o total anual. Ex: cupom R$50 em plano anual R$1200 = R$50 de desconto (total R$1150). A configuracao do cupom (porcentagem vs fixo) e feita no cadastro do cupom pelo admin, com plena consciencia do tipo de aplicacao.

### 4.3. Tipo de Aplicacao

| Tipo | Comportamento |
|------|--------------|
| `PRIMEIRA_COBRANCA` | Desconto aplicado SOMENTE na primeira cobranca. Apos pagamento confirmado (webhook), atualiza assinatura no Assas para valor cheio e remove cupom da assinatura local. |
| `RECORRENTE` | Desconto aplicado em TODAS as cobrancas geradas (mensal e anual) enquanto cupom vigente (dtInicio/dtFim). |

**Fluxo PRIMEIRA_COBRANCA com Assas**:
1. Criar assinatura no Assas com valor com desconto
2. Webhook PAYMENT_CONFIRMED recebido
3. `confirmarPagamento()` detecta cupom PRIMEIRA_COBRANCA
4. Chama `assasClient.atualizarAssinatura()` com valor cheio (PUT /subscriptions/{id})
5. Remove cupom da assinatura local

**Fluxo RECORRENTE com cobrancas**:
- Mensais: scheduler `gerarCobrancasMensais()` aplica desconto se `cupom.isVigente()`
- Anuais: scheduler `gerarCobrancasAnuais()` aplica desconto se `cupom.isVigente()`
- Se cupom expirou (dtFim < agora) -> valor cheio

### 4.4. Auditoria de Uso

Cada uso de cupom e registrado em `cupom_utilizacao`:
- `valor_original` - valor antes do desconto
- `valor_desconto` - valor do desconto aplicado
- `valor_final` - valor apos desconto
- `planoCodigo`, `cicloCobranca` - contexto do uso

---

## 5. Integracao Assas

### 5.1. Configuracao

```properties
assas.api.url=https://sandbox.asaas.com/api/v3  # ou producao
assas.api.key=<TOKEN>
assas.webhook.token=<TOKEN_WEBHOOK>
```

### 5.2. Operacoes

| Operacao | Metodo | Endpoint Assas | Quando |
|----------|--------|----------------|--------|
| Criar Cliente | POST | `/v3/customers` | Ao escolher plano (se nao existe) |
| Criar Assinatura | POST | `/v3/subscriptions` | Ao escolher/reativar/trocar plano |
| Atualizar Assinatura | PUT | `/v3/subscriptions/{id}` | Apos 1o pagamento com cupom PRIMEIRA_COBRANCA |
| Cancelar Assinatura | DELETE | `/v3/subscriptions/{id}` | Ao cancelar plano / trocar plano |
| Buscar Pagamento | GET | `/v3/payments/{id}` | Consulta manual |

### 5.3. Dados enviados ao Assas

**Cliente** (`AssasCustomerRequest`):
- name, cpfCnpj, email, phone

**Assinatura** (`AssasSubscriptionRequest`):
- customer (assas_customer_id)
- billingType: PIX | BOLETO | CREDIT_CARD
- value: valor final (com desconto se cupom aplicado)
- cycle: MONTHLY | YEARLY
- nextDueDate: D+1
- description: nome do plano

### 5.4. Webhook (Recebimento)

Endpoint: recebe payload `AssasWebhookPayload` com evento e dados do pagamento.
- Autentica via token no header
- Localiza cobranca pelo `assas_payment_id`
- Atualiza status conforme evento
- **PAYMENT_CONFIRMED**: renovacao do proximo vencimento + ajuste cupom PRIMEIRA_COBRANCA
- **PAYMENT_OVERDUE**: marca cobranca como vencida (Assas gerencia retentativas)

---

## 6. Schedulers (Jobs Automaticos)

| Horario | Job | Descricao |
|---------|-----|-----------|
| Diario 2:00 | `expirarTrials()` | Migra trials expirados para plano gratuito |
| Diario 2:00 | `marcarCobrancasVencidas()` | Marca cobrancas pendentes com vencimento passado como VENCIDA |
| Diario 2:00 | `bloquearCancelamentoExpirado()` | Cancelados com periodo expirado -> plano gratuito |
| Diario 3:00 | `gerarCobrancasAnuais()` | Gera cobranca de renovacao 30 dias antes do vencimento anual |
| Diario 9:00 | `notificarTrialsExpirando()` | Envia email para trials que expiram em 6 dias |
| Mensal dia 1, 6:00 | `gerarCobrancasMensais()` | Gera cobrancas para assinaturas MENSAL ativas |

---

## 7. Endpoints da API

### App (Usuario)
| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| GET | `/api/v1/assinatura/status` | Status da assinatura |
| GET | `/api/v1/assinatura/cobrancas` | Cobrancas do usuario |
| POST | `/api/v1/assinatura/validar-cupom` | Validar cupom (autenticado) |
| POST | `/api/v1/assinatura/escolher-plano` | Escolher plano (primeira vez / pos-trial) |
| POST | `/api/v1/assinatura/trocar-plano` | Upgrade/Downgrade de plano (com pro-rata) |
| POST | `/api/v1/assinatura/cancelar` | Cancelar assinatura (acesso ate fim do periodo) |
| POST | `/api/v1/assinatura/reativar` | Reativar assinatura cancelada/vencida |
| GET | `/api/v1/public/planos` | Listar planos (publico) |
| POST | `/api/v1/public/planos/validar-cupom` | Validar cupom (publico) |

### Admin
| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| CRUD | `/api/v1/admin/assinaturas` | Gerenciar assinaturas |
| GET | `/api/v1/admin/assinaturas/dashboard` | Metricas de billing |
| CRUD | `/api/v1/admin/cupons` | Gerenciar cupons |
| GET | `/api/v1/admin/cupons/vigentes` | Cupons ativos |
| GET | `/api/v1/admin/cupons/{id}/utilizacoes` | Historico de uso |

---

## 8. Migrations Relacionadas

| Versao | Arquivo | Descricao |
|--------|---------|-----------|
| V34 | `plano_bellory_promo_mensal.sql` | Campos de promocao mensal no plano |
| V35 | `assinatura_cobranca_plataforma.sql` | Tabelas core: assinatura, cobranca, pagamento |
| V36 | `cupom_desconto.sql` | Tabelas de cupom e utilizacao |
| V38 | `assinatura_forma_pagamento.sql` | Forma pagamento e dt_trial_notificado |
| V39 | `cupom_tipo_aplicacao.sql` | Campo tipo_aplicacao no cupom |

---

## 9. Resolucao dos Pontos de Atencao

### 9.1. Trial Expirado sem Acao - RESOLVIDO

**Solucao implementada**:
- `isBloqueada()` agora inclui `isTrialExpirado()` na verificacao
- Scheduler `expirarTrials()` migra automaticamente para plano GRATUITO
- Se plano gratuito nao existe: fallback para status VENCIDA
- Limites do plano gratuito sao aplicados automaticamente pela entidade `PlanoLimitesBellory`
- Cliente ao logar ve a tela de upgrade de plano

**Arquivos alterados**: `Assinatura.java`, `AssinaturaService.java`

---

### 9.2. Cobranca Anual e Renovacao - RESOLVIDO

**Solucao implementada**:
- Novo scheduler `gerarCobrancasAnuais()` roda diariamente as 3:00 AM
- Gera cobranca de renovacao 30 dias antes do vencimento
- Query `findAnuaisParaRenovacao()` busca assinaturas anuais ativas com vencimento proximo
- Protecao contra duplicatas via `existsByAssinaturaIdAndReferenciaMesAndReferenciaAno`
- Fallback local caso Assas falhe

**Arquivos alterados**: `AssinaturaService.java`, `AssinaturaSchedulerService.java`, `AssinaturaRepository.java`

---

### 9.3. Cupom Recorrente em Plano Anual - RESOLVIDO

**Solucao implementada**:
- `gerarCobrancasAnuais()` verifica e aplica cupom RECORRENTE se vigente
- Mesma logica de `gerarCobrancasMensais()`: verifica `tipoAplicacao == RECORRENTE && isVigente()`
- Se cupom expirou, renovacao anual cobra valor cheio

**Arquivos alterados**: `AssinaturaService.java`

---

### 9.4. Sincronizacao Assas x Local - RESOLVIDO

**Solucao implementada**:
- Cupom PRIMEIRA_COBRANCA: assinatura criada no Assas com valor com desconto
- Apos PAYMENT_CONFIRMED (webhook): `atualizarAssinatura()` no Assas com valor CHEIO
- Cupom removido da assinatura local (cupom_id=null, valor_desconto=null, cupom_codigo=null)
- Proximas cobrancas no Assas serao pelo valor cheio

**Novo metodo no AssasClient**: `atualizarAssinatura(String id, AssasSubscriptionRequest request)` -> PUT `/v3/subscriptions/{id}`

**Arquivos alterados**: `AssasClient.java`, `AssinaturaService.java` (metodo `confirmarPagamento`)

---

### 9.5. Mudanca de Plano / Upgrade / Downgrade - RESOLVIDO

**Solucao implementada**:
- Novo metodo `trocarPlano(EscolherPlanoDTO dto)` no `AssinaturaService`
- Calculo de pro-rata: `credito = (valor_atual * dias_restantes) / dias_totais_ciclo`
  - MENSAL: dias_totais = 30
  - ANUAL: dias_totais = 365
- Valor da primeira cobranca = novo_valor - credito_pro_rata
- Se credito > novo_valor, cobranca = R$ 0,00
- Cancela assinatura antiga no Assas e cria nova
- Suporte a cupom na troca de plano

**Arquivos alterados**: `AssinaturaService.java`

---

### 9.6. Cancelamento e Reembolso - RESOLVIDO

**Solucao implementada**:
- `cancelarAssinatura()`: marca como CANCELADA, cancela no Assas, cancela cobrancas pendentes
- **Periodo de graca**: cliente usa ate `dt_proximo_vencimento`
- `buildStatusDTO()` mostra data de acesso restante para status CANCELADA
- Apos periodo expirar: scheduler `bloquearCancelamentoExpirado()` migra para plano gratuito
- `reativarAssinatura()`: reativa com novo plano, data de inicio = agora, cobranca imediata (D+1)
  - Proximo vencimento = agora + 1 mes (ou + 1 ano)
  - Nova assinatura no Assas

**SEM reembolso**: politica definida como "usa ate o fim do periodo pago"

**Arquivos alterados**: `AssinaturaService.java`, `AssinaturaSchedulerService.java`

---

### 9.7. Retentativa de Pagamento - RESOLVIDO

**Solucao implementada**:
- Pagamento controlado 100% pelo Assas (retentativas automaticas)
- Webhook PAYMENT_CONFIRMED: processa cobranca, libera acesso, renova assinatura
- Webhook PAYMENT_OVERDUE: marca cobranca como vencida
- `confirmarPagamento()` agora renova `dt_proximo_vencimento` ao confirmar pagamento

**Arquivos alterados**: `AssinaturaService.java`

---

### 9.8. Concorrencia na Geracao de Cobrancas - OK

**Status**: Ja estava implementado com `existsByAssinaturaIdAndReferenciaMesAndReferenciaAno`.
Mantido para cobrancas mensais E anuais.

---

### 9.9. Valor do Desconto Anual Fixo vs Percentual - VALIDADO

**Regra validada e mantida**:
- `PERCENTUAL`: aplica porcentagem sobre o valor total (mensal ou anual)
- `VALOR_FIXO`: aplica valor exato, com cap no valor original (`min(desconto, valor)`)
- O cupom e configurado pelo admin com plena consciencia do tipo
- O cupom pode ser restrito a ciclo MENSAL ou ANUAL via campo `cicloCobranca`
- Se quiser desconto mensal equivalente no anual, o admin cria cupom PERCENTUAL

**Nenhuma alteracao necessaria.**

---

## 10. Diagrama de Tabelas

```
plano_bellory (admin)
    |
    | 1:N
    v
assinatura (admin)  <----  cupom_desconto (admin)
    |                           |
    | 1:N                       | 1:N
    v                           v
cobranca_plataforma         cupom_utilizacao
    |
    | 1:N
    v
pagamento_plataforma
```

---

## 11. Resumo das Regras de Negocio

| # | Regra | Status |
|---|-------|--------|
| 1 | Trial de 14 dias no cadastro | IMPLEMENTADO |
| 2 | Notificacao 6 dias antes do trial expirar | IMPLEMENTADO |
| 3 | Trial expirado -> migra para plano gratuito | IMPLEMENTADO (ajustado) |
| 4 | Bloqueio efetivo de trial expirado | IMPLEMENTADO (ajustado) |
| 5 | Ciclo MENSAL com cobranca mensal | IMPLEMENTADO |
| 6 | Ciclo ANUAL com cobranca unica + renovacao local | IMPLEMENTADO (novo) |
| 7 | Cupom PRIMEIRA_COBRANCA com sincronizacao Assas | IMPLEMENTADO (ajustado) |
| 8 | Cupom RECORRENTE em cobrancas mensais | IMPLEMENTADO |
| 9 | Cupom RECORRENTE em renovacao anual | IMPLEMENTADO (novo) |
| 10 | Validacao de cupom por plano/ciclo/segmento/org | IMPLEMENTADO |
| 11 | Limite global e por org no cupom | IMPLEMENTADO |
| 12 | Criacao de cliente/assinatura no Assas | IMPLEMENTADO |
| 13 | Atualizacao de assinatura no Assas (PUT) | IMPLEMENTADO (novo) |
| 14 | Webhook de pagamento com renovacao de vencimento | IMPLEMENTADO (ajustado) |
| 15 | Geracao automatica de cobrancas mensais | IMPLEMENTADO |
| 16 | Geracao automatica de cobrancas anuais (30 dias antes) | IMPLEMENTADO (novo) |
| 17 | Upgrade/Downgrade de plano com pro-rata | IMPLEMENTADO (novo) |
| 18 | Cancelamento com periodo de graca | IMPLEMENTADO (novo) |
| 19 | Reativacao de assinatura cancelada/vencida | IMPLEMENTADO (novo) |
| 20 | Bloqueio de cancelamento expirado -> plano gratuito | IMPLEMENTADO (novo) |
| 21 | Retentativa de pagamento via Assas + webhook | IMPLEMENTADO (ajustado) |
| 22 | Desconto anual fixo vs percentual validado | VALIDADO |
