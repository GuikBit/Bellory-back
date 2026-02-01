-- Migração para adicionar campos de controle do fluxo de confirmação de agendamento
-- Usado pelo N8N para rastrear o estado do processo de confirmação/reagendamento

-- Alterar tamanho da coluna status para acomodar novos valores
ALTER TABLE app.notificacao_enviada
ALTER COLUMN status TYPE VARCHAR(30);

-- Adicionar campo para data desejada no reagendamento
ALTER TABLE app.notificacao_enviada
ADD COLUMN IF NOT EXISTS data_desejada_reagendamento DATE;

-- Adicionar campo para armazenar horários disponíveis (JSON)
ALTER TABLE app.notificacao_enviada
ADD COLUMN IF NOT EXISTS horarios_disponiveis TEXT;

-- Adicionar campo para data/hora da resposta do cliente
ALTER TABLE app.notificacao_enviada
ADD COLUMN IF NOT EXISTS dt_resposta TIMESTAMP;

-- Adicionar campo para armazenar a resposta do cliente (SIM, NAO, REAGENDAR)
ALTER TABLE app.notificacao_enviada
ADD COLUMN IF NOT EXISTS resposta_cliente VARCHAR(50);

-- Adicionar campo para nome da instância WhatsApp
ALTER TABLE app.notificacao_enviada
ADD COLUMN IF NOT EXISTS instance_name VARCHAR(100);

-- Criar índice para busca por telefone e status (usado pelo N8N)
CREATE INDEX IF NOT EXISTS idx_notificacao_telefone_status
ON app.notificacao_enviada(telefone_destino, status);

-- Criar índice para busca por instance_name e status
CREATE INDEX IF NOT EXISTS idx_notificacao_instance_status
ON app.notificacao_enviada(instance_name, status);

-- Comentários nas colunas
COMMENT ON COLUMN app.notificacao_enviada.data_desejada_reagendamento IS 'Data desejada pelo cliente para reagendamento';
COMMENT ON COLUMN app.notificacao_enviada.horarios_disponiveis IS 'JSON com horários disponíveis apresentados ao cliente';
COMMENT ON COLUMN app.notificacao_enviada.dt_resposta IS 'Data/hora em que o cliente respondeu';
COMMENT ON COLUMN app.notificacao_enviada.resposta_cliente IS 'Resposta do cliente: SIM, NAO ou REAGENDAR';
COMMENT ON COLUMN app.notificacao_enviada.instance_name IS 'Nome da instância WhatsApp usada';
