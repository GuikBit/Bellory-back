-- ============================================
-- V67: Flag is_sistema em arquivos internos
-- ============================================
-- Marca arquivos do sistema interno (ex.: assinaturas de termo de consentimento)
-- para que nao apareçam na listagem do file explorer do tenant nem contem na cota
-- de armazenamento do plano.

ALTER TABLE app.arquivo
    ADD COLUMN IF NOT EXISTS is_sistema BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_arquivo_is_sistema ON app.arquivo(is_sistema);

COMMENT ON COLUMN app.arquivo.is_sistema IS 'Quando true, arquivo e interno do sistema (ex.: assinatura de termo) e nao e listado nem contabilizado para o tenant';
