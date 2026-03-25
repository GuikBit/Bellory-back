# Frontend - Guia de Integração com a API de Landing Pages (Opção A)

## O Que Mudou na API

A API agora unifica **site-config** (modo fácil) e **landing-pages** (page builder) em uma única fonte de verdade.

### Fluxo Unificado

```
┌──────────────────────────────────────────────────────────┐
│  ESCRITA                                                  │
│                                                           │
│  PATCH /api/v1/site-config/hero ──┐                      │
│  (modo fácil - formulário)        │                      │
│                                   ├──► LandingPage       │
│  PUT /api/v1/landing-pages/{id}  ──┘    (is_home=true)   │
│  (page builder - avançado)               + seções         │
│                                                           │
├──────────────────────────────────────────────────────────┤
│  LEITURA (pública)                                        │
│                                                           │
│  GET /api/v1/public/site/{slug}/home                      │
│    → Verifica LandingPage publicada (is_home=true)        │
│    → Se existe: usa settings das seções da LandingPage    │
│    → Se não: usa SitePublicoConfig (fallback)             │
│    → Retorna HomePageDTO (sempre o mesmo formato)         │
│                                                           │
│  GET /api/v1/public/site/{slug}/pages                     │ NOVO
│    → Lista landing pages publicadas                       │
│                                                           │
│  GET /api/v1/public/site/{slug}/pages/{pageSlug}          │ NOVO
│    → Retorna landing page completa com seções             │
└──────────────────────────────────────────────────────────┘
```

---

## Novos Endpoints Públicos

### GET `/api/v1/public/site/{slug}/pages`

Lista todas as landing pages publicadas da organização (sem seções).

```json
{
  "success": true,
  "dados": [
    {
      "id": 1,
      "slug": "home",
      "nome": "Página Inicial",
      "tipo": "HOME",
      "isHome": true,
      "status": "PUBLISHED",
      "versao": 3,
      "dtPublicacao": "2025-03-20T14:00:00",
      "dtCriacao": "2025-01-15T10:30:00"
    },
    {
      "id": 5,
      "slug": "promocao-verao",
      "nome": "Promoção de Verão",
      "tipo": "PROMOCAO",
      "isHome": false,
      "status": "PUBLISHED"
    }
  ]
}
```

### GET `/api/v1/public/site/{slug}/pages/{pageSlug}`

Retorna uma landing page completa com todas as seções visíveis.

```json
{
  "success": true,
  "dados": {
    "id": 5,
    "slug": "promocao-verao",
    "nome": "Promoção de Verão",
    "tipo": "PROMOCAO",
    "status": "PUBLISHED",
    "sections": [
      {
        "id": 10,
        "sectionId": "uuid-hero-1",
        "tipo": "HERO",
        "nome": "Banner Principal",
        "ordem": 0,
        "visivel": true,
        "template": null,
        "content": { "layout": "centered", "elements": [...] },
        "styles": { "desktop": {...}, "tablet": {...}, "mobile": {...} },
        "settings": { "title": "Promoção Verão 50% OFF", "backgroundUrl": "..." },
        "animations": { "type": "fadeIn", "duration": 500 }
      }
    ],
    "globalSettings": { "theme": "light", "primaryColor": "#8B5CF6" },
    "seoSettings": { "title": "Promoção Verão - Barbearia", "description": "..." },
    "customCss": null,
    "customJs": null
  }
}
```

---

## Correções Necessárias no Frontend

### 1. Alinhar Tipos TypeScript com a API

A API retorna campos em **camelCase Java padrão**. O frontend precisa usar os mesmos nomes.

**Tipo correto para `LandingPageDTO`:**

```typescript
interface LandingPageDTO {
  id: number;              // NÃO string
  slug: string;
  nome: string;
  descricao?: string;
  tipo: 'HOME' | 'PROMOCAO' | 'EVENTO' | 'CAMPANHA' | 'CUSTOM';
  isHome: boolean;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  sections: LandingPageSectionDTO[];
  globalSettings?: GlobalSettingsDTO;
  seoSettings?: SeoSettingsDTO;
  customCss?: string;
  customJs?: string;
  faviconUrl?: string;
  versao: number;
  dtPublicacao?: string;   // NÃO createdAt
  dtCriacao: string;       // NÃO updatedAt
  dtAtualizacao?: string;
}
```

**Tipo correto para `LandingPageSectionDTO`:**

```typescript
interface LandingPageSectionDTO {
  id: number;
  sectionId: string;
  tipo: SectionType;
  nome?: string;
  ordem: number;           // NÃO order
  visivel: boolean;        // NÃO visible
  template?: string;
  content?: SectionContentDTO;
  styles?: SectionStylesDTO;
  settings?: Record<string, unknown>;
  animations?: AnimationDTO;
  locked?: boolean;
}
```

**Campos que mudaram de nome:**

| Frontend ANTIGO | API REAL | Ação |
|----------------|----------|------|
| `page.id` (string) | `page.id` (number) | Trocar tipo para `number` |
| `page.createdAt` | `page.dtCriacao` | Renomear |
| `page.updatedAt` | `page.dtAtualizacao` | Renomear |
| `page.currentVersion` | `page.versao` | Renomear |
| `section.order` | `section.ordem` | Renomear |
| `section.visible` | `section.visivel` | Renomear |

---

### 2. Remover Fallback para Dados Demo

**Arquivo:** `HomeLandingPage.tsx`

Remover completamente o `demoLandingPage` e o fallback silencioso.

**Antes (bugado):**
```typescript
const pages = pagesData?.pages || [demoLandingPage];
const isDemo = !!error || !pagesData;
```

**Depois (correto):**
```typescript
const pages = pagesData ?? [];

// Se houve erro, mostrar mensagem
if (error) {
  return <ErrorState message="Erro ao carregar páginas" onRetry={refetch} />;
}
```

E no `handleCreatePage`, remover o catch que cria página local. Mostrar toast de erro ao invés.

---

### 3. Corrigir Request de Criação de Página

**Antes (bugado):**
```typescript
const newPage = await createMutation.mutateAsync({
  nome,
  tipo,
  status: 'DRAFT',           // API NÃO aceita status na criação
  globalSettings: {...},
  sections: [],               // API NÃO aceita sections na criação
});
```

**Depois (correto):**
```typescript
const newPage = await createMutation.mutateAsync({
  nome,
  tipo,
  // slug é gerado automaticamente pela API se não informado
  // isHome: tipo === 'HOME' // opcional
});
```

A API `POST /api/v1/landing-pages` aceita apenas: `nome` (obrigatório), `slug`, `descricao`, `tipo`, `isHome`, `templateId`, `globalSettings`, `seoSettings`.

---

### 4. Corrigir IDs nas Operações de CRUD

A API espera `id: Long` (number). O frontend usa `id: string` em alguns lugares.

**Arquivo:** `HomeLandingPage.tsx`

```typescript
// ANTES
const handleDeletePage = async (pageId: string) => { ... }

// DEPOIS
const handleDeletePage = async (pageId: number) => { ... }
```

Atualizar todos os hooks (`useDeleteLandingPage`, `useDuplicateLandingPage`, etc.) para aceitar `number`.

---

### 5. Corrigir Auto-Save - Formato do Payload

O auto-save no `LandingPageEditor.tsx` envia `landingPage` inteiro. A API `PUT /api/v1/landing-pages/{id}` espera:

```typescript
interface UpdateLandingPageRequest {
  nome?: string;
  descricao?: string;
  slug?: string;
  globalSettings?: GlobalSettingsDTO;
  seoSettings?: SeoSettingsDTO;
  customCss?: string;
  customJs?: string;
  faviconUrl?: string;
  sections?: UpdateSectionDTO[];  // Lista completa de seções
  createVersion?: boolean;        // default: true
  versionDescription?: string;
}
```

**Cada seção no update:**
```typescript
interface UpdateSectionDTO {
  sectionId: string;      // UUID da seção
  nome?: string;
  template?: string;
  visivel?: boolean;
  ordem?: number;
  content?: string;       // JSON stringificado
  styles?: string;        // JSON stringificado
  settings?: string;      // JSON stringificado
  animations?: string;
}
```

O frontend precisa transformar o estado do store para esse formato antes de enviar.

---

### 6. Section Editors: Alinhar Nomes de Campos nos Settings

Os section editors (HeroSectionEditor, AboutSectionEditor, etc.) salvam settings com nomes em **português** (`titulo`, `subtitulo`, `descricao`). A API `PATCH /site-config/*` usa nomes em **inglês** (`title`, `subtitle`, `description`).

**Duas opções:**

**Opção A (recomendada):** Mudar os section editors para usar nomes em inglês, alinhando com os request DTOs da API:

```typescript
// HeroSectionEditor - ANTES
update({ titulo: e.target.value })

// HeroSectionEditor - DEPOIS
update({ title: e.target.value })
```

**Opção B:** Manter português e fazer mapeamento no EditorSiteProvider. Mais frágil.

**Mapeamento completo por seção:**

**HERO:**
| Editor (pt) | API/Settings (en) |
|---|---|
| `titulo` | `title` |
| `subtitulo` | `subtitle` |
| `backgroundImage` | `backgroundUrl` |
| `backgroundOverlay` | `backgroundOverlay` |
| `buttons` | `buttons` |
| `showStats` | `showBookingForm` |

**ABOUT:**
| Editor (pt) | API/Settings (en) |
|---|---|
| `titulo` | `title` |
| `descricao` | `description` |
| `imagem` | `imageUrl` |
| `videoUrl` | `videoUrl` |
| `missao` | `mission` |
| `visao` | `vision` |
| `valores` | `values` |
| `highlights` | `highlights` |

**SERVICES:**
| Editor (pt) | API/Settings (en) |
|---|---|
| `titulo` | `sectionTitle` |
| `subtitulo` | `sectionSubtitle` |
| `showPrices` | `showPrices` |
| `showDuration` | `showDuration` |
| `maxItems` | `featuredLimit` |
| `categoryFilter` | (não existe na API) |

**HEADER:**
| Editor (pt) | API/Settings (en) |
|---|---|
| `logoUrl` | `logoUrl` |
| `menuItems` | `menuItems` |
| `actionButtons` | `actionButtons` |
| `showPhone` | `showPhone` |
| `showSocial` | `showSocial` |
| `sticky` | `sticky` |

**FOOTER:**
| Editor (pt) | API/Settings (en) |
|---|---|
| `copyrightText` | `copyrightText` |
| `showHours` | `showHours` |
| `showSocial` | `showSocial` |
| `showNewsletter` | `showNewsletter` |
| `linkSections` | `linkSections` |

---

### 7. EditorSiteProvider - Simplificar Merge

Após alinhar os nomes dos campos (item 6), o `EditorSiteProvider.tsx` pode usar um merge genérico ao invés de mapear campo a campo.

**Antes (frágil):**
```typescript
const mergedHero = homeData.hero ? {
  ...homeData.hero,
  ...(heroSettings.titulo && { title: heroSettings.titulo }),
  ...(heroSettings.subtitulo && { subtitle: heroSettings.subtitulo }),
  // ... 10+ linhas de mapeamento manual
} : null;
```

**Depois (limpo):**
```typescript
const mergedHero = homeData.hero ? {
  ...homeData.hero,
  ...heroSettings,  // Nomes já batem, merge direto
} : null;
```

---

### 8. Implementar Botões Publicar/Despublicar

**Arquivo:** `HomeLandingPage.tsx` (header do editor) e `LandingPageEditor.tsx`

Adicionar no header do editor:

```typescript
// Hooks
const publishMutation = usePublishLandingPage();
const unpublishMutation = useUnpublishLandingPage();

// No JSX do header
{selectedPage.status === 'DRAFT' && (
  <BarbeariaButton
    variant="primary"
    size="sm"
    onClick={() => publishMutation.mutateAsync(selectedPage.id)}
    disabled={publishMutation.isPending}
  >
    Publicar
  </BarbeariaButton>
)}

{selectedPage.status === 'PUBLISHED' && (
  <BarbeariaButton
    variant="outline"
    size="sm"
    onClick={() => unpublishMutation.mutateAsync(selectedPage.id)}
  >
    Despublicar
  </BarbeariaButton>
)}
```

**Endpoints da API:**
- `POST /api/v1/landing-pages/{id}/publish` → muda status para PUBLISHED
- `POST /api/v1/landing-pages/{id}/unpublish` → muda status para DRAFT

---

### 9. Implementar Drag-and-Drop de Seções

**Arquivo:** `SectionListSidebar.tsx`

Instalar `@dnd-kit/core` e `@dnd-kit/sortable` e substituir o drag nativo.

Ao finalizar o drag, chamar:
```typescript
// Reordenar via API
await api.put(`/api/v1/landing-pages/${pageId}/sections/reorder`, {
  sectionIds: newOrder.map(s => s.sectionId)
});

// Ou atualizar local e deixar o auto-save sincronizar
editor.reorderSections(newOrder);
```

---

### 10. Renderização de Landing Pages Extras no Site Público

Para renderizar landing pages que **não são a home** (promoções, eventos, etc.), adicionar rota no frontend:

```typescript
// Routing.tsx
<Route path="/s/:slug/p/:pageSlug" element={<LandingPageView />} />
```

O componente `LandingPageView`:
```typescript
function LandingPageView() {
  const { slug, pageSlug } = useParams();
  const { data } = useQuery(['landing-page', slug, pageSlug], () =>
    api.get(`/api/v1/public/site/${slug}/pages/${pageSlug}`)
  );

  // Renderiza seções da landing page usando SectionPreviewRenderer
  // ou componentes reais baseados no tipo
}
```

---

### 11. Hooks de API Necessários

Criar/atualizar os hooks em `useLandingPage.ts`:

```typescript
// Já devem existir
useLandingPages()                    // GET /api/v1/landing-pages
useCreateLandingPage()               // POST /api/v1/landing-pages
useUpdateLandingPage()               // PUT /api/v1/landing-pages/{id}
useDeleteLandingPage()               // DELETE /api/v1/landing-pages/{id}
useDuplicateLandingPage()            // POST /api/v1/landing-pages/{id}/duplicate

// Adicionar
usePublishLandingPage()              // POST /api/v1/landing-pages/{id}/publish
useUnpublishLandingPage()            // POST /api/v1/landing-pages/{id}/unpublish
useLandingPageVersions(pageId)       // GET /api/v1/landing-pages/{id}/versions
useRestoreVersion()                  // POST /api/v1/landing-pages/{id}/versions/{v}/restore

// Públicos (sem auth)
usePublicLandingPages(slug)          // GET /api/v1/public/site/{slug}/pages
usePublicLandingPage(slug, pageSlug) // GET /api/v1/public/site/{slug}/pages/{pageSlug}
```

---

## Resumo de Prioridades

| # | Tarefa | Impacto | Esforço |
|---|--------|---------|---------|
| 1 | Alinhar tipos TypeScript (id: number, ordem, visivel, dtCriacao) | Crítico | Médio |
| 2 | Remover fallback demo / mostrar erros reais | Crítico | Baixo |
| 3 | Corrigir payload de criação de página | Crítico | Baixo |
| 4 | Alinhar nomes nos Section Editors (pt → en) | Alto | Médio |
| 5 | Corrigir auto-save payload format | Alto | Médio |
| 6 | Simplificar EditorSiteProvider merge | Alto | Médio |
| 7 | Botões publicar/despublicar | Alto | Baixo |
| 8 | Drag-and-drop de seções funcional | Médio | Médio |
| 9 | Rota pública para landing pages extras | Médio | Baixo |
| 10 | Hooks de API completos | Médio | Baixo |
