ALTER TABLE app.config_sistema
    ADD comissao_padrao BOOLEAN;

ALTER TABLE app.config_sistema
    ADD enviar_confitmacao_fora_horario BOOLEAN;

ALTER TABLE app.config_sistema
    ADD enviar_lembrete_email BOOLEAN;

ALTER TABLE app.config_sistema
    ADD enviar_lembrete_sms BOOLEAN;

ALTER TABLE app.config_sistema
    ADD enviar_lembrete_whatsapp BOOLEAN;

ALTER TABLE app.config_sistema
    ADD mostrar_notas_colaborador BOOLEAN;

ALTER TABLE app.config_sistema
    ADD selecionar_colaborador_agendamento BOOLEAN;

ALTER TABLE app.config_sistema
    ADD tempo_confirmacao INTEGER;

ALTER TABLE app.config_sistema
    ADD tempo_lembrete_pos_confirmacao INTEGER;


