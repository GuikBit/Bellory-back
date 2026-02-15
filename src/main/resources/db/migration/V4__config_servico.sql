ALTER TABLE app.config_sistema
    ADD modo_vizualizacao VARCHAR(255);

ALTER TABLE app.config_sistema
    ADD mostrar_avaliacao BOOLEAN;

ALTER TABLE app.config_sistema
    ADD mostrar_valor_agendamento BOOLEAN;

ALTER TABLE app.config_sistema
    ADD unico_servico_agendamento BOOLEAN;