-- Adicionar soft delete no funcionario
ALTER TABLE app.funcionario ADD COLUMN IF NOT EXISTS is_deletado BOOLEAN DEFAULT FALSE;
ALTER TABLE app.funcionario ADD COLUMN IF NOT EXISTS dt_deletado TIMESTAMP;

-- Index para filtrar deletados nas listagens
CREATE INDEX IF NOT EXISTS idx_funcionario_org_deletado ON app.funcionario(organizacao_id, is_deletado);
