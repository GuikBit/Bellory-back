-- Migration para criar tabelas do Editor de Landing Pages
-- Sistema completo para criação e edição de landing pages customizáveis

-- ==================== TABELA PRINCIPAL: LANDING PAGE ====================

CREATE TABLE IF NOT EXISTS app.landing_page (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL,

    -- Identificação
    slug VARCHAR(100) NOT NULL,
    nome VARCHAR(200) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(50) DEFAULT 'HOME', -- HOME, PROMOCAO, EVENTO, CAMPANHA, CUSTOM

    -- Flags
    is_home BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED

    -- Configurações globais (JSON)
    global_settings TEXT, -- {"theme": "dark", "primaryColor": "#ff6b00", ...}
    seo_settings TEXT, -- {"title": "...", "description": "...", ...}

    -- Customizações
    custom_css TEXT,
    custom_js TEXT,
    favicon_url VARCHAR(500),

    -- Versionamento
    versao INTEGER DEFAULT 1,
    dt_publicacao TIMESTAMP,
    publicado_por BIGINT,

    -- Auditoria
    ativo BOOLEAN DEFAULT TRUE,
    dt_criacao TIMESTAMP DEFAULT NOW(),
    dt_atualizacao TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_landing_page_organizacao
        FOREIGN KEY (organizacao_id)
        REFERENCES app.organizacao(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_landing_page_slug
        UNIQUE (organizacao_id, slug)
);

-- Índices para landing_page
CREATE INDEX IF NOT EXISTS idx_landing_page_org
    ON app.landing_page(organizacao_id);

CREATE INDEX IF NOT EXISTS idx_landing_page_status
    ON app.landing_page(organizacao_id, status)
    WHERE ativo = TRUE;

CREATE INDEX IF NOT EXISTS idx_landing_page_home
    ON app.landing_page(organizacao_id, is_home)
    WHERE is_home = TRUE AND ativo = TRUE;


-- ==================== TABELA DE SEÇÕES ====================

CREATE TABLE IF NOT EXISTS app.landing_page_section (
    id BIGSERIAL PRIMARY KEY,
    landing_page_id BIGINT NOT NULL,

    -- Identificação
    section_id VARCHAR(100) NOT NULL, -- UUID único da seção
    tipo VARCHAR(50) NOT NULL, -- HEADER, HERO, ABOUT, SERVICES, PRODUCTS, etc.
    nome VARCHAR(200),

    -- Ordenação e visibilidade
    ordem INTEGER NOT NULL DEFAULT 0,
    visivel BOOLEAN DEFAULT TRUE,

    -- Template/Variante
    template VARCHAR(100), -- hero-centered, hero-split, services-grid, etc.

    -- Conteúdo e configuração (JSON)
    content TEXT, -- Estrutura de elementos da seção
    styles TEXT, -- Estilos responsivos da seção
    settings TEXT, -- Configurações específicas do tipo
    animations TEXT, -- Configurações de animação
    visibility_rules TEXT, -- Regras de visibilidade por device
    data_source TEXT, -- Fonte de dados dinâmicos

    -- Flags
    locked BOOLEAN DEFAULT FALSE, -- Se está bloqueada para edição

    -- Auditoria
    ativo BOOLEAN DEFAULT TRUE,
    dt_criacao TIMESTAMP DEFAULT NOW(),
    dt_atualizacao TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_section_landing_page
        FOREIGN KEY (landing_page_id)
        REFERENCES app.landing_page(id)
        ON DELETE CASCADE
);

-- Índices para landing_page_section
CREATE INDEX IF NOT EXISTS idx_section_landing_page
    ON app.landing_page_section(landing_page_id);

CREATE INDEX IF NOT EXISTS idx_section_order
    ON app.landing_page_section(landing_page_id, ordem)
    WHERE ativo = TRUE;

CREATE INDEX IF NOT EXISTS idx_section_id
    ON app.landing_page_section(section_id);


-- ==================== TABELA DE VERSÕES ====================

CREATE TABLE IF NOT EXISTS app.landing_page_version (
    id BIGSERIAL PRIMARY KEY,
    landing_page_id BIGINT NOT NULL,

    -- Versão
    versao INTEGER NOT NULL,

    -- Snapshot completo da página (JSON)
    snapshot TEXT NOT NULL,

    -- Metadados
    descricao VARCHAR(500),
    tipo VARCHAR(20) DEFAULT 'MANUAL', -- AUTO_SAVE, MANUAL, PUBLISH

    -- Quem criou
    criado_por BIGINT,
    criado_por_nome VARCHAR(200),

    -- Auditoria
    dt_criacao TIMESTAMP DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_version_landing_page
        FOREIGN KEY (landing_page_id)
        REFERENCES app.landing_page(id)
        ON DELETE CASCADE
);

-- Índices para landing_page_version
CREATE INDEX IF NOT EXISTS idx_version_landing_page
    ON app.landing_page_version(landing_page_id);

CREATE INDEX IF NOT EXISTS idx_version_number
    ON app.landing_page_version(landing_page_id, versao DESC);


-- ==================== COMENTÁRIOS ====================

COMMENT ON TABLE app.landing_page IS 'Landing pages customizáveis da organização';
COMMENT ON TABLE app.landing_page_section IS 'Seções que compõem uma landing page';
COMMENT ON TABLE app.landing_page_version IS 'Histórico de versões das landing pages';

COMMENT ON COLUMN app.landing_page.global_settings IS 'JSON: {"theme": "dark", "primaryColor": "#ff6b00", "fontFamily": "Inter"}';
COMMENT ON COLUMN app.landing_page.seo_settings IS 'JSON: {"title": "...", "description": "...", "ogImage": "..."}';

COMMENT ON COLUMN app.landing_page_section.content IS 'JSON com estrutura de elementos: {"layout": "...", "elements": [...]}';
COMMENT ON COLUMN app.landing_page_section.styles IS 'JSON com estilos responsivos: {"desktop": {...}, "tablet": {...}, "mobile": {...}}';
COMMENT ON COLUMN app.landing_page_section.data_source IS 'JSON: {"source": "services", "filter": {"isHome": true}, "limit": 6}';

COMMENT ON COLUMN app.landing_page_version.snapshot IS 'JSON completo da página no momento da versão';
