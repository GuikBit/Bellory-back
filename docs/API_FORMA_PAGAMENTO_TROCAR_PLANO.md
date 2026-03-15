# API - Endpoints de Forma de Pagamento e Troca de Plano

Base URL: `/api/v1/assinatura`

---

## 1. GET /forma-pagamento

Retorna a forma de pagamento atual da assinatura da organização. Se for cartão de crédito, retorna os dados seguros do cartão salvo no Asaas (nunca retorna número completo ou CVV).

### Headers

| Header          | Tipo   | Obrigatório | Descrição          |
|-----------------|--------|-------------|--------------------|
| `Authorization` | String | Sim         | `Bearer {jwt_token}` |

### Request

Sem body — a organização é identificada automaticamente pelo JWT.

### Response — Sucesso (200)

```json
{
  "success": true,
  "dados": {
    "formaPagamento": "CARTAO_CREDITO",
    "possuiFormaPagamento": true,
    "ultimosQuatroDigitos": "1234",
    "bandeira": "VISA",
    "nomePortador": null,
    "creditCardToken": "a8f7d3e2-1b9c-4a5d-..."
  }
}
```

### Campos da Response

| Campo                  | Tipo    | Nullable | Descrição |
|------------------------|---------|----------|-----------|
| `formaPagamento`       | String  | Sim      | `"PIX"`, `"BOLETO"` ou `"CARTAO_CREDITO"`. `null` se nunca escolheu plano. |
| `possuiFormaPagamento` | Boolean | Não      | `true` se já tem forma de pagamento definida. |
| `ultimosQuatroDigitos` | String  | Sim      | Últimos dígitos do cartão (mascarado pelo Asaas). Presente apenas quando `formaPagamento = "CARTAO_CREDITO"`. |
| `bandeira`             | String  | Sim      | Bandeira do cartão (`"VISA"`, `"MASTERCARD"`, `"ELO"`, etc.). Presente apenas quando `formaPagamento = "CARTAO_CREDITO"`. |
| `nomePortador`         | String  | Sim      | Nome do portador do cartão, se disponível. |
| `creditCardToken`      | String  | Sim      | Token do Asaas para reusar o cartão sem reenviar dados completos. Presente apenas quando `formaPagamento = "CARTAO_CREDITO"`. |

### Cenários de Response

**Cartão de crédito salvo:**
```json
{
  "success": true,
  "dados": {
    "formaPagamento": "CARTAO_CREDITO",
    "possuiFormaPagamento": true,
    "ultimosQuatroDigitos": "4587",
    "bandeira": "MASTERCARD",
    "creditCardToken": "a8f7d3e2-1b9c-4a5d-8e7f-2c3b4a5d6e7f"
  }
}
```

**PIX como forma de pagamento:**
```json
{
  "success": true,
  "dados": {
    "formaPagamento": "PIX",
    "possuiFormaPagamento": true
  }
}
```

**Boleto como forma de pagamento:**
```json
{
  "success": true,
  "dados": {
    "formaPagamento": "BOLETO",
    "possuiFormaPagamento": true
  }
}
```

**Sem forma de pagamento (trial ou nunca escolheu plano):**
```json
{
  "success": true,
  "dados": {
    "formaPagamento": null,
    "possuiFormaPagamento": false
  }
}
```

### Erros

| Código | Cenário |
|--------|---------|
| 401    | Token JWT inválido ou expirado |
| 500    | Erro interno (ex: falha de comunicação com Asaas — retorna `possuiFormaPagamento: true` sem dados do cartão) |

---

## 2. POST /trocar-plano

Realiza upgrade, downgrade ou troca de ciclo de cobrança do plano atual. Calcula automaticamente o valor pro-rata (proporcional aos dias restantes do ciclo).

### Headers

| Header          | Tipo   | Obrigatório | Descrição          |
|-----------------|--------|-------------|--------------------|
| `Authorization` | String | Sim         | `Bearer {jwt_token}` |
| `Content-Type`  | String | Sim         | `application/json` |

### Request Body

```json
{
  "planoCodigo": "profissional",
  "cicloCobranca": "MENSAL",
  "formaPagamento": "CARTAO_CREDITO",
  "codigoCupom": "DESCONTO10",
  "creditCard": {
    "holderName": "GUILHERME SILVA",
    "number": "5162306219378829",
    "expiryMonth": "05",
    "expiryYear": "2028",
    "ccv": "318"
  },
  "creditCardToken": null
}
```

### Campos do Request

| Campo             | Tipo          | Obrigatório | Descrição |
|-------------------|---------------|-------------|-----------|
| `planoCodigo`     | String        | **Sim**     | Código do plano destino (ex: `"basico"`, `"profissional"`, `"empresarial"`). |
| `cicloCobranca`   | String        | **Sim**     | `"MENSAL"` ou `"ANUAL"`. |
| `formaPagamento`  | String        | **Sim**     | `"PIX"`, `"BOLETO"` ou `"CARTAO_CREDITO"`. |
| `codigoCupom`     | String        | Não         | Código de cupom de desconto (opcional). |
| `creditCard`      | CreditCardDTO | Condicional | Dados do cartão. **Obrigatório** quando `formaPagamento = "CARTAO_CREDITO"` e não tem `creditCardToken`. |
| `creditCardToken` | String        | Condicional | Token do cartão salvo (obtido via `GET /forma-pagamento`). **Alternativa** ao envio de `creditCard`. |

### CreditCardDTO (objeto `creditCard`)

| Campo        | Tipo   | Obrigatório | Validação           | Descrição |
|--------------|--------|-------------|---------------------|-----------|
| `holderName` | String | Sim         | Não pode ser vazio  | Nome impresso no cartão. |
| `number`     | String | Sim         | Não pode ser vazio  | Número completo do cartão. |
| `expiryMonth`| String | Sim         | Regex: `01` a `12`  | Mês de validade (2 dígitos). |
| `expiryYear` | String | Sim         | Regex: 4 dígitos    | Ano de validade (4 dígitos). |
| `ccv`        | String | Sim         | Regex: 3 ou 4 dígitos | Código de segurança. |

> **Importante:** Os dados do cartão são enviados diretamente ao Asaas e **nunca são armazenados** no banco de dados da Bellory.

### Lógica de Negócio

O endpoint calcula automaticamente o **pro-rata** (valor proporcional) com base nos dias restantes do ciclo atual:

```
valorAtualProporcional = (valorPlanoAtual / diasTotalCiclo) * diasRestantesCiclo
valorNovoProporcional  = (valorPlanoNovo / diasTotalCiclo) * diasRestantesCiclo
proRata = valorNovoProporcional - valorAtualProporcional
```

Com base no resultado do pro-rata:

| Cenário | proRata | Comportamento |
|---------|---------|---------------|
| **Upgrade** | `> 0` | Cria cobrança avulsa no Asaas. Status → `UPGRADE_PENDENTE`. Plano muda após confirmação do pagamento (webhook). |
| **Downgrade** | `< 0` | Crédito salvo em `creditoProRata`. Cancela assinatura antiga e cria nova no Asaas. Status mantém `ATIVA`. |
| **Mesmo valor** | `= 0` | Troca direta. Cancela assinatura antiga e cria nova no Asaas. Status mantém `ATIVA`. |

### Response — Sucesso (200)

```json
{
  "success": true,
  "message": "Plano alterado com sucesso",
  "dados": {
    "id": 42,
    "organizacaoId": 15,
    "organizacaoNome": "Minha Empresa",
    "planoBelloryId": 3,
    "planoNome": "Profissional",
    "planoCodigo": "profissional",
    "status": "UPGRADE_PENDENTE",
    "cicloCobranca": "MENSAL",
    "formaPagamento": "CARTAO_CREDITO",
    "dtInicioTrial": "2025-01-01T00:00:00",
    "dtFimTrial": "2025-01-15T00:00:00",
    "dtInicio": "2025-01-15T10:30:00",
    "dtProximoVencimento": "2025-02-15T10:30:00",
    "dtCancelamento": null,
    "creditoProRata": null,
    "planoAnteriorCodigo": "basico",
    "assasCustomerId": "cus_abc123",
    "assasSubscriptionId": "sub_xyz789",
    "cupomCodigo": null,
    "valorDesconto": null,
    "dtCriacao": "2025-01-01T00:00:00"
  }
}
```

### Campos da Response (`AssinaturaResponseDTO`)

| Campo                  | Tipo         | Nullable | Descrição |
|------------------------|--------------|----------|-----------|
| `id`                   | Long         | Não      | ID da assinatura. |
| `organizacaoId`        | Long         | Não      | ID da organização. |
| `organizacaoNome`      | String       | Sim      | Nome da organização. |
| `planoBelloryId`       | Long         | Não      | ID do plano (já é o plano destino no upgrade). |
| `planoNome`            | String       | Sim      | Nome do plano. |
| `planoCodigo`          | String       | Sim      | Código do plano. |
| `status`               | String       | Não      | Status após a troca (ver tabela abaixo). |
| `cicloCobranca`        | String       | Não      | `"MENSAL"` ou `"ANUAL"`. |
| `formaPagamento`       | String       | Sim      | `"PIX"`, `"BOLETO"` ou `"CARTAO_CREDITO"`. |
| `dtInicioTrial`        | DateTime     | Sim      | Data de início do trial. |
| `dtFimTrial`           | DateTime     | Sim      | Data de fim do trial. |
| `dtInicio`             | DateTime     | Sim      | Data de início da assinatura paga. |
| `dtProximoVencimento`  | DateTime     | Sim      | Próximo vencimento do ciclo. |
| `dtCancelamento`       | DateTime     | Sim      | Data de cancelamento (se cancelado). |
| `creditoProRata`       | BigDecimal   | Sim      | Crédito do pro-rata (presente no downgrade). |
| `planoAnteriorCodigo`  | String       | Sim      | Código do plano anterior (presente em upgrade/downgrade). |
| `assasCustomerId`      | String       | Sim      | ID do cliente no Asaas. |
| `assasSubscriptionId`  | String       | Sim      | ID da assinatura no Asaas. |
| `cupomCodigo`          | String       | Sim      | Código do cupom aplicado. |
| `valorDesconto`        | BigDecimal   | Sim      | Valor do desconto do cupom. |
| `dtCriacao`            | DateTime     | Não      | Data de criação do registro. |

### Status retornados por cenário

| Cenário       | `status` retornado    | Acesso bloqueado? | Próximo passo |
|---------------|-----------------------|-------------------|---------------|
| **Upgrade**   | `UPGRADE_PENDENTE`    | Não (mantém plano atual) | Aguardar webhook de confirmação de pagamento. |
| **Downgrade** | `ATIVA`               | Não | Plano já foi trocado. Crédito salvo em `creditoProRata`. |
| **Mesmo valor** | `ATIVA`             | Não | Plano já foi trocado imediatamente. |

### Erros

| Código | Cenário | Exemplo de mensagem |
|--------|---------|---------------------|
| 400    | Assinatura não está ativa | `"Apenas assinaturas ativas podem trocar de plano"` |
| 400    | Plano não encontrado | `"Plano nao encontrado: enterprise"` |
| 400    | Plano inativo | `"Plano indisponivel"` |
| 400    | Mesmo plano e ciclo | `"Voce ja esta neste plano com este ciclo de cobranca"` |
| 400    | Cupom inválido | `"Cupom invalido: Cupom expirado"` |
| 400    | Erro ao processar upgrade | `"Erro ao processar upgrade. Tente novamente."` |
| 401    | Token JWT inválido | — |
| 500    | Erro interno | `"Erro interno: ..."` |

---

## 3. POST /preview-troca-plano

Preview **somente leitura** dos valores pro-rata antes de confirmar a troca. Nenhuma alteração é feita na assinatura ou no Asaas. Use este endpoint para exibir ao usuário o resumo financeiro da troca antes de chamar `/trocar-plano`.

### Headers

| Header          | Tipo   | Obrigatório | Descrição          |
|-----------------|--------|-------------|--------------------|
| `Authorization` | String | Sim         | `Bearer {jwt_token}` |
| `Content-Type`  | String | Sim         | `application/json` |

### Request Body

Usa o mesmo DTO do `/trocar-plano` (`EscolherPlanoDTO`). Os campos `creditCard`, `creditCardToken` e `codigoCupom` são **ignorados** — apenas `planoCodigo`, `cicloCobranca` e `formaPagamento` são usados.

```json
{
  "planoCodigo": "profissional",
  "cicloCobranca": "MENSAL",
  "formaPagamento": "CARTAO_CREDITO"
}
```

### Campos do Request

| Campo             | Tipo   | Obrigatório | Descrição |
|-------------------|--------|-------------|-----------|
| `planoCodigo`     | String | **Sim**     | Código do plano destino (ex: `"basico"`, `"profissional"`, `"empresarial"`). |
| `cicloCobranca`   | String | **Sim**     | `"MENSAL"` ou `"ANUAL"`. |
| `formaPagamento`  | String | **Sim**     | `"PIX"`, `"BOLETO"` ou `"CARTAO_CREDITO"`. Usado apenas para referência, não afeta o cálculo. |
| `codigoCupom`     | String | Ignorado    | — |
| `creditCard`      | Object | Ignorado    | — |
| `creditCardToken` | String | Ignorado    | — |

### Lógica de Cálculo

O cálculo pro-rata é baseado nos dias restantes do ciclo atual:

```
diasRestantesCiclo = dias entre HOJE e dtProximoVencimento
diasTotalCiclo     = 30 (mensal) ou 365 (anual)

valorAtualProporcional = (valorPlanoAtual / diasTotalCiclo) * diasRestantesCiclo
valorNovoProporcional  = (valorPlanoNovo  / diasTotalCiclo) * diasRestantesCiclo

valorProRata = valorNovoProporcional - valorAtualProporcional
```

- `valorProRata > 0` → **Upgrade** (o usuário paga a diferença)
- `valorProRata < 0` → **Downgrade** (o usuário recebe crédito)
- `valorProRata = 0` → **Mesmo valor** (troca direta)

### Response — Sucesso (200)

**Exemplo de Upgrade:**
```json
{
  "success": true,
  "dados": {
    "planoAtualCodigo": "basico",
    "planoAtualNome": "Básico",
    "novoPlanoCodigo": "profissional",
    "novoPlanoNome": "Profissional",
    "cicloCobranca": "MENSAL",
    "valorAtualProporcional": 36.00,
    "valorNovoProporcional": 60.00,
    "valorProRata": 24.00,
    "diasRestantesCiclo": 18,
    "diasTotalCiclo": 30,
    "isUpgrade": true,
    "mensagem": "Upgrade: será gerada uma cobrança avulsa de R$ 24,00 referente aos 18 dias restantes do ciclo."
  }
}
```

**Exemplo de Downgrade:**
```json
{
  "success": true,
  "dados": {
    "planoAtualCodigo": "empresarial",
    "planoAtualNome": "Empresarial",
    "novoPlanoCodigo": "profissional",
    "novoPlanoNome": "Profissional",
    "cicloCobranca": "MENSAL",
    "valorAtualProporcional": 60.00,
    "valorNovoProporcional": 36.00,
    "valorProRata": -24.00,
    "diasRestantesCiclo": 18,
    "diasTotalCiclo": 30,
    "isUpgrade": false,
    "mensagem": "Downgrade: você receberá um crédito de R$ 24,00 aplicado no próximo ciclo."
  }
}
```

**Exemplo de Mesmo Valor (troca de ciclo):**
```json
{
  "success": true,
  "dados": {
    "planoAtualCodigo": "profissional",
    "planoAtualNome": "Profissional",
    "novoPlanoCodigo": "profissional",
    "novoPlanoNome": "Profissional",
    "cicloCobranca": "ANUAL",
    "valorAtualProporcional": 36.00,
    "valorNovoProporcional": 36.00,
    "valorProRata": 0.00,
    "diasRestantesCiclo": 18,
    "diasTotalCiclo": 30,
    "isUpgrade": false,
    "mensagem": "Troca de ciclo sem custo adicional."
  }
}
```

### Campos da Response (`ProRataPreviewDTO`)

| Campo                    | Tipo       | Nullable | Descrição |
|--------------------------|------------|----------|-----------|
| `planoAtualCodigo`       | String     | Não      | Código do plano atual. |
| `planoAtualNome`         | String     | Não      | Nome do plano atual. |
| `novoPlanoCodigo`        | String     | Não      | Código do plano destino. |
| `novoPlanoNome`          | String     | Não      | Nome do plano destino. |
| `cicloCobranca`          | String     | Não      | Ciclo solicitado (`"MENSAL"` ou `"ANUAL"`). |
| `valorAtualProporcional` | BigDecimal | Não      | Crédito proporcional do plano atual (dias restantes). |
| `valorNovoProporcional`  | BigDecimal | Não      | Custo proporcional do plano novo (dias restantes). |
| `valorProRata`           | BigDecimal | Não      | Diferença. Positivo = cobrança (upgrade). Negativo = crédito (downgrade). Zero = mesmo valor. |
| `diasRestantesCiclo`     | Long       | Não      | Dias restantes até o próximo vencimento. |
| `diasTotalCiclo`         | Long       | Não      | Total de dias do ciclo (30 para mensal, 365 para anual). |
| `isUpgrade`              | Boolean    | Não      | `true` se upgrade (cobrança), `false` se downgrade ou mesmo valor (crédito). |
| `mensagem`               | String     | Não      | Mensagem legível para exibir diretamente ao usuário. |

### Erros

| Código | Cenário | Exemplo de mensagem |
|--------|---------|---------------------|
| 400    | Assinatura não está ativa | `"Apenas assinaturas ativas podem trocar de plano"` |
| 400    | Plano não encontrado | `"Plano nao encontrado: enterprise"` |
| 400    | Plano inativo | `"Plano indisponivel"` |
| 400    | Mesmo plano e ciclo | `"Voce ja esta neste plano com este ciclo de cobranca"` |
| 401    | Token JWT inválido | — |
| 500    | Erro interno | `"Erro interno: ..."` |

---

## 4. Fluxo Recomendado no Frontend

### Troca de plano com cartão salvo

```
1. GET /forma-pagamento
   → Verifica se tem cartão salvo (possuiFormaPagamento + creditCardToken)

2. Usuário seleciona novo plano

3. POST /preview-troca-plano
   → Exibe resumo com valores pro-rata

4. Usuário confirma
   → Se tem cartão salvo e quer usar: enviar creditCardToken
   → Se quer usar outro cartão: enviar creditCard com dados novos
   → Se PIX/BOLETO: enviar formaPagamento correspondente

5. POST /trocar-plano
   → Executa a troca
```

### Diagrama de decisão do cartão

```
GET /forma-pagamento
  │
  ├─ possuiFormaPagamento: false
  │   → Exibir formulário de pagamento completo
  │
  ├─ formaPagamento: "PIX" ou "BOLETO"
  │   → Exibir: "Forma atual: PIX/Boleto"
  │   → Opção: manter ou alterar
  │
  └─ formaPagamento: "CARTAO_CREDITO"
      │
      ├─ creditCardToken presente
      │   → Exibir: "Cartão •••• {ultimosQuatroDigitos} ({bandeira})"
      │   → Botão: "Usar este cartão" → envia creditCardToken no trocar-plano
      │   → Botão: "Usar outro cartão" → abre formulário de cartão
      │
      └─ creditCardToken ausente (erro ao buscar no Asaas)
          → Exibir formulário de cartão completo
```

### Exemplo de chamada — usando cartão salvo

```typescript
// 1. Buscar forma de pagamento
const formaPag = await api.get('/assinatura/forma-pagamento');

// 2. Preview
const preview = await api.post('/assinatura/preview-troca-plano', {
  planoCodigo: 'profissional',
  cicloCobranca: 'MENSAL',
  formaPagamento: 'CARTAO_CREDITO'
});

// 3. Confirmar com token do cartão salvo
const result = await api.post('/assinatura/trocar-plano', {
  planoCodigo: 'profissional',
  cicloCobranca: 'MENSAL',
  formaPagamento: 'CARTAO_CREDITO',
  creditCardToken: formaPag.dados.creditCardToken
});
```

### Exemplo de chamada — usando cartão novo

```typescript
const result = await api.post('/assinatura/trocar-plano', {
  planoCodigo: 'profissional',
  cicloCobranca: 'MENSAL',
  formaPagamento: 'CARTAO_CREDITO',
  creditCard: {
    holderName: 'GUILHERME SILVA',
    number: '5162306219378829',
    expiryMonth: '05',
    expiryYear: '2028',
    ccv: '318'
  }
});
```

### Exemplo de chamada — PIX

```typescript
const result = await api.post('/assinatura/trocar-plano', {
  planoCodigo: 'profissional',
  cicloCobranca: 'MENSAL',
  formaPagamento: 'PIX'
});
```
