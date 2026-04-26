-- Evolui app.agendamento_questionario de simples join table para tabela de tracking
-- por questionário/agendamento. Permite saber se a mensagem foi enviada ao cliente,
-- se ele já respondeu, e referenciar a resposta gerada.

ALTER TABLE app.agendamento_questionario
    DROP CONSTRAINT IF EXISTS agendamento_questionario_pkey;

ALTER TABLE app.agendamento_questionario
    ADD COLUMN id BIGSERIAL PRIMARY KEY;

ALTER TABLE app.agendamento_questionario
    ADD CONSTRAINT uk_aq_ag_q UNIQUE (agendamento_id, questionario_id);

ALTER TABLE app.agendamento_questionario
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE';

ALTER TABLE app.agendamento_questionario
    ADD COLUMN dt_envio TIMESTAMP;

ALTER TABLE app.agendamento_questionario
    ADD COLUMN dt_resposta TIMESTAMP;

ALTER TABLE app.agendamento_questionario
    ADD COLUMN resposta_questionario_id BIGINT;

ALTER TABLE app.agendamento_questionario
    ADD COLUMN dt_criacao TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE app.agendamento_questionario
    ADD CONSTRAINT fk_aq_resposta
        FOREIGN KEY (resposta_questionario_id)
        REFERENCES app.resposta_questionario(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_aq_status
    ON app.agendamento_questionario(status);

CREATE INDEX IF NOT EXISTS idx_aq_agendamento_id
    ON app.agendamento_questionario(agendamento_id);
