ALTER TABLE app.cliente
    ADD CONSTRAINT uc_cliente_telefone UNIQUE (telefone);
