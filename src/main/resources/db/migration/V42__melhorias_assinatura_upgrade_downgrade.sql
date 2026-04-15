-- ========================================
-- V41: Melhorias Assinatura - Upgrade/Downgrade
-- Novos campos para suporte a upgrade pro-rata, downgrade agendado,
-- remoção de campos obsoletos, e ajustes em cupom_desconto e plano_bellory.
-- ========================================

-- Ampliar coluna status para comportar novos valores de enum (ex: AGUARDANDO_PAGAMENTO)
ALTER TABLE admin.assinatura ALTER COLUMN status TYPE VARCHAR(30);

-- Adicionar novos campos para controle de upgrade/downgrade
ALTER TABLE admin.assinatura ADD COLUMN IF NOT EXISTS credito_pro_rata DECIMAL(10,2);
ALTER TABLE admin.assinatura ADD COLUMN IF NOT EXISTS cobranca_upgrade_assas_id VARCHAR(100);
ALTER TABLE admin.assinatura ADD COLUMN IF NOT EXISTS plano_anterior_codigo VARCHAR(50);

-- Remover campos de valor que agora vêm do PlanoBellory/Asaas
ALTER TABLE admin.assinatura DROP COLUMN IF EXISTS valor_mensal;
ALTER TABLE admin.assinatura DROP COLUMN IF EXISTS valor_anual;

-- Índice para rastrear cobranças de upgrade pendentes
CREATE INDEX IF NOT EXISTS idx_assinatura_cobranca_upgrade
    ON admin.assinatura(cobranca_upgrade_assas_id)
    WHERE cobranca_upgrade_assas_id IS NOT NULL;

-- Adicionar meses_recorrencia ao cupom_desconto
ALTER TABLE admin.cupom_desconto ADD COLUMN IF NOT EXISTS meses_recorrencia INTEGER;

-- Remover campo cartao_credito_id de plano_bellory (campo indevido)
ALTER TABLE admin.plano_bellory DROP COLUMN IF EXISTS cartao_credito_id;
