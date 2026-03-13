-- V41: Remover dados sensiveis de cartao de credito (PCI-DSS compliance)
-- Dados do cartao agora sao enviados diretamente ao Asaas e nunca armazenados localmente

-- 1. Adicionar novas colunas seguras na tabela cartao_credito
ALTER TABLE app.cartao_credito ADD COLUMN IF NOT EXISTS ultimos_quatro_digitos VARCHAR(4);
ALTER TABLE app.cartao_credito ADD COLUMN IF NOT EXISTS assas_credit_card_token VARCHAR(255);
ALTER TABLE app.cartao_credito ADD COLUMN IF NOT EXISTS dt_criacao TIMESTAMP DEFAULT now();
ALTER TABLE app.cartao_credito ADD COLUMN IF NOT EXISTS dt_atualizacao TIMESTAMP;

-- 2. Migrar ultimos 4 digitos do numero existente (se houver dados)
UPDATE app.cartao_credito
SET ultimos_quatro_digitos = RIGHT(REPLACE(numero_cartao, '-', ''), 4)
WHERE numero_cartao IS NOT NULL AND ultimos_quatro_digitos IS NULL;

-- 3. Remover colunas com dados sensiveis
ALTER TABLE app.cartao_credito DROP COLUMN IF EXISTS numero_cartao;
ALTER TABLE app.cartao_credito DROP COLUMN IF EXISTS cvv;
ALTER TABLE app.cartao_credito DROP COLUMN IF EXISTS data_vencimento;

-- 4. Remover dados sensiveis de dados_faturamento_organizacao
ALTER TABLE app.dados_faturamento_organizacao DROP COLUMN IF EXISTS numero_cartao;
ALTER TABLE app.dados_faturamento_organizacao DROP COLUMN IF EXISTS dt_validade_cartao;
