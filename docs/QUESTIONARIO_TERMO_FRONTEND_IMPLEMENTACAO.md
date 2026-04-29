# Termo de Consentimento + Assinatura Digital — Guia de Implementação Front-end

> **Objetivo:** alinhar o front-end com o back-end já entregue (PRs 1–6, migrations V66–V69).
> Este documento é **executável**: contém endpoints exatos, tipos TS/JS prontos, snippets React e checklist de tarefas.
>
> **Documentos relacionados (back-end):**
> - `docs/QUESTIONARIO_TERMO_CONSENTIMENTO_BACKEND.md` — especificação e plano completo (já implementado).
> - `docs/API_QUESTIONARIO_SPRINGBOOT.md` — estrutura existente do questionário.
> - `docs/API_RESPOSTA_QUESTIONARIO_SPRINGBOOT.md` — endpoints de resposta existentes.
> - `docs/QUESTIONARIOS_FRONTEND_GUIDE.md` — guia anterior do front.

---

## Sumário

1. [Visão geral da feature](#1-visão-geral-da-feature)
2. [Endpoints da API](#2-endpoints-da-api)
3. [Tipos TypeScript](#3-tipos-typescript)
4. [Helper de substituição de variáveis](#4-helper-de-substituição-de-variáveis)
5. [Helper de captura de assinatura (canvas)](#5-helper-de-captura-de-assinatura-canvas)
6. [QuestionarioCadastro.tsx — editor de perguntas](#6-questionariocadastrotsx--editor-de-perguntas)
7. [QuestionarioResposta.tsx — preenchimento](#7-questionariorespostatsx--preenchimento)
8. [Tela de auditoria (admin)](#8-tela-de-auditoria-admin)
9. [Comprovante PDF — botão de download](#9-comprovante-pdf--botão-de-download)
10. [AgendamentoQuestionario — tracking duplo](#10-agendamentoquestionario--tracking-duplo)
11. [Validações que o front deve replicar](#11-validações-que-o-front-deve-replicar)
12. [Casos de uso end-to-end](#12-casos-de-uso-end-to-end)
13. [Limitações da fase 1](#13-limitações-da-fase-1)
14. [Checklist de entrega](#14-checklist-de-entrega)

---

## 1. Visão geral da feature

Permite questionários conterem **perguntas tipo `TERMO_CONSENTIMENTO`** (texto Markdown com placeholders) e **`ASSINATURA`** (canvas digital → PNG base64). O cliente lê o termo, marca aceite explícito, assina, e o sistema preserva prova legal: snapshot do texto, hash SHA-256, IP, User-Agent, timestamp.

### Fluxo macro

```
┌─────────────┐    ┌─────────────────────┐    ┌─────────────────┐
│ Admin cria  │───▶│ Cliente preenche +  │───▶│ Backend grava + │
│ questionário│    │ aceita + assina     │    │ Audita + PDF    │
└─────────────┘    └─────────────────────┘    └─────────────────┘
```

### Casos de uso

1. **Anamnese pré-procedimento** (caso âncora — `Servico.anamnese_id` já existe).
2. **Termo pré-procedimento químico** (declaração de ciência de riscos).
3. **Autorização de uso de imagem** (opt-in fotos antes/depois).
4. **Comprovante pós-atendimento** (recebimento de orientações).

---

## 2. Endpoints da API

### 2.1. Templates de termo (catálogo estático)

```
GET /api/v1/questionarios/templates-termo
```
Lista os templates pré-definidos da plataforma (autenticado, qualquer role).

```
GET /api/v1/questionarios/templates-termo/{id}
```
Busca um template específico (id = enum `TipoTemplateTermo`).

### 2.2. Cadastro de questionário (endpoints existentes; payload estendido)

```
POST /api/v1/questionarios
PUT  /api/v1/questionarios/{id}
```
O `QuestionarioCreateDTO` já existente, com `PerguntaCreateDTO` agora aceitando campos novos para `TERMO_CONSENTIMENTO` e `ASSINATURA` (ver §3).

### 2.3. Submissão de resposta

```
POST /api/v1/questionarios/{questionarioId}/respostas              # autenticado
POST /api/v1/public/{slug}/questionarios/{id}/respostas            # público (anamnese, link direto)
```
Payload `RespostaQuestionarioCreateDTO` com campos novos em `RespostaPerguntaCreateDTO`.

### 2.4. Auditoria (admin)

```
GET /api/v1/resposta-questionario/{id}/auditoria
```
Retorna `AuditoriaTermoDTO` com termos aceitos, hash recalculado vs armazenado, IP/UA, status de soft-delete. **Requer ROLE_ADMIN ou ROLE_SUPERADMIN.**

```
GET /api/v1/resposta-questionario/{id}/assinatura/{cliente|profissional}?perguntaId={pId}
```
Download direto da imagem PNG/SVG da assinatura (Content-Type detectado por magic number). **Permite ADMIN ou cliente dono.**

### 2.5. Comprovante PDF

```
GET /api/v1/resposta-questionario/{id}/comprovante.pdf
```
Download do PDF (Content-Type: `application/pdf`, `Content-Disposition: inline`). **Permite ADMIN ou cliente dono.** Disponível mesmo quando a resposta está soft-deleted (LGPD Art. 18).

### 2.6. Delete (comportamento alterado)

```
DELETE /api/v1/questionarios/{questionarioId}/respostas/{respostaId}
```
Quando a resposta tem termo aceito ou assinatura, o backend faz **soft-delete** automaticamente. Front não muda — só recebe `success: true` como antes. Listagens já filtram soft-deleted.

---

## 3. Tipos TypeScript

Arquivo sugerido: `src/types/questionario.ts` (ou onde já estiverem os tipos existentes — apenas estender).

### 3.1. Enums novos

```ts
export enum TipoPergunta {
  TEXTO_CURTO = 'TEXTO_CURTO',
  TEXTO_LONGO = 'TEXTO_LONGO',
  NUMERO = 'NUMERO',
  SELECAO_UNICA = 'SELECAO_UNICA',
  SELECAO_MULTIPLA = 'SELECAO_MULTIPLA',
  ESCALA = 'ESCALA',
  DATA = 'DATA',
  HORA = 'HORA',
  AVALIACAO_ESTRELAS = 'AVALIACAO_ESTRELAS',
  SIM_NAO = 'SIM_NAO',
  // NOVOS
  TERMO_CONSENTIMENTO = 'TERMO_CONSENTIMENTO',
  ASSINATURA = 'ASSINATURA',
}

export enum FormatoAssinatura {
  PNG_BASE64 = 'PNG_BASE64',
  SVG = 'SVG',
}

export enum TipoTemplateTermo {
  PADRAO_BELLORY = 'PADRAO_BELLORY',
  PADRAO_PROCEDIMENTO = 'PADRAO_PROCEDIMENTO',
  PADRAO_PROCEDIMENTO_QUIMICO = 'PADRAO_PROCEDIMENTO_QUIMICO',
  PADRAO_USO_IMAGEM = 'PADRAO_USO_IMAGEM',
  CUSTOM = 'CUSTOM',
}

export enum StatusAssinatura {
  NAO_REQUERIDA = 'NAO_REQUERIDA',
  PENDENTE = 'PENDENTE',
  ASSINADA = 'ASSINADA',
}

export enum StatusQuestionarioAgendamento {
  PENDENTE = 'PENDENTE',
  ENVIADO = 'ENVIADO',
  RESPONDIDO = 'RESPONDIDO',
  FALHOU = 'FALHOU',
}
```

### 3.2. PerguntaCreateDTO — campos novos

```ts
export interface PerguntaCreateDTO {
  texto: string;
  descricao?: string;
  tipo: TipoPergunta;
  obrigatoria?: boolean;
  ordem: number;
  opcoes?: OpcaoRespostaCreateDTO[];

  // ESCALA
  escalaMin?: number;
  escalaMax?: number;
  labelMin?: string;
  labelMax?: string;

  // Validação
  minCaracteres?: number;
  maxCaracteres?: number;
  minValor?: number;
  maxValor?: number;

  // ===== NOVO: TERMO_CONSENTIMENTO =====
  textoTermo?: string;                  // min 10, max 10000 — Markdown com {{placeholders}}
  templateTermoId?: TipoTemplateTermo;  // origem do template (auditoria)
  requerAceiteExplicito?: boolean;      // default false

  // ===== NOVO: ASSINATURA =====
  formatoAssinatura?: FormatoAssinatura;     // default PNG_BASE64
  larguraAssinatura?: number;                // default 600, range [200, 1200]
  alturaAssinatura?: number;                 // default 200, range [100, 600]
  exigirAssinaturaProfissional?: boolean;    // default false
}
```

### 3.3. PerguntaDTO (leitura) — campos novos

```ts
export interface PerguntaDTO {
  id: number;
  texto: string;
  descricao?: string;
  tipo: TipoPergunta;
  obrigatoria: boolean;
  ordem: number;
  opcoes?: OpcaoRespostaDTO[];
  escalaMin?: number;
  escalaMax?: number;
  labelMin?: string;
  labelMax?: string;
  minCaracteres?: number;
  maxCaracteres?: number;
  minValor?: number;
  maxValor?: number;
  // NOVO
  textoTermo?: string;
  templateTermoId?: TipoTemplateTermo;
  requerAceiteExplicito?: boolean;
  formatoAssinatura?: FormatoAssinatura;
  larguraAssinatura?: number;
  alturaAssinatura?: number;
  exigirAssinaturaProfissional?: boolean;
}
```

### 3.4. RespostaPerguntaCreateDTO — payload de submit

```ts
export interface RespostaPerguntaCreateDTO {
  perguntaId: number;
  respostaTexto?: string;
  respostaNumero?: number;
  respostaOpcaoIds?: number[];
  respostaData?: string;   // ISO yyyy-MM-dd
  respostaHora?: string;   // ISO HH:mm:ss

  // ===== NOVO: TERMO_CONSENTIMENTO =====
  aceitouTermo?: boolean;
  /**
   * IMPORTANTE: front substitui {{placeholders}} ANTES de submeter.
   * Servidor calcula sha256(textoTermoRenderizado) e congela como prova legal.
   */
  textoTermoRenderizado?: string;

  // ===== NOVO: ASSINATURA =====
  /** Data URL completo: "data:image/png;base64,iVBORw0KGgo..." */
  assinaturaClienteBase64?: string;
  /** Idem, quando pergunta exige assinatura do profissional. */
  assinaturaProfissionalBase64?: string;
}
```

> ⚠️ **NÃO ENVIE** `dataAceite`, `hashTermo`, `ipAceite` ou `userAgent` por pergunta — o servidor ignora e usa os próprios valores.

### 3.5. RespostaPerguntaDTO (leitura)

```ts
export interface RespostaPerguntaDTO {
  id: number;
  perguntaId: number;
  perguntaTexto: string;
  tipoPergunta: TipoPergunta;
  respostaTexto?: string;
  respostaNumero?: number;
  opcoesSelecionadas?: OpcaoSelecionadaDTO[];
  respostaData?: string;
  respostaHora?: string;
  respostaFormatada?: string;   // string pronta para UI (já cobre TERMO/ASSINATURA)

  // NOVO
  aceitouTermo?: boolean;
  dataAceite?: string;                          // ISO LocalDateTime
  textoTermoRenderizado?: string;
  hashTermo?: string;                           // SHA-256 hex
  arquivoAssinaturaClienteId?: number;
  arquivoAssinaturaProfissionalId?: number;
}
```

### 3.6. TemplateTermoDTO

```ts
export interface TemplateTermoDTO {
  id: TipoTemplateTermo;
  nome: string;
  descricao: string;
  conteudo: string;        // Markdown com {{variaveis}}
  variaveis: string[];     // lista das variáveis suportadas
  editavel: boolean;       // false para templates Bellory
}
```

### 3.7. AgendamentoQuestionarioDTO — tracking duplo

```ts
export interface AgendamentoQuestionarioDTO {
  id: number;
  agendamentoId: number;
  questionarioId: number;
  /** Status da resposta (existente). */
  status: StatusQuestionarioAgendamento;
  /** NOVO: status independente da assinatura. */
  statusAssinatura: StatusAssinatura;
  dtEnvio?: string;
  dtResposta?: string;
  dtAssinatura?: string;     // NOVO
  respostaQuestionarioId?: number;
  dtCriacao: string;
}
```

### 3.8. AuditoriaTermoDTO

```ts
export interface AuditoriaTermoDTO {
  respostaQuestionarioId: number;
  questionarioId: number;
  questionarioTitulo: string;
  clienteId?: number;
  agendamentoId?: number;
  dtResposta: string;
  ipOrigem?: string;
  userAgent?: string;
  dispositivo?: string;
  deletado: boolean;
  dtDeletado?: string;
  termos: TermoAceito[];
  assinaturas: AssinaturaCapturada[];
}

export interface TermoAceito {
  respostaPerguntaId: number;
  perguntaId: number;
  perguntaTexto: string;
  aceitouTermo?: boolean;
  dataAceite?: string;
  textoTermoRenderizado?: string;
  hashTermoEsperado?: string;
  hashTermoCalculado?: string;
  /** ⚠️ false = hash adulterado (alerta vermelho na UI). */
  integridadeOk: boolean;
}

export interface AssinaturaCapturada {
  respostaPerguntaId: number;
  perguntaId: number;
  perguntaTexto: string;
  arquivoAssinaturaClienteId?: number;
  arquivoAssinaturaProfissionalId?: number;
  /** Path relativo já pronto: /api/v1/resposta-questionario/{id}/assinatura/cliente?perguntaId=... */
  urlAssinaturaCliente?: string;
  urlAssinaturaProfissional?: string;
}
```

---

## 4. Resolução das variáveis — feita pelo BACKEND (PR7)

> **⚠️ MUDANÇA IMPORTANTE:** o front **NÃO precisa mais** substituir as variáveis `{{nomeCliente}}`, `{{cpfCliente}}`, etc. O backend faz isso quando o GET do questionário recebe os IDs.

### 4.1. Como funciona

Ao acessar a URL do questionário (ex.: `/avaliacao/bellory-salon-awhxm/3/responder?cliente=18&agendamento=126&funcionario=9`), o front:

1. Extrai os IDs da query string.
2. Chama o GET passando esses IDs:
   ```
   GET /api/v1/public/{slug}/questionarios/{id}?clienteId=18&agendamentoId=126&funcionarioId=9
   ```
3. Cada `PerguntaDTO` tipo TERMO_CONSENTIMENTO vem com **dois campos**:
   - `textoTermo` — template original com `{{placeholders}}` (referência).
   - `textoTermoRenderizado` — texto **já resolvido** pronto pra exibir. **Use este.**
4. No POST de resposta, devolve o `textoTermoRenderizado` recebido (servidor calcula o hash sobre ele).

### 4.2. Exemplo de chamada

```ts
const params = new URLSearchParams(window.location.search);

const response = await api.get(
  `/api/v1/public/${slug}/questionarios/${id}`,
  {
    params: {
      clienteId: params.get('cliente') ?? undefined,
      agendamentoId: params.get('agendamento') ?? undefined,
      funcionarioId: params.get('funcionario') ?? undefined,
    },
  }
);

const questionario: QuestionarioDTO = response.data.dados;
// questionario.perguntas[N].textoTermoRenderizado já vem pronto
```

### 4.3. Variáveis canônicas (resolvidas pelo backend)

| Variável | Origem |
|---|---|
| `{{nomeCliente}}` | `Cliente.nomeCompleto` |
| `{{cpfCliente}}` | `Cliente.cpf` |
| `{{telefoneCliente}}` | `Cliente.telefone` |
| `{{rgCliente}}` | resolve para vazio (modelo atual sem RG) |
| `{{nomeProfissional}}` | `Funcionario.nomeCompleto` (informado ou único do agendamento) |
| `{{nomeEstabelecimento}}` | `Organizacao.nomeFantasia` |
| `{{cnpjEstabelecimento}}` | `Organizacao.cnpj` |
| `{{enderecoEstabelecimento}}` | endereço principal formatado |
| `{{nomeProcedimento}}` | nomes dos serviços do agendamento (joined `, `) |
| `{{dataAtendimento}}` | `dd/MM/yyyy` |
| `{{horarioAtendimento}}` | `HH:mm` |

### 4.4. Erros possíveis do GET

- **403 Forbidden**: cliente/agendamento/funcionário não pertence ao tenant do slug, ou o agendamento não pertence ao cliente informado.
- **404 Not Found**: questionário inexistente ou inativo.
- **429 Too Many Requests**: rate limit (60/h por IP no GET).

### 4.5. Fallback: GET sem os IDs

Se o front omitir os parâmetros, `textoTermoRenderizado` vem `null` e o front recebe apenas `textoTermo` cru. Nesse caso, o cliente só consegue ver os placeholders literais `{{nomeCliente}}` — não use esse fluxo no preenchimento real, apenas para preview de admin.

> ✂️ **Não há mais helper `templateRenderer.ts`** no front. Não construa `TemplateContexto` manualmente.

---

## 5. Helper de captura de assinatura (canvas)

Arquivo sugerido: `src/components/AssinaturaCanvas.tsx`.

### 5.1. Componente

```tsx
import { useRef, useState, useImperativeHandle, forwardRef } from 'react';

export interface AssinaturaCanvasRef {
  /** Retorna data URL "data:image/png;base64,..." ou null se canvas estiver vazio. */
  getDataUrl: () => string | null;
  clear: () => void;
  isEmpty: () => boolean;
}

interface Props {
  largura?: number;   // default 600
  altura?: number;    // default 200
  disabled?: boolean;
}

export const AssinaturaCanvas = forwardRef<AssinaturaCanvasRef, Props>(
  ({ largura = 600, altura = 200, disabled = false }, ref) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const [drawing, setDrawing] = useState(false);
    const [hasStrokes, setHasStrokes] = useState(false);

    const getCtx = () => canvasRef.current?.getContext('2d') ?? null;

    const start = (x: number, y: number) => {
      if (disabled) return;
      const ctx = getCtx();
      if (!ctx) return;
      ctx.beginPath();
      ctx.moveTo(x, y);
      ctx.lineWidth = 2;
      ctx.lineCap = 'round';
      ctx.strokeStyle = '#000';
      setDrawing(true);
    };
    const move = (x: number, y: number) => {
      if (!drawing) return;
      const ctx = getCtx();
      if (!ctx) return;
      ctx.lineTo(x, y);
      ctx.stroke();
      setHasStrokes(true);
    };
    const end = () => setDrawing(false);

    const onMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
      const r = canvasRef.current!.getBoundingClientRect();
      start(e.clientX - r.left, e.clientY - r.top);
    };
    const onMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
      const r = canvasRef.current!.getBoundingClientRect();
      move(e.clientX - r.left, e.clientY - r.top);
    };
    const onTouchStart = (e: React.TouchEvent<HTMLCanvasElement>) => {
      const r = canvasRef.current!.getBoundingClientRect();
      const t = e.touches[0];
      start(t.clientX - r.left, t.clientY - r.top);
    };
    const onTouchMove = (e: React.TouchEvent<HTMLCanvasElement>) => {
      e.preventDefault();
      const r = canvasRef.current!.getBoundingClientRect();
      const t = e.touches[0];
      move(t.clientX - r.left, t.clientY - r.top);
    };

    useImperativeHandle(ref, () => ({
      getDataUrl: () => {
        if (!hasStrokes || !canvasRef.current) return null;
        return canvasRef.current.toDataURL('image/png');
      },
      clear: () => {
        const ctx = getCtx();
        if (ctx && canvasRef.current) {
          ctx.clearRect(0, 0, canvasRef.current.width, canvasRef.current.height);
        }
        setHasStrokes(false);
      },
      isEmpty: () => !hasStrokes,
    }));

    return (
      <div>
        <canvas
          ref={canvasRef}
          width={largura}
          height={altura}
          style={{ border: '1px solid #ccc', borderRadius: 4, touchAction: 'none' }}
          onMouseDown={onMouseDown}
          onMouseMove={onMouseMove}
          onMouseUp={end}
          onMouseLeave={end}
          onTouchStart={onTouchStart}
          onTouchMove={onTouchMove}
          onTouchEnd={end}
        />
      </div>
    );
  }
);
```

### 5.2. Uso

```tsx
const canvasRef = useRef<AssinaturaCanvasRef>(null);

// Render
<AssinaturaCanvas
  ref={canvasRef}
  largura={pergunta.larguraAssinatura ?? 600}
  altura={pergunta.alturaAssinatura ?? 200}
/>
<button type="button" onClick={() => canvasRef.current?.clear()}>Limpar</button>

// No submit
const dataUrl = canvasRef.current?.getDataUrl();
if (!dataUrl && pergunta.obrigatoria) {
  alert('Assinatura obrigatória.');
  return;
}
respostaPergunta.assinaturaClienteBase64 = dataUrl ?? undefined;
```

---

## 6. QuestionarioCadastro.tsx — editor de perguntas

### 6.1. Tipos disponíveis

Adicionar `TERMO_CONSENTIMENTO` e `ASSINATURA` ao seletor de tipo de pergunta.

```ts
const TIPOS_DISPONIVEIS: { value: TipoPergunta; label: string }[] = [
  { value: TipoPergunta.TEXTO_CURTO, label: 'Texto Curto' },
  { value: TipoPergunta.TEXTO_LONGO, label: 'Texto Longo' },
  // ... existentes ...
  { value: TipoPergunta.TERMO_CONSENTIMENTO, label: 'Termo de Consentimento' },
  { value: TipoPergunta.ASSINATURA, label: 'Assinatura Digital' },
];

// Esconde TERMO/ASSINATURA quando questionário é anônimo
const tiposVisiveis = anonimo
  ? TIPOS_DISPONIVEIS.filter(t =>
      t.value !== TipoPergunta.TERMO_CONSENTIMENTO &&
      t.value !== TipoPergunta.ASSINATURA)
  : TIPOS_DISPONIVEIS;
```

### 6.2. Painel quando tipo = TERMO_CONSENTIMENTO

```tsx
{pergunta.tipo === TipoPergunta.TERMO_CONSENTIMENTO && (
  <div className="termo-config">
    <label>Template:</label>
    <select
      value={pergunta.templateTermoId ?? TipoTemplateTermo.CUSTOM}
      onChange={(e) => {
        const tpId = e.target.value as TipoTemplateTermo;
        const template = templates.find(t => t.id === tpId);
        atualizarPergunta({
          templateTermoId: tpId,
          textoTermo: template?.conteudo ?? pergunta.textoTermo,
        });
      }}
    >
      <option value={TipoTemplateTermo.CUSTOM}>Customizado</option>
      {templates.map(t => (
        <option key={t.id} value={t.id}>{t.nome}</option>
      ))}
    </select>

    <label>Texto do termo (Markdown, {`{{variavel}}`} suportado):</label>
    <textarea
      value={pergunta.textoTermo ?? ''}
      onChange={(e) => atualizarPergunta({ textoTermo: e.target.value })}
      rows={12}
      maxLength={10000}
      placeholder="Eu, {{nomeCliente}}, autorizo..."
    />
    <small>{(pergunta.textoTermo?.length ?? 0)} / 10000 caracteres (mínimo 10)</small>

    <label>
      <input
        type="checkbox"
        checked={pergunta.requerAceiteExplicito ?? false}
        onChange={(e) => atualizarPergunta({ requerAceiteExplicito: e.target.checked })}
      />
      Exigir aceite explícito (checkbox "Li e concordo")
    </label>
  </div>
)}
```

### 6.3. Painel quando tipo = ASSINATURA

```tsx
{pergunta.tipo === TipoPergunta.ASSINATURA && (
  <div className="assinatura-config">
    <label>Largura do canvas (200–1200 px):</label>
    <input
      type="number"
      min={200}
      max={1200}
      value={pergunta.larguraAssinatura ?? 600}
      onChange={(e) => atualizarPergunta({ larguraAssinatura: Number(e.target.value) })}
    />

    <label>Altura do canvas (100–600 px):</label>
    <input
      type="number"
      min={100}
      max={600}
      value={pergunta.alturaAssinatura ?? 200}
      onChange={(e) => atualizarPergunta({ alturaAssinatura: Number(e.target.value) })}
    />

    <label>
      <input
        type="checkbox"
        checked={pergunta.exigirAssinaturaProfissional ?? false}
        onChange={(e) => atualizarPergunta({ exigirAssinaturaProfissional: e.target.checked })}
      />
      Exigir também assinatura do profissional responsável
    </label>
  </div>
)}
```

### 6.4. Carregar templates ao montar

```tsx
const [templates, setTemplates] = useState<TemplateTermoDTO[]>([]);

useEffect(() => {
  api.get<{ dados: TemplateTermoDTO[] }>('/api/v1/questionarios/templates-termo')
    .then(res => setTemplates(res.data.dados))
    .catch(() => setTemplates([]));
}, []);
```

### 6.5. Validação no submit

Replicar as regras do backend para feedback imediato:

```ts
function validarPerguntas(perguntas: PerguntaCreateDTO[], anonimo: boolean): string | null {
  for (const p of perguntas) {
    const ehTermoOuAssinatura =
      p.tipo === TipoPergunta.TERMO_CONSENTIMENTO ||
      p.tipo === TipoPergunta.ASSINATURA;

    if (anonimo && ehTermoOuAssinatura) {
      return 'Questionário anônimo não pode conter perguntas de termo ou assinatura.';
    }
    if (ehTermoOuAssinatura && p.opcoes && p.opcoes.length > 0) {
      return 'Termo/assinatura não aceitam opções de resposta.';
    }
    if (p.tipo === TipoPergunta.TERMO_CONSENTIMENTO) {
      if (!p.textoTermo || p.textoTermo.trim().length < 10) {
        return `Texto do termo é obrigatório (mínimo 10 caracteres) em "${p.texto}".`;
      }
      if (p.textoTermo.length > 10000) {
        return `Texto do termo excede 10000 caracteres em "${p.texto}".`;
      }
    }
    if (p.tipo === TipoPergunta.ASSINATURA) {
      if (p.larguraAssinatura !== undefined && (p.larguraAssinatura < 200 || p.larguraAssinatura > 1200)) {
        return `Largura da assinatura deve estar entre 200 e 1200 px em "${p.texto}".`;
      }
      if (p.alturaAssinatura !== undefined && (p.alturaAssinatura < 100 || p.alturaAssinatura > 600)) {
        return `Altura da assinatura deve estar entre 100 e 600 px em "${p.texto}".`;
      }
    }
  }
  return null;
}
```

---

## 7. QuestionarioResposta.tsx — preenchimento

### 7.1. Componente para bloco TERMO_CONSENTIMENTO

> **Importante:** `pergunta.textoTermoRenderizado` já vem pronto do backend (PR7). O componente não faz substituição.

```tsx
function BlocoTermo({ pergunta, onChange }: {
  pergunta: PerguntaDTO;
  onChange: (resposta: Partial<RespostaPerguntaCreateDTO>) => void;
}) {
  // Texto pronto vindo do servidor (com placeholders já substituídos).
  // Fallback: textoTermo cru se o GET tiver sido feito sem os IDs (apenas preview admin).
  const textoExibicao = pergunta.textoTermoRenderizado ?? pergunta.textoTermo ?? '';

  const [aceitou, setAceitou] = useState(false);

  useEffect(() => {
    onChange({
      aceitouTermo: aceitou,
      // Devolve EXATAMENTE o que o cliente viu — servidor calcula sha256 sobre isso.
      textoTermoRenderizado: aceitou ? textoExibicao : undefined,
    });
  }, [aceitou, textoExibicao]);

  return (
    <div className="bloco-termo">
      <h3>{pergunta.texto}</h3>
      <div className="termo-conteudo">
        <ReactMarkdown>{textoExibicao}</ReactMarkdown>
      </div>
      {pergunta.requerAceiteExplicito && (
        <label className="aceite-explicito">
          <input
            type="checkbox"
            checked={aceitou}
            onChange={(e) => setAceitou(e.target.checked)}
          />
          Li e concordo com os termos acima
        </label>
      )}
    </div>
  );
}
```

### 7.2. Componente para bloco ASSINATURA

```tsx
function BlocoAssinatura({ pergunta, onChange }: {
  pergunta: PerguntaDTO;
  onChange: (resposta: Partial<RespostaPerguntaCreateDTO>) => void;
}) {
  const canvasClienteRef = useRef<AssinaturaCanvasRef>(null);
  const canvasProfRef = useRef<AssinaturaCanvasRef>(null);

  const handleConfirm = () => {
    const cliente = canvasClienteRef.current?.getDataUrl();
    const profissional = pergunta.exigirAssinaturaProfissional
      ? canvasProfRef.current?.getDataUrl()
      : undefined;

    onChange({
      assinaturaClienteBase64: cliente ?? undefined,
      assinaturaProfissionalBase64: profissional ?? undefined,
    });
  };

  return (
    <div className="bloco-assinatura">
      <h3>{pergunta.texto}</h3>

      <label>Sua assinatura:</label>
      <AssinaturaCanvas
        ref={canvasClienteRef}
        largura={pergunta.larguraAssinatura ?? 600}
        altura={pergunta.alturaAssinatura ?? 200}
      />
      <button type="button" onClick={() => canvasClienteRef.current?.clear()}>Limpar</button>

      {pergunta.exigirAssinaturaProfissional && (
        <>
          <label>Assinatura do profissional responsável:</label>
          <AssinaturaCanvas
            ref={canvasProfRef}
            largura={pergunta.larguraAssinatura ?? 600}
            altura={pergunta.alturaAssinatura ?? 200}
          />
          <button type="button" onClick={() => canvasProfRef.current?.clear()}>Limpar</button>
        </>
      )}

      <button type="button" onClick={handleConfirm}>Confirmar assinatura</button>
    </div>
  );
}
```

### 7.3. Switch principal de renderização

```tsx
function renderBlocoPergunta(p: PerguntaDTO, onChange: (r: Partial<RespostaPerguntaCreateDTO>) => void) {
  switch (p.tipo) {
    case TipoPergunta.TEXTO_CURTO:
    case TipoPergunta.TEXTO_LONGO:
      return <BlocoTexto pergunta={p} onChange={onChange} />;
    // ... outros tipos existentes ...
    case TipoPergunta.TERMO_CONSENTIMENTO:
      return <BlocoTermo pergunta={p} onChange={onChange} />;
    case TipoPergunta.ASSINATURA:
      return <BlocoAssinatura pergunta={p} onChange={onChange} />;
    default:
      return <div>Tipo não suportado: {p.tipo}</div>;
  }
}
```

### 7.4. Validação antes do submit

```ts
function validarRespostas(perguntas: PerguntaDTO[], respostas: Map<number, RespostaPerguntaCreateDTO>): string | null {
  for (const p of perguntas) {
    const r = respostas.get(p.id);

    if (p.tipo === TipoPergunta.TERMO_CONSENTIMENTO) {
      if (p.requerAceiteExplicito && !r?.aceitouTermo) {
        return `Você precisa aceitar o termo: "${p.texto}".`;
      }
      if (r?.aceitouTermo && !r.textoTermoRenderizado) {
        return `Erro interno: termo aceito sem texto renderizado em "${p.texto}".`;
      }
    }

    if (p.tipo === TipoPergunta.ASSINATURA) {
      if (p.obrigatoria && !r?.assinaturaClienteBase64) {
        return `Assinatura obrigatória em "${p.texto}".`;
      }
      if (p.exigirAssinaturaProfissional && r?.assinaturaClienteBase64 && !r.assinaturaProfissionalBase64) {
        return `Assinatura do profissional obrigatória em "${p.texto}".`;
      }
    }

    if (p.obrigatoria && !temAlgumaResposta(r)) {
      return `Pergunta obrigatória sem resposta: "${p.texto}".`;
    }
  }
  return null;
}
```

### 7.5. Submit

```ts
async function submeter(formData: {
  questionarioId: number;
  clienteId?: number;
  agendamentoId?: number;
  respostas: RespostaPerguntaCreateDTO[];
}, slug?: string) {
  const url = slug
    ? `/api/v1/public/${slug}/questionarios/${formData.questionarioId}/respostas`
    : `/api/v1/questionarios/${formData.questionarioId}/respostas`;

  const payload = {
    questionarioId: formData.questionarioId,
    clienteId: formData.clienteId,
    agendamentoId: formData.agendamentoId,
    userAgent: navigator.userAgent,
    dispositivo: detectarDispositivo(), // 'mobile' | 'desktop' | 'tablet'
    respostas: formData.respostas,
    // dataAceite/hashTermo/ip → NÃO enviar; servidor seta
  };

  const { data } = await api.post(url, payload);
  return data;
}
```

---

## 8. Tela de auditoria (admin)

Apenas para usuários com role ADMIN/SUPERADMIN. Sugestão de página: `src/pages/admin/AuditoriaResposta.tsx`.

```tsx
function AuditoriaResposta({ respostaId }: { respostaId: number }) {
  const [auditoria, setAuditoria] = useState<AuditoriaTermoDTO | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    api.get<{ dados: AuditoriaTermoDTO }>(`/api/v1/resposta-questionario/${respostaId}/auditoria`)
      .then(res => setAuditoria(res.data.dados))
      .catch(err => setErro(err.response?.status === 403 ? 'Sem permissão' : 'Erro ao carregar auditoria'));
  }, [respostaId]);

  if (erro) return <div className="erro">{erro}</div>;
  if (!auditoria) return <div>Carregando...</div>;

  return (
    <div className="auditoria">
      <h2>Auditoria — {auditoria.questionarioTitulo}</h2>

      <section className="metadados">
        <p><strong>Data:</strong> {format(new Date(auditoria.dtResposta), 'dd/MM/yyyy HH:mm:ss')}</p>
        <p><strong>IP:</strong> {auditoria.ipOrigem ?? '—'}</p>
        <p><strong>User-Agent:</strong> <code>{auditoria.userAgent ?? '—'}</code></p>
        <p><strong>Dispositivo:</strong> {auditoria.dispositivo ?? '—'}</p>
        {auditoria.deletado && (
          <p className="alerta">⚠️ Soft-deleted em {auditoria.dtDeletado}</p>
        )}
      </section>

      <section className="termos">
        <h3>Termos aceitos ({auditoria.termos.length})</h3>
        {auditoria.termos.map(t => (
          <div key={t.respostaPerguntaId} className="termo-bloco">
            <h4>{t.perguntaTexto}</h4>
            <p>
              {t.aceitouTermo ? '✅ Aceito' : '❌ Não aceito'}
              {t.dataAceite && ` em ${format(new Date(t.dataAceite), 'dd/MM/yyyy HH:mm:ss')}`}
            </p>
            <div className={`integridade ${t.integridadeOk ? 'ok' : 'falha'}`}>
              {t.integridadeOk
                ? '✅ Integridade verificada (hash bate)'
                : '⚠️ HASH NÃO CONFERE — possível adulteração'}
            </div>
            <details>
              <summary>Texto aceito + hash</summary>
              <p><strong>Hash esperado:</strong> <code>{t.hashTermoEsperado}</code></p>
              <p><strong>Hash recalculado:</strong> <code>{t.hashTermoCalculado}</code></p>
              <pre>{t.textoTermoRenderizado}</pre>
            </details>
          </div>
        ))}
      </section>

      <section className="assinaturas">
        <h3>Assinaturas ({auditoria.assinaturas.length})</h3>
        {auditoria.assinaturas.map(a => (
          <div key={a.respostaPerguntaId} className="assinatura-bloco">
            <h4>{a.perguntaTexto}</h4>
            {a.urlAssinaturaCliente && (
              <div>
                <p>Cliente:</p>
                <ImagemAutenticada src={a.urlAssinaturaCliente} alt="Assinatura cliente" />
              </div>
            )}
            {a.urlAssinaturaProfissional && (
              <div>
                <p>Profissional:</p>
                <ImagemAutenticada src={a.urlAssinaturaProfissional} alt="Assinatura profissional" />
              </div>
            )}
          </div>
        ))}
      </section>

      <a
        href={`/api/v1/resposta-questionario/${respostaId}/comprovante.pdf`}
        target="_blank"
        rel="noopener noreferrer"
        className="btn-pdf"
      >
        📄 Baixar comprovante PDF
      </a>
    </div>
  );
}
```

### 8.1. Helper para imagem autenticada (`<ImagemAutenticada>`)

Como o endpoint de download requer JWT, não dá para usar `<img src=...>` direto. Faça fetch com auth e converta para object URL:

```tsx
function ImagemAutenticada({ src, alt }: { src: string; alt: string }) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);

  useEffect(() => {
    let revoked = false;
    api.get(src, { responseType: 'blob' })
      .then(res => {
        if (revoked) return;
        const url = URL.createObjectURL(res.data);
        setObjectUrl(url);
      });
    return () => {
      revoked = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [src]);

  return objectUrl ? <img src={objectUrl} alt={alt} style={{ maxWidth: 400, border: '1px solid #ddd' }} /> : <span>Carregando...</span>;
}
```

---

## 9. Comprovante PDF — botão de download

Para abrir inline:

```tsx
<a
  href={`/api/v1/resposta-questionario/${respostaId}/comprovante.pdf`}
  target="_blank"
  rel="noopener noreferrer"
>
  📄 Baixar comprovante
</a>
```

> ⚠️ Como o endpoint exige JWT, navegação direta via `<a>` só funciona se o JWT estiver em cookie. Se for header `Authorization`, faça download via fetch:

```tsx
async function baixarPdf(respostaId: number) {
  const res = await api.get(
    `/api/v1/resposta-questionario/${respostaId}/comprovante.pdf`,
    { responseType: 'blob' }
  );
  const url = URL.createObjectURL(res.data);
  window.open(url, '_blank');
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}
```

---

## 10. AgendamentoQuestionario — tracking duplo

Na tela de gestão de agendamentos / anamnese, mostrar **dois badges separados**:

```tsx
function StatusBadges({ aq }: { aq: AgendamentoQuestionarioDTO }) {
  return (
    <div className="status-badges">
      <Badge variant={statusRespostaVariant(aq.status)}>
        Resposta: {labelStatusResposta(aq.status)}
      </Badge>
      {aq.statusAssinatura !== StatusAssinatura.NAO_REQUERIDA && (
        <Badge variant={statusAssinaturaVariant(aq.statusAssinatura)}>
          Assinatura: {labelStatusAssinatura(aq.statusAssinatura)}
        </Badge>
      )}
    </div>
  );
}

function labelStatusResposta(s: StatusQuestionarioAgendamento): string {
  return {
    PENDENTE: 'Pendente',
    ENVIADO: 'Enviado',
    RESPONDIDO: 'Respondido',
    FALHOU: 'Falhou',
  }[s];
}

function labelStatusAssinatura(s: StatusAssinatura): string {
  return {
    NAO_REQUERIDA: 'Não requerida',
    PENDENTE: 'Pendente',
    ASSINADA: 'Assinada',
  }[s];
}
```

Quando `statusAssinatura === NAO_REQUERIDA`, esconda o badge.

---

## 11. Validações que o front deve replicar

Lista das validações que o backend retorna 400. Replicar no front melhora UX.

### Cadastro de pergunta

| Regra | Mensagem sugerida |
|---|---|
| `anonimo=true` + tipo TERMO/ASSINATURA | "Questionário anônimo não pode conter perguntas de termo ou assinatura." |
| Tipo TERMO/ASSINATURA + opções não-vazias | "Termo/assinatura não aceitam opções de resposta." |
| TERMO sem `textoTermo` | "Texto do termo é obrigatório." |
| `textoTermo` < 10 caracteres | "Texto do termo deve ter no mínimo 10 caracteres." |
| `textoTermo` > 10000 caracteres | "Texto do termo excede 10000 caracteres." |
| `larguraAssinatura` fora de [200, 1200] | "Largura da assinatura deve estar entre 200 e 1200 px." |
| `alturaAssinatura` fora de [100, 600] | "Altura da assinatura deve estar entre 100 e 600 px." |

### Submissão de resposta

| Regra | Mensagem sugerida |
|---|---|
| `requerAceiteExplicito=true` + `aceitouTermo!=true` | "Você precisa aceitar o termo." |
| `aceitouTermo=true` sem `textoTermoRenderizado` | (erro interno do front — bug) |
| Pergunta ASSINATURA obrigatória sem `assinaturaClienteBase64` | "Assinatura obrigatória." |
| `exigirAssinaturaProfissional=true` + ausência da assinatura do profissional | "Assinatura do profissional obrigatória." |
| Prefixo do data URL inválido | "Formato de assinatura inválido. Aceito apenas PNG ou SVG." |
| Bytes descodificados > 200KB | "Assinatura excede 200KB." |

### Detecção de canvas vazio

```ts
function canvasVazio(canvas: HTMLCanvasElement): boolean {
  const ctx = canvas.getContext('2d');
  if (!ctx) return true;
  const data = ctx.getImageData(0, 0, canvas.width, canvas.height).data;
  for (let i = 3; i < data.length; i += 4) {
    if (data[i] !== 0) return false;  // pixel não-transparente encontrado
  }
  return true;
}
```

---

## 12. Casos de uso end-to-end

### 12.1. Cadastrar questionário com termo + assinatura

```ts
const payload: QuestionarioCreateDTO = {
  titulo: 'Anamnese Pré-Procedimento',
  descricao: 'Ficha de saúde + termo de responsabilidade',
  tipo: TipoQuestionario.CLIENTE,
  ativo: true,
  obrigatorio: false,
  anonimo: false,
  perguntas: [
    {
      texto: 'Você tem alergia a algum medicamento?',
      tipo: TipoPergunta.SIM_NAO,
      obrigatoria: true,
      ordem: 1,
    },
    {
      texto: 'Termo de responsabilidade',
      tipo: TipoPergunta.TERMO_CONSENTIMENTO,
      obrigatoria: true,
      ordem: 2,
      templateTermoId: TipoTemplateTermo.PADRAO_PROCEDIMENTO,
      textoTermo: 'Eu, **{{nomeCliente}}**, autorizo o procedimento {{nomeProcedimento}}...',
      requerAceiteExplicito: true,
    },
    {
      texto: 'Assinatura',
      tipo: TipoPergunta.ASSINATURA,
      obrigatoria: true,
      ordem: 3,
      formatoAssinatura: FormatoAssinatura.PNG_BASE64,
      larguraAssinatura: 600,
      alturaAssinatura: 200,
      exigirAssinaturaProfissional: true,
    },
  ],
};

await api.post('/api/v1/questionarios', payload);
```

### 12.2. Cliente preenche e assina (fluxo público via slug)

```ts
// 1. URL: /avaliacao/{slug}/{id}/responder?cliente=18&agendamento=126&funcionario=9
// 2. Front faz GET passando os IDs — backend resolve as variáveis
const params = new URLSearchParams(window.location.search);
const { data: { dados: questionario } } = await api.get<{ dados: QuestionarioDTO }>(
  `/api/v1/public/${slug}/questionarios/${id}`,
  {
    params: {
      clienteId: params.get('cliente'),
      agendamentoId: params.get('agendamento'),
      funcionarioId: params.get('funcionario'),
    },
  },
);

// 3. Cliente vê o termo já renderizado e preenche/assina
// 4. No submit, devolve o textoTermoRenderizado recebido
const perguntaTermo = questionario.perguntas.find(p => p.tipo === TipoPergunta.TERMO_CONSENTIMENTO)!;

const payload: RespostaQuestionarioCreateDTO = {
  questionarioId: questionario.id,
  clienteId: Number(params.get('cliente')),
  agendamentoId: Number(params.get('agendamento')),
  userAgent: navigator.userAgent,
  dispositivo: 'mobile',
  respostas: [
    { perguntaId: 100, respostaTexto: 'Não' },                                    // SIM_NAO
    {
      perguntaId: perguntaTermo.id,                                               // TERMO
      aceitouTermo: true,
      textoTermoRenderizado: perguntaTermo.textoTermoRenderizado,                 // pronto do backend
    },
    {
      perguntaId: 102,                                                            // ASSINATURA
      assinaturaClienteBase64: canvasCliente.toDataURL('image/png'),
      assinaturaProfissionalBase64: canvasProf.toDataURL('image/png'),
    },
  ],
};

await api.post(`/api/v1/public/${slug}/questionarios/${questionario.id}/respostas`, payload);
```

### 12.3. Admin auditando uma resposta

```ts
const auditoria = (await api.get<{ dados: AuditoriaTermoDTO }>(
  `/api/v1/resposta-questionario/789/auditoria`
)).data.dados;

if (!auditoria.termos.every(t => t.integridadeOk)) {
  alert('⚠️ Pelo menos um termo teve hash adulterado.');
}
```

### 12.4. Cliente baixando o próprio comprovante

```ts
const blob = (await api.get(
  `/api/v1/resposta-questionario/789/comprovante.pdf`,
  { responseType: 'blob' }
)).data;
const url = URL.createObjectURL(blob);
window.open(url, '_blank');
```

---

## 13. Limitações da fase 1

| Limitação | O que isso significa pro front | Workaround / fase 2 |
|---|---|---|
| **SVG não é embutido no PDF** | Se o canvas exporta SVG, o PDF mostra placeholder de texto. | **Sempre exporte PNG** via `canvas.toDataURL('image/png')`. |
| **Logo da org não aparece no PDF** | Cabeçalho do PDF mostra só nome + CNPJ. | Fase 2: implementar fetch local da logo no backend. |
| **Sem CRUD de templates** | Não tente exibir UI de "Criar meu template". | Use só o catálogo `GET /templates-termo` por enquanto. |
| **Render Markdown no PDF é texto plano** | Negrito/itálico/headings perdem formatação no PDF. Visual no front continua rico (use `react-markdown`). | Fase 2: render rico via troca de fonte. |
| **Estatísticas podem incluir respostas soft-deleted** | Bug latente — não crítico em fase 1. | Fase 2 do backend. |
| **Trigger de imutabilidade não existe** | Confiamos em design (sem rota de update). | Fase 2 do backend. |

---

## 14. Checklist de entrega front-end

### Tipos / utils

- [ ] Adicionar enums `TipoPergunta` (estendido), `FormatoAssinatura`, `TipoTemplateTermo`, `StatusAssinatura`.
- [ ] Adicionar interfaces `PerguntaCreateDTO` (com campos novos), `PerguntaDTO` (incluindo `textoTermoRenderizado`), `RespostaPerguntaCreateDTO`, `RespostaPerguntaDTO`, `TemplateTermoDTO`, `AuditoriaTermoDTO`, `AgendamentoQuestionarioDTO`.
- [ ] **Não criar `templateRenderer.ts`** — backend resolve as variáveis (PR7).
- [ ] Implementar componente `AssinaturaCanvas`.
- [ ] Implementar componente `ImagemAutenticada` (fetch + objectURL).

### QuestionarioCadastro.tsx

- [ ] Adicionar opções "Termo de Consentimento" e "Assinatura" no seletor de tipo.
- [ ] Esconder esses tipos quando `anonimo === true`.
- [ ] Painel de configuração TERMO_CONSENTIMENTO com select de template + textarea + checkbox.
- [ ] Painel de configuração ASSINATURA com inputs de largura/altura + checkbox profissional.
- [ ] Carregar templates ao montar (`GET /templates-termo`).
- [ ] Validações no submit (reaproveitar mensagens da seção §11).

### QuestionarioResposta.tsx

- [ ] No mount, ler `cliente`/`agendamento`/`funcionario` da query string e passar como `clienteId`/`agendamentoId`/`funcionarioId` no GET do questionário.
- [ ] `BlocoTermo`: render Markdown a partir de `pergunta.textoTermoRenderizado` (já vem pronto do backend) + checkbox de aceite.
- [ ] `BlocoAssinatura`: canvas cliente + canvas profissional (quando `exigirAssinaturaProfissional`).
- [ ] Switch de renderização cobre TERMO_CONSENTIMENTO e ASSINATURA.
- [ ] Validação de aceite + assinatura antes de submeter.
- [ ] No submit, anexar `aceitouTermo`, `textoTermoRenderizado` (devolver o que foi recebido do GET), `assinaturaClienteBase64`, `assinaturaProfissionalBase64`.

### Listagem / gestão de agendamento

- [ ] Mostrar `statusAssinatura` como badge separado, ao lado do `status` da resposta.
- [ ] Esconder badge de assinatura quando `statusAssinatura === NAO_REQUERIDA`.
- [ ] Mostrar `dtAssinatura` quando disponível.

### Tela de auditoria (admin)

- [ ] Página/dialog `AuditoriaResposta` com:
  - [ ] Metadados (data, IP, UA, dispositivo, soft-delete).
  - [ ] Lista de termos aceitos com `integridadeOk` (✅/⚠️).
  - [ ] Lista de assinaturas com preview via `ImagemAutenticada`.
  - [ ] Botão "Baixar comprovante PDF".
- [ ] Restringir acesso por role (verificar role no front antes de mostrar link).

### Comprovante PDF

- [ ] Botão "Baixar comprovante" no detalhe da resposta (admin) e no detalhe do agendamento (cliente).
- [ ] Implementar via fetch + blob → object URL para suportar JWT em header.

### Polimento

- [ ] Mensagens de erro alinhadas com as do backend (seção §11).
- [ ] Indicar visualmente caracteres restantes em `textoTermo` (X/10000).
- [ ] Esconder UI de "Criar template" (não existe na fase 1).
- [ ] Garantir que `canvas.toDataURL('image/png')` é usado (não `'image/svg+xml'`).
- [ ] Botão "Limpar" em cada canvas de assinatura.

---

## Resumo executivo

3 telas afetadas (`QuestionarioCadastro`, `QuestionarioResposta`, `AuditoriaResposta`), 2 componentes novos (`AssinaturaCanvas`, `ImagemAutenticada`), 9 endpoints consumidos (1 catálogo + 1 cadastro + 2 submissão + 2 auditoria + 1 PDF + 2 já existentes para listar/buscar resposta). Tracking de agendamento ganha um badge extra para assinatura.

**Trabalho que NÃO precisa fazer (graças ao PR7):** o backend resolve as variáveis `{{nomeCliente}}` etc. Sem helper de renderização no front, sem montagem de contexto, sem endpoints extras pra buscar dados de cliente/serviço.

**Prioridade de implementação sugerida:**
1. Tipos + utils (sem UI ainda).
2. `QuestionarioCadastro` (admin testa cadastro).
3. `QuestionarioResposta` (cliente preenche).
4. Tela de auditoria + botão PDF (validação legal).
5. Tracking duplo nos badges.
