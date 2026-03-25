-- Novos campos de configuração da seção Team
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_layout VARCHAR(20) DEFAULT 'grid';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_card_style VARCHAR(20) DEFAULT 'default';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_photo_shape VARCHAR(20) DEFAULT 'square';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_photo_height INTEGER DEFAULT 280;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_show_bio BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_show_services BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_show_schedule BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_carousel_auto_play BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_carousel_speed INTEGER DEFAULT 4000;
