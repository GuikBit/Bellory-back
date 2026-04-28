-- =====================================================================
-- V65 - Fila de Espera
-- Cliente que opta por entrar na fila ao agendar pode ser notificado
-- quando surgir um horario antes do seu agendamento (mesmo funcionario,
-- duracao compativel). Match e UPDATE no Agendamento existente.
--
-- Idempotente: usa IF NOT EXISTS para tolerar execucao parcial anterior.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Flags de fila no Agendamento
-- ---------------------------------------------------------------------
ALTER TABLE app.agendamento
    ADD COLUMN IF NOT EXISTS entrou_fila_espera BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE app.agendamento
    ADD COLUMN IF NOT EXISTS reagendado_por_fila BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE app.agendamento
    ADD COLUMN IF NOT EXISTS dt_original_fila TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_agendamento_fila_espera
    ON app.agendamento (organizacao_id, entrou_fila_espera, status, dt_agendamento)
    WHERE entrou_fila_espera = TRUE;

-- ---------------------------------------------------------------------
-- 2. Tabela de tentativas (rastreio de ofertas enviadas pela fila)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app.fila_espera_tentativa (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL,
    agendamento_id BIGINT NOT NULL,
    agendamento_cancelado_id BIGINT NOT NULL,
    funcionario_id BIGINT NOT NULL,
    notificacao_enviada_id BIGINT NULL,
    cascata_origem_id BIGINT NULL,
    cascata_nivel INTEGER NOT NULL DEFAULT 1,
    slot_inicio TIMESTAMP NOT NULL,
    slot_fim TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    dt_envio TIMESTAMP NULL,
    dt_resposta TIMESTAMP NULL,
    dt_expira TIMESTAMP NOT NULL,
    dt_criacao TIMESTAMP NOT NULL DEFAULT now(),
    dt_atualizacao TIMESTAMP NULL,
    CONSTRAINT fk_fet_organizacao
        FOREIGN KEY (organizacao_id) REFERENCES app.organizacao (id),
    CONSTRAINT fk_fet_agendamento
        FOREIGN KEY (agendamento_id) REFERENCES app.agendamento (id),
    CONSTRAINT fk_fet_agendamento_cancelado
        FOREIGN KEY (agendamento_cancelado_id) REFERENCES app.agendamento (id),
    CONSTRAINT fk_fet_notificacao_enviada
        FOREIGN KEY (notificacao_enviada_id) REFERENCES app.notificacao_enviada (id) ON DELETE SET NULL,
    CONSTRAINT fk_fet_cascata_origem
        FOREIGN KEY (cascata_origem_id) REFERENCES app.fila_espera_tentativa (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_fet_status_expira
    ON app.fila_espera_tentativa (status, dt_expira);

CREATE INDEX IF NOT EXISTS idx_fet_org_agend
    ON app.fila_espera_tentativa (organizacao_id, agendamento_id);

CREATE INDEX IF NOT EXISTS idx_fet_slot
    ON app.fila_espera_tentativa (funcionario_id, slot_inicio, slot_fim, status);

CREATE INDEX IF NOT EXISTS idx_fet_agendamento_cancelado
    ON app.fila_espera_tentativa (agendamento_cancelado_id, status);

-- ---------------------------------------------------------------------
-- 3. Configuracao da feature em config_sistema (embutida em ConfigAgendamento)
-- ---------------------------------------------------------------------
ALTER TABLE app.config_sistema
    ADD COLUMN IF NOT EXISTS usar_fila_espera BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE app.config_sistema
    ADD COLUMN IF NOT EXISTS fila_max_cascata INTEGER NOT NULL DEFAULT 5;

ALTER TABLE app.config_sistema
    ADD COLUMN IF NOT EXISTS fila_timeout_minutos INTEGER NOT NULL DEFAULT 30;

ALTER TABLE app.config_sistema
    ADD COLUMN IF NOT EXISTS fila_antecedencia_horas INTEGER NOT NULL DEFAULT 3;

-- ---------------------------------------------------------------------
-- 4. Liberar novos valores em notificacao_enviada.tipo
--    (chk_notif_tipo de V12 restringia a 'CONFIRMACAO','LEMBRETE')
-- ---------------------------------------------------------------------
ALTER TABLE app.notificacao_enviada
    DROP CONSTRAINT IF EXISTS chk_notif_tipo;
