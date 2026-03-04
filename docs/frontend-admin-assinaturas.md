# Gerenciamento de Assinaturas & Billing — Frontend Admin

## Contexto

O backend expõe endpoints de gestão de assinaturas e cobrança da plataforma em `/api/v1/admin/assinaturas` (requer ROLE_SUPERADMIN ou ROLE_ADMIN). Além disso, o login (`/api/v1/auth/login`) agora retorna o campo `organizacao.assinatura` com o status da assinatura, e há endpoints de self-service em `/api/v1/assinatura` para as organizações gerenciarem seu próprio plano.

O projeto Bellory-Admin usa: **React 19 + Vite + TypeScript + Tailwind CSS + TanStack React Query + Axios + Lucide React + Framer Motion + react-hot-toast**.

Não usa shadcn/ui — os componentes UI são customizados em `components/ui/` (Button, Input, Card, Badge, etc.).

### O que é este módulo?

A Bellory cobra assinaturas das organizações (salões) pelo uso da plataforma. O gateway de pagamento é o **Assas**. Organizações novas ganham **14 dias de trial gratuito**. Após o trial, sem assinatura ativa, o sistema bloqueia as operações (frontend redireciona para tela de planos).

**IMPORTANTE**: As entidades Cobranca/Pagamento existentes (schema `app`) são para os salões cobrarem **seus** clientes. Este módulo é para a **Bellory cobrar os salões** — as tabelas ficam no schema `admin`.

---

## Parte 1 — Painel Administrativo (Admin)

### 1.1. API — Endpoints Admin

Base URL configurada no axios (`services/api.ts`) termina em `/api`, então os paths abaixo são relativos a isso.

| Método | Path | Descrição | Request Body | Response Body |
|--------|------|-----------|-------------|---------------|
| `GET` | `/v1/admin/assinaturas` | Listar todas as assinaturas | — | `ResponseAPI<AssinaturaResponse[]>` |
| `GET` | `/v1/admin/assinaturas/dashboard` | Métricas de billing | — | `ResponseAPI<BillingDashboard>` |
| `GET` | `/v1/admin/assinaturas/:id` | Detalhe de uma assinatura por ID | — | `ResponseAPI<AssinaturaResponse>` |
| `GET` | `/v1/admin/assinaturas/:id/cobrancas` | Cobranças de uma assinatura | — | `ResponseAPI<CobrancaPlataforma[]>` |
| `GET` | `/v1/admin/assinaturas/organizacao/:orgId` | Assinatura de uma organização | — | `ResponseAPI<AssinaturaResponse>` |
| `GET` | `/v1/admin/assinaturas/organizacao/:orgId/cobrancas` | Cobranças de uma organização | — | `ResponseAPI<CobrancaPlataforma[]>` |
| `GET` | `/v1/admin/assinaturas/organizacao/:orgId/pagamentos` | Pagamentos de uma organização | — | `ResponseAPI<PagamentoPlataforma[]>` |
| `GET` | `/v1/admin/assinaturas/cobrancas/:cobrancaId/pagamentos` | Pagamentos de uma cobrança | — | `ResponseAPI<PagamentoPlataforma[]>` |
| `POST` | `/v1/admin/assinaturas/:id/cancelar` | Cancelar assinatura | — | `ResponseAPI<AssinaturaResponse>` |
| `POST` | `/v1/admin/assinaturas/:id/suspender` | Suspender assinatura | — | `ResponseAPI<AssinaturaResponse>` |
| `POST` | `/v1/admin/assinaturas/:id/reativar` | Reativar assinatura | — | `ResponseAPI<AssinaturaResponse>` |

#### Query Params do GET /admin/assinaturas

| Param | Tipo | Descrição |
|-------|------|-----------|
| `status` | `TRIAL` \| `ATIVA` \| `VENCIDA` \| `CANCELADA` \| `SUSPENSA` | Filtrar por status |
| `planoCodigo` | `string` | Filtrar por código do plano |

Exemplos:
- `GET /v1/admin/assinaturas` — todas
- `GET /v1/admin/assinaturas?status=TRIAL` — somente trials
- `GET /v1/admin/assinaturas?status=VENCIDA` — somente vencidas

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

## 1.2. Types — `src/types/assinatura.ts`

```typescript
// === Status da Assinatura ===
export type StatusAssinatura = 'TRIAL' | 'ATIVA' | 'VENCIDA' | 'CANCELADA' | 'SUSPENSA'

// === Ciclo de Cobrança ===
export type CicloCobranca = 'MENSAL' | 'ANUAL'

// === Status da Cobrança ===
export type StatusCobrancaPlataforma = 'PENDENTE' | 'PAGA' | 'VENCIDA' | 'CANCELADA' | 'ESTORNADA'

// === Forma de Pagamento ===
export type FormaPagamentoPlataforma = 'PIX' | 'BOLETO' | 'CARTAO_CREDITO'

// === Response completa da Assinatura (Admin) ===
export interface AssinaturaResponse {
  id: number
  organizacaoId: number
  organizacaoNome: string
  planoBelloryId: number
  planoNome: string
  planoCodigo: string
  status: StatusAssinatura
  cicloCobranca: CicloCobranca
  dtInicioTrial: string | null
  dtFimTrial: string | null
  dtInicio: string | null
  dtProximoVencimento: string | null
  dtCancelamento: string | null
  valorMensal: number | null
  valorAnual: number | null
  assasCustomerId: string | null
  assasSubscriptionId: string | null
  dtCriacao: string
}

// === Status leve retornado no login ===
export interface AssinaturaStatus {
  bloqueado: boolean
  statusAssinatura: string
  diasRestantesTrial: number | null
  mensagem: string
  planoCodigo: string | null
  planoNome: string | null
}

// === Cobrança da Plataforma ===
export interface CobrancaPlataforma {
  id: number
  valor: number
  dtVencimento: string           // formato: "2026-03-10"
  dtPagamento: string | null
  status: StatusCobrancaPlataforma
  formaPagamento: FormaPagamentoPlataforma | null
  assasInvoiceUrl: string | null
  assasBankSlipUrl: string | null
  assasPixQrCode: string | null
  assasPixCopiaCola: string | null
  referenciaMes: number
  referenciaAno: number
  dtCriacao: string
}

// === Pagamento da Plataforma ===
export interface PagamentoPlataforma {
  id: number
  cobrancaId: number
  valor: number
  status: 'PENDENTE' | 'CONFIRMADO' | 'RECUSADO' | 'ESTORNADO'
  formaPagamento: FormaPagamentoPlataforma
  assasPaymentId: string | null
  dtPagamento: string | null
  dtCriacao: string
}

// === Input para escolher plano (self-service org) ===
export interface EscolherPlano {
  planoCodigo: string
  cicloCobranca: CicloCobranca
  formaPagamento: FormaPagamentoPlataforma
}

// === Dashboard de Billing (Admin) ===
export interface BillingDashboard {
  mrr: number
  totalAtivas: number
  totalTrial: number
  totalVencidas: number
  totalCanceladas: number
  totalSuspensas: number
  receitaMesAtual: number
}

// === Filtros Admin ===
export interface AssinaturaFiltro {
  status?: StatusAssinatura
  planoCodigo?: string
}
```

---

## 1.3. Service — `src/services/assinaturas.ts`

```typescript
import { api } from './api'
import type {
  AssinaturaResponse,
  CobrancaPlataforma,
  PagamentoPlataforma,
  BillingDashboard,
  AssinaturaFiltro,
} from '../types/assinatura'

// ==================== LISTAGEM GERAL ====================

// Listar todas as assinaturas (com filtros opcionais)
export async function getAssinaturas(filtro?: AssinaturaFiltro): Promise<AssinaturaResponse[]> {
  const params: Record<string, string> = {}
  if (filtro?.status) params.status = filtro.status
  if (filtro?.planoCodigo) params.planoCodigo = filtro.planoCodigo

  const response = await api.get('/v1/admin/assinaturas', { params })
  return response.data.dados
}

// Dashboard de billing
export async function getBillingDashboard(): Promise<BillingDashboard> {
  const response = await api.get('/v1/admin/assinaturas/dashboard')
  return response.data.dados
}

// ==================== POR ASSINATURA ID ====================

// Detalhe de uma assinatura por ID
export async function getAssinatura(id: number): Promise<AssinaturaResponse> {
  const response = await api.get(`/v1/admin/assinaturas/${id}`)
  return response.data.dados
}

// Cobrancas de uma assinatura
export async function getCobrancasAssinatura(id: number): Promise<CobrancaPlataforma[]> {
  const response = await api.get(`/v1/admin/assinaturas/${id}/cobrancas`)
  return response.data.dados
}

// ==================== POR ORGANIZACAO ID ====================

// Buscar assinatura de uma organizacao
export async function getAssinaturaByOrganizacao(orgId: number): Promise<AssinaturaResponse> {
  const response = await api.get(`/v1/admin/assinaturas/organizacao/${orgId}`)
  return response.data.dados
}

// Cobrancas de uma organizacao
export async function getCobrancasOrganizacao(orgId: number): Promise<CobrancaPlataforma[]> {
  const response = await api.get(`/v1/admin/assinaturas/organizacao/${orgId}/cobrancas`)
  return response.data.dados
}

// Pagamentos de uma organizacao (historico completo)
export async function getPagamentosOrganizacao(orgId: number): Promise<PagamentoPlataforma[]> {
  const response = await api.get(`/v1/admin/assinaturas/organizacao/${orgId}/pagamentos`)
  return response.data.dados
}

// ==================== PAGAMENTOS POR COBRANCA ====================

// Pagamentos de uma cobranca especifica
export async function getPagamentosCobranca(cobrancaId: number): Promise<PagamentoPlataforma[]> {
  const response = await api.get(`/v1/admin/assinaturas/cobrancas/${cobrancaId}/pagamentos`)
  return response.data.dados
}

// ==================== ACOES ====================

// Cancelar assinatura
export async function cancelarAssinatura(id: number): Promise<AssinaturaResponse> {
  const response = await api.post(`/v1/admin/assinaturas/${id}/cancelar`)
  return response.data.dados
}

// Suspender assinatura
export async function suspenderAssinatura(id: number): Promise<AssinaturaResponse> {
  const response = await api.post(`/v1/admin/assinaturas/${id}/suspender`)
  return response.data.dados
}

// Reativar assinatura
export async function reativarAssinatura(id: number): Promise<AssinaturaResponse> {
  const response = await api.post(`/v1/admin/assinaturas/${id}/reativar`)
  return response.data.dados
}
```

---

## 1.4. Query Hooks — `src/queries/useAssinaturas.ts`

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getAssinaturas,
  getAssinatura,
  getCobrancasAssinatura,
  getAssinaturaByOrganizacao,
  getCobrancasOrganizacao,
  getPagamentosOrganizacao,
  getPagamentosCobranca,
  cancelarAssinatura,
  suspenderAssinatura,
  reativarAssinatura,
  getBillingDashboard,
} from '../services/assinaturas'
import type { AssinaturaFiltro } from '../types/assinatura'
import toast from 'react-hot-toast'

const QUERY_KEY = 'admin-assinaturas'
const DASHBOARD_KEY = 'admin-billing-dashboard'

// === Queries - Listagem Geral ===

export function useAssinaturas(filtro?: AssinaturaFiltro) {
  return useQuery({
    queryKey: [QUERY_KEY, filtro],
    queryFn: () => getAssinaturas(filtro),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: false,
  })
}

export function useBillingDashboard() {
  return useQuery({
    queryKey: [DASHBOARD_KEY],
    queryFn: getBillingDashboard,
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
  })
}

// === Queries - Por Assinatura ID ===

export function useAssinatura(id: number) {
  return useQuery({
    queryKey: [QUERY_KEY, id],
    queryFn: () => getAssinatura(id),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: false,
    enabled: !!id,
  })
}

export function useCobrancasAssinatura(id: number) {
  return useQuery({
    queryKey: [QUERY_KEY, id, 'cobrancas'],
    queryFn: () => getCobrancasAssinatura(id),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: false,
    enabled: !!id,
  })
}

// === Queries - Por Organizacao ID ===
// Use estes hooks quando estiver dentro do detalhe de uma organizacao

export function useAssinaturaByOrganizacao(orgId: number) {
  return useQuery({
    queryKey: [QUERY_KEY, 'org', orgId],
    queryFn: () => getAssinaturaByOrganizacao(orgId),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: false,
    enabled: !!orgId,
  })
}

export function useCobrancasOrganizacao(orgId: number) {
  return useQuery({
    queryKey: [QUERY_KEY, 'org', orgId, 'cobrancas'],
    queryFn: () => getCobrancasOrganizacao(orgId),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: false,
    enabled: !!orgId,
  })
}

export function usePagamentosOrganizacao(orgId: number) {
  return useQuery({
    queryKey: [QUERY_KEY, 'org', orgId, 'pagamentos'],
    queryFn: () => getPagamentosOrganizacao(orgId),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: false,
    enabled: !!orgId,
  })
}

// === Queries - Pagamentos por Cobranca ===

export function usePagamentosCobranca(cobrancaId: number) {
  return useQuery({
    queryKey: [QUERY_KEY, 'cobranca', cobrancaId, 'pagamentos'],
    queryFn: () => getPagamentosCobranca(cobrancaId),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: false,
    enabled: !!cobrancaId,
  })
}

// === Mutations ===

export function useCancelarAssinatura() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => cancelarAssinatura(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [DASHBOARD_KEY] })
      toast.success('Assinatura cancelada com sucesso')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao cancelar assinatura')
    },
  })
}

export function useSuspenderAssinatura() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => suspenderAssinatura(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [DASHBOARD_KEY] })
      toast.success('Assinatura suspensa com sucesso')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao suspender assinatura')
    },
  })
}

export function useReativarAssinatura() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => reativarAssinatura(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      queryClient.invalidateQueries({ queryKey: [DASHBOARD_KEY] })
      toast.success('Assinatura reativada com sucesso')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao reativar assinatura')
    },
  })
}
```

---

## 1.5. Páginas a Criar — Admin

### 1.5.1. `src/pages/assinaturas/BillingDashboard.tsx`

Página principal do módulo de billing. Exibe métricas consolidadas no topo e a lista de assinaturas abaixo.

**Cards de Métricas (topo da página):**

| Card | Valor | Ícone | Cor sugerida |
|------|-------|-------|-------------|
| MRR (Receita Recorrente) | `dashboard.mrr` formatado como BRL | `DollarSign` | Verde |
| Receita do Mês | `dashboard.receitaMesAtual` formatado como BRL | `TrendingUp` | Verde claro |
| Assinaturas Ativas | `dashboard.totalAtivas` | `CheckCircle` | Verde |
| Em Trial | `dashboard.totalTrial` | `Clock` | Azul |
| Vencidas | `dashboard.totalVencidas` | `AlertTriangle` | Vermelho |
| Canceladas | `dashboard.totalCanceladas` | `XCircle` | Cinza |
| Suspensas | `dashboard.totalSuspensas` | `Pause` | Amarelo |

**Formatação de moeda:**
```typescript
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value)
}
```

Layout sugerido: Grid de 4 colunas no desktop, 2 no tablet, 1 no mobile. Cards com fundo branco, sombra leve, ícone à esquerda, valor grande e label pequeno.

---

### 1.5.2. `src/pages/assinaturas/AssinaturasList.tsx`

Tabela principal de assinaturas (pode ser integrada abaixo do Dashboard ou em rota separada).

**Colunas da Tabela:**

| Coluna | Campo | Formatação |
|--------|-------|-----------|
| Organização | `organizacaoNome` | Texto |
| Plano | `planoNome` | Badge com cor do plano |
| Status | `status` | Badge colorido (ver mapeamento abaixo) |
| Ciclo | `cicloCobranca` | "Mensal" / "Anual" |
| Valor | `valorMensal` ou `valorAnual` | Moeda BRL (baseado no ciclo) |
| Próx. Vencimento | `dtProximoVencimento` | Data formatada ou "—" |
| Trial até | `dtFimTrial` | Data formatada (só se status = TRIAL) |
| Integração Assas | `assasSubscriptionId` | Ícone check/x |
| Ações | — | Botões de ação |

**Mapeamento de cores dos badges de status:**

```typescript
const statusConfig: Record<StatusAssinatura, { label: string; color: string; bgColor: string }> = {
  TRIAL: {
    label: 'Trial',
    color: 'text-blue-700',
    bgColor: 'bg-blue-100',
  },
  ATIVA: {
    label: 'Ativa',
    color: 'text-green-700',
    bgColor: 'bg-green-100',
  },
  VENCIDA: {
    label: 'Vencida',
    color: 'text-red-700',
    bgColor: 'bg-red-100',
  },
  CANCELADA: {
    label: 'Cancelada',
    color: 'text-gray-700',
    bgColor: 'bg-gray-100',
  },
  SUSPENSA: {
    label: 'Suspensa',
    color: 'text-yellow-700',
    bgColor: 'bg-yellow-100',
  },
}
```

**Filtros (acima da tabela):**
- Select de Status: Todos / Trial / Ativa / Vencida / Cancelada / Suspensa
- Busca por nome da organização (client-side filter no array retornado)

**Ações por linha (dropdown ou botões):**
- **Ver detalhes** → navega para `/assinaturas/:id`
- **Reativar** → visível apenas se status = VENCIDA, CANCELADA ou SUSPENSA (com confirmação)
- **Suspender** → visível apenas se status = ATIVA ou TRIAL (com confirmação)
- **Cancelar** → visível apenas se status != CANCELADA (com confirmação e alerta: "Esta ação cancelará a assinatura no Assas também")

**Dialogs de confirmação:**
Usar modal/dialog com texto descritivo:
- Cancelar: "Tem certeza que deseja cancelar a assinatura de **{organizacaoNome}**? Isso também cancelará a assinatura no gateway de pagamento."
- Suspender: "Tem certeza que deseja suspender a assinatura de **{organizacaoNome}**? A organização perderá acesso ao sistema."
- Reativar: "Deseja reativar a assinatura de **{organizacaoNome}**?"

---

### 1.5.3. `src/pages/assinaturas/AssinaturaDetail.tsx`

Página de detalhe de uma assinatura individual.

**Seção 1 — Informações da Assinatura:**

| Campo | Valor | Formatação |
|-------|-------|-----------|
| ID | `assinatura.id` | `#123` |
| Organização | `assinatura.organizacaoNome` | Link para `/organizacoes/:organizacaoId` |
| Plano | `assinatura.planoNome` (`assinatura.planoCodigo`) | Badge |
| Status | `assinatura.status` | Badge colorido |
| Ciclo | `assinatura.cicloCobranca` | "Mensal" / "Anual" |
| Valor Mensal | `assinatura.valorMensal` | Moeda BRL |
| Valor Anual | `assinatura.valorAnual` | Moeda BRL |
| Início Trial | `assinatura.dtInicioTrial` | Data formatada ou "—" |
| Fim Trial | `assinatura.dtFimTrial` | Data formatada ou "—" |
| Início Assinatura | `assinatura.dtInicio` | Data formatada ou "—" |
| Próximo Vencimento | `assinatura.dtProximoVencimento` | Data formatada ou "—" |
| Data Cancelamento | `assinatura.dtCancelamento` | Data formatada ou "—" |
| Assas Customer ID | `assinatura.assasCustomerId` | Monospace ou "Não integrado" |
| Assas Subscription ID | `assinatura.assasSubscriptionId` | Monospace ou "Não integrado" |
| Criada em | `assinatura.dtCriacao` | Data e hora |

**Seção 2 — Botões de Ação (cabeçalho da página):**
- Reativar / Suspender / Cancelar (mesma lógica de visibilidade e confirmação da lista)
- Voltar para lista

**Seção 3 — Histórico de Cobrancas (tabela abaixo):**

Usa `useCobrancasAssinatura(id)` para carregar as cobranças.

| Coluna | Campo | Formatação |
|--------|-------|-----------|
| # | `cobranca.id` | Número |
| Referência | `referenciaMes`/`referenciaAno` | "Mar/2026" |
| Valor | `cobranca.valor` | Moeda BRL |
| Vencimento | `cobranca.dtVencimento` | Data |
| Pagamento | `cobranca.dtPagamento` | Data ou "—" |
| Status | `cobranca.status` | Badge colorido |
| Forma | `cobranca.formaPagamento` | "PIX" / "Boleto" / "Cartão" |
| Links | `assasInvoiceUrl`, `assasBankSlipUrl` | Ícone de link externo |

**Mapeamento de cores dos badges de status de cobrança:**

```typescript
const cobrancaStatusConfig: Record<StatusCobrancaPlataforma, { label: string; color: string; bgColor: string }> = {
  PENDENTE: { label: 'Pendente', color: 'text-yellow-700', bgColor: 'bg-yellow-100' },
  PAGA:     { label: 'Paga',     color: 'text-green-700',  bgColor: 'bg-green-100' },
  VENCIDA:  { label: 'Vencida',  color: 'text-red-700',    bgColor: 'bg-red-100' },
  CANCELADA:{ label: 'Cancelada',color: 'text-gray-700',   bgColor: 'bg-gray-100' },
  ESTORNADA:{ label: 'Estornada',color: 'text-purple-700', bgColor: 'bg-purple-100' },
}
```

**Formatação de referência:**

```typescript
function formatReferencia(mes: number, ano: number): string {
  const meses = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
                 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez']
  return `${meses[mes - 1]}/${ano}`
}
```

**Formatação de forma de pagamento:**

```typescript
const formaPagamentoLabels: Record<string, string> = {
  PIX: 'PIX',
  BOLETO: 'Boleto',
  CARTAO_CREDITO: 'Cartão de Crédito',
}
```

---

## Parte 2 — Self-Service da Organização (App)

Estes endpoints são usados pelo frontend principal das organizações (não o admin), para que cada organização veja e gerencie sua própria assinatura.

### 2.1. API — Endpoints Self-Service

| Método | Path | Descrição | Request Body | Response Body |
|--------|------|-----------|-------------|---------------|
| `GET` | `/v1/assinatura/status` | Status da minha assinatura | — | `ResponseAPI<AssinaturaStatus>` |
| `GET` | `/v1/assinatura/cobrancas` | Minhas cobrancas | — | `ResponseAPI<CobrancaPlataforma[]>` |
| `POST` | `/v1/assinatura/escolher-plano` | Escolher/trocar plano | `EscolherPlano` | `ResponseAPI<AssinaturaResponse>` |

#### Query Params do GET /assinatura/cobrancas

| Param | Tipo | Descrição |
|-------|------|-----------|
| `status` | `PENDENTE` \| `PAGA` \| `VENCIDA` \| `CANCELADA` \| `ESTORNADA` | Filtrar por status |

---

### 2.2. Service — `src/services/minhaAssinatura.ts`

```typescript
import { api } from './api'
import type {
  AssinaturaStatus,
  AssinaturaResponse,
  CobrancaPlataforma,
  EscolherPlano,
} from '../types/assinatura'

// Status da minha assinatura
export async function getMinhaAssinaturaStatus(): Promise<AssinaturaStatus> {
  const response = await api.get('/v1/assinatura/status')
  return response.data.dados
}

// Minhas cobrancas
export async function getMinhasCobrancas(status?: string): Promise<CobrancaPlataforma[]> {
  const params: Record<string, string> = {}
  if (status) params.status = status

  const response = await api.get('/v1/assinatura/cobrancas', { params })
  return response.data.dados
}

// Escolher/trocar plano
export async function escolherPlano(data: EscolherPlano): Promise<AssinaturaResponse> {
  const response = await api.post('/v1/assinatura/escolher-plano', data)
  return response.data.dados
}
```

---

### 2.3. Query Hooks — `src/queries/useMinhaAssinatura.ts`

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getMinhaAssinaturaStatus,
  getMinhasCobrancas,
  escolherPlano,
} from '../services/minhaAssinatura'
import type { EscolherPlano } from '../types/assinatura'
import toast from 'react-hot-toast'

const STATUS_KEY = 'minha-assinatura-status'
const COBRANCAS_KEY = 'minhas-cobrancas'

export function useMinhaAssinaturaStatus() {
  return useQuery({
    queryKey: [STATUS_KEY],
    queryFn: getMinhaAssinaturaStatus,
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: true,  // Importante: atualizar ao voltar para a aba
  })
}

export function useMinhasCobrancas(status?: string) {
  return useQuery({
    queryKey: [COBRANCAS_KEY, status],
    queryFn: () => getMinhasCobrancas(status),
    staleTime: 2 * 60 * 1000,
    refetchOnWindowFocus: false,
  })
}

export function useEscolherPlano() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: EscolherPlano) => escolherPlano(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [STATUS_KEY] })
      queryClient.invalidateQueries({ queryKey: [COBRANCAS_KEY] })
      toast.success('Plano ativado com sucesso!')
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao escolher plano')
    },
  })
}
```

---

## Parte 3 — Bloqueio por Assinatura (Login + Guard)

### 3.1. Alteração no tipo do Login Response

O endpoint de login (`POST /v1/auth/login`) agora retorna o campo `assinatura` dentro de `organizacao`:

```typescript
// Atualizar o tipo OrganizacaoInfo existente:
export interface OrganizacaoInfo {
  id: number
  nome: string
  nomeFantasia: string
  plano: PlanoBellory
  configSistema: ConfigSistema
  tema: Tema
  ativo: boolean
  dtCadastro: string
  limitesPersonalizados: PlanoLimites | null

  // NOVO CAMPO:
  assinatura: AssinaturaStatus | null
}
```

O campo `assinatura` no response do login tem esta estrutura:

```json
{
  "organizacao": {
    "id": 1,
    "nome": "Salão Exemplo",
    "assinatura": {
      "bloqueado": false,
      "statusAssinatura": "TRIAL",
      "diasRestantesTrial": 12,
      "mensagem": "Voce esta no periodo de teste. Restam 12 dias.",
      "planoCodigo": "basico",
      "planoNome": "Básico"
    }
  }
}
```

### 3.2. Lógica de Bloqueio

Após o login, armazenar `organizacao.assinatura` no estado global (context/store). A lógica de bloqueio é:

```typescript
// No AuthContext ou similar:
const assinatura = loginResponse.organizacao.assinatura

if (assinatura?.bloqueado) {
  // Redirecionar para /planos ou /assinatura/escolher-plano
  // Não permitir acesso às páginas do sistema
  navigate('/escolher-plano')
} else if (assinatura?.statusAssinatura === 'TRIAL') {
  // Permitir acesso mas mostrar banner de trial
  // "Você está no período de teste. Restam X dias."
}
```

### 3.3. Guard de Rota — `src/components/guards/AssinaturaGuard.tsx`

```typescript
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

interface Props {
  children: React.ReactNode
}

export function AssinaturaGuard({ children }: Props) {
  const { organizacao } = useAuth()
  const assinatura = organizacao?.assinatura

  // Se bloqueado, redireciona para tela de planos
  if (assinatura?.bloqueado) {
    return <Navigate to="/escolher-plano" replace />
  }

  return <>{children}</>
}
```

Usar o guard nas rotas protegidas:

```tsx
// No App.tsx, envolver as rotas do app (não admin) com o guard:
<Route element={<AssinaturaGuard><AppLayout /></AssinaturaGuard>}>
  <Route path="/dashboard" element={<Dashboard />} />
  <Route path="/agendamentos" element={<Agendamentos />} />
  {/* ... demais rotas do app ... */}
</Route>

// Rota de escolher plano fica FORA do guard:
<Route path="/escolher-plano" element={<EscolherPlano />} />
```

### 3.4. Banner de Trial — `src/components/layout/TrialBanner.tsx`

Componente exibido no topo do layout quando a organização está em trial:

```typescript
import { Clock, ArrowRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

export function TrialBanner() {
  const navigate = useNavigate()
  const { organizacao } = useAuth()
  const assinatura = organizacao?.assinatura

  // Só mostra se em trial e não bloqueado
  if (!assinatura || assinatura.statusAssinatura !== 'TRIAL' || assinatura.bloqueado) {
    return null
  }

  const dias = assinatura.diasRestantesTrial ?? 0
  const urgente = dias <= 3

  return (
    <div className={`px-4 py-2 text-sm flex items-center justify-between ${
      urgente ? 'bg-red-50 text-red-700' : 'bg-blue-50 text-blue-700'
    }`}>
      <div className="flex items-center gap-2">
        <Clock className="w-4 h-4" />
        <span>
          {dias > 0
            ? `Período de teste: ${dias} ${dias === 1 ? 'dia restante' : 'dias restantes'}`
            : 'Seu período de teste expira hoje!'}
        </span>
      </div>
      <button
        onClick={() => navigate('/escolher-plano')}
        className="flex items-center gap-1 font-medium hover:underline"
      >
        Escolher plano <ArrowRight className="w-3 h-3" />
      </button>
    </div>
  )
}
```

Adicionar no layout principal (AppLayout), antes do conteúdo:

```tsx
<TrialBanner />
<main>{children}</main>
```

---

## Parte 4 — Página de Escolher Plano

### 4.1. `src/pages/assinatura/EscolherPlano.tsx`

Página exibida quando a organização precisa escolher/trocar plano. Acessível tanto pelo redirecionamento de bloqueio quanto por link no banner de trial.

**Layout:**

1. **Título e subtítulo:** "Escolha seu plano" / "Selecione o plano ideal para o seu negócio"

2. **Toggle Mensal/Anual:** Switch para alternar entre preços mensais e anuais. Se anual, mostrar badge de desconto ("Economize X%")

3. **Grid de Planos:** Buscar planos com `GET /v1/public/planos` (endpoint público existente). Exibir cards lado a lado:

   Para cada plano:
   - Ícone e nome
   - Badge se `popular = true`
   - Tagline
   - Preço (mensal ou anual conforme toggle)
   - Lista de features com check/x
   - Botão CTA: abre modal de pagamento

4. **Modal de Forma de Pagamento:** Ao clicar no CTA:
   - Mostra 3 opções: PIX, Boleto, Cartão de Crédito
   - Botão "Confirmar" chama `POST /v1/assinatura/escolher-plano` com:
     ```json
     {
       "planoCodigo": "plus",
       "cicloCobranca": "MENSAL",
       "formaPagamento": "PIX"
     }
     ```
   - Após sucesso, mostrar toast e redirecionar para `/dashboard`
   - Se tiver dados de PIX na resposta (invoice URL), mostrar QR Code

5. **Se já tem assinatura ativa:** Mostrar plano atual destacado com badge "Plano atual", desabilitar botão nesse card

---

## Parte 5 — Integração no Projeto Existente

### 5.1. Rotas — `src/App.tsx`

```typescript
// Lazy imports (adicionar junto com os outros)
const BillingDashboard = lazy(() => import('./pages/assinaturas/BillingDashboard'))
const AssinaturasList = lazy(() => import('./pages/assinaturas/AssinaturasList'))
const AssinaturaDetail = lazy(() => import('./pages/assinaturas/AssinaturaDetail'))
const EscolherPlano = lazy(() => import('./pages/assinatura/EscolherPlano'))

// Rotas Admin (dentro do bloco ProtectedRoute admin):
<Route path="/assinaturas" element={<BillingDashboard />} />
<Route path="/assinaturas/lista" element={<AssinaturasList />} />
<Route path="/assinaturas/:id" element={<AssinaturaDetail />} />

// Rota App (fora do AssinaturaGuard, precisa estar autenticado mas não bloqueado):
<Route path="/escolher-plano" element={<EscolherPlano />} />
```

### 5.2. Sidebar — `src/components/layout/Sidebar.tsx`

Adicionar item no menu do admin:

```typescript
import { /* ... existentes ... */, Receipt } from 'lucide-react'

// No array navItems do admin, adicionar:
{ path: '/assinaturas', icon: Receipt, label: 'Assinaturas' },
```

---

## 6. Formatação de Datas

Usar formatação consistente em todas as páginas:

```typescript
function formatDate(dateString: string | null): string {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

function formatDateTime(dateString: string | null): string {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
```

---

## 7. Dados de Exemplo (para referência visual)

### Assinatura em Trial

```json
{
  "id": 1,
  "organizacaoId": 5,
  "organizacaoNome": "Salão Beauty Hair",
  "planoBelloryId": 2,
  "planoNome": "Básico",
  "planoCodigo": "basico",
  "status": "TRIAL",
  "cicloCobranca": "MENSAL",
  "dtInicioTrial": "2026-02-20T10:30:00",
  "dtFimTrial": "2026-03-06T10:30:00",
  "dtInicio": null,
  "dtProximoVencimento": null,
  "dtCancelamento": null,
  "valorMensal": 59.90,
  "valorAnual": 574.80,
  "assasCustomerId": null,
  "assasSubscriptionId": null,
  "dtCriacao": "2026-02-20T10:30:00"
}
```

### Assinatura Ativa com Assas

```json
{
  "id": 2,
  "organizacaoId": 3,
  "organizacaoNome": "Barbearia do João",
  "planoBelloryId": 3,
  "planoNome": "Plus",
  "planoCodigo": "plus",
  "status": "ATIVA",
  "cicloCobranca": "MENSAL",
  "dtInicioTrial": "2026-01-10T08:00:00",
  "dtFimTrial": "2026-01-24T08:00:00",
  "dtInicio": "2026-01-20T14:30:00",
  "dtProximoVencimento": "2026-04-20T14:30:00",
  "dtCancelamento": null,
  "valorMensal": 89.90,
  "valorAnual": 862.80,
  "assasCustomerId": "cus_000012345678",
  "assasSubscriptionId": "sub_000087654321",
  "dtCriacao": "2026-01-10T08:00:00"
}
```

### Cobrança Pendente

```json
{
  "id": 10,
  "valor": 89.90,
  "dtVencimento": "2026-03-10",
  "dtPagamento": null,
  "status": "PENDENTE",
  "formaPagamento": "PIX",
  "assasInvoiceUrl": "https://www.asaas.com/i/abc123",
  "assasBankSlipUrl": null,
  "assasPixQrCode": "data:image/png;base64,...",
  "assasPixCopiaCola": "00020126580014br.gov.bcb.pix...",
  "referenciaMes": 3,
  "referenciaAno": 2026,
  "dtCriacao": "2026-03-01T06:00:00"
}
```

### Dashboard de Billing

```json
{
  "mrr": 4250.50,
  "totalAtivas": 47,
  "totalTrial": 12,
  "totalVencidas": 3,
  "totalCanceladas": 8,
  "totalSuspensas": 1,
  "receitaMesAtual": 3890.00
}
```

### Status no Login (Trial)

```json
{
  "bloqueado": false,
  "statusAssinatura": "TRIAL",
  "diasRestantesTrial": 4,
  "mensagem": "Voce esta no periodo de teste. Restam 4 dias.",
  "planoCodigo": "basico",
  "planoNome": "Básico"
}
```

### Status no Login (Bloqueado)

```json
{
  "bloqueado": true,
  "statusAssinatura": "VENCIDA",
  "diasRestantesTrial": null,
  "mensagem": "Sua assinatura esta vencida. Regularize o pagamento para continuar.",
  "planoCodigo": "plus",
  "planoNome": "Plus"
}
```

---

## 8. Resumo — Arquivos a Criar

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| `src/types/assinatura.ts` | Types | Interfaces TypeScript de assinatura, cobrança, pagamento, dashboard |
| `src/services/assinaturas.ts` | Service | Chamadas API admin (listar, cancelar, suspender, reativar, dashboard) |
| `src/services/minhaAssinatura.ts` | Service | Chamadas API self-service (status, cobrancas, escolher plano) |
| `src/queries/useAssinaturas.ts` | Hooks | React Query hooks admin (queries + mutations) |
| `src/queries/useMinhaAssinatura.ts` | Hooks | React Query hooks self-service |
| `src/pages/assinaturas/BillingDashboard.tsx` | Página | Dashboard de billing com métricas + lista resumida |
| `src/pages/assinaturas/AssinaturasList.tsx` | Página | Listagem completa com filtros e ações |
| `src/pages/assinaturas/AssinaturaDetail.tsx` | Página | Detalhe da assinatura + histórico de cobranças |
| `src/pages/assinatura/EscolherPlano.tsx` | Página | Seleção de plano com toggle mensal/anual |
| `src/components/guards/AssinaturaGuard.tsx` | Guard | Bloqueio de rotas para assinatura inativa |
| `src/components/layout/TrialBanner.tsx` | Layout | Banner de aviso de trial no topo |

## Arquivos a Modificar

| Arquivo | O que alterar |
|---------|---------------|
| `src/App.tsx` | Adicionar lazy imports + 4 rotas + envolver rotas do app com `AssinaturaGuard` |
| `src/components/layout/Sidebar.tsx` | Adicionar item "Assinaturas" com ícone `Receipt` no menu admin |
| `src/components/layout/AppLayout.tsx` | Adicionar `<TrialBanner />` antes do conteúdo |
| `src/types/auth.ts` (ou similar) | Adicionar campo `assinatura: AssinaturaStatus \| null` em `OrganizacaoInfo` |
| `src/contexts/AuthContext.tsx` (ou similar) | Armazenar `assinatura` no estado global após login |

---

## 9. Regras de Negócio Importantes

1. **Trial = 14 dias** — Organizações novas começam com trial. Após expirar sem escolher plano, o status vira VENCIDA e o sistema bloqueia.

2. **Bloqueio é por `assinatura.bloqueado`** — Não verificar status manualmente. O backend já calcula se deve bloquear (VENCIDA, CANCELADA, SUSPENSA, ou TRIAL expirado).

3. **Webhook é automático** — Quando o Assas confirma um pagamento, o backend atualiza a cobrança e assinatura automaticamente. O frontend só precisa re-fetchar os dados.

4. **Assas pode estar desconfigurado** — Se `assasCustomerId` e `assasSubscriptionId` forem `null`, a integração com Assas não está ativa. O admin pode gerenciar cobranças manualmente. O frontend deve mostrar "Não integrado" ao invés de um ID vazio.

5. **Admin vs Self-Service** — O admin (`/api/v1/admin/assinaturas`) pode ver e gerenciar TODAS as assinaturas. O self-service (`/api/v1/assinatura`) só vê a assinatura da organização do usuário logado (baseado no JWT token).

6. **Rotas admin requerem role** — Os endpoints admin requerem `ROLE_PLATFORM_ADMIN`, `ROLE_SUPERADMIN` ou `ROLE_ADMIN`. Atenção: o ROLE_ADMIN aqui é o admin da PLATAFORMA, não o admin da organização.

7. **Cancelar no admin cancela no Assas** — Ao cancelar pelo admin, o backend automaticamente cancela a assinatura no gateway Assas. Informar isso no dialog de confirmação.

8. **Múltiplas cobranças por assinatura** — Uma assinatura pode ter várias cobranças (uma por mês/ano). A tabela de cobranças no detalhe mostra o histórico completo.

9. **Tela de escolher plano é acessível sem assinatura ativa** — Essa rota deve ficar FORA do `AssinaturaGuard` mas DENTRO da autenticação. O usuário precisa estar logado mas pode estar com assinatura bloqueada.

10. **PIX tem QR Code e copia-e-cola** — Quando a forma de pagamento é PIX, a cobrança pode ter `assasPixQrCode` (imagem base64) e `assasPixCopiaCola` (texto para copiar). Mostrar ambos na UI.
