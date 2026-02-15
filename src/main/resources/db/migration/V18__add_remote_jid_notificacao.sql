-- V18: Adiciona coluna remote_jid na tabela notificacao_enviada
-- Armazena o identificador WhatsApp do cliente (ex: 553298220082@s.whatsapp.net)

ALTER TABLE app.notificacao_enviada
ADD COLUMN IF NOT EXISTS remote_jid VARCHAR(100);

-- Índice para buscas por remote_jid + status (usado no fluxo de confirmação n8n)
CREATE INDEX IF NOT EXISTS idx_notificacao_remote_jid_status
    ON app.notificacao_enviada(remote_jid, status);
