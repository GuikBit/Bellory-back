-- Novos campos de configuração do Footer
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_layout VARCHAR(20) DEFAULT 'columns';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_logo_height INTEGER DEFAULT 40;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_show_logo BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_social_style VARCHAR(30) DEFAULT 'icon-rounded';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_divider_style VARCHAR(20) DEFAULT 'line';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_show_contact BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_show_back_to_top BOOLEAN DEFAULT false;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_newsletter_title VARCHAR(255);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_newsletter_placeholder VARCHAR(255);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_columns INTEGER DEFAULT 4;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_compact_hours BOOLEAN DEFAULT false;
