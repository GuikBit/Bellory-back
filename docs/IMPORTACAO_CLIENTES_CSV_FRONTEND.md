# Importacao de Clientes via CSV - Guia de Integracao Frontend

## Visao Geral

Funcionalidade para cadastrar clientes em massa atraves de upload de um arquivo CSV. O backend processa o arquivo de forma **assincrona**: a requisicao de upload retorna imediatamente com um `importId`, e o frontend deve fazer **polling** num endpoint de status para mostrar o progresso e exibir erros por linha ao final.

**Base URL:** `/api/v1/cliente`
**Autenticacao:** JWT obrigatorio (Bearer token)
**Multitenancy:** Organizacao identificada automaticamente pelo token

### Fluxo resumido (UX)

A tela tem duas areas: **historico de importacoes** (lista) e **modal de nova importacao** (upload + acompanhamento). O polling do status acontece **somente quando o usuario abre uma importacao individual**, nao na listagem.

1. Tela de historico:
   - `GET /importar-csv` → lista as importacoes da organizacao (mais recentes primeiro).
   - Mostra status, nome do arquivo, percentual e contadores de cada uma.
   - Botao "Nova importacao" abre o modal.
   - Click em uma linha abre o detalhe (item 4).
2. No modal de nova importacao:
   - Baixa o template CSV oficial (`GET /importar-csv/template`).
   - Preenche o arquivo no Excel/Sheets/editor de texto e arrasta de volta no modal.
   - Frontend chama `POST /importar-csv` (multipart) → recebe `importId` e status `PENDENTE`.
   - Inicia polling no detalhe (item 4) e ja atualiza a tela de historico ao concluir.
3. Detalhe de uma importacao:
   - Frontend faz polling a cada 2s em `GET /importar-csv/{importId}`, atualizando barra de progresso.
   - Quando `status = CONCLUIDO` ou `FALHA`, **para o polling** e mostra resumo:
     - Linhas importadas / ignoradas / total.
     - Tabela com `linha` e `motivo` para cada erro.
     - Em caso de `FALHA`, mostrar `mensagemFalha`.
   - Para importacoes ja em estado terminal, **nao iniciar polling** — basta exibir o GET inicial.
4. Refresh da listagem de clientes apos `CONCLUIDO` com `importados > 0`.

---

## Endpoints

### 1. Baixar template CSV

```
GET /api/v1/cliente/importar-csv/template
Accept: text/csv
```

**Response:**
- `Content-Type: text/csv; charset=UTF-8`
- `Content-Disposition: attachment; filename="template_clientes.csv"`
- Body (binario, com BOM UTF-8 para o Excel reconhecer acentos):

```csv
nomeCompleto;telefone;email;cpf;dataNascimento
Maria Silva;(32) 99999-9999;maria@exemplo.com;123.456.789-00;1990-05-12
Joao Pereira;(11) 98888-8888;;;
```

**Exemplo de uso (JS/TS):**

```ts
async function baixarTemplate() {
  const res = await fetch('/api/v1/cliente/importar-csv/template', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'template_clientes.csv';
  a.click();
  URL.revokeObjectURL(url);
}
```

---

### 2. Iniciar importacao

```
POST /api/v1/cliente/importar-csv
Content-Type: multipart/form-data
```

**Form fields:**

| Campo | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| `file` | File (CSV) | Sim | Arquivo CSV. Tamanho maximo: **10 MB**. |

**Response 202 Accepted:**

```json
{
  "success": true,
  "message": "Importacao iniciada. Acompanhe pelo importId.",
  "dados": {
    "id": 42,
    "status": "PENDENTE",
    "nomeArquivo": "clientes-novembro.csv",
    "totalLinhas": 0,
    "processadas": 0,
    "importados": 0,
    "ignorados": 0,
    "percentual": 0,
    "erros": [],
    "mensagemFalha": null,
    "dtInicio": "2026-05-04T14:30:00",
    "dtFim": null
  }
}
```

**Erros possiveis:**

| HTTP | Mensagem | Quando |
|------|----------|--------|
| 400 | "Arquivo CSV obrigatorio." | `file` ausente ou vazio |
| 400 | "Arquivo excede o limite de 10 MB (...)" | Arquivo > 10 MB |
| 400 | "Falha ao ler o arquivo: ..." | IO ao ler bytes do upload |
| 500 | "Erro interno ao iniciar importacao: ..." | Falha inesperada |

> O backend **nao valida o conteudo CSV nesse endpoint** (header, linhas etc.). Validacoes de conteudo acontecem no worker async — se o CSV nao tiver o header obrigatorio, a importacao termina em `status = FALHA` com `mensagemFalha` populada.

**Exemplo:**

```ts
async function iniciarImportacao(file: File) {
  const form = new FormData();
  form.append('file', file);

  const res = await fetch('/api/v1/cliente/importar-csv', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  const json = await res.json();
  if (!json.success) throw new Error(json.message);
  return json.dados as ImportacaoStatus; // { id, status, ... }
}
```

---

### 3. Listar importacoes da organizacao

```
GET /api/v1/cliente/importar-csv
```

Retorna **resumo** de todas as importacoes da organizacao logada, ordenadas pela mais recente primeiro. **Nao inclui o array `erros`** — pra ver os erros completos, abra a importacao e use o endpoint de status individual (item 4).

**Response 200:**

```json
{
  "success": true,
  "message": "Importacoes recuperadas.",
  "dados": [
    {
      "id": 42,
      "status": "CONCLUIDO",
      "nomeArquivo": "clientes-novembro.csv",
      "totalLinhas": 250,
      "processadas": 250,
      "importados": 240,
      "ignorados": 10,
      "percentual": 100,
      "mensagemFalha": null,
      "dtInicio": "2026-05-04T14:30:00",
      "dtFim": "2026-05-04T14:32:18"
    },
    {
      "id": 41,
      "status": "FALHA",
      "nomeArquivo": "clientes-outubro.csv",
      "totalLinhas": 0,
      "processadas": 0,
      "importados": 0,
      "ignorados": 0,
      "percentual": 0,
      "mensagemFalha": "Header invalido: coluna 'telefone' ausente. Esperado: nomeCompleto,telefone,email,cpf,dataNascimento",
      "dtInicio": "2026-05-04T13:10:00",
      "dtFim": "2026-05-04T13:10:01"
    },
    {
      "id": 40,
      "status": "PROCESSANDO",
      "nomeArquivo": "clientes-setembro.csv",
      "totalLinhas": 1000,
      "processadas": 320,
      "importados": 300,
      "ignorados": 20,
      "percentual": 32,
      "mensagemFalha": null,
      "dtInicio": "2026-05-04T15:00:00",
      "dtFim": null
    }
  ]
}
```

**Erros possiveis:**

| HTTP | Mensagem | Quando |
|------|----------|--------|
| 500 | "Erro interno ao listar importacoes: ..." | Falha inesperada |

> **Nao faca polling neste endpoint.** Ele responde uma snapshot — se quiser ver progresso ao vivo, use o endpoint de status individual (item 4) sobre o `id` que o usuario clicou. Recarregue a listagem manualmente quando voltar para a tela de historico (ou apos o polling de uma importacao terminar).

> A listagem hoje **nao e paginada**. Se a base crescer muito, abrir um issue para paginar (`?page=`/`?size=`). Como referencia, ate a casa de centenas de registros isso esta tranquilo.

**Exemplo:**

```ts
async function listarImportacoes(): Promise<ImportacaoResumo[]> {
  const res = await fetch('/api/v1/cliente/importar-csv', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const json = (await res.json()) as ResponseAPI<ImportacaoResumo[]>;
  if (!json.success || !json.dados) throw new Error(json.message);
  return json.dados;
}
```

---

### 4. Consultar status (polling)

```
GET /api/v1/cliente/importar-csv/{importId}
```

**Response 200:**

```json
{
  "success": true,
  "message": "Status da importacao recuperado.",
  "dados": {
    "id": 42,
    "status": "PROCESSANDO",
    "nomeArquivo": "clientes-novembro.csv",
    "totalLinhas": 250,
    "processadas": 120,
    "importados": 110,
    "ignorados": 10,
    "percentual": 48,
    "erros": [
      { "linha": 5, "motivo": "Telefone obrigatorio" },
      { "linha": 8, "motivo": "CPF invalido (esperado 11 digitos)" }
    ],
    "mensagemFalha": null,
    "dtInicio": "2026-05-04T14:30:00",
    "dtFim": null
  }
}
```

**Erros possiveis:**

| HTTP | Mensagem | Quando |
|------|----------|--------|
| 404 | "Importacao N nao encontrada" | `importId` nao existe |
| 404 | "Importacao N nao pertence a esta organizacao" | Tentativa de consultar import de outro tenant |
| 500 | "Erro interno ao consultar importacao: ..." | Falha inesperada |

> O campo `percentual` e calculado a partir de `processadas / totalLinhas` arredondado para inteiro. **Enquanto `status = PENDENTE`** (antes do worker comecar), `totalLinhas = 0` e `percentual = 0` — mostre um loading indeterminado ate o status virar `PROCESSANDO`.

> Os campos `processadas`, `importados`, `ignorados` e `erros` sao atualizados pelo worker **a cada 20 linhas processadas** (flush periodico). Nao espere progresso fluido em CSVs muito pequenos.

---

## Estados (`status`)

| Valor | Significado | UI sugerida |
|-------|-------------|-------------|
| `PENDENTE` | Registro criado, worker async ainda nao comecou | Spinner indeterminado, "Aguardando processamento..." |
| `PROCESSANDO` | Worker consumindo o CSV linha a linha | Barra de progresso com `percentual`, contadores ao vivo |
| `CONCLUIDO` | Processou todas as linhas (com ou sem erros parciais) | Tela de resumo: importados / ignorados / tabela de erros |
| `FALHA` | Erro fatal (CSV invalido, header errado, IO etc.) | Mensagem de erro destacada com `mensagemFalha`; oferecer "Tentar novamente" |

**Estado terminal:** `CONCLUIDO` e `FALHA`. Pare o polling quando atingir um deles.

---

## Formato do CSV

### Header obrigatorio

```
nomeCompleto;telefone;email;cpf;dataNascimento
```

- A **ordem nao importa**, mas todas as 5 colunas devem existir.
- Se uma coluna obrigatoria estiver ausente, a importacao termina em `FALHA` com `mensagemFalha = "Header invalido: coluna 'X' ausente. Esperado: nomeCompleto,telefone,email,cpf,dataNascimento"`.

### Delimitador

- O backend **detecta automaticamente** entre `,` e `;` analisando a primeira linha.
- Se ambos aparecerem, escolhe o de maior frequencia.
- BOM UTF-8 (gerado pelo Excel BR) e tratado automaticamente.

### Encoding

- **UTF-8** obrigatorio. Se o usuario salvar do Excel BR como "CSV (separado por virgulas)", normalmente sai em ANSI/Windows-1252 e os acentos quebram. **Oriente no UI a salvar como "CSV UTF-8"**.

### Validacoes por linha

| Campo | Regra | Erro retornado |
|-------|-------|----------------|
| `nomeCompleto` | Obrigatorio (nao pode ser vazio/em branco) | "Nome obrigatorio" |
| `telefone` | Obrigatorio. Unico no CSV (ignorando formatacao). Unico por organizacao. | "Telefone obrigatorio" / "Telefone X repetido no CSV" / "Telefone X ja cadastrado" |
| `email` | Opcional. Se preenchido, deve ser unico na organizacao. | "Email X ja cadastrado" |
| `cpf` | Opcional. Se preenchido, deve ter 11 digitos (apos remover pontos/tracos). Unico por organizacao. | "CPF invalido (esperado 11 digitos)" / "CPF ja cadastrado" |
| `dataNascimento` | Opcional. Aceita `yyyy-MM-dd` ou `dd/MM/yyyy`. | "Data de nascimento invalida (use yyyy-MM-dd ou dd/MM/yyyy): X" |

### Comportamento ao importar

- Cada linha valida cria um `Cliente` com:
  - `username` gerado automaticamente: `cliente_rapido_<6 digitos aleatorios>`.
  - `password = "cliente_rapido"` (encoded). Cliente sera obrigado a redefinir.
  - `email` vazio salvo como `cliente_rapido@gmail.com` (placeholder).
  - `isCadastroIncompleto = true` para sinalizar perfil pendente.
  - `ativo = true`, `role = ROLE_CLIENTE`.
- Cada linha roda em **transacao propria**: erro em uma linha **nao derruba** as outras.
- O numero de linha reportado em `erros[].linha` e **1-based contando o header** (header = linha 1, primeira linha de dados = linha 2). Use isso no UI para referenciar a linha exata do arquivo do usuario.

---

## Tipos TypeScript sugeridos

```ts
export type StatusImportacao = 'PENDENTE' | 'PROCESSANDO' | 'CONCLUIDO' | 'FALHA';

export interface ImportacaoErro {
  linha: number;     // 1-based, header = 1
  motivo: string;
}

// Retornado pela LISTAGEM (sem erros[] pra payload leve).
export interface ImportacaoResumo {
  id: number;
  status: StatusImportacao;
  nomeArquivo: string | null;
  totalLinhas: number;
  processadas: number;
  importados: number;
  ignorados: number;
  percentual: number;     // 0-100
  mensagemFalha: string | null;
  dtInicio: string;       // ISO LocalDateTime
  dtFim: string | null;
}

// Retornado pelo STATUS individual (inclui erros[] completos).
export interface ImportacaoStatus extends ImportacaoResumo {
  erros: ImportacaoErro[];
}

export const TERMINAL_STATES: StatusImportacao[] = ['CONCLUIDO', 'FALHA'];
export const isTerminal = (s: StatusImportacao) => TERMINAL_STATES.includes(s);

export interface ResponseAPI<T> {
  success: boolean;
  message: string;
  dados?: T;
  errorCode?: number;
}
```

---

## Service / Hook de polling

Recomendacao: encapsular o polling num hook ou service para nao deixar timers orfaos.

```ts
const TERMINAL_STATES: StatusImportacao[] = ['CONCLUIDO', 'FALHA'];
const INTERVAL_MS = 2000;

async function getStatus(id: number): Promise<ImportacaoStatus> {
  const res = await fetch(`/api/v1/cliente/importar-csv/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const json = (await res.json()) as ResponseAPI<ImportacaoStatus>;
  if (!json.success || !json.dados) throw new Error(json.message);
  return json.dados;
}

export function pollImportacao(
  id: number,
  onUpdate: (s: ImportacaoStatus) => void,
  onTerminal: (s: ImportacaoStatus) => void,
  onError: (e: Error) => void,
): () => void {
  let cancelado = false;
  let timer: ReturnType<typeof setTimeout> | null = null;

  const tick = async () => {
    if (cancelado) return;
    try {
      const s = await getStatus(id);
      onUpdate(s);
      if (TERMINAL_STATES.includes(s.status)) {
        onTerminal(s);
        return;
      }
      timer = setTimeout(tick, INTERVAL_MS);
    } catch (e) {
      onError(e as Error);
    }
  };

  tick();
  return () => {
    cancelado = true;
    if (timer) clearTimeout(timer);
  };
}
```

**Uso (React):**

```tsx
function ImportacaoModal({ onClose }: { onClose: () => void }) {
  const [status, setStatus] = useState<ImportacaoStatus | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const cancelRef = useRef<(() => void) | null>(null);

  useEffect(() => () => cancelRef.current?.(), []);

  async function handleUpload(file: File) {
    setErro(null);
    try {
      const inicial = await iniciarImportacao(file);
      setStatus(inicial);
      cancelRef.current = pollImportacao(
        inicial.id,
        setStatus,
        (final) => {
          setStatus(final);
          if (final.status === 'CONCLUIDO') {
            // disparar refresh da lista de clientes
          }
        },
        (e) => setErro(e.message),
      );
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  // ...render
}
```

---

## UI / UX recomendado

### Tela de historico de importacoes

A tela principal lista as importacoes da organizacao e e o ponto de entrada da feature.

**Layout:**

- Header com botao "Nova importacao" (abre o modal — ver proxima secao).
- Tabela com colunas: `Arquivo`, `Status`, `Progresso`, `Importados / Ignorados / Total`, `Inicio`, `Fim`.
- Cada linha e clicavel: abre o detalhe (drawer/modal/rota) que faz polling.
- Linha com `status = FALHA` deve mostrar tooltip com `mensagemFalha`.
- Linha com `status = PROCESSANDO` ou `PENDENTE` pode mostrar um spinner discreto na coluna de status — sinaliza que ainda esta rodando.

**Comportamento:**

- Ao montar a tela: chamar `GET /importar-csv` uma vez. Sem polling automatico aqui.
- Botao "Atualizar" no header pra refazer o GET manualmente (opcional).
- Quando o usuario terminar uma importacao no modal de nova importacao, **recarregar a listagem** para refletir o novo registro.
- Quando o usuario fechar o detalhe de uma importacao em andamento, **recarregar a listagem** para que o status mais recente apareca na tabela.

**Renderizacao do status em badges (sugestao):**

| Status | Cor | Icone |
|--------|-----|-------|
| `PENDENTE` | cinza | clock |
| `PROCESSANDO` | azul | spinner |
| `CONCLUIDO` (sem ignorados) | verde | check |
| `CONCLUIDO` (com ignorados) | amarelo | alert-triangle |
| `FALHA` | vermelho | x-circle |

### Detalhe de uma importacao (com polling)

Aberto ao clicar em uma linha da listagem ou logo apos iniciar uma nova importacao.

**Comportamento:**

- Chama `GET /importar-csv/{id}` na montagem.
- Se `isTerminal(status)`, **nao inicia polling** — apenas renderiza.
- Senao, inicia o polling (`pollImportacao`) ate cair em estado terminal.
- Ao desmontar (fechar drawer/sair da rota), **cancela o polling** com a funcao retornada por `pollImportacao`.
- Ao terminar:
  - `CONCLUIDO`: dispara refresh da listagem de clientes (se aberta) e da listagem de importacoes.
  - `FALHA`: mantem o `mensagemFalha` em destaque.

### Modal de nova importacao em 3 estados

**1) Estado inicial (sem upload):**
- Botao "Baixar template CSV" (chama o endpoint do template).
- Drop zone "Arraste o arquivo CSV aqui ou clique para selecionar".
- Texto auxiliar: "Maximo 10 MB. Encoding UTF-8. Colunas obrigatorias: nomeCompleto, telefone, email, cpf, dataNascimento."
- Validacao client-side antes de enviar:
  - Tipo: `.csv` ou MIME `text/csv` / `application/vnd.ms-excel`.
  - Tamanho <= 10 MB (rejeitar antes de enviar para evitar trafego inutil).

**2) Estado processando:**
- Nome do arquivo + barra de progresso (`percentual`).
- Contadores ao vivo: "X de Y linhas — Z importados / W ignorados".
- Lista incremental dos primeiros N erros (collapsible). A lista cresce conforme o polling traz novos `erros`.
- Botao **"Fechar"** disponivel — o processamento continua no servidor; o usuario pode reabrir a modal vendo o mesmo `importId`.
- **Nao oferecer "Cancelar"** — backend ainda nao expoe esse endpoint.

**3) Estado terminal:**

`CONCLUIDO`:
- Header verde: "Importacao concluida".
- Resumo grande: `importados` / `ignorados` / `totalLinhas`.
- Se `ignorados > 0`: tabela com colunas `Linha` e `Motivo` (com filtro/busca para CSVs grandes). Botao "Exportar erros para CSV" e util para o usuario corrigir o arquivo.
- Botao "Importar outro arquivo" / "Fechar".

`FALHA`:
- Header vermelho: "Importacao falhou".
- `mensagemFalha` em destaque (geralmente erro de header ou parser).
- Sugestao: "Verifique se o arquivo possui o header esperado e esta em UTF-8."
- Botao "Tentar novamente" (volta para o estado 1).

### Estados de erro client-side

| Cenario | Tratamento |
|---------|------------|
| Arquivo > 10 MB | Bloquear antes do upload; toast "Arquivo excede 10 MB". |
| Extensao errada | Toast "Apenas arquivos .csv sao aceitos". |
| 401/403 no upload | Redirecionar para login. |
| 4xx/5xx no upload | Mostrar `message` da `ResponseAPI`. |
| Polling falhando intermitentemente | Ate 3 retries silenciosos. Apos isso, mostrar "Conexao instavel" mas **nao parar** — manter ultimo `status` visivel e permitir "Recarregar". |
| Tab em background | O `setTimeout` reduz cadencia automaticamente em alguns browsers. Considerar `Page Visibility API` para acelerar quando volta ao foreground. |

### Persistencia de `importId`

Salve `importId` em `localStorage` ao iniciar. Se o usuario fechar a aba e voltar, ofereca "Voltar para importacao em andamento" enquanto `status != CONCLUIDO/FALHA`. Limpe quando atingir estado terminal.

---

## Permissoes / Acesso

A importacao herda as mesmas regras do CRUD de clientes (`POST /api/v1/cliente`). O endpoint exige token JWT valido com organizacao no contexto. Restrinja o botao de importacao apenas a perfis com permissao de criar clientes (admin/gestor) — esconda no menu para clientes finais.

---

## Pontos de atencao para QA

- [ ] CSV sem BOM e CSV com BOM (Excel BR) — ambos devem funcionar.
- [ ] CSV com `,` e CSV com `;` — ambos devem funcionar (auto-deteccao).
- [ ] CSV com header em ordem trocada (`telefone;nomeCompleto;...`) — deve aceitar.
- [ ] CSV sem coluna obrigatoria — deve terminar em `FALHA` com mensagem clara.
- [ ] Linha sem nome — `ignorados++`, motivo "Nome obrigatorio", linha correta no relatorio.
- [ ] CPF com pontuacao (`123.456.789-00`) — deve aceitar.
- [ ] CPF com 10 digitos — `ignorados++`, motivo "CPF invalido".
- [ ] Telefone duplicado dentro do mesmo CSV — segundo deve ser ignorado.
- [ ] Telefone ja cadastrado no banco — deve ser ignorado.
- [ ] CSV de 10 MB — limite no limite, deve aceitar.
- [ ] CSV de 10 MB + 1 byte — deve rejeitar com 400.
- [ ] Status `CONCLUIDO` com 0 erros — UI mostra resumo verde sem tabela.
- [ ] Status `CONCLUIDO` com 100% de erros — UI nao deve quebrar (importados = 0).
- [ ] Polling em CSV grande (>500 linhas) — verificar UX da barra de progresso e crescimento da lista de erros.
- [ ] Refresh da listagem de clientes apos `CONCLUIDO` com `importados > 0`.
- [ ] Tela de historico mostra todas as importacoes da organizacao em ordem decrescente.
- [ ] Tela de historico nao faz polling automatico (sem requests repetidos no Network).
- [ ] Click em importacao `PROCESSANDO` abre detalhe e inicia polling; ao terminar, polling para.
- [ ] Click em importacao em estado terminal abre detalhe sem iniciar polling.
- [ ] Fechar o detalhe enquanto polling ativo cancela o polling (sem leaks no Network).
- [ ] Apos concluir uma nova importacao, voltar para historico mostra o novo registro.
- [ ] Importacao de outro tenant nao aparece na listagem (multitenancy).

---

## Referencias do backend

- Controller: `controller/app/ClienteController.java` (endpoints `importar-csv*`)
- Service: `service/cliente/ClienteImportacaoService.java`
- Worker async: `service/cliente/ClienteImportacaoWorker.java`
- Entity: `model/entity/importacao/ClienteImportacao.java`
- Enum: `model/entity/importacao/StatusImportacao.java`
- DTOs: `model/dto/clienteDTO/ImportacaoStatusDTO.java`, `ImportacaoResumoDTO.java`, `ImportacaoErroDTO.java`
- Migration: `db/migration/V72__cliente_importacao.sql`
- Evento de auditoria pos-importacao: `model/event/ClientesImportadosEvent.java`
