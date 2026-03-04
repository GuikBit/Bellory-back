-- =============================================================
-- V36: Sistema de Cupom de Desconto para Assinaturas
-- =============================================================

-- Tabela principal de cupons
CREATE TABLE IF NOT EXISTS admin.cupom_desconto (
    id                      BIGSERIAL       PRIMARY KEY,
    codigo                  VARCHAR(50)     NOT NULL UNIQUE,
    descricao               VARCHAR(255),
    tipo_desconto           VARCHAR(20)     NOT NULL, -- PERCENTUAL, VALOR_FIXO
    valor_desconto          NUMERIC(10,2)   NOT NULL,
    dt_inicio               TIMESTAMP,
    dt_fim                  TIMESTAMP,
    max_utilizacoes         INTEGER,
    max_utilizacoes_por_org INTEGER,
    total_utilizado         INTEGER         NOT NULL DEFAULT 0,
    planos_permitidos       JSONB,          -- ["basico","plus","premium"]
    segmentos_permitidos    JSONB,          -- ["Barbearia","Salao de Beleza"]
    organizacoes_permitidas JSONB,          -- [1, 5, 10]
    ciclo_cobranca          VARCHAR(10),    -- MENSAL, ANUAL ou null (ambos)
    ativo                   BOOLEAN         NOT NULL DEFAULT TRUE,
    dt_criacao              TIMESTAMP       NOT NULL DEFAULT NOW(),
    dt_atualizacao          TIMESTAMP,
    user_criacao            BIGINT,
    user_atualizacao        BIGINT
);

CREATE INDEX idx_cupom_desconto_codigo ON admin.cupom_desconto (codigo);
CREATE INDEX idx_cupom_desconto_ativo ON admin.cupom_desconto (ativo);

-- Tabela de utilizacoes de cupom
CREATE TABLE IF NOT EXISTS admin.cupom_utilizacao (
    id                  BIGSERIAL       PRIMARY KEY,
    cupom_id            BIGINT          NOT NULL REFERENCES admin.cupom_desconto(id),
    organizacao_id      BIGINT          NOT NULL,
    assinatura_id       BIGINT          REFERENCES admin.assinatura(id),
    cobranca_id         BIGINT          REFERENCES admin.cobranca_plataforma(id),
    valor_original      NUMERIC(10,2)   NOT NULL,
    valor_desconto      NUMERIC(10,2)   NOT NULL,
    valor_final         NUMERIC(10,2)   NOT NULL,
    plano_codigo        VARCHAR(50),
    ciclo_cobranca      VARCHAR(10),
    dt_utilizacao       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cupom_utilizacao_cupom ON admin.cupom_utilizacao (cupom_id);
CREATE INDEX idx_cupom_utilizacao_org ON admin.cupom_utilizacao (organizacao_id);

-- Adicionar campos de cupom na assinatura
ALTER TABLE admin.assinatura
    ADD COLUMN IF NOT EXISTS cupom_id        BIGINT REFERENCES admin.cupom_desconto(id),
    ADD COLUMN IF NOT EXISTS valor_desconto  NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS cupom_codigo    VARCHAR(50);

-- Adicionar campos de cupom na cobranca_plataforma
ALTER TABLE admin.cobranca_plataforma
    ADD COLUMN IF NOT EXISTS cupom_id                BIGINT REFERENCES admin.cupom_desconto(id),
    ADD COLUMN IF NOT EXISTS valor_original          NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS valor_desconto_aplicado NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS cupom_codigo            VARCHAR(50);
