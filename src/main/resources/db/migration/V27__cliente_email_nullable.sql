-- Permite email nulo para clientes rapidos (cadastro sem email)
ALTER TABLE app.cliente ALTER COLUMN email DROP NOT NULL;


