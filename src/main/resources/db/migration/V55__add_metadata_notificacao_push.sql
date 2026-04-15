-- Adiciona coluna metadata (JSON como TEXT) para dados estruturados das notificações
ALTER TABLE app.notificacao_push ADD COLUMN metadata TEXT;
