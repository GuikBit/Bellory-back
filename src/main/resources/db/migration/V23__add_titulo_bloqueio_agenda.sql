-- V23: Adiciona campo titulo na tabela bloqueio_agenda
ALTER TABLE app.bloqueio_agenda ADD COLUMN IF NOT EXISTS titulo VARCHAR(100);
