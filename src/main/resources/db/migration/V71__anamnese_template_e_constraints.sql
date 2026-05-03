-- =============================================================================
-- V71: Plugar mensagem de ANAMNESE no sistema de templates por tenant
--
-- Antes: a mensagem enviada pelo AnamneseWhatsAppService era uma string Java
-- hardcoded ("Tudo certo com seu agendamento na ..."), nao customizavel por
-- organizacao. Agora ela passa pelo mesmo fluxo de CONFIRMACAO/LEMBRETE:
--   1) tenta app.config_notificacao.mensagem_template (orgId, ANAMNESE, 0)
--   2) cai no admin.template_bellory (WHATSAPP, ANAMNESE, padrao=true)
--
-- Anamnese e event-driven (dispara apos commit do agendamento), nao tem janela
-- de horas. Usamos horas_antes=0 como sentinel para preservar a UNIQUE atual
-- (organizacao_id, tipo, horas_antes) sem mudar a estrutura da tabela.
-- =============================================================================

-- 1) Permitir ANAMNESE em config_notificacao (chk_tipo originalmente listava
--    apenas CONFIRMACAO e LEMBRETE - V12).
ALTER TABLE app.config_notificacao DROP CONSTRAINT IF EXISTS chk_tipo;
ALTER TABLE app.config_notificacao ADD CONSTRAINT chk_tipo
    CHECK (tipo IN ('CONFIRMACAO', 'LEMBRETE', 'ANAMNESE'));

-- 2) Permitir horas_antes=0 (sentinel para tipos event-driven como ANAMNESE).
--    O CHECK original era BETWEEN 1 AND 48.
ALTER TABLE app.config_notificacao DROP CONSTRAINT IF EXISTS chk_horas;
ALTER TABLE app.config_notificacao ADD CONSTRAINT chk_horas
    CHECK (horas_antes BETWEEN 0 AND 48);

-- 3) Seed do template padrao - identico a string que estava no Java
--    (AnamneseWhatsAppService.montarMensagem antes desta V71). Apos esta
--    migration, qualquer alteracao no texto pode ser feita por SQL ou pelo
--    proprio tenant via /api/v1/config/notificacao.
INSERT INTO admin.template_bellory (codigo, nome, descricao, tipo, categoria, conteudo, variaveis_disponiveis, padrao, icone)
VALUES (
    'whatsapp-anamnese',
    'Convite de Anamnese',
    'Mensagem de convite para o cliente responder o questionario (anamnese) vinculado ao agendamento recem-criado.',
    'WHATSAPP',
    'ANAMNESE',
    E'Olá, {{nome_cliente}}! 💖\n\nTudo certo com seu agendamento na *{{nome_empresa}}* para *{{data_agendamento}} às {{hora_agendamento}}* — estamos ansiosos para te receber!\n\nPara nossa equipe te atender com toda a segurança e carinho que você merece, preparamos algumas perguntas rápidas: *{{titulo_questionario}}*. Leva pouquinho tempo, prometo. 🙌\n\n👉 {{link_anamnese}}\n\nSuas respostas ficam salvas e nos ajudam a personalizar seu atendimento. Qualquer dúvida, e só responder por aqui!',
    '[{"nome":"nome_cliente","descricao":"Primeiro nome do cliente","exemplo":"Joao"},{"nome":"nome_empresa","descricao":"Nome fantasia da organizacao","exemplo":"Studio Bellory"},{"nome":"data_agendamento","descricao":"Data do agendamento","exemplo":"15/03"},{"nome":"hora_agendamento","descricao":"Horario do agendamento","exemplo":"14:30"},{"nome":"titulo_questionario","descricao":"Titulo do questionario/anamnese","exemplo":"Anamnese padrao"},{"nome":"link_anamnese","descricao":"URL publica para o cliente responder o questionario","exemplo":"https://app.bellory.com.br/avaliacao/..."}]',
    true,
    'ClipboardList'
);
