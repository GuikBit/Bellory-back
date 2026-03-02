-- Campos de promocao mensal nos planos Bellory
ALTER TABLE admin.plano_bellory
    ADD COLUMN promo_mensal_ativa BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN promo_mensal_preco DECIMAL(10,2),
    ADD COLUMN promo_mensal_texto VARCHAR(100);
