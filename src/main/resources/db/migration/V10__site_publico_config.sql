-- Migration para criar a tabela de configuração do site público
-- Esta tabela armazena todas as configurações personalizáveis do site externo do cliente

CREATE TABLE IF NOT EXISTS app.site_publico_config (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL UNIQUE,

    -- Header Config
    header_logo_url VARCHAR(500),
    header_logo_alt VARCHAR(100),
    header_menu_items TEXT, -- JSON array de itens do menu
    header_action_buttons TEXT, -- JSON array de botões de ação
    header_show_phone BOOLEAN DEFAULT TRUE,
    header_show_social BOOLEAN DEFAULT FALSE,
    header_sticky BOOLEAN DEFAULT TRUE,

    -- Hero Config
    hero_type VARCHAR(20) DEFAULT 'TEMPLATE', -- TEMPLATE ou CUSTOM_HTML
    hero_title VARCHAR(255),
    hero_subtitle TEXT,
    hero_background_url VARCHAR(500),
    hero_background_overlay DOUBLE PRECISION DEFAULT 0.5,
    hero_custom_html TEXT, -- HTML customizado quando hero_type = CUSTOM_HTML
    hero_buttons TEXT, -- JSON array de botões
    hero_show_booking_form BOOLEAN DEFAULT FALSE,

    -- About Section
    about_title VARCHAR(255),
    about_subtitle VARCHAR(255),
    about_description TEXT,
    about_full_description TEXT, -- Descrição completa para página dedicada
    about_image_url VARCHAR(500),
    about_gallery_images TEXT, -- JSON array de URLs
    about_video_url VARCHAR(500),
    about_highlights TEXT, -- JSON array de highlights/diferenciais
    about_mission TEXT,
    about_vision TEXT,
    about_values TEXT,

    -- Services Section
    services_section_title VARCHAR(255) DEFAULT 'Nossos Serviços',
    services_section_subtitle VARCHAR(255),
    services_show_prices BOOLEAN DEFAULT TRUE,
    services_show_duration BOOLEAN DEFAULT TRUE,
    services_featured_limit INTEGER DEFAULT 6,

    -- Products Section
    products_section_title VARCHAR(255) DEFAULT 'Produtos em Destaque',
    products_section_subtitle VARCHAR(255),
    products_show_prices BOOLEAN DEFAULT TRUE,
    products_featured_limit INTEGER DEFAULT 8,

    -- Team Section
    team_section_title VARCHAR(255) DEFAULT 'Nossa Equipe',
    team_section_subtitle VARCHAR(255),
    team_show_section BOOLEAN DEFAULT TRUE,

    -- Booking Section
    booking_section_title VARCHAR(255) DEFAULT 'Agende seu Horário',
    booking_section_subtitle VARCHAR(255),
    booking_enabled BOOLEAN DEFAULT TRUE,

    -- Footer Config
    footer_description TEXT,
    footer_logo_url VARCHAR(500),
    footer_link_sections TEXT, -- JSON array de seções com links
    footer_copyright_text VARCHAR(255),
    footer_show_map BOOLEAN DEFAULT TRUE,
    footer_show_hours BOOLEAN DEFAULT TRUE,
    footer_show_social BOOLEAN DEFAULT TRUE,
    footer_show_newsletter BOOLEAN DEFAULT FALSE,

    -- General Settings
    home_sections_order TEXT DEFAULT '["HERO","ABOUT","SERVICES","PRODUCTS","TEAM","BOOKING"]', -- JSON array
    custom_css TEXT,
    custom_js TEXT,
    external_scripts TEXT, -- JSON array de scripts externos

    active BOOLEAN DEFAULT TRUE,
    dt_criacao TIMESTAMP DEFAULT NOW(),
    dt_atualizacao TIMESTAMP,

    -- Foreign Key
    CONSTRAINT fk_site_config_organizacao
        FOREIGN KEY (organizacao_id)
        REFERENCES app.organizacao(id)
        ON DELETE CASCADE
);

-- Índice para busca por organização
CREATE INDEX IF NOT EXISTS idx_site_config_org
    ON app.site_publico_config(organizacao_id);

-- Índice para configurações ativas
CREATE INDEX IF NOT EXISTS idx_site_config_active
    ON app.site_publico_config(organizacao_id, active)
    WHERE active = TRUE;

-- Comentários nas colunas principais
COMMENT ON TABLE app.site_publico_config IS 'Configurações do site público da organização';
COMMENT ON COLUMN app.site_publico_config.hero_type IS 'Tipo do hero: TEMPLATE (usa campos) ou CUSTOM_HTML (usa hero_custom_html)';
COMMENT ON COLUMN app.site_publico_config.header_menu_items IS 'JSON: [{"label": "Início", "href": "/", "order": 1}]';
COMMENT ON COLUMN app.site_publico_config.hero_buttons IS 'JSON: [{"label": "Agendar", "href": "#agendar", "type": "primary"}]';
COMMENT ON COLUMN app.site_publico_config.about_highlights IS 'JSON: [{"icon": "star", "title": "10+ Anos", "description": "De experiência"}]';
COMMENT ON COLUMN app.site_publico_config.footer_link_sections IS 'JSON: [{"title": "Links", "links": [{"label": "Início", "href": "/"}]}]';
COMMENT ON COLUMN app.site_publico_config.home_sections_order IS 'Ordem das seções na home: HERO, ABOUT, SERVICES, PRODUCTS, TEAM, BOOKING, TESTIMONIALS';
