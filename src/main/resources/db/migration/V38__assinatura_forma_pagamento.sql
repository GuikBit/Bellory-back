-- Adiciona forma de pagamento preferida na assinatura para renovacao automatica
ALTER TABLE admin.assinatura
    ADD COLUMN forma_pagamento VARCHAR(20);

-- Data em que o email de aviso de expiracao do trial foi enviado (evita duplicados)
ALTER TABLE admin.assinatura
    ADD COLUMN dt_trial_notificado TIMESTAMP;

-- Index para buscar trials que estao prestes a expirar (para envio de email)
CREATE INDEX idx_assinatura_trial_fim ON admin.assinatura (dt_fim_trial)
    WHERE status = 'TRIAL';
