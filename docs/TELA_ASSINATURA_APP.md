# Tela de Gerenciamento de Assinatura - APP (Cliente)

**Data**: 2026-03-09
**Versao Backend**: v2 - Asaas como source of truth

---

## 1. Visao Geral

Tela completa de gerenciamento da assinatura dentro do sistema do cliente (APP). A tela deve ser acessivel pelo menu lateral em **Configuracoes > Plano / Assinatura** e conter todas as informacoes e acoes relacionadas a assinatura do servico.

### Rota sugerida
```
/app/configuracoes/assinatura
```

### Endpoints utilizados nesta tela

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/assinatura/status` | GET | Status completo da assinatura |
| `/api/v1/assinatura/cobrancas` | GET | Historico de cobrancas |
| `/api/v1/assinatura/cobrancas?status=PENDENTE` | GET | Cobrancas pendentes |
| `/api/v1/assinatura/escolher-plano` | POST | Escolher/ativar plano |
| `/api/v1/assinatura/trocar-plano` | POST | Trocar de plano (upgrade/downgrade) |
| `/api/v1/assinatura/cancelar` | POST | Cancelar assinatura |
| `/api/v1/assinatura/reativar` | POST | Reativar assinatura |
| `/api/v1/assinatura/validar-cupom` | POST | Validar cupom de desconto |
| `/api/v1/planos` | GET | Listar planos disponiveis |

---

## 2. Fonte de Dados Principal

Ao abrir a tela, fazer **duas chamadas em paralelo**:

```javascript
const [statusRes, cobrancasRes] = await Promise.all([
  api.get('/assinatura/status'),
  api.get('/assinatura/cobrancas')
]);

const status = statusRes.data.dados;    // AssinaturaStatusDTO
const cobrancas = cobrancasRes.data.dados; // List<CobrancaPlataformaDTO>
```

### 2.1. AssinaturaStatusDTO (response de `/assinatura/status`)

```typescript
interface AssinaturaStatus {
  bloqueado: boolean;
  statusAssinatura: string;       // "TRIAL" | "ATIVA" | "VENCIDA" | "CANCELADA" | "SUSPENSA"
  situacao: string;                // Enum semantico (ver secao 3)
  mensagem: string;
  planoCodigo: string;             // "gratuito" | "basico" | "plus" | "premium"
  planoNome: string;               // "Gratuito" | "Basico" | "Plus" | "Premium"
  planoGratuito: boolean;
  diasRestantesTrial: number | null;
  dtFimTrial: string | null;       // "2026-03-23" (LocalDate)
  temCobrancaPendente: boolean;
  valorPendente: number | null;
  dtVencimentoProximaCobranca: string | null;
  dtAcessoAte: string | null;      // Data limite acesso pos-cancelamento
  cicloCobranca: string | null;    // "MENSAL" | "ANUAL"
  dtProximoVencimento: string | null;
}
```

### 2.2. CobrancaPlataformaDTO (response de `/assinatura/cobrancas`)

```typescript
interface Cobranca {
  id: number;
  valor: number;
  dtVencimento: string;            // "2026-03-15" (LocalDate)
  dtPagamento: string | null;      // "2026-03-14T10:30:00" (LocalDateTime)
  status: string;                  // "PENDENTE" | "PAGA" | "VENCIDA" | "CANCELADA" | "ESTORNADA"
  formaPagamento: string | null;   // "PIX" | "BOLETO" | "CARTAO_CREDITO"
  assasInvoiceUrl: string | null;  // Link para fatura completa
  assasBankSlipUrl: string | null; // Link para boleto PDF
  assasPixQrCode: string | null;   // Base64 da imagem QR Code PIX
  assasPixCopiaCola: string | null;// String PIX copia e cola
  referenciaMes: number;
  referenciaAno: number;
  cupomCodigo: string | null;
  valorOriginal: number | null;
  valorDescontoAplicado: number | null;
  dtCriacao: string;
}
```

---

## 3. Enum `situacao` - Decisoes de UI

O campo `situacao` determina o que mostrar na tela. Ele e mais granular que `statusAssinatura`.

| situacao | Descricao | Bloqueado? | Secoes visiveis |
|----------|-----------|------------|-----------------|
| `TRIAL_ATIVO` | Periodo de teste em andamento | Nao | Accordion Trial, Planos, Info Basica |
| `TRIAL_EXPIRADO` | Trial acabou sem escolher plano | Sim | Alerta Bloqueio, Planos |
| `PLANO_GRATUITO` | Usando plano gratuito | Nao | Info Plano, Upgrade CTA, Planos |
| `ATIVA` | Plano pago ativo e em dia | Nao | Info Plano, Cobrancas, Pagamento, Acoes |
| `PAGAMENTO_PENDENTE` | Ativa mas com cobranca pendente | Nao | Info Plano, Alerta Pendente, Cobrancas, Pagamento |
| `PAGAMENTO_ATRASADO` | Pagamento vencido | Sim | Alerta Bloqueio, Cobranca Vencida, Pagamento |
| `CANCELADA_COM_ACESSO` | Cancelou mas acesso ate data X | Nao | Info Plano, Banner Cancel, Planos, Cobrancas |
| `CANCELADA_SEM_ACESSO` | Cancelou e acesso expirou | Sim | Alerta Bloqueio, Planos |
| `SUSPENSA` | Suspensa pelo admin | Sim | Alerta Bloqueio (suporte) |
| `SEM_ASSINATURA` | Sem registro de assinatura | Sim | Alerta Bloqueio, Planos |

---

## 4. Layout da Tela

A tela e composta por secoes condicionais. A ordem de renderizacao:

```
+================================================================+
|  HEADER: "Minha Assinatura"                                     |
+================================================================+
|                                                                  |
|  [1. ALERTA DE BLOQUEIO - se bloqueado]                         |
|                                                                  |
|  [2. ACCORDION TRIAL - se TRIAL_ATIVO]                          |
|                                                                  |
|  [3. CARD INFO DO PLANO ATUAL]                                  |
|                                                                  |
|  [4. ALERTA PAGAMENTO PENDENTE - se temCobrancaPendente]        |
|                                                                  |
|  [5. BANNER CANCELAMENTO - se CANCELADA_COM_ACESSO]             |
|                                                                  |
|  [6. PROXIMA COBRANCA - card]                                   |
|                                                                  |
|  [7. HISTORICO DE COBRANCAS - tabela/lista]                     |
|                                                                  |
|  [8. FORMA DE PAGAMENTO]                                        |
|                                                                  |
|  [9. ACOES (trocar plano, cancelar, reativar)]                  |
|                                                                  |
+================================================================+
```

---

## 5. Secao 1: Alerta de Bloqueio

**Condicao:** `status.bloqueado === true`

Exibir um card/modal de tela cheia ou banner proeminente impedindo uso normal.

```
+--------------------------------------------------------------+
|  [Icone de alerta]                                           |
|                                                              |
|  {{ status.mensagem }}                                       |
|                                                              |
|  [Botao Primario]          [Link Secundario]                 |
+--------------------------------------------------------------+
```

### Acoes por situacao:

| situacao | Botao Primario | Link Secundario |
|----------|---------------|-----------------|
| `TRIAL_EXPIRADO` | "Escolher Plano" → abre modal planos | "Continuar no Gratuito" → POST `/escolher-plano` com planoCodigo="gratuito" |
| `PAGAMENTO_ATRASADO` | "Regularizar Pagamento" → scroll para cobranca vencida | "Falar com Suporte" |
| `CANCELADA_SEM_ACESSO` | "Reativar Assinatura" → abre modal planos | - |
| `SUSPENSA` | - | "Falar com Suporte" (WhatsApp/email) |
| `SEM_ASSINATURA` | "Escolher Plano" → abre modal planos | - |

---

## 6. Secao 2: Accordion Trial

**Condicao:** `status.situacao === 'TRIAL_ATIVO'`

Accordion expansivel (aberto por padrao) mostrando informacoes do periodo de teste.

```
+--------------------------------------------------------------+
| v  Periodo de Teste                                          |
|--------------------------------------------------------------|
|                                                              |
|  Voce esta no periodo de teste gratuito.                     |
|                                                              |
|  Dias restantes: {{ status.diasRestantesTrial }}             |
|  Expira em: {{ formatDate(status.dtFimTrial) }}              |
|                                                              |
|  [===========================-------] 10/14 dias             |
|                                                              |
|  Apos o trial, voce pode:                                    |
|  - Escolher um plano pago para desbloquear todos os recursos |
|  - Continuar no plano gratuito com recursos limitados        |
|                                                              |
|  [ Escolher um Plano ]                                       |
|                                                              |
+--------------------------------------------------------------+
```

### Logica da barra de progresso:
```javascript
const diasTotais = 14;
const diasUsados = diasTotais - status.diasRestantesTrial;
const percentual = (diasUsados / diasTotais) * 100;
// Cor: verde (>7 dias), amarelo (3-7 dias), vermelho (<3 dias)
```

### Cores por urgencia:
- **>7 dias restantes**: Verde (`#4CAF50`)
- **3-7 dias restantes**: Amarelo/Laranja (`#FF9800`)
- **<3 dias restantes**: Vermelho (`#F44336`)

---

## 7. Secao 3: Card Info do Plano Atual

**Condicao:** Sempre visivel (exceto quando bloqueado por SUSPENSA/SEM_ASSINATURA)

```
+--------------------------------------------------------------+
|  Plano Atual                                                 |
|--------------------------------------------------------------|
|                                                              |
|  [Icone]  {{ status.planoNome }}                             |
|           {{ status.cicloCobranca === 'ANUAL' ?              |
|              'Cobranca Anual' : 'Cobranca Mensal' }}         |
|                                                              |
|  +------------------+  +------------------+                  |
|  | Proximo venc.    |  | Valor            |                  |
|  | 15/04/2026       |  | R$ 129,90/mes    |                  |
|  +------------------+  +------------------+                  |
|                                                              |
|  +------------------+  +------------------+                  |
|  | Status           |  | Forma Pgto       |                  |
|  | Ativa            |  | PIX              |                  |
|  +------------------+  +------------------+                  |
|                                                              |
|  Se tem cupom aplicado:                                      |
|  Cupom: BEMVINDO20 (-R$ 15,98)                              |
|                                                              |
+--------------------------------------------------------------+
```

### Dados utilizados:
```javascript
const planoInfo = {
  nome: status.planoNome,
  codigo: status.planoCodigo,
  ciclo: status.cicloCobranca,         // "MENSAL" | "ANUAL"
  proximoVencimento: status.dtProximoVencimento,
  gratuito: status.planoGratuito,
  // valor: buscar do plano ou calcular
};
```

### Chip de status:

| statusAssinatura | Cor | Texto |
|-----------------|-----|-------|
| TRIAL | Azul | "Em teste" |
| ATIVA | Verde | "Ativa" |
| VENCIDA | Vermelho | "Pagamento atrasado" |
| CANCELADA | Cinza | "Cancelada" |
| SUSPENSA | Vermelho escuro | "Suspensa" |

### Se plano gratuito (`status.planoGratuito === true`):
Mostrar CTA de upgrade:
```
+--------------------------------------------------------------+
|  Voce esta no plano gratuito.                                |
|  Faca upgrade para desbloquear mais recursos.                |
|                                                              |
|  [ Ver Planos Disponiveis ]                                  |
+--------------------------------------------------------------+
```

---

## 8. Secao 4: Alerta Pagamento Pendente

**Condicao:** `status.temCobrancaPendente === true && !status.bloqueado`

```
+--------------------------------------------------------------+
|  [!] Pagamento Pendente                                      |
|--------------------------------------------------------------|
|  Voce tem uma cobranca pendente de R$ {{ valorPendente }}    |
|  com vencimento em {{ dtVencimentoProximaCobranca }}.        |
|                                                              |
|  [ Pagar Agora ]                                             |
+--------------------------------------------------------------+
```

Cor: Amarelo/Warning. Botao "Pagar Agora" rola ate a secao de cobrancas ou abre modal de pagamento.

---

## 9. Secao 5: Banner Cancelamento

**Condicao:** `status.situacao === 'CANCELADA_COM_ACESSO'`

```
+--------------------------------------------------------------+
|  Sua assinatura foi cancelada                                |
|--------------------------------------------------------------|
|  Voce ainda pode usar o sistema ate                          |
|  {{ formatDate(status.dtAcessoAte) }}.                       |
|                                                              |
|  Apos essa data, seu acesso sera limitado ao plano gratuito. |
|                                                              |
|  [ Reativar Assinatura ]                                     |
+--------------------------------------------------------------+
```

Cor: Cinza/Info. Botao "Reativar" chama `POST /assinatura/reativar`.

---

## 10. Secao 6: Proxima Cobranca

**Condicao:** Existe cobranca com `status === 'PENDENTE'` na lista de cobrancas

Filtrar a primeira cobranca pendente da lista:
```javascript
const proximaCobranca = cobrancas.find(c => c.status === 'PENDENTE');
```

```
+--------------------------------------------------------------+
|  Proxima Cobranca                                            |
|--------------------------------------------------------------|
|                                                              |
|  Valor: R$ {{ proximaCobranca.valor }}                       |
|  Vencimento: {{ formatDate(proximaCobranca.dtVencimento) }}  |
|  Referencia: {{ proximaCobranca.referenciaMes }}/            |
|              {{ proximaCobranca.referenciaAno }}              |
|                                                              |
|  Se tem desconto aplicado:                                   |
|  Valor original: R$ {{ valorOriginal }}                      |
|  Desconto ({{ cupomCodigo }}): -R$ {{ valorDescontoAplicado }}|
|  Valor final: R$ {{ valor }}                                 |
|                                                              |
|  +---Opcoes de Pagamento---+                                 |
|  |                         |                                 |
|  |  [PIX]  [Boleto]  [Fatura] |                              |
|  |                         |                                 |
|  +-------------------------+                                 |
|                                                              |
+--------------------------------------------------------------+
```

### Botoes de pagamento (baseados nos campos da cobranca):

| Campo | Acao | Visivel quando |
|-------|------|----------------|
| `assasPixCopiaCola` | Abre modal PIX (QR Code + Copia e Cola) | Nao nulo |
| `assasBankSlipUrl` | Abre link do boleto em nova aba | Nao nulo |
| `assasInvoiceUrl` | Abre fatura completa em nova aba | Nao nulo |

### Modal PIX:
```
+----------------------------------------------+
|  Pagar com PIX                    [X]        |
|----------------------------------------------|
|                                              |
|  [QR Code - imagem base64]                   |
|  (assasPixQrCode)                            |
|                                              |
|  Ou copie o codigo:                          |
|  +----------------------------------------+  |
|  | {{ assasPixCopiaCola }}                |  |
|  +----------------------------------------+  |
|  [ Copiar Codigo ]                           |
|                                              |
|  Valor: R$ {{ valor }}                       |
|  Vencimento: {{ dtVencimento }}              |
|                                              |
+----------------------------------------------+
```

---

## 11. Secao 7: Historico de Cobrancas

**Condicao:** Lista de cobrancas nao vazia

### Layout: Tabela (desktop) / Cards (mobile)

```
+--------------------------------------------------------------+
|  Historico de Cobrancas                                      |
|--------------------------------------------------------------|
|                                                              |
|  Ref.       | Valor     | Vencimento  | Status    | Acoes   |
|  -----------|-----------|-------------|-----------|---------|
|  03/2026    | R$ 129,90 | 15/03/2026  | [PAGA]    | [Ver]   |
|  02/2026    | R$ 113,92 | 15/02/2026  | [PAGA]    | [Ver]   |
|  01/2026    | R$ 129,90 | 15/01/2026  | [PAGA]    | [Ver]   |
|  04/2026    | R$ 129,90 | 15/04/2026  | [PENDENTE]| [Pagar] |
|                                                              |
|  Mostrando {{ cobrancas.length }} cobrancas                  |
+--------------------------------------------------------------+
```

### Chips de status com cores:

| Status | Cor | Icone |
|--------|-----|-------|
| PENDENTE | Amarelo (`#FF9800`) | Relogio |
| PAGA | Verde (`#4CAF50`) | Check |
| VENCIDA | Vermelho (`#F44336`) | Exclamacao |
| CANCELADA | Cinza (`#9E9E9E`) | X |
| ESTORNADA | Roxo (`#9C27B0`) | Seta voltar |

### Botao "Ver" - Expande detalhes da cobranca:

```
+--------------------------------------------------------------+
|  Detalhes da Cobranca #{{ id }}                              |
|--------------------------------------------------------------|
|  Referencia: {{ referenciaMes }}/{{ referenciaAno }}         |
|  Valor: R$ {{ valor }}                                       |
|  Data de Vencimento: {{ formatDate(dtVencimento) }}          |
|  Data de Pagamento: {{ formatDateTime(dtPagamento) || '-' }} |
|  Forma de Pagamento: {{ formaPagamento || '-' }}             |
|  Status: {{ status }}                                        |
|                                                              |
|  Se tem cupom:                                               |
|  Cupom: {{ cupomCodigo }}                                    |
|  Valor Original: R$ {{ valorOriginal }}                      |
|  Desconto: -R$ {{ valorDescontoAplicado }}                   |
|  Valor Cobrado: R$ {{ valor }}                               |
|                                                              |
|  Se PENDENTE:                                                |
|  [ Pagar com PIX ] [ Baixar Boleto ] [ Ver Fatura ]         |
|                                                              |
|  Se PAGA:                                                    |
|  [ Ver Fatura/Comprovante ]                                  |
|                                                              |
+--------------------------------------------------------------+
```

---

## 12. Secao 8: Forma de Pagamento

**Condicao:** `!status.planoGratuito && status.statusAssinatura !== 'TRIAL'`

Mostra a forma de pagamento configurada na assinatura. Como o Asaas gerencia os pagamentos, esta secao e informativa.

```
+--------------------------------------------------------------+
|  Forma de Pagamento                                          |
|--------------------------------------------------------------|
|                                                              |
|  Metodo atual: {{ formaPagamento }}                          |
|                                                              |
|  [Icone PIX]  PIX                                            |
|  As cobrancas sao geradas automaticamente pelo Asaas.        |
|  Voce recebera o QR Code/boleto antes do vencimento.         |
|                                                              |
|  Para alterar a forma de pagamento, troque o plano           |
|  selecionando uma nova forma no processo.                    |
|                                                              |
+--------------------------------------------------------------+
```

### Icones por forma:
| Forma | Icone sugerido |
|-------|---------------|
| PIX | QR Code / Logo PIX |
| BOLETO | Codigo de barras |
| CARTAO_CREDITO | Cartao de credito |

**Nota:** Para trocar a forma de pagamento sem trocar o plano, o usuario pode usar "Trocar Plano" selecionando o mesmo plano com forma diferente. Alternativamente, implemente um endpoint dedicado no futuro.

---

## 13. Secao 9: Acoes

### 13.1. Trocar Plano

**Condicao:** `status.statusAssinatura === 'ATIVA' && !status.planoGratuito`

```
[ Trocar Plano ]
```

Abre modal/pagina de selecao de planos (reutilizar componente de escolha de plano).

**Endpoint:** `POST /api/v1/assinatura/trocar-plano`

```typescript
// Request
interface TrocarPlanoRequest {
  planoCodigo: string;       // "basico" | "plus" | "premium"
  cicloCobranca: string;     // "MENSAL" | "ANUAL"
  formaPagamento: string;    // "PIX" | "BOLETO" | "CARTAO_CREDITO"
  codigoCupom?: string;
}
```

Mostrar preview do pro-rata antes de confirmar (calcular no frontend ou adicionar endpoint de preview).

### 13.2. Cancelar Assinatura

**Condicao:** `status.statusAssinatura === 'ATIVA' && !status.planoGratuito`

```
[ Cancelar Assinatura ]  (cor: vermelho, outline)
```

Abre dialog de confirmacao:
```
+----------------------------------------------+
|  Cancelar Assinatura                  [X]    |
|----------------------------------------------|
|                                              |
|  Tem certeza que deseja cancelar?            |
|                                              |
|  Voce continuara tendo acesso ate            |
|  {{ formatDate(dtProximoVencimento) }}.      |
|                                              |
|  Apos essa data, sua conta sera migrada      |
|  para o plano gratuito com recursos          |
|  limitados.                                  |
|                                              |
|  [ Manter Assinatura ]  [ Confirmar Cancel.] |
+----------------------------------------------+
```

**Endpoint:** `POST /api/v1/assinatura/cancelar`
- Nao precisa body
- Response: `AssinaturaResponseDTO` atualizado

### 13.3. Reativar Assinatura

**Condicao:** `status.situacao === 'CANCELADA_COM_ACESSO' || status.situacao === 'CANCELADA_SEM_ACESSO' || status.statusAssinatura === 'VENCIDA'`

```
[ Reativar Assinatura ]  (cor: primaria)
```

**Endpoint:** `POST /api/v1/assinatura/reativar`
- Nao precisa body
- Response: `AssinaturaResponseDTO` atualizado
- Apos sucesso: atualizar status no store e recarregar tela

### 13.4. Escolher Plano (Trial/Gratuito/Sem Assinatura)

**Condicao:** `status.situacao in ['TRIAL_ATIVO', 'TRIAL_EXPIRADO', 'PLANO_GRATUITO', 'SEM_ASSINATURA', 'CANCELADA_SEM_ACESSO']`

Abre modal/pagina de selecao de planos.

---

## 14. Modal de Selecao de Planos

Componente reutilizavel para escolher/trocar plano. Usado em varios pontos da tela.

### 14.1. Carregar planos

```javascript
const planosRes = await api.get('/planos');
const { planoAtualCodigo, planoAtualNome, planosDisponiveis } = planosRes.data.dados;
```

### 14.2. Interface PlanoOrganizacaoDTO

```typescript
interface PlanoOrganizacao {
  planoAtualCodigo: string;
  planoAtualNome: string;
  planosDisponiveis: PlanoBelloryPublic[];
}

interface PlanoBelloryPublic {
  id: string;            // codigo do plano
  name: string;
  tagline: string;
  description: string;
  popular: boolean;
  cta: string;
  badge: string | null;
  icon: string;
  color: string;
  gradient: string;
  price: number;         // preco mensal
  yearlyPrice: number;   // preco anual
  yearlyDiscount: number;// % desconto anual
  promoMensalAtiva: boolean;
  promoMensalPreco: number | null;
  promoMensalTexto: string | null;
  features: PlanoFeature[];
  limits: PlanoLimites;
}

interface PlanoFeature {
  text: string;
  included: boolean;
}

interface PlanoLimites {
  maxAgendamentosMes: number | null;
  maxUsuarios: number | null;
  maxClientes: number | null;
  maxServicos: number | null;
  maxUnidades: number | null;
  permiteAgendamentoOnline: boolean;
  permiteWhatsapp: boolean;
  permiteSite: boolean;
  permiteEcommerce: boolean;
  permiteRelatoriosAvancados: boolean;
  permiteApi: boolean;
  permiteIntegracaoPersonalizada: boolean;
  suportePrioritario: boolean;
  suporte24x7: boolean;
}
```

### 14.3. Layout do Modal

```
+================================================================+
|  Escolha seu Plano                                     [X]     |
|================================================================|
|                                                                  |
|  Toggle: [ Mensal ] [ Anual (economize ate 23%) ]               |
|                                                                  |
|  +-------------+ +-------------+ +-------------+ +-------------+|
|  | Gratuito    | | Basico      | | Plus        | | Premium     ||
|  | R$ 0        | | R$ 79,90/m  | | R$ 129,90/m | | R$ 199,90/m||
|  |             | |             | | MAIS POPULAR| |             ||
|  | - 50 agend. | | - Ilimitado | | + WhatsApp  | | + Tudo      ||
|  | - 1 usuario | | - 3 usuarios| | + Site      | | + API       ||
|  | ...         | | ...         | | ...         | | ...         ||
|  |             | |             | |             | |             ||
|  | [Atual]     | | [Escolher]  | | [Escolher]  | | [Consultar] ||
|  +-------------+ +-------------+ +-------------+ +-------------+|
|                                                                  |
|  Cupom de desconto: [______________] [Validar]                  |
|  {{ cupomMensagem }}                                            |
|                                                                  |
+================================================================+
```

### 14.4. Fluxo de Escolha

```
1. Usuario seleciona plano
2. Seleciona ciclo (MENSAL/ANUAL)
3. Seleciona forma de pagamento (PIX/BOLETO/CARTAO_CREDITO)
4. (Opcional) Digita cupom → POST /assinatura/validar-cupom
5. Resumo do pedido com valores
6. Confirma → POST /assinatura/escolher-plano
7. Sucesso → Fecha modal, atualiza status, mostra toast
```

### 14.5. Validar Cupom

```javascript
const cupomRes = await api.post('/assinatura/validar-cupom', {
  codigoCupom: 'BEMVINDO20',
  planoCodigo: 'plus',
  cicloCobranca: 'MENSAL'
});

// Response CupomValidacaoResponseDTO
{
  valido: true,
  mensagem: "Cupom aplicado: 20% de desconto na primeira cobranca",
  tipoDesconto: "PERCENTUAL",
  tipoAplicacao: "PRIMEIRA_COBRANCA",
  percentualDesconto: 20.00,
  valorDesconto: 25.98,
  valorOriginal: 129.90,
  valorComDesconto: 103.92
}
```

### 14.6. Confirmar Escolha

```javascript
const response = await api.post('/assinatura/escolher-plano', {
  planoCodigo: 'plus',
  cicloCobranca: 'MENSAL',
  formaPagamento: 'PIX',
  codigoCupom: 'BEMVINDO20'  // opcional
});

// Response: AssinaturaResponseDTO
// Atualizar store global e recarregar status
```

---

## 15. Resumo Visual por Situacao

### TRIAL_ATIVO
```
[Accordion Trial aberto]
[Card Plano: "Gratuito - Em teste"]
[Botao: "Escolher Plano"]
```

### TRIAL_EXPIRADO
```
[BLOQUEIO: "Seu trial expirou"]
[Planos para escolher]
```

### PLANO_GRATUITO
```
[Card Plano: "Gratuito"]
[Banner: "Faca upgrade para mais recursos"]
[Planos para upgrade]
```

### ATIVA
```
[Card Plano: "Plus - Mensal"]
[Proxima Cobranca]
[Historico de Cobrancas]
[Forma de Pagamento]
[Acoes: Trocar Plano | Cancelar]
```

### PAGAMENTO_PENDENTE
```
[Card Plano: "Plus - Mensal"]
[ALERTA: Pagamento pendente de R$ 129,90]
[Proxima Cobranca com botoes de pagamento]
[Historico de Cobrancas]
[Forma de Pagamento]
[Acoes: Trocar Plano | Cancelar]
```

### PAGAMENTO_ATRASADO
```
[BLOQUEIO: "Pagamento atrasado"]
[Cobranca vencida com botoes de pagamento]
[Historico]
```

### CANCELADA_COM_ACESSO
```
[Card Plano: "Plus - Cancelada"]
[Banner: "Acesso ate 15/04/2026"]
[Historico de Cobrancas]
[Botao: "Reativar Assinatura"]
[Planos disponiveis]
```

### CANCELADA_SEM_ACESSO
```
[BLOQUEIO: "Assinatura encerrada"]
[Planos para reativar]
```

### SUSPENSA
```
[BLOQUEIO: "Conta suspensa - contate suporte"]
```

---

## 16. Formatacoes e Helpers

```typescript
// Formatar moeda
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value);
}

// Formatar data (LocalDate: "2026-03-15")
function formatDate(dateStr: string): string {
  if (!dateStr) return '-';
  const [year, month, day] = dateStr.split('-');
  return `${day}/${month}/${year}`;
}

// Formatar data+hora (LocalDateTime: "2026-03-15T10:30:00")
function formatDateTime(dateTimeStr: string): string {
  if (!dateTimeStr) return '-';
  const dt = new Date(dateTimeStr);
  return dt.toLocaleDateString('pt-BR') + ' ' + dt.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

// Label da forma de pagamento
function formatPaymentMethod(method: string): string {
  const map: Record<string, string> = {
    'PIX': 'PIX',
    'BOLETO': 'Boleto Bancario',
    'CARTAO_CREDITO': 'Cartao de Credito'
  };
  return map[method] || method;
}

// Label do ciclo
function formatCycle(cycle: string): string {
  return cycle === 'ANUAL' ? 'Anual' : 'Mensal';
}

// Cor do status da cobranca
function getStatusColor(status: string): string {
  const colors: Record<string, string> = {
    'PENDENTE': '#FF9800',
    'PAGA': '#4CAF50',
    'VENCIDA': '#F44336',
    'CANCELADA': '#9E9E9E',
    'ESTORNADA': '#9C27B0'
  };
  return colors[status] || '#757575';
}
```

---

## 17. Atualizacao de Estado Global

Apos qualquer acao que muda o status da assinatura, atualizar o store/context global:

```javascript
// Acoes que requerem reload do status:
// 1. Escolher plano (POST /escolher-plano)
// 2. Trocar plano (POST /trocar-plano)
// 3. Cancelar (POST /cancelar)
// 4. Reativar (POST /reativar)

async function recarregarStatusAssinatura() {
  const res = await api.get('/assinatura/status');
  assinaturaStore.atualizarAssinatura(res.data.dados);
}
```

Isso garante que banners, alertas e bloqueios em outras telas sejam atualizados imediatamente.

---

## 18. Tratamento de Erros

### 18.1. Erro 403 do AssinaturaInterceptor

Se qualquer request retornar 403 com `dados.bloqueado === true`, redirecionar para esta tela:

```javascript
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 403 && error.response.data?.dados?.bloqueado) {
      assinaturaStore.atualizarAssinatura(error.response.data.dados);
      router.push('/app/configuracoes/assinatura');
      return;
    }
    return Promise.reject(error);
  }
);
```

### 18.2. Erros de Acao

| Acao | Erro Comum | Tratamento |
|------|-----------|------------|
| Escolher plano | "Plano indisponivel" | Toast erro, recarregar planos |
| Validar cupom | "Cupom invalido: ..." | Mostrar mensagem do backend |
| Cancelar | "Assinatura nao esta ativa" | Recarregar status |
| Reativar | "Status nao permite reativacao" | Recarregar status |
| Trocar plano | "Pro-rata negativo" | Mostrar mensagem, sugerir contato |

---

## 19. Checklist de Implementacao

### Prioridade Alta
- [ ] Tela principal com layout condicional por `situacao`
- [ ] Card info do plano atual
- [ ] Accordion de trial com barra de progresso
- [ ] Alerta de bloqueio com acoes
- [ ] Historico de cobrancas (tabela/cards)
- [ ] Detalhes da cobranca (expandir)

### Prioridade Media
- [ ] Modal de selecao de planos (com toggle mensal/anual)
- [ ] Validacao de cupom no modal
- [ ] Modal PIX (QR Code + copia e cola)
- [ ] Botao boleto (abrir em nova aba)
- [ ] Cancelar assinatura com dialog de confirmacao
- [ ] Reativar assinatura
- [ ] Alerta de pagamento pendente

### Prioridade Baixa
- [ ] Banner de cancelamento com countdown
- [ ] Animacoes de transicao entre estados
- [ ] Polling do status a cada 5 min (detectar pagamento confirmado)
- [ ] Notificacao push quando pagamento for confirmado
- [ ] Comparativo de planos lado a lado no modal
- [ ] Preview de pro-rata na troca de plano
