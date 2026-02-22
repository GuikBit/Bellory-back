-- V29: Tabela de sessão de agendamento para o agente de IA
-- Armazena o estado da sessão de agendamento do cliente via WhatsApp
-- com TTL de 2 horas e renovação automática a cada interação.

CREATE TABLE IF NOT EXISTS auto.booking_session (
    id BIGSERIAL PRIMARY KEY,
    remote_jid VARCHAR(100) NOT NULL,
    instance_name VARCHAR(100) NOT NULL,
    organizacao_id BIGINT NOT NULL,
    session_data JSONB NOT NULL DEFAULT '{
        "passo_atual": 1,
        "cliente_id": null,
        "cliente_nome": null,
        "cliente_telefone": null,
        "servico_id": null,
        "servico_nome": null,
        "servico_preco": null,
        "servico_duracao": null,
        "funcionario_id": null,
        "funcionario_nome": null,
        "data_selecionada": null,
        "hora_selecionada": null
    }'::jsonb,
    passo_atual INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP DEFAULT (NOW() + INTERVAL '2 hours'),
    CONSTRAINT uk_booking_session_jid_instance UNIQUE (remote_jid, instance_name)
);

-- Índices para consultas frequentes
CREATE INDEX IF NOT EXISTS idx_booking_session_jid ON auto.booking_session (remote_jid);
CREATE INDEX IF NOT EXISTS idx_booking_session_expires ON auto.booking_session (expires_at);
CREATE INDEX IF NOT EXISTS idx_booking_session_org ON auto.booking_session (organizacao_id);

-- Função para limpar sessões expiradas
CREATE OR REPLACE FUNCTION auto.limpar_booking_session_expirado()
RETURNS void
LANGUAGE plpgsql
AS $function$
BEGIN
    DELETE FROM auto.booking_session
    WHERE expires_at < NOW();
END;
$function$;
