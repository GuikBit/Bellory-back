-- V26: Adicionar colunas de logo e banner na organizacao

ALTER TABLE app.organizacao ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);
ALTER TABLE app.organizacao ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500);
