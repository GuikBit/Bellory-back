-- Coluna JSON para configuração de transições entre seções
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS transitions TEXT;
