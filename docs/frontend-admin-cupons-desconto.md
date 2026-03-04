# Cupons de Desconto — Documentacao para Frontend

## Contexto

O backend expõe endpoints de gestão de cupons de desconto em `/api/v1/admin/cupons` (requer ROLE_SUPERADMIN ou ROLE_ADMIN), e um endpoint de validação de cupom no self-service em `/api/v1/assinatura/validar-cupom` (requer organização autenticada).

Os cupons de desconto permitem criar promoções para as assinaturas dos planos Bellory, com configurações flexíveis de restrição por empresa, período de validade, segmento (publicoAlvo), plano e ciclo de cobrança. O desconto é aplicado **apenas na primeira cobrança** — as cobranças futuras usam o valor original do plano.

O projeto Bellory-Admin usa: **React 19 + Vite + TypeScript + Tailwind CSS + TanStack React Query + Axios + Lucide React + Framer Motion + react-hot-toast**.

Não usa shadcn/ui — os componentes UI são customizados em `components/ui/` (Button, Input, Card, Badge, etc.).

---

## Parte 1 — Painel Administrativo (Admin)

### 1.1. API — Endpoints Admin de Cupons

Base URL já configurada no axios (`services/api.ts`) termina em `/api`, então os paths abaixo são relativos a isso.

| Método | Path | Descrição | Request Body | Response Body |
|--------|------|-----------|-------------|---------------|
| `GET` | `/v1/admin/cupons` | Listar todos os cupons | — | `ResponseAPI<CupomDesconto[]>` |
| `GET` | `/v1/admin/cupons/vigentes` | Listar apenas cupons vigentes | — | `ResponseAPI<CupomDesconto[]>` |
| `GET` | `/v1/admin/cupons/:id` | Buscar cupom por ID | — | `ResponseAPI<CupomDesconto>` |
| `POST` | `/v1/admin/cupons` | Criar novo cupom | `CupomDescontoCreate` | `ResponseAPI<CupomDesconto>` |
| `PUT` | `/v1/admin/cupons/:id` | Atualizar cupom | `CupomDescontoUpdate` | `ResponseAPI<CupomDesconto>` |
| `DELETE` | `/v1/admin/cupons/:id` | Desativar cupom (soft delete) | — | `ResponseAPI<void>` |
| `PATCH` | `/v1/admin/cupons/:id/ativar` | Reativar cupom | — | `ResponseAPI<CupomDesconto>` |
| `GET` | `/v1/admin/cupons/:id/utilizacoes` | Histórico de uso do cupom | — | `ResponseAPI<CupomUtilizacao[]>` |

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

### 1.2. Types — `src/types/cupom.ts`

```typescript
// === Tipos de Desconto ===
export type TipoDesconto = 'PERCENTUAL' | 'VALOR_FIXO'

// === Ciclos de Cobrança ===
export type CicloCobranca = 'MENSAL' | 'ANUAL'

// === Response completa (GET lista e GET por ID) ===
export interface CupomDesconto {
  id: number
  codigo: string                          // Ex: "BEMVINDO50", "LANCAMENTO"
  descricao: string | null                // Descrição interna
  tipoDesconto: TipoDesconto             // "PERCENTUAL" ou "VALOR_FIXO"
  valorDesconto: number                   // Ex: 50.00 (50% se PERCENTUAL, R$50 se VALOR_FIXO)
  dtInicio: string | null                 // ISO datetime — início da vigência
  dtFim: string | null                    // ISO datetime — fim da vigência
  maxUtilizacoes: number | null           // null = ilimitado
  maxUtilizacoesPorOrg: number | null     // null = ilimitado
  totalUtilizado: number                  // Contador de usos
  planosPermitidos: string[] | null       // ["basico", "plus", "premium"] ou null = todos
  segmentosPermitidos: string[] | null    // ["Barbearia", "Salao de Beleza"] ou null = todos
  organizacoesPermitidas: number[] | null // [1, 5, 10] ou null = todas
  cicloCobranca: string | null            // "MENSAL", "ANUAL" ou null = ambos
  ativo: boolean
  vigente: boolean                        // Campo computado pelo backend (ativo + dentro do período)
  dtCriacao: string
  dtAtualizacao: string | null
}

// === Request para criar cupom (POST) ===
export interface CupomDescontoCreate {
  codigo: string                          // Obrigatório, max 50 chars (será convertido para UPPERCASE)
  descricao?: string                      // Opcional, max 255 chars
  tipoDesconto: TipoDesconto             // Obrigatório
  valorDesconto: number                   // Obrigatório, deve ser positivo
  dtInicio?: string                       // Opcional, ISO datetime
  dtFim?: string                          // Opcional, ISO datetime
  maxUtilizacoes?: number                 // Opcional
  maxUtilizacoesPorOrg?: number           // Opcional
  planosPermitidos?: string[]             // Opcional — códigos de planos
  segmentosPermitidos?: string[]          // Opcional — publicoAlvo das organizações
  organizacoesPermitidas?: number[]       // Opcional — IDs de organizações
  cicloCobranca?: CicloCobranca           // Opcional — se null, vale para ambos
}

// === Request para atualizar cupom (PUT) — todos os campos são opcionais ===
export interface CupomDescontoUpdate {
  codigo?: string
  descricao?: string
  tipoDesconto?: TipoDesconto
  valorDesconto?: number
  dtInicio?: string
  dtFim?: string
  maxUtilizacoes?: number
  maxUtilizacoesPorOrg?: number
  planosPermitidos?: string[]
  segmentosPermitidos?: string[]
  organizacoesPermitidas?: number[]
  cicloCobranca?: CicloCobranca
}

// === Registro de utilização do cupom ===
export interface CupomUtilizacao {
  id: number
  cupomId: number
  cupomCodigo: string
  organizacaoId: number
  assinaturaId: number | null
  cobrancaId: number | null
  valorOriginal: number                   // Valor do plano antes do desconto
  valorDesconto: number                   // Valor do desconto aplicado
  valorFinal: number                      // Valor que a organização pagou
  planoCodigo: string | null
  cicloCobranca: string | null
  dtUtilizacao: string                    // ISO datetime
}

// === Response da validação de cupom (usado no self-service) ===
export interface CupomValidacaoResponse {
  valido: boolean
  mensagem: string                        // Mensagem descritiva do resultado
  tipoDesconto: TipoDesconto | null       // Presente apenas se válido
  valorDesconto: number | null            // Valor do desconto calculado
  valorOriginal: number | null            // Preço original do plano
  valorComDesconto: number | null         // Preço final com desconto
}

// === Request para validar cupom (self-service) ===
export interface ValidarCupomRequest {
  codigoCupom: string                     // Obrigatório
  planoCodigo: string                     // Obrigatório
  cicloCobranca: CicloCobranca            // Obrigatório
}
```

---

### 1.3. Service — `src/services/cupomService.ts`

```typescript
import api from './api'
import type {
  CupomDesconto,
  CupomDescontoCreate,
  CupomDescontoUpdate,
  CupomUtilizacao,
  CupomValidacaoResponse,
  ValidarCupomRequest
} from '../types/cupom'

const BASE = '/v1/admin/cupons'

// === Admin ===

export const cupomService = {
  listarTodos: async (): Promise<CupomDesconto[]> => {
    const { data } = await api.get(BASE)
    return data.dados
  },

  listarVigentes: async (): Promise<CupomDesconto[]> => {
    const { data } = await api.get(`${BASE}/vigentes`)
    return data.dados
  },

  buscarPorId: async (id: number): Promise<CupomDesconto> => {
    const { data } = await api.get(`${BASE}/${id}`)
    return data.dados
  },

  criar: async (dto: CupomDescontoCreate): Promise<CupomDesconto> => {
    const { data } = await api.post(BASE, dto)
    return data.dados
  },

  atualizar: async (id: number, dto: CupomDescontoUpdate): Promise<CupomDesconto> => {
    const { data } = await api.put(`${BASE}/${id}`, dto)
    return data.dados
  },

  desativar: async (id: number): Promise<void> => {
    await api.delete(`${BASE}/${id}`)
  },

  ativar: async (id: number): Promise<CupomDesconto> => {
    const { data } = await api.patch(`${BASE}/${id}/ativar`)
    return data.dados
  },

  listarUtilizacoes: async (id: number): Promise<CupomUtilizacao[]> => {
    const { data } = await api.get(`${BASE}/${id}/utilizacoes`)
    return data.dados
  },
}

// === Self-service (organização autenticada) ===

export const validarCupom = async (dto: ValidarCupomRequest): Promise<CupomValidacaoResponse> => {
  const { data } = await api.post('/v1/assinatura/validar-cupom', dto)
  return data.dados
}
```

---

### 1.4. React Query Hooks — `src/hooks/useCupom.ts`

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { cupomService } from '../services/cupomService'
import type { CupomDescontoCreate, CupomDescontoUpdate } from '../types/cupom'
import toast from 'react-hot-toast'

const QUERY_KEY = 'cupons'

export function useCupons() {
  return useQuery({
    queryKey: [QUERY_KEY],
    queryFn: cupomService.listarTodos,
  })
}

export function useCuponsVigentes() {
  return useQuery({
    queryKey: [QUERY_KEY, 'vigentes'],
    queryFn: cupomService.listarVigentes,
  })
}

export function useCupom(id: number) {
  return useQuery({
    queryKey: [QUERY_KEY, id],
    queryFn: () => cupomService.buscarPorId(id),
    enabled: !!id,
  })
}

export function useCupomUtilizacoes(id: number) {
  return useQuery({
    queryKey: [QUERY_KEY, id, 'utilizacoes'],
    queryFn: () => cupomService.listarUtilizacoes(id),
    enabled: !!id,
  })
}

export function useCriarCupom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: CupomDescontoCreate) => cupomService.criar(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Cupom criado com sucesso')
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erro ao criar cupom')
    },
  })
}

export function useAtualizarCupom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, dto }: { id: number; dto: CupomDescontoUpdate }) =>
      cupomService.atualizar(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Cupom atualizado com sucesso')
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erro ao atualizar cupom')
    },
  })
}

export function useDesativarCupom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => cupomService.desativar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Cupom desativado')
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erro ao desativar cupom')
    },
  })
}

export function useAtivarCupom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => cupomService.ativar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] })
      toast.success('Cupom ativado')
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Erro ao ativar cupom')
    },
  })
}
```

---

## Parte 2 — Self-Service (Organização)

### 2.1. Endpoint de Validação de Cupom

Antes de confirmar o plano, o frontend pode validar o cupom para mostrar o desconto em tempo real:

| Método | Path | Descrição | Request Body | Response Body |
|--------|------|-----------|-------------|---------------|
| `POST` | `/v1/assinatura/validar-cupom` | Validar cupom para o plano | `ValidarCupomRequest` | `ResponseAPI<CupomValidacaoResponse>` |

**Request:**
```json
{
  "codigoCupom": "BEMVINDO50",
  "planoCodigo": "plus",
  "cicloCobranca": "MENSAL"
}
```

**Response (cupom válido):**
```json
{
  "success": true,
  "dados": {
    "valido": true,
    "mensagem": "Cupom valido",
    "tipoDesconto": "PERCENTUAL",
    "valorDesconto": 49.95,
    "valorOriginal": 99.90,
    "valorComDesconto": 49.95
  }
}
```

**Response (cupom inválido):**
```json
{
  "success": true,
  "dados": {
    "valido": false,
    "mensagem": "Cupom fora do periodo de validade",
    "tipoDesconto": null,
    "valorDesconto": null,
    "valorOriginal": null,
    "valorComDesconto": null
  }
}
```

### 2.2. Endpoint Escolher Plano (Atualizado)

O endpoint `POST /v1/assinatura/escolher-plano` agora aceita um campo opcional `codigoCupom`:

```json
{
  "planoCodigo": "plus",
  "cicloCobranca": "MENSAL",
  "formaPagamento": "PIX",
  "codigoCupom": "BEMVINDO50"
}
```

Se `codigoCupom` for informado e inválido, o endpoint retorna `400 Bad Request` com a mensagem de erro. Se for válido, o desconto é aplicado automaticamente na primeira cobrança.

### 2.3. Response Atualizada de Assinatura

O `AssinaturaResponse` agora inclui dois campos novos:

```typescript
interface AssinaturaResponse {
  // ... campos existentes ...
  cupomCodigo: string | null      // Código do cupom aplicado
  valorDesconto: number | null    // Valor do desconto na primeira cobrança
}
```

### 2.4. Response Atualizada de Cobrança

O `CobrancaPlataforma` agora inclui campos de desconto:

```typescript
interface CobrancaPlataforma {
  // ... campos existentes ...
  cupomCodigo: string | null            // Código do cupom aplicado
  valorOriginal: number | null          // Preço original sem desconto
  valorDescontoAplicado: number | null  // Valor do desconto
}
```

**Nota:** `valorOriginal` e `valorDescontoAplicado` só aparecem na primeira cobrança (a que teve cupom). Cobranças futuras (mensais/anuais) usam o preço cheio do plano e esses campos são `null`.

---

## Parte 3 — Regras de Negócio

### 3.1. Tipos de Desconto

| Tipo | Descrição | Exemplo |
|------|-----------|---------|
| `PERCENTUAL` | Desconto em porcentagem sobre o valor do plano | `valorDesconto: 50` = 50% de desconto |
| `VALOR_FIXO` | Desconto em valor absoluto (R$) | `valorDesconto: 30.00` = R$ 30,00 de desconto |

Para `VALOR_FIXO`, o desconto nunca ultrapassa o valor do plano (mínimo de R$ 0,00 na cobrança).

### 3.2. Validações do Cupom

Quando um cupom é utilizado (no `escolher-plano` ou na `validar-cupom`), o backend valida nesta ordem:

1. **Existência e ativo** — O cupom deve existir e estar com `ativo = true`
2. **Vigência** — `dtInicio <= agora <= dtFim` (campos opcionais, null = sem restrição)
3. **Limite global** — `totalUtilizado < maxUtilizacoes` (null = ilimitado)
4. **Limite por organização** — Conta quantas vezes a org já usou esse cupom vs `maxUtilizacoesPorOrg`
5. **Plano permitido** — `planosPermitidos` contém o plano selecionado (null/vazio = todos)
6. **Segmento permitido** — `segmentosPermitidos` contém o `publicoAlvo` da organização (null/vazio = todos)
7. **Organização permitida** — `organizacoesPermitidas` contém o ID da org (null/vazio = todas)
8. **Ciclo de cobrança** — `cicloCobranca` do cupom é compatível (null = ambos)

Se qualquer validação falhar, o campo `mensagem` da resposta explica o motivo.

### 3.3. Fluxo Completo de Aplicação

```
Frontend: Tela de Escolha de Plano
  │
  ├─ Usuário digita código do cupom
  │
  ├─ POST /v1/assinatura/validar-cupom { codigoCupom, planoCodigo, cicloCobranca }
  │   └─ Resposta mostra: valorOriginal, valorDesconto, valorComDesconto
  │   └─ Frontend exibe preview do desconto em tempo real
  │
  ├─ Usuário confirma
  │
  └─ POST /v1/assinatura/escolher-plano { planoCodigo, cicloCobranca, formaPagamento, codigoCupom }
      │
      ├─ Backend valida cupom novamente (segurança)
      ├─ Calcula valor com desconto
      ├─ Cria assinatura no Assas com valor descontado
      ├─ Salva assinatura (cupomCodigo, valorDesconto)
      ├─ Cria primeira cobrança (valor descontado, valorOriginal, valorDescontoAplicado)
      ├─ Registra CupomUtilizacao + incrementa totalUtilizado
      └─ Cobranças futuras (scheduler mensal) → usa valor original do plano (SEM desconto)
```

### 3.4. Desconto Apenas na Primeira Cobrança

O cupom de desconto é aplicado **exclusivamente na primeira cobrança** da assinatura. O scheduler que gera cobranças mensais/anuais (`gerarCobrancasMensais`) usa `assinatura.valorMensal` / `assinatura.valorAnual`, que é o preço cheio do plano.

---

## Parte 4 — Sugestão de Implementação Frontend

### 4.1. Rota Admin

```
/admin/cupons          → Lista de cupons
/admin/cupons/novo     → Formulário de criação
/admin/cupons/:id      → Detalhe + edição + histórico de uso
```

### 4.2. Página de Listagem de Cupons

**Cards/Badges sugeridos:**

| Campo | Exibição |
|-------|----------|
| `codigo` | Badge com fundo colorido (ex: `BEMVINDO50`) |
| `tipoDesconto` + `valorDesconto` | `50%` ou `R$ 30,00` |
| `vigente` | Badge verde "Vigente" / vermelho "Expirado" / cinza "Inativo" |
| `totalUtilizado` / `maxUtilizacoes` | `12/100 usos` ou `12/∞` |
| `dtInicio` / `dtFim` | `01/03/2026 - 31/03/2026` ou `Sem limite` |
| `ativo` | Toggle ou badge |

**Filtros sugeridos:**
- Status: Todos / Vigentes / Inativos / Expirados
- Botão "Novo Cupom"

### 4.3. Formulário de Criação/Edição

```
┌─────────────────────────────────────────────────────────┐
│  Novo Cupom de Desconto                                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Código *          [________________] (auto UPPERCASE)  │
│  Descrição         [________________]                   │
│                                                         │
│  ── Desconto ───────────────────────                    │
│  Tipo *            (●) Percentual  ( ) Valor Fixo       │
│  Valor *           [________] (% ou R$)                 │
│                                                         │
│  ── Vigência ───────────────────────                    │
│  Data Início       [____/____/________]                 │
│  Data Fim          [____/____/________]                 │
│                                                         │
│  ── Limites ────────────────────────                    │
│  Máx. Utilizações       [____] (vazio = ilimitado)      │
│  Máx. por Organização   [____] (vazio = ilimitado)      │
│                                                         │
│  ── Restrições ─────────────────────                    │
│  Planos Permitidos      [multi-select: basico,plus...]  │
│  Segmentos              [multi-select: Barbearia...]    │
│  Organizações           [multi-select com busca por ID] │
│  Ciclo de Cobrança      [select: Todos/Mensal/Anual]   │
│                                                         │
│              [Cancelar]  [Salvar Cupom]                 │
└─────────────────────────────────────────────────────────┘
```

**Observações do formulário:**
- O campo `codigo` deve ser convertido para uppercase no `onChange` (o backend já faz isso, mas melhor UX)
- Se `tipoDesconto = PERCENTUAL`, o campo valor deve ter sufixo `%` e max 100
- Se `tipoDesconto = VALOR_FIXO`, o campo valor deve ter prefixo `R$`
- Os campos de restrição (planos, segmentos, organizações) são opcionais — se vazio, vale para todos
- Para o multi-select de planos, buscar a lista com `GET /v1/admin/planos`

### 4.4. Página de Detalhe do Cupom

Deve conter:
1. **Header** — Código do cupom, badge de status, botão Editar / Desativar / Ativar
2. **Resumo** — Tipo desconto, valor, vigência, limites, restrições
3. **Barra de progresso** — `totalUtilizado / maxUtilizacoes` (se aplicável)
4. **Tabela de utilizações** — Histórico com colunas: Organização, Plano, Valor Original, Desconto, Valor Final, Data

### 4.5. Integração na Tela de Escolha de Plano (Self-Service)

Na tela onde a organização escolhe/troca de plano, adicionar:

1. **Campo de texto** "Tem um cupom de desconto?" com botão "Aplicar"
2. Ao clicar "Aplicar", chamar `POST /v1/assinatura/validar-cupom`
3. Se `valido = true`:
   - Mostrar badge verde "Cupom aplicado!"
   - Exibir: ~~R$ 99,90~~ **R$ 49,95** (riscado + novo valor)
   - Mostrar texto: "Desconto de 50% aplicado na primeira cobrança"
4. Se `valido = false`:
   - Mostrar badge vermelho com a `mensagem` retornada
   - Manter campo para o usuário tentar outro código
5. Ao confirmar o plano, enviar `codigoCupom` no body do `escolher-plano`

---

## Parte 5 — Exemplos de Request/Response

### 5.1. Criar Cupom (Admin)

**POST** `/v1/admin/cupons`

```json
{
  "codigo": "BEMVINDO50",
  "descricao": "Desconto de boas-vindas - 50% na primeira mensalidade",
  "tipoDesconto": "PERCENTUAL",
  "valorDesconto": 50,
  "dtInicio": "2026-03-01T00:00:00",
  "dtFim": "2026-06-30T23:59:59",
  "maxUtilizacoes": 100,
  "maxUtilizacoesPorOrg": 1,
  "planosPermitidos": ["basico", "plus", "premium"],
  "segmentosPermitidos": null,
  "organizacoesPermitidas": null,
  "cicloCobranca": null
}
```

**Response (201):**
```json
{
  "success": true,
  "message": "Cupom criado com sucesso",
  "dados": {
    "id": 1,
    "codigo": "BEMVINDO50",
    "descricao": "Desconto de boas-vindas - 50% na primeira mensalidade",
    "tipoDesconto": "PERCENTUAL",
    "valorDesconto": 50.00,
    "dtInicio": "2026-03-01T00:00:00",
    "dtFim": "2026-06-30T23:59:59",
    "maxUtilizacoes": 100,
    "maxUtilizacoesPorOrg": 1,
    "totalUtilizado": 0,
    "planosPermitidos": ["basico", "plus", "premium"],
    "segmentosPermitidos": null,
    "organizacoesPermitidas": null,
    "cicloCobranca": null,
    "ativo": true,
    "vigente": true,
    "dtCriacao": "2026-03-03T17:00:00",
    "dtAtualizacao": null
  }
}
```

### 5.2. Criar Cupom Exclusivo para Empresa (Admin)

```json
{
  "codigo": "VIP-SALON123",
  "descricao": "Cupom exclusivo para o Salon 123",
  "tipoDesconto": "VALOR_FIXO",
  "valorDesconto": 30.00,
  "maxUtilizacoes": 1,
  "maxUtilizacoesPorOrg": 1,
  "organizacoesPermitidas": [123],
  "planosPermitidos": ["premium"],
  "cicloCobranca": "MENSAL"
}
```

### 5.3. Atualizar Cupom (Admin)

**PUT** `/v1/admin/cupons/1`

Apenas os campos enviados são atualizados (partial update):

```json
{
  "maxUtilizacoes": 200,
  "dtFim": "2026-12-31T23:59:59"
}
```

### 5.4. Listar Utilizações (Admin)

**GET** `/v1/admin/cupons/1/utilizacoes`

```json
{
  "success": true,
  "message": "Utilizacoes listadas com sucesso",
  "dados": [
    {
      "id": 1,
      "cupomId": 1,
      "cupomCodigo": "BEMVINDO50",
      "organizacaoId": 45,
      "assinaturaId": 12,
      "cobrancaId": 87,
      "valorOriginal": 99.90,
      "valorDesconto": 49.95,
      "valorFinal": 49.95,
      "planoCodigo": "plus",
      "cicloCobranca": "MENSAL",
      "dtUtilizacao": "2026-03-03T15:30:00"
    }
  ]
}
```

### 5.5. Validar Cupom (Self-Service)

**POST** `/v1/assinatura/validar-cupom`

```json
{
  "codigoCupom": "BEMVINDO50",
  "planoCodigo": "plus",
  "cicloCobranca": "MENSAL"
}
```

**Response (cupom válido):**
```json
{
  "success": true,
  "dados": {
    "valido": true,
    "mensagem": "Cupom valido",
    "tipoDesconto": "PERCENTUAL",
    "valorDesconto": 49.95,
    "valorOriginal": 99.90,
    "valorComDesconto": 49.95
  }
}
```

### 5.6. Escolher Plano com Cupom (Self-Service)

**POST** `/v1/assinatura/escolher-plano`

```json
{
  "planoCodigo": "plus",
  "cicloCobranca": "MENSAL",
  "formaPagamento": "PIX",
  "codigoCupom": "BEMVINDO50"
}
```

**Response:** `AssinaturaResponse` padrão, agora incluindo `cupomCodigo` e `valorDesconto`.

### 5.7. Mensagens de Erro de Validação

| Cenário | Mensagem |
|---------|----------|
| Cupom inexistente ou inativo | "Cupom nao encontrado ou inativo" |
| Fora do período | "Cupom fora do periodo de validade" |
| Limite global atingido | "Cupom atingiu o limite maximo de utilizacoes" |
| Limite por org atingido | "Cupom ja utilizado o maximo de vezes para esta organizacao" |
| Plano não permitido | "Cupom nao e valido para o plano selecionado" |
| Segmento não permitido | "Cupom nao e valido para o segmento da sua organizacao" |
| Org não permitida | "Cupom nao e valido para esta organizacao" |
| Ciclo incompatível | "Cupom nao e valido para o ciclo de cobranca selecionado" |

---

## Parte 6 — Modelo de Dados (Referência)

### Tabela `admin.cupom_desconto`

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | `BIGSERIAL` | PK |
| `codigo` | `VARCHAR(50)` | Código único do cupom (UPPERCASE) |
| `descricao` | `VARCHAR(255)` | Descrição interna |
| `tipo_desconto` | `VARCHAR(20)` | `PERCENTUAL` ou `VALOR_FIXO` |
| `valor_desconto` | `NUMERIC(10,2)` | Valor do desconto |
| `dt_inicio` | `TIMESTAMP` | Início da vigência (nullable) |
| `dt_fim` | `TIMESTAMP` | Fim da vigência (nullable) |
| `max_utilizacoes` | `INTEGER` | Limite global de usos (nullable = ilimitado) |
| `max_utilizacoes_por_org` | `INTEGER` | Limite por organização (nullable = ilimitado) |
| `total_utilizado` | `INTEGER` | Contador de usos (default 0) |
| `planos_permitidos` | `JSONB` | `["basico","plus"]` ou null |
| `segmentos_permitidos` | `JSONB` | `["Barbearia"]` ou null |
| `organizacoes_permitidas` | `JSONB` | `[1, 5]` ou null |
| `ciclo_cobranca` | `VARCHAR(10)` | `MENSAL`, `ANUAL` ou null |
| `ativo` | `BOOLEAN` | Soft delete flag |
| `dt_criacao` | `TIMESTAMP` | Auditoria |
| `dt_atualizacao` | `TIMESTAMP` | Auditoria |

### Tabela `admin.cupom_utilizacao`

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | `BIGSERIAL` | PK |
| `cupom_id` | `BIGINT` | FK para cupom_desconto |
| `organizacao_id` | `BIGINT` | Organização que usou |
| `assinatura_id` | `BIGINT` | FK para assinatura |
| `cobranca_id` | `BIGINT` | FK para cobranca_plataforma |
| `valor_original` | `NUMERIC(10,2)` | Preço do plano |
| `valor_desconto` | `NUMERIC(10,2)` | Desconto aplicado |
| `valor_final` | `NUMERIC(10,2)` | Valor cobrado |
| `plano_codigo` | `VARCHAR(50)` | Snapshot do plano |
| `ciclo_cobranca` | `VARCHAR(10)` | Snapshot do ciclo |
| `dt_utilizacao` | `TIMESTAMP` | Data/hora do uso |

### Campos Adicionados em `admin.assinatura`

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `cupom_id` | `BIGINT` | FK para cupom_desconto (nullable) |
| `valor_desconto` | `NUMERIC(10,2)` | Valor do desconto aplicado (nullable) |
| `cupom_codigo` | `VARCHAR(50)` | Código do cupom aplicado (nullable) |

### Campos Adicionados em `admin.cobranca_plataforma`

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `cupom_id` | `BIGINT` | FK para cupom_desconto (nullable) |
| `valor_original` | `NUMERIC(10,2)` | Preço original sem desconto (nullable) |
| `valor_desconto_aplicado` | `NUMERIC(10,2)` | Valor do desconto (nullable) |
| `cupom_codigo` | `VARCHAR(50)` | Código do cupom (nullable) |
