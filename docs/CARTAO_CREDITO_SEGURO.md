# Pagamento com Cartao de Credito - Guia de Implementacao Frontend

## Resumo da Mudanca

Os dados do cartao de credito **NAO sao mais armazenados** no banco de dados do Bellory.
Agora os dados sao enviados diretamente ao Asaas (gateway de pagamento) durante a criacao da assinatura.

**Antes:** Numero completo do cartao, CVV e data de validade eram salvos na tabela `cartao_credito`.
**Agora:** Apenas ultimos 4 digitos, bandeira e token do Asaas sao armazenados (para exibicao e recobranca).

---

## Fluxo de Pagamento com Cartao

Existem **duas opcoes** de integracao. Recomendamos a **Opcao 1** (tokenizacao via Asaas.js) por ser a mais segura.

### Opcao 1: Tokenizacao via Asaas.js (RECOMENDADA)

Nessa opcao, os dados do cartao **nunca passam pelo backend do Bellory**. O frontend tokeniza direto com o Asaas.

#### Passo 1 - Incluir o Asaas.js

```html
<script src="https://www.asaas.com/asaas.js"></script>
```

#### Passo 2 - Tokenizar o Cartao

```javascript
// Inicializar Asaas.js
const asaas = new Asaas({
  accessToken: 'SUA_CHAVE_PUBLICA_ASAAS' // chave publica, nao a secreta
});

async function tokenizarCartao(dadosCartao) {
  try {
    const response = await asaas.creditCard.tokenize({
      customer: customerIdAsaas, // ID do cliente no Asaas (retornado pelo backend)
      creditCard: {
        holderName: dadosCartao.nomePortador,
        number: dadosCartao.numero,
        expiryMonth: dadosCartao.mesValidade,
        expiryYear: dadosCartao.anoValidade,
        ccv: dadosCartao.cvv
      },
      creditCardHolderInfo: {
        name: dadosCartao.nomePortador,
        email: organizacao.email,
        cpfCnpj: organizacao.cnpj,
        postalCode: organizacao.cep,
        addressNumber: organizacao.numero,
        phone: organizacao.telefone
      }
    });

    return response.creditCardToken;
  } catch (error) {
    console.error('Erro ao tokenizar cartao:', error);
    throw error;
  }
}
```

#### Passo 3 - Enviar Token ao Backend

```javascript
async function escolherPlano(planoCodigo, ciclo, dadosCartao) {
  // Primeiro, tokenizar o cartao com Asaas.js
  const token = await tokenizarCartao(dadosCartao);

  // Enviar ao backend apenas o token (sem dados sensiveis)
  const response = await fetch('/api/v1/assinatura/escolher-plano', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    },
    body: JSON.stringify({
      planoCodigo: planoCodigo,
      cicloCobranca: ciclo,           // "MENSAL" ou "ANUAL"
      formaPagamento: "CARTAO_CREDITO",
      codigoCupom: cupom || null,
      creditCardToken: token           // Token seguro do Asaas
    })
  });

  return response.json();
}
```

---

### Opcao 2: Envio Direto pelo Backend (alternativa)

Nessa opcao, os dados do cartao passam pelo backend mas **NAO sao armazenados** - sao encaminhados diretamente ao Asaas na mesma requisicao.

```javascript
async function escolherPlanoComCartao(planoCodigo, ciclo, dadosCartao) {
  const response = await fetch('/api/v1/assinatura/escolher-plano', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    },
    body: JSON.stringify({
      planoCodigo: planoCodigo,
      cicloCobranca: ciclo,
      formaPagamento: "CARTAO_CREDITO",
      codigoCupom: cupom || null,
      creditCard: {
        holderName: dadosCartao.nomePortador,
        number: dadosCartao.numero,        // Numero completo (sem formatacao)
        expiryMonth: dadosCartao.mesValidade, // "01" a "12"
        expiryYear: dadosCartao.anoValidade,  // "2027"
        ccv: dadosCartao.cvv                  // "123" ou "1234"
      }
    })
  });

  return response.json();
}
```

---

## Estrutura do Request - `POST /api/v1/assinatura/escolher-plano`

```json
{
  "planoCodigo": "PRO",
  "cicloCobranca": "MENSAL",
  "formaPagamento": "CARTAO_CREDITO",
  "codigoCupom": null,

  // OPCAO 1 - Token (recomendado)
  "creditCardToken": "tok_xxxxxxxxxxxx",

  // OPCAO 2 - Dados diretos (alternativa)
  "creditCard": {
    "holderName": "JOAO DA SILVA",
    "number": "4111111111111111",
    "expiryMonth": "12",
    "expiryYear": "2027",
    "ccv": "123"
  }
}
```

> **IMPORTANTE:** Envie `creditCardToken` OU `creditCard`, nunca ambos. Se ambos forem enviados, o token tem prioridade.

---

## Endpoints Afetados

Os dados do cartao se aplicam a **todos os endpoints** que criam assinaturas no Asaas:

| Endpoint | Descricao | Cartao Obrigatorio? |
|---|---|---|
| `POST /api/v1/assinatura/escolher-plano` | Primeira assinatura | Sim, se `formaPagamento = CARTAO_CREDITO` |
| `POST /api/v1/assinatura/trocar-plano` | Upgrade/downgrade | Sim, se `formaPagamento = CARTAO_CREDITO` |
| `POST /api/v1/assinatura/reativar` | Reativar assinatura | Sim, se `formaPagamento = CARTAO_CREDITO` |

Para PIX e BOLETO, nenhuma mudanca - continuam funcionando como antes.

---

## Formulario de Cartao - Componente Frontend

### Campos Necessarios

| Campo | Label | Tipo | Validacao | Exemplo |
|---|---|---|---|---|
| `holderName` / `nomePortador` | Nome no Cartao | text | Obrigatorio, min 3 chars | JOAO DA SILVA |
| `number` / `numero` | Numero do Cartao | text (mascarado) | 13-19 digitos | 4111 1111 1111 1111 |
| `expiryMonth` / `mesValidade` | Mes Validade | select | 01-12 | 12 |
| `expiryYear` / `anoValidade` | Ano Validade | select | Ano atual + 10 | 2027 |
| `ccv` / `cvv` | Codigo de Seguranca | password | 3-4 digitos | 123 |

### Exemplo React/Next.js

```tsx
interface CreditCardForm {
  nomePortador: string;
  numero: string;
  mesValidade: string;
  anoValidade: string;
  cvv: string;
}

function CartaoCreditoForm({ onSubmit }: { onSubmit: (data: CreditCardForm) => void }) {
  const [form, setForm] = useState<CreditCardForm>({
    nomePortador: '',
    numero: '',
    mesValidade: '',
    anoValidade: '',
    cvv: ''
  });

  const handleChange = (field: keyof CreditCardForm, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }));
  };

  const formatCardNumber = (value: string) => {
    const digits = value.replace(/\D/g, '').slice(0, 16);
    return digits.replace(/(\d{4})(?=\d)/g, '$1 ');
  };

  const detectBrand = (number: string): string => {
    const n = number.replace(/\D/g, '');
    if (/^4/.test(n)) return 'visa';
    if (/^5[1-5]/.test(n)) return 'mastercard';
    if (/^3[47]/.test(n)) return 'amex';
    if (/^(636368|438935|504175|451416|636297)/.test(n)) return 'elo';
    if (/^(606282|3841)/.test(n)) return 'hipercard';
    return '';
  };

  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(form); }}>
      <div>
        <label>Nome no Cartao</label>
        <input
          type="text"
          value={form.nomePortador}
          onChange={(e) => handleChange('nomePortador', e.target.value.toUpperCase())}
          placeholder="COMO ESTA NO CARTAO"
          required
        />
      </div>

      <div>
        <label>Numero do Cartao</label>
        <input
          type="text"
          value={formatCardNumber(form.numero)}
          onChange={(e) => handleChange('numero', e.target.value.replace(/\D/g, ''))}
          placeholder="0000 0000 0000 0000"
          maxLength={19}
          required
        />
        {form.numero && <span className="card-brand">{detectBrand(form.numero)}</span>}
      </div>

      <div className="row">
        <div>
          <label>Validade</label>
          <select
            value={form.mesValidade}
            onChange={(e) => handleChange('mesValidade', e.target.value)}
            required
          >
            <option value="">Mes</option>
            {Array.from({ length: 12 }, (_, i) => {
              const m = String(i + 1).padStart(2, '0');
              return <option key={m} value={m}>{m}</option>;
            })}
          </select>

          <select
            value={form.anoValidade}
            onChange={(e) => handleChange('anoValidade', e.target.value)}
            required
          >
            <option value="">Ano</option>
            {Array.from({ length: 10 }, (_, i) => {
              const y = String(new Date().getFullYear() + i);
              return <option key={y} value={y}>{y}</option>;
            })}
          </select>
        </div>

        <div>
          <label>CVV</label>
          <input
            type="password"
            value={form.cvv}
            onChange={(e) => handleChange('cvv', e.target.value.replace(/\D/g, '').slice(0, 4))}
            placeholder="123"
            maxLength={4}
            required
          />
        </div>
      </div>

      <button type="submit">Confirmar Pagamento</button>
    </form>
  );
}
```

---

## Validacoes no Frontend

### Antes de Enviar

1. **Numero do cartao:** Validar com algoritmo de Luhn
2. **Data de validade:** Nao pode estar expirado
3. **CVV:** 3 digitos (Visa/Master/Elo) ou 4 digitos (Amex)
4. **Nome:** Pelo menos 3 caracteres

```javascript
function validarLuhn(numero) {
  const digits = numero.replace(/\D/g, '');
  let soma = 0;
  let dobrar = false;

  for (let i = digits.length - 1; i >= 0; i--) {
    let n = parseInt(digits[i], 10);
    if (dobrar) {
      n *= 2;
      if (n > 9) n -= 9;
    }
    soma += n;
    dobrar = !dobrar;
  }

  return soma % 10 === 0;
}

function validarCartaoExpirado(mes, ano) {
  const agora = new Date();
  const validade = new Date(parseInt(ano), parseInt(mes), 0); // ultimo dia do mes
  return validade >= agora;
}
```

---

## Tratamento de Erros

### Erros Comuns do Backend

| Codigo | Mensagem | Acao no Frontend |
|---|---|---|
| 400 | `Dados do cartao de credito ou token sao obrigatorios para pagamento com cartao` | Exibir formulario do cartao |
| 400 | `Cartao recusado` (do Asaas) | Exibir msg: "Cartao recusado. Verifique os dados ou use outro cartao." |
| 400 | `Fundos insuficientes` (do Asaas) | Exibir msg: "Saldo insuficiente. Tente outro cartao." |
| 500 | Erro generico | Exibir msg: "Erro ao processar pagamento. Tente novamente." |

### Exemplo de Tratamento

```javascript
try {
  const result = await escolherPlano(plano, ciclo, dadosCartao);
  if (result.success) {
    // Redirecionar para pagina de sucesso
    router.push('/assinatura/sucesso');
  } else {
    showError(result.message);
  }
} catch (error) {
  if (error.message.includes('cartao')) {
    showError('Verifique os dados do cartao e tente novamente.');
  } else {
    showError('Erro ao processar pagamento. Tente novamente em instantes.');
  }
}
```

---

## Seguranca - Boas Praticas

1. **NUNCA** armazene dados do cartao no localStorage, sessionStorage ou cookies
2. **NUNCA** logue dados do cartao no console em producao
3. Use `type="password"` para o campo CVV
4. Limpe os dados do cartao da memoria apos o envio
5. Use HTTPS obrigatoriamente
6. Prefira a **Opcao 1** (tokenizacao via Asaas.js) - os dados nunca passam pelo seu servidor
7. Se usar **Opcao 2**, garanta que os dados trafegam apenas via HTTPS e nao sao logados

```javascript
// Limpar dados sensiveis apos envio
function limparDadosCartao(form) {
  form.numero = '';
  form.cvv = '';
  form.mesValidade = '';
  form.anoValidade = '';
}
```

---

## Exibicao de Cartao Salvo

Apos a assinatura com cartao, o backend armazena apenas dados seguros para exibicao:

```json
{
  "ultimosQuatroDigitos": "1111",
  "bandeira": "VISA",
  "nomePortador": "JOAO DA SILVA"
}
```

Use esses dados para exibir o cartao cadastrado:

```tsx
function CartaoSalvo({ cartao }) {
  return (
    <div className="cartao-salvo">
      <BandeiraIcon tipo={cartao.bandeira} />
      <span>**** **** **** {cartao.ultimosQuatroDigitos}</span>
      <span>{cartao.nomePortador}</span>
    </div>
  );
}
```

---

## Mudanca de Forma de Pagamento

Se o usuario quiser trocar de PIX/Boleto para Cartao (ou vice-versa), use o endpoint `trocar-plano` com o mesmo plano mas diferente `formaPagamento`:

```javascript
// Trocar para cartao de credito
await fetch('/api/v1/assinatura/trocar-plano', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
  body: JSON.stringify({
    planoCodigo: planoAtual.codigo,    // mesmo plano
    cicloCobranca: cicloAtual,         // mesmo ciclo
    formaPagamento: "CARTAO_CREDITO",  // nova forma
    creditCard: { /* dados do cartao */ }
  })
});
```
