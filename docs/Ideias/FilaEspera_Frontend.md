# Fila de Espera — Guia de integração Frontend

Doc complementar ao [`FilaEspera.md`](./FilaEspera.md) e [`FilaEspera_N8N.md`](./FilaEspera_N8N.md). Lista **todas as mudanças necessárias no frontend** para a feature funcionar end-to-end.

---

## Resumo executivo

| Área | Mudança | Onde |
|---|---|---|
| 1. Admin — Config Agendamento | 4 campos novos no formulário | Tela de configurações da org |
| 2. Site externo (booking público) | Checkbox "entrar na fila de espera" | Step final do agendamento |
| 3. Painel agendamento | Badge "Adiantado pela fila" + data original | Card/detalhe do agendamento |
| 4. Push notifications | Tratar `origem: FILA_ESPERA` no metadata | Componente de notificações |
| 5. (Opcional) Painel admin | Visualizar fila/histórico de tentativas | Aba nova ou drawer |

Tudo é incremental — o frontend pode ser entregue em fases. **Mínimo viável**: áreas 1 + 2.

---

## 1. Admin — Configurações da organização

### Endpoint
- `GET /api/v1/configuracao` → `ConfigSistemaDTO` (já contém `configAgendamento`)
- `PUT /api/v1/configuracao/agendamento` → corpo é `ConfigAgendamento` (entity flat)

### 4 campos novos em `configAgendamento`

```ts
interface ConfigAgendamento {
  // ... campos existentes ...

  /** Habilita a feature de fila de espera para a org. Default: false. */
  usarFilaEspera: boolean;

  /** Máximo de reagendamentos em cadeia por evento de cancelamento. Default: 5. */
  filaMaxCascata: number;

  /** Quanto tempo (em min) o cliente tem para responder à oferta. Default: 30. */
  filaTimeoutMinutos: number;

  /** Antecedência mínima (em h) para que um slot liberado seja oferecido pela fila. Default: 3. */
  filaAntecedenciaHoras: number;
}
```

### UI sugerida (na tela existente "Configurações de Agendamento")

```
┌─ Fila de Espera ──────────────────────────────────────────────┐
│                                                                │
│  [×] Habilitar fila de espera                                  │
│      Permite que clientes optem por receber adiantamento de    │
│      horário via WhatsApp quando outro cliente cancelar.       │
│                                                                │
│  ┌─ Configurações avançadas ──────────────────────────┐       │
│  │ Máximo de reagendamentos em cadeia (1–10)          │       │
│  │ [   5   ]                                           │       │
│  │                                                     │       │
│  │ Tempo de resposta do cliente (5–120 min)           │       │
│  │ [   30  ]                                           │       │
│  │                                                     │       │
│  │ Antecedência mínima do slot (1–24 h)               │       │
│  │ [   3   ]                                           │       │
│  └─────────────────────────────────────────────────────┘       │
└────────────────────────────────────────────────────────────────┘
```

**Comportamento:**
- "Configurações avançadas" só aparecem se `usarFilaEspera = true`.
- Validações sugeridas:
  - `filaMaxCascata`: 1–10 (limite duro do banco é INTEGER, mas 10 já é generoso)
  - `filaTimeoutMinutos`: 5–120
  - `filaAntecedenciaHoras`: 1–24

**Tooltip explicativo no checkbox:** "Quando alguém cancelar um agendamento, o sistema oferece o horário (via WhatsApp) ao próximo cliente da fila que tenha agendamento posterior compatível. Se aceitar, o agendamento dele é adiantado."

---

## 2. Site externo (booking público) — Checkbox "entrar na fila"

### Endpoint
- `POST /api/v1/agendamento` — adicionado **um novo campo opcional** no payload:

```ts
interface AgendamentoCreateDTO {
  // ... campos existentes ...
  organizacaoId: number;
  clienteId: number;
  servicoIds: number[];
  funcionarioIds: number[];
  dtAgendamento: string; // ISO datetime
  observacao?: string;
  requerSinal?: boolean;
  // ... etc ...

  /** Cliente quer entrar na fila para receber adiantamento se surgir vaga antes. Default: false. */
  entrarFilaEspera?: boolean;
}
```

### UI sugerida (no step final do booking, depois de selecionar data/hora)

```
┌─────────────────────────────────────────────────────────────────┐
│  Resumo do agendamento                                           │
│                                                                  │
│  Data: 20/05/2026 às 14:00                                       │
│  Profissional: Maria                                             │
│  Serviço: Corte + escova (90min)                                 │
│  Valor: R$ 120,00 (sinal: R$ 36,00)                              │
│                                                                  │
│  ┌─ Fila de espera (opcional) ──────────────────────────┐        │
│  │ [×] Avise-me se surgir um horário antes              │        │
│  │                                                      │        │
│  │ Se outro cliente cancelar e abrir uma vaga antes do  │        │
│  │ seu horário com o mesmo profissional, enviamos uma   │        │
│  │ mensagem no WhatsApp e você decide se quer adiantar. │        │
│  └──────────────────────────────────────────────────────┘        │
│                                                                  │
│  [ Confirmar e pagar sinal ]                                     │
└─────────────────────────────────────────────────────────────────┘
```

### Comportamento

1. Default `false` (desmarcado).
2. **Só renderiza o checkbox se `configSistema.configAgendamento.usarFilaEspera === true`** — caso contrário esconde inteiramente o bloco. O frontend já pode buscar essa config no carregamento da página de booking público (via `GET /api/v1/configuracao` ou endpoint público equivalente).
3. Envia `entrarFilaEspera: true` apenas quando o usuário marcar — caso contrário pode omitir do payload (default no backend é `false`).
4. **Não há feedback visual diferente após o agendamento** — a fila funciona em background.

> **Importante:** o checkbox aparece sempre (quando habilitado pela org), não só quando o horário desejado está cheio. A fila é proativa: o cliente já agendou um dia, e quer ser avisado se aparecer um dia melhor antes.

---

## 3. Painel agendamento — Indicação de "adiantado pela fila"

### Endpoint
- `GET /api/v1/agendamento/{id}` (já existente) — agora retorna 3 campos novos no `AgendamentoDTO`:

```ts
interface AgendamentoDTO {
  // ... campos existentes ...

  /** Cliente está na fila aguardando possível adiantamento. */
  entrouFilaEspera: boolean;

  /** Agendamento foi adiantado via fila (true depois que cliente aceitou uma oferta). */
  reagendadoPorFila: boolean;

  /** dt do agendamento ANTES do primeiro adiantamento via fila. Usar para mostrar
   *  "Foi adiantado de DD/MM HH:mm" no histórico. */
  dtOriginalFila: string | null; // ISO datetime
}
```

### UI sugerida

**No card do agendamento (lista):**
```
┌──────────────────────────────────────────────────────┐
│  📅  20/05  14:00  Maria — Corte + escova           │
│                                                      │
│  🚀 Adiantado pela fila (era 25/05 às 16:00)        │ ← se reagendadoPorFila
│  ⏳ Em fila de espera                                │ ← se entrouFilaEspera (e !reagendado)
└──────────────────────────────────────────────────────┘
```

**No detalhe do agendamento (drawer/página):**
- Se `reagendadoPorFila = true`: badge azul "Adiantado pela fila" + linha no histórico:
  > "Reagendado de **25/05/2026 às 16:00** para **20/05/2026 às 14:00** (via fila de espera)"
- Se `entrouFilaEspera = true` e `!reagendadoPorFila`: badge cinza "Na fila — aguardando vaga antecipada".

**Cores sugeridas:**
- `reagendadoPorFila` → cor de sucesso/destaque (verde ou roxo)
- `entrouFilaEspera` (sem ainda ter reagendado) → cor neutra/aviso (cinza ou âmbar)

---

## 4. Push notifications — Distinguir origem "FILA_ESPERA"

Quando um cliente aceita uma oferta de fila, o backend dispara um push para admin/funcionário/recepção. O componente que renderiza notificações push já existe — **só precisa interpretar 1 metadata novo**:

```json
{
  "agendamentoId": 123,
  "clienteId": 456,
  "nomeCliente": "João Silva",
  "dtAgendamentoOriginal": "2026-05-25T16:00:00",
  "dtAgendamentoNova": "2026-05-20T14:00:00",
  "origem": "FILA_ESPERA"
}
```

A notificação chega como `categoria: AGENDAMENTO`, `prioridade: ALTA`. Tipo (`tipoEvento`) é `"AGENDAMENTO"` — o que diferencia é o `metadata.origem`.

### UI sugerida

Quando `metadata.origem === "FILA_ESPERA"`:
- Ícone diferente (ex: 🚀 ou ⚡) em vez do ícone padrão de agendamento
- Título: "Agendamento adiantado pela fila"
- Subtítulo destacando ambas as datas (`X foi adiantado de A para B`)
- Sem alteração no roteamento (clique continua indo para `/agendamentos/{id}`)

---

## 5. (Opcional) Painel admin — Lista da fila de espera

> Esta área não é obrigatória pra feature funcionar — é uma melhoria de UX para o admin acompanhar a fila. O endpoint já existe.

### Endpoints
- `GET /api/v1/webhook/fila-espera/agendamento/{agendamentoId}` → lista todas as tentativas (ativas e finalizadas) de um agendamento específico
- *Não há endpoint "listar tudo da org" ainda — pode ser PR futura se for útil*

### Resposta
```ts
interface FilaEsperaTentativaDTO {
  id: number;
  agendamentoId: number;
  clienteId: number;
  nomeCliente: string;
  telefoneCliente: string;
  funcionarioId: number;
  slotInicio: string;       // ISO datetime
  slotFim: string;
  dtAgendamentoOriginal: string; // dtAgendamento ATUAL do cliente alvo (a vaga que ele tem hoje)
  status:
    | "PENDENTE"
    | "ENVIADO"
    | "AGUARDANDO_RESPOSTA"
    | "ACEITO"
    | "RECUSADO"
    | "EXPIRADO"
    | "SUPERADO"
    | "FALHA";
  dtEnvio: string | null;
  dtResposta: string | null;
  dtExpira: string;
  cascataNivel: number;     // 1..5
  finalizada: boolean;
}
```

### UI sugerida (drawer "Histórico de fila" no detalhe do agendamento)
```
┌─ Histórico da fila — Agendamento #123 ─────────────────┐
│                                                          │
│  ✅ #45  ACEITO  nivel 1   Maria → 20/05 14:00          │
│      Enviado 28/04 09:00 | Respondido 28/04 09:08        │
│                                                          │
│  ⏰ #44  EXPIRADO  nivel 1   Maria → 20/05 14:00         │
│      Enviado 28/04 08:30 | Sem resposta                  │
│                                                          │
│  🚫 #43  SUPERADO  nivel 1   Maria → 20/05 14:00         │
│      Cliente cancelou agendamento original              │
└──────────────────────────────────────────────────────────┘
```

Mostra a cronologia (mais recente em cima) e ajuda o admin a entender por que um cliente foi adiantado / por que outro não foi.

---

## 6. Endpoints de referência (quick reference)

| Função | Método | Path |
|---|---|---|
| Buscar config (inclui fila) | GET | `/api/v1/configuracao` |
| Salvar config agendamento | PUT | `/api/v1/configuracao/agendamento` |
| Criar agendamento (com `entrarFilaEspera`) | POST | `/api/v1/agendamento` |
| Buscar agendamento (mostra `reagendadoPorFila`/`dtOriginalFila`) | GET | `/api/v1/agendamento/{id}` |
| Listar tentativas de um agendamento | GET | `/api/v1/webhook/fila-espera/agendamento/{id}` |
| Aceitar oferta (geralmente N8N) | POST | `/api/v1/webhook/fila-espera/tentativa/{id}/aceitar` |
| Recusar oferta (geralmente N8N) | POST | `/api/v1/webhook/fila-espera/tentativa/{id}/recusar` |
| Status da tentativa | GET | `/api/v1/webhook/fila-espera/tentativa/{id}/status` |

---

## 7. O que **não** muda

- Disponibilidade (`POST /api/v1/agendamento/disponibilidade`): o backend automaticamente esconde slots reservados para a fila. Frontend não precisa fazer nada.
- Cancelamento: quando um cliente em fila cancela seu próprio agendamento, o backend já invalida as tentativas e segue pro próximo da fila — frontend não precisa orquestrar nada.
- Reagendamento via UI normal: continua funcionando, sem interação com fila.

---

## 8. Sequência de entrega sugerida

1. **Sprint 1 — Mínimo viável:**
   - Adicionar 4 campos no formulário de Configurações de Agendamento (área 1)
   - Adicionar checkbox no booking público condicional ao `usarFilaEspera` (área 2)
   - Sem isso, o feature está "construído mas nada chega no banco"

2. **Sprint 2 — Visibilidade:**
   - Mostrar badge `reagendadoPorFila` + `dtOriginalFila` nos cards/detalhes (área 3)
   - Tratar metadata `origem: FILA_ESPERA` em pushes (área 4)

3. **Sprint 3+ — Melhoria admin (opcional):**
   - Drawer com histórico de tentativas no detalhe do agendamento (área 5)
   - Idealmente integrar com a evolução do bot N8N descrita em `FilaEspera_N8N.md`

---

## 9. Como testar end-to-end no frontend

Pré-requisitos:
- Backend com PRs 1–6 aplicadas
- Pelo menos 1 instância WhatsApp (Evolution API) conectada na org
- Feature flag habilitada (`PUT /configuracao/agendamento` com `usarFilaEspera: true`)

Passos:
1. **Cliente A** agenda no booking público — data X+10 dias, sem marcar checkbox de fila.
2. **Cliente B** agenda — data X+15 dias, **marcando** "entrar na fila".
3. No painel admin, cancelar o agendamento de A.
4. Aguardar ~10 segundos. Cliente B deve receber WhatsApp com a oferta.
5. Cliente B responde "SIM" → painel deve mostrar:
   - Card de B com badge "Adiantado pela fila"
   - Push notification para admin/funcionário com `origem: FILA_ESPERA`
6. (Opcional) Cancelar o agendamento adiantado de B → cascata oferece pro próximo da fila se houver.
