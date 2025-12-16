
-- ALTER TABLE organizacao
--     ADD slug VARCHAR(255);
--
-- ALTER TABLE organizacao
--     ALTER COLUMN slug SET NOT NULL;

ALTER TABLE organizacao ADD COLUMN IF NOT EXISTS slug VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS idx_organizacao_slug ON organizacao(slug) WHERE slug IS NOT NULL;

