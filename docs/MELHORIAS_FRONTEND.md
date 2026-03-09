# Melhorias Necessarias no Frontend - Sistema de Assinaturas

**Data**: 2026-03-09
**Backend version**: Pos-remodelagem Asaas como source of truth

---

## 1. Resumo das Mudancas no Backend

O backend foi reestruturado para usar o Asaas como fonte principal de dados de cobranca/pagamento. As principais mudancas que afetam o frontend:

1. **Novo campo `situacao`** no `AssinaturaStatusDTO` - enum semantico para o frontend
2. **Novo campo `assinatura`** no response do `/auth/validate` e `/auth/me`
3. **Novos campos** no `AssinaturaStatusDTO`: `planoGratuito`, `dtFimTrial`, `dtAcessoAte`, `cicloCobranca`, `dtProximoVencimento`
4. **Cobrancas agora vem do Asaas** - links de pagamento (PIX, boleto) sao sempre atuais
5. **Trial restaurado** - novos clientes comecam em trial de 14 dias

---

## 2. Mudancas no Login (`POST /api/v1/auth/login`)

### 2.1. Response Atualizado

O campo `organizacao.assinatura` agora retorna dados mais ricos:

```json
{
    "success": true,
    "token": "eyJ...",
    "user": { ... },
    "organizacao": {
        "id": 1,
        "nome": "Salao X",
        "assinatura": {
            "bloqueado": false,
            "statusAssinatura": "ATIVA",
            "situacao": "PAGAMENTO_PENDENTE",
            "mensagem": "Voce tem um pagamento pendente...",
            "planoCodigo": "plus",
            "planoNome": "Plano Plus",
            "planoGratuito": false,
            "cicloCobranca": "MENSAL",
            "dtProximoVencimento": "2026-04-09",
            "temCobrancaPendente": true,
            "valorPendente": 79.90,
            "dtVencimentoProximaCobranca": "2026-03-15"
        }
    }
}
```

### 2.2. Campo `situacao` - O que mostrar para cada valor

| Situacao | Bloqueado? | Acao no Frontend |
|----------|-----------|-----------------|
| `TRIAL_ATIVO` | Nao | Banner superior: "Voce esta em trial. Restam X dias. [Escolher plano]" |
| `TRIAL_EXPIRADO` | Sim | Tela cheia: "Seu trial expirou. Escolha um plano para continuar." |
| `PLANO_GRATUITO` | Nao | Banner: "Voce esta no plano gratuito. [Fazer upgrade]" |
| `ATIVA` | Nao | Acesso normal. Sem banners. |
| `PAGAMENTO_PENDENTE` | Nao | Alerta: "Pagamento pendente de R$ X. [Ver detalhes]" |
| `PAGAMENTO_ATRASADO` | Sim | Tela cheia: "Pagamento atrasado. [Regularizar]" |
| `CANCELADA_COM_ACESSO` | Nao | Banner: "Assinatura cancelada. Acesso ate DD/MM/YYYY. [Reativar]" |
| `CANCELADA_SEM_ACESSO` | Sim | Tela cheia: "Assinatura encerrada. [Escolher plano]" |
| `SUSPENSA` | Sim | Tela cheia: "Conta suspensa. Entre em contato com o suporte." |
| `SEM_ASSINATURA` | Sim | Tela cheia: "Nenhuma assinatura. [Escolher plano]" |

### 2.3. Logica de Bloqueio no Frontend

```javascript
// Apos login, verificar assinatura
const assinatura = response.organizacao.assinatura;

if (assinatura.bloqueado) {
    // Redirecionar para tela de bloqueio baseada na situacao
    switch (assinatura.situacao) {
        case 'TRIAL_EXPIRADO':
        case 'SEM_ASSINATURA':
        case 'CANCELADA_SEM_ACESSO':
            navigate('/escolher-plano');
            break;
        case 'PAGAMENTO_ATRASADO':
            navigate('/regularizar-pagamento');
            break;
        case 'SUSPENSA':
            navigate('/conta-suspensa');
            break;
    }
} else {
    // Acesso permitido - mostrar banners conforme situacao
    if (assinatura.situacao === 'TRIAL_ATIVO') {
        showTrialBanner(assinatura.diasRestantesTrial, assinatura.dtFimTrial);
    } else if (assinatura.situacao === 'PLANO_GRATUITO') {
        showUpgradeBanner();
    } else if (assinatura.situacao === 'PAGAMENTO_PENDENTE') {
        showPaymentAlert(assinatura.valorPendente, assinatura.dtVencimentoProximaCobranca);
    } else if (assinatura.situacao === 'CANCELADA_COM_ACESSO') {
        showCancelBanner(assinatura.dtAcessoAte);
    }
}
```

---

## 3. Mudancas na Validacao de Token (`POST /api/v1/auth/validate`)

### Novo campo no response:

```json
{
    "valid": true,
    "username": "user@email.com",
    "userId": 1,
    "userType": "USER",
    "roles": ["ROLE_ADMIN"],
    "expiresAt": "09/03/2026 18:00:00",
    "assinatura": {
        "bloqueado": false,
        "situacao": "ATIVA",
        ...
    }
}
```

### Acao necessaria:
- **Ao validar token** (page refresh, troca de rota), ler o campo `assinatura` e atualizar o estado global.
- Usar a mesma logica de bloqueio/banner do login.

---

## 4. Mudancas no `/auth/me` (`GET /api/v1/auth/me`)

### Novo campo no response `UserInfoDTO`:

```json
{
    "id": 1,
    "username": "user@email.com",
    "nomeCompleto": "Joao",
    ...
    "assinatura": {
        "bloqueado": false,
        "situacao": "ATIVA",
        ...
    }
}
```

### Acao necessaria:
- Ao chamar `/me`, atualizar o estado da assinatura no store/context.

---

## 5. Mudancas na Listagem de Cobrancas (`GET /api/v1/assinatura/cobrancas`)

### Antes:
- Retornava dados salvos localmente (podiam estar desatualizados)
- Links de PIX/boleto eram copias estaticas

### Agora:
- Consulta o Asaas em tempo real
- Links de pagamento (`assasInvoiceUrl`, `assasBankSlipUrl`, `assasPixQrCode`, `assasPixCopiaCola`) sao sempre atuais
- Status mapeado: `PENDING` → `PENDENTE`, `RECEIVED`/`CONFIRMED` → `PAGA`, `OVERDUE` → `VENCIDA`

### Acao necessaria:
- **Remover cache local** de cobrancas (se houver). Os dados agora sao sempre frescos.
- **Links de pagamento** podem ser usados diretamente:
  - `assasInvoiceUrl` → Link para fatura (abre no navegador)
  - `assasBankSlipUrl` → Link do boleto PDF
  - `assasPixQrCode` → Imagem base64 do QR Code PIX
  - `assasPixCopiaCola` → String para copiar (PIX copia e cola)

---

## 6. Telas a Criar/Atualizar

### 6.1. Tela de Bloqueio (nova ou atualizar existente)

Tela que aparece quando `bloqueado: true`. Deve mostrar conteudo dinamico baseado na `situacao`:

```
+----------------------------------------------+
|                                              |
|        [Icone baseado na situacao]           |
|                                              |
|     {{ mensagem }}                           |
|                                              |
|     [Botao primario baseado na situacao]     |
|     [Link secundario: "Falar com suporte"]   |
|                                              |
+----------------------------------------------+
```

| Situacao | Botao Primario | Link Secundario |
|----------|---------------|-----------------|
| TRIAL_EXPIRADO | "Escolher Plano" → /escolher-plano | "Continuar no gratuito" → /dashboard |
| PAGAMENTO_ATRASADO | "Regularizar Pagamento" → /cobrancas | "Falar com suporte" |
| CANCELADA_SEM_ACESSO | "Reativar Assinatura" → /escolher-plano | - |
| SUSPENSA | - | "Falar com suporte" |
| SEM_ASSINATURA | "Escolher Plano" → /escolher-plano | - |

### 6.2. Banner de Trial (atualizar)

```
+----------------------------------------------+
| ⏳ Periodo de teste: restam {diasRestantesTrial} dias (ate {dtFimTrial})  [Escolher plano →] |
+----------------------------------------------+
```

### 6.3. Banner de Plano Gratuito (novo)

```
+----------------------------------------------+
| 🆓 Voce esta no plano gratuito. Faca upgrade para desbloquear mais recursos.  [Ver planos →] |
+----------------------------------------------+
```

### 6.4. Alerta de Pagamento Pendente (novo)

```
+----------------------------------------------+
| ⚠️ Pagamento pendente de R$ {valorPendente} com vencimento em {dtVencimentoProximaCobranca}  [Pagar agora →] |
+----------------------------------------------+
```

### 6.5. Banner de Cancelamento (atualizar)

```
+----------------------------------------------+
| Sua assinatura foi cancelada. Voce pode usar ate {dtAcessoAte}.  [Reativar →] |
+----------------------------------------------+
```

### 6.6. Tela de Cobrancas (atualizar)

A tela de cobrancas deve:
1. Mostrar a lista de cobrancas vindas do endpoint
2. Para cada cobranca PENDENTE, mostrar botoes de acao:
   - Se PIX: Mostrar QR Code (`assasPixQrCode`) e botao "Copiar codigo" (`assasPixCopiaCola`)
   - Se Boleto: Botao "Baixar boleto" → `assasBankSlipUrl`
   - Link "Ver fatura" → `assasInvoiceUrl`
3. Para cobrancas PAGAS: Mostrar data de pagamento e status

---

## 7. Store/Context de Assinatura

### Recomendacao: Criar um estado global para assinatura

```javascript
// assinaturaStore.js (ou Context/Zustand/Redux)

const assinaturaStore = {
    // Estado
    situacao: null,        // 'TRIAL_ATIVO', 'ATIVA', etc.
    bloqueado: false,
    statusAssinatura: null,
    planoCodigo: null,
    planoNome: null,
    planoGratuito: false,
    diasRestantesTrial: null,
    dtFimTrial: null,
    temCobrancaPendente: false,
    valorPendente: null,
    dtAcessoAte: null,
    cicloCobranca: null,
    dtProximoVencimento: null,
    mensagem: null,

    // Actions
    atualizarAssinatura(assinaturaDTO) { ... },
    limparAssinatura() { ... },

    // Getters computados
    get deveMostrarBanner() {
        return ['TRIAL_ATIVO', 'PLANO_GRATUITO', 'PAGAMENTO_PENDENTE', 'CANCELADA_COM_ACESSO'].includes(this.situacao);
    },
    get deveBloquear() {
        return this.bloqueado;
    }
};
```

### Quando atualizar o store:
1. **Login** (`POST /auth/login`) → `response.organizacao.assinatura`
2. **Validacao de token** (`POST /auth/validate`) → `response.assinatura`
3. **Get user** (`GET /auth/me`) → `response.assinatura`
4. **Apos escolher plano** (`POST /assinatura/escolher-plano`) → recarregar status
5. **Apos cancelar** (`POST /assinatura/cancelar`) → recarregar status
6. **Apos reativar** (`POST /assinatura/reativar`) → recarregar status
7. **Polling periodico** (opcional): `GET /assinatura/status` a cada 5 min

---

## 8. Interceptor HTTP (Frontend)

### Tratar o HTTP 403 do AssinaturaInterceptor:

```javascript
// httpInterceptor.js
axios.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 403) {
            const data = error.response.data;
            if (data?.dados?.bloqueado) {
                // Atualizar store com os dados de bloqueio
                assinaturaStore.atualizarAssinatura({
                    bloqueado: true,
                    statusAssinatura: data.dados.statusAssinatura,
                    mensagem: data.dados.mensagem
                });
                // Redirecionar para tela de bloqueio
                navigate('/assinatura-bloqueada');
                return; // Nao propagar erro
            }
        }
        return Promise.reject(error);
    }
);
```

---

## 9. Pagina de Escolher Plano (atualizar)

### Mudancas:
- Apos selecionar plano e confirmar, o **Asaas gera a cobranca automaticamente**
- O frontend nao precisa mais se preocupar em criar cobranca local
- Apos `POST /assinatura/escolher-plano`, o response ja retorna o `AssinaturaResponseDTO` atualizado
- Redirecionar para dashboard e mostrar alerta: "Plano ativado! Sua primeira cobranca vence amanha."

### Fluxo:
```
1. Usuario seleciona plano, ciclo (mensal/anual), forma de pagamento
2. (Opcional) Aplica cupom → POST /assinatura/validar-cupom
3. Confirma → POST /assinatura/escolher-plano
4. Sucesso → Atualizar store de assinatura → Redirecionar para /dashboard
5. Erro → Mostrar mensagem de erro
```

---

## 10. Pagina Admin - Dashboard de Assinaturas

### Nenhuma mudanca critica:
- O endpoint `GET /admin/assinaturas/dashboard` continua funcionando igual
- Metricas (MRR, total por status) continuam vindo do backend

---

## 11. Checklist de Implementacao Frontend

### Prioridade Alta
- [ ] Atualizar handler do login para ler `situacao` ao inves de apenas `statusAssinatura`
- [ ] Criar/atualizar tela de bloqueio com conteudo dinamico por `situacao`
- [ ] Atualizar interceptor HTTP para tratar 403 do AssinaturaInterceptor
- [ ] Criar store/context global de assinatura
- [ ] Ler `assinatura` do response de `/auth/validate` e `/auth/me`

### Prioridade Media
- [ ] Criar banner de trial (com dias restantes e data de expiracao)
- [ ] Criar banner de plano gratuito (com CTA de upgrade)
- [ ] Criar alerta de pagamento pendente (com valor e data)
- [ ] Criar banner de cancelamento (com data de acesso restante)
- [ ] Atualizar tela de cobrancas para usar links do Asaas (sempre atuais)

### Prioridade Baixa
- [ ] Adicionar polling de status (`GET /assinatura/status`) a cada 5 min
- [ ] Animacao de transicao entre trial → plano gratuito
- [ ] Pagina de comparacao de planos com destaque para features bloqueadas
- [ ] Pagina de historico de pagamentos com filtros

---

## 12. Valores do Enum `situacao` (Referencia Rapida)

```typescript
type SituacaoAssinatura =
    | 'TRIAL_ATIVO'          // Trial em andamento
    | 'TRIAL_EXPIRADO'       // Trial acabou (bloqueado)
    | 'PLANO_GRATUITO'       // No plano free
    | 'ATIVA'                // Plano pago ativo
    | 'PAGAMENTO_PENDENTE'   // Ativa mas com cobranca pendente
    | 'PAGAMENTO_ATRASADO'   // Pagamento vencido (bloqueado)
    | 'CANCELADA_COM_ACESSO' // Cancelou mas ainda pode usar
    | 'CANCELADA_SEM_ACESSO' // Cancelou e periodo expirou (bloqueado)
    | 'SUSPENSA'             // Admin suspendeu (bloqueado)
    | 'SEM_ASSINATURA';      // Sem registro de assinatura (bloqueado)
```

---

## 13. Campos Novos no AssinaturaStatusDTO

| Campo | Tipo | Descricao | Quando aparece |
|-------|------|-----------|----------------|
| `situacao` | String | Enum semantico (ver tabela acima) | Sempre |
| `planoGratuito` | boolean | Se o plano atual e gratuito | Sempre |
| `dtFimTrial` | LocalDate | Data exata do fim do trial | Quando em TRIAL |
| `dtAcessoAte` | LocalDate | Ate quando pode usar apos cancelar | Quando CANCELADA_COM_ACESSO |
| `cicloCobranca` | String | MENSAL ou ANUAL | Quando tem plano ativo |
| `dtProximoVencimento` | LocalDate | Proxima renovacao | Quando tem plano ativo |
