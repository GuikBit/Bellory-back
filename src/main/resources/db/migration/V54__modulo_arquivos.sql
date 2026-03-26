-- ========================================
-- V54 - Módulo de Gerenciamento de Arquivos
-- ========================================

-- Tabela de pastas (suporte a hierarquia)
CREATE TABLE app.pasta_arquivo (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id) ON DELETE CASCADE,
    pasta_pai_id BIGINT REFERENCES app.pasta_arquivo(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL,
    caminho_completo VARCHAR(1000) NOT NULL,
    dt_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    dt_atualizacao TIMESTAMP NOT NULL DEFAULT NOW(),
    criado_por BIGINT,
    UNIQUE(organizacao_id, caminho_completo)
);

CREATE INDEX idx_pasta_arquivo_org ON app.pasta_arquivo(organizacao_id);
CREATE INDEX idx_pasta_arquivo_pai ON app.pasta_arquivo(pasta_pai_id);

-- Tabela de arquivos (metadados)
CREATE TABLE app.arquivo (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id) ON DELETE CASCADE,
    pasta_id BIGINT REFERENCES app.pasta_arquivo(id) ON DELETE SET NULL,
    nome_original VARCHAR(500) NOT NULL,
    nome_armazenado VARCHAR(500) NOT NULL,
    caminho_relativo VARCHAR(1000) NOT NULL,
    extensao VARCHAR(20),
    content_type VARCHAR(150),
    tamanho BIGINT NOT NULL,
    dt_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    criado_por BIGINT
);

CREATE INDEX idx_arquivo_org ON app.arquivo(organizacao_id);
CREATE INDEX idx_arquivo_pasta ON app.arquivo(pasta_id);
CREATE INDEX idx_arquivo_org_pasta ON app.arquivo(organizacao_id, pasta_id);

-- Adicionar limite de storage nos planos
ALTER TABLE admin.plano_limites_bellory
    ADD COLUMN IF NOT EXISTS max_storage_mb INTEGER DEFAULT 500;

-- Comentários
COMMENT ON TABLE app.pasta_arquivo IS 'Pastas para organização de arquivos por organização';
COMMENT ON TABLE app.arquivo IS 'Metadados dos arquivos enviados por organização';
COMMENT ON COLUMN admin.plano_limites_bellory.max_storage_mb IS 'Limite de armazenamento em MB por organização (null = ilimitado)';
