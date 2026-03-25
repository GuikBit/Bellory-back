-- Novos campos de configuração do header
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS header_layout VARCHAR(20) DEFAULT 'default';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS header_menu_style VARCHAR(20) DEFAULT 'default';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS header_transparent_on_hero BOOLEAN DEFAULT false;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS header_show_cart BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS header_logo_height INTEGER DEFAULT 40;
