-- ============================================================
-- SQL Setup para os fluxos N8N - Bellory
-- Execute este script no banco PostgreSQL antes de ativar os fluxos
-- ============================================================

-- Schema auto (usado pelo n8n para controle interno)
CREATE SCHEMA IF NOT EXISTS auto;

-- ============================================================
-- 1. Cache de instâncias (evita chamadas repetidas à API)
-- ============================================================
CREATE TABLE IF NOT EXISTS auto.instance_cache (
    id BIGSERIAL PRIMARY KEY,
    instance_name VARCHAR(100) NOT NULL UNIQUE,
    instance_data JSONB NOT NULL,
    cached_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL DEFAULT (NOW() + INTERVAL '30 minutes')
);

CREATE INDEX IF NOT EXISTS idx_instance_cache_name
    ON auto.instance_cache(instance_name);
CREATE INDEX IF NOT EXISTS idx_instance_cache_expires
    ON auto.instance_cache(expires_at);

-- ============================================================
-- 2. Buffer de mensagens (debounce - agrupa mensagens rápidas)
-- ============================================================
CREATE TABLE IF NOT EXISTS auto.message_buffer (
    id BIGSERIAL PRIMARY KEY,
    remote_jid VARCHAR(100) NOT NULL,
    instance_name VARCHAR(100),
    message_text TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_message_buffer_jid
    ON auto.message_buffer(remote_jid, processed);
CREATE INDEX IF NOT EXISTS idx_message_buffer_created
    ON auto.message_buffer(created_at);

-- ============================================================
-- 3. Memória de chat (usado pelo n8n Postgres Memory node)
-- ============================================================
CREATE TABLE IF NOT EXISTS auto.chat_memory (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_memory_session
    ON auto.chat_memory(session_id);

-- ============================================================
-- 4. Limpeza automática de dados antigos
-- ============================================================

-- Limpar buffer de mensagens processadas (mais de 24h)
CREATE OR REPLACE FUNCTION auto.limpar_buffer_antigo()
RETURNS void AS $$
BEGIN
    DELETE FROM auto.message_buffer
    WHERE processed = true
    AND created_at < NOW() - INTERVAL '24 hours';
END;
$$ LANGUAGE plpgsql;

-- Limpar cache expirado
CREATE OR REPLACE FUNCTION auto.limpar_cache_expirado()
RETURNS void AS $$
BEGIN
    DELETE FROM auto.instance_cache
    WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- Limpar memória de chat antiga (mais de 7 dias)
CREATE OR REPLACE FUNCTION auto.limpar_chat_memory_antigo()
RETURNS void AS $$
BEGIN
    DELETE FROM auto.chat_memory
    WHERE created_at < NOW() - INTERVAL '7 days';
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 5. Verificação de integridade
-- ============================================================

-- Verificar se as tabelas do app schema existem
DO $$
BEGIN
    -- Verificar tabelas essenciais
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'app' AND table_name = 'notificacao_enviada') THEN
        RAISE NOTICE 'AVISO: Tabela app.notificacao_enviada não encontrada. Execute as migrations Flyway primeiro.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'app' AND table_name = 'instance') THEN
        RAISE NOTICE 'AVISO: Tabela app.instance não encontrada. Execute as migrations Flyway primeiro.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'app' AND table_name = 'config_notificacao') THEN
        RAISE NOTICE 'AVISO: Tabela app.config_notificacao não encontrada. Execute as migrations Flyway primeiro.';
    END IF;

    RAISE NOTICE 'Setup do schema auto concluído com sucesso!';
END;
$$;
