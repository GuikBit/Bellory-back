-- =====================================================================
-- V72 - Importacao de clientes via CSV
-- Rastreia importacoes em massa: status, contadores e erros por linha.
-- Async: o request retorna importId imediatamente; processamento e
-- concluido em background pela ClienteImportacaoService.
-- =====================================================================

CREATE TABLE IF NOT EXISTS app.cliente_importacao (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL,
    usuario_id BIGINT NULL,
    nome_arquivo VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    total_linhas INTEGER NOT NULL DEFAULT 0,
    processadas INTEGER NOT NULL DEFAULT 0,
    importados INTEGER NOT NULL DEFAULT 0,
    ignorados INTEGER NOT NULL DEFAULT 0,
    erros JSONB NULL,
    mensagem_falha TEXT NULL,
    dt_inicio TIMESTAMP NOT NULL DEFAULT now(),
    dt_fim TIMESTAMP NULL,
    CONSTRAINT fk_cli_imp_organizacao
        FOREIGN KEY (organizacao_id) REFERENCES app.organizacao (id)
);

CREATE INDEX IF NOT EXISTS idx_cli_imp_org_status
    ON app.cliente_importacao (organizacao_id, status, dt_inicio DESC);
