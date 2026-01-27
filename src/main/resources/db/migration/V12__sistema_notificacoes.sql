-- =====================================================
-- SISTEMA DE NOTIFICACOES - BELLORY
-- =====================================================

-- Adiciona campo status na tabela instance
ALTER TABLE app.instance ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- Tabela de configuracao por organizacao
CREATE TABLE app.config_notificacao (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    horas_antes INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    mensagem_template TEXT,
    dt_criacao TIMESTAMP DEFAULT now(),
    dt_atualizacao TIMESTAMP,

    CONSTRAINT fk_config_notif_org FOREIGN KEY (organizacao_id)
        REFERENCES app.organizacao(id) ON DELETE CASCADE,
    CONSTRAINT uq_config_notif UNIQUE(organizacao_id, tipo, horas_antes),
    CONSTRAINT chk_tipo CHECK (tipo IN ('CONFIRMACAO', 'LEMBRETE')),
    CONSTRAINT chk_horas CHECK (horas_antes BETWEEN 1 AND 48)
);

-- Tabela de controle de envios
CREATE TABLE app.notificacao_enviada (
    id BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    horas_antes INTEGER NOT NULL,
    dt_envio TIMESTAMP NOT NULL DEFAULT now(),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    erro_mensagem TEXT,
    whatsapp_message_id VARCHAR(100),
    telefone_destino VARCHAR(20),
    dt_criacao TIMESTAMP DEFAULT now(),

    CONSTRAINT fk_notif_agendamento FOREIGN KEY (agendamento_id)
        REFERENCES app.agendamento(id) ON DELETE CASCADE,
    CONSTRAINT uq_notif_enviada UNIQUE(agendamento_id, tipo, horas_antes),
    CONSTRAINT chk_notif_tipo CHECK (tipo IN ('CONFIRMACAO', 'LEMBRETE')),
    CONSTRAINT chk_notif_status CHECK (status IN ('PENDENTE','ENVIADO','ENTREGUE','FALHA','CANCELADO'))
);

-- Indices
CREATE INDEX idx_config_notif_org ON app.config_notificacao(organizacao_id);
CREATE INDEX idx_config_notif_ativo ON app.config_notificacao(ativo) WHERE ativo = true;
CREATE INDEX idx_notif_env_lookup ON app.notificacao_enviada(agendamento_id, tipo, horas_antes);
CREATE INDEX idx_notif_env_status ON app.notificacao_enviada(status);
CREATE INDEX idx_agendamento_notif ON app.agendamento(dt_agendamento, status) WHERE status = 'AGENDADO';
CREATE INDEX idx_instance_status ON app.instance(status) WHERE status = 'CONNECTED';

-- Dados iniciais: config padrao para organizacoes existentes
INSERT INTO app.config_notificacao (organizacao_id, tipo, horas_antes, ativo)
SELECT id, 'CONFIRMACAO', 24, true FROM app.organizacao
ON CONFLICT DO NOTHING;

INSERT INTO app.config_notificacao (organizacao_id, tipo, horas_antes, ativo)
SELECT id, 'LEMBRETE', 2, true FROM app.organizacao
ON CONFLICT DO NOTHING;
