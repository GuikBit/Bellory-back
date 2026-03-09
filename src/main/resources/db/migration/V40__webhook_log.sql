-- Tabela de log de webhooks do Asaas
CREATE TABLE IF NOT EXISTS admin.webhook_log (
    id                      BIGSERIAL PRIMARY KEY,
    assinatura_id           BIGINT REFERENCES admin.assinatura(id),
    evento                  VARCHAR(50) NOT NULL,
    assas_payment_id        VARCHAR(100),
    assas_subscription_id   VARCHAR(100),
    valor                   DECIMAL(10,2),
    status_pagamento        VARCHAR(30),
    payload_resumo          TEXT,
    dt_recebimento          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_log_assinatura ON admin.webhook_log(assinatura_id);
CREATE INDEX IF NOT EXISTS idx_webhook_log_evento ON admin.webhook_log(evento);
CREATE INDEX IF NOT EXISTS idx_webhook_log_assas_payment ON admin.webhook_log(assas_payment_id);
