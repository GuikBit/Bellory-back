# Documento de Melhorias — Modelo de Assinatura Bellory

**Integração com Asaas | Março 2026 | v1.0**

---

## 1. Resumo Executivo

Este documento apresenta uma análise detalhada do modelo de assinatura atual do sistema Bellory, com foco na integração com o gateway de pagamentos Asaas. O objetivo central é garantir que o Asaas seja a **fonte única de verdade** (single source of truth) para dados financeiros, reduzindo duplicidade de dados no banco local e simplificando a sincronização.

A análise identificou problemas críticos na lógica de upgrade/downgrade de planos, na gestão de estados inconsistentes entre o banco local e o Asaas, e na ausência de cálculos proporcionais (pro-rata) para troca de plano.

---

## 2. Problemas Críticos Identificados

| Severidade | Área | Problema | Impacto |
|------------|------|----------|---------|
| **CRÍTICO** | `trocarPlano()` | Não calcula pro-rata de upgrade/downgrade. Cancela assinatura antiga e cria nova sem cobrar diferença ou gerar crédito. | Cliente faz upgrade sem pagar diferença do mês corrente; downgrade não gera crédito. |
| **CRÍTICO** | `trocarPlano()` | Cancela assinatura no Asaas antes de criar a nova. Se a criação falhar, o cliente fica sem assinatura. | Janela de inconsistência: cliente sem assinatura ativa em nenhum lado. |
| **CRÍTICO** | `escolherPlano()` | Marca assinatura como ATIVA antes de confirmar pagamento no Asaas. Se o pagamento for boleto/pix, o acesso é liberado antes do pagamento. | Acesso liberado sem pagamento confirmado para boleto/pix. |
| **ALTO** | Dados Locais | Armazena valorMensal, valorAnual, dtProximoVencimento localmente duplicando dados do Asaas, gerando risco de dessincronização. | Dados divergentes entre banco local e Asaas. |
| **ALTO** | `confirmarPagamento` | Calcula dtProximoVencimento com `LocalDateTime.now()` ao invés de usar nextDueDate do Asaas. | Vencimento local pode divergir do Asaas em horas/dias. |
| **ALTO** | Webhook | Não processa eventos de assinatura (SUBSCRIPTION_*), apenas PAYMENT_*. Mudanças diretas na assinatura no painel Asaas não são refletidas. | Status do Asaas não reflete localmente quando alterado pelo painel. |
| **MÉDIO** | `AssasClient` | Cache de pagamentos pode retornar dados stale após webhook. A evição é feita mas pode ter race conditions. | Dados exibidos ao usuário podem estar desatualizados. |
| **MÉDIO** | Cupom | Cupom RECORRENTE é aplicado na criação da assinatura no Asaas, mas não há mecanismo para remover o desconto quando o cupom expirar. | Desconto recorrente pode durar para sempre. |
| **MÉDIO** | Scheduler | `verificarInadimplentes()` carrega TODAS as assinaturas ativas e faz N chamadas individuais ao Asaas, sem paginação. | Performance degradada com crescimento da base de clientes. |
| **BAIXO** | `criarAssinaturaTrial` | Cria cliente no Asaas durante o trial. Se o usuário nunca converter, o cliente fica órfão no Asaas. | Clientes órfãos no Asaas consumindo cota da conta. |
| **BAIXO** | Controller | AdminAssinaturaController captura Exception genérica em todos os endpoints ao invés de usar @ControllerAdvice. | Dificuldade de manutenção e tratamento de erros inconsistente. |

---

## 3. Detalhamento das Melhorias

### 3.1 Implementar Pro-Rata em Upgrade/Downgrade

**Problema Atual:**

O método `trocarPlano()` simplesmente cancela a assinatura antiga no Asaas e cria uma nova com o valor cheio do novo plano. Não há cálculo da diferença proporcional ao período já pago.

**Cenário Exemplo — Upgrade (Plano 2 R$139 → Plano 3 R$199):**

- Cliente pagou R$139 no dia 01/03 (mensal)
- No dia 15/03, quer fazer upgrade para o Plano 3 (R$199)
- Dias restantes no ciclo: 16 dias de 31
- Crédito do plano atual: R$139 × (16/31) = R$71,74
- Custo proporcional do novo plano: R$199 × (16/31) = R$102,71
- **Diferença a cobrar: R$102,71 − R$71,74 = R$30,97**
- Cliente paga R$30,97 via cobrança avulsa para efetivar o upgrade

**Cenário Exemplo — Downgrade (Plano 2 R$139 → Plano 1 R$69):**

- Cliente pagou R$139 no dia 01/03
- No dia 15/03, quer fazer downgrade para Plano 1 (R$69)
- Crédito do plano atual: R$139 × (16/31) = R$71,74
- Custo proporcional do novo plano: R$69 × (16/31) = R$35,61
- **Crédito gerado: R$71,74 − R$35,61 = R$36,13**
- Esse crédito será descontado na próxima cobrança recorrente

**Método `calcularProRata()` a implementar:**

```java
/**
 * Calcula o valor pro-rata para upgrade ou downgrade.
 * 
 * @return valor positivo = cobrança avulsa (upgrade), negativo = crédito (downgrade)
 */
public BigDecimal calcularProRata(Assinatura assinatura, PlanoBellory novoPlano, CicloCobranca novoCiclo) {
    // 1. Buscar nextDueDate do Asaas (fim do ciclo atual)
    AssasSubscriptionResponse sub = assasClient.buscarAssinatura(assinatura.getAssasSubscriptionId());
    LocalDate fimCiclo = LocalDate.parse(sub.getNextDueDate());
    LocalDate hoje = LocalDate.now();
    
    // 2. Calcular dias restantes e total do ciclo
    long diasRestantes = ChronoUnit.DAYS.between(hoje, fimCiclo);
    long diasTotalCiclo = assinatura.getCicloCobranca() == CicloCobranca.ANUAL ? 365 : fimCiclo.lengthOfMonth();
    
    if (diasRestantes <= 0) {
        return BigDecimal.ZERO; // Ciclo já venceu, não há pro-rata
    }
    
    // 3. Valor atual e novo
    BigDecimal valorAtual = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
            ? assinatura.getPlanoBellory().getPrecoAnual()
            : assinatura.getPlanoBellory().getPrecoMensal();
    BigDecimal valorNovo = novoCiclo == CicloCobranca.ANUAL
            ? novoPlano.getPrecoAnual()
            : novoPlano.getPrecoMensal();
    
    // 4. Calcular pro-rata
    BigDecimal fatorProporcional = BigDecimal.valueOf(diasRestantes)
            .divide(BigDecimal.valueOf(diasTotalCiclo), 4, RoundingMode.HALF_UP);
    
    BigDecimal creditoAtual = valorAtual.multiply(fatorProporcional).setScale(2, RoundingMode.HALF_UP);
    BigDecimal custoNovo = valorNovo.multiply(fatorProporcional).setScale(2, RoundingMode.HALF_UP);
    
    // Positivo = upgrade (cobrar), Negativo = downgrade (crédito)
    return custoNovo.subtract(creditoAtual);
}
```

**Fluxo de Upgrade Proposto:**

```
1. Cliente solicita upgrade
2. Sistema calcula pro-rata → valor positivo (diferença a cobrar)
3. Gera cobrança avulsa no Asaas via assasClient.criarCobrancaAvulsa()
4. Salva referência: assinatura.setCobrancaUpgradeAssasId(paymentId)
5. Status muda para UPGRADE_PENDENTE
6. Webhook PAYMENT_CONFIRMED da cobrança avulsa:
   a. Cria nova assinatura no Asaas com novo plano
   b. Se sucesso → cancela assinatura antiga no Asaas
   c. Atualiza plano e status local para ATIVA
```

**Fluxo de Downgrade Proposto:**

```
1. Cliente solicita downgrade
2. Sistema calcula pro-rata → valor negativo (crédito)
3. Armazena crédito: assinatura.setCreditoProRata(valorCredito)
4. Cria nova assinatura no Asaas com valor ajustado (preço novo − crédito)
5. Se sucesso → cancela assinatura antiga no Asaas
6. Webhook PAYMENT_CONFIRMED da primeira cobrança com desconto:
   a. Atualiza assinatura no Asaas para valor cheio do novo plano
   b. Limpa creditoProRata
```

---

### 3.2 Eliminar Janela de Inconsistência na Troca de Plano

**Problema Atual:**

No `trocarPlano()`, a assinatura antiga é cancelada no Asaas **ANTES** de criar a nova. Se a criação da nova assinatura falhar (erro de rede, cartão recusado, etc.), o cliente fica sem assinatura em nenhum lado.

```java
// CÓDIGO ATUAL (PROBLEMÁTICO)
// Passo 1: cancela antiga — ponto sem retorno
assasClient.cancelarAssinatura(assinatura.getAssasSubscriptionId()); 
// Passo 2: cria nova — se falhar aqui, cliente sem assinatura!
AssasSubscriptionResponse sub = assasClient.criarAssinatura(assasRequest);
```

**Solução Proposta:**

```java
// CÓDIGO CORRIGIDO
// Passo 1: cria nova primeiro
AssasSubscriptionResponse novaSub = assasClient.criarAssinatura(assasRequest);
if (novaSub == null) {
    throw new IllegalStateException("Falha ao criar nova assinatura no Asaas. Assinatura atual mantida.");
}

// Passo 2: só cancela antiga após sucesso
String antigaSubId = assinatura.getAssasSubscriptionId();
assinatura.setAssasSubscriptionId(novaSub.getId());
assinaturaRepository.save(assinatura);

try {
    assasClient.cancelarAssinatura(antigaSubId);
} catch (AssasApiException e) {
    // Logar para resolver manualmente — a nova já está ativa
    log.error("Assinatura antiga {} não foi cancelada no Asaas. Resolver manualmente.", antigaSubId, e);
}
```

---

### 3.3 Não Liberar Acesso Antes do Pagamento

**Problema Atual:**

O método `escolherPlano()` marca a assinatura como `StatusAssinatura.ATIVA` imediatamente após criar a subscription no Asaas, independentemente da forma de pagamento. Para boleto e PIX, o pagamento ainda não foi confirmado neste momento.

**Solução Proposta:**

```java
// No escolherPlano(), após criar assinatura no Asaas:
if (forma == FormaPagamentoPlataforma.CARTAO_CREDITO) {
    // Cartão: Asaas tenta cobrar na hora
    assinatura.setStatus(StatusAssinatura.ATIVA);
} else {
    // Boleto/PIX: aguardar confirmação via webhook
    assinatura.setStatus(StatusAssinatura.AGUARDANDO_PAGAMENTO);
}

// No webhook PAYMENT_CONFIRMED:
if (assinatura.getStatus() == StatusAssinatura.AGUARDANDO_PAGAMENTO) {
    assinatura.setStatus(StatusAssinatura.ATIVA);
    assinaturaRepository.save(assinatura);
    log.info("Primeiro pagamento confirmado — assinatura ativada para org: {}", 
             assinatura.getOrganizacao().getId());
}
```

**Atualizar `verificarAcessoPermitido()`:**

```java
case AGUARDANDO_PAGAMENTO -> {
    builder.bloqueado(true);
    builder.mensagem("Aguardando confirmação do pagamento. "
        + "Após a confirmação, seu acesso será liberado automaticamente.");
}
```

---

### 3.4 Asaas como Fonte Única de Verdade

**Problema Atual:**

O sistema armazena localmente: `valorMensal`, `valorAnual`, `dtProximoVencimento`, status de cobranças, pagamentos. Esses dados frequentemente ficam dessincronizados com o Asaas.

**Dados que devem ficar APENAS no Asaas:**

- Valor da assinatura recorrente (o Asaas é quem cobra)
- Data do próximo vencimento (o Asaas gerencia o ciclo)
- Status individual de cada cobrança/pagamento
- Dados de cartão de crédito (já funciona assim)
- URLs de boleto, PIX QR Code, fatura

**Dados que devem ficar no banco local:**

- `assasCustomerId` e `assasSubscriptionId` (referência ao Asaas)
- Status da assinatura (TRIAL, ATIVA, CANCELADA, etc.) — cache do Asaas, atualizado por webhook
- Plano atual (`planoBellory`) — informação de negócio local
- Ciclo de cobrança (referência local)
- Dados de cupom aplicado
- Histórico de ações (logs, auditoria)

**Campos a remover da entidade `Assinatura`:**

```java
// REMOVER estes campos:
// private BigDecimal valorMensal;   → consultar PlanoBellory.getPrecoMensal()
// private BigDecimal valorAnual;    → consultar PlanoBellory.getPrecoAnual()
// private LocalDateTime dtProximoVencimento; → consultar Asaas com cache
```

**Campos a adicionar:**

```java
// ADICIONAR estes campos:
private BigDecimal creditoProRata;          // crédito pendente de downgrade
private String cobrancaUpgradeAssasId;      // ID da cobrança avulsa de pro-rata
private String planoAnteriorCodigo;         // para rollback em caso de falha
```

**Método para consultar próximo vencimento:**

```java
/**
 * Busca dtProximoVencimento do Asaas (com cache de 5min no AssasClient).
 * Fallback para cálculo local se Asaas estiver indisponível.
 */
public LocalDate getProximoVencimento(Assinatura assinatura) {
    if (assinatura.getAssasSubscriptionId() == null) {
        return null;
    }
    try {
        AssasSubscriptionResponse sub = assasClient.buscarAssinatura(assinatura.getAssasSubscriptionId());
        if (sub != null && sub.getNextDueDate() != null) {
            return LocalDate.parse(sub.getNextDueDate());
        }
    } catch (AssasApiException e) {
        log.warn("Erro ao buscar nextDueDate do Asaas: {}", e.getMessage());
    }
    return null; // frontend deve tratar ausência
}
```

---

### 3.5 Usar nextDueDate do Asaas no Webhook

**Problema Atual:**

No `confirmarPagamento()`, o `dtProximoVencimento` é calculado com `LocalDateTime.now().plusMonths(1)`. Isso gera divergência com o Asaas.

**Solução Proposta:**

```java
private void confirmarPagamento(AssasWebhookPayload.Payment payment, Assinatura assinatura) {
    // ... código existente de atualizar cobrança local ...

    if (assinatura.getStatus() == StatusAssinatura.VENCIDA 
            || assinatura.getStatus() == StatusAssinatura.AGUARDANDO_PAGAMENTO) {
        assinatura.setStatus(StatusAssinatura.ATIVA);
    }

    // ANTES (problemático):
    // assinatura.setDtProximoVencimento(LocalDateTime.now().plusMonths(1));
    
    // DEPOIS (correto): buscar do Asaas
    if (payment.getSubscription() != null) {
        try {
            AssasSubscriptionResponse sub = assasClient.buscarAssinatura(payment.getSubscription());
            if (sub != null && sub.getNextDueDate() != null) {
                // Asaas é a fonte de verdade para o próximo vencimento
                // Se ainda mantiver dtProximoVencimento como cache local:
                LocalDate nextDue = LocalDate.parse(sub.getNextDueDate());
                assinatura.setDtProximoVencimento(nextDue.atStartOfDay());
            }
        } catch (AssasApiException e) {
            log.warn("Não foi possível buscar nextDueDate do Asaas. Usando cálculo local como fallback.");
            // Fallback: calcular baseado no dueDate do pagamento
            if (payment.getDueDate() != null) {
                LocalDate dueDate = LocalDate.parse(payment.getDueDate());
                LocalDate nextDue = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                        ? dueDate.plusYears(1) : dueDate.plusMonths(1);
                assinatura.setDtProximoVencimento(nextDue.atStartOfDay());
            }
        }
    }

    assinaturaRepository.save(assinatura);
    
    // ... código existente de cupom PRIMEIRA_COBRANÇA ...
}
```

---

### 3.6 Processar Webhooks de Assinatura

**Problema Atual:**

O método `processarWebhookPagamento()` só processa eventos `PAYMENT_*`. Eventos de assinatura como `SUBSCRIPTION_UPDATED`, `SUBSCRIPTION_DELETED`, `SUBSCRIPTION_EXPIRED` não são tratados.

**Solução Proposta:**

```java
@Transactional
public void processarWebhookAssinatura(AssasWebhookSubscriptionPayload payload) {
    if (payload == null || payload.getSubscription() == null) {
        log.warn("Webhook Asaas de assinatura recebido com payload inválido");
        return;
    }

    String event = payload.getEvent();
    String subscriptionId = payload.getSubscription().getId();
    
    log.info("Webhook Asaas assinatura - evento: {}, subscriptionId: {}", event, subscriptionId);

    // Idempotência
    if (webhookLogRepository.existsByAssasSubscriptionIdAndEvento(subscriptionId, event)) {
        log.info("Webhook de assinatura já processado: subscriptionId={}, evento={}", subscriptionId, event);
        return;
    }

    Assinatura assinatura = assinaturaRepository.findByAssasSubscriptionId(subscriptionId).orElse(null);
    if (assinatura == null) {
        log.warn("Assinatura local não encontrada para subscriptionId: {}", subscriptionId);
        return;
    }

    // Registrar log
    WebhookLog webhookLog = WebhookLog.builder()
            .assinatura(assinatura)
            .evento(event)
            .assasSubscriptionId(subscriptionId)
            .build();
    webhookLogRepository.save(webhookLog);

    // Evictar cache
    assasClient.evictSubscriptionCache(subscriptionId);
    assasClient.evictPaymentsCache(subscriptionId);

    switch (event) {
        case "SUBSCRIPTION_UPDATED" -> {
            // Atualizar dados que possam ter mudado no Asaas
            AssasSubscriptionResponse sub = assasClient.buscarAssinatura(subscriptionId);
            if (sub != null) {
                // Sincronizar status
                if ("INACTIVE".equals(sub.getStatus()) || "EXPIRED".equals(sub.getStatus())) {
                    assinatura.setStatus(StatusAssinatura.VENCIDA);
                } else if ("ACTIVE".equals(sub.getStatus())) {
                    if (assinatura.getStatus() == StatusAssinatura.VENCIDA) {
                        assinatura.setStatus(StatusAssinatura.ATIVA);
                    }
                }
                assinaturaRepository.save(assinatura);
            }
        }
        case "SUBSCRIPTION_DELETED" -> {
            assinatura.setStatus(StatusAssinatura.CANCELADA);
            assinatura.setDtCancelamento(LocalDateTime.now());
            assinaturaRepository.save(assinatura);
            log.info("Assinatura cancelada via webhook - org: {}", assinatura.getOrganizacao().getId());
        }
        case "SUBSCRIPTION_EXPIRED" -> {
            assinatura.setStatus(StatusAssinatura.VENCIDA);
            assinaturaRepository.save(assinatura);
            log.info("Assinatura expirada via webhook - org: {}", assinatura.getOrganizacao().getId());
        }
        case "SUBSCRIPTION_RENEWED" -> {
            assinatura.setStatus(StatusAssinatura.ATIVA);
            assinaturaRepository.save(assinatura);
            log.info("Assinatura renovada via webhook - org: {}", assinatura.getOrganizacao().getId());
        }
        default -> log.info("Evento de assinatura Asaas não tratado: {}", event);
    }
}
```

---

### 3.7 Controle de Cupom Recorrente com Validade

**Problema Atual:**

Cupons do tipo RECORRENTE são aplicados ao criar a assinatura no Asaas, mas não há mecanismo para reverter o desconto quando o cupom expira ou atinge um limite de meses.

**Solução Proposta:**

Adicionar campo na entidade `CupomDesconto`:

```java
private Integer mesesRecorrencia; // null = infinito, 3 = desconto por 3 meses
```

No webhook de pagamento confirmado ou no scheduler, verificar:

```java
// Dentro de confirmarPagamento(), após confirmar pagamento:
if (assinatura.getCupom() != null 
        && assinatura.getCupom().getTipoAplicacao() == TipoAplicacaoCupom.RECORRENTE
        && assinatura.getCupom().getMesesRecorrencia() != null) {
    
    // Contar quantas cobranças já foram pagas com este cupom
    long cobrancasPagas = cupomUtilizacaoRepository.countByCupomIdAndAssinaturaId(
            assinatura.getCupom().getId(), assinatura.getId());
    
    if (cobrancasPagas >= assinatura.getCupom().getMesesRecorrencia()) {
        // Cupom expirou — atualizar Asaas para valor cheio
        BigDecimal valorCheio = assinatura.getCicloCobranca() == CicloCobranca.ANUAL
                ? assinatura.getPlanoBellory().getPrecoAnual()
                : assinatura.getPlanoBellory().getPrecoMensal();
        
        assasClient.atualizarAssinatura(
                assinatura.getAssasSubscriptionId(),
                AssasSubscriptionRequest.builder().value(valorCheio).build()
        );
        
        assinatura.setCupom(null);
        assinatura.setValorDesconto(null);
        assinatura.setCupomCodigo(null);
        assinaturaRepository.save(assinatura);
        
        log.info("Cupom RECORRENTE expirado após {} meses - Asaas atualizado para valor cheio: {} - org: {}",
                cobrancasPagas, valorCheio, assinatura.getOrganizacao().getId());
    }
}
```

---

### 3.8 Otimizar Scheduler de Inadimplentes

**Problema Atual:**

O método `verificarInadimplentes()` busca TODAS as assinaturas ativas e faz uma chamada ao Asaas para cada uma. Com 1.000 clientes, são 1.000 chamadas HTTP a cada execução.

**Solução Proposta:**

```java
@Transactional
public void verificarInadimplentes() {
    if (!assasClient.isConfigurado()) return;

    // OPÇÃO 1: Usar API de listagem de pagamentos com filtro
    // Buscar todos os pagamentos OVERDUE de uma vez
    try {
        AssasPaymentListResponse overduePayments = assasClient.buscarPagamentosPorStatus("OVERDUE");
        if (overduePayments != null && overduePayments.getData() != null) {
            Set<String> subscriptionsOverdue = overduePayments.getData().stream()
                    .map(AssasPaymentResponse::getSubscription)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            // Buscar assinaturas locais correspondentes em batch
            List<Assinatura> assinaturas = assinaturaRepository
                    .findByAssasSubscriptionIdInAndStatus(subscriptionsOverdue, StatusAssinatura.ATIVA);
            
            for (Assinatura assinatura : assinaturas) {
                assinatura.setStatus(StatusAssinatura.VENCIDA);
                assinaturaRepository.save(assinatura);
                log.info("Inadimplente detectado - org: {}", assinatura.getOrganizacao().getId());
            }
        }
    } catch (AssasApiException e) {
        log.error("Erro ao verificar inadimplentes via API Asaas: {}", e.getMessage());
    }
}
```

Adicionar no `AssasClient`:

```java
public AssasPaymentListResponse buscarPagamentosPorStatus(String status) {
    verificarConfiguracao();
    String url = assasApiUrl + "/v3/payments?status=" + status + "&limit=100";
    HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
    ResponseEntity<AssasPaymentListResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, AssasPaymentListResponse.class);
    return response.getBody();
}
```

---

### 3.9 Adiar Criação do Cliente no Asaas

**Problema Atual:**

O método `criarAssinaturaTrial()` cria o cliente no Asaas imediatamente. Se o usuário nunca sair do trial, esse cliente fica órfão no Asaas.

**Solução Proposta:**

```java
// REMOVER do criarAssinaturaTrial():
// try {
//     AssasCustomerResponse customer = assasClient.criarCliente(...);
//     assinatura.setAssasCustomerId(customer.getId());
// } catch ...

// MANTER APENAS no escolherPlano() (já existe como fallback):
if (assinatura.getAssasCustomerId() == null) {
    Organizacao org = assinatura.getOrganizacao();
    AssasCustomerResponse customer = assasClient.criarCliente(
            AssasCustomerRequest.builder()
                    .name(org.getNomeFantasia())
                    .cpfCnpj(org.getCnpj())
                    .email(org.getEmailPrincipal())
                    .phone(org.getTelefone1())
                    .build()
    );
    assinatura.setAssasCustomerId(customer.getId());
}
```

---

## 4. Novos Status Propostos

Adicionar ao enum `StatusAssinatura`:

```java
public enum StatusAssinatura {
    TRIAL,
    AGUARDANDO_PAGAMENTO,   // NOVO: boleto/pix aguardando primeiro pagamento
    ATIVA,
    UPGRADE_PENDENTE,       // NOVO: cobrança avulsa de pro-rata gerada, aguardando pagamento
    DOWNGRADE_AGENDADO,     // NOVO: downgrade solicitado, aguardando efetivação
    VENCIDA,
    CANCELADA,
    SUSPENSA
}
```

Atualizar `isBloqueada()`:

```java
public boolean isBloqueada() {
    return switch (status) {
        case TRIAL -> isTrialExpirado();
        case AGUARDANDO_PAGAMENTO -> true;  // Bloquear até confirmar pagamento
        case UPGRADE_PENDENTE -> false;     // Mantém acesso do plano atual
        case DOWNGRADE_AGENDADO -> false;   // Mantém acesso do plano atual
        case ATIVA -> false;
        case VENCIDA, CANCELADA, SUSPENSA -> true;
    };
}
```

---

## 5. Resumo: Dados Local vs Asaas

| Dado | Onde Manter | Justificativa |
|------|-------------|---------------|
| Customer ID | Local (ref) | Referência para vincular ao Asaas |
| Subscription ID | Local (ref) | Referência para vincular ao Asaas |
| Status da assinatura | Ambos | Cache local atualizado por webhook; Asaas é a fonte |
| Plano atual | Local | Informação de negócio local |
| Ciclo de cobrança | Local (ref) | Informação de negócio local |
| Valor da assinatura | Asaas apenas | Asaas gerencia cobranças; local consulta PlanoBellory |
| Próximo vencimento | Asaas apenas | Consultar via API com cache (TTL 5min) |
| Lista de cobranças | Asaas apenas | Consultar via API; não duplicar |
| Status de pagamentos | Asaas apenas | Atualizado por webhook, consultado via API |
| Dados de cartão | Asaas apenas | Nunca armazenar localmente (PCI compliance) |
| Cupom aplicado | Local | Lógica de negócio local |
| Crédito pro-rata | Local | Controle de crédito para downgrade |
| Logs de webhook | Local | Auditoria e idempotência |

---

## 6. Prioridade de Implementação

### Fase 1 — Crítico (Implementar imediatamente)

- Implementar pro-rata em upgrade/downgrade com cobrança avulsa
- Eliminar janela de inconsistência na troca de plano (criar antes de cancelar)
- Criar status `AGUARDANDO_PAGAMENTO` para boleto/pix

### Fase 2 — Alto (Próximo sprint)

- Migrar `dtProximoVencimento` para consulta ao Asaas
- Remover `valorMensal`/`valorAnual` da entidade Assinatura
- Implementar webhooks de assinatura (`SUBSCRIPTION_*`)

### Fase 3 — Médio (Backlog priorizado)

- Controle de validade de cupom recorrente
- Otimizar scheduler de inadimplentes (lotes + paginação)
- Melhorar estratégia de cache com TTL e invalidação por webhook

### Fase 4 — Baixo (Melhorias contínuas)

- Adiar criação do cliente no Asaas para o momento da conversão
- Migrar controller para `@ControllerAdvice`
- Eliminar tabela `CobrancaPlataforma` para recorrentes

---

## 7. Considerações Finais

O princípio central desta refatoração é: **se o Asaas já gerencia a informação, não duplique no banco local**. O sistema local deve manter apenas referências (IDs), cache de status (atualizado por webhooks) e dados de negócio que não existem no Asaas (planos, cupons, créditos de pro-rata).

A implementação do pro-rata é a mudança mais impactante para o negócio, pois garante que o cliente pague corretamente ao trocar de plano. Sem isso, upgrades geram prejuízo (cliente não paga a diferença) e downgrades geram insatisfação (cliente não recebe crédito).

Recomenda-se implementar cada fase com testes de integração que simulem cenários de webhook e verificar o comportamento end-to-end com o sandbox do Asaas antes de ir para produção.
