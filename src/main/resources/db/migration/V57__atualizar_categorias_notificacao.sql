-- Migrar categorias antigas para as novas
UPDATE app.notificacao_push SET categoria = 'SISTEMA' WHERE categoria = 'CHAT';
UPDATE app.notificacao_push SET categoria = 'SISTEMA' WHERE categoria = 'TELEFONIA';

-- Recategorizar notificações existentes baseado na origem
UPDATE app.notificacao_push SET categoria = 'AGENDAMENTO' WHERE origem = 'AGENDAMENTO' AND categoria = 'SISTEMA';
UPDATE app.notificacao_push SET categoria = 'CLIENTE' WHERE origem = 'CLIENTE' AND categoria = 'SISTEMA';
UPDATE app.notificacao_push SET categoria = 'PAGAMENTO' WHERE origem = 'FINANCEIRO' AND categoria = 'SISTEMA';
