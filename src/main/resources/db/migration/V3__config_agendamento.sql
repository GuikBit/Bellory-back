ALTER TABLE app.config_sistema
    ADD agend_tolerancia INTEGER;

ALTER TABLE app.config_sistema
    ADD aprovar_agendamento BOOLEAN;

ALTER TABLE app.config_sistema
    ADD aprovar_agendamento_agente BOOLEAN;

ALTER TABLE app.config_sistema
    ADD cancelamento_cliente BOOLEAN;

ALTER TABLE app.config_sistema
    ADD cobrar_sinal BOOLEAN;

ALTER TABLE app.config_sistema
    ADD cobrar_sinal_agente BOOLEAN;

ALTER TABLE app.config_sistema
    ADD max_dias_agendamento INTEGER;

ALTER TABLE app.config_sistema
    ADD min_dias_agendamento INTEGER;

ALTER TABLE app.config_sistema
    ADD ocultar_domingo BOOLEAN;

ALTER TABLE app.config_sistema
    ADD ocultar_fimsemana BOOLEAN;

ALTER TABLE app.config_sistema
    ADD porcent_sinal INTEGER;

ALTER TABLE app.config_sistema
    ADD porcent_sinal_agente INTEGER;

ALTER TABLE app.config_sistema
    ADD tempo_cancelamento_cliente INTEGER;