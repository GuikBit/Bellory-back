# Questionários — Guia de Frontend

Documento completo para construir o módulo de Questionários no front-end. Cobre toda a hierarquia (Questionário → Pergunta → Opção → Resposta), endpoints, tipos, regras de validação, formato dos payloads/respostas e fluxos de UX recomendados.

> Base URL da API: `/api/v1`
> Todos os endpoints exigem autenticação JWT (header `Authorization: Bearer <token>`) e são automaticamente filtrados por `organizacao_id` (multitenancy via TenantContext).

---

## 1. Visão Geral

O módulo de Questionários permite à organização criar formulários dinâmicos com diferentes tipos de perguntas, coletar respostas e analisar estatísticas. Os principais casos de uso são:

- **Cadastro** de clientes/colaboradores com perguntas customizadas.
- **Avaliação de desempenho** de funcionários.
- **Feedback** pós-atendimento, pós-agendamento, pós-bot.
- **Pesquisa de satisfação** (NPS, CSAT).

### Modelo hierárquico

```
Questionário (titulo, tipo, ativo, anonimo, obrigatorio, ...)
  └─ Pergunta (texto, tipo, obrigatoria, ordem, validações...)
       └─ OpcaoResposta (texto, valor, ordem)   ← apenas para SELECAO_UNICA / SELECAO_MULTIPLA

RespostaQuestionario (cliente, agendamento, dispositivo, IP, tempo...)
  └─ RespostaPergunta (texto | número | data | hora | opcaoIds)
```

---

## 2. Tipos de Questionário (`TipoQuestionario`)

Use no campo `tipo` do questionário. Valor enviado/recebido = nome literal do enum.

| Enum | Descrição | Sugestão de uso |
|---|---|---|
| `CLIENTE` | Cadastro de Cliente | Onboarding/perfil do cliente |
| `COLABORADOR` | Cadastro de Colaborador | Onboarding interno |
| `AVALIACAO_DESEMPENHO` | Avaliação de Desempenho | Avaliações periódicas de funcionários |
| `FEEDBACK_ATENDIMENTO` | Feedback de Atendimento | Após interação com atendente |
| `FEEDBACK_AGENDAMENTO` | Feedback de Agendamento | Pós-serviço (vincular `agendamentoId`) |
| `FEEDBACK_BOT` | Feedback do Bot | Avaliação de chatbot/IA |
| `FEEDBACK_GERAL` | Feedback Geral | Caixa de sugestões |
| `PESQUISA_SATISFACAO` | Pesquisa de Satisfação | NPS/CSAT |
| `OUTRO` | Outro | Casos avulsos |

---

## 3. Tipos de Pergunta (`TipoPergunta`)

Cada tipo determina **quais campos** a Pergunta utiliza e **como a Resposta é enviada**.

| Enum | Descrição | Campos usados na Pergunta | Como o usuário responde |
|---|---|---|---|
| `TEXTO_CURTO` | Texto Curto | `minCaracteres`, `maxCaracteres` | `respostaTexto` (string) |
| `TEXTO_LONGO` | Texto Longo | `minCaracteres`, `maxCaracteres` | `respostaTexto` (string, ex: textarea) |
| `NUMERO` | Número | `minValor`, `maxValor` | `respostaNumero` (BigDecimal) |
| `SELECAO_UNICA` | Seleção Única | `opcoes[]` | `respostaOpcaoIds` (array com **1** id) |
| `SELECAO_MULTIPLA` | Seleção Múltipla | `opcoes[]` | `respostaOpcaoIds` (array com 1+ ids) |
| `ESCALA` | Escala Linear | `escalaMin`, `escalaMax`, `labelMin`, `labelMax` | `respostaNumero` (inteiro entre escalaMin e escalaMax) |
| `DATA` | Data | — | `respostaData` (`YYYY-MM-DD`) |
| `HORA` | Hora | — | `respostaHora` (`HH:mm:ss`) |
| `AVALIACAO_ESTRELAS` | Avaliação por Estrelas | — (fixo 1–5) | `respostaNumero` (1 a 5) |
| `SIM_NAO` | Sim/Não | — | `respostaTexto` exatamente `"Sim"` ou `"Não"` |

### Tabela cruzada: tipo → campo de resposta

| Tipo | `respostaTexto` | `respostaNumero` | `respostaOpcaoIds` | `respostaData` | `respostaHora` |
|---|:-:|:-:|:-:|:-:|:-:|
| TEXTO_CURTO / TEXTO_LONGO | ✅ | | | | |
| NUMERO / ESCALA / AVALIACAO_ESTRELAS | | ✅ | | | |
| SELECAO_UNICA / SELECAO_MULTIPLA | | | ✅ | | |
| DATA | | | | ✅ | |
| HORA | | | | | ✅ |
| SIM_NAO | ✅ (`"Sim"` / `"Não"`) | | | | |

> O backend valida estritamente: campo de resposta de outro tipo é ignorado, mas o **tipo correto** precisa estar preenchido se a pergunta for obrigatória.

---

## 4. Envelope padrão da API (`ResponseAPI<T>`)

Toda resposta segue:

```json
{
  "success": true,
  "message": "Mensagem opcional",
  "dados": { /* T (objeto, lista ou null) */ },
  "errorCode": null,
  "errors": null
}
```

Em erro, `success=false`, `errorCode` recebe o HTTP status (400, 403, 404, 500) e `message` traz a descrição. Faça parsing único em todos os fetches.

---

## 5. Endpoints — Questionários

Base: `/api/v1/questionarios`

### 5.1 Criar questionário

```
POST /api/v1/questionarios
```

**Request body** (`QuestionarioCreateDTO`):

```json
{
  "titulo": "Pesquisa de Satisfação",
  "descricao": "Avalie nosso atendimento",
  "tipo": "PESQUISA_SATISFACAO",
  "ativo": true,
  "obrigatorio": false,
  "anonimo": false,
  "urlImagem": "https://cdn.../banner.png",
  "corTema": "#FF6B6B",
  "perguntas": [
    {
      "texto": "Qual seu nível de satisfação?",
      "descricao": "De 0 a 10",
      "tipo": "ESCALA",
      "obrigatoria": true,
      "ordem": 1,
      "escalaMin": 0,
      "escalaMax": 10,
      "labelMin": "Péssimo",
      "labelMax": "Excelente"
    },
    {
      "texto": "O que mais gostou?",
      "tipo": "SELECAO_MULTIPLA",
      "obrigatoria": false,
      "ordem": 2,
      "opcoes": [
        { "texto": "Atendimento", "valor": "atendimento", "ordem": 1 },
        { "texto": "Preço", "valor": "preco", "ordem": 2 },
        { "texto": "Qualidade", "valor": "qualidade", "ordem": 3 }
      ]
    }
  ]
}
```

**Response** `201 CREATED` → `dados: QuestionarioDTO` (com `id` e `perguntas[].id` gerados).

**Erros**: `400` (validação), `403` (sem org no token).

### 5.2 Atualizar questionário

```
PUT /api/v1/questionarios/{id}
```

Mesmo body de criação. **Importante:** o backend faz `clearPerguntas()` e recria todas as perguntas — então **envie sempre o array completo**, mesmo as que não mudaram. IDs antigos de perguntas/opções são descartados.

**Response** `200 OK` → `dados: QuestionarioDTO`.
**Erros**: `404` se não encontrar, `403` se for de outra org.

### 5.3 Buscar por ID

```
GET /api/v1/questionarios/{id}
```

Retorna o questionário **com perguntas e opções carregadas**. Use para a tela de edição/visualização.

**Response** `200 OK` → `dados: QuestionarioDTO` (com `totalRespostas` preenchido).

### 5.4 Listar todos (não paginado)

```
GET /api/v1/questionarios
```

Lista todos os questionários da organização, **ordenados por `dtCriacao DESC`**, incluindo inativos (mas não deletados).

**Response** `200 OK` → `dados: QuestionarioDTO[]`

### 5.5 Listar somente ativos

```
GET /api/v1/questionarios/ativos
```

Filtra `ativo=true` e `is_deletado=false`. Use para públicos finais (cliente respondendo).

### 5.6 Listar por tipo

```
GET /api/v1/questionarios/tipo/{tipo}
```

`{tipo}` = qualquer valor de `TipoQuestionario`. Ex: `/tipo/FEEDBACK_AGENDAMENTO`.

### 5.7 Pesquisar (paginado)

```
GET /api/v1/questionarios/pesquisar?termo=<texto>&page=0&size=20&sort=dtCriacao,desc
```

Busca `LIKE` em `titulo` ou `descricao` (case-insensitive). Retorna `Page<QuestionarioDTO>`:

```json
{
  "success": true,
  "dados": {
    "content": [ /* QuestionarioDTO[] */ ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0,
    "size": 20
  }
}
```

### 5.8 Deletar (soft delete)

```
DELETE /api/v1/questionarios/{id}
```

Marca `is_deletado=true`. As respostas existentes permanecem (relatórios continuam funcionando).

### 5.9 Total de respostas

```
GET /api/v1/questionarios/{id}/total-respostas
```

Retorna `dados: <Long>` — útil para badges de listagem sem precisar carregar respostas.

---

## 6. Endpoints — Respostas de Questionário

Base: `/api/v1/questionarios/{questionarioId}/respostas`

### 6.1 Registrar resposta

```
POST /api/v1/questionarios/{questionarioId}/respostas
```

**Request body** (`RespostaQuestionarioCreateDTO`):

```json
{
  "clienteId": 12,
  "colaboradorId": null,
  "agendamentoId": 89,
  "userAgent": "Mozilla/5.0...",
  "dispositivo": "MOBILE_IOS",
  "tempoPreenchimentoSegundos": 87,
  "respostas": [
    { "perguntaId": 16, "respostaTexto": "Ótimo serviço!" },
    { "perguntaId": 17, "respostaNumero": 9 },
    { "perguntaId": 18, "respostaOpcaoIds": [101, 103] },
    { "perguntaId": 19, "respostaTexto": "Sim" },
    { "perguntaId": 20, "respostaData": "2026-04-25" },
    { "perguntaId": 21, "respostaHora": "14:30:00" }
  ]
}
```

> O `questionarioId` no body é **sobrescrito** pela URL — pode omitir.

**Validações automáticas no backend**:
- Questionário precisa estar `ativo=true` (senão 400 "não está mais aceitando respostas").
- **Cliente pode responder o mesmo questionário várias vezes** (sem bloqueio de duplicidade por `clienteId`).
- Se `agendamentoId` foi enviado, bloqueia duplo registro para o mesmo agendamento (1 agendamento = 1 avaliação).
- Todas as perguntas obrigatórias precisam estar respondidas.
- Cada resposta valida limites do tipo (min/max caracteres, escala, intervalo de opções, etc.).

**Response** `201 CREATED` → `dados: RespostaQuestionarioDTO` (com `respostaFormatada` calculado por pergunta — ex: estrelas viram `"★★★★☆"`).

**Erros comuns** (`400`):
- "Este agendamento já foi avaliado."
- "Resposta deve ter no mínimo X caracteres."
- "Selecione apenas uma opção para a pergunta '...'."
- "Avaliação deve ser entre 1 e 5 estrelas."
- "A pergunta '...' é obrigatória."

### 6.2 Buscar resposta por ID

```
GET /api/v1/questionarios/{questionarioId}/respostas/{respostaId}
```

### 6.3 Listar respostas (paginado)

```
GET /api/v1/questionarios/{questionarioId}/respostas?page=0&size=20&sort=dtResposta,desc
```

Retorna `Page<RespostaQuestionarioDTO>`.

### 6.4 Listar por período

```
GET /api/v1/questionarios/{questionarioId}/respostas/periodo?inicio=2026-04-01T00:00:00&fim=2026-04-30T23:59:59&page=0&size=20
```

Datas em ISO-8601 (`yyyy-MM-ddTHH:mm:ss`).

### 6.5 Verificar se cliente já respondeu

```
GET /api/v1/questionarios/{questionarioId}/respostas/verificar?clienteId=12
```

Resposta:
```json
{ "success": true, "dados": { "respondido": true } }
```

Indica apenas se o cliente já tem **alguma** resposta registrada — útil para mostrar histórico ou aviso "você já respondeu antes". **Não bloqueia novas respostas** — o cliente pode responder o mesmo questionário quantas vezes quiser.

### 6.6 Histórico de respostas de um cliente

```
GET /api/v1/questionarios/{questionarioId}/respostas/cliente/{clienteId}/historico
```

Retorna **lista** de todas as respostas que o cliente deu para o questionário, ordenadas por `dtResposta DESC` (mais recentes primeiro). Cada item é um `RespostaQuestionarioDTO` completo (com perguntas, opções, `respostaFormatada`, `clienteNome`).

**Response** `200 OK` → `dados: RespostaQuestionarioDTO[]`. Retorna `[]` (lista vazia) se não houver respostas — **não retorna 404**.

**Casos de uso**: ficha do cliente com timeline de respostas; comparação entre respostas no tempo (ex: medir evolução em pesquisa de satisfação periódica); auditoria. Para "última resposta" use `historico[0]`.

### 6.7 Verificar se agendamento já foi avaliado

```
GET /api/v1/questionarios/{questionarioId}/respostas/verificar-agendamento?agendamentoId=89
```

Resposta: `{ "avaliado": true|false }`. Útil em fluxos pós-atendimento.

### 6.8 Estatísticas (dashboard)

```
GET /api/v1/questionarios/{questionarioId}/respostas/estatisticas
```

Retorna `EstatisticasQuestionarioDTO` completo (ver schema abaixo) — agregado pronto para gráficos.

### 6.9 NPS (Net Promoter Score)

```
GET /api/v1/questionarios/{questionarioId}/respostas/nps?perguntaId=17
```

`perguntaId` deve apontar para uma pergunta `ESCALA` (idealmente 0–10). Retorna:

```json
{ "success": true, "dados": { "nps": 42.5 } }
```

Cálculo: `% promotores (9–10) − % detratores (0–6)`. Faixa: −100 a +100. Retorna `0.0` se ainda não há respostas.

### 6.10 Relatório por período

```
GET /api/v1/questionarios/{questionarioId}/respostas/relatorio?inicio=2026-04-01T00:00:00&fim=2026-04-30T23:59:59
```

Retorna `RelatorioRespostasDTO` com **todas as respostas do período + estatísticas agregadas + contagens distintas** de clientes e agendamentos. Pode ser pesado — use com filtros de período.

### 6.11 Deletar resposta

```
DELETE /api/v1/questionarios/{questionarioId}/respostas/{respostaId}
```

Hard delete (cascateia as `RespostaPergunta`).

---

## 7. Endpoints Públicos (sem JWT)

Base: `/api/v1/public/{slug}/questionarios`

Endpoints abertos para uso da landing page / fluxo do cliente final, **sem autenticação JWT**. A organização é resolvida pelo `{slug}` da URL (mesmo slug do `PublicBookingController`). O backend exige que o `SitePublicoConfig` da organização esteja `active=true` — caso contrário retorna `404 NOT FOUND` com mensagem "Organização não encontrada ou site indisponível".

**Rate limiting** (por IP):
- GETs: 60 requests/hora
- POSTs: 10 requests/hora
- Excedido → `429 TOO MANY REQUESTS`.

### 7.1 Buscar questionário público

```
GET /api/v1/public/{slug}/questionarios/{id}
```

Retorna `QuestionarioDTO` com perguntas e opções carregadas. Backend exige `ativo=true` — se inativo, retorna `404` "Este questionário não está disponível.".

**Response** `200 OK` → `dados: QuestionarioDTO`.
**Response** `404 NOT FOUND` → questionário inexistente, inativo, ou de outra organização.

### 7.2 Registrar resposta pública

```
POST /api/v1/public/{slug}/questionarios/{id}/respostas
```

Body idêntico ao `RespostaQuestionarioCreateDTO` da seção 6.1. Mesmas validações (obrigatórias, tipos, anti-duplicidade por cliente/agendamento). O backend:
- Sobrescreve `questionarioId` do body com o `{id}` da URL.
- Preenche `userAgent` automaticamente se não vier no body (a partir do header `User-Agent` da request).
- Captura o IP via `X-Forwarded-For`/`X-Real-IP`/`RemoteAddr`.
- Valida que o questionário pertence à organização do slug.

**Response** `201 CREATED` → `dados: RespostaQuestionarioDTO`.
**Erros** `400` → mesmas mensagens da seção 6.1.

### 7.3 Verificar se já respondeu (público)

```
GET /api/v1/public/{slug}/questionarios/{id}/respostas/verificar?clienteId=12
GET /api/v1/public/{slug}/questionarios/{id}/respostas/verificar?clienteId=12&agendamentoId=74
```

Retorna `{ "respondido": true|false }`. Use **antes** de exibir o formulário (evita 400 ao submeter).

**`clienteId` é obrigatório, `agendamentoId` é opcional** — escolha o modo conforme o tipo de questionário:

| Cenário | Passar `agendamentoId`? | Lógica aplicada |
|---|---|---|
| Anamnese pré-procedimento (link do WhatsApp tem `?agendamento=...`) | **Sim** | Verifica se aquele agendamento específico já foi respondido. Cliente pode ter feito anamnese em outro agendamento e ainda assim PRECISA responder neste. |
| Cadastro de cliente, pesquisa de satisfação geral, NPS sem vínculo a atendimento | Não | Verifica se o cliente já tem **alguma** resposta para o questionário. |

> **Regra prática para o front**: se a query string da página de resposta (`/avaliacao/{slug}/{id}/responder?...`) tiver `agendamento=`, repasse esse valor como `agendamentoId` na chamada de `/verificar`. Caso contrário, omita.

Sem o `agendamentoId` no caso de anamnese, o backend pode retornar `respondido: true` por causa de uma anamnese antiga e o front mostraria "avaliação já registrada" indevidamente.

---

## 8. Schemas detalhados

### 8.1 `QuestionarioDTO`

```ts
{
  id: number;
  organizacaoId: number;
  titulo: string;            // max 255
  descricao: string | null;  // max 1000
  tipo: TipoQuestionario;
  perguntas: PerguntaDTO[];
  ativo: boolean;
  obrigatorio: boolean;
  anonimo: boolean;
  urlImagem: string | null;  // max 500
  corTema: string | null;    // max 7 (ex: "#RRGGBB")
  dtCriacao: string;         // ISO-8601
  dtAtualizacao: string | null;
  totalRespostas: number;    // calculado server-side
}
```

### 8.2 `PerguntaDTO`

```ts
{
  id: number;
  texto: string;             // max 500, obrigatório
  descricao: string | null;  // max 1000
  tipo: TipoPergunta;
  obrigatoria: boolean;
  ordem: number;             // inteiro, obrigatório
  opcoes: OpcaoRespostaDTO[]; // somente SELECAO_*

  // Tipo ESCALA
  escalaMin: number | null;
  escalaMax: number | null;
  labelMin: string | null;   // max 100
  labelMax: string | null;   // max 100

  // Tipo TEXTO_CURTO/LONGO
  minCaracteres: number | null;
  maxCaracteres: number | null;

  // Tipo NUMERO
  minValor: number | null;   // BigDecimal precision=15 scale=4
  maxValor: number | null;
}
```

### 8.3 `OpcaoRespostaDTO`

```ts
{
  id: number;
  texto: string;     // max 255, obrigatório
  valor: string | null; // valor "código" opcional, ex: "atendimento"
  ordem: number;     // obrigatório
}
```

### 8.4 `RespostaQuestionarioDTO`

```ts
{
  id: number;
  questionarioId: number;
  questionarioTitulo: string;
  clienteId: number | null;
  clienteNome: string | null;       // populado a partir de Cliente.nomeCompleto
  colaboradorId: number | null;
  colaboradorNome: string | null;   // populado a partir de Funcionario.nomeCompleto
  agendamentoId: number | null;
  respostas: RespostaPerguntaDTO[];
  dtResposta: string;               // ISO-8601
  ipOrigem: string | null;
  dispositivo: string | null;
  tempoPreenchimentoSegundos: number | null;
}
```

> `clienteNome`/`colaboradorNome` ficam `null` apenas se o `clienteId`/`colaboradorId` estiver ausente, ou se o registro foi deletado posteriormente — nesse caso, faça fallback para `Cliente #${id}` no front.

### 8.5 `RespostaPerguntaDTO`

```ts
{
  id: number;
  perguntaId: number;
  perguntaTexto: string;
  tipoPergunta: TipoPergunta;
  respostaTexto: string | null;
  respostaNumero: number | null;
  opcoesSelecionadas: { id, texto, valor }[];
  respostaData: string | null;   // YYYY-MM-DD
  respostaHora: string | null;   // HH:mm:ss
  respostaFormatada: string | null; // ex: "★★★★☆", "Sim", "Atendimento, Qualidade"
}
```

> Use `respostaFormatada` direto em listagens — economiza lógica no front.

### 8.6 `EstatisticasQuestionarioDTO`

```ts
{
  questionarioId: number;
  questionarioTitulo: string;
  totalRespostas: number;
  respostasHoje: number;
  respostasUltimos7Dias: number;
  respostasUltimos30Dias: number;
  mediaTempoPreenchimentoSegundos: number | null;
  taxaConclusao: number | null;          // % preenchimento real (0–100)
  primeiraResposta: string | null;
  ultimaResposta: string | null;
  estatisticasPerguntas: EstatisticasPerguntaDTO[];
  respostasPorDia: { [yyyy_MM_dd: string]: number } | null;  // últimos 30 dias
  respostasPorHora: { [hh: string]: number } | null;          // chave "00"–"23"
}
```

> Todos os campos acima são populados pelo backend. `taxaConclusao` = `(respostasPreenchidas / (totalRespostas × totalPerguntas)) × 100`. Vem `null` quando não há respostas.

### 8.7 `EstatisticasPerguntaDTO`

```ts
{
  perguntaId: number;
  perguntaTexto: string;
  tipo: TipoPergunta;
  totalRespostas: number;
  respostasEmBranco: number;

  // Numéricas (NUMERO/ESCALA/AVALIACAO_ESTRELAS)
  media: number | null;
  mediana: number | null;          // calculada em memória sobre lista ordenada
  moda: number | null;             // valor mais frequente da distribuicaoNotas
  desvioPadrao: number | null;
  valorMinimo: number | null;
  valorMaximo: number | null;
  distribuicaoNotas: { [nota: number]: number } | null;

  // Seleção (SELECAO_UNICA / SELECAO_MULTIPLA)
  estatisticasOpcoes: { opcaoId, opcaoTexto, totalSelecoes, percentual }[] | null;

  // SIM_NAO
  totalSim: number | null;
  totalNao: number | null;
  percentualSim: number | null;

  // Texto (TEXTO_CURTO / TEXTO_LONGO)
  mediaCaracteres: number | null;
  palavrasFrequentes: string[] | null;  // top 10 (sem stopwords PT-BR, mín. 3 chars)
}
```

> Todos os campos vêm preenchidos pelo backend. Os opcionais (`null`) ocorrem quando não há dados suficientes (ex: pergunta sem nenhuma resposta numérica → `media`/`mediana`/`moda` ficam `null`).

---

## 9. Regras e validações (espelhar no front)

### Geral
- `titulo`: obrigatório, 1–255 chars.
- `descricao`: ≤ 1000 chars.
- `corTema`: hex 7 chars (`#RRGGBB`).
- `urlImagem`: ≤ 500 chars.

### Pergunta
- `texto`: obrigatório, ≤ 500 chars.
- `tipo`: obrigatório.
- `ordem`: obrigatório (inteiro). O backend renumera se vier null, mas envie sempre.
- `obrigatoria`: default `false`.

### Por tipo de pergunta
- **TEXTO_CURTO/LONGO**: se `minCaracteres` definido, validar; idem `maxCaracteres`. Convencional: TEXTO_CURTO ≤ 255, TEXTO_LONGO até 5000.
- **NUMERO**: `minValor`/`maxValor` (BigDecimal). Aceita decimais.
- **ESCALA**: defaults backend = `escalaMin=1`, `escalaMax=10`. Sugiro forçar o usuário a definir. `labelMin/Max` opcionais — exibir nos extremos da escala.
- **AVALIACAO_ESTRELAS**: fixo 1–5. Não envie escalaMin/Max.
- **SELECAO_UNICA / SELECAO_MULTIPLA**: exigir pelo menos 2 opções no front (UX). Backend não exige mínimo, mas faz com que opções vazias sejam inúteis.
- **SIM_NAO**: front renderiza dois botões/radios; envia exatamente `"Sim"` ou `"Não"` em `respostaTexto`.
- **DATA**: input date HTML; envia `YYYY-MM-DD`.
- **HORA**: input time; envia `HH:mm:ss` (zero-pad).

### Resposta
- Pelo menos 1 item em `respostas[]` (`@NotEmpty`).
- Se a pergunta é `obrigatoria=true`, o item correspondente precisa ter o campo do tipo correto preenchido.
- **SELECAO_UNICA** com mais de 1 id → 400.
- IDs de opções precisam pertencer à pergunta; senão 400 "Opção selecionada inválida: X".
- **Anti-duplicidade**: cliente não-anônimo só responde 1x; agendamento só pode ter 1 resposta.

---

## 10. Fluxos de UX recomendados

### 10.1 Tela "Construtor de Questionário"
1. Form principal: título, descrição, tipo (`<select>` populado com `TipoQuestionario`), corTema (color picker), urlImagem (upload), flags (`ativo`, `obrigatorio`, `anonimo`).
2. Lista dinâmica de perguntas com drag-and-drop atualizando `ordem`.
3. Cada pergunta: ao mudar `tipo`, **renderizar apenas os campos relevantes** (vide tabela seção 3) — esconder os demais e zerá-los antes de submit para não enviar lixo.
4. Para tipos de seleção: editor inline de opções com botão "+ adicionar opção".
5. Validar no front antes de POST (replicar regras seção 9) para evitar round-trip.
6. Em edição (`PUT`): **carregar via `GET /{id}`** e enviar payload completo no submit (perguntas são recriadas).

### 10.2 Tela "Listagem"
- Tabs: **Todos** (`GET /`) | **Ativos** (`GET /ativos`) | **Por tipo** (`GET /tipo/{tipo}`).
- Barra de busca → `GET /pesquisar` (paginado) com debounce 400ms.
- Cards mostram: título, tipo (badge), `totalRespostas` (badge), status `ativo`, ações (Editar / Estatísticas / Deletar).

### 10.3 Tela "Responder Questionário" (público/cliente)
1. Antes de exibir, chamar o endpoint de verificação. **Se a URL da página tiver `?agendamento=...`** (ex: link de anamnese pré-procedimento enviado por WhatsApp), passe `agendamentoId` para que a checagem seja feita por agendamento — não por cliente. Sem isso, o cliente que já respondeu uma anamnese antes vê "avaliação já registrada" indevidamente. **No fluxo público (sem JWT)**, use o endpoint da seção 7.3 (`/api/v1/public/{slug}/...`).
2. Renderizar perguntas ordenadas por `ordem`.
3. Componente de pergunta polimórfico: mapeie `tipo → componente`:
   - `TEXTO_CURTO` → `<input>`
   - `TEXTO_LONGO` → `<textarea>`
   - `NUMERO` → `<input type="number">`
   - `ESCALA` → slider com labels nos extremos
   - `AVALIACAO_ESTRELAS` → 5 estrelas clicáveis
   - `SELECAO_UNICA` → `<radio>`
   - `SELECAO_MULTIPLA` → `<checkbox>`
   - `SIM_NAO` → 2 botões grandes
   - `DATA` → `<input type="date">`
   - `HORA` → `<input type="time">`
4. Cronômetro do lado para enviar `tempoPreenchimentoSegundos` no submit (UX nice-to-have).
5. Detectar `userAgent` (`navigator.userAgent`) e `dispositivo` (mobile/desktop) e enviar.
6. Submit → `POST /api/v1/questionarios/{id}/respostas`.
7. Tratar erros 400 mostrando a mensagem ao lado da pergunta correspondente quando possível.

### 10.4 Tela "Dashboard / Estatísticas"
- Header: total / hoje / 7d / 30d (`GET /estatisticas`).
- Tempo médio de preenchimento.
- Por pergunta:
  - Numéricas: gráfico de barras com `distribuicaoNotas` + cards (média, min, max, desvio padrão).
  - Seleção: gráfico de pizza/barras com `estatisticasOpcoes[].percentual`.
  - SIM_NAO: donut com `percentualSim`.
  - Texto: lista de `palavrasFrequentes` + `mediaCaracteres`.
- Se houver pergunta ESCALA 0–10 → botão **"Calcular NPS"** que chama `/nps?perguntaId=...` e exibe medidor.
- Botão **"Gerar relatório"** com seletor de datas → `/relatorio?inicio=...&fim=...` → exporta CSV/PDF no front.

---

## 11. Dispositivos sugeridos para `dispositivo`

Não há enum no backend (string livre, max 100). Convenção sugerida no front:
- `MOBILE_IOS`, `MOBILE_ANDROID`, `TABLET`, `DESKTOP`, `BOT`, `EMBEDDED`.

---

## 12. Erros e códigos HTTP

| HTTP | Quando |
|---|---|
| `200` | OK em GETs e PUTs/DELETEs bem-sucedidos |
| `201` | Criação bem-sucedida (POST) |
| `400` | Validação (Bean Validation ou regras de negócio) |
| `403` | Token sem `organizacao_id` ou recurso de outra org |
| `404` | Questionário/resposta não encontrada (ou site inativo nos endpoints públicos) |
| `429` | Rate limit excedido (apenas endpoints públicos) |
| `500` | Erro inesperado (logado server-side) |

Ler `dados.message` do envelope para exibir ao usuário em toasts.

---

## 13. Exemplo end-to-end (TypeScript fetch)

```ts
const API = '/api/v1';

async function call<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...(init?.headers ?? {}),
    },
  });
  const env = await res.json();
  if (!env.success) throw new Error(env.message ?? 'Erro');
  return env.dados as T;
}

// Listar
const lista = await call<QuestionarioDTO[]>('/questionarios/ativos');

// Criar
const novo = await call<QuestionarioDTO>('/questionarios', {
  method: 'POST',
  body: JSON.stringify(payload),
});

// Responder
await call<RespostaQuestionarioDTO>(
  `/questionarios/${novo.id}/respostas`,
  { method: 'POST', body: JSON.stringify(respostaPayload) }
);

// Estatísticas
const stats = await call<EstatisticasQuestionarioDTO>(
  `/questionarios/${novo.id}/respostas/estatisticas`
);

// Histórico de respostas do cliente (lista, ordenado mais recente primeiro)
const historico = await call<RespostaQuestionarioDTO[]>(
  `/questionarios/${novo.id}/respostas/cliente/${clienteId}/historico`
);
const ultimaResposta = historico[0]; // null-safe se a lista vier vazia

// === Fluxo público (sem JWT) ===
async function callPublic<T>(slug: string, path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API}/public/${slug}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
  });
  const env = await res.json();
  if (!env.success) throw new Error(env.message ?? 'Erro');
  return env.dados as T;
}

const qPublico = await callPublic<QuestionarioDTO>('minha-org', `/questionarios/${id}`);
const respPublica = await callPublic<RespostaQuestionarioDTO>(
  'minha-org',
  `/questionarios/${id}/respostas`,
  { method: 'POST', body: JSON.stringify(payload) }
);
```

---

## 14. Checklist de paridade front-end ↔ back-end

- [ ] Enum `TipoQuestionario` espelhado no front com labels (`getDescricao()` do backend).
- [ ] Enum `TipoPergunta` espelhado com componente de renderização por tipo.
- [ ] Validações replicadas no front (min/max, obrigatoriedade, opções).
- [ ] Tratamento de paginação para `/pesquisar` e `/respostas`.
- [ ] Pré-checagem por `agendamentoId` antes de submeter resposta vinculada a agendamento (a única validação de duplicidade restante).
- [ ] Limpeza de campos irrelevantes ao trocar tipo de pergunta no construtor.
- [ ] Exibição de `respostaFormatada` em listagens.
- [ ] Cálculo de NPS apenas para perguntas ESCALA.
- [ ] Suporte a soft delete (não exibir `is_deletado=true` nas listas — backend já filtra).
- [ ] Endpoints públicos (`/api/v1/public/{slug}/...`) usam o slug correto da organização e **não** enviam `Authorization`.
- [ ] Tratamento de `429 TOO MANY REQUESTS` nos endpoints públicos (rate limit).
- [ ] Endpoint `respostas/cliente/{clienteId}/historico` retorna **lista** ordenada DESC — usar `[0]` para "última resposta". Fallback `Cliente #${id}` se `clienteNome` vier null em algum item.

---

## 15. Pontos de atenção

1. **Edição substitui perguntas**: o `PUT /{id}` deleta todas as perguntas/opções existentes e recria. Implicações:
   - As respostas antigas continuam vinculadas aos IDs das perguntas antigas (via `pergunta_id` em `RespostaPergunta`). Após edição, IDs novos quebram a referência **conceitual** (mesma pergunta, ID novo) — relatórios mostrarão "pergunta deletada" para respostas antigas.
   - **Recomendação**: bloquear ou avisar a edição se o questionário já tiver respostas. Ou implementar versionamento.
2. **Múltiplas respostas por cliente**: o backend **permite** que o mesmo cliente responda o mesmo questionário várias vezes. A única deduplicação é por `agendamentoId` (1 agendamento = 1 resposta). Para mostrar "última resposta" use o endpoint de histórico (§ 6.6) e pegue o primeiro item.
3. **Cor tema**: validar formato hex no front (`/^#[0-9A-F]{6}$/i`).
4. **Listagem `/questionarios`** retorna **todos** (ativos+inativos não-deletados). Filtre no front se quiser somente ativos sem chamar `/ativos`.
5. **Multitenancy**: nunca enviar `organizacaoId` no body — vem do JWT. Se o JSON de criação tiver, é ignorado.
6. **`MultipleBagFetchException` resolvida**: as queries que carregam `Questionario` com `Pergunta` + `OpcaoResposta` foram divididas em duas — transparente para o front.
7. **Endpoints públicos exigem site ativo**: o `SitePublicoConfig.active=true` é pré-requisito. Se a organização tem o site desativado, todos os endpoints `/public/{slug}/questionarios/...` retornam `404`. O front deve tratar como "questionário indisponível", não como erro técnico.
8. **Slug em endpoints públicos**: é o mesmo slug da organização usado em `/public/site/{slug}/booking/...`. Vem da rota da landing page do front (geralmente `params.slug`).

---

Última atualização: 2026-04-25.
