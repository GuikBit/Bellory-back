# Modulo de Arquivos - Guia de Integracao Frontend

## Visao Geral

Modulo de gerenciamento de arquivos para organizacoes. O file explorer mostra **todas as pastas** da organizacao:

| Pasta | Tipo | Pode deletar? | Pode renomear? | Pode fazer upload? | Conteudo |
|-------|------|---------------|----------------|--------------------| ---------|
| **Colaboradores** | Sistema | Nao | Nao | Nao | Fotos de perfil dos funcionarios |
| **Servicos** | Sistema | Nao | Nao | Nao | Imagens dos servicos |
| **Produtos** | Sistema | Nao | Nao | Nao | Imagens dos produtos |
| **Organizacao** | Sistema | Nao | Nao | Nao | Logo e banner (subpastas logo/, banner/) |
| **Arquivos** | Usuario | Nao (raiz) | Nao (raiz) | **Sim** | Arquivos e pastas criados pelo usuario |

> A pasta **Clientes** existe no disco mas **nao aparece** no modulo.

**Storage**: O calculo de uso de armazenamento considera **TODOS** os arquivos da organizacao (sistema + usuario), exceto clientes.

**Base URL:** `/api/v1/arquivos`
**Autenticacao:** JWT obrigatorio (Bearer token)
**Multitenancy:** Organizacao identificada automaticamente pelo token

---

## Conceito: campo `sistema`

Todos os DTOs (PastaArquivoDTO e ArquivoDTO) possuem o campo `sistema: boolean`:

- **`sistema: true`** = pasta/arquivo do sistema. **Somente leitura**. O frontend deve:
  - Esconder botoes de deletar, renomear, mover
  - Permitir apenas visualizacao/download
  - Usar icone diferenciado (ex: cadeado ou badge "Sistema")

- **`sistema: false`** = pasta/arquivo do usuario. CRUD completo permitido.

---

## Endpoints

### 1. NAVEGACAO (File Explorer)

A navegacao tem **3 endpoints** dependendo do tipo de pasta:

#### 1a. Raiz - `GET /api/v1/arquivos/navegar`

Retorna a raiz com todas as pastas (sistema + arquivos do usuario).

**Response:**
```json
{
  "success": true,
  "dados": {
    "id": null,
    "nome": "Raiz",
    "caminhoCompleto": "",
    "pastaPaiId": null,
    "totalArquivos": 0,
    "tamanhoTotal": 268435456,
    "tamanhoTotalFormatado": "256.0 MB",
    "sistema": false,
    "subpastas": [
      {
        "nome": "Colaboradores",
        "caminhoCompleto": "colaboradores",
        "totalArquivos": 8,
        "tamanhoTotal": 4194304,
        "tamanhoTotalFormatado": "4.0 MB",
        "sistema": true
      },
      {
        "nome": "Servicos",
        "caminhoCompleto": "servicos",
        "totalArquivos": 15,
        "tamanhoTotal": 10485760,
        "tamanhoTotalFormatado": "10.0 MB",
        "sistema": true
      },
      {
        "nome": "Produtos",
        "caminhoCompleto": "produtos",
        "totalArquivos": 3,
        "tamanhoTotal": 2097152,
        "tamanhoTotalFormatado": "2.0 MB",
        "sistema": true
      },
      {
        "nome": "Organizacao",
        "caminhoCompleto": "organizacao",
        "totalArquivos": 2,
        "tamanhoTotal": 1048576,
        "tamanhoTotalFormatado": "1.0 MB",
        "sistema": true
      },
      {
        "nome": "Arquivos",
        "caminhoCompleto": "arquivos",
        "totalArquivos": 45,
        "tamanhoTotal": 157286400,
        "tamanhoTotalFormatado": "150.0 MB",
        "sistema": false
      }
    ],
    "arquivos": []
  }
}
```

#### 1b. Pasta do sistema - `GET /api/v1/arquivos/navegar/sistema/{nomePasta}`

Navega dentro de uma pasta do sistema. Nomes validos: `colaboradores`, `servicos`, `produtos`, `organizacao`, `arquivos`.

**IMPORTANTE:** Ao clicar em "Arquivos", este endpoint retorna as pastas do usuario (do banco) + arquivos na raiz da pasta arquivos.

```
GET /api/v1/arquivos/navegar/sistema/colaboradores
GET /api/v1/arquivos/navegar/sistema/organizacao
GET /api/v1/arquivos/navegar/sistema/arquivos   <-- entra no modulo do usuario
```

**Response (ex: organizacao):**
```json
{
  "success": true,
  "dados": {
    "id": null,
    "nome": "Organizacao",
    "caminhoCompleto": "organizacao",
    "sistema": true,
    "subpastas": [
      { "nome": "logo", "caminhoCompleto": "organizacao/logo", "totalArquivos": 1, "sistema": true },
      { "nome": "banner", "caminhoCompleto": "organizacao/banner", "totalArquivos": 1, "sistema": true }
    ],
    "arquivos": []
  }
}
```

#### 1c. Subpasta do sistema - `GET /api/v1/arquivos/navegar/sistema/organizacao/logo`

Navega em subpastas de pastas do sistema (ex: `organizacao/logo`, `organizacao/banner`).

```
GET /api/v1/arquivos/navegar/sistema/organizacao/logo
GET /api/v1/arquivos/navegar/sistema/organizacao/banner
```

#### 1d. Pasta do usuario (modulo) - `GET /api/v1/arquivos/navegar?pastaId={id}`

Navega em pastas criadas pelo usuario dentro de "Arquivos". Usa o ID da pasta (do banco de dados).

```
GET /api/v1/arquivos/navegar?pastaId=3
```

---

### Fluxo de Navegacao Completo

```
RAIZ (GET /navegar)
  ├── Colaboradores (sistema=true)
  │     └── click -> GET /navegar/sistema/colaboradores
  │         └── lista fotos de perfil (somente leitura)
  │
  ├── Servicos (sistema=true)
  │     └── click -> GET /navegar/sistema/servicos
  │
  ├── Produtos (sistema=true)
  │     └── click -> GET /navegar/sistema/produtos
  │
  ├── Organizacao (sistema=true)
  │     └── click -> GET /navegar/sistema/organizacao
  │         ├── logo/ -> GET /navegar/sistema/organizacao/logo
  │         └── banner/ -> GET /navegar/sistema/organizacao/banner
  │
  └── Arquivos (sistema=false)  <-- UNICA pasta com CRUD
        └── click -> GET /navegar/sistema/arquivos
            ├── Contratos (id=1) -> GET /navegar?pastaId=1
            │     └── 2026 (id=4) -> GET /navegar?pastaId=4
            ├── Fotos Eventos (id=2) -> GET /navegar?pastaId=2
            └── relatorio.pdf, planilha.xlsx (arquivos na raiz)
```

**Logica do frontend para decidir qual endpoint usar ao clicar em uma pasta:**

```javascript
function onFolderClick(pasta) {
  if (pasta.id !== null) {
    // Pasta do banco (usuario) - usar pastaId
    navigate(`/navegar?pastaId=${pasta.id}`);
  } else {
    // Pasta do sistema ou raiz - usar caminho
    navigate(`/navegar/sistema/${pasta.caminhoCompleto}`);
  }
}

function onBackClick(pastaAtual) {
  if (pastaAtual.pastaPaiId !== null) {
    navigate(`/navegar?pastaId=${pastaAtual.pastaPaiId}`);
  } else if (pastaAtual.caminhoCompleto && pastaAtual.caminhoCompleto.includes('/')) {
    // Subpasta de sistema (ex: organizacao/logo -> voltar para organizacao)
    const parentPath = pastaAtual.caminhoCompleto.split('/').slice(0, -1).join('/');
    navigate(`/navegar/sistema/${parentPath}`);
  } else if (pastaAtual.caminhoCompleto) {
    // Pasta raiz do sistema -> voltar para raiz
    navigate('/navegar');
  }
}
```

---

### 2. UPLOAD DE ARQUIVOS

#### `POST /api/v1/arquivos/upload`

Upload de um ou mais arquivos. **So funciona para pastas do modulo** (dentro de "Arquivos").

**Content-Type:** `multipart/form-data`

**Form Params:**
| Param | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| files | MultipartFile[] | Sim | Um ou mais arquivos |
| pastaId | Long | Nao | ID da pasta destino (null = raiz da pasta Arquivos) |

**Limites:**
- Tamanho maximo por arquivo: **20MB**
- Tamanho maximo por request: **100MB**
- Storage total: depende do plano (inclui TODOS os arquivos da org)

**Formatos permitidos:**
- Imagens: jpg, jpeg, png, gif, webp, svg, bmp, ico
- Documentos: pdf, doc, docx, xls, xlsx, ppt, pptx, odt, ods, odp
- Texto: txt, csv, rtf, md
- Compactados: zip, rar, 7z, tar, gz
- Outros: json, xml

**Exemplo (JavaScript/fetch):**
```javascript
const formData = new FormData();
files.forEach(file => formData.append('files', file));
if (pastaId) formData.append('pastaId', pastaId);

const response = await fetch('/api/v1/arquivos/upload', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: formData
});
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "3 arquivo(s) enviado(s) com sucesso.",
  "dados": [
    {
      "id": 15,
      "nomeOriginal": "contrato-cliente.pdf",
      "extensao": "pdf",
      "contentType": "application/pdf",
      "tamanho": 2097152,
      "tamanhoFormatado": "2.0 MB",
      "url": "https://api-dev.bellory.com.br/uploads/1/arquivos/contratos/1_1711324800000.pdf",
      "pastaId": 1,
      "pastaNome": "contratos",
      "sistema": false
    }
  ]
}
```

**Erros:**
| Mensagem | Causa |
|----------|-------|
| "Arquivo muito grande. Maximo: 20MB." | Arquivo excede 20MB |
| "Formato nao permitido: .exe" | Extensao bloqueada |
| "Limite de armazenamento atingido. Uso atual: 480.0 MB / Limite: 500.0 MB" | Plano lotado |

---

### 3. GERENCIAMENTO DE ARQUIVOS (somente modulo do usuario)

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/arquivos?pastaId={id}` | GET | Lista arquivos de uma pasta |
| `/api/v1/arquivos/todos` | GET | Lista TODOS os arquivos do modulo |
| `/api/v1/arquivos/{id}` | DELETE | Deleta arquivo |
| `/api/v1/arquivos/{id}/mover` | PUT | Move arquivo para outra pasta |
| `/api/v1/arquivos/{id}/renomear` | PUT | Renomeia arquivo |

**Mover - Body:**
```json
{ "pastaId": 2 }        // para uma pasta
{ "pastaId": null }      // para a raiz
```

**Renomear - Body:**
```json
{ "nome": "novo-nome" }  // extensao original e mantida
```

---

### 4. GERENCIAMENTO DE PASTAS (somente modulo do usuario)

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/arquivos/pastas` | POST | Cria pasta |
| `/api/v1/arquivos/pastas?pastaPaiId={id}` | GET | Lista subpastas |
| `/api/v1/arquivos/pastas/{id}` | GET | Detalhes da pasta |
| `/api/v1/arquivos/pastas/{id}/renomear` | PUT | Renomeia pasta |
| `/api/v1/arquivos/pastas/{id}` | DELETE | Deleta pasta + conteudo |

**Criar pasta - Body:**
```json
{
  "nome": "Contratos 2026",
  "pastaPaiId": null
}
```

> Nomes reservados (`colaboradores`, `servicos`, `produtos`, `organizacao`, `arquivos`) sao bloqueados na criacao.

---

### 5. USO DE STORAGE

#### `GET /api/v1/arquivos/storage`

Retorna uso de armazenamento **total** da organizacao (sistema + usuario).

```json
{
  "success": true,
  "dados": {
    "organizacaoId": 1,
    "totalBytes": 268435456,
    "totalFormatado": "256.0 MB",
    "totalArquivos": 73,
    "totalPastas": 12,
    "limiteMb": 500,
    "limiteFormatado": "500.0 MB",
    "percentualUsado": 51.2,
    "limiteAtingido": false
  }
}
```

---

## Componentes Sugeridos para o Frontend

### 1. FileExplorer (pagina principal)
- Breadcrumb: Raiz > Organizacao > logo
- Grid/Lista de pastas e arquivos
- **Condicional por `sistema`:**
  - `sistema: true` -> icone com badge/cadeado, sem botoes de acao
  - `sistema: false` -> botoes de Renomear, Mover, Deletar
- Barra de storage no topo
- Botoes "Nova Pasta" e "Upload" **somente visivel dentro de "Arquivos"**

### 2. UploadDropzone
- Drag & drop zone (so ativa quando dentro de pasta do usuario)
- Selecao multipla
- Progress bar por arquivo
- Validacao client-side antes de enviar

### 3. StorageBar
- Barra de progresso visual
- "256 MB de 500 MB usados (51.2%)"
- Cor: verde < 80%, amarelo 80-95%, vermelho > 95%
- Se `limiteAtingido: true` -> mensagem de alerta + desabilitar upload

### 4. FolderCard / FileCard
- Icone baseado na extensao
- Badge "Sistema" para `sistema: true`
- Context menu adaptativo:
  - Sistema: apenas "Abrir", "Download"
  - Usuario: "Abrir", "Download", "Renomear", "Mover", "Deletar"

### 5. MoveDialog
- Modal com arvore de pastas do usuario (apenas pastas com `sistema: false`)
- Opcao "Mover para raiz (Arquivos)"

---

## Mapeamento de Icones

```javascript
const folderIcons = {
  'colaboradores': 'users',        // ou People
  'servicos': 'scissors',          // ou Briefcase
  'produtos': 'shopping-bag',      // ou Package
  'organizacao': 'building',       // ou Building2
  'arquivos': 'folder',            // pasta normal
};

const fileIcons = {
  'jpg': 'image', 'jpeg': 'image', 'png': 'image', 'gif': 'image',
  'webp': 'image', 'svg': 'image', 'bmp': 'image', 'ico': 'image',
  'pdf': 'file-pdf',
  'doc': 'file-word', 'docx': 'file-word', 'odt': 'file-word', 'rtf': 'file-word',
  'xls': 'file-excel', 'xlsx': 'file-excel', 'ods': 'file-excel', 'csv': 'file-excel',
  'ppt': 'file-powerpoint', 'pptx': 'file-powerpoint', 'odp': 'file-powerpoint',
  'txt': 'file-text', 'md': 'file-text',
  'zip': 'file-archive', 'rar': 'file-archive', '7z': 'file-archive',
  'json': 'file-code', 'xml': 'file-code',
  'default': 'file'
};
```

---

## Modelo de Dados (DTOs)

### ArquivoDTO
| Campo | Tipo | Descricao |
|-------|------|-----------|
| id | Long | ID do arquivo (null para arquivos do sistema) |
| nomeOriginal | String | Nome do arquivo |
| extensao | String | Extensao (pdf, jpg, etc) |
| contentType | String | MIME type |
| tamanho | Long | Tamanho em bytes |
| tamanhoFormatado | String | "2.5 MB" |
| url | String | URL para download/visualizacao |
| caminhoRelativo | String | Caminho no storage |
| pastaId | Long | ID da pasta (null se raiz) |
| pastaNome | String | Nome da pasta |
| dtCriacao | DateTime | Data de upload |
| criadoPor | Long | ID do usuario |
| **sistema** | **Boolean** | **true = somente leitura** |

### PastaArquivoDTO
| Campo | Tipo | Descricao |
|-------|------|-----------|
| id | Long | ID da pasta (null para pastas do sistema) |
| nome | String | Nome de exibicao |
| caminhoCompleto | String | Caminho (ex: "organizacao/logo") |
| pastaPaiId | Long | ID da pasta pai |
| totalArquivos | Integer | Qtd de arquivos |
| tamanhoTotal | Long | Tamanho em bytes |
| tamanhoTotalFormatado | String | "15.0 MB" |
| subpastas | PastaArquivoDTO[] | Subpastas |
| arquivos | ArquivoDTO[] | Arquivos |
| dtCriacao | DateTime | Data de criacao |
| dtAtualizacao | DateTime | Data de atualizacao |
| **sistema** | **Boolean** | **true = nao pode deletar/renomear** |

### StorageUsageDTO
| Campo | Tipo | Descricao |
|-------|------|-----------|
| organizacaoId | Long | ID da organizacao |
| totalBytes | Long | Total usado (TUDO da org, exceto clientes) |
| totalFormatado | String | "256.0 MB" |
| totalArquivos | Integer | Total de arquivos |
| totalPastas | Integer | Total de pastas (sistema + usuario) |
| limiteMb | Long | Limite do plano em MB (null = sem limite) |
| limiteFormatado | String | "500.0 MB" ou "Ilimitado" |
| percentualUsado | Double | 51.2 (= 51.2%) |
| limiteAtingido | Boolean | true se atingiu o limite |

---

## Tratamento de Erros

```json
{ "success": false, "message": "Mensagem descritiva do erro" }
```

| HTTP Status | Quando |
|-------------|--------|
| 400 | Validacao (arquivo grande, formato invalido, limite atingido, nome duplicado, nome reservado) |
| 401 | Token JWT ausente ou expirado |
| 404 | Arquivo/pasta nao encontrada |
| 500 | Erro interno |
