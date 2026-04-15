-- backgroundColor para cada seção do site
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS header_background_color VARCHAR(20);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_background_color VARCHAR(20);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_background_color VARCHAR(20);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_background_color VARCHAR(20);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_background_color VARCHAR(20);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_background_color VARCHAR(20);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_background_color VARCHAR(20);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS booking_background_color VARCHAR(20);
