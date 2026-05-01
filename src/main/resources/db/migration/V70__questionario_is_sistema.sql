-- ============================================
-- V70: Registros sistemicos em questionario (anamnese padrao)
-- ============================================
-- is_sistema:    questionario criado e mantido pelo sistema; nao pode ser deletado
--                nem desativado pelo admin (apenas perguntas/descricao/cor/imagem
--                e configuracoes de termo/assinatura podem ser alteradas).
-- chave_sistema: identificador estavel do registro sistemico (ex.: ANAMNESE_PADRAO).
--                Permite o codigo localizar o registro sem depender de id ou titulo.
-- A unique parcial garante 1 registro por (organizacao, chave_sistema), permitindo
-- multiplos questionarios sistemicos no futuro (FEEDBACK_PADRAO, etc.) sem schema novo.

ALTER TABLE app.questionario
    ADD COLUMN IF NOT EXISTS is_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS chave_sistema VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS uk_questionario_org_chave_sistema
    ON app.questionario(organizacao_id, chave_sistema)
    WHERE chave_sistema IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_questionario_is_sistema
    ON app.questionario(is_sistema);

COMMENT ON COLUMN app.questionario.is_sistema IS 'Quando true, registro mantido pelo sistema: bloqueia delete e mudanca de ativo/titulo/tipo/chave_sistema. Apenas conteudo (perguntas, descricao, etc.) pode ser editado.';
COMMENT ON COLUMN app.questionario.chave_sistema IS 'Chave logica estavel do registro sistemico (ex.: ANAMNESE_PADRAO). Unique parcial por organizacao.';

-- ============================================
-- Backfill: cria a anamnese padrao para cada organizacao existente que ainda nao tem.
-- Idempotente via WHERE NOT EXISTS — re-executar a migration nao duplica.
-- Para organizacoes novas (criadas apos esta migration), a materializacao acontece
-- em QuestionarioSistemaService.materializarPadroes chamado por OrganizacaoService.create.
-- ============================================
DO $$
DECLARE
    org_record RECORD;
    novo_questionario_id BIGINT;
BEGIN
    FOR org_record IN SELECT id FROM app.organizacao LOOP
        IF NOT EXISTS (
            SELECT 1 FROM app.questionario
            WHERE organizacao_id = org_record.id
              AND chave_sistema = 'ANAMNESE_PADRAO'
        ) THEN
            INSERT INTO app.questionario (
                organizacao_id, titulo, descricao, tipo,
                ativo, obrigatorio, anonimo,
                is_deletado, is_sistema, chave_sistema, dt_criacao
            ) VALUES (
                org_record.id,
                'Anamnese Padrão',
                'Questionário de anamnese padrão para procedimentos estéticos. Preencha com atenção antes do atendimento — essas informações ajudam o profissional a garantir sua segurança.',
                'CLIENTE',
                TRUE, TRUE, FALSE,
                FALSE, TRUE, 'ANAMNESE_PADRAO',
                NOW()
            )
            RETURNING id INTO novo_questionario_id;

            INSERT INTO app.pergunta (questionario_id, texto, tipo, obrigatoria, ordem) VALUES
                (novo_questionario_id,
                 'Possui alguma alergia conhecida (medicamentos, cosméticos, esmalte, henna, látex, etc.)? Se sim, descreva.',
                 'TEXTO_LONGO', TRUE, 1),
                (novo_questionario_id,
                 'Faz uso de medicamentos contínuos? Se sim, quais?',
                 'TEXTO_LONGO', TRUE, 2),
                (novo_questionario_id,
                 'Possui alguma condição de saúde relevante (diabetes, hipertensão, problemas cardíacos, doenças autoimunes, distúrbios de coagulação)?',
                 'TEXTO_LONGO', TRUE, 3),
                (novo_questionario_id,
                 'Está gestante ou amamentando?',
                 'SIM_NAO', TRUE, 4),
                (novo_questionario_id,
                 'Possui sensibilidade ou irritação na pele (eczema, dermatite, psoríase, rosácea)?',
                 'TEXTO_LONGO', FALSE, 5),
                (novo_questionario_id,
                 'Já passou por algum procedimento estético recente? Qual e há quanto tempo?',
                 'TEXTO_LONGO', FALSE, 6),
                (novo_questionario_id,
                 'Tem cicatrizes, manchas, ferimentos ativos ou tatuagens na área a ser tratada?',
                 'TEXTO_LONGO', FALSE, 7),
                (novo_questionario_id,
                 'Observações adicionais que julgue importante informar ao profissional.',
                 'TEXTO_LONGO', FALSE, 8);
        END IF;
    END LOOP;
END $$;
