# Gerenciamento de Templates Bellory — Frontend Admin

## Contexto

O backend expõe um CRUD completo de templates globais em `/api/v1/admin/templates` (requer ROLE_SUPERADMIN ou ROLE_ADMIN). Templates são modelos de mensagens (WhatsApp e Email) usados pela plataforma. Este documento descreve tudo o que o frontend **Bellory-Admin** precisa implementar para consumir esses endpoints.

O projeto Bellory-Admin usa: **React 19 + Vite + TypeScript + Tailwind CSS + TanStack React Query + Axios + Lucide React + Framer Motion + react-hot-toast**.

Não usa shadcn/ui — os componentes UI são customizados em `components/ui/` (Button, Input, Card, Badge, etc.).

### O que são Templates?

Templates são mensagens-modelo com variáveis (`{{nome_cliente}}` para WhatsApp, `${nomeCliente}` para Email) que a plataforma usa para:
- **WhatsApp**: Confirmação e lembrete de agendamentos
- **Email**: Boas-vindas, reset de senha, cobranças

Cada combinação `tipo + categoria` pode ter **um template padrão** (flag `padrao = true`). Ao criar uma nova organização, os templates WhatsApp padrão são automaticamente copiados para a configuração de notificações da org.

### Conteúdo dos Templates de Email

Os templates de email seed (originais) são armazenados no banco apenas como referência ao nome do arquivo HTML (ex: `"cobranca-aviso"`). O **backend resolve automaticamente** essa referência e devolve o HTML completo nas APIs de GET e Preview. Ou seja:

- **GET /templates** e **GET /templates/:id** → o campo `conteudo` já vem com o HTML completo, pronto para exibir e editar
- **PUT /templates/:id** → o frontend envia o HTML editado, que é salvo direto no banco
- **POST /templates/:id/preview** → o preview já trabalha com o HTML resolvido, substituindo as variáveis Thymeleaf

Para renderizar no React:
- Use `<iframe srcDoc={conteudo} sandbox="allow-same-origin" />` — **recomendado**, isola o CSS do email do resto da aplicação
- Ou `<div dangerouslySetInnerHTML={{ __html: conteudo }} />` — mais simples, mas o CSS do email pode vazar

---

## 1. API — Endpoints Disponíveis

Base URL já configurada no axios (`services/api.ts`) termina em `/api`, então os paths abaixo são relativos a isso.

| Método | Path | Descrição | Request Body | Response Body |
|--------|------|-----------|-------------|---------------|
| `GET` | `/v1/admin/templates` | Listar ativos (com filtros opcionais) | — | `ResponseAPI<TemplateBellory[]>` |
| `GET` | `/v1/admin/templates/:id` | Buscar por ID | — | `ResponseAPI<TemplateBellory>` |
| `POST` | `/v1/admin/templates` | Criar template | `TemplateBelloryCreate` | `ResponseAPI<TemplateBellory>` |
| `PUT` | `/v1/admin/templates/:id` | Atualizar template | `TemplateBelloryUpdate` | `ResponseAPI<TemplateBellory>` |
| `DELETE` | `/v1/admin/templates/:id` | Desativar (soft delete) | — | `ResponseAPI<void>` |
| `PATCH` | `/v1/admin/templates/:id/ativar` | Reativar template | — | `ResponseAPI<TemplateBellory>` |
| `PATCH` | `/v1/admin/templates/:id/padrao` | Marcar como padrão | — | `ResponseAPI<TemplateBellory>` |
| `POST` | `/v1/admin/templates/:id/preview` | Preview com variáveis | `TemplatePreviewRequest` | `ResponseAPI<string>` |

### Query Params do GET /templates

| Param | Tipo | Descrição |
|-------|------|-----------|
| `tipo` | `WHATSAPP` \| `EMAIL` | Filtrar por tipo |
| `categoria` | `CONFIRMACAO` \| `LEMBRETE` \| `BEM_VINDO` \| `RESET_SENHA` \| `COBRANCA_AVISO` \| `COBRANCA_LEMBRETE` | Filtrar por categoria |

Exemplos:
- `GET /v1/admin/templates` — todos os ativos
- `GET /v1/admin/templates?tipo=WHATSAPP` — só WhatsApp
- `GET /v1/admin/templates?tipo=EMAIL&categoria=COBRANCA_AVISO` — emails de cobrança aviso

### Formato da Response (ResponseAPI)

Todas as respostas vêm encapsuladas neste formato:

```typescript
interface ResponseAPI<T> {
  success: boolean
  message: string
  dados: T        // <-- os dados reais ficam aqui
  errorCode?: number
  errors?: Record<string, string>
}
```

Então no service, para extrair os dados: `response.data.dados`.

---

## 2. Types — `src/types/template.ts`

```typescript
// === Tipos do Template ===
export type TipoTemplate = 'WHATSAPP' | 'EMAIL'

export type CategoriaTemplate =
  | 'CONFIRMACAO'
  | 'LEMBRETE'
  | 'BEM_VINDO'
  | 'RESET_SENHA'
  | 'COBRANCA_AVISO'
  | 'COBRANCA_LEMBRETE'

// === Variável disponível no template ===
export interface VariavelTemplate {
  nome: string        // Nome da variável (ex: "nome_cliente")
  descricao: string   // Descrição legível (ex: "Nome do cliente")
  exemplo: string     // Valor de exemplo (ex: "João Silva")
}

// === Response completa (GET lista e GET por ID) ===
export interface TemplateBellory {
  id: number
  codigo: string
  nome: string
  descricao: string | null
  tipo: TipoTemplate
  categoria: CategoriaTemplate
  assunto: string | null            // Subject do email, null para WhatsApp
  conteudo: string                  // Corpo: texto para WhatsApp, HTML/referência para Email
  variaveisDisponiveis: VariavelTemplate[]
  ativo: boolean
  padrao: boolean                   // Se é o template padrão para tipo+categoria
  icone: string | null              // Nome do ícone Lucide

  // Auditoria
  dtCriacao: string
  dtAtualizacao: string | null
  userCriacao: number | null
  userAtualizacao: number | null
}

// === Input de criação (POST) ===
export interface TemplateBelloryCreate {
  codigo: string                    // Obrigatório, único, max 50
  nome: string                      // Obrigatório, max 100
  descricao?: string
  tipo: TipoTemplate                // Obrigatório
  categoria: CategoriaTemplate      // Obrigatório
  assunto?: string                  // max 255, relevante para EMAIL
  conteudo: string                  // Obrigatório
  variaveisDisponiveis?: VariavelTemplate[]
  icone?: string                    // max 50
}

// === Input de atualização (PUT) — tudo opcional ===
export interface TemplateBelloryUpdate {
  codigo?: string
  nome?: string
  descricao?: string
  tipo?: TipoTemplate
  categoria?: CategoriaTemplate
  assunto?: string
  conteudo?: string
  variaveisDisponiveis?: VariavelTemplate[]
  icone?: string
}

// === Input para preview (POST /:id/preview) ===
export interface TemplatePreviewRequest {
  variaveis?: Record<string, string>  // Se vazio, usa os exemplos das variáveis
}
```

### Labels para exibição

```typescript
// Mapeamento de labels amigáveis para uso na UI
export const TIPO_LABELS: Record<TipoTemplate, string> = {
  WHATSAPP: 'WhatsApp',
  EMAIL: 'E-mail',
}

export const CATEGORIA_LABELS: Record<CategoriaTemplate, string> = {
  CONFIRMACAO: 'Confirmação',
  LEMBRETE: 'Lembrete',
  BEM_VINDO: 'Boas-vindas',
  RESET_SENHA: 'Reset de Senha',
  COBRANCA_AVISO: 'Cobrança - Aviso',
  COBRANCA_LEMBRETE: 'Cobrança - Lembrete',
}

// Cores para os badges de tipo
export const TIPO_COLORS: Record<TipoTemplate, { bg: string; text: string }> = {
  WHATSAPP: { bg: 'bg-green-100', text: 'text-green-700' },
  EMAIL: { bg: 'bg-blue-100', text: 'text-blue-700' },
}
```

---

## 3. Service — `src/services/templates.ts`

```typescript
import { api } from './api'
import type {
  TemplateBellory,
  TemplateBelloryCreate,
  TemplateBelloryUpdate,
  TemplatePreviewRequest,
  TipoTemplate,
  CategoriaTemplate,
} from '../types/template'

interface GetTemplatesParams {
  tipo?: TipoTemplate
  categoria?: CategoriaTemplate
}

// Listar templates (apenas ativos)
export async function getTemplates(params?: GetTemplatesParams): Promise<TemplateBellory[]> {
  const response = await api.get('/v1/admin/templates', { params })
  return response.data.dados
}

// Buscar template por ID
export async function getTemplate(id: number): Promise<TemplateBellory> {
  const response = await api.get(`/v1/admin/templates/${id}`)
  return response.data.dados
}

// Criar template
export async function criarTemplate(data: TemplateBelloryCreate): Promise<TemplateBellory> {
  const response = await api.post('/v1/admin/templates', data)
  return response.data.dados
}

// Atualizar template
export async function atualizarTemplate(id: number, data: TemplateBelloryUpdate): Promise<TemplateBellory> {
  const response = await api.put(`/v1/admin/templates/${id}`, data)
  return response.data.dados
}

// Desativar template (soft delete)
export async function desativarTemplate(id: number): Promise<void> {
  await api.delete(`/v1/admin/templates/${id}`)
}

// Ativar template
export async function ativarTemplate(id: number): Promise<TemplateBellory> {
  const response = await api.patch(`/v1/admin/templates/${id}/ativar`)
  return response.data.dados
}

// Marcar como padrão (desmarca o anterior do mesmo tipo+categoria)
export async function marcarPadrao(id: number): Promise<TemplateBellory> {
  const response = await api.patch(`/v1/admin/templates/${id}/padrao`)
  return response.data.dados
}

// Preview: substitui variáveis e retorna o template renderizado
export async function previewTemplate(id: number, data?: TemplatePreviewRequest): Promise<string> {
  const response = await api.post(`/v1/admin/templates/${id}/preview`, data || {})
  return response.data.dados
}
```

---

## 4. Query Hooks — `src/queries/useTemplates.ts`

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getTemplates,
  getTemplate,
  criarTemplate,
  atualizarTemplate,
  desativarTemplate,
  ativarTemplate,
  marcarPadrao,
  previewTemplate,
} from '../services/templates'
import type {
  TemplateBelloryCreate,
  TemplateBelloryUpdate,
  TemplatePreviewRequest,
  TipoTemplate,
  CategoriaTemplate,
} from '../types/template'
import toast from 'react-hot-toast'

const QUERY_KEY = 'admin-templates'

// === Queries ===

export function useTemplates(tipo?: TipoTemplate, categoria?: CategoriaTemplate) {
  return useQuery({
    queryKey: [QUERY_KEY, { tipo, categoria }],
    queryFn: () => getTemplates({ tipo, categoria }),
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
  })
}

export function useTemplate(id: number) {
  return useQuery({
    queryKey: [QUERY_KEY, id],
    queryFn: () => getTemplate(id),
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
    enabled: !!id,
  })
}

export function useTemplatePreview(id: number, data?: TemplatePreviewRequest) {
  return useQuery({
    queryKey: [QUERY_KEY, 'preview', id, data],
    queryFn: () => previewTemplate(id, data),
    staleTime: 0,          // Sempre busca fresco
    enabled: !!id,
    refetchOnWindowFocus: false,
  })
}

// === Mutations ===

export function useCriarTemplate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: TemplateBelloryCreate) => criarTemplate(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Template criado com sucesso')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao criar template')
    },
  })
}

export function useAtualizarTemplate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: TemplateBelloryUpdate }) => atualizarTemplate(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Template atualizado com sucesso')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar template')
    },
  })
}

export function useDesativarTemplate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => desativarTemplate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Template desativado com sucesso')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao desativar template')
    },
  })
}

export function useAtivarTemplate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => ativarTemplate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Template ativado com sucesso')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao ativar template')
    },
  })
}

export function useMarcarPadrao() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => marcarPadrao(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Template marcado como padrão')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao marcar como padrão')
    },
  })
}

export function usePreviewTemplate() {
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data?: TemplatePreviewRequest }) => previewTemplate(id, data),
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao gerar preview')
    },
  })
}
```

---

## 5. Páginas a Criar

### 5.1. `src/pages/templates/TemplatesList.tsx`

Página principal de listagem. Deve conter:

- **Cards/Tabela** com todos os templates ativos, agrupados visualmente por tipo (WhatsApp / Email)
- Cada item mostra:
  - Ícone Lucide (campo `icone`) com cor baseada no tipo (verde WhatsApp, azul Email)
  - Nome e código
  - Badge de tipo (`WHATSAPP` / `EMAIL`) com cores distintas
  - Badge de categoria (ex: "Confirmação", "Lembrete")
  - Badge "Padrão" destacado se `padrao = true` (estrela dourada ou similar)
  - Trecho do conteúdo (primeiros ~80 chars truncados)
  - Para emails: mostrar o assunto
- **Filtros**:
  - Tabs ou botões de tipo: Todos | WhatsApp | Email
  - Dropdown de categoria (Todos, Confirmação, Lembrete, Boas-vindas, Reset de Senha, Cobrança Aviso, Cobrança Lembrete)
- **Busca** por nome/código
- **Botão "Novo Template"** → navega para `/templates/novo`
- **Ações por template** (dropdown ou botões):
  - Editar → navega para `/templates/:id`
  - Preview → abre modal com preview renderizado
  - Marcar como Padrão (com confirmação: "Isso vai desmarcar o padrão atual deste tipo+categoria")
  - Desativar / Ativar (com confirmação)
- Skeleton loading durante carregamento

### 5.2. `src/pages/templates/TemplateForm.tsx`

Página de criação e edição (reutiliza o mesmo componente). Se recebe `id` via URL params, é modo edição (carrega dados com `useTemplate(id)`), senão é criação.

**Campos do formulário organizados em seções:**

**Informações Básicas:**
- `codigo` — Input texto (obrigatório na criação). Slug-like, lowercase, sem espaços. Ex: `whatsapp-confirmacao-v2`
- `nome` — Input texto (obrigatório). Ex: `Confirmação de Agendamento V2`
- `descricao` — Textarea (opcional). Descrição interna do template
- `tipo` — Select com 2 opções: WhatsApp / Email (obrigatório). **Ao trocar o tipo, ajustar os campos visíveis** (assunto só aparece para Email)
- `categoria` — Select com 6 opções (obrigatório). Mostrar labels amigáveis
- `icone` — Select/dropdown com ícones Lucide sugeridos (MessageSquare, Bell, Mail, KeyRound, Receipt, AlertTriangle, etc.)

**Conteúdo:**
- `assunto` — Input texto (só visível quando `tipo = EMAIL`). Subject do email
- `conteudo` — **Área principal do formulário**:
  - Para **WhatsApp**: Textarea grande com suporte a formatação WhatsApp (`*bold*`, `_italic_`, `~strikethrough~`). Mostrar uma referência rápida de formatação
  - Para **Email**: Textarea/Code editor para HTML. Idealmente um editor com syntax highlighting (pode usar um `<textarea>` com `font-family: monospace` como solução simples)
  - Mostrar abaixo do campo as **variáveis disponíveis** como chips/tags clicáveis que inserem a variável no cursor

**Variáveis Disponíveis (array dinâmico):**
- Lista editável de variáveis
- Cada item: `nome` (input, obrigatório), `descricao` (input), `exemplo` (input)
- Botões: "Adicionar variável", remover (ícone X)
- Variáveis comuns pré-sugeridas baseadas no tipo:
  - WhatsApp: `nome_cliente`, `data_agendamento`, `hora_agendamento`, `servico`, `profissional`, `local`, `valor`, `nome_empresa`
  - Email Cobrança: `nomeCliente`, `nomeOrganizacao`, `valorCobranca`, `dataVencimento`, `descricaoCobranca`, `numeroCobranca`, `diasAtraso`

**Ações:**
- Botão "Salvar" (chama POST ou PUT dependendo do modo)
- Botão "Cancelar" (navega de volta para `/templates`)
- Botão "Preview" (chama POST `/:id/preview` e mostra resultado — apenas no modo edição, quando já tem ID)

**Preview (painel lateral ou modal):**
- Para **WhatsApp**: renderizar o texto com formatação WhatsApp simulada (bold, italic, etc.) em um "balão" estilo WhatsApp
- Para **Email**: renderizar o HTML em um `<iframe>` ou `dangerouslySetInnerHTML` com sandbox
- Ao clicar em "Preview", substituir as variáveis pelos valores de exemplo configurados

### 5.3. `src/pages/templates/TemplatePreviewModal.tsx`

Modal/Drawer para visualização de preview. Pode ser usado tanto na lista quanto no form:

- Recebe `templateId` e opcionalmente `variaveis` customizadas
- Chama `POST /templates/:id/preview`
- Para WhatsApp: mostra em um mock de chat com balão verde
- Para Email: mostra em um iframe/container com o HTML renderizado
- Botão "Copiar conteúdo" para copiar o texto/HTML

---

## 6. Integração no Projeto Existente

### 6.1. Rotas — `src/App.tsx`

Adicionar os lazy imports e rotas dentro do bloco protegido:

```typescript
// Lazy imports (adicionar junto com os outros)
const TemplatesList = lazy(() => import('./pages/templates/TemplatesList').then(m => ({ default: m.TemplatesList })))
const TemplateForm = lazy(() => import('./pages/templates/TemplateForm').then(m => ({ default: m.TemplateForm })))

// Dentro de <Routes>, no bloco ProtectedRoute + AdminLayout:
<Route path="/templates" element={<TemplatesList />} />
<Route path="/templates/novo" element={<TemplateForm />} />
<Route path="/templates/:id" element={<TemplateForm />} />
```

### 6.2. Sidebar — `src/components/layout/Sidebar.tsx`

Adicionar item no menu principal:

```typescript
import { /* ... existentes ... */, FileText } from 'lucide-react'

// No array navItems, adicionar:
{ path: '/templates', icon: FileText, label: 'Templates' },
```

---

## 7. Dados de Exemplo (para referência visual)

### Template WhatsApp — Confirmação (padrão)

```json
{
  "id": 1,
  "codigo": "whatsapp-confirmacao",
  "nome": "Confirmação de Agendamento",
  "descricao": "Mensagem padrão de confirmação de agendamento via WhatsApp",
  "tipo": "WHATSAPP",
  "categoria": "CONFIRMACAO",
  "assunto": null,
  "conteudo": "Olá *{{nome_cliente}}*! 👋\n\n✅ Seu agendamento está *aguardando confirmação*!\n\n📋 *Detalhes do agendamento:*\n- Serviço: {{servico}}\n- Data: {{data_agendamento}}\n- Horário: {{hora_agendamento}}\n- Profissional: {{profissional}}\n- Local: {{local}}\n- Valor: {{valor}}\n\n📍 _{{nome_empresa}}_\n\nPodemos confirmar? Digite: 😊\n*Sim* para confirmar ✅\n*Não* para cancelar ❌\n*Remarcar* para reagendar o serviço 📅\n\n_Estamos aguardando o seu retorno._",
  "variaveisDisponiveis": [
    { "nome": "nome_cliente", "descricao": "Nome do cliente", "exemplo": "João Silva" },
    { "nome": "data_agendamento", "descricao": "Data do agendamento", "exemplo": "15/03/2026" },
    { "nome": "hora_agendamento", "descricao": "Horário do agendamento", "exemplo": "14:30" },
    { "nome": "servico", "descricao": "Nome do serviço", "exemplo": "Corte Masculino" },
    { "nome": "profissional", "descricao": "Nome do profissional", "exemplo": "Maria Santos" },
    { "nome": "local", "descricao": "Endereço do estabelecimento", "exemplo": "Rua das Flores, 123" },
    { "nome": "valor", "descricao": "Valor do serviço", "exemplo": "R$ 50,00" },
    { "nome": "nome_empresa", "descricao": "Nome da empresa", "exemplo": "Barbearia Top" }
  ],
  "ativo": true,
  "padrao": true,
  "icone": "MessageSquare",
  "dtCriacao": "2026-02-27T18:00:00",
  "dtAtualizacao": null,
  "userCriacao": null,
  "userAtualizacao": null
}
```

### Template Email — Cobrança Lembrete (padrão)

> **Nota:** O campo `conteudo` abaixo é retornado com o **HTML completo** (~200 linhas). Foi truncado aqui por brevidade. Na API real, virá o HTML inteiro do email, pronto para renderizar em `<iframe srcDoc={...}>`.

```json
{
  "id": 6,
  "codigo": "email-cobranca-lembrete",
  "nome": "Lembrete de Cobrança Pendente",
  "descricao": "Email de lembrete sobre cobrança pendente ou vencida",
  "tipo": "EMAIL",
  "categoria": "COBRANCA_LEMBRETE",
  "assunto": "Lembrete de Cobrança - Bellory",
  "conteudo": "<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\"><head>...HTML COMPLETO DO EMAIL...</html>",
  "variaveisDisponiveis": [
    { "nome": "nomeCliente", "descricao": "Nome do cliente", "exemplo": "João Silva" },
    { "nome": "nomeOrganizacao", "descricao": "Nome da organização", "exemplo": "Barbearia Top" },
    { "nome": "valorCobranca", "descricao": "Valor da cobrança", "exemplo": "R$ 150,00" },
    { "nome": "dataVencimento", "descricao": "Data de vencimento", "exemplo": "15/04/2026" },
    { "nome": "descricaoCobranca", "descricao": "Descrição da cobrança", "exemplo": "Plano Premium - Mensal" },
    { "nome": "numeroCobranca", "descricao": "Número da cobrança", "exemplo": "COB-2026-001" },
    { "nome": "diasAtraso", "descricao": "Dias em atraso", "exemplo": "5" }
  ],
  "ativo": true,
  "padrao": true,
  "icone": "AlertTriangle",
  "dtCriacao": "2026-02-27T18:00:00",
  "dtAtualizacao": null,
  "userCriacao": null,
  "userAtualizacao": null
}
```

### Response do Preview (POST /:id/preview)

Para WhatsApp (retorna texto com variáveis substituídas):

```json
{
  "success": true,
  "message": "Preview gerado com sucesso",
  "dados": "Olá *João Silva*! 👋\n\n✅ Seu agendamento está *aguardando confirmação*!\n\n📋 *Detalhes do agendamento:*\n- Serviço: Corte Masculino\n- Data: 15/03/2026\n- Horário: 14:30\n- Profissional: Maria Santos\n- Local: Rua das Flores, 123\n- Valor: R$ 50,00\n\n📍 _Barbearia Top_\n\nPodemos confirmar? Digite: 😊\n*Sim* para confirmar ✅\n*Não* para cancelar ❌\n*Remarcar* para reagendar o serviço 📅\n\n_Estamos aguardando o seu retorno._"
}
```

---

## 8. Componentes Específicos Sugeridos

### 8.1. `WhatsAppPreview` — Mock de chat WhatsApp

Componente reutilizável que renderiza texto com formatação WhatsApp:

```typescript
interface WhatsAppPreviewProps {
  text: string
  senderName?: string  // Nome da empresa no topo do balão
}
```

**Regras de formatação WhatsApp:**
- `*texto*` → **bold**
- `_texto_` → *italic*
- `~texto~` → ~~strikethrough~~
- `\n` → quebra de linha
- Emojis renderizados normalmente

**Aparência:** Fundo cinza claro (#e5ddd5), balão verde claro (#dcf8c6) alinhado à direita, fonte system, timestamp "agora" no canto inferior direito do balão.

### 8.2. `EmailPreview` — Visualizador de HTML

```typescript
interface EmailPreviewProps {
  html: string
}
```

Renderizar o HTML dentro de um `<iframe srcDoc={html}>` com `sandbox` para segurança, ou dentro de um container com `dangerouslySetInnerHTML`. Usar iframe é mais seguro e isola o CSS.

### 8.3. `VariaveisEditor` — Editor de variáveis

```typescript
interface VariaveisEditorProps {
  value: VariavelTemplate[]
  onChange: (vars: VariavelTemplate[]) => void
  sugestoes?: VariavelTemplate[]  // Variáveis pré-sugeridas para o tipo selecionado
}
```

- Tabela editável com 3 colunas: Nome, Descrição, Exemplo
- Botão "Adicionar variável"
- Botão "Carregar sugestões" que preenche com as variáveis padrão do tipo
- Ícone X para remover cada linha

### 8.4. `VariavelChip` — Chip clicável de variável

Componente que mostra uma variável como tag/chip e ao clicar insere no textarea:

```typescript
interface VariavelChipProps {
  variavel: VariavelTemplate
  tipoTemplate: TipoTemplate
  onClick: (placeholder: string) => void
}
```

- WhatsApp: insere `{{nome_variavel}}`
- Email: insere `${nomeVariavel}`
- Tooltip com descrição e exemplo ao hover

---

## 9. Resumo — Arquivos a Criar

| Arquivo | Tipo |
|---------|------|
| `src/types/template.ts` | TypeScript interfaces + labels/cores |
| `src/services/templates.ts` | Funções de chamada à API |
| `src/queries/useTemplates.ts` | React Query hooks (queries + mutations) |
| `src/pages/templates/TemplatesList.tsx` | Página de listagem com filtros |
| `src/pages/templates/TemplateForm.tsx` | Página de criação/edição com preview |
| `src/pages/templates/TemplatePreviewModal.tsx` | Modal de preview (WhatsApp/Email) |
| `src/components/templates/WhatsAppPreview.tsx` | Mock de chat WhatsApp |
| `src/components/templates/EmailPreview.tsx` | Visualizador de HTML de email |
| `src/components/templates/VariaveisEditor.tsx` | Editor de variáveis (tabela editável) |
| `src/components/templates/VariavelChip.tsx` | Chip clicável para inserir variável |

## Arquivos a Modificar

| Arquivo | O que alterar |
|---------|---------------|
| `src/App.tsx` | Adicionar lazy imports + 3 rotas (`/templates`, `/templates/novo`, `/templates/:id`) |
| `src/components/layout/Sidebar.tsx` | Adicionar item "Templates" com ícone `FileText` |

---

## 10. Regras de Negócio Importantes

1. **Código é único** — a API retorna erro se tentar criar/atualizar com código duplicado
2. **Um padrão por tipo+categoria** — ao marcar como padrão, o anterior é automaticamente desmarcado pelo backend. O frontend deve exibir confirmação: *"Isso vai desmarcar o template padrão atual para [tipo] - [categoria]. Continuar?"*
3. **Não pode marcar inativo como padrão** — a API retorna erro. Desabilitar o botão "Marcar como padrão" se `ativo = false`
4. **Variáveis WhatsApp vs Email** — WhatsApp usa `{{variavel}}`, Email usa `${variavel}` (sintaxe Thymeleaf). O preview trata ambos os formatos
5. **Conteúdo de Email** — O backend resolve automaticamente referências a arquivos HTML do classpath. O `conteudo` retornado pela API **sempre contém o HTML completo**, pronto para exibir e editar. Ao salvar via PUT, o HTML editado é persistido diretamente no banco
6. **Preview sem variáveis** — Se o body do preview vier sem `variaveis` (ou vazio), o backend usa automaticamente os valores de `exemplo` das variáveis disponíveis
7. **Campo assunto** — Só relevante para tipo EMAIL. Esconder/desabilitar quando tipo = WHATSAPP
8. **Templates seed não devem ser deletados** — Os 6 templates iniciais (seed da migration) são essenciais. O frontend pode mostrar um aviso visual se o template for um dos códigos padrão (`whatsapp-confirmacao`, `whatsapp-lembrete`, `email-bem-vindo`, `email-reset-senha`, `email-cobranca-aviso`, `email-cobranca-lembrete`)

---

## 11. UX/UI Sugerida

### Lista — Layout em Cards

Cada card do template:

```
┌─────────────────────────────────────────────────┐
│  [MessageSquare]  Confirmação de Agendamento    │
│                                                 │
│  [WHATSAPP]  [CONFIRMAÇÃO]  [⭐ PADRÃO]         │
│                                                 │
│  whatsapp-confirmacao                           │
│                                                 │
│  "Olá *{{nome_cliente}}*! 👋 Seu agendamento..."│
│                                                 │
│  8 variáveis  ·  Criado em 27/02/2026           │
│                                                 │
│  [Editar]  [Preview]  [⋮ Mais]                  │
└─────────────────────────────────────────────────┘
```

### Form — Layout em 2 colunas

```
┌──────────────────────────┬──────────────────────┐
│  FORMULÁRIO              │  PREVIEW AO VIVO     │
│                          │                      │
│  Informações Básicas     │  ┌──────────────┐    │
│  [codigo] [nome]         │  │  Balão WhatsApp│   │
│  [tipo ▼] [categoria ▼]  │  │  ou            │   │
│  [icone ▼] [assunto]     │  │  Email HTML    │   │
│                          │  └──────────────┘    │
│  Conteúdo                │                      │
│  ┌────────────────────┐  │  Variáveis:          │
│  │ Textarea/Editor    │  │  [nome_cliente]      │
│  │                    │  │  [data_agendamento]  │
│  │                    │  │  [hora_agendamento]  │
│  └────────────────────┘  │  ...                 │
│                          │                      │
│  Variáveis Disponíveis   │                      │
│  [nome] [desc] [exemplo] │                      │
│  [nome] [desc] [exemplo] │                      │
│  [+ Adicionar]           │                      │
│                          │                      │
│  [Cancelar] [Salvar]     │                      │
└──────────────────────────┴──────────────────────┘
```

### WhatsApp Preview — Mock

```
┌──────────────────────────────────┐
│  ← Barbearia Top        ⋮       │  (header cinza escuro)
│─────────────────────────────────│
│                                  │  (fundo #e5ddd5)
│                                  │
│        ┌─────────────────────┐   │
│        │ Olá *João Silva*! 👋│   │  (balão #dcf8c6)
│        │                     │   │
│        │ ✅ Seu agendamento  │   │
│        │ está *aguardando    │   │
│        │ confirmação*!       │   │
│        │                     │   │
│        │ 📋 *Detalhes:*      │   │
│        │ - Serviço: Corte    │   │
│        │ - Data: 15/03/2026  │   │
│        │ - Horário: 14:30    │   │
│        │ ...                 │   │
│        │            18:30 ✓✓ │   │
│        └─────────────────────┘   │
│                                  │
│  [📎] [Mensagem...        ] [🎤]│  (footer)
└──────────────────────────────────┘
```
