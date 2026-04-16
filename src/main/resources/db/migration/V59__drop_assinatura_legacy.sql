-- ========================================
-- V59: Drop do schema legado de assinatura/Asaas/planos/cobranca
-- ========================================
-- Apos migracao completa para a Payment API externa, removemos as colunas
-- e tabelas que serviam ao ciclo interno Asaas + planos locais. A Assinatura
-- fica apenas como vinculo (customerId + subscriptionId) com a Payment API.

-- 1) Drop das tabelas dependentes de assinatura/plano
DROP TABLE IF EXISTS admin.cupom_utilizacao CASCADE;
DROP TABLE IF EXISTS admin.cobranca_plataforma CASCADE;
DROP TABLE IF EXISTS admin.pagamento_plataforma CASCADE;
DROP TABLE IF EXISTS admin.webhook_log CASCADE;
DROP TABLE IF EXISTS admin.cupom_desconto CASCADE;

-- 2) Drop das colunas legadas em admin.assinatura
ALTER TABLE admin.assinatura
    DROP CONSTRAINT IF EXISTS assinatura_plano_bellory_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_assinatura_plano_bellory,
    DROP CONSTRAINT IF EXISTS assinatura_plano_agendado_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_assinatura_plano_agendado,
    DROP CONSTRAINT IF EXISTS assinatura_cupom_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_assinatura_cupom;

ALTER TABLE admin.assinatura
    DROP COLUMN IF EXISTS plano_bellory_id,
    DROP COLUMN IF EXISTS status,
    DROP COLUMN IF EXISTS ciclo_cobranca,
    DROP COLUMN IF EXISTS dt_inicio_trial,
    DROP COLUMN IF EXISTS dt_fim_trial,
    DROP COLUMN IF EXISTS dt_trial_notificado,
    DROP COLUMN IF EXISTS dt_inicio,
    DROP COLUMN IF EXISTS dt_proximo_vencimento,
    DROP COLUMN IF EXISTS dt_cancelamento,
    DROP COLUMN IF EXISTS valor_mensal,
    DROP COLUMN IF EXISTS valor_anual,
    DROP COLUMN IF EXISTS forma_pagamento,
    DROP COLUMN IF EXISTS cupom_id,
    DROP COLUMN IF EXISTS valor_desconto,
    DROP COLUMN IF EXISTS cupom_codigo,
    DROP COLUMN IF EXISTS assas_customer_id,
    DROP COLUMN IF EXISTS assas_subscription_id,
    DROP COLUMN IF EXISTS credito_pro_rata,
    DROP COLUMN IF EXISTS cobranca_upgrade_assas_id,
    DROP COLUMN IF EXISTS plano_anterior_codigo,
    DROP COLUMN IF EXISTS plano_agendado_id,
    DROP COLUMN IF EXISTS ciclo_agendado,
    DROP COLUMN IF EXISTS dt_atualizacao;

-- 3) Drop dos indices legados da assinatura
DROP INDEX IF EXISTS admin.idx_assinatura_status;
DROP INDEX IF EXISTS admin.idx_assinatura_dt_fim_trial;
DROP INDEX IF EXISTS admin.idx_assinatura_dt_proximo_vencimento;

-- 4) Drop da FK plano_id + limites_personalizados_id na organizacao
ALTER TABLE app.organizacao
    DROP CONSTRAINT IF EXISTS organizacao_plano_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_organizacao_plano,
    DROP CONSTRAINT IF EXISTS organizacao_limites_personalizados_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_organizacao_limites_personalizados;

DROP INDEX IF EXISTS app.idx_organizacao_plano_id;

ALTER TABLE app.organizacao
    DROP COLUMN IF EXISTS plano_id,
    DROP COLUMN IF EXISTS limites_personalizados_id;

-- 5) Drop das tabelas de plano Bellory e limites
DROP TABLE IF EXISTS admin.plano_limites_bellory CASCADE;
DROP TABLE IF EXISTS admin.plano_bellory CASCADE;
DROP TABLE IF EXISTS admin.plano_limites CASCADE;
DROP TABLE IF EXISTS admin.plano CASCADE;
