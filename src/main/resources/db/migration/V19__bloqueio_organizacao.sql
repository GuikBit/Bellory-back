-- V19: Tabela de bloqueios/feriados da organização
-- Permite cadastrar feriados nacionais, regionais e bloqueios manuais
-- que impedem agendamentos em determinados dias para toda a organização.

CREATE TABLE IF NOT EXISTS app.bloqueio_organizacao (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    tipo VARCHAR(50) NOT NULL,       -- FERIADO ou BLOQUEIO
    descricao VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    origem VARCHAR(50) NOT NULL,     -- NACIONAL ou MANUAL
    ano_referencia INTEGER,          -- Ano de referência para feriados importados
    dt_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    dt_atualizacao TIMESTAMP,

    CONSTRAINT fk_bloqueio_org_organizacao
        FOREIGN KEY (organizacao_id) REFERENCES app.organizacao(id) ON DELETE CASCADE,

    CONSTRAINT chk_bloqueio_org_tipo
        CHECK (tipo IN ('FERIADO', 'BLOQUEIO')),

    CONSTRAINT chk_bloqueio_org_origem
        CHECK (origem IN ('NACIONAL', 'MANUAL')),

    CONSTRAINT chk_bloqueio_org_datas
        CHECK (data_fim >= data_inicio)
);

-- Índices para consultas frequentes
CREATE INDEX IF NOT EXISTS idx_bloqueio_org_organizacao_id ON app.bloqueio_organizacao(organizacao_id);
CREATE INDEX IF NOT EXISTS idx_bloqueio_org_datas ON app.bloqueio_organizacao(data_inicio, data_fim);
CREATE INDEX IF NOT EXISTS idx_bloqueio_org_ativo ON app.bloqueio_organizacao(organizacao_id, ativo);
CREATE INDEX IF NOT EXISTS idx_bloqueio_org_ano ON app.bloqueio_organizacao(organizacao_id, ano_referencia);
