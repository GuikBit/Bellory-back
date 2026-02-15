-- V25: Tabelas de horário de funcionamento da organização

CREATE TABLE IF NOT EXISTS app.horario_funcionamento (
    id              BIGSERIAL       PRIMARY KEY,
    organizacao_id  BIGINT          NOT NULL REFERENCES app.organizacao(id),
    dia_semana      VARCHAR(10)     NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT true,
    CONSTRAINT uk_horario_func_org_dia UNIQUE (organizacao_id, dia_semana)
);

CREATE TABLE IF NOT EXISTS app.periodo_funcionamento (
    id                          BIGSERIAL       PRIMARY KEY,
    horario_funcionamento_id    BIGINT          NOT NULL REFERENCES app.horario_funcionamento(id) ON DELETE CASCADE,
    hora_inicio                 TIME            NOT NULL,
    hora_fim                    TIME            NOT NULL
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_horario_func_org_id ON app.horario_funcionamento(organizacao_id);
CREATE INDEX IF NOT EXISTS idx_horario_func_org_dia ON app.horario_funcionamento(organizacao_id, dia_semana);
CREATE INDEX IF NOT EXISTS idx_periodo_func_horario_id ON app.periodo_funcionamento(horario_funcionamento_id);
