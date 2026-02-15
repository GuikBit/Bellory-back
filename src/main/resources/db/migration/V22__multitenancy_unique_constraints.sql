-- =============================================
-- V22: Ajuste de constraints para multitenancy
-- Altera unique constraints globais para compostas (organizacao_id + campo)
-- Permite que organizacoes diferentes tenham os mesmos usernames/emails/etc
-- =============================================

-- ADMIN: Remover constraints globais e criar compostas
ALTER TABLE app.admin DROP CONSTRAINT IF EXISTS uc_admin_username;
ALTER TABLE app.admin DROP CONSTRAINT IF EXISTS uc_admin_email;
ALTER TABLE app.admin ADD CONSTRAINT uk_admin_org_username UNIQUE (organizacao_id, username);
ALTER TABLE app.admin ADD CONSTRAINT uk_admin_org_email UNIQUE (organizacao_id, email);

-- FUNCIONARIO: Remover constraints globais e criar compostas
ALTER TABLE app.funcionario DROP CONSTRAINT IF EXISTS uc_funcionario_username;
ALTER TABLE app.funcionario DROP CONSTRAINT IF EXISTS uc_funcionario_email;
ALTER TABLE app.funcionario ADD CONSTRAINT uk_funcionario_org_username UNIQUE (organizacao_id, username);
ALTER TABLE app.funcionario ADD CONSTRAINT uk_funcionario_org_email UNIQUE (organizacao_id, email);

-- CLIENTE: Remover constraints globais e criar compostas
ALTER TABLE app.cliente DROP CONSTRAINT IF EXISTS uc_cliente_username;
ALTER TABLE app.cliente DROP CONSTRAINT IF EXISTS uc_cliente_email;
ALTER TABLE app.cliente DROP CONSTRAINT IF EXISTS uc_cliente_telefone;
ALTER TABLE app.cliente ADD CONSTRAINT uk_cliente_org_username UNIQUE (organizacao_id, username);
ALTER TABLE app.cliente ADD CONSTRAINT uk_cliente_org_email UNIQUE (organizacao_id, email);
ALTER TABLE app.cliente ADD CONSTRAINT uk_cliente_org_telefone UNIQUE (organizacao_id, telefone);
