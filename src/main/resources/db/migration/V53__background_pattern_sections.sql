-- backgroundPattern e patternOpacity para cada seção
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS header_background_pattern VARCHAR(30);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS header_pattern_opacity DOUBLE PRECISION;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_background_pattern VARCHAR(30);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_pattern_opacity DOUBLE PRECISION;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_background_pattern VARCHAR(30);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS about_pattern_opacity DOUBLE PRECISION;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_background_pattern VARCHAR(30);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS footer_pattern_opacity DOUBLE PRECISION;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_background_pattern VARCHAR(30);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS services_pattern_opacity DOUBLE PRECISION;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_background_pattern VARCHAR(30);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS products_pattern_opacity DOUBLE PRECISION;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_background_pattern VARCHAR(30);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS team_pattern_opacity DOUBLE PRECISION;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS booking_background_pattern VARCHAR(30);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS booking_pattern_opacity DOUBLE PRECISION;
