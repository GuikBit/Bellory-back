
ALTER TABLE app.servico
    ADD preco_final DECIMAL(10, 2);

UPDATE app.servico
SET preco_final = preco;

ALTER TABLE app.servico
    ALTER COLUMN preco_final SET NOT NULL;


ALTER TABLE app.servico
    DROP COLUMN desconto;

ALTER TABLE app.servico
    ADD desconto DECIMAL(4, 1);

UPDATE app.servico
SET desconto = 0;

ALTER TABLE app.servico
    ALTER COLUMN desconto SET NOT NULL;

