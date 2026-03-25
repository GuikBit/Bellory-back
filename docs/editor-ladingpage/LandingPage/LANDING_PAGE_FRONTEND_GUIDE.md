# Landing Page - Guia Completo para o Frontend

> Documento de referencia para integracao do frontend com a API de Landing Pages do Bellory.

---

## Sumario

1. [Visao Geral](#1-visao-geral)
2. [Endpoints da API](#2-endpoints-da-api)
3. [Modelos de Dados (DTOs)](#3-modelos-de-dados-dtos)
4. [Fluxos Principais](#4-fluxos-principais)
5. [Tipos de Secoes](#5-tipos-de-secoes)
6. [Tipos de Elementos](#6-tipos-de-elementos)
7. [Estrutura JSON das Secoes](#7-estrutura-json-das-secoes)
8. [Estilos Responsivos](#8-estilos-responsivos)
9. [Sistema de Versoes](#9-sistema-de-versoes)
10. [Variaveis Dinamicas](#10-variaveis-dinamicas)
11. [Exemplos Praticos Completos](#11-exemplos-praticos-completos)

---

## 1. Visao Geral

O sistema de Landing Pages permite criar, editar e publicar paginas personalizadas por organizacao. Cada landing page e composta por **secoes** ordenadas, e cada secao contem **elementos** flexiveis armazenados em JSON.

### Arquitetura

```
LandingPage (1)
  ├── globalSettings (JSON: tema, cores, fontes)
  ├── seoSettings (JSON: meta tags, OG, etc.)
  ├── customCss / customJs
  └── sections (N) - ordenadas por "ordem"
        ├── content (JSON: layout, background, elements[])
        ├── styles (JSON: desktop, tablet, mobile)
        ├── settings (JSON: config especifica)
        ├── animations (JSON: animacoes)
        ├── visibilityRules (JSON: regras de visibilidade)
        └── dataSource (JSON: fonte de dados dinamicos)

LandingPageVersion (historico)
  └── snapshot (JSON: copia completa da pagina)
```

### Ciclo de Vida

```
DRAFT  ──(publish)──>  PUBLISHED  ──(unpublish)──>  DRAFT
                                                      │
ARCHIVED  <──────────────────────────────────────────-┘
```

---

## 2. Endpoints da API

**Base URL:** `/api/v1/landing-pages`

Todos os endpoints requerem autenticacao JWT e retornam o formato:

```json
{
  "success": true,
  "message": "Mensagem de sucesso",
  "data": { ... },
  "errorCode": null
}
```

### 2.1 Landing Page - CRUD

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `GET` | `/` | Listar todas as landing pages |
| `GET` | `/paginated?page=0&size=10` | Listar com paginacao |
| `GET` | `/{id}` | Buscar por ID (com secoes) |
| `GET` | `/by-slug/{slug}` | Buscar por slug (com secoes) |
| `POST` | `/` | Criar nova landing page |
| `PUT` | `/{id}` | Atualizar landing page |
| `DELETE` | `/{id}` | Deletar (soft delete) |

### 2.2 Landing Page - Acoes

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `POST` | `/{id}/publish` | Publicar a pagina |
| `POST` | `/{id}/unpublish` | Despublicar (voltar para DRAFT) |
| `POST` | `/{id}/duplicate?novoNome=Copia` | Duplicar a pagina |

### 2.3 Secoes

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `POST` | `/{landingPageId}/sections` | Adicionar secao |
| `PUT` | `/{landingPageId}/sections/{sectionId}` | Atualizar secao |
| `PUT` | `/{landingPageId}/sections/reorder` | Reordenar secoes |
| `POST` | `/{landingPageId}/sections/{sectionId}/duplicate` | Duplicar secao |
| `DELETE` | `/{landingPageId}/sections/{sectionId}` | Deletar secao |

> **Nota:** O `{sectionId}` nos endpoints de secao e o campo `sectionId` (UUID string), **nao** o `id` numerico.

### 2.4 Versoes

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `GET` | `/{landingPageId}/versions` | Listar historico de versoes |
| `POST` | `/{landingPageId}/versions/{versao}/restore` | Restaurar uma versao |

### 2.5 Metadata (Enums)

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `GET` | `/metadata/section-types` | Tipos de secoes disponiveis |
| `GET` | `/metadata/element-types` | Tipos de elementos disponiveis |

---

## 3. Modelos de Dados (DTOs)

### 3.1 LandingPageDTO (Resposta)

```typescript
interface LandingPageDTO {
  id: number;
  slug: string;
  nome: string;
  descricao: string | null;
  tipo: "HOME" | "PROMOCAO" | "EVENTO" | "CAMPANHA" | "CUSTOM";
  isHome: boolean;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  sections: LandingPageSectionDTO[];
  globalSettings: GlobalSettingsDTO | null;
  seoSettings: SeoSettingsDTO | null;
  customCss: string | null;
  customJs: string | null;
  faviconUrl: string | null;
  versao: number;
  dtPublicacao: string | null;    // ISO datetime
  publicadoPor: number | null;
  ativo: boolean;
  dtCriacao: string;              // ISO datetime
  dtAtualizacao: string | null;   // ISO datetime
}
```

### 3.2 GlobalSettingsDTO

```typescript
interface GlobalSettingsDTO {
  theme: string | null;            // "light", "dark", "custom"
  primaryColor: string | null;     // "#6366f1"
  secondaryColor: string | null;
  accentColor: string | null;
  backgroundColor: string | null;
  textColor: string | null;
  fontFamily: string | null;       // "Inter, sans-serif"
  headingFontFamily: string | null;
  borderRadius: string | null;     // "8px"
  customVariables: Record<string, any> | null;
}
```

### 3.3 SeoSettingsDTO

```typescript
interface SeoSettingsDTO {
  title: string | null;
  description: string | null;
  keywords: string | null;
  ogTitle: string | null;
  ogDescription: string | null;
  ogImage: string | null;
  twitterCard: string | null;
  canonicalUrl: string | null;
  noIndex: boolean;
  noFollow: boolean;
  customMeta: Record<string, string> | null;
}
```

### 3.4 LandingPageSectionDTO

```typescript
interface LandingPageSectionDTO {
  id: number;
  sectionId: string;       // UUID - usar este nos endpoints
  tipo: string;            // SectionType enum value
  nome: string;
  ordem: number;
  visivel: boolean;
  template: string | null; // "hero-centered", "hero-split", etc.
  content: SectionContentDTO | null;
  styles: SectionStylesDTO | null;
  settings: Record<string, any> | null;
  animations: AnimationDTO | null;
  visibilityRules: VisibilityRulesDTO | null;
  dataSource: DataSourceDTO | null;
  locked: boolean;
  ativo: boolean;
  dtCriacao: string;
  dtAtualizacao: string | null;
}
```

### 3.5 SectionContentDTO

```typescript
interface SectionContentDTO {
  layout: string | null;           // "full-width", "contained", "split"
  background: BackgroundDTO | null;
  elements: ElementDTO[];
  container: ContainerDTO | null;
}

interface BackgroundDTO {
  type: "color" | "image" | "video" | "gradient" | null;
  color: string | null;
  imageUrl: string | null;
  videoUrl: string | null;
  gradient: string | null;         // "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
  overlay: boolean;
  overlayColor: string | null;     // "rgba(0,0,0,0.5)"
  position: string | null;        // "center center"
  size: string | null;             // "cover"
  repeat: string | null;           // "no-repeat"
  attachment: string | null;       // "fixed", "scroll"
}

interface ContainerDTO {
  maxWidth: string | null;         // "1200px"
  centered: boolean;
  padding: string | null;          // "0 20px"
}
```

### 3.6 ElementDTO

Este e o bloco fundamental de construcao. Cada elemento e recursivo (pode ter `children`).

```typescript
interface ElementDTO {
  id: string;                     // UUID
  type: string;                   // ElementType enum value
  tag: string | null;             // "h1", "h2", "p", "span", "div"
  content: string | null;         // Texto do elemento (suporta variaveis)
  htmlContent: string | null;     // Para RICH_TEXT
  url: string | null;             // URL de imagem, video ou link
  urlMobile: string | null;       // URL alternativa para mobile
  alt: string | null;             // Alt text para imagens
  action: ActionDTO | null;       // Acao ao clicar
  icon: IconDTO | null;           // Icone do elemento
  children: ElementDTO[] | null;  // Elementos filhos (recursivo)
  styles: ElementStylesDTO | null;
  variant: string | null;         // "primary", "secondary", "outline", "ghost"
  size: string | null;            // "xs", "sm", "md", "lg", "xl"
  props: Record<string, any> | null;  // Props especificas do tipo
  animation: AnimationDTO | null;
  visible: boolean;
  visibilityRules: VisibilityRulesDTO | null;
  dataBinding: string | null;     // Referencia a campo dinamico
  condition: string | null;       // Condicao de renderizacao
  order: number | null;
}
```

### 3.7 ActionDTO

```typescript
interface ActionDTO {
  type: "link" | "scroll" | "modal" | "submit" | "whatsapp" | "phone" | "email" | "external" | null;
  href: string | null;            // URL ou #sectionId
  target: string | null;          // "_blank", "_self"
  modalId: string | null;         // ID do modal a abrir
  message: string | null;         // Mensagem para WhatsApp
  trackingEvent: string | null;   // Evento de analytics
}
```

### 3.8 IconDTO

```typescript
interface IconDTO {
  name: string | null;            // "phone", "mail", "star"
  library: "lucide" | "heroicons" | "fontawesome" | "custom" | null;
  position: string | null;        // "left", "right"
  size: string | null;            // "16px", "24px"
  color: string | null;
  customUrl: string | null;       // URL para icone customizado
}
```

### 3.9 Estilos dos Elementos

```typescript
interface ElementStylesDTO {
  desktop: ElementResponsiveStyleDTO | null;
  tablet: ElementResponsiveStyleDTO | null;
  mobile: ElementResponsiveStyleDTO | null;
}

interface ElementResponsiveStyleDTO {
  // Layout
  display: string | null;
  position: string | null;
  top: string | null;
  right: string | null;
  bottom: string | null;
  left: string | null;
  zIndex: string | null;
  overflow: string | null;

  // Flexbox
  flexDirection: string | null;
  justifyContent: string | null;
  alignItems: string | null;
  flexWrap: string | null;
  gap: string | null;
  flex: string | null;

  // Grid
  gridTemplateColumns: string | null;
  gridTemplateRows: string | null;
  gridColumn: string | null;
  gridRow: string | null;
  gridGap: string | null;

  // Sizing
  width: string | null;
  height: string | null;
  minWidth: string | null;
  maxWidth: string | null;
  minHeight: string | null;
  maxHeight: string | null;

  // Spacing
  margin: string | null;
  marginTop: string | null;
  marginRight: string | null;
  marginBottom: string | null;
  marginLeft: string | null;
  padding: string | null;
  paddingTop: string | null;
  paddingRight: string | null;
  paddingBottom: string | null;
  paddingLeft: string | null;

  // Typography
  fontSize: string | null;
  fontWeight: string | null;
  fontFamily: string | null;
  lineHeight: string | null;
  letterSpacing: string | null;
  textAlign: string | null;
  textTransform: string | null;
  textDecoration: string | null;
  color: string | null;

  // Background
  backgroundColor: string | null;
  backgroundImage: string | null;
  backgroundSize: string | null;
  backgroundPosition: string | null;

  // Border
  border: string | null;
  borderTop: string | null;
  borderRight: string | null;
  borderBottom: string | null;
  borderLeft: string | null;
  borderRadius: string | null;
  borderColor: string | null;

  // Effects
  boxShadow: string | null;
  opacity: string | null;
  transform: string | null;
  transition: string | null;
  cursor: string | null;
  filter: string | null;
  backdropFilter: string | null;

  // Custom
  customStyles: Record<string, string> | null;
}
```

### 3.10 Outros DTOs

```typescript
interface AnimationDTO {
  type: string | null;       // "fadeIn", "slideUp", "zoomIn", etc.
  duration: number | null;   // em ms
  delay: number | null;      // em ms
  easing: string | null;     // "ease", "ease-in-out", "cubic-bezier(...)"
  repeat: boolean;
}

interface VisibilityRulesDTO {
  showOnDesktop: boolean;    // default: true
  showOnTablet: boolean;     // default: true
  showOnMobile: boolean;     // default: true
  startDate: string | null;  // ISO datetime
  endDate: string | null;    // ISO datetime
}

interface DataSourceDTO {
  source: string | null;     // "services", "products", "team", "testimonials"
  filter: Record<string, any> | null;
  limit: number | null;
  orderBy: string | null;
  orderDirection: string | null;  // "ASC", "DESC"
}
```

---

## 4. Fluxos Principais

### 4.1 Criar uma Landing Page

```http
POST /api/v1/landing-pages
Content-Type: application/json
Authorization: Bearer {jwt}
```

```json
{
  "nome": "Pagina Inicial",
  "slug": "home",
  "descricao": "Pagina principal do site",
  "tipo": "HOME",
  "isHome": true,
  "globalSettings": {
    "theme": "light",
    "primaryColor": "#6366f1",
    "secondaryColor": "#8b5cf6",
    "accentColor": "#f59e0b",
    "backgroundColor": "#ffffff",
    "textColor": "#1f2937",
    "fontFamily": "Inter, sans-serif",
    "headingFontFamily": "Poppins, sans-serif",
    "borderRadius": "8px"
  },
  "seoSettings": {
    "title": "Minha Empresa - Servicos",
    "description": "Descricao para SEO",
    "ogTitle": "Minha Empresa",
    "ogDescription": "Descricao para redes sociais"
  }
}
```

**Resposta:** Retorna `LandingPageDTO` completo com secoes padrao criadas automaticamente:
- HEADER, HERO, ABOUT, SERVICES, TEAM, BOOKING, FOOTER

> **Nota:** Se `slug` nao for informado, sera gerado automaticamente a partir do `nome`.
> Se `isHome: true`, qualquer outra pagina que era home perde esse status.

### 4.2 Atualizar uma Landing Page

```http
PUT /api/v1/landing-pages/{id}
```

```json
{
  "nome": "Pagina Atualizada",
  "slug": "home",
  "globalSettings": {
    "theme": "dark",
    "primaryColor": "#10b981",
    "backgroundColor": "#0f172a",
    "textColor": "#f8fafc"
  },
  "seoSettings": {
    "title": "Novo Titulo SEO"
  },
  "customCss": ".hero-section { min-height: 100vh; }",
  "createVersion": true,
  "versionDescription": "Mudanca para tema escuro"
}
```

> **`createVersion`** (default: `true`): Quando `true`, cria automaticamente um snapshot da versao anterior antes de aplicar as alteracoes. Util para poder restaurar depois.

### 4.3 Adicionar uma Secao

```http
POST /api/v1/landing-pages/{landingPageId}/sections
```

```json
{
  "tipo": "TESTIMONIALS",
  "nome": "Depoimentos",
  "template": "testimonials-carousel",
  "posicao": 4,
  "content": {
    "layout": "contained",
    "background": {
      "type": "color",
      "color": "#f9fafb"
    },
    "container": {
      "maxWidth": "1200px",
      "centered": true
    },
    "elements": [
      {
        "id": "heading-1",
        "type": "heading",
        "tag": "h2",
        "content": "O que nossos clientes dizem",
        "styles": {
          "desktop": {
            "textAlign": "center",
            "fontSize": "36px",
            "fontWeight": "700",
            "marginBottom": "48px"
          }
        }
      },
      {
        "id": "testimonials-list",
        "type": "testimonial_list",
        "props": {
          "layout": "carousel",
          "autoplay": true,
          "interval": 5000,
          "showAvatar": true,
          "showRating": true
        }
      }
    ]
  }
}
```

> **`posicao`**: Indice onde inserir a secao (0-based). As secoes existentes com `ordem >= posicao` sao deslocadas automaticamente. Se omitido, insere no final.

### 4.4 Atualizar uma Secao

```http
PUT /api/v1/landing-pages/{landingPageId}/sections/{sectionId}
```

O `{sectionId}` e o UUID (campo `sectionId` do DTO), **nao** o `id` numerico.

```json
{
  "nome": "Hero Atualizado",
  "visivel": true,
  "content": {
    "layout": "full-width",
    "background": {
      "type": "image",
      "imageUrl": "https://exemplo.com/hero-bg.jpg",
      "overlay": true,
      "overlayColor": "rgba(0,0,0,0.6)",
      "position": "center center",
      "size": "cover"
    },
    "elements": [
      {
        "id": "hero-title",
        "type": "heading",
        "tag": "h1",
        "content": "Bem-vindo a {empresa}",
        "styles": {
          "desktop": {
            "fontSize": "56px",
            "fontWeight": "800",
            "color": "#ffffff",
            "textAlign": "center",
            "marginBottom": "24px"
          },
          "tablet": {
            "fontSize": "40px"
          },
          "mobile": {
            "fontSize": "28px"
          }
        }
      },
      {
        "id": "hero-cta",
        "type": "button",
        "content": "Agendar Agora",
        "variant": "primary",
        "size": "lg",
        "action": {
          "type": "scroll",
          "href": "#booking"
        },
        "icon": {
          "name": "calendar",
          "library": "lucide",
          "position": "left"
        },
        "styles": {
          "desktop": {
            "padding": "16px 48px",
            "fontSize": "18px",
            "borderRadius": "9999px"
          }
        }
      }
    ]
  },
  "animations": {
    "type": "fadeIn",
    "duration": 800,
    "delay": 200,
    "easing": "ease-out",
    "repeat": false
  },
  "visibilityRules": {
    "showOnDesktop": true,
    "showOnTablet": true,
    "showOnMobile": true
  }
}
```

### 4.5 Reordenar Secoes

```http
PUT /api/v1/landing-pages/{landingPageId}/sections/reorder
```

```json
{
  "sectionIds": [
    "uuid-header",
    "uuid-hero",
    "uuid-services",
    "uuid-about",
    "uuid-testimonials",
    "uuid-booking",
    "uuid-footer"
  ]
}
```

> A nova ordem e definida pela posicao no array. O backend atualiza o campo `ordem` de cada secao.

### 4.6 Publicar

```http
POST /api/v1/landing-pages/{id}/publish
```

Sem body. Muda o status para `PUBLISHED`, registra `dtPublicacao` e `publicadoPor`, e cria uma versao do tipo `PUBLISH`.

### 4.7 Duplicar Pagina

```http
POST /api/v1/landing-pages/{id}/duplicate?novoNome=Copia da Pagina
```

Cria uma copia completa da pagina (com todas as secoes), com status `DRAFT` e `isHome: false`.

### 4.8 Restaurar Versao

```http
POST /api/v1/landing-pages/{landingPageId}/versions/{versao}/restore
```

Restaura a pagina para o estado do snapshot da versao especificada. Todas as secoes atuais sao substituidas. Uma nova versao e criada registrando a restauracao.

---

## 5. Tipos de Secoes

Disponivel via `GET /api/v1/landing-pages/metadata/section-types`

| Tipo | Label | Descricao |
|------|-------|-----------|
| `HEADER` | Cabecalho | Menu e barra de navegacao |
| `FOOTER` | Rodape | Rodape com informacoes de contato |
| `HERO` | Hero / Banner | Banner principal de apresentacao |
| `ABOUT` | Sobre | Secao sobre a empresa/profissional |
| `SERVICES` | Servicos | Lista de servicos oferecidos |
| `PRODUCTS` | Produtos | Lista de produtos |
| `PRICING` | Precos | Tabela de precos/planos |
| `TEAM` | Equipe | Membros da equipe |
| `TESTIMONIALS` | Depoimentos | Depoimentos de clientes |
| `GALLERY` | Galeria | Galeria de imagens/videos |
| `CTA` | Call to Action | Secao de chamada para acao |
| `BOOKING` | Agendamento | Formulario de agendamento |
| `CONTACT` | Contato | Formulario de contato |
| `NEWSLETTER` | Newsletter | Cadastro de newsletter |
| `FAQ` | FAQ | Perguntas frequentes |
| `FEATURES` | Features | Lista de funcionalidades |
| `STATS` | Estatisticas | Numeros e metricas |
| `TIMELINE` | Timeline | Linha do tempo / historico |
| `VIDEO` | Video | Secao de video |
| `MAP` | Mapa | Mapa de localizacao |
| `CAROUSEL` | Carrossel | Slider de conteudo |
| `TABS` | Abas | Conteudo em abas |
| `ACCORDION` | Acordeao | Conteudo expansivel |
| `CUSTOM` | Personalizado | Secao totalmente customizada |
| `HTML` | HTML Livre | HTML/CSS livre |

---

## 6. Tipos de Elementos

Disponivel via `GET /api/v1/landing-pages/metadata/element-types`

### Texto
`HEADING`, `PARAGRAPH`, `TEXT`, `RICH_TEXT`, `QUOTE`, `LIST`

### Midia
`IMAGE`, `VIDEO`, `ICON`, `AVATAR`, `LOGO`, `GALLERY`, `BACKGROUND`

### Interativos
`BUTTON`, `LINK`, `MENU`, `DROPDOWN`, `TABS`, `ACCORDION`, `MODAL_TRIGGER`

### Formulario
`FORM`, `INPUT`, `TEXTAREA`, `SELECT`, `CHECKBOX`, `RADIO`, `DATE_PICKER`, `TIME_PICKER`, `FILE_UPLOAD`

### Layout
`CONTAINER`, `ROW`, `COLUMN`, `GRID`, `FLEX`, `SPACER`, `DIVIDER`, `SECTION`

### Cards
`CARD`, `SERVICE_CARD`, `PRODUCT_CARD`, `TEAM_CARD`, `TESTIMONIAL_CARD`, `PRICING_CARD`, `FEATURE_CARD`

### Dados Dinamicos
`SERVICE_LIST`, `PRODUCT_LIST`, `TEAM_LIST`, `TESTIMONIAL_LIST`, `BOOKING_FORM`, `CONTACT_FORM`

### Widgets
`SOCIAL_LINKS`, `RATING`, `BADGE`, `TAG`, `PROGRESS`, `COUNTER`, `COUNTDOWN`, `MAP`, `WHATSAPP_BUTTON`

### Especiais
`CAROUSEL`, `SLIDER`, `MARQUEE`, `EMBED`, `HTML`, `SCRIPT`

---

## 7. Estrutura JSON das Secoes

### 7.1 HERO Section

```json
{
  "content": {
    "layout": "full-width",
    "background": {
      "type": "image",
      "imageUrl": "https://exemplo.com/hero.jpg",
      "overlay": true,
      "overlayColor": "rgba(0,0,0,0.5)",
      "size": "cover",
      "position": "center center",
      "attachment": "fixed"
    },
    "container": {
      "maxWidth": "1200px",
      "centered": true,
      "padding": "0 20px"
    },
    "elements": [
      {
        "id": "uuid-1",
        "type": "heading",
        "tag": "h1",
        "content": "Titulo Principal",
        "order": 0,
        "visible": true,
        "styles": {
          "desktop": { "fontSize": "56px", "fontWeight": "800", "color": "#fff" },
          "tablet": { "fontSize": "40px" },
          "mobile": { "fontSize": "28px" }
        }
      },
      {
        "id": "uuid-2",
        "type": "paragraph",
        "tag": "p",
        "content": "Subtitulo descritivo da pagina",
        "order": 1,
        "visible": true,
        "styles": {
          "desktop": { "fontSize": "20px", "color": "rgba(255,255,255,0.9)", "maxWidth": "600px" }
        }
      },
      {
        "id": "uuid-3",
        "type": "button",
        "content": "Agendar Agora",
        "variant": "primary",
        "size": "lg",
        "order": 2,
        "action": { "type": "scroll", "href": "#booking" },
        "icon": { "name": "calendar", "library": "lucide", "position": "left" }
      }
    ]
  }
}
```

### 7.2 HEADER Section

```json
{
  "content": {
    "layout": "full-width",
    "background": {
      "type": "color",
      "color": "#ffffff"
    },
    "elements": [
      {
        "id": "uuid-logo",
        "type": "logo",
        "url": "{logo_url}",
        "alt": "{empresa}",
        "styles": {
          "desktop": { "height": "40px" }
        }
      },
      {
        "id": "uuid-menu",
        "type": "menu",
        "children": [
          {
            "id": "uuid-link-1",
            "type": "link",
            "content": "Inicio",
            "action": { "type": "scroll", "href": "#hero" }
          },
          {
            "id": "uuid-link-2",
            "type": "link",
            "content": "Servicos",
            "action": { "type": "scroll", "href": "#services" }
          },
          {
            "id": "uuid-link-3",
            "type": "link",
            "content": "Sobre",
            "action": { "type": "scroll", "href": "#about" }
          },
          {
            "id": "uuid-link-4",
            "type": "link",
            "content": "Contato",
            "action": { "type": "scroll", "href": "#contact" }
          }
        ]
      },
      {
        "id": "uuid-cta",
        "type": "button",
        "content": "Agendar",
        "variant": "primary",
        "action": { "type": "scroll", "href": "#booking" }
      }
    ]
  }
}
```

### 7.3 SERVICES Section (com dados dinamicos)

```json
{
  "content": {
    "layout": "contained",
    "background": { "type": "color", "color": "#f9fafb" },
    "container": { "maxWidth": "1200px", "centered": true },
    "elements": [
      {
        "id": "uuid-title",
        "type": "heading",
        "tag": "h2",
        "content": "Nossos Servicos",
        "styles": {
          "desktop": { "textAlign": "center", "marginBottom": "48px" }
        }
      },
      {
        "id": "uuid-service-list",
        "type": "service_list",
        "props": {
          "layout": "grid",
          "columns": 3,
          "showImage": true,
          "showPrice": true,
          "showDuration": true,
          "showDescription": true,
          "cardVariant": "elevated"
        }
      }
    ]
  },
  "dataSource": {
    "source": "services",
    "limit": 6,
    "orderBy": "nome",
    "orderDirection": "ASC",
    "filter": { "ativo": true }
  }
}
```

### 7.4 TEAM Section

```json
{
  "content": {
    "layout": "contained",
    "elements": [
      {
        "id": "uuid-title",
        "type": "heading",
        "tag": "h2",
        "content": "Nossa Equipe"
      },
      {
        "id": "uuid-team-list",
        "type": "team_list",
        "props": {
          "layout": "grid",
          "columns": 4,
          "showAvatar": true,
          "showRole": true,
          "showBio": true,
          "avatarShape": "circle"
        }
      }
    ]
  },
  "dataSource": {
    "source": "team",
    "orderBy": "nome",
    "orderDirection": "ASC"
  }
}
```

### 7.5 FAQ Section

```json
{
  "content": {
    "layout": "contained",
    "container": { "maxWidth": "800px", "centered": true },
    "elements": [
      {
        "id": "uuid-title",
        "type": "heading",
        "tag": "h2",
        "content": "Perguntas Frequentes"
      },
      {
        "id": "uuid-faq-1",
        "type": "accordion",
        "children": [
          {
            "id": "uuid-q1",
            "type": "card",
            "props": {
              "question": "Como funciona o agendamento?",
              "answer": "Voce pode agendar online pelo nosso site...",
              "defaultOpen": false
            }
          },
          {
            "id": "uuid-q2",
            "type": "card",
            "props": {
              "question": "Quais formas de pagamento?",
              "answer": "Aceitamos cartao, pix e dinheiro."
            }
          }
        ]
      }
    ]
  }
}
```

### 7.6 CTA Section

```json
{
  "content": {
    "layout": "full-width",
    "background": {
      "type": "gradient",
      "gradient": "linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)"
    },
    "container": { "maxWidth": "800px", "centered": true },
    "elements": [
      {
        "id": "uuid-title",
        "type": "heading",
        "tag": "h2",
        "content": "Pronto para agendar?",
        "styles": {
          "desktop": { "color": "#ffffff", "textAlign": "center" }
        }
      },
      {
        "id": "uuid-desc",
        "type": "paragraph",
        "content": "Faca seu agendamento agora e garanta seu horario.",
        "styles": {
          "desktop": { "color": "rgba(255,255,255,0.9)", "textAlign": "center" }
        }
      },
      {
        "id": "uuid-btn",
        "type": "button",
        "content": "Agendar Agora",
        "variant": "secondary",
        "size": "xl",
        "action": { "type": "whatsapp", "message": "Ola! Gostaria de agendar um horario." },
        "icon": { "name": "message-circle", "library": "lucide", "position": "left" }
      }
    ]
  }
}
```

### 7.7 FOOTER Section

```json
{
  "content": {
    "layout": "full-width",
    "background": { "type": "color", "color": "#1f2937" },
    "elements": [
      {
        "id": "uuid-logo",
        "type": "logo",
        "url": "{logo_url}",
        "styles": { "desktop": { "height": "32px", "marginBottom": "16px" } }
      },
      {
        "id": "uuid-contact-info",
        "type": "container",
        "children": [
          {
            "id": "uuid-phone",
            "type": "link",
            "content": "{telefone}",
            "action": { "type": "phone" },
            "icon": { "name": "phone", "library": "lucide", "position": "left" }
          },
          {
            "id": "uuid-email",
            "type": "link",
            "content": "{email}",
            "action": { "type": "email" },
            "icon": { "name": "mail", "library": "lucide", "position": "left" }
          },
          {
            "id": "uuid-address",
            "type": "text",
            "content": "{endereco}",
            "icon": { "name": "map-pin", "library": "lucide", "position": "left" }
          }
        ]
      },
      {
        "id": "uuid-social",
        "type": "social_links",
        "props": {
          "instagram": "https://instagram.com/empresa",
          "facebook": "https://facebook.com/empresa",
          "whatsapp": "5511999999999"
        }
      },
      {
        "id": "uuid-copyright",
        "type": "text",
        "content": "© 2026 {empresa}. Todos os direitos reservados.",
        "styles": {
          "desktop": { "color": "rgba(255,255,255,0.5)", "fontSize": "14px", "textAlign": "center" }
        }
      }
    ]
  }
}
```

---

## 8. Estilos Responsivos

O sistema suporta 3 breakpoints. O frontend deve aplicar estilos com merge/heranca:

```
desktop (base) -> tablet (override) -> mobile (override)
```

### Logica de merge no frontend

```typescript
function getStyles(elementStyles: ElementStylesDTO, breakpoint: "desktop" | "tablet" | "mobile") {
  const base = elementStyles?.desktop || {};

  if (breakpoint === "desktop") return base;
  if (breakpoint === "tablet") return { ...base, ...elementStyles?.tablet };
  if (breakpoint === "mobile") return { ...base, ...elementStyles?.tablet, ...elementStyles?.mobile };
}
```

### Estilos da Secao (SectionStylesDTO)

```typescript
interface SectionStylesDTO {
  desktop: ResponsiveStyleDTO | null;
  tablet: ResponsiveStyleDTO | null;
  mobile: ResponsiveStyleDTO | null;
}

interface ResponsiveStyleDTO {
  padding: string | null;
  paddingTop: string | null;
  paddingRight: string | null;
  paddingBottom: string | null;
  paddingLeft: string | null;
  margin: string | null;
  marginTop: string | null;
  marginRight: string | null;
  marginBottom: string | null;
  marginLeft: string | null;
  minHeight: string | null;
  maxHeight: string | null;
  backgroundColor: string | null;
  borderRadius: string | null;
  boxShadow: string | null;
  customStyles: Record<string, string> | null;
}
```

---

## 9. Sistema de Versoes

Cada alteracao salva pode gerar uma versao automaticamente.

### Tipos de Versao

| Tipo | Quando |
|------|--------|
| `AUTO_SAVE` | Criacao de secoes padrao ao criar pagina |
| `MANUAL` | Update com `createVersion: true` |
| `PUBLISH` | Ao publicar a pagina |

### Listar Versoes

```http
GET /api/v1/landing-pages/{id}/versions
```

```json
[
  {
    "id": 3,
    "versao": 3,
    "descricao": "Publicacao da pagina",
    "tipo": "PUBLISH",
    "criadoPor": 1,
    "criadoPorNome": "Admin",
    "dtCriacao": "2026-02-25T14:30:00"
  },
  {
    "id": 2,
    "versao": 2,
    "descricao": "Atualizacao do hero",
    "tipo": "MANUAL",
    "criadoPor": 1,
    "criadoPorNome": "Admin",
    "dtCriacao": "2026-02-25T14:00:00"
  }
]
```

### Restaurar Versao

```http
POST /api/v1/landing-pages/{id}/versions/2/restore
```

Restaura o estado completo da pagina (incluindo todas as secoes) para o snapshot da versao 2.

---

## 10. Variaveis Dinamicas

O campo `content` dos elementos suporta variaveis que sao substituidas no backend ao criar secoes padrao. O frontend tambem pode usar isso para substituicao em tempo de renderizacao.

| Variavel | Descricao |
|----------|-----------|
| `{empresa}` | Nome da organizacao |
| `{telefone}` | Telefone da organizacao |
| `{email}` | E-mail da organizacao |
| `{endereco}` | Endereco completo |
| `{logo_url}` | URL do logo da organizacao |
| `{whatsapp}` | Numero de WhatsApp |

O frontend deve fazer o replace dessas variaveis ao renderizar a pagina publica, usando os dados da organizacao.

---

## 11. Exemplos Praticos Completos

### 11.1 Fluxo: Criar pagina do zero e publicar

```typescript
// 1. Criar a pagina (secoes padrao serao geradas)
const page = await api.post("/api/v1/landing-pages", {
  nome: "Site Principal",
  tipo: "HOME",
  isHome: true,
  globalSettings: {
    theme: "light",
    primaryColor: "#6366f1",
    fontFamily: "Inter, sans-serif"
  },
  seoSettings: {
    title: "Minha Empresa - Agendamento Online",
    description: "Agende seus servicos online de forma rapida e facil."
  }
});

const landingPageId = page.data.id;

// 2. Atualizar a secao HERO (pegar sectionId da resposta)
const heroSection = page.data.sections.find(s => s.tipo === "HERO");

await api.put(`/api/v1/landing-pages/${landingPageId}/sections/${heroSection.sectionId}`, {
  content: {
    layout: "full-width",
    background: {
      type: "image",
      imageUrl: "https://meusite.com/hero.jpg",
      overlay: true,
      overlayColor: "rgba(0,0,0,0.5)"
    },
    elements: [
      {
        id: crypto.randomUUID(),
        type: "heading",
        tag: "h1",
        content: "Transforme seu visual",
        visible: true,
        order: 0,
        styles: {
          desktop: { fontSize: "56px", fontWeight: "800", color: "#fff", textAlign: "center" },
          mobile: { fontSize: "32px" }
        }
      },
      {
        id: crypto.randomUUID(),
        type: "button",
        content: "Agendar Agora",
        variant: "primary",
        size: "lg",
        visible: true,
        order: 1,
        action: { type: "scroll", href: "#booking" }
      }
    ]
  }
});

// 3. Adicionar uma secao de FAQ antes do footer
await api.post(`/api/v1/landing-pages/${landingPageId}/sections`, {
  tipo: "FAQ",
  nome: "Perguntas Frequentes",
  posicao: 5,
  content: {
    layout: "contained",
    container: { maxWidth: "800px", centered: true },
    elements: [
      {
        id: crypto.randomUUID(),
        type: "heading",
        tag: "h2",
        content: "Duvidas Frequentes",
        order: 0
      },
      {
        id: crypto.randomUUID(),
        type: "accordion",
        order: 1,
        children: [
          {
            id: crypto.randomUUID(),
            type: "card",
            props: { question: "Como agendar?", answer: "Basta clicar em Agendar Agora..." }
          },
          {
            id: crypto.randomUUID(),
            type: "card",
            props: { question: "Posso cancelar?", answer: "Sim, com 24h de antecedencia." }
          }
        ]
      }
    ]
  }
});

// 4. Publicar
await api.post(`/api/v1/landing-pages/${landingPageId}/publish`);
```

### 11.2 Fluxo: Editor Drag-and-Drop (salvar tudo de uma vez)

O endpoint `PUT /{id}` aceita o array `sections` completo, permitindo salvar toda a pagina de uma vez:

```typescript
await api.put(`/api/v1/landing-pages/${landingPageId}`, {
  sections: [
    {
      sectionId: "uuid-header",
      tipo: "HEADER",
      nome: "Cabecalho",
      ordem: 0,
      visivel: true,
      content: { /* ... */ }
    },
    {
      sectionId: "uuid-hero",
      tipo: "HERO",
      nome: "Banner",
      ordem: 1,
      visivel: true,
      content: { /* ... */ },
      styles: { /* ... */ },
      animations: { type: "fadeIn", duration: 800 }
    },
    // ... todas as secoes
  ],
  createVersion: true,
  versionDescription: "Save completo do editor"
});
```

### 11.3 Fluxo: Renderizar pagina publica

```typescript
// Buscar por slug (site publico)
const page = await api.get(`/api/v1/landing-pages/by-slug/home`);

// Filtrar secoes visiveis e ordenar
const sections = page.data.sections
  .filter(s => s.visivel && s.ativo)
  .sort((a, b) => a.ordem - b.ordem);

// Aplicar globalSettings como CSS variables
const root = document.documentElement;
const gs = page.data.globalSettings;
if (gs) {
  root.style.setProperty("--primary", gs.primaryColor);
  root.style.setProperty("--secondary", gs.secondaryColor);
  root.style.setProperty("--accent", gs.accentColor);
  root.style.setProperty("--bg", gs.backgroundColor);
  root.style.setProperty("--text", gs.textColor);
  root.style.setProperty("--font", gs.fontFamily);
  root.style.setProperty("--font-heading", gs.headingFontFamily);
  root.style.setProperty("--radius", gs.borderRadius);
}

// Aplicar SEO
if (page.data.seoSettings) {
  document.title = page.data.seoSettings.title;
  // ... meta tags
}

// Aplicar customCss
if (page.data.customCss) {
  const style = document.createElement("style");
  style.textContent = page.data.customCss;
  document.head.appendChild(style);
}

// Renderizar cada secao
sections.forEach(section => {
  renderSection(section);
});
```

---

## Resumo Rapido

| O que | Como |
|-------|------|
| Criar pagina | `POST /api/v1/landing-pages` com `CreateLandingPageRequest` |
| Editar secao | `PUT /.../{sectionId}` usando UUID da secao |
| Salvar tudo | `PUT /{id}` com array `sections` completo |
| Reordenar | `PUT /.../reorder` com array de UUIDs |
| Publicar | `POST /{id}/publish` |
| Desfazer | `POST /.../versions/{versao}/restore` |
| Dados dinamicos | Usar `dataSource` na secao + elementos tipo `*_list` |
| Responsivo | Definir `styles.desktop`, `.tablet`, `.mobile` com merge |
| Variaveis | Usar `{empresa}`, `{telefone}`, etc. no `content` |
