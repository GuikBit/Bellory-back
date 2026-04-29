-- ============================================
-- V66: Termo de Consentimento e Assinatura Digital em Perguntas
-- ============================================
-- Adiciona suporte a perguntas do tipo TERMO_CONSENTIMENTO e ASSINATURA.
-- Todas as colunas sao nullable para preservar compatibilidade com perguntas existentes.

ALTER TABLE app.pergunta
    ADD COLUMN IF NOT EXISTS texto_termo TEXT,
    ADD COLUMN IF NOT EXISTS template_termo_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS requer_aceite_explicito BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS formato_assinatura VARCHAR(20),
    ADD COLUMN IF NOT EXISTS largura_assinatura INTEGER,
    ADD COLUMN IF NOT EXISTS altura_assinatura INTEGER,
    ADD COLUMN IF NOT EXISTS exigir_assinatura_profissional BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN app.pergunta.texto_termo IS 'Conteudo do termo (Markdown com placeholders) quando tipo = TERMO_CONSENTIMENTO';
COMMENT ON COLUMN app.pergunta.template_termo_id IS 'Identifica o template de origem (auditoria): PADRAO_BELLORY, PADRAO_PROCEDIMENTO, PADRAO_PROCEDIMENTO_QUIMICO, PADRAO_USO_IMAGEM, CUSTOM';
COMMENT ON COLUMN app.pergunta.requer_aceite_explicito IS 'Se true, exige checkbox "Li e concordo" antes do submit';
COMMENT ON COLUMN app.pergunta.formato_assinatura IS 'Formato esperado da assinatura quando tipo = ASSINATURA: PNG_BASE64 ou SVG';
COMMENT ON COLUMN app.pergunta.largura_assinatura IS 'Largura do canvas de assinatura em pixels (range 200-1200, default 600)';
COMMENT ON COLUMN app.pergunta.altura_assinatura IS 'Altura do canvas de assinatura em pixels (range 100-600, default 200)';
COMMENT ON COLUMN app.pergunta.exigir_assinatura_profissional IS 'Se true, captura tambem a assinatura do funcionario responsavel';
