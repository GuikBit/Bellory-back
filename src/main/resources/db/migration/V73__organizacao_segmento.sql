-- Adiciona segmento de negócio na organização (ex: BARBEARIA, SALAO, ESTETICA).
ALTER TABLE app.organizacao
    ADD COLUMN segmento VARCHAR(50);

CREATE INDEX idx_organizacao_segmento ON app.organizacao (segmento);
