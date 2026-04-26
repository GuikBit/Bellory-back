-- Vínculo N:N entre Agendamento e Questionario.
-- Os questionários (anamneses) são derivados dos serviços do agendamento no momento
-- da criação: para cada Servico com anamnese_id preenchido, o questionário entra aqui.
-- Deduplicado por PK composta — se 2 serviços apontarem para o mesmo questionário,
-- o cliente responde apenas 1 vez.
CREATE TABLE IF NOT EXISTS app.agendamento_questionario (
    agendamento_id  BIGINT NOT NULL,
    questionario_id BIGINT NOT NULL,
    PRIMARY KEY (agendamento_id, questionario_id),
    CONSTRAINT fk_aq_agendamento
        FOREIGN KEY (agendamento_id)
        REFERENCES app.agendamento(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_aq_questionario
        FOREIGN KEY (questionario_id)
        REFERENCES app.questionario(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_aq_questionario_id
    ON app.agendamento_questionario(questionario_id);
