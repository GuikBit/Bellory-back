-- Novos campos de configuração da seção About
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_show_gallery BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_show_highlights BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_show_mvv BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_layout_style VARCHAR(30) DEFAULT 'default';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_year_founded INTEGER;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_team_photo_url VARCHAR(500);
