-- Novos campos de configuração da seção Products
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_layout VARCHAR(20) DEFAULT 'grid';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_card_style VARCHAR(20) DEFAULT 'default';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_columns INTEGER DEFAULT 4;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_card_image_height INTEGER DEFAULT 220;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_show_rating BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_show_category BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_show_description BOOLEAN DEFAULT false;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_show_discount BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_show_stock BOOLEAN DEFAULT false;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_show_add_to_cart BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_hover_effect VARCHAR(20) DEFAULT 'zoom';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_badge_style VARCHAR(20) DEFAULT 'pill';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_auto_play BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_auto_play_speed INTEGER DEFAULT 4000;
