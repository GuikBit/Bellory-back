-- Troca de plano agendada: o cliente usa o plano atual ate o fim do ciclo,
-- e na virada do ciclo o plano muda localmente e o valor eh atualizado no Asaas.

ALTER TABLE admin.assinatura
    ADD COLUMN plano_agendado_id BIGINT,
    ADD COLUMN ciclo_agendado VARCHAR(10);

ALTER TABLE admin.assinatura
    ADD CONSTRAINT fk_assinatura_plano_agendado
    FOREIGN KEY (plano_agendado_id) REFERENCES admin.plano_bellory(id);

CREATE INDEX idx_assinatura_plano_agendado ON admin.assinatura(plano_agendado_id)
    WHERE plano_agendado_id IS NOT NULL;
