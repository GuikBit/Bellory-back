-- ============================================
-- V69: Soft-delete em resposta_questionario (LGPD)
-- ============================================
-- Respostas com termo de consentimento aceito ou assinatura digital nao podem
-- ser hard-deleted (LGPD Art. 16, II — retencao para cumprimento de obrigacao legal).
-- Soft-delete preserva o registro mantendo a possibilidade de auditoria.

ALTER TABLE app.resposta_questionario
    ADD COLUMN IF NOT EXISTS is_deletado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS usuario_deletado VARCHAR(255),
    ADD COLUMN IF NOT EXISTS dt_deletado TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_resposta_questionario_deletado
    ON app.resposta_questionario(is_deletado);

COMMENT ON COLUMN app.resposta_questionario.is_deletado IS 'Soft-delete obrigatorio quando ha termo aceito ou assinatura (retencao legal LGPD Art. 16, II)';
COMMENT ON COLUMN app.resposta_questionario.usuario_deletado IS 'Username do usuario que executou a delecao (audit trail)';
COMMENT ON COLUMN app.resposta_questionario.dt_deletado IS 'Timestamp da delecao (servidor)';
