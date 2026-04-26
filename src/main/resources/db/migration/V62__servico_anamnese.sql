-- Adiciona vínculo opcional entre Servico e um Questionario do tipo anamnese.
-- Permite disparar o questionário de anamnese ao cliente quando o agendamento for criado.
ALTER TABLE app.servico
    ADD COLUMN IF NOT EXISTS anamnese_id BIGINT NULL;

ALTER TABLE app.servico
    ADD CONSTRAINT fk_servico_anamnese
        FOREIGN KEY (anamnese_id)
        REFERENCES app.questionario(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_servico_anamnese_id ON app.servico(anamnese_id);
