# Bellory - Documentacao da API Publica para Front-End

## Base URL

```
/api/public/site
```

> **Nota:** Nenhum endpoint requer autenticacao. Todos sao publicos.

---

## Envelope de Resposta Padrao

Todas as respostas seguem o formato `ResponseAPI<T>`:

```json
{
  "success": true,
  "message": "Mensagem descritiva",
  "dados": { /* dados tipados */ },
  "errorCode": null,
  "errors": null
}
```

| Campo       | Tipo               | Descricao                                         |
|-------------|--------------------|----------------------------------------------------|
| `success`   | `boolean`          | `true` se a operacao foi bem-sucedida              |
| `message`   | `string`           | Mensagem descritiva do resultado                   |
| `dados`     | `T` (generico)     | Payload com os dados tipados                       |
| `errorCode` | `number \| null`   | Codigo HTTP do erro (400, 404, 500) ou `null`      |
| `errors`    | `object \| null`   | Mapa de erros de validacao ou `null`               |

### Codigos HTTP retornados

| Codigo | Situacao                         |
|--------|----------------------------------|
| `200`  | Sucesso                          |
| `400`  | Slug vazio ou invalido           |
| `404`  | Organizacao/recurso nao encontrado |
| `500`  | Erro interno do servidor         |

---

## Fluxo Recomendado para o Front-End

```
1. Usuario acessa: app.bellory.com.br/{slug}
2. Front-end extrai o slug da URL
3. Chama GET /api/public/site/{slug}/home
4. Renderiza a pagina inteira com os dados recebidos
5. Usa endpoints individuais para lazy loading ou paginas dedicadas
```

### Estrategia de Carregamento

| Estrategia           | Endpoint              | Quando usar                             |
|----------------------|-----------------------|-----------------------------------------|
| **Carga completa**   | `/{slug}/home`        | Renderizacao inicial da home page       |
| **Lazy loading**     | `/{slug}/header`, etc | Carregar secoes sob demanda             |
| **Pagina dedicada**  | `/{slug}/services`    | Pagina listando todos os servicos       |
| **Detalhe**          | `/{slug}/services/1`  | Pagina de detalhe de um servico/produto |

---

## 1. HOME PAGE (Endpoint Principal)

### `GET /{slug}/home`

Retorna **todos os dados necessarios** para renderizar a home page completa em uma unica chamada.

**Exemplo:** `GET /api/public/site/barbeariadoje/home`

**Response** `HomePageDTO`:

```json
{
  "success": true,
  "message": "Home page recuperada com sucesso",
  "dados": {
    "organizacao": { /* OrganizacaoPublicDTO */ },
    "siteConfig": { /* SiteConfigDTO */ },
    "header": { /* HeaderConfigDTO */ },
    "hero": { /* HeroSectionDTO */ },
    "about": { /* AboutSectionDTO */ },
    "services": { /* ServicesSectionDTO */ },
    "products": { /* ProductsSectionDTO */ },
    "team": { /* TeamSectionDTO */ },
    "booking": { /* BookingSectionDTO */ },
    "footer": { /* FooterConfigDTO */ },
    "sectionsOrder": ["hero", "about", "services", "products", "team", "booking"],
    "seo": { /* SeoMetadataDTO */ },
    "features": { /* FeaturesDTO */ },
    "customAssets": {
      "customCss": "string | null",
      "customJs": "string | null",
      "externalScripts": [
        { "name": "string", "script": "string", "position": "head | body-start | body-end" }
      ]
    }
  }
}
```

> **Dica:** Use o campo `sectionsOrder` para ordenar dinamicamente as secoes na UI.
> Use o campo `features` para mostrar/esconder modulos inteiros (agendamento, e-commerce, etc.).

---

## 2. HEADER

### `GET /{slug}/header`

**Response** `HeaderConfigDTO`:

```json
{
  "dados": {
    "logoUrl": "https://...",
    "logoAlt": "Nome da Barbearia",
    "menuItems": [
      {
        "label": "Servicos",
        "href": "#servicos",
        "order": 1,
        "external": false,
        "subItems": [
          { "label": "Cortes", "href": "#cortes", "order": 1, "external": false, "subItems": null }
        ]
      }
    ],
    "actionButtons": [
      {
        "label": "Agendar",
        "href": "#booking",
        "type": "primary",
        "icon": "calendar"
      }
    ],
    "showPhone": true,
    "phoneNumber": "(11) 99999-9999",
    "showSocial": true,
    "sticky": true,
    "socialLinks": {
      "instagram": "https://instagram.com/...",
      "facebook": "https://facebook.com/...",
      "whatsapp": "https://wa.me/...",
      "youtube": null,
      "linkedin": null,
      "tiktok": null
    }
  }
}
```

#### Tipos internos

**MenuItemDTO** (suporta submenu recursivo):

| Campo      | Tipo              | Descricao                    |
|------------|-------------------|------------------------------|
| `label`    | `string`          | Texto do item                |
| `href`     | `string`          | Link/ancora                  |
| `order`    | `number`          | Ordem de exibicao            |
| `external` | `boolean`         | Abre em nova aba?            |
| `subItems` | `MenuItemDTO[]`   | Subitens (dropdown)          |

**ActionButtonDTO**:

| Campo   | Tipo     | Descricao                              |
|---------|----------|----------------------------------------|
| `label` | `string` | Texto do botao                         |
| `href`  | `string` | Link destino                           |
| `type`  | `string` | `"primary"`, `"secondary"`, `"outline"` |
| `icon`  | `string` | Nome do icone                          |

---

## 3. HERO / BANNER

### `GET /{slug}/hero`

**Response** `HeroSectionDTO`:

```json
{
  "dados": {
    "type": "TEMPLATE",
    "title": "Barbearia do Je",
    "subtitle": "O melhor corte da cidade",
    "backgroundUrl": "https://...",
    "backgroundOverlay": 0.5,
    "customHtml": null,
    "buttons": [
      { "label": "Agendar Agora", "href": "#booking", "type": "primary", "icon": "calendar" }
    ],
    "showBookingForm": true,
    "stats": {
      "yearsExperience": 10,
      "happyClients": 5000,
      "servicesCount": 25,
      "teamSize": 8
    }
  }
}
```

| Campo              | Tipo      | Descricao                                           |
|--------------------|-----------|------------------------------------------------------|
| `type`             | `string`  | `"TEMPLATE"` ou `"CUSTOM_HTML"`                      |
| `backgroundOverlay`| `number`  | Opacidade do overlay (0.0 a 1.0)                     |
| `customHtml`       | `string`  | HTML customizado (usado quando `type = CUSTOM_HTML`) |
| `showBookingForm`  | `boolean` | Se deve exibir formulario de agendamento no hero     |

> **Front-end:** Se `type === "CUSTOM_HTML"`, renderize `customHtml` com `dangerouslySetInnerHTML` (ou equivalente). Caso contrario, use o template padrao com `title`, `subtitle`, `buttons` e `stats`.

---

## 4. ABOUT / SOBRE

### `GET /{slug}/about` (versao resumida para home)

### `GET /{slug}/about/full` (versao completa para pagina dedicada)

**Response** `AboutSectionDTO`:

```json
{
  "dados": {
    "title": "Sobre Nos",
    "subtitle": "Conheca nossa historia",
    "description": "Texto resumido...",
    "imageUrl": "https://...",
    "videoUrl": "https://youtube.com/...",
    "galleryImages": ["https://...", "https://..."],
    "highlights": [
      { "icon": "award", "title": "10 anos", "description": "de experiencia" }
    ],
    "fullDescription": "Texto completo... (somente no /about/full)",
    "mission": "Nossa missao... (somente no /about/full)",
    "vision": "Nossa visao... (somente no /about/full)",
    "values": "Nossos valores... (somente no /about/full)",
    "organizationInfo": {
      "name": "Barbearia do Je",
      "foundedYear": "2014",
      "address": "Rua X, 123",
      "phone": "(11) 99999-9999",
      "email": "contato@barbearia.com",
      "whatsapp": "5511999999999"
    }
  }
}
```

> **Nota:** Os campos `fullDescription`, `mission`, `vision`, `values` so estao preenchidos no endpoint `/about/full`.

---

## 5. FOOTER

### `GET /{slug}/footer`

**Response** `FooterConfigDTO`:

```json
{
  "dados": {
    "logoUrl": "https://...",
    "description": "A melhor barbearia da cidade",
    "copyrightText": "2024 Barbearia do Je. Todos os direitos reservados.",
    "linkSections": [
      {
        "title": "Links Rapidos",
        "links": [
          { "label": "Servicos", "href": "/servicos", "external": false }
        ]
      }
    ],
    "contactInfo": {
      "phone": "(11) 99999-9999",
      "whatsapp": "5511999999999",
      "email": "contato@barbearia.com",
      "address": "Rua X, 123 - Centro"
    },
    "socialLinks": {
      "instagram": "https://...",
      "facebook": "https://...",
      "whatsapp": "https://wa.me/...",
      "youtube": null,
      "linkedin": null,
      "tiktok": null
    },
    "showMap": true,
    "showHours": true,
    "showSocial": true,
    "showNewsletter": false,
    "horariosFuncionamento": [
      {
        "diaSemana": "SEGUNDA",
        "diaSemanaLabel": "Segunda-feira",
        "aberto": true,
        "horaAbertura": "09:00",
        "horaFechamento": "18:00",
        "observacao": null
      }
    ],
    "endereco": {
      "logradouro": "Rua X",
      "numero": "123",
      "complemento": "Sala 1",
      "bairro": "Centro",
      "cidade": "Sao Paulo",
      "uf": "SP",
      "cep": "01000-000",
      "latitude": -23.5505,
      "longitude": -46.6333,
      "googleMapsUrl": "https://maps.google.com/..."
    }
  }
}
```

> **Front-end:** Use `latitude`/`longitude` ou `googleMapsUrl` para renderizar o mapa. Use as flags `showMap`, `showHours`, `showSocial`, `showNewsletter` para controlar visibilidade.

---

## 6. SERVICOS

### `GET /{slug}/services/featured`

Retorna servicos em destaque (para a home page).

### `GET /{slug}/services?page=0&size=12`

Retorna todos os servicos com paginacao.

| Parametro | Tipo   | Default | Descricao             |
|-----------|--------|---------|-----------------------|
| `page`    | `int`  | `0`     | Numero da pagina      |
| `size`    | `int`  | `12`    | Itens por pagina      |

**Response** `ServicesSectionDTO`:

```json
{
  "dados": {
    "title": "Nossos Servicos",
    "subtitle": "Confira o que oferecemos",
    "showPrices": true,
    "showDuration": true,
    "servicos": [
      {
        "id": 1,
        "nome": "Corte Masculino",
        "descricao": "Corte moderno...",
        "categoriaId": 1,
        "categoriaNome": "Cortes",
        "genero": "Masculino",
        "tempoEstimadoMinutos": 30,
        "preco": 45.00,
        "precoComDesconto": 39.90,
        "descontoPercentual": 11.33,
        "imagens": ["https://..."],
        "disponivel": true,
        "funcionarioIds": [1, 2, 3]
      }
    ],
    "categorias": [
      {
        "id": 1,
        "label": "Cortes",
        "value": "cortes",
        "icone": "scissors",
        "quantidadeServicos": 5
      }
    ],
    "totalServicos": 25
  }
}
```

> **Front-end:** Use `categorias` para criar filtros. Use `totalServicos` para calcular paginacao: `totalPages = Math.ceil(totalServicos / size)`. Respeite `showPrices` e `showDuration` para esconder precos/duracao se desabilitados.

### `GET /{slug}/services/{id}`

Retorna detalhes completos de um servico.

**Response** `ServicoDetalhadoDTO`:

```json
{
  "dados": {
    "id": 1,
    "nome": "Corte Masculino",
    "descricao": "Corte moderno...",
    "descricaoCompleta": "Descricao detalhada do servico...",
    "categoriaId": 1,
    "categoriaNome": "Cortes",
    "preco": 45.00,
    "precoComDesconto": 39.90,
    "descontoPercentual": 11.33,
    "genero": "Masculino",
    "tempoEstimadoMinutos": 30,
    "disponivel": true,
    "imagens": ["https://...", "https://..."],
    "imagemPrincipal": "https://...",
    "profissionais": [
      {
        "id": 1,
        "nome": "Joao Silva",
        "apelido": "Joao",
        "foto": "https://...",
        "cargo": "Barbeiro Senior",
        "descricao": "Especialista em...",
        "servicoIds": [1, 2],
        "servicoNomes": ["Corte Masculino", "Barba"],
        "horarios": [
          {
            "diaSemana": "SEGUNDA",
            "diaSemanaLabel": "Segunda-feira",
            "ativo": true,
            "horarios": [
              { "inicio": "09:00", "fim": "12:00" },
              { "inicio": "13:00", "fim": "18:00" }
            ]
          }
        ]
      }
    ],
    "produtosUtilizados": [
      { "id": 1, "nome": "Pomada Modeladora", "imagemUrl": "https://..." }
    ],
    "permiteAgendamentoOnline": true,
    "requerSinal": false,
    "percentualSinal": null,
    "seo": {
      "title": "Corte Masculino - Barbearia do Je",
      "description": "...",
      "keywords": "corte, masculino, barbearia",
      "canonicalUrl": "https://...",
      "ogImage": "https://..."
    }
  }
}
```

---

## 7. PRODUTOS

### `GET /{slug}/products/featured`

Retorna produtos em destaque (para a home page).

### `GET /{slug}/products?page=0&size=12`

Retorna todos os produtos com paginacao.

| Parametro | Tipo   | Default | Descricao             |
|-----------|--------|---------|-----------------------|
| `page`    | `int`  | `0`     | Numero da pagina      |
| `size`    | `int`  | `12`    | Itens por pagina      |

**Response** `ProductsSectionDTO`:

```json
{
  "dados": {
    "title": "Nossos Produtos",
    "subtitle": "Produtos profissionais",
    "showPrices": true,
    "produtos": [
      {
        "id": 1,
        "nome": "Pomada Modeladora",
        "descricao": "Pomada de alta fixacao...",
        "preco": 49.90,
        "precoComDesconto": 39.90,
        "descontoPercentual": 20,
        "imagens": ["https://..."],
        "categoria": "Finalizers",
        "emEstoque": true,
        "avaliacao": 4.5,
        "totalAvaliacoes": 120
      }
    ],
    "categorias": [
      { "id": 1, "label": "Finalizers", "value": "finalizers", "icone": "package", "quantidadeServicos": 8 }
    ],
    "totalProdutos": 30
  }
}
```

### `GET /{slug}/products/{id}`

Retorna detalhes completos de um produto.

**Response** `ProdutoDetalhadoDTO`:

```json
{
  "dados": {
    "id": 1,
    "nome": "Pomada Modeladora",
    "descricao": "Pomada de alta fixacao...",
    "descricaoCompleta": "Descricao completa do produto...",
    "preco": 49.90,
    "precoComDesconto": 39.90,
    "descontoPercentual": 20,
    "precoCusto": null,
    "imagens": ["https://...", "https://..."],
    "imagemPrincipal": "https://...",
    "categoriaId": 1,
    "categoriaNome": "Finalizers",
    "emEstoque": true,
    "quantidadeEstoque": 50,
    "unidade": "UN",
    "avaliacao": 4.5,
    "totalAvaliacoes": 120,
    "marca": "American Crew",
    "modelo": null,
    "peso": 100.0,
    "codigoBarras": "7891234567890",
    "codigoInterno": "POM-001",
    "ingredientes": ["Cera de abelha", "Oleo de argan"],
    "comoUsar": ["Aplique nos cabelos secos", "Modele como desejar"],
    "especificacoes": {
      "Peso": "100g",
      "Fixacao": "Alta",
      "Brilho": "Medio"
    },
    "produtosRelacionados": [
      { "id": 2, "nome": "Shampoo Anti-Residuo", "preco": 35.00, "imagens": ["https://..."] }
    ],
    "seo": {
      "title": "Pomada Modeladora - Barbearia do Je",
      "description": "...",
      "keywords": "pomada, modeladora, cabelo",
      "canonicalUrl": "https://...",
      "ogImage": "https://..."
    }
  }
}
```

---

## 8. EQUIPE

### `GET /{slug}/team`

**Response** `TeamSectionDTO`:

```json
{
  "dados": {
    "title": "Nossa Equipe",
    "subtitle": "Profissionais qualificados",
    "showSection": true,
    "membros": [
      {
        "id": 1,
        "nome": "Joao Silva",
        "apelido": "Joao",
        "foto": "https://...",
        "cargo": "Barbeiro Senior",
        "descricao": "Especialista em cortes modernos...",
        "servicoIds": [1, 2, 3],
        "servicoNomes": ["Corte Masculino", "Barba", "Pigmentacao"],
        "horarios": [
          {
            "diaSemana": "SEGUNDA",
            "diaSemanaLabel": "Segunda-feira",
            "ativo": true,
            "horarios": [
              { "inicio": "09:00", "fim": "12:00" },
              { "inicio": "13:00", "fim": "18:00" }
            ]
          }
        ]
      }
    ],
    "totalMembros": 8
  }
}
```

> **Front-end:** Respeite `showSection`. Se `false`, nao renderize a secao de equipe. Use `servicoNomes` para exibir as especialidades de cada profissional.

---

## 9. AGENDAMENTO

### `GET /{slug}/booking`

**Response** `BookingSectionDTO`:

```json
{
  "dados": {
    "title": "Agende seu Horario",
    "subtitle": "Escolha o melhor horario para voce",
    "enabled": true,
    "servicosDisponiveis": [
      {
        "id": 1,
        "nome": "Corte Masculino",
        "preco": 45.00,
        "tempoEstimadoMinutos": 30,
        "funcionarioIds": [1, 2]
      }
    ],
    "profissionaisDisponiveis": [
      {
        "id": 1,
        "nome": "Joao Silva",
        "foto": "https://...",
        "servicoIds": [1, 2],
        "horarios": [...]
      }
    ],
    "config": {
      "requiresDeposit": false,
      "depositPercentage": null,
      "minAdvanceHours": 2,
      "maxAdvanceDays": 30,
      "allowMultipleServices": true,
      "requiresLogin": false
    }
  }
}
```

#### Regras de negocio para o front-end

| Campo                    | Regra                                                          |
|--------------------------|----------------------------------------------------------------|
| `enabled`                | Se `false`, nao exibir secao de agendamento                    |
| `requiresDeposit`        | Se `true`, informar ao usuario sobre pagamento de sinal        |
| `depositPercentage`      | Percentual do sinal (ex: 20.0 = 20%)                          |
| `minAdvanceHours`        | Minimo de horas de antecedencia para agendar                   |
| `maxAdvanceDays`         | Maximo de dias no futuro para agendar                          |
| `allowMultipleServices`  | Se `true`, permitir selecionar multiplos servicos              |
| `requiresLogin`          | Se `true`, exigir login antes de agendar                       |

---

## 10. ENDPOINTS UTILITARIOS

### `GET /{slug}/exists`

Verifica se um slug existe.

```json
{
  "success": true,
  "message": "Slug encontrado",
  "dados": {
    "slug": "barbeariadoje",
    "exists": true,
    "available": false
  }
}
```

> **Uso:** Validacao em tempo real ao criar/buscar uma organizacao.

### `GET /{slug}/basic`

Retorna apenas informacoes basicas da organizacao (leve).

**Response** `OrganizacaoPublicDTO`:

```json
{
  "dados": {
    "id": 1,
    "nome": "Barbearia do Je LTDA",
    "nomeFantasia": "Barbearia do Je",
    "slug": "barbeariadoje",
    "descricao": "A melhor barbearia da cidade",
    "logo": "https://...",
    "banner": "https://...",
    "telefone": "(11) 3333-4444",
    "whatsapp": "5511999999999",
    "email": "contato@barbearia.com",
    "endereco": {
      "logradouro": "Rua X",
      "numero": "123",
      "complemento": null,
      "bairro": "Centro",
      "cidade": "Sao Paulo",
      "uf": "SP",
      "cep": "01000-000",
      "latitude": -23.5505,
      "longitude": -46.6333,
      "googleMapsUrl": "https://maps.google.com/..."
    }
  }
}
```

### `GET /{slug}` (Legado)

Retorna todos os dados publicos da organizacao no formato antigo. **Mantido para compatibilidade.** Use `/{slug}/home` para novas implementacoes.

**Response** `PublicSiteResponseDTO`:

```json
{
  "dados": {
    "organizacao": { /* OrganizacaoPublicDTO */ },
    "siteConfig": { /* SiteConfigDTO */ },
    "equipe": [ /* FuncionarioPublicDTO[] */ ],
    "servicos": [ /* ServicoPublicDTO[] */ ],
    "categorias": [ /* CategoriaPublicDTO[] */ ],
    "produtosDestaque": [ /* ProdutoPublicDTO[] */ ],
    "horariosFuncionamento": [ /* HorarioFuncionamentoDTO[] */ ],
    "redesSociais": { /* RedesSociaisDTO */ },
    "seo": { /* SeoMetadataDTO */ },
    "features": { /* FeaturesDTO */ }
  }
}
```

---

## 11. TEMA / CONFIGURACAO VISUAL

Presente dentro de `siteConfig.tema` na resposta da home page.

```json
{
  "siteConfig": {
    "tema": {
      "nome": "Dark Gold",
      "tipo": "dark",
      "cores": {
        "primary": "#D4AF37",
        "secondary": "#1a1a2e",
        "accent": "#FFD700",
        "background": "#0f0f23",
        "text": "#FFFFFF",
        "textSecondary": "#B0B0B0",
        "cardBackground": "#1a1a2e",
        "cardBackgroundSecondary": "#252540",
        "buttonText": "#000000",
        "backgroundLinear": "linear-gradient(135deg, #0f0f23 0%, #1a1a2e 100%)",
        "success": "#00C853",
        "warning": "#FFD600",
        "error": "#FF1744",
        "info": "#2979FF",
        "border": "#333366",
        "borderLight": "#444477",
        "divider": "#2a2a4a",
        "overlay": "rgba(0,0,0,0.7)",
        "modalBackground": "#1a1a2e",
        "inputBackground": "#252540",
        "inputBorder": "#333366",
        "inputFocus": "#D4AF37",
        "placeholder": "#666688",
        "navBackground": "#0f0f23",
        "navHover": "#252540",
        "navActive": "#D4AF37",
        "online": "#00C853",
        "offline": "#757575",
        "away": "#FFD600",
        "busy": "#FF1744"
      },
      "fonts": {
        "heading": "Playfair Display",
        "body": "Inter",
        "mono": "JetBrains Mono"
      },
      "borderRadius": {
        "small": "4px",
        "medium": "8px",
        "large": "12px",
        "xl": "16px",
        "full": "9999px"
      },
      "shadows": {
        "base": "0 1px 3px rgba(0,0,0,0.3)",
        "md": "0 4px 6px rgba(0,0,0,0.4)",
        "lg": "0 10px 15px rgba(0,0,0,0.5)",
        "primaryGlow": "0 0 20px rgba(212,175,55,0.3)",
        "accentGlow": "0 0 20px rgba(255,215,0,0.3)"
      }
    },
    "logoUrl": "https://...",
    "faviconUrl": "https://..."
  }
}
```

### Aplicacao do tema no front-end (CSS Variables)

```typescript
function applyTheme(tema: Tema) {
  const root = document.documentElement;

  // Cores
  Object.entries(tema.cores).forEach(([key, value]) => {
    if (value) {
      const cssVar = `--color-${key.replace(/([A-Z])/g, '-$1').toLowerCase()}`;
      root.style.setProperty(cssVar, value);
    }
  });

  // Fonts
  if (tema.fonts) {
    root.style.setProperty('--font-heading', tema.fonts.heading);
    root.style.setProperty('--font-body', tema.fonts.body);
    root.style.setProperty('--font-mono', tema.fonts.mono);
  }

  // Border Radius
  if (tema.borderRadius) {
    Object.entries(tema.borderRadius).forEach(([key, value]) => {
      if (value) root.style.setProperty(`--radius-${key}`, value);
    });
  }

  // Shadows
  if (tema.shadows) {
    Object.entries(tema.shadows).forEach(([key, value]) => {
      if (value) {
        const cssVar = `--shadow-${key.replace(/([A-Z])/g, '-$1').toLowerCase()}`;
        root.style.setProperty(cssVar, value);
      }
    });
  }
}
```

---

## 12. FEATURES (Flags de Funcionalidades)

Presente no `features` da home page. Use para controlar quais modulos exibir.

```json
{
  "features": {
    "agendamentoOnline": true,
    "ecommerce": true,
    "planosClientes": false,
    "avaliacoes": true,
    "chat": false,
    "notificacoesPush": true
  }
}
```

| Flag                 | Efeito no front-end                                   |
|----------------------|-------------------------------------------------------|
| `agendamentoOnline`  | Exibir secao/botoes de agendamento                    |
| `ecommerce`          | Exibir secao de produtos e carrinho                   |
| `planosClientes`     | Exibir area de planos/assinaturas                     |
| `avaliacoes`         | Exibir estrelas e reviews                             |
| `chat`               | Habilitar widget de chat                              |
| `notificacoesPush`   | Solicitar permissao de notificacoes push              |

---

## 13. SEO METADATA

Presente no `seo` da home page. Use para popular tags `<head>`.

```json
{
  "seo": {
    "title": "Barbearia do Je - A melhor barbearia",
    "description": "Cortes modernos, barba e tratamentos capilares...",
    "keywords": "barbearia, corte masculino, barba, sao paulo",
    "ogImage": "https://...",
    "ogTitle": "Barbearia do Je",
    "ogDescription": "Cortes modernos e barba...",
    "canonicalUrl": "https://app.bellory.com.br/barbeariadoje"
  }
}
```

### Implementacao no front-end (Next.js / Nuxt / etc.)

```typescript
// Next.js exemplo
export function generateMetadata(seo: SeoMetadataDTO) {
  return {
    title: seo.title,
    description: seo.description,
    keywords: seo.keywords,
    openGraph: {
      title: seo.ogTitle || seo.title,
      description: seo.ogDescription || seo.description,
      images: seo.ogImage ? [{ url: seo.ogImage }] : [],
    },
    alternates: {
      canonical: seo.canonicalUrl,
    },
  };
}
```

---

## 14. MAPA COMPLETO DE ROTAS

| Metodo | Endpoint                        | Descricao                          | Response DTO            |
|--------|---------------------------------|------------------------------------|--------------------------|
| GET    | `/{slug}/home`                  | Home page completa                 | `HomePageDTO`            |
| GET    | `/{slug}/header`                | Configuracao do header             | `HeaderConfigDTO`        |
| GET    | `/{slug}/hero`                  | Banner/hero principal              | `HeroSectionDTO`         |
| GET    | `/{slug}/about`                 | Sobre (resumido)                   | `AboutSectionDTO`        |
| GET    | `/{slug}/about/full`            | Sobre (completo)                   | `AboutSectionDTO`        |
| GET    | `/{slug}/footer`                | Configuracao do footer             | `FooterConfigDTO`        |
| GET    | `/{slug}/services/featured`     | Servicos em destaque               | `ServicesSectionDTO`     |
| GET    | `/{slug}/services?page=&size=`  | Todos os servicos (paginado)       | `ServicesSectionDTO`     |
| GET    | `/{slug}/services/{id}`         | Detalhe de um servico              | `ServicoDetalhadoDTO`    |
| GET    | `/{slug}/products/featured`     | Produtos em destaque               | `ProductsSectionDTO`     |
| GET    | `/{slug}/products?page=&size=`  | Todos os produtos (paginado)       | `ProductsSectionDTO`     |
| GET    | `/{slug}/products/{id}`         | Detalhe de um produto              | `ProdutoDetalhadoDTO`    |
| GET    | `/{slug}/team`                  | Equipe                             | `TeamSectionDTO`         |
| GET    | `/{slug}/booking`               | Info de agendamento                | `BookingSectionDTO`      |
| GET    | `/{slug}`                       | Dados completos (legado)           | `PublicSiteResponseDTO`  |
| GET    | `/{slug}/exists`                | Verificar se slug existe           | `Map<String, Object>`   |
| GET    | `/{slug}/basic`                 | Info basica da organizacao         | `OrganizacaoPublicDTO`   |

---

## 15. SUGESTAO DE ESTRUTURA DE PAGINAS NO FRONT-END

```
/[slug]                    -> Home Page          (GET /{slug}/home)
/[slug]/sobre              -> Pagina Sobre       (GET /{slug}/about/full)
/[slug]/servicos           -> Lista de Servicos  (GET /{slug}/services?page=0&size=12)
/[slug]/servicos/[id]      -> Detalhe Servico    (GET /{slug}/services/{id})
/[slug]/produtos           -> Lista de Produtos  (GET /{slug}/products?page=0&size=12)
/[slug]/produtos/[id]      -> Detalhe Produto    (GET /{slug}/products/{id})
/[slug]/equipe             -> Pagina Equipe      (GET /{slug}/team)
/[slug]/agendar            -> Agendamento        (GET /{slug}/booking)
```

### Componentes sugeridos

```
components/
  layout/
    Header.tsx          <- HeaderConfigDTO
    Footer.tsx          <- FooterConfigDTO
  sections/
    HeroSection.tsx     <- HeroSectionDTO
    AboutSection.tsx    <- AboutSectionDTO
    ServicesSection.tsx  <- ServicesSectionDTO
    ProductsSection.tsx <- ProductsSectionDTO
    TeamSection.tsx     <- TeamSectionDTO
    BookingSection.tsx  <- BookingSectionDTO
  pages/
    HomePage.tsx        <- HomePageDTO (orquestra todas as sections)
    AboutPage.tsx       <- AboutSectionDTO (full)
    ServicesList.tsx    <- ServicesSectionDTO (paginado)
    ServiceDetail.tsx   <- ServicoDetalhadoDTO
    ProductsList.tsx    <- ProductsSectionDTO (paginado)
    ProductDetail.tsx   <- ProdutoDetalhadoDTO
    TeamPage.tsx        <- TeamSectionDTO
    BookingPage.tsx     <- BookingSectionDTO
  shared/
    ThemeProvider.tsx   <- Aplica Tema via CSS variables
    SeoHead.tsx         <- Aplica SeoMetadataDTO nas meta tags
    FeatureGate.tsx     <- Condiciona renderizacao via FeaturesDTO
```

---

## 16. EXEMPLO DE SERVICO HTTP (TypeScript/Axios)

```typescript
import axios from 'axios';

const API_BASE = 'https://api.bellory.com.br/api/public/site';

interface ResponseAPI<T> {
  success: boolean;
  message: string;
  dados: T;
  errorCode?: number;
  errors?: Record<string, string>;
}

export const publicSiteApi = {
  // Home page completa
  getHomePage: (slug: string) =>
    axios.get<ResponseAPI<HomePageDTO>>(`${API_BASE}/${slug}/home`),

  // Secoes individuais
  getHeader: (slug: string) =>
    axios.get<ResponseAPI<HeaderConfigDTO>>(`${API_BASE}/${slug}/header`),

  getHero: (slug: string) =>
    axios.get<ResponseAPI<HeroSectionDTO>>(`${API_BASE}/${slug}/hero`),

  getAbout: (slug: string, full = false) =>
    axios.get<ResponseAPI<AboutSectionDTO>>(`${API_BASE}/${slug}/about${full ? '/full' : ''}`),

  getFooter: (slug: string) =>
    axios.get<ResponseAPI<FooterConfigDTO>>(`${API_BASE}/${slug}/footer`),

  // Servicos
  getFeaturedServices: (slug: string) =>
    axios.get<ResponseAPI<ServicesSectionDTO>>(`${API_BASE}/${slug}/services/featured`),

  getAllServices: (slug: string, page = 0, size = 12) =>
    axios.get<ResponseAPI<ServicesSectionDTO>>(`${API_BASE}/${slug}/services`, {
      params: { page, size }
    }),

  getServiceById: (slug: string, id: number) =>
    axios.get<ResponseAPI<ServicoDetalhadoDTO>>(`${API_BASE}/${slug}/services/${id}`),

  // Produtos
  getFeaturedProducts: (slug: string) =>
    axios.get<ResponseAPI<ProductsSectionDTO>>(`${API_BASE}/${slug}/products/featured`),

  getAllProducts: (slug: string, page = 0, size = 12) =>
    axios.get<ResponseAPI<ProductsSectionDTO>>(`${API_BASE}/${slug}/products`, {
      params: { page, size }
    }),

  getProductById: (slug: string, id: number) =>
    axios.get<ResponseAPI<ProdutoDetalhadoDTO>>(`${API_BASE}/${slug}/products/${id}`),

  // Equipe
  getTeam: (slug: string) =>
    axios.get<ResponseAPI<TeamSectionDTO>>(`${API_BASE}/${slug}/team`),

  // Agendamento
  getBookingInfo: (slug: string) =>
    axios.get<ResponseAPI<BookingSectionDTO>>(`${API_BASE}/${slug}/booking`),

  // Utilitarios
  checkSlugExists: (slug: string) =>
    axios.get<ResponseAPI<{ slug: string; exists: boolean; available: boolean }>>(
      `${API_BASE}/${slug}/exists`
    ),

  getBasicInfo: (slug: string) =>
    axios.get<ResponseAPI<OrganizacaoPublicDTO>>(`${API_BASE}/${slug}/basic`),
};
```

---

## 17. TRATAMENTO DE ERROS NO FRONT-END

```typescript
async function fetchWithErrorHandling<T>(request: Promise<AxiosResponse<ResponseAPI<T>>>) {
  try {
    const { data } = await request;

    if (!data.success) {
      throw new ApiError(data.message, data.errorCode);
    }

    return data.dados;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const body = error.response?.data as ResponseAPI<T> | undefined;

      switch (status) {
        case 400:
          throw new ApiError(body?.message || 'Slug invalido', 400);
        case 404:
          throw new ApiError(body?.message || 'Nao encontrado', 404);
        case 500:
          throw new ApiError(body?.message || 'Erro interno', 500);
        default:
          throw new ApiError('Erro de conexao', 0);
      }
    }
    throw error;
  }
}

// Uso
const homePage = await fetchWithErrorHandling(publicSiteApi.getHomePage('barbeariadoje'));
```

> **Nota:** Campos `null` sao omitidos no JSON (via `@JsonInclude(NON_NULL)`). Sempre verifique se um campo existe antes de usa-lo no front-end.
