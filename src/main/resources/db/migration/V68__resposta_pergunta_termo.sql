-- ============================================
-- V68: Termo de Consentimento e Assinatura em respostas
-- ============================================
-- Adiciona campos de prova legal por resposta_pergunta e o status duplo de
-- assinatura no agendamento_questionario.

ALTER TABLE app.resposta_pergunta
    ADD COLUMN IF NOT EXISTS aceitou_termo BOOLEAN,
    ADD COLUMN IF NOT EXISTS data_aceite TIMESTAMP,
    ADD COLUMN IF NOT EXISTS texto_termo_renderizado TEXT,
    ADD COLUMN IF NOT EXISTS arquivo_assinatura_cliente_id BIGINT,
    ADD COLUMN IF NOT EXISTS arquivo_assinatura_profissional_id BIGINT,
    ADD COLUMN IF NOT EXISTS hash_termo VARCHAR(64);

COMMENT ON COLUMN app.resposta_pergunta.aceitou_termo IS 'True quando o cliente marcou o checkbox "Li e concordo" da pergunta TERMO_CONSENTIMENTO';
COMMENT ON COLUMN app.resposta_pergunta.data_aceite IS 'Timestamp do servidor (NUNCA do cliente) no momento do POST do aceite';
COMMENT ON COLUMN app.resposta_pergunta.texto_termo_renderizado IS 'Snapshot imutavel do texto que o cliente viu, com placeholders ja substituidos';
COMMENT ON COLUMN app.resposta_pergunta.arquivo_assinatura_cliente_id IS 'FK logico para app.arquivo (is_sistema=true) com a assinatura do cliente';
COMMENT ON COLUMN app.resposta_pergunta.arquivo_assinatura_profissional_id IS 'FK logico para app.arquivo (is_sistema=true) com a assinatura do profissional, quando exigir_assinatura_profissional=true';
COMMENT ON COLUMN app.resposta_pergunta.hash_termo IS 'SHA-256 de texto_termo_renderizado calculado pelo servidor para deteccao de adulteracao';

-- Status duplo de assinatura no tracking do agendamento
ALTER TABLE app.agendamento_questionario
    ADD COLUMN IF NOT EXISTS status_assinatura VARCHAR(20) NOT NULL DEFAULT 'NAO_REQUERIDA',
    ADD COLUMN IF NOT EXISTS dt_assinatura TIMESTAMP;

COMMENT ON COLUMN app.agendamento_questionario.status_assinatura IS 'Estado da assinatura do questionario: NAO_REQUERIDA (sem pergunta ASSINATURA), PENDENTE (existe pergunta mas nao foi capturada) ou ASSINADA';
COMMENT ON COLUMN app.agendamento_questionario.dt_assinatura IS 'Timestamp em que a assinatura foi capturada (status_assinatura = ASSINADA)';
