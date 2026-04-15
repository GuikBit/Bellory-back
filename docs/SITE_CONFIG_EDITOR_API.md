# API de Edição do Site Público - Documentação Frontend

## Visão Geral

Esta API permite que cada organização personalize todas as seções do seu site público externo.
Os endpoints de **edição** requerem autenticação (JWT) e usam o contexto do tenant logado.
Os endpoints **públicos** de leitura (`/api/v1/public/site/{slug}/*`) continuam funcionando normalmente e refletem automaticamente qualquer alteração feita por estes endpoints.

**Base URL:** `/api/v1/site-config`
**Autenticação:** Bearer Token (JWT)
**Content-Type:** `application/json`

### Formato de Resposta Padrão

Todas as respostas seguem o padrão `ResponseAPI<T>`:

```json
{
  "success": true,
  "message": "Mensagem de sucesso ou erro",
  "dados": { ... },
  "errorCode": null,
  "errors": null
}
```

### Comportamento de Atualização Parcial (PATCH)

- Campos enviados como `null` são **ignorados** (valor atual mantido)
- Campos enviados com valor são **atualizados**
- Para listas/arrays: enviar `[]` (vazio) **limpa** o campo; enviar `null` **mantém** o valor atual
- Se a organização ainda não tem uma configuração salva, o primeiro PATCH/PUT **cria** automaticamente

---

## 1. Configuração Completa

### GET `/api/v1/site-config`

Retorna toda a configuração do site da organização logada.

**Response `200`:**

```json
{
  "success": true,
  "dados": {
    "id": 1,
    "organizacaoId": 10,
    "hero": { ... },
    "header": { ... },
    "about": { ... },
    "footer": { ... },
    "services": { ... },
    "products": { ... },
    "team": { ... },
    "booking": { ... },
    "general": { ... },
    "active": true,
    "dtCriacao": "2025-01-15T10:30:00",
    "dtAtualizacao": "2025-03-20T14:22:00"
  }
}
```

> Se a organização ainda não tem configuração, retorna `active: false` e os demais campos `null`.

### PUT `/api/v1/site-config`

Salva/atualiza **todas** as seções de uma vez. Útil para "salvar tudo" ou importar configuração.

**Request Body:**

```json
{
  "hero": { ... },
  "header": { ... },
  "about": { ... },
  "footer": { ... },
  "services": { ... },
  "products": { ... },
  "team": { ... },
  "booking": { ... },
  "general": { ... }
}
```

> Seções enviadas como `null` no PUT são ignoradas (não zera a seção).

---

## 2. Hero (Banner Principal)

### PATCH `/api/v1/site-config/hero`

Personaliza o banner principal do site.

**Componente Frontend:** `SiteHero.tsx`

**Request Body:**

```json
{
  "type": "TEMPLATE",
  "title": "Bem-vindo à Barbearia do Jé",
  "subtitle": "Qualidade e excelência em serviços de barbearia desde 2015",
  "backgroundUrl": "https://storage.exemplo.com/hero-bg.jpg",
  "backgroundOverlay": 0.5,
  "customHtml": null,
  "buttons": [
    {
      "label": "Agendar Agora",
      "href": "#agendar",
      "type": "primary",
      "icon": null
    },
    {
      "label": "Ver Serviços",
      "href": "#servicos",
      "type": "secondary",
      "icon": null
    }
  ],
  "showBookingForm": false
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `type` | `string` | Tipo do hero: `"TEMPLATE"` (padrão com título/subtítulo/botões) ou `"CUSTOM_HTML"` (HTML livre) |
| `title` | `string` | Título principal exibido no hero (max 255 chars) |
| `subtitle` | `string` | Subtítulo/descrição abaixo do título |
| `backgroundUrl` | `string` | URL da imagem de fundo (max 500 chars). Se `null`, usa gradiente do tema |
| `backgroundOverlay` | `number` | Opacidade do overlay escuro sobre a imagem (0.0 a 1.0). Padrão: `0.5` |
| `customHtml` | `string` | HTML personalizado (usado apenas quando `type = "CUSTOM_HTML"`) |
| `buttons` | `array` | Lista de botões CTA no hero |
| `buttons[].label` | `string` | Texto do botão |
| `buttons[].href` | `string` | Link do botão (ex: `"#agendar"`, `"/servicos"`, URL externa) |
| `buttons[].type` | `string` | Estilo: `"primary"`, `"secondary"` ou `"outline"` |
| `buttons[].icon` | `string` | Nome do ícone (opcional) |
| `showBookingForm` | `boolean` | Se `true`, exibe formulário de agendamento rápido no hero |

### Como o Frontend Consome (Leitura)

O endpoint público `GET /api/v1/public/site/{slug}/hero` retorna `HeroSectionDTO` que inclui os campos acima **mais** `stats` (calculado automaticamente):

```json
{
  "stats": {
    "yearsExperience": null,
    "happyClients": null,
    "servicesCount": 12,
    "teamSize": 5
  }
}
```

> `servicesCount` e `teamSize` são calculados automaticamente a partir dos serviços e funcionários cadastrados. `yearsExperience` e `happyClients` não são editáveis por enquanto.

---

## 3. Header (Navegação)

### PATCH `/api/v1/site-config/header`

Personaliza o cabeçalho/navegação do site.

**Componente Frontend:** `SiteHeader.tsx`

**Request Body:**

```json
{
  "logoUrl": "https://storage.exemplo.com/logo.png",
  "logoAlt": "Barbearia do Jé",
  "menuItems": [
    { "label": "Início", "href": "/", "order": 1, "external": false, "subItems": null },
    { "label": "Serviços", "href": "#servicos", "order": 2, "external": false, "subItems": null },
    { "label": "Produtos", "href": "#produtos", "order": 3, "external": false, "subItems": null },
    { "label": "Equipe", "href": "#equipe", "order": 4, "external": false, "subItems": null },
    { "label": "Sobre", "href": "#sobre", "order": 5, "external": false, "subItems": null },
    { "label": "Contato", "href": "#contato", "order": 6, "external": false, "subItems": null }
  ],
  "actionButtons": [
    { "label": "Agendar", "href": "#agendar", "type": "primary", "icon": null }
  ],
  "showPhone": true,
  "showSocial": false,
  "sticky": true
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `logoUrl` | `string` | URL da imagem do logo (max 500 chars). Se `null`, exibe o nome da organização como texto |
| `logoAlt` | `string` | Texto alternativo do logo (max 100 chars). Padrão: nome fantasia da org |
| `menuItems` | `array` | Itens do menu de navegação |
| `menuItems[].label` | `string` | Texto exibido no menu |
| `menuItems[].href` | `string` | Link de destino (âncora `#secao` ou rota `/pagina`) |
| `menuItems[].order` | `integer` | Ordem de exibição (1, 2, 3...) |
| `menuItems[].external` | `boolean` | Se `true`, abre em nova aba |
| `menuItems[].subItems` | `array\|null` | Sub-itens do menu (mesma estrutura, recursivo) |
| `actionButtons` | `array` | Botões CTA no header (ex: "Agendar") |
| `actionButtons[].label` | `string` | Texto do botão |
| `actionButtons[].href` | `string` | Link do botão |
| `actionButtons[].type` | `string` | Estilo: `"primary"`, `"secondary"` ou `"outline"` |
| `actionButtons[].icon` | `string` | Nome do ícone (opcional) |
| `showPhone` | `boolean` | Exibir telefone no header. Padrão: `true`. O número vem de `Organizacao.telefone1` |
| `showSocial` | `boolean` | Exibir ícones de redes sociais no header. Padrão: `false`. Os links vêm de `Organizacao.redesSociais` |
| `sticky` | `boolean` | Header fixo no topo ao rolar. Padrão: `true` |

### Dados Automáticos (não editáveis aqui)

- `phoneNumber`: vem de `Organizacao.telefone1`
- `socialLinks`: vem de `Organizacao.redesSociais` (Instagram, Facebook, WhatsApp, YouTube, LinkedIn, TikTok)

> Para editar telefone e redes sociais, use os endpoints de edição da organização.

---

## 4. About (Seção Sobre)

### PATCH `/api/v1/site-config/about`

Personaliza a seção "Sobre Nós" do site.

**Componente Frontend:** `SiteAbout.tsx`

**Request Body:**

```json
{
  "title": "Sobre Nós",
  "subtitle": "Conheça nossa história",
  "description": "A Barbearia do Jé nasceu em 2015 com o objetivo de oferecer serviços de qualidade...",
  "fullDescription": "Texto mais completo para a página dedicada 'Sobre'...",
  "imageUrl": "https://storage.exemplo.com/about-banner.jpg",
  "galleryImages": [
    "https://storage.exemplo.com/gallery-1.jpg",
    "https://storage.exemplo.com/gallery-2.jpg",
    "https://storage.exemplo.com/gallery-3.jpg"
  ],
  "videoUrl": "https://www.youtube.com/embed/VIDEO_ID",
  "highlights": [
    { "icon": "star", "title": "10+ Anos", "description": "De experiência no mercado" },
    { "icon": "users", "title": "5000+ Clientes", "description": "Atendidos com excelência" },
    { "icon": "award", "title": "Premiado", "description": "Melhor barbearia da região 2024" }
  ],
  "mission": "Oferecer serviços de barbearia com excelência, cuidado e atenção personalizada.",
  "vision": "Ser referência em cuidados masculinos na região até 2027.",
  "values": "Qualidade, Respeito, Inovação, Compromisso"
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `title` | `string` | Título da seção (max 255 chars). Padrão: `"Sobre Nós"` |
| `subtitle` | `string` | Subtítulo (max 255 chars) |
| `description` | `string` | Descrição resumida (exibida na home) |
| `fullDescription` | `string` | Descrição completa (exibida na página `/sobre` dedicada) |
| `imageUrl` | `string` | URL da imagem principal/banner (max 500 chars) |
| `galleryImages` | `array<string>` | Lista de URLs de imagens para galeria |
| `videoUrl` | `string` | URL do vídeo embed (YouTube, Vimeo, etc.) (max 500 chars). Se presente, o vídeo é exibido no lugar da imagem |
| `highlights` | `array` | Lista de diferenciais/destaques exibidos em cards |
| `highlights[].icon` | `string` | Nome do ícone (ex: `"star"`, `"users"`, `"award"`) |
| `highlights[].title` | `string` | Título do destaque |
| `highlights[].description` | `string` | Descrição do destaque |
| `mission` | `string` | Texto da missão (exibido na versão completa) |
| `vision` | `string` | Texto da visão (exibido na versão completa) |
| `values` | `string` | Valores da organização como texto. No front, são exibidos como tags separadas |

### Como o Frontend Exibe

- **Home page** (`GET /{slug}/about`): exibe `title`, `description`, `image/video`, `highlights`
- **Página dedicada** (`GET /{slug}/about/full`): exibe tudo acima + `fullDescription`, `mission`, `vision`, `values`, `gallery`
- **Organization Info**: dados de contato (endereço, telefone, email) são preenchidos automaticamente da `Organizacao`

---

## 5. Footer (Rodapé)

### PATCH `/api/v1/site-config/footer`

Personaliza o rodapé do site.

**Componente Frontend:** `SiteFooter.tsx`

**Request Body:**

```json
{
  "description": "Sua barbearia de confiança desde 2015.",
  "logoUrl": "https://storage.exemplo.com/logo-footer.png",
  "linkSections": [
    {
      "title": "Links Rápidos",
      "links": [
        { "label": "Início", "href": "/", "external": false },
        { "label": "Serviços", "href": "/servicos", "external": false },
        { "label": "Agendar", "href": "/agendar", "external": false }
      ]
    },
    {
      "title": "Institucional",
      "links": [
        { "label": "Sobre Nós", "href": "/sobre", "external": false },
        { "label": "Política de Privacidade", "href": "/privacidade", "external": false },
        { "label": "Instagram", "href": "https://instagram.com/barbearia", "external": true }
      ]
    }
  ],
  "copyrightText": "© 2025 Barbearia do Jé. Todos os direitos reservados.",
  "showMap": true,
  "showHours": true,
  "showSocial": true,
  "showNewsletter": false
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `description` | `string` | Texto descritivo exibido na primeira coluna do footer |
| `logoUrl` | `string` | URL do logo do footer (max 500 chars). Se `null`, usa o logo do header |
| `linkSections` | `array` | Seções de links organizadas em colunas |
| `linkSections[].title` | `string` | Título da seção de links (ex: "Links Rápidos") |
| `linkSections[].links` | `array` | Lista de links da seção |
| `linkSections[].links[].label` | `string` | Texto do link |
| `linkSections[].links[].href` | `string` | URL/rota do link |
| `linkSections[].links[].external` | `boolean` | Se `true`, abre em nova aba |
| `copyrightText` | `string` | Texto de copyright (max 255 chars). Padrão: `"© {ano} {nomeFantasia}. Todos os direitos reservados."` |
| `showMap` | `boolean` | Exibir mapa no footer. Padrão: `true` |
| `showHours` | `boolean` | Exibir horários de funcionamento. Padrão: `true` |
| `showSocial` | `boolean` | Exibir ícones de redes sociais. Padrão: `true` |
| `showNewsletter` | `boolean` | Exibir campo de newsletter. Padrão: `false` |

### Dados Automáticos (não editáveis aqui)

- `contactInfo` (phone, whatsapp, email, address): vem da `Organizacao`
- `socialLinks`: vem de `Organizacao.redesSociais`
- `horariosFuncionamento`: vem dos horários cadastrados dos funcionários
- `endereco`: vem de `Organizacao.enderecoPrincipal`

---

## 6. Serviços (Configuração da Seção)

### PATCH `/api/v1/site-config/services`

Configura como a seção de serviços é exibida. **Não edita os serviços em si** (use `/api/v1/servicos` para isso).

**Componente Frontend:** `SiteServicosDestaque.tsx`, `SiteServicosPage.tsx`

**Request Body:**

```json
{
  "sectionTitle": "Nossos Serviços",
  "sectionSubtitle": "Conheça tudo o que oferecemos para você",
  "showPrices": true,
  "showDuration": true,
  "featuredLimit": 6
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `sectionTitle` | `string` | Título da seção (max 255 chars). Padrão: `"Nossos Serviços"` |
| `sectionSubtitle` | `string` | Subtítulo da seção (max 255 chars) |
| `showPrices` | `boolean` | Exibir preços nos cards de serviço. Padrão: `true` |
| `showDuration` | `boolean` | Exibir tempo estimado nos cards. Padrão: `true` |
| `featuredLimit` | `integer` | Quantidade de serviços exibidos na home. Padrão: `6` |

### O Que NÃO é Editável Aqui

- Lista de serviços, preços, categorias, imagens → gerenciados em `/api/v1/servicos`
- Dados de cada serviço aparecem automaticamente no site público baseados no cadastro

---

## 7. Produtos (Configuração da Seção)

### PATCH `/api/v1/site-config/products`

Configura como a seção de produtos é exibida. **Não edita os produtos em si** (use `/api/v1/produtos` para isso).

**Componente Frontend:** `SiteProdutosDestaque.tsx`, `SiteProdutosPage.tsx`

**Request Body:**

```json
{
  "sectionTitle": "Produtos em Destaque",
  "sectionSubtitle": "Os melhores produtos para seus cuidados",
  "showPrices": true,
  "featuredLimit": 8
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `sectionTitle` | `string` | Título da seção (max 255 chars). Padrão: `"Produtos em Destaque"` |
| `sectionSubtitle` | `string` | Subtítulo da seção (max 255 chars) |
| `showPrices` | `boolean` | Exibir preços nos cards de produto. Padrão: `true` |
| `featuredLimit` | `integer` | Quantidade de produtos exibidos na home (carousel). Padrão: `8` |

---

## 8. Equipe (Configuração da Seção)

### PATCH `/api/v1/site-config/team`

Configura como a seção de equipe é exibida. **Não edita os funcionários em si**.

**Componente Frontend:** `SiteEquipeDestaque.tsx`

**Request Body:**

```json
{
  "sectionTitle": "Nossa Equipe",
  "sectionSubtitle": "Profissionais qualificados prontos para atender você",
  "showSection": true
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `sectionTitle` | `string` | Título da seção (max 255 chars). Padrão: `"Nossa Equipe"` |
| `sectionSubtitle` | `string` | Subtítulo da seção (max 255 chars) |
| `showSection` | `boolean` | Exibir ou ocultar a seção inteira. Padrão: `true` |

### O Que NÃO é Editável Aqui

- Lista de membros, fotos, cargos, horários → gerenciados em `/api/v1/funcionarios`
- Visibilidade individual: controlada pelo campo `isVisivelExterno` de cada funcionário

---

## 9. Agendamento (Configuração da Seção)

### PATCH `/api/v1/site-config/booking`

Configura a seção de agendamento no site.

**Componente Frontend:** `SiteAgendamento.tsx`

**Request Body:**

```json
{
  "sectionTitle": "Agende seu Horário",
  "sectionSubtitle": "Escolha o serviço, profissional e horário ideal para você",
  "enabled": true
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `sectionTitle` | `string` | Título da seção (max 255 chars). Padrão: `"Agende seu Horário"` |
| `sectionSubtitle` | `string` | Subtítulo da seção (max 255 chars) |
| `enabled` | `boolean` | Habilitar/desabilitar agendamento online. Padrão: `true` |

---

## 10. Configurações Gerais

### PATCH `/api/v1/site-config/general`

Controla a ordem das seções na home, CSS/JS customizado e scripts externos.

**Request Body:**

```json
{
  "homeSectionsOrder": [
    "HERO",
    "ABOUT",
    "SERVICES",
    "PRODUCTS",
    "TEAM",
    "BOOKING"
  ],
  "customCss": ".hero-section { border-radius: 0 !important; }",
  "customJs": "console.log('Site carregado');",
  "externalScripts": [
    {
      "name": "Google Analytics",
      "script": "<script async src='https://www.googletagmanager.com/gtag/js?id=GA_ID'></script>",
      "position": "head"
    },
    {
      "name": "Chat Widget",
      "script": "<script src='https://chat.exemplo.com/widget.js'></script>",
      "position": "body-end"
    }
  ],
  "active": true
}
```

### Campos

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `homeSectionsOrder` | `array<string>` | Ordem das seções na home page. Valores possíveis: `"HERO"`, `"ABOUT"`, `"SERVICES"`, `"PRODUCTS"`, `"TEAM"`, `"BOOKING"`, `"TESTIMONIALS"`. Remover um item da lista **oculta** a seção |
| `customCss` | `string` | CSS customizado injetado na página |
| `customJs` | `string` | JavaScript customizado injetado na página |
| `externalScripts` | `array` | Scripts externos (analytics, chat widgets, etc.) |
| `externalScripts[].name` | `string` | Nome identificador do script |
| `externalScripts[].script` | `string` | Tag `<script>` completa |
| `externalScripts[].position` | `string` | Onde injetar: `"head"`, `"body-start"` ou `"body-end"` |
| `active` | `boolean` | Ativar/desativar o site público inteiro |

### Controlando Visibilidade de Seções

Para **ocultar** uma seção da home, basta removê-la do array `homeSectionsOrder`:

```json
// Home com apenas Hero, Serviços e Agendamento (sem About, Products, Team)
{
  "homeSectionsOrder": ["HERO", "SERVICES", "BOOKING"]
}
```

Para **reordenar**, mude a posição no array:

```json
// Serviços antes do About
{
  "homeSectionsOrder": ["HERO", "SERVICES", "ABOUT", "PRODUCTS", "TEAM", "BOOKING"]
}
```

---

## Exemplos de Uso no Frontend

### Fluxo Básico: Carregar → Editar → Salvar

```typescript
// 1. Carregar configuração atual
const response = await api.get('/api/v1/site-config');
const config = response.data.dados;

// 2. Exibir no formulário de edição
setHeroForm({
  title: config.hero?.title || '',
  subtitle: config.hero?.subtitle || '',
  backgroundUrl: config.hero?.backgroundUrl || '',
  backgroundOverlay: config.hero?.backgroundOverlay || 0.5,
  buttons: config.hero?.buttons || [],
  // ...
});

// 3. Salvar apenas a seção editada
const heroData = {
  title: heroForm.title,
  subtitle: heroForm.subtitle,
  backgroundUrl: heroForm.backgroundUrl,
  backgroundOverlay: heroForm.backgroundOverlay,
  buttons: heroForm.buttons,
};

await api.patch('/api/v1/site-config/hero', heroData);
```

### Salvar Apenas Um Campo

Graças ao PATCH parcial, você pode enviar apenas o campo que mudou:

```typescript
// Mudar apenas o título do hero
await api.patch('/api/v1/site-config/hero', {
  title: "Novo Título"
});
// Todos os outros campos do hero permanecem inalterados
```

### Salvar Tudo de Uma Vez

```typescript
await api.put('/api/v1/site-config', {
  hero: { title: "...", subtitle: "..." },
  header: { logoUrl: "...", sticky: true },
  about: { description: "..." },
  footer: { showNewsletter: true },
  services: { showPrices: true, featuredLimit: 8 },
  products: { showPrices: true },
  team: { showSection: true },
  booking: { enabled: true },
  general: { homeSectionsOrder: ["HERO", "ABOUT", "SERVICES", "BOOKING"] }
});
```

### Reordenar Seções via Drag and Drop

```typescript
// Após o usuário reordenar via drag-and-drop
const newOrder = ["HERO", "SERVICES", "ABOUT", "TEAM", "PRODUCTS", "BOOKING"];
await api.patch('/api/v1/site-config/general', {
  homeSectionsOrder: newOrder
});
```

---

## Mapa: O Que é Editável vs. Automático

| Dado | Editável via Site Config? | Fonte |
|------|---------------------------|-------|
| Hero título, subtítulo, fundo, botões | Sim | `PATCH /hero` |
| Hero stats (serviços, equipe) | Não | Calculado dos dados cadastrados |
| Header logo, menu, botões CTA | Sim | `PATCH /header` |
| Header telefone, redes sociais | Não | `Organizacao` (editar org) |
| About textos, imagem, vídeo, galeria | Sim | `PATCH /about` |
| About missão, visão, valores | Sim | `PATCH /about` |
| About info da org (endereço, tel, email) | Não | `Organizacao` (editar org) |
| Serviços título, mostrar preço/duração | Sim | `PATCH /services` |
| Lista de serviços, preços, categorias | Não | `/api/v1/servicos` |
| Produtos título, mostrar preço | Sim | `PATCH /products` |
| Lista de produtos, preços, estoque | Não | `/api/v1/produtos` |
| Equipe título, mostrar/ocultar seção | Sim | `PATCH /team` |
| Lista de funcionários, fotos, cargos | Não | `/api/v1/funcionarios` |
| Agendamento título, habilitar/desabilitar | Sim | `PATCH /booking` |
| Footer descrição, links, copyright, flags | Sim | `PATCH /footer` |
| Footer contato, horários, endereço | Não | `Organizacao` + horários cadastrados |
| Ordem e visibilidade das seções | Sim | `PATCH /general` |
| CSS/JS customizado, scripts externos | Sim | `PATCH /general` |
| Tema/cores do site | Não | `Organizacao.tema` (editar org) |

---

## Códigos de Erro

| Código | Situação |
|--------|----------|
| `200` | Sucesso |
| `400` | Dados inválidos no request |
| `401` | Token JWT ausente ou expirado |
| `404` | Organização não encontrada no contexto |
| `500` | Erro interno do servidor |

---

## Endpoints Públicos de Leitura (Referência)

Estes endpoints já existiam e retornam os dados personalizados automaticamente:

| Método | Rota | Retorna |
|--------|------|---------|
| `GET` | `/api/v1/public/site/{slug}/home` | Home page completa (todas as seções) |
| `GET` | `/api/v1/public/site/{slug}/header` | Header config |
| `GET` | `/api/v1/public/site/{slug}/hero` | Hero section |
| `GET` | `/api/v1/public/site/{slug}/about` | About (resumido) |
| `GET` | `/api/v1/public/site/{slug}/about/full` | About (completo) |
| `GET` | `/api/v1/public/site/{slug}/footer` | Footer config |
| `GET` | `/api/v1/public/site/{slug}/services/featured` | Serviços destaque |
| `GET` | `/api/v1/public/site/{slug}/services` | Todos serviços (paginado) |
| `GET` | `/api/v1/public/site/{slug}/products/featured` | Produtos destaque |
| `GET` | `/api/v1/public/site/{slug}/products` | Todos produtos (paginado) |
| `GET` | `/api/v1/public/site/{slug}/team` | Equipe |
| `GET` | `/api/v1/public/site/{slug}/booking` | Info agendamento |

> Estes endpoints são **públicos** (sem autenticação) e usam o `slug` na URL.
> Os endpoints de **edição** (`/api/v1/site-config/*`) usam JWT e identificam a organização pelo token.
