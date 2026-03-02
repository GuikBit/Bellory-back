-- ========================================
-- V35: Sistema de Assinatura e Cobranca da Plataforma
-- Schema: admin
-- ========================================

-- Tabela de Assinaturas
CREATE TABLE admin.assinatura (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id),
    plano_bellory_id BIGINT NOT NULL REFERENCES admin.plano_bellory(id),

    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    ciclo_cobranca VARCHAR(10) NOT NULL DEFAULT 'MENSAL',

    -- Trial
    dt_inicio_trial TIMESTAMP,
    dt_fim_trial TIMESTAMP,

    -- Assinatura ativa
    dt_inicio TIMESTAMP,
    dt_proximo_vencimento TIMESTAMP,
    dt_cancelamento TIMESTAMP,

    -- Valor atual
    valor_mensal DECIMAL(10,2),
    valor_anual DECIMAL(10,2),

    -- Integracao Assas
    assas_customer_id VARCHAR(100),
    assas_subscription_id VARCHAR(100),

    -- Auditoria
    dt_criacao TIMESTAMP NOT NULL DEFAULT now(),
    dt_atualizacao TIMESTAMP,

    CONSTRAINT uk_assinatura_organizacao UNIQUE (organizacao_id)
);

CREATE INDEX idx_assinatura_status ON admin.assinatura(status);
CREATE INDEX idx_assinatura_dt_fim_trial ON admin.assinatura(dt_fim_trial) WHERE status = 'TRIAL';
CREATE INDEX idx_assinatura_dt_proximo_vencimento ON admin.assinatura(dt_proximo_vencimento);

-- Tabela de Cobrancas da Plataforma
CREATE TABLE admin.cobranca_plataforma (
    id BIGSERIAL PRIMARY KEY,
    assinatura_id BIGINT NOT NULL REFERENCES admin.assinatura(id),
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id),

    valor DECIMAL(10,2) NOT NULL,
    dt_vencimento DATE NOT NULL,
    dt_pagamento TIMESTAMP,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    forma_pagamento VARCHAR(20),

    -- Dados Assas
    assas_payment_id VARCHAR(100),
    assas_invoice_url VARCHAR(500),
    assas_bank_slip_url VARCHAR(500),
    assas_pix_qr_code TEXT,
    assas_pix_copia_cola TEXT,

    referencia_mes INTEGER,
    referencia_ano INTEGER,

    dt_criacao TIMESTAMP NOT NULL DEFAULT now(),
    dt_atualizacao TIMESTAMP
);

CREATE INDEX idx_cobranca_plat_assinatura ON admin.cobranca_plataforma(assinatura_id);
CREATE INDEX idx_cobranca_plat_organizacao ON admin.cobranca_plataforma(organizacao_id);
CREATE INDEX idx_cobranca_plat_status ON admin.cobranca_plataforma(status);
CREATE INDEX idx_cobranca_plat_vencimento ON admin.cobranca_plataforma(dt_vencimento) WHERE status = 'PENDENTE';

-- Tabela de Pagamentos da Plataforma
CREATE TABLE admin.pagamento_plataforma (
    id BIGSERIAL PRIMARY KEY,
    cobranca_id BIGINT NOT NULL REFERENCES admin.cobranca_plataforma(id),

    valor DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    forma_pagamento VARCHAR(20) NOT NULL,

    -- Dados da transacao
    assas_payment_id VARCHAR(100),
    assas_transaction_id VARCHAR(100),
    comprovante_url VARCHAR(500),

    dt_pagamento TIMESTAMP,
    dt_criacao TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_pagamento_plat_cobranca ON admin.pagamento_plataforma(cobranca_id);
CREATE INDEX idx_pagamento_plat_assas ON admin.pagamento_plataforma(assas_payment_id);

-- Seed: criar assinatura ATIVA para organizacoes existentes
INSERT INTO admin.assinatura (organizacao_id, plano_bellory_id, status, ciclo_cobranca, dt_inicio, dt_criacao)
SELECT o.id, o.plano_id, 'ATIVA', 'MENSAL', now(), now()
FROM app.organizacao o
WHERE o.ativo = true
  AND NOT EXISTS (SELECT 1 FROM admin.assinatura a WHERE a.organizacao_id = o.id);
