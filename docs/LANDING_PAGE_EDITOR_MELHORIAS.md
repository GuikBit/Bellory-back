# Landing Page Editor - Documento de Melhorias

## Diagnóstico Geral

O editor tem uma **arquitetura sólida** (API completa, store, preview com componentes reais, merge de dados), mas está com vários problemas que impedem o uso real. Os problemas são divididos em 3 categorias: **Bugs Críticos**, **Problemas de Arquitetura** e **Funcionalidades Faltantes**.

---

## 1. BUGS CRÍTICOS

### 1.1 Incompatibilidade de Tipos entre Frontend e API

**Problema:** O `HomeLandingPage.tsx` define `LandingPageDTO` com campos em **camelCase inglês** (`createdAt`, `updatedAt`, `currentVersion`), mas a API retorna em **padrão Java** (`dtCriacao`, `dtAtualizacao`, `versao`).

**Onde:** `HomeLandingPage.tsx:12-25` (demo data) e todos os hooks de API.

**Campos divergentes:**

| Frontend espera | API retorna |
|----------------|-------------|
| `id` (string) | `id` (Long/number) |
| `createdAt` | `dtCriacao` |
| `updatedAt` | `dtAtualizacao` |
| `currentVersion` | `versao` |
| `sections[].visible` | `sections[].visivel` |
| `sections[].order` | `sections[].ordem` |
| `sections[].content.elements` | `sections[].content` (JSON string no banco) |
| `sections[].background` | Dentro de `content` na API |
| `sections[].settings` | `sections[].settings` (JSON string no banco) |

**Correção:** Criar um adapter/mapper no hook de API que converte a resposta da API para o tipo do frontend, ou alinhar os tipos do frontend com a API.

---

### 1.2 Drag-and-Drop de Seções Não Funciona

**Problema:** O `SectionListSidebar.tsx` tem `draggable` e eventos `onDragStart`/`onDragEnd`/`onDragOver` nos itens, mas:
- O `_draggedIndex` é salvo mas nunca usado (prefixo `_` indica isso)
- Não há `onDrop` handler
- Não há chamada para `onReorderSections` (a prop existe na interface mas não é usada)
- Não há feedback visual de onde a seção será solta

**Onde:** `SectionListSidebar.tsx:116,186-189`

**Correção:**
- Implementar `onDrop` handler que calcula a nova ordem
- Usar a prop `onReorderSections` para chamar `editor.reorderSections()`
- Adicionar indicador visual de posição de drop (linha azul entre seções)
- Considerar usar `@dnd-kit/core` para drag-and-drop mais robusto

---

### 1.3 Dados de Demonstração Mascaram Erros Reais

**Problema:** `HomeLandingPage.tsx:284` faz fallback silencioso para dados demo quando a API falha:
```typescript
const pages = pagesData?.pages || [demoLandingPage];
const isDemo = !!error || !pagesData;
```

E o `handleCreatePage` (linha 302) cria uma página local quando a criação falha, sem notificar o usuário do erro real.

**Correção:**
- Mostrar toast/alerta com o erro real da API
- Separar claramente modo demo (sem backend) vs modo produção (com backend)
- Não fazer fallback silencioso - se a API falhar, mostrar estado de erro com botão de retry

---

### 1.4 Settings dos Section Editors Não Persistem na API

**Problema:** Os section editors (Hero, About, etc.) editam `section.settings`, mas esse campo precisa ser serializado como JSON e enviado via `PUT /api/v1/landing-pages/{id}` ou `PUT /api/v1/landing-pages/{id}/sections/{sectionId}`. O auto-save do `LandingPageEditor` envia `landingPage` inteiro, mas a estrutura de dados do frontend não bate com o que a API espera.

**Onde:** `HeroSectionEditor.tsx:21-25` chama `onUpdateSettings` → `editor.updateSection()` → atualiza store local. O auto-save (linha 86 do `LandingPageEditor`) tenta enviar para a API, mas os campos estão em formato diferente.

**Correção:** O `useUpdateLandingPage` precisa fazer a transformação dos dados antes de enviar para a API, convertendo o formato do store para o formato que a API espera.

---

### 1.5 Preview Não Reflete Edições em Tempo Real Corretamente

**Problema:** O `EditorSiteProvider.tsx` faz merge entre dados da API (`homeData`) e overrides do editor (`landingPage.sections[].settings`), mas:
- Usa nomes de campos em português nos settings (`titulo`, `subtitulo`, `descricao`) que precisam ser mapeados para inglês nos DTOs do SiteContext (`title`, `subtitle`, `description`)
- O mapeamento é manual e frágil (linhas 170-280), fácil de esquecer um campo
- Se `homeData` for null (primeira carga), o preview fica vazio

**Onde:** `EditorSiteProvider.tsx:137-325`

**Correção:**
- Padronizar nomes: ou tudo português ou tudo inglês
- Criar um mapper centralizado `settingsToSiteContext(tipo, settings, homeData)` ao invés de if/spread manual
- Mostrar skeleton/placeholder enquanto `homeData` carrega

---

### 1.6 O `selectedPage` é Passado Como `null` no EditorPreview

**Problema:** Na linha 246 do `LandingPageEditor.tsx`:
```typescript
<EditorPreview selectedPage={null} ...>
```

A prop `selectedPage` é sempre `null`, tornando-a inútil. Isso é resquício do editor legado.

**Correção:** Remover a prop `selectedPage` do `EditorPreview` e limpar o código legado comentado (linhas 310-453 do `LandingPageEditor.tsx`).

---

### 1.7 IDs Inconsistentes (String vs Number)

**Problema:** O frontend usa `id: string` (ex: `'demo-1'`, `'local-${Date.now()}'`), mas a API retorna `id: Long` (number). Isso causa problemas em:
- `handleDeletePage(page.id)` - enviando string para API que espera number
- `handleDuplicatePage(page.id)` - mesmo problema
- Comparações de igualdade que falham

**Correção:** Usar `id: number | string` no tipo e converter quando necessário, ou padronizar para `number` e tratar o caso demo separadamente.

---

## 2. PROBLEMAS DE ARQUITETURA

### 2.1 Dois Sistemas de Edição Conflitantes

**Problema:** Existem dois caminhos de edição que se sobrepõem:

1. **Section Editors** (HeroSectionEditor, AboutSectionEditor, etc.) - Editam via `settings` do section, com campos tipados e formulários dedicados. Atualizam o store via `editor.updateSection(id, { settings })`.

2. **Element System** (ElementEditor, AddElementPanel, etc.) - Editam via `content.elements[]` do section, com drag-and-drop de elementos genéricos. Atualizam via `editor.addElement()`, `editor.updateElement()`.

O preview (`SectionPreviewRenderer`) renderiza **componentes reais** (SiteHero, SiteAbout, etc.) que consomem dados do `SiteContext` (alimentado pelos settings via `EditorSiteProvider`). Mas os **elementos** do `content.elements` são renderizados pelo `SectionRenderer` genérico, que é usado apenas para seções do tipo `CUSTOM`.

**Resultado:** Para seções padrão (HERO, ABOUT, etc.), os elementos dentro de `content` são **ignorados** na preview - o que aparece vem dos settings. Os elementos só importam para seções CUSTOM.

**Correção:** Definir claramente:
- **Seções padrão** (HERO, ABOUT, SERVICES, etc.): Editadas via **Section Editors** → Settings. Preview renderiza o componente real. **Remover** o sistema de elementos dessas seções.
- **Seções CUSTOM**: Editadas via **Element System** → Content/Elements. Preview renderiza via SectionRenderer.

---

### 2.2 Store do Editor Não Está Nos Arquivos

**Problema:** O `editorStore` é importado (`stores/editorStore`) mas não está nos arquivos do docs. Sem ver o store, não dá para verificar:
- Se o `undo/redo` funciona corretamente
- Se o `isDirty` é setado em todas as operações
- Se `addSection`, `removeSection`, `reorderSections` atualizam as ordens corretamente
- Se o save serializa os dados no formato correto para a API

**Correção:** Garantir que o editorStore:
- Usa `immer` ou similar para mutations seguras
- Mantém um history stack para undo/redo
- Marca `isDirty = true` em toda mutação
- Tem um método `toApiFormat()` que converte o estado do store para o formato da API

---

### 2.3 Auto-Save Pode Causar Perda de Dados

**Problema:** O auto-save (debounce 3000ms) envia a landing page inteira para a API. Se duas edições acontecerem rapidamente e a primeira request ainda estiver em andamento, pode haver race condition.

**Correção:**
- Implementar queue de saves (não enviar novo save enquanto o anterior está em andamento)
- Usar optimistic updates no store
- Adicionar indicador visual claro de "salvando..." vs "salvo" vs "erro ao salvar"

---

## 3. FUNCIONALIDADES FALTANTES

### 3.1 Publicação e Despublicação

**Status:** O botão "Ver Site" só aparece se `status === 'PUBLISHED'`, mas **não existe botão para publicar/despublicar**. A API já tem os endpoints:
- `POST /api/v1/landing-pages/{id}/publish`
- `POST /api/v1/landing-pages/{id}/unpublish`

**Correção:** Adicionar no header do editor:
- Botão "Publicar" (quando DRAFT) → chama publish endpoint → muda status para PUBLISHED
- Botão "Despublicar" (quando PUBLISHED) → chama unpublish endpoint
- Badge de status visível

---

### 3.2 Versionamento/Histórico

**Status:** A API tem endpoints completos de versionamento (`GET /versions`, `POST /versions/{versao}/restore`), mas o frontend **não tem nenhuma UI para isso**.

**Correção:** Adicionar:
- Botão "Histórico de Versões" no header do editor
- Modal/drawer lateral com lista de versões (data, descrição, quem criou)
- Botão "Restaurar" em cada versão
- Preview diff entre versão atual e versão selecionada (opcional, avançado)

---

### 3.3 Preview em Nova Aba / Modo Preview

**Status:** Não existe forma de ver a landing page como o cliente final verá, sem os controles de edição.

**Correção:**
- Botão "Visualizar" no toolbar que abre a página em nova aba
- URL pública: `/s/{slug-org}/p/{slug-landing-page}` (precisa endpoint público na API)
- Modo preview inline: esconde todos os controles de edição, mostra só o conteúdo

---

### 3.4 Endpoint Público Para Landing Pages

**Status:** A API tem endpoints autenticados (`/api/v1/landing-pages`), mas **não existe endpoint público** para servir landing pages publicadas ao visitante externo.

**Correção na API:** Criar:
```
GET /api/v1/public/site/{slug}/pages                    → Lista páginas publicadas
GET /api/v1/public/site/{slug}/pages/{pageSlug}         → Retorna página com seções
```

---

### 3.5 Responsividade do Editor

**Status:** O editor tem tratamento de mobile (sidebar overlay, floating button), mas:
- O panel de propriedades (aside direito) só aparece em `lg:` breakpoint
- Em tablet, não há como editar propriedades de seções/elementos
- O device preview muda a largura visual mas não testa estilos responsivos reais

**Correção:**
- Em telas médias, usar drawer/modal para o panel de propriedades
- Garantir que os estilos `tablet` e `mobile` das seções são realmente aplicados no device preview

---

### 3.6 Upload de Imagens

**Status:** Os campos de imagem (backgroundImage no hero, aboutImage, etc.) são **input text com URL**. O cliente precisa hospedar a imagem em outro lugar e colar a URL.

**Correção:**
- Integrar com o endpoint de upload que já existe (`AdminSuporteImagemController`)
- Adicionar componente de upload com preview inline
- Drag-and-drop de imagem no campo

---

### 3.7 Templates Prontos

**Status:** O `CreateLandingPageRequest` tem campo `templateId`, e o service suporta copiar de template, mas **não existem templates pré-criados** e **não há UI para selecionar template**.

**Correção:**
- Criar templates padrão na API (Barbearia, Salão, Clínica, etc.) com seções pré-configuradas
- No modal de criação, mostrar galeria de templates com preview visual
- Permitir que o admin Bellory crie templates globais

---

### 3.8 SEO e Configurações Globais

**Status:** A API suporta `seoSettings` e `globalSettings` na landing page, mas **não existe UI para editar** (título SEO, descrição, OG image, cores do tema, etc.).

**Correção:** Adicionar tab "Configurações" no editor com:
- SEO: título, descrição, OG image, robots
- Tema: cor primária, fonte, modo dark/light
- CSS/JS customizado
- Favicon

---

## 4. PRIORIDADES DE IMPLEMENTAÇÃO

### Fase 1 - Corrigir Bugs (tornar funcional)
1. Alinhar tipos frontend ↔ API (bug 1.1 + 1.7)
2. Remover fallback silencioso para demo (bug 1.3)
3. Corrigir persistência de settings na API (bug 1.4)
4. Limpar código legado comentado (bug 1.6)
5. Definir claramente: Section Editors para padrão, Elements para CUSTOM (problema 2.1)

### Fase 2 - Funcionalidades Core (tornar útil)
6. Implementar drag-and-drop de seções (bug 1.2)
7. Adicionar botões publicar/despublicar (feature 3.1)
8. Criar endpoint público para landing pages (feature 3.4)
9. Implementar UI de versionamento (feature 3.2)
10. Adicionar modo preview (feature 3.3)

### Fase 3 - Melhorias de UX (tornar bom)
11. Upload de imagens integrado (feature 3.6)
12. Editor de SEO e configurações globais (feature 3.8)
13. Templates prontos (feature 3.7)
14. Melhorar responsividade do editor (feature 3.5)
15. Melhorar auto-save com queue (problema 2.3)

---

## 5. DECISÃO ARQUITETURAL: UNIFICAR COM SITE-CONFIG?

### Recomendação: SIM, usando Opção A

A landing page com `is_home = true` deve ser a **fonte de verdade** da home page do site externo.

**Como unificar:**

1. Quando o cliente cria sua org, criar automaticamente uma `LandingPage` com `is_home = true` e seções padrão (HEADER, HERO, ABOUT, SERVICES, PRODUCTS, TEAM, BOOKING, FOOTER)

2. Os endpoints `PATCH /api/v1/site-config/hero`, `/about`, etc. que criamos passam a ser um **atalho** que edita a seção correspondente da LandingPage home por baixo

3. O endpoint público `GET /api/v1/public/site/{slug}/home` verifica:
   - Se existe LandingPage com `is_home = true` e status `PUBLISHED` → usa ela
   - Senão → fallback para o comportamento atual (SitePublicoConfig + dados automáticos)

4. O editor de landing pages permite edição avançada da home e criação de páginas extras

**Benefícios:**
- Uma única fonte de verdade
- Cliente simples usa formulários (site-config = modo fácil)
- Cliente avançado usa page builder (landing page editor)
- Páginas extras (promoções, eventos) usam o mesmo sistema
