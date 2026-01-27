# Landing Page Editor - Documentação

## Visão Geral

O Editor de Landing Pages permite criar e customizar páginas públicas para os clientes da organização. O sistema é flexível, responsivo e preparado para integração com IA.

---

## Arquitetura

### Estrutura de Dados

```
LandingPage (Página)
├── globalSettings (configurações de tema/cores)
├── seoSettings (meta tags)
├── customCss / customJs
└── sections[] (seções da página)
    ├── tipo (HERO, ABOUT, SERVICES, etc.)
    ├── template (variante visual)
    ├── content (elementos e layout)
    │   ├── layout
    │   ├── background
    │   └── elements[]
    │       ├── type (heading, button, image, etc.)
    │       ├── content / url
    │       ├── action
    │       ├── styles (responsivo)
    │       └── children[] (para containers)
    ├── styles (estilos da seção)
    └── dataSource (dados dinâmicos)
```

---

## API Endpoints

### Landing Pages

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/landing-pages` | Lista todas as páginas |
| GET | `/api/landing-pages/{id}` | Busca página por ID |
| GET | `/api/landing-pages/by-slug/{slug}` | Busca por slug |
| POST | `/api/landing-pages` | Cria nova página |
| PUT | `/api/landing-pages/{id}` | Atualiza página |
| POST | `/api/landing-pages/{id}/publish` | Publica página |
| POST | `/api/landing-pages/{id}/unpublish` | Despublica |
| POST | `/api/landing-pages/{id}/duplicate` | Duplica página |
| DELETE | `/api/landing-pages/{id}` | Deleta página |

### Seções

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/landing-pages/{id}/sections` | Adiciona seção |
| PUT | `/api/landing-pages/{id}/sections/{sectionId}` | Atualiza seção |
| PUT | `/api/landing-pages/{id}/sections/reorder` | Reordena seções |
| POST | `/api/landing-pages/{id}/sections/{sectionId}/duplicate` | Duplica seção |
| DELETE | `/api/landing-pages/{id}/sections/{sectionId}` | Deleta seção |

### Versões

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/landing-pages/{id}/versions` | Lista versões |
| POST | `/api/landing-pages/{id}/versions/{v}/restore` | Restaura versão |

### Metadados

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/landing-pages/metadata/section-types` | Tipos de seção |
| GET | `/api/landing-pages/metadata/element-types` | Tipos de elemento |

---

## Tipos de Seção

| Tipo | Descrição |
|------|-----------|
| HEADER | Menu de navegação |
| HERO | Banner principal |
| ABOUT | Sobre a empresa |
| SERVICES | Lista de serviços |
| PRODUCTS | Lista de produtos |
| TEAM | Equipe |
| TESTIMONIALS | Depoimentos |
| PRICING | Planos/preços |
| GALLERY | Galeria |
| BOOKING | Agendamento |
| CONTACT | Contato |
| FAQ | Perguntas frequentes |
| CTA | Call to action |
| FOOTER | Rodapé |
| CUSTOM | Personalizado |

---

## Tipos de Elemento

### Texto
- `heading` - Títulos (h1-h6)
- `paragraph` - Parágrafos
- `text` - Texto inline
- `rich-text` - Texto com HTML
- `quote` - Citações
- `list` - Listas

### Mídia
- `image` - Imagens
- `video` - Vídeos
- `icon` - Ícones
- `avatar` - Fotos de perfil
- `logo` - Logotipos
- `gallery` - Galerias

### Interativos
- `button` - Botões
- `link` - Links
- `menu` - Menus
- `tabs` - Abas
- `accordion` - Acordeões

### Layout
- `container` - Container
- `row` - Linha
- `column` - Coluna
- `grid` - Grade
- `spacer` - Espaçador
- `divider` - Divisor

### Cards
- `card` - Card genérico
- `service-card` - Card de serviço
- `product-card` - Card de produto
- `team-card` - Card de membro
- `testimonial-card` - Card de depoimento
- `pricing-card` - Card de plano

### Dados Dinâmicos
- `service-list` - Lista de serviços
- `product-list` - Lista de produtos
- `team-list` - Lista de equipe
- `booking-form` - Formulário de agendamento
- `contact-form` - Formulário de contato

### Widgets
- `social-links` - Redes sociais
- `rating` - Avaliações
- `counter` - Contadores animados
- `map` - Mapas
- `whatsapp-button` - Botão WhatsApp

---

## Estrutura JSON dos Elementos

### Elemento Básico

```json
{
  "id": "element-uuid",
  "type": "heading",
  "tag": "h1",
  "content": "Bem-vindo à {empresa}",
  "styles": {
    "desktop": {
      "fontSize": "48px",
      "fontWeight": "bold",
      "color": "#ffffff",
      "textAlign": "center"
    },
    "tablet": {
      "fontSize": "36px"
    },
    "mobile": {
      "fontSize": "28px"
    }
  }
}
```

### Botão com Ação

```json
{
  "id": "btn-1",
  "type": "button",
  "content": "Agendar Agora",
  "variant": "primary",
  "size": "lg",
  "icon": {
    "name": "calendar",
    "position": "left"
  },
  "action": {
    "type": "scroll",
    "href": "#booking"
  },
  "styles": {
    "desktop": {
      "padding": "16px 32px",
      "borderRadius": "8px"
    }
  }
}
```

### Imagem

```json
{
  "id": "img-1",
  "type": "image",
  "url": "https://exemplo.com/imagem.jpg",
  "urlMobile": "https://exemplo.com/imagem-mobile.jpg",
  "alt": "Descrição da imagem",
  "styles": {
    "desktop": {
      "width": "100%",
      "maxWidth": "600px",
      "borderRadius": "16px"
    }
  }
}
```

### Container com Filhos

```json
{
  "id": "container-1",
  "type": "container",
  "styles": {
    "desktop": {
      "display": "flex",
      "flexDirection": "row",
      "gap": "24px",
      "justifyContent": "center"
    },
    "mobile": {
      "flexDirection": "column"
    }
  },
  "children": [
    { "id": "child-1", "type": "heading", "content": "Título" },
    { "id": "child-2", "type": "paragraph", "content": "Texto..." }
  ]
}
```

### Lista Dinâmica de Serviços

```json
{
  "id": "services-1",
  "type": "service-list",
  "props": {
    "layout": "grid",
    "columns": 3,
    "showPrice": true,
    "showDuration": true,
    "cardVariant": "minimal"
  }
}
```

---

## Variáveis Dinâmicas

Use variáveis no conteúdo para dados dinâmicos da organização:

| Variável | Descrição |
|----------|-----------|
| `{empresa}` | Nome fantasia |
| `{telefone}` | Telefone principal |
| `{whatsapp}` | WhatsApp |
| `{email}` | Email |
| `{endereco}` | Endereço formatado |
| `{cidade}` | Cidade |
| `{descricao_empresa}` | Descrição da empresa |
| `{horario_funcionamento}` | Horários |

Exemplo:
```json
{
  "content": "Ligue agora: {telefone} ou envie WhatsApp para {whatsapp}"
}
```

---

## Estilos Responsivos

Os estilos são organizados por breakpoint:

```json
{
  "styles": {
    "desktop": { "fontSize": "48px", "padding": "120px 0" },
    "tablet": { "fontSize": "36px", "padding": "80px 0" },
    "mobile": { "fontSize": "24px", "padding": "60px 20px" }
  }
}
```

Breakpoints padrão:
- **Desktop**: >= 1024px
- **Tablet**: 768px - 1023px
- **Mobile**: < 768px

---

## Implementação no Frontend

### 1. Estrutura de Pastas Sugerida

```
src/
├── components/
│   └── landing-editor/
│       ├── Editor.tsx              # Componente principal
│       ├── SectionList.tsx         # Lista de seções (sidebar)
│       ├── SectionRenderer.tsx     # Renderiza seção
│       ├── ElementRenderer.tsx     # Renderiza elemento
│       ├── ElementEditor.tsx       # Edição de elemento
│       ├── StyleEditor.tsx         # Editor de estilos
│       ├── DevicePreview.tsx       # Preview responsivo
│       └── elements/               # Componentes de elementos
│           ├── Heading.tsx
│           ├── Button.tsx
│           ├── Image.tsx
│           └── ...
├── hooks/
│   ├── useLandingPage.ts          # Hook para API
│   ├── useAutoSave.ts             # Auto-save
│   └── useUndo.ts                 # Undo/Redo
└── stores/
    └── editorStore.ts             # Estado do editor (Zustand)
```

### 2. Estado do Editor (Zustand)

```typescript
interface EditorState {
  landingPage: LandingPageDTO | null;
  selectedSectionId: string | null;
  selectedElementId: string | null;
  devicePreview: 'desktop' | 'tablet' | 'mobile';
  isDirty: boolean;
  history: LandingPageDTO[];
  historyIndex: number;

  // Actions
  setLandingPage: (page: LandingPageDTO) => void;
  selectSection: (sectionId: string) => void;
  selectElement: (elementId: string) => void;
  updateSection: (sectionId: string, updates: Partial<Section>) => void;
  updateElement: (sectionId: string, elementId: string, updates: Partial<Element>) => void;
  addSection: (tipo: string, posicao?: number) => void;
  removeSection: (sectionId: string) => void;
  reorderSections: (sectionIds: string[]) => void;
  setDevicePreview: (device: 'desktop' | 'tablet' | 'mobile') => void;
  undo: () => void;
  redo: () => void;
  save: () => Promise<void>;
}
```

### 3. Componente Principal

```tsx
// Editor.tsx
export function LandingPageEditor({ pageId }: { pageId: string }) {
  const { landingPage, selectedSectionId, devicePreview } = useEditorStore();

  return (
    <div className="flex h-screen">
      {/* Sidebar - Lista de Seções */}
      <aside className="w-64 border-r">
        <SectionList />
      </aside>

      {/* Preview Central */}
      <main className="flex-1 overflow-auto">
        <DevicePreview device={devicePreview}>
          {landingPage?.sections.map(section => (
            <SectionRenderer
              key={section.sectionId}
              section={section}
              isSelected={section.sectionId === selectedSectionId}
            />
          ))}
        </DevicePreview>
      </main>

      {/* Panel Direito - Editor de Propriedades */}
      <aside className="w-80 border-l">
        {selectedSectionId && <SectionEditor />}
        {selectedElementId && <ElementEditor />}
      </aside>
    </div>
  );
}
```

### 4. Renderização de Elementos

```tsx
// ElementRenderer.tsx
export function ElementRenderer({ element }: { element: ElementDTO }) {
  const styles = useResponsiveStyles(element.styles);

  switch (element.type) {
    case 'heading':
      const Tag = element.tag || 'h2';
      return <Tag style={styles}>{parseVariables(element.content)}</Tag>;

    case 'button':
      return (
        <Button
          variant={element.variant}
          size={element.size}
          style={styles}
          onClick={() => handleAction(element.action)}
        >
          {element.icon && <Icon name={element.icon.name} />}
          {element.content}
        </Button>
      );

    case 'image':
      return (
        <img
          src={element.url}
          alt={element.alt}
          style={styles}
        />
      );

    case 'container':
      return (
        <div style={styles}>
          {element.children?.map(child => (
            <ElementRenderer key={child.id} element={child} />
          ))}
        </div>
      );

    case 'service-list':
      return <ServiceList {...element.props} />;

    default:
      return null;
  }
}
```

### 5. Preview Responsivo

```tsx
// DevicePreview.tsx
const DEVICE_WIDTHS = {
  desktop: '100%',
  tablet: '768px',
  mobile: '375px'
};

export function DevicePreview({ device, children }) {
  return (
    <div className="flex justify-center p-4 bg-gray-100">
      <div
        className="bg-white shadow-xl transition-all"
        style={{
          width: DEVICE_WIDTHS[device],
          minHeight: '100vh'
        }}
      >
        {children}
      </div>
    </div>
  );
}
```

### 6. Drag & Drop para Reordenar

```tsx
// Usar @dnd-kit/core ou react-beautiful-dnd
import { DndContext, closestCenter } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';

export function SectionList() {
  const { landingPage, reorderSections } = useEditorStore();

  const handleDragEnd = (event) => {
    const { active, over } = event;
    if (active.id !== over.id) {
      const oldIndex = sections.findIndex(s => s.sectionId === active.id);
      const newIndex = sections.findIndex(s => s.sectionId === over.id);
      const newOrder = arrayMove(sections, oldIndex, newIndex);
      reorderSections(newOrder.map(s => s.sectionId));
    }
  };

  return (
    <DndContext collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={sections} strategy={verticalListSortingStrategy}>
        {sections.map(section => (
          <SortableSection key={section.sectionId} section={section} />
        ))}
      </SortableContext>
    </DndContext>
  );
}
```

### 7. Auto-Save

```typescript
// useAutoSave.ts
export function useAutoSave(debounceMs = 2000) {
  const { landingPage, isDirty, save } = useEditorStore();

  useEffect(() => {
    if (!isDirty) return;

    const timer = setTimeout(() => {
      save();
    }, debounceMs);

    return () => clearTimeout(timer);
  }, [landingPage, isDirty, debounceMs]);
}
```

---

## Integração com IA

### Estrutura do Prompt para IA

```typescript
interface AIPromptContext {
  // Dados da organização
  organization: {
    name: string;
    businessType: string;  // "barbearia", "salão de beleza", etc.
    description: string;
    services: string[];
    location: string;
    targetAudience: string;
  };

  // Requisição
  request: {
    sectionType: string;   // HERO, ABOUT, SERVICES, etc.
    style: string;         // professional, casual, modern, bold
    tone: string;          // formal, friendly, exciting
    prompt?: string;       // Instruções adicionais do usuário
  };

  // Estrutura esperada
  outputSchema: {
    // Definição da estrutura JSON esperada
  };
}
```

### Exemplo de Prompt

```
Você é um especialista em criação de landing pages para {businessType}.

CONTEXTO DO NEGÓCIO:
- Nome: {organization.name}
- Descrição: {organization.description}
- Serviços: {organization.services.join(', ')}
- Localização: {organization.location}
- Público-alvo: {organization.targetAudience}

TAREFA:
Crie o conteúdo para uma seção "{sectionType}" com estilo "{style}" e tom "{tone}".

{prompt ? `INSTRUÇÕES ADICIONAIS: ${prompt}` : ''}

REGRAS:
1. Use variáveis dinâmicas onde apropriado: {empresa}, {telefone}, {whatsapp}, {endereco}
2. O conteúdo deve ser persuasivo e focado em conversão
3. Mantenha textos concisos e impactantes
4. Sugira imagens apropriadas (descreva o que deveria aparecer)

FORMATO DE SAÍDA:
Retorne APENAS um JSON válido seguindo esta estrutura:
{outputSchema}
```

### API para IA

```typescript
// POST /api/landing-pages/ai/generate-section
interface AIGenerateSectionRequest {
  sectionType: string;
  prompt?: string;
  style: string;
  tone: string;
  businessContext?: string;
  variations?: number;
}

interface AIGenerateSectionResponse {
  sections: LandingPageSectionDTO[];
  suggestions: {
    images: string[];      // Descrições de imagens sugeridas
    colors: string[];      // Paleta de cores sugerida
    alternatives: string[]; // Variações de texto
  };
}
```

### Fluxo de Geração

1. Usuário seleciona tipo de seção
2. Usuário descreve o que deseja (opcional)
3. Sistema envia contexto + prompt para IA
4. IA retorna estrutura JSON da seção
5. Sistema valida e apresenta preview
6. Usuário pode ajustar ou regenerar
7. Ao aprovar, seção é adicionada à página

---

## Boas Práticas

### Performance
- Lazy load de seções longas
- Debounce em edições
- Cache de imagens
- Virtualização para muitos elementos

### UX
- Preview em tempo real
- Undo/Redo (Ctrl+Z, Ctrl+Shift+Z)
- Auto-save com indicador
- Feedback visual de salvamento

### Acessibilidade
- Alt text obrigatório para imagens
- Hierarquia correta de headings
- Contraste adequado de cores
- Labels para formulários

---

## Exemplos de Uso

### Criar Nova Página

```typescript
const response = await api.post('/api/landing-pages', {
  nome: 'Promoção de Verão',
  tipo: 'PROMOCAO',
  globalSettings: {
    theme: 'light',
    primaryColor: '#ff6b00'
  }
});
```

### Adicionar Seção HERO

```typescript
const response = await api.post(`/api/landing-pages/${pageId}/sections`, {
  tipo: 'HERO',
  template: 'hero-centered',
  content: {
    layout: 'centered',
    background: {
      type: 'image',
      imageUrl: 'https://...',
      overlay: 0.5
    },
    elements: [
      {
        id: 'title',
        type: 'heading',
        tag: 'h1',
        content: 'Bem-vindo à {empresa}'
      },
      {
        id: 'cta',
        type: 'button',
        content: 'Agendar Agora',
        variant: 'primary',
        action: { type: 'scroll', href: '#booking' }
      }
    ]
  }
});
```

### Atualizar Elemento

```typescript
// No frontend, atualizar estado local
updateElement(sectionId, elementId, {
  content: 'Novo título',
  styles: {
    desktop: { fontSize: '52px' }
  }
});

// Salvar (debounced)
await api.put(`/api/landing-pages/${pageId}/sections/${sectionId}`, section);
```
