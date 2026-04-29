# Migração Front-end → PR7 (Resolução server-side de variáveis)

> **Cenário:** o front-end já foi implementado seguindo o doc original
> `QUESTIONARIO_TERMO_FRONTEND_IMPLEMENTACAO.md`. O backend ganhou uma nova capacidade
> (PR7) que **simplifica** o front: agora o servidor resolve os placeholders
> `{{nomeCliente}}`, `{{cpfCliente}}`, etc. direto no GET do questionário.
>
> Este documento é um **delta** focado em adaptar o front existente. Não
> reimplementa do zero.
>
> **Status:** PR7 já implementado no backend. Front precisa migrar.
>
> **Documentos relacionados:**
> - `docs/QUESTIONARIO_TERMO_FRONTEND_IMPLEMENTACAO.md` — doc principal (já atualizado pra refletir PR7).
> - `docs/QUESTIONARIO_TERMO_CONSENTIMENTO_BACKEND.md` — spec backend.

---

## Sumário

1. [TL;DR — o que mudou](#1-tldr--o-que-mudou)
2. [Mudanças obrigatórias](#2-mudanças-obrigatórias)
3. [Limpezas recomendadas](#3-limpezas-recomendadas)
4. [Novos comportamentos](#4-novos-comportamentos)
5. [URL com query params — boas práticas](#5-url-com-query-params--boas-práticas)
6. [Edge cases que mudaram](#6-edge-cases-que-mudaram)
7. [Checklist de migração](#7-checklist-de-migração)
8. [Resumo do impacto em arquivos](#8-resumo-do-impacto-em-arquivos)
9. [Tempo estimado](#9-tempo-estimado)

---

## 1. TL;DR — o que mudou

| Antes (front renderizava) | Agora (backend renderiza) |
|---|---|
| Front chamava `GET /api/v1/public/{slug}/questionarios/{id}` sem params | Front passa `?clienteId=&agendamentoId=&funcionarioId=` no GET |
| Front montava `TemplateContexto` com 11 variáveis | Sem contexto — backend resolve via JPA |
| Front chamava `renderTemplate(textoTermo, contexto)` antes de exibir | Front lê `pergunta.textoTermoRenderizado` direto |
| Front chamava `validarContexto()` para checar obrigatórias | Backend valida ownership, não variáveis |
| Bloco TERMO importava `templateRenderer.ts` | Sem dependência |

**A semântica do POST não muda** — front continua devolvendo `textoTermoRenderizado`
(agora valor recebido do GET, não calculado localmente). Backend continua calculando
`sha256(textoTermoRenderizado)` e congelando como prova legal.

---

## 2. Mudanças obrigatórias

> Sem essas mudanças, o front continua funcionando (placeholders crus aparecem),
> mas você desperdiça o backend novo. Faça pelo menos estas três.

### 2.1. Adicionar campo `textoTermoRenderizado` ao tipo TS

```ts
// src/types/questionario.ts (ou onde estiverem os tipos)

export interface PerguntaDTO {
  // ... todos os campos existentes ...
  textoTermo?: string;
  textoTermoRenderizado?: string;  // ← NOVO: pode vir populado pelo backend
  // ...
}
```

Sem isso, TypeScript reclamaria do acesso ao novo campo.

### 2.2. Passar IDs no GET do questionário

No carregamento do `QuestionarioResposta.tsx`:

```ts
// ANTES
const { data } = await api.get(`/api/v1/public/${slug}/questionarios/${id}`);

// DEPOIS
const params = new URLSearchParams(window.location.search);
const { data } = await api.get(`/api/v1/public/${slug}/questionarios/${id}`, {
  params: {
    clienteId: params.get('cliente') ?? undefined,
    agendamentoId: params.get('agendamento') ?? undefined,
    funcionarioId: params.get('funcionario') ?? undefined,
  },
});
```

> ⚠️ **Atenção aos nomes**:
> - URL do front-end (que já existe): `?cliente=18&agendamento=126&funcionario=9`.
> - Query params do backend: `?clienteId=18&agendamentoId=126&funcionarioId=9` (camelCase).
>
> O mapping é feito no momento da chamada via `URLSearchParams` → params do request.

### 2.3. `BlocoTermo` consome `textoTermoRenderizado` direto

```tsx
// ANTES
function BlocoTermo({ pergunta, contexto, onChange }) {
  const textoRenderizado = useMemo(
    () => renderTemplate(pergunta.textoTermo ?? '', contexto),
    [pergunta.textoTermo, contexto]
  );
  const [aceitou, setAceitou] = useState(false);

  useEffect(() => {
    onChange({
      aceitouTermo: aceitou,
      textoTermoRenderizado: aceitou ? textoRenderizado : undefined,
    });
  }, [aceitou, textoRenderizado]);

  return (
    <div>
      <ReactMarkdown>{textoRenderizado}</ReactMarkdown>
      {/* ... */}
    </div>
  );
}

// DEPOIS
function BlocoTermo({ pergunta, onChange }) {
  // Texto pronto vindo do servidor; fallback para textoTermo cru se GET
  // foi feito sem os IDs (ex.: preview admin).
  const textoExibicao = pergunta.textoTermoRenderizado ?? pergunta.textoTermo ?? '';
  const [aceitou, setAceitou] = useState(false);

  useEffect(() => {
    onChange({
      aceitouTermo: aceitou,
      textoTermoRenderizado: aceitou ? textoExibicao : undefined,
    });
  }, [aceitou, textoExibicao]);

  return (
    <div>
      <ReactMarkdown>{textoExibicao}</ReactMarkdown>
      {/* ... */}
    </div>
  );
}
```

A prop `contexto` some da assinatura. O `useMemo` some.

### 2.4. Remover `contexto` do switch de renderização

```tsx
// ANTES
function renderBlocoPergunta(p, contexto, onChange) {
  switch (p.tipo) {
    // ...
    case TipoPergunta.TERMO_CONSENTIMENTO:
      return <BlocoTermo pergunta={p} contexto={contexto} onChange={onChange} />;
    // ...
  }
}

// DEPOIS
function renderBlocoPergunta(p, onChange) {
  switch (p.tipo) {
    // ...
    case TipoPergunta.TERMO_CONSENTIMENTO:
      return <BlocoTermo pergunta={p} onChange={onChange} />;
    // ...
  }
}
```

E no POST do submit, o `textoTermoRenderizado` continua sendo enviado — mas o valor
agora vem do `pergunta.textoTermoRenderizado` recebido do GET, **não** de uma chamada
local a `renderTemplate()`.

---

## 3. Limpezas recomendadas

> Não obrigatórias para o sistema funcionar, mas removem código morto e simplificam manutenção.

### 3.1. Deletar `src/utils/templateRenderer.ts`

Funções `renderTemplate()` e `validarContexto()` não são mais chamadas em fluxo de produção.

```bash
# Faça grep antes de deletar para garantir zero usos:
grep -rn "renderTemplate\|validarContexto\|templateRenderer" src/
```

Se algum lugar legítimo usar (ex.: preview offline em admin), mantenha o arquivo.
Caso contrário, delete.

### 3.2. Remover interface `TemplateContexto`

```ts
// REMOVER de src/types/questionario.ts (ou onde estiver)
export interface TemplateContexto {
  nomeCliente?: string;
  cpfCliente?: string;
  // ... 11 campos ...
}
```

### 3.3. Remover montagem do contexto em `QuestionarioResposta.tsx`

```ts
// REMOVER
const contexto: TemplateContexto = {
  nomeCliente: cliente.nomeCompleto,
  cpfCliente: cliente.cpf,
  nomeEstabelecimento: organizacao.nomeFantasia,
  nomeProcedimento: agendamento.servicos.map(s => s.nome).join(', '),
  dataAtendimento: format(agendamento.data, 'dd/MM/yyyy'),
  // ... mais 6 ...
};
```

**Bônus:** se você fazia chamadas extras ao backend só para montar esse contexto
(ex.: `GET /clientes/{id}` ou `GET /agendamentos/{id}` apenas para pegar o nome),
você pode removê-las também — o backend já tem acesso a esses dados via JPA no
endpoint do questionário.

### 3.4. Remover `validarContexto()` do submit

```ts
// REMOVER
const faltantes = validarContexto(pergunta.textoTermo!, contexto);
if (faltantes.length > 0) {
  alert(`Não é possível submeter: faltam dados para ${faltantes.join(', ')}.`);
  return;
}
```

A validação de obrigatórias agora acontece no backend (via ownership). Se faltar
dado de cliente/agendamento, o GET retorna 403 — **não** chega na hora do submit.

---

## 4. Novos comportamentos

### 4.1. Tratar 403 do GET

Se o cliente acessar a URL com `cliente=99` (que não pertence à org do slug), ou
um agendamento que não é dele, o backend retorna **403 Forbidden**:

```ts
try {
  const { data } = await api.get(...);
} catch (err) {
  const status = err.response?.status;
  if (status === 403) {
    // Tenant inválido OU agendamento de outro cliente
    setErro('Link inválido. Verifique se o link foi enviado corretamente ou contate o estabelecimento.');
    return;
  }
  if (status === 404) {
    setErro('Questionário não encontrado ou inativo.');
    return;
  }
  if (status === 429) {
    setErro('Muitas tentativas. Tente novamente em alguns minutos.');
    return;
  }
  throw err;
}
```

> Antes do PR7, esse cenário passava silenciosamente — front montava o contexto com
> qualquer dado disponível. Agora há proteção real contra scraping cross-tenant.

### 4.2. Detectar variáveis não-resolvidas (defensivo)

Se algum `{{placeholder}}` sobrar no `textoTermoRenderizado`, é porque:
- O nome da variável está errado no template (typo).
- Backend não tinha o dado (ex.: agendamento sem serviço).

Para alertar em dev/preview admin:

```ts
const PLACEHOLDER_REGEX = /\{\{(\w+)\}\}/g;

function detectarVariaveisNaoResolvidas(texto: string): string[] {
  const matches = [...texto.matchAll(PLACEHOLDER_REGEX)];
  return [...new Set(matches.map(m => m[1]))];
}

// Uso (opcional)
const naoResolvidas = detectarVariaveisNaoResolvidas(textoExibicao);
if (naoResolvidas.length > 0 && process.env.NODE_ENV !== 'production') {
  console.warn('Variáveis não resolvidas no termo:', naoResolvidas);
}
```

### 4.3. Preview no QuestionarioCadastro (admin)

Se você tem botão "Pré-visualizar" no editor de questionário:

- **Sem IDs**: mostra `{{placeholders}}` literais (modo template-only).
- **Com IDs de teste**: passa um cliente/agendamento de teste pra ver como vai ficar.

Sugestão: input opcional "Pré-visualizar com agendamento ID:" no editor que adiciona
os params no GET. Útil para o admin verificar se as variáveis fazem sentido antes de
publicar.

```tsx
const [previewAgendamentoId, setPreviewAgendamentoId] = useState<string>('');

const handlePreview = async () => {
  const params = previewAgendamentoId
    ? { agendamentoId: Number(previewAgendamentoId) }
    : {};
  const { data } = await api.get(`/api/v1/questionarios/${questionarioId}`, { params });
  abrirModalPreview(data.dados);
};
```

---

## 5. URL com query params — boas práticas

Como sua URL é `/avaliacao/{slug}/{id}/responder?cliente=18&agendamento=126&funcionario=9`:

### 5.1. Hook para extrair IDs

```ts
// src/hooks/useResponderUrl.ts
export function useResponderUrl() {
  const params = new URLSearchParams(window.location.search);
  return {
    clienteId: parseIntOrNull(params.get('cliente')),
    agendamentoId: parseIntOrNull(params.get('agendamento')),
    funcionarioId: parseIntOrNull(params.get('funcionario')),
  };
}

function parseIntOrNull(v: string | null): number | null {
  if (!v) return null;
  const n = parseInt(v, 10);
  return isNaN(n) ? null : n;
}
```

### 5.2. Reutilizar os mesmos IDs no POST

Os `clienteId` e `agendamentoId` que vão no GET também devem ir no payload do POST:

```ts
const { clienteId, agendamentoId, funcionarioId } = useResponderUrl();

// GET
const { data: { dados: questionario } } = await api.get(
  `/api/v1/public/${slug}/questionarios/${id}`,
  { params: { clienteId, agendamentoId, funcionarioId } },
);

// POST
await api.post(`/api/v1/public/${slug}/questionarios/${id}/respostas`, {
  questionarioId: id,
  clienteId,        // ← mesmo ID
  agendamentoId,    // ← mesmo ID
  // colaboradorId opcional — distinto de funcionarioId só no contexto de quem ATENDE
  userAgent: navigator.userAgent,
  dispositivo: detectarDispositivo(),
  respostas: [/* ... */],
});
```

---

## 6. Edge cases que mudaram

### 6.1. Acesso direto sem params

Se alguém acessar `/avaliacao/{slug}/{id}/responder` (sem `?cliente=...&agendamento=...`),
o backend devolve `textoTermoRenderizado = null`.

**Comportamento sugerido no front:**
- **Modo cliente respondendo**: redirecionar/avisar que o link está incompleto.
- **Modo preview admin**: mostrar `textoTermo` com placeholders (já cobre via fallback `?? pergunta.textoTermo` no `BlocoTermo`).

```ts
// No QuestionarioResposta.tsx
const { clienteId, agendamentoId } = useResponderUrl();
if (!clienteId || !agendamentoId) {
  return <ErroLinkIncompleto />;
}
```

### 6.2. Cliente diferente do dono do agendamento

Se a URL tem `cliente=18&agendamento=126` mas o agendamento 126 é do cliente 25:
- Backend retorna **403 Forbidden** explícito.
- Front mostra erro "Link inválido" (ver §4.1).

Antes do PR7, isso passava silenciosamente (front montava o contexto com qualquer
dado disponível). Agora há proteção real.

### 6.3. Variável `{{rgCliente}}` em template

O modelo de Cliente atual não tem campo RG. Backend resolve para vazio.

Se algum template Bellory usa `{{rgCliente}}`, ele aparece como `RG: ` (vazio).
Considerar revisar os templates `PADRAO_PROCEDIMENTO` se isso for um problema.

### 6.4. Cliente que muda durante o preenchimento

Se o nome do cliente mudar no banco entre o GET e o POST (improvável, mas possível
em fluxo público longo), o que ele VIU no GET é o que vale juridicamente. Backend
calcula hash sobre o `textoTermoRenderizado` que o front devolve no POST, que é
exatamente o que o cliente leu.

---

## 7. Checklist de migração

### Obrigatório (senão fica desperdiçando o backend)

- [ ] `PerguntaDTO` ganha campo `textoTermoRenderizado?: string`.
- [ ] `QuestionarioResposta.tsx` adiciona `clienteId`/`agendamentoId`/`funcionarioId` ao GET.
- [ ] `BlocoTermo` lê `pergunta.textoTermoRenderizado ?? pergunta.textoTermo`.
- [ ] Remove prop `contexto` do `BlocoTermo` e do switch `renderBlocoPergunta`.
- [ ] No POST, `textoTermoRenderizado` continua sendo enviado (mesma key, valor agora vem do GET).

### Limpeza (recomendado — código morto)

- [ ] Deletar `src/utils/templateRenderer.ts` (após `grep` confirmar zero usos legítimos).
- [ ] Remover interface `TemplateContexto` dos tipos.
- [ ] Remover montagem manual de `contexto` no `QuestionarioResposta.tsx`.
- [ ] Remover chamada `validarContexto()` no submit.
- [ ] Remover lookups extras de cliente/agendamento que só serviam pra montar o contexto.

### Robustez (defensivo)

- [ ] Tratar 403 no GET com mensagem clara ("link inválido").
- [ ] Tratar 429 com mensagem de rate-limit.
- [ ] Tratar 404 com mensagem "questionário inativo".
- [ ] Verificar `clienteId`/`agendamentoId` presentes na URL antes de chamar o GET.
- [ ] (Opcional dev) `console.warn` quando sobrar `{{var}}` no texto renderizado.
- [ ] Ajustar preview admin do editor de questionário (modo com/sem IDs).

### Doc

- [ ] Atualizar comentários internos do código que mencionam `renderTemplate`.
- [ ] Se houver README front, anotar que o backend é autoridade na resolução.

---

## 8. Resumo do impacto em arquivos

| Arquivo | Status | Ação |
|---|---|---|
| `src/types/questionario.ts` | ✏️ Editar | Adicionar `textoTermoRenderizado` em `PerguntaDTO`; remover `TemplateContexto` |
| `src/utils/templateRenderer.ts` | ❌ Deletar | Após `grep` confirmar zero usos |
| `src/pages/QuestionarioResposta.tsx` | ✏️ Editar | GET com params, remover montagem de contexto, simplificar submit, tratar 403/404/429 |
| `src/components/BlocoTermo.tsx` | ✏️ Editar | Sem prop `contexto`, lê `textoTermoRenderizado` |
| `src/hooks/useResponderUrl.ts` | ➕ Criar | (sugestão) extrair IDs da query string |
| `src/pages/QuestionarioCadastro.tsx` | (opcional) | Adicionar preview com IDs de teste |
| `src/api.ts` ou cliente HTTP | (sem mudança) | URLs e auth iguais |

---

## 9. Tempo estimado

| Escopo | Tempo |
|---|---|
| **Mínimo (só obrigatório)** | 30–60 min — passa a usar backend novo, mantém código legado in-place |
| **Limpeza completa** | +1–2h — remove `templateRenderer`, refatora `QuestionarioResposta` |
| **Robustez (tratamento de erros + preview admin)** | +30 min |
| **Total realista** | **2–3h** se você tem o front estável da v1 |

---

## 10. Após a migração — exemplo end-to-end

URL: `/avaliacao/bellory-salon-awhxm/3/responder?cliente=18&agendamento=126&funcionario=9`

```ts
// 1. Hook extrai IDs
const { clienteId, agendamentoId, funcionarioId } = useResponderUrl();

if (!clienteId || !agendamentoId) {
  return <ErroLinkIncompleto />;
}

// 2. Carrega questionário com placeholders já resolvidos
const { data: { dados: questionario } } = await api.get(
  `/api/v1/public/${slug}/questionarios/${id}`,
  { params: { clienteId, agendamentoId, funcionarioId } },
);

// 3. Renderiza — BlocoTermo lê textoTermoRenderizado direto
<form>
  {questionario.perguntas.map(p =>
    renderBlocoPergunta(p, (resposta) => atualizarResposta(p.id, resposta)),
  )}
</form>

// 4. Submete — devolve textoTermoRenderizado recebido + assinatura
const perguntaTermo = questionario.perguntas.find(
  p => p.tipo === TipoPergunta.TERMO_CONSENTIMENTO,
);

await api.post(`/api/v1/public/${slug}/questionarios/${id}/respostas`, {
  questionarioId: id,
  clienteId,
  agendamentoId,
  userAgent: navigator.userAgent,
  dispositivo: detectarDispositivo(),
  respostas: [
    { perguntaId: 100, respostaTexto: 'Não' },                                  // SIM_NAO
    {
      perguntaId: perguntaTermo.id,                                             // TERMO
      aceitouTermo: true,
      textoTermoRenderizado: perguntaTermo.textoTermoRenderizado,               // do GET
    },
    {
      perguntaId: 102,                                                          // ASSINATURA
      assinaturaClienteBase64: canvasCliente.toDataURL('image/png'),
      assinaturaProfissionalBase64: canvasProf.toDataURL('image/png'),
    },
  ],
});
```

Pronto — front simplificado, backend autoritativo, prova legal preservada.
