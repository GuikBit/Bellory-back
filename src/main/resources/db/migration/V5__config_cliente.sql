ALTER TABLE app.config_sistema
    ADD precisa_cadastro_agendar BOOLEAN;

ALTER TABLE app.config_sistema
    ADD programa_fidelidade BOOLEAN;

ALTER TABLE app.config_sistema
    ADD valor_gasto_um_ponto DECIMAL(10, 2);