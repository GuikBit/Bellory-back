-- Novos campos de configuração da seção Services
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_card_style VARCHAR(20) DEFAULT 'default';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_show_category BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_show_description BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_show_image BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_show_discount BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_card_image_height INTEGER DEFAULT 200;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_show_category_filter BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_columns INTEGER DEFAULT 3;
