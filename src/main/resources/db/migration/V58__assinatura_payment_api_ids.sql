-- ========================================
-- V58: Integracao Payment API na assinatura
-- Schema: admin
-- ========================================
-- Adiciona identificadores da Payment API externa (customer e subscription).
-- Colunas legadas (assas_customer_id, assas_subscription_id, status, ciclo_cobranca,
-- datas de trial, cupom, pro-rata, troca agendada) serao dropadas em migration
-- futura apos remocao dos services Asaas legados (Fase 8 da migracao).

ALTER TABLE admin.assinatura
    ADD COLUMN IF NOT EXISTS payment_api_customer_id BIGINT,
    ADD COLUMN IF NOT EXISTS payment_api_subscription_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_assinatura_payment_api_customer_id
    ON admin.assinatura(payment_api_customer_id);

CREATE INDEX IF NOT EXISTS idx_assinatura_payment_api_subscription_id
    ON admin.assinatura(payment_api_subscription_id);
