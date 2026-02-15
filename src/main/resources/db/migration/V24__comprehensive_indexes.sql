-- =====================================================================
-- V24 - Criação abrangente de índices para performance
-- =====================================================================
-- Esta migration adiciona índices em todas as tabelas core que estavam
-- sem cobertura adequada. Índices organizados por prioridade e módulo.
-- Utiliza CREATE INDEX IF NOT EXISTS e CREATE INDEX CONCURRENTLY não é
-- possível dentro de transação Flyway, então usamos criação normal.
-- =====================================================================

-- ===========================================
-- 1. AGENDAMENTO (Tabela mais crítica)
-- ===========================================

-- FK: organizacao_id (filtro principal de multitenancy)
CREATE INDEX IF NOT EXISTS idx_agendamento_organizacao_id
    ON app.agendamento(organizacao_id);

-- FK: cliente_id
CREATE INDEX IF NOT EXISTS idx_agendamento_cliente_id
    ON app.agendamento(cliente_id);

-- Status (filtro muito frequente)
CREATE INDEX IF NOT EXISTS idx_agendamento_status
    ON app.agendamento(status);

-- Data do agendamento (range queries)
CREATE INDEX IF NOT EXISTS idx_agendamento_dt_agendamento
    ON app.agendamento(dt_agendamento);

-- Compostos para queries mais comuns (org + data, org + status)
CREATE INDEX IF NOT EXISTS idx_agendamento_org_dt
    ON app.agendamento(organizacao_id, dt_agendamento);

CREATE INDEX IF NOT EXISTS idx_agendamento_org_status
    ON app.agendamento(organizacao_id, status);

-- Composto triplo: org + data + status (dashboards e relatórios)
CREATE INDEX IF NOT EXISTS idx_agendamento_org_dt_status
    ON app.agendamento(organizacao_id, dt_agendamento, status);

-- Data de criação (relatórios temporais)
CREATE INDEX IF NOT EXISTS idx_agendamento_dt_criacao
    ON app.agendamento(dt_criacao);

-- Confirmação (parcial - só onde tem confirmação)
CREATE INDEX IF NOT EXISTS idx_agendamento_dt_confirmacao
    ON app.agendamento(dt_confirmacao)
    WHERE dt_confirmacao IS NOT NULL;

-- ===========================================
-- 2. CLIENTE
-- ===========================================

-- FK: organizacao_id (multitenancy)
CREATE INDEX IF NOT EXISTS idx_cliente_organizacao_id
    ON app.cliente(organizacao_id);

-- Username global (autenticação - login sem contexto org)
CREATE INDEX IF NOT EXISTS idx_cliente_username
    ON app.cliente(username);

-- Email global (busca)
CREATE INDEX IF NOT EXISTS idx_cliente_email
    ON app.cliente(email);

-- Telefone (busca e notificações)
CREATE INDEX IF NOT EXISTS idx_cliente_telefone
    ON app.cliente(telefone)
    WHERE telefone IS NOT NULL;

-- CPF (busca)
CREATE INDEX IF NOT EXISTS idx_cliente_cpf
    ON app.cliente(cpf)
    WHERE cpf IS NOT NULL;

-- Org + ativo (listagens de clientes ativos)
CREATE INDEX IF NOT EXISTS idx_cliente_org_ativo
    ON app.cliente(organizacao_id, ativo);

-- Data de criação (relatórios de novos clientes)
CREATE INDEX IF NOT EXISTS idx_cliente_dt_criacao
    ON app.cliente(dt_criacao);

-- Aniversário: mês e dia (queries com EXTRACT)
CREATE INDEX IF NOT EXISTS idx_cliente_nascimento_mes
    ON app.cliente(EXTRACT(MONTH FROM data_nascimento))
    WHERE data_nascimento IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_cliente_nascimento_dia
    ON app.cliente(EXTRACT(DAY FROM data_nascimento))
    WHERE data_nascimento IS NOT NULL;

-- ===========================================
-- 3. FUNCIONARIO
-- ===========================================

-- FK: organizacao_id (multitenancy)
CREATE INDEX IF NOT EXISTS idx_funcionario_organizacao_id
    ON app.funcionario(organizacao_id);

-- Username global (autenticação)
CREATE INDEX IF NOT EXISTS idx_funcionario_username
    ON app.funcionario(username);

-- Email global
CREATE INDEX IF NOT EXISTS idx_funcionario_email
    ON app.funcionario(email);

-- CPF (busca)
CREATE INDEX IF NOT EXISTS idx_funcionario_cpf
    ON app.funcionario(cpf)
    WHERE cpf IS NOT NULL;

-- FK: cargo_id
CREATE INDEX IF NOT EXISTS idx_funcionario_cargo_id
    ON app.funcionario(cargo_id);

-- Org + ativo (listagens)
CREATE INDEX IF NOT EXISTS idx_funcionario_org_ativo
    ON app.funcionario(organizacao_id, ativo);

-- Visível externo (site público)
CREATE INDEX IF NOT EXISTS idx_funcionario_org_visivel
    ON app.funcionario(organizacao_id, ativo, is_visivel_externo)
    WHERE ativo = true AND is_visivel_externo = true;

-- ===========================================
-- 4. ADMIN
-- ===========================================

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_admin_organizacao_id
    ON app.admin(organizacao_id);

-- Username global (autenticação)
CREATE INDEX IF NOT EXISTS idx_admin_username
    ON app.admin(username);

-- Email global
CREATE INDEX IF NOT EXISTS idx_admin_email
    ON app.admin(email);

-- ===========================================
-- 5. ORGANIZACAO
-- ===========================================

-- Slug já tem UNIQUE, mas index explícito com ativo para site público
CREATE INDEX IF NOT EXISTS idx_organizacao_slug_ativo
    ON app.organizacao(slug, ativo);

-- FK: plano_id
CREATE INDEX IF NOT EXISTS idx_organizacao_plano_id
    ON app.organizacao(plano_id);

-- Ativo (filtro comum)
CREATE INDEX IF NOT EXISTS idx_organizacao_ativo
    ON app.organizacao(ativo)
    WHERE ativo = true;

-- Data de cadastro (relatórios)
CREATE INDEX IF NOT EXISTS idx_organizacao_dt_cadastro
    ON app.organizacao(dt_cadastro);

-- ===========================================
-- 6. COBRANCA
-- ===========================================

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_cobranca_organizacao_id
    ON app.cobranca(organizacao_id);

-- FK: cliente_id
CREATE INDEX IF NOT EXISTS idx_cobranca_cliente_id
    ON app.cobranca(cliente_id);

-- FK: agendamento_id
CREATE INDEX IF NOT EXISTS idx_cobranca_agendamento_id
    ON app.cobranca(agendamento_id);

-- FK: compra_id (já tem UNIQUE, mas explícito para clareza)
-- SKIP: uc_cobranca_compra já cobre

-- FK: cobranca_relacionada_id (já tem UNIQUE)
-- SKIP: uc_cobranca_cobranca_relacionada já cobre

-- Status (filtro muito frequente)
CREATE INDEX IF NOT EXISTS idx_cobranca_status
    ON app.cobranca(status_cobranca);

-- Tipo de cobrança
CREATE INDEX IF NOT EXISTS idx_cobranca_tipo
    ON app.cobranca(tipo_cobranca);

-- Subtipo (SINAL, RESTANTE, INTEGRAL)
CREATE INDEX IF NOT EXISTS idx_cobranca_subtipo
    ON app.cobranca(subtipo_cobranca_agendamento)
    WHERE subtipo_cobranca_agendamento IS NOT NULL;

-- Compostos org + status / org + tipo
CREATE INDEX IF NOT EXISTS idx_cobranca_org_status
    ON app.cobranca(organizacao_id, status_cobranca);

CREATE INDEX IF NOT EXISTS idx_cobranca_org_tipo
    ON app.cobranca(organizacao_id, tipo_cobranca);

-- Vencimento (cobranças vencidas)
CREATE INDEX IF NOT EXISTS idx_cobranca_dt_vencimento
    ON app.cobranca(dt_vencimento);

-- Cobranças vencidas pendentes (parcial - query muito usada)
CREATE INDEX IF NOT EXISTS idx_cobranca_vencidas
    ON app.cobranca(organizacao_id, dt_vencimento)
    WHERE status_cobranca IN ('PENDENTE', 'PARCIALMENTE_PAGO');

-- Data de criação (relatórios por período)
CREATE INDEX IF NOT EXISTS idx_cobranca_dt_criacao
    ON app.cobranca(dt_criacao);

CREATE INDEX IF NOT EXISTS idx_cobranca_org_dt_criacao
    ON app.cobranca(organizacao_id, dt_criacao);

-- Gateway (integração de pagamento)
CREATE INDEX IF NOT EXISTS idx_cobranca_gateway_intent
    ON app.cobranca(gateway_payment_intent_id)
    WHERE gateway_payment_intent_id IS NOT NULL;

-- ===========================================
-- 7. PAGAMENTO
-- ===========================================

-- FK: cobranca_id
CREATE INDEX IF NOT EXISTS idx_pagamento_cobranca_id
    ON app.pagamento(cobranca_id);

-- FK: cliente_id
CREATE INDEX IF NOT EXISTS idx_pagamento_cliente_id
    ON app.pagamento(cliente_id);

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_pagamento_organizacao_id
    ON app.pagamento(organizacao_id);

-- FK: cartao_credito_id
CREATE INDEX IF NOT EXISTS idx_pagamento_cartao_credito_id
    ON app.pagamento(cartao_credito_id)
    WHERE cartao_credito_id IS NOT NULL;

-- Status do pagamento
CREATE INDEX IF NOT EXISTS idx_pagamento_status
    ON app.pagamento(status_pagamento);

-- Composto: cobranca + status (pagamentos confirmados por cobrança)
CREATE INDEX IF NOT EXISTS idx_pagamento_cobranca_status
    ON app.pagamento(cobranca_id, status_pagamento);

-- Forma de pagamento (analytics)
CREATE INDEX IF NOT EXISTS idx_pagamento_forma
    ON app.pagamento(forma_pagamento)
    WHERE forma_pagamento IS NOT NULL;

-- Método de pagamento
CREATE INDEX IF NOT EXISTS idx_pagamento_metodo
    ON app.pagamento(metodo_pagamento)
    WHERE metodo_pagamento IS NOT NULL;

-- Data de criação
CREATE INDEX IF NOT EXISTS idx_pagamento_dt_criacao
    ON app.pagamento(dt_criacao);

-- Data de pagamento
CREATE INDEX IF NOT EXISTS idx_pagamento_dt_pagamento
    ON app.pagamento(dt_pagamento)
    WHERE dt_pagamento IS NOT NULL;

-- ===========================================
-- 8. SERVICO
-- ===========================================

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_servico_organizacao_id
    ON app.servico(organizacao_id);

-- FK: categoria_id
CREATE INDEX IF NOT EXISTS idx_servico_categoria_id
    ON app.servico(categoria_id);

-- Composto: org + deletado + ativo (listagem padrão)
CREATE INDEX IF NOT EXISTS idx_servico_org_ativo
    ON app.servico(organizacao_id, is_deletado, ativo);

-- Home (serviços na página inicial)
CREATE INDEX IF NOT EXISTS idx_servico_org_home
    ON app.servico(organizacao_id, is_home)
    WHERE is_home = true AND ativo = true AND is_deletado = false;

-- Ativo (filtro simples)
CREATE INDEX IF NOT EXISTS idx_servico_ativo
    ON app.servico(ativo);

-- Deletado (soft delete)
CREATE INDEX IF NOT EXISTS idx_servico_deletado
    ON app.servico(is_deletado);

-- ===========================================
-- 9. PRODUTO
-- ===========================================

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_produto_organizacao_id
    ON app.produto(organizacao_id);

-- FK: categoria_id
CREATE INDEX IF NOT EXISTS idx_produto_categoria_id
    ON app.produto(categoria_id);

-- Status do produto
CREATE INDEX IF NOT EXISTS idx_produto_status
    ON app.produto(status_produto);

-- Composto: org + ativo (listagem padrão)
CREATE INDEX IF NOT EXISTS idx_produto_org_ativo
    ON app.produto(organizacao_id, ativo);

-- Destaque (produtos em destaque)
CREATE INDEX IF NOT EXISTS idx_produto_org_destaque
    ON app.produto(organizacao_id, destaque)
    WHERE destaque = true AND ativo = true;

-- Código de barras (busca)
CREATE INDEX IF NOT EXISTS idx_produto_codigo_barras
    ON app.produto(codigo_barras)
    WHERE codigo_barras IS NOT NULL;

-- Marca (filtro)
CREATE INDEX IF NOT EXISTS idx_produto_marca
    ON app.produto(marca)
    WHERE marca IS NOT NULL;

-- Estoque baixo (alerta de estoque)
CREATE INDEX IF NOT EXISTS idx_produto_estoque_baixo
    ON app.produto(organizacao_id, quantidade_estoque, estoque_minimo);

-- Data de criação (ordenação)
CREATE INDEX IF NOT EXISTS idx_produto_dt_criacao
    ON app.produto(dt_criacao);

-- Preço (range queries e ordenação)
CREATE INDEX IF NOT EXISTS idx_produto_preco
    ON app.produto(preco);

-- ===========================================
-- 10. CATEGORIA
-- ===========================================

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_categoria_organizacao_id
    ON app.categoria(organizacao_id);

-- Composto: org + tipo + ativo (query mais comum)
CREATE INDEX IF NOT EXISTS idx_categoria_org_tipo_ativo
    ON app.categoria(organizacao_id, tipo, ativo);

-- ===========================================
-- 11. CARGO_FUNCIONARIO
-- ===========================================

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_cargo_organizacao_id
    ON app.cargo_funcionario(organizacao_id);

-- Org + ativo (listagem de cargos ativos)
CREATE INDEX IF NOT EXISTS idx_cargo_org_ativo
    ON app.cargo_funcionario(organizacao_id, ativo);

-- ===========================================
-- 12. COMPRA
-- ===========================================

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_compra_organizacao_id
    ON app.compra(organizacao_id);

-- FK: cliente_id
CREATE INDEX IF NOT EXISTS idx_compra_cliente_id
    ON app.compra(cliente_id);

-- Status da compra
CREATE INDEX IF NOT EXISTS idx_compra_status
    ON app.compra(status_compra);

-- Composto: org + status
CREATE INDEX IF NOT EXISTS idx_compra_org_status
    ON app.compra(organizacao_id, status_compra);

-- Data de criação (relatórios)
CREATE INDEX IF NOT EXISTS idx_compra_dt_criacao
    ON app.compra(dt_criacao);

-- Tipo de compra
CREATE INDEX IF NOT EXISTS idx_compra_tipo
    ON app.compra(tipo_compra);

-- ===========================================
-- 13. COMPRA_PRODUTO
-- ===========================================

-- FK: compra_id
CREATE INDEX IF NOT EXISTS idx_compra_produto_compra_id
    ON app.compra_produto(compra_id);

-- FK: produto_id
CREATE INDEX IF NOT EXISTS idx_compra_produto_produto_id
    ON app.compra_produto(produto_id);

-- ===========================================
-- 14. CARTAO_CREDITO
-- ===========================================

-- FK: cliente_id
CREATE INDEX IF NOT EXISTS idx_cartao_cliente_id
    ON app.cartao_credito(cliente_id);

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_cartao_organizacao_id
    ON app.cartao_credito(organizacao_id);

-- Cartão principal ativo (busca mais comum)
CREATE INDEX IF NOT EXISTS idx_cartao_principal
    ON app.cartao_credito(cliente_id, is_principal)
    WHERE is_principal = true AND ativo = true;

-- ===========================================
-- 15. ENDERECO
-- ===========================================

-- FK: cliente_id
CREATE INDEX IF NOT EXISTS idx_endereco_cliente_id
    ON app.endereco(cliente_id);

-- Endereço principal ativo
CREATE INDEX IF NOT EXISTS idx_endereco_principal
    ON app.endereco(cliente_id, is_principal)
    WHERE is_principal = true AND ativo = true;

-- Tipo de endereço
CREATE INDEX IF NOT EXISTS idx_endereco_tipo
    ON app.endereco(tipo)
    WHERE tipo IS NOT NULL;

-- ===========================================
-- 16. BLOQUEIO_AGENDA
-- ===========================================

-- FK: funcionario_id
CREATE INDEX IF NOT EXISTS idx_bloqueio_agenda_funcionario_id
    ON app.bloqueio_agenda(funcionario_id);

-- Tipo de bloqueio
CREATE INDEX IF NOT EXISTS idx_bloqueio_agenda_tipo
    ON app.bloqueio_agenda(tipo_bloqueio);

-- Composto: funcionário + período (detecção de conflitos)
CREATE INDEX IF NOT EXISTS idx_bloqueio_agenda_func_periodo
    ON app.bloqueio_agenda(funcionario_id, inicio_bloqueio, fim_bloqueio);

-- ===========================================
-- 17. JORNADA_DIA
-- ===========================================

-- FK: funcionario_id
CREATE INDEX IF NOT EXISTS idx_jornada_dia_funcionario_id
    ON app.jornada_dia(funcionario_id);

-- Composto: funcionário + dia da semana (lookup de horário)
CREATE INDEX IF NOT EXISTS idx_jornada_dia_func_dia
    ON app.jornada_dia(funcionario_id, dia_semana);

-- Ativo (jornadas ativas)
CREATE INDEX IF NOT EXISTS idx_jornada_dia_ativo
    ON app.jornada_dia(funcionario_id, ativo)
    WHERE ativo = true;

-- ===========================================
-- 18. HORARIO_TRABALHO
-- ===========================================

-- FK: jornada_dia_id
CREATE INDEX IF NOT EXISTS idx_horario_trabalho_jornada_id
    ON app.horario_trabalho(jornada_dia_id);

-- ===========================================
-- 19. JOIN TABLES (ManyToMany)
-- ===========================================

-- agendamento_funcionario
CREATE INDEX IF NOT EXISTS idx_agend_func_agendamento_id
    ON app.agendamento_funcionario(agendamento_id);

CREATE INDEX IF NOT EXISTS idx_agend_func_funcionario_id
    ON app.agendamento_funcionario(funcionario_id);

-- agendamento_servico
CREATE INDEX IF NOT EXISTS idx_agend_serv_agendamento_id
    ON app.agendamento_servico(agendamento_id);

CREATE INDEX IF NOT EXISTS idx_agend_serv_servico_id
    ON app.agendamento_servico(servico_id);

-- funcionario_servico
CREATE INDEX IF NOT EXISTS idx_func_serv_funcionario_id
    ON app.funcionario_servico(funcionario_id);

CREATE INDEX IF NOT EXISTS idx_func_serv_servico_id
    ON app.funcionario_servico(servico_id);

-- ===========================================
-- 20. API_KEYS
-- ===========================================

-- FK: organizacao_id + ativo
CREATE INDEX IF NOT EXISTS idx_apikey_org_ativo
    ON app.api_keys(organizacao_id, ativo);

-- User + type + ativo (busca de chaves por usuário)
CREATE INDEX IF NOT EXISTS idx_apikey_user_type_ativo
    ON app.api_keys(user_id, user_type, ativo);

-- Expiração (cleanup de chaves expiradas)
CREATE INDEX IF NOT EXISTS idx_apikey_expires_at
    ON app.api_keys(expires_at)
    WHERE expires_at IS NOT NULL;

-- ===========================================
-- 21. INSTANCE (complementos)
-- ===========================================

-- FK: organizacao_id (não tinha)
CREATE INDEX IF NOT EXISTS idx_instance_organizacao_id
    ON app.instance(organizacao_id);

-- Org + não deletado (listagem principal)
CREATE INDEX IF NOT EXISTS idx_instance_org_deletado
    ON app.instance(organizacao_id, deletado)
    WHERE deletado = false;

-- ===========================================
-- 22. INSTANCE_KNOWLEDGEBASE
-- ===========================================

-- FK: instance_id
CREATE INDEX IF NOT EXISTS idx_kb_instance_id
    ON app.instance_knowledgebase(instance_id);

-- Instance + tipo
CREATE INDEX IF NOT EXISTS idx_kb_instance_type
    ON app.instance_knowledgebase(instance_id, type);

-- ===========================================
-- 23. PLANO
-- ===========================================

-- Ativo (listagem de planos disponíveis)
CREATE INDEX IF NOT EXISTS idx_plano_ativo
    ON app.plano(ativo)
    WHERE ativo = true;

-- ===========================================
-- 24. PLANO_SERVICO
-- ===========================================

-- FK: organizacao_id
CREATE INDEX IF NOT EXISTS idx_plano_servico_organizacao_id
    ON app.plano_servico(organizacao_id);

-- Org + ativo
CREATE INDEX IF NOT EXISTS idx_plano_servico_org_ativo
    ON app.plano_servico(organizacao_id, ativo);

-- ===========================================
-- 25. PLANO_CLIENTE
-- ===========================================

-- FK: cliente_id
CREATE INDEX IF NOT EXISTS idx_plano_cliente_cliente_id
    ON app.plano_cliente(cliente_id);

-- FK: plano_servico_id
CREATE INDEX IF NOT EXISTS idx_plano_cliente_plano_servico_id
    ON app.plano_cliente(plano_servico_id);

-- Cliente + status (planos ativos do cliente)
CREATE INDEX IF NOT EXISTS idx_plano_cliente_cliente_status
    ON app.plano_cliente(cliente_id, status);

-- Data fim (verificação de expiração)
CREATE INDEX IF NOT EXISTS idx_plano_cliente_dt_fim
    ON app.plano_cliente(dt_fim)
    WHERE dt_fim IS NOT NULL;

-- ===========================================
-- 26. PLANO_LIMITES
-- ===========================================

-- FK: plano_id (já tem UNIQUE, mas para joins)
-- SKIP: uc_plano_limites_plano já cobre

-- ===========================================
-- 27. PLANO_BELLORY (admin schema)
-- ===========================================

-- Ativo + ordem (listagem pública de planos)
CREATE INDEX IF NOT EXISTS idx_plano_bellory_ativo_ordem
    ON admin.plano_bellory(ativo, ordem_exibicao)
    WHERE ativo = true;

-- ===========================================
-- 28. COMPLEMENTOS FINANCEIRO
-- ===========================================

-- conta_pagar: FK conta_bancaria_id (faltava)
CREATE INDEX IF NOT EXISTS idx_cp_conta_bancaria
    ON app.conta_pagar(conta_bancaria_id);

-- conta_pagar: composto org + dt_vencimento (queries de vencimento)
CREATE INDEX IF NOT EXISTS idx_cp_org_vencimento
    ON app.conta_pagar(organizacao_id, dt_vencimento);

-- conta_pagar: composto org + status (listagem filtrada)
CREATE INDEX IF NOT EXISTS idx_cp_org_status
    ON app.conta_pagar(organizacao_id, status);

-- conta_pagar: FK conta_pagar_origem_id (recorrência)
CREATE INDEX IF NOT EXISTS idx_cp_origem
    ON app.conta_pagar(conta_pagar_origem_id)
    WHERE conta_pagar_origem_id IS NOT NULL;

-- conta_pagar: fornecedor (busca por fornecedor)
CREATE INDEX IF NOT EXISTS idx_cp_fornecedor
    ON app.conta_pagar(fornecedor)
    WHERE fornecedor IS NOT NULL;

-- conta_receber: FK conta_bancaria_id (faltava)
CREATE INDEX IF NOT EXISTS idx_cr_conta_bancaria
    ON app.conta_receber(conta_bancaria_id);

-- conta_receber: FK cobranca_id
CREATE INDEX IF NOT EXISTS idx_cr_cobranca
    ON app.conta_receber(cobranca_id)
    WHERE cobranca_id IS NOT NULL;

-- conta_receber: FK conta_receber_origem_id (recorrência)
CREATE INDEX IF NOT EXISTS idx_cr_origem
    ON app.conta_receber(conta_receber_origem_id)
    WHERE conta_receber_origem_id IS NOT NULL;

-- conta_receber: composto org + dt_vencimento
CREATE INDEX IF NOT EXISTS idx_cr_org_vencimento
    ON app.conta_receber(organizacao_id, dt_vencimento);

-- conta_receber: composto org + status
CREATE INDEX IF NOT EXISTS idx_cr_org_status
    ON app.conta_receber(organizacao_id, status);

-- lancamento_financeiro: FK conta_bancaria_destino_id (transferências)
CREATE INDEX IF NOT EXISTS idx_lf_conta_destino
    ON app.lancamento_financeiro(conta_bancaria_destino_id)
    WHERE conta_bancaria_destino_id IS NOT NULL;

-- lancamento_financeiro: composto org + tipo + período
CREATE INDEX IF NOT EXISTS idx_lf_org_tipo_dt
    ON app.lancamento_financeiro(organizacao_id, tipo, dt_lancamento);

-- lancamento_financeiro: composto org + status + período
CREATE INDEX IF NOT EXISTS idx_lf_org_status_dt
    ON app.lancamento_financeiro(organizacao_id, status, dt_lancamento);

-- categoria_financeira: org + ativo (faltava composto)
CREATE INDEX IF NOT EXISTS idx_catfin_org_ativo
    ON app.categoria_financeira(organizacao_id, ativo);

-- centro_custo: org + ativo
CREATE INDEX IF NOT EXISTS idx_cc_org_ativo
    ON app.centro_custo(organizacao_id, ativo);

-- conta_bancaria: org + ativo
CREATE INDEX IF NOT EXISTS idx_cb_org_ativo
    ON app.conta_bancaria(organizacao_id, ativo);

-- conta_bancaria: principal
CREATE INDEX IF NOT EXISTS idx_cb_org_principal
    ON app.conta_bancaria(organizacao_id, principal)
    WHERE principal = true;

-- ===========================================
-- 29. NOTIFICACAO (complementos)
-- ===========================================

-- Config notificação: org + tipo (faltava)
CREATE INDEX IF NOT EXISTS idx_config_notif_org_tipo
    ON app.config_notificacao(organizacao_id, tipo);

-- Notificação enviada: dt_envio + status (falhas recentes)
CREATE INDEX IF NOT EXISTS idx_notif_env_dt_status
    ON app.notificacao_enviada(dt_envio, status);

-- Notificação enviada: resposta_cliente (filtro de respostas)
CREATE INDEX IF NOT EXISTS idx_notif_env_resposta
    ON app.notificacao_enviada(resposta_cliente)
    WHERE resposta_cliente IS NOT NULL;

-- ===========================================
-- 30. TRACKING (complementos ao V21)
-- ===========================================

-- Sessions: UTM campaign (analytics de campanhas)
CREATE INDEX IF NOT EXISTS idx_tracking_sessions_utm_campaign
    ON site.tracking_sessions(utm_campaign)
    WHERE utm_campaign IS NOT NULL;

-- Sessions: device_type (analytics de dispositivo)
CREATE INDEX IF NOT EXISTS idx_tracking_sessions_device
    ON site.tracking_sessions(device_type);

-- Sessions: bounce rate
CREATE INDEX IF NOT EXISTS idx_tracking_sessions_bounce
    ON site.tracking_sessions(is_bounce);

-- Sessions: visitor + started (histórico de sessões)
CREATE INDEX IF NOT EXISTS idx_tracking_sessions_visitor_started
    ON site.tracking_sessions(visitor_id, started_at);

-- Page views: visitor_id (histórico do visitante)
CREATE INDEX IF NOT EXISTS idx_tracking_pv_visitor
    ON site.tracking_page_views(visitor_id);

-- Visitors: last_seen_at (visitantes ativos)
CREATE INDEX IF NOT EXISTS idx_tracking_visitors_last_seen
    ON site.tracking_visitors(last_seen_at);

-- Visitors: geo (analytics geográfico)
CREATE INDEX IF NOT EXISTS idx_tracking_visitors_country
    ON site.tracking_visitors(country)
    WHERE country IS NOT NULL;

-- Error events: session_id (erros por sessão)
CREATE INDEX IF NOT EXISTS idx_tracking_ee_session
    ON site.tracking_error_events(session_id);

-- Error events: status_code (erros HTTP)
CREATE INDEX IF NOT EXISTS idx_tracking_ee_status_code
    ON site.tracking_error_events(status_code)
    WHERE status_code IS NOT NULL;

-- Scroll events: page_path (scroll por página)
CREATE INDEX IF NOT EXISTS idx_tracking_se_page
    ON site.tracking_scroll_events(page_path);

-- Interaction events: visitor_id (interações do visitante)
CREATE INDEX IF NOT EXISTS idx_tracking_ie_visitor
    ON site.tracking_interaction_events(visitor_id);

-- Conversion events: session_id (conversões por sessão)
CREATE INDEX IF NOT EXISTS idx_tracking_ce_session
    ON site.tracking_conversion_events(session_id);

-- Performance: device + browser (performance por dispositivo)
CREATE INDEX IF NOT EXISTS idx_tracking_ps_device
    ON site.tracking_performance_snapshots(device_type)
    WHERE device_type IS NOT NULL;

-- ===========================================
-- 31. ELEMENT COLLECTIONS (tabelas auxiliares)
-- ===========================================

-- servico_produtos (FK do servico)
CREATE INDEX IF NOT EXISTS idx_servico_produtos_servico_id
    ON app.servico_produtos(servico_id);

-- servico_imagens (FK do servico)
CREATE INDEX IF NOT EXISTS idx_servico_imagens_servico_id
    ON app.servico_imagens(servico_id);

-- produto_imagens (FK do produto)
CREATE INDEX IF NOT EXISTS idx_produto_imagens_produto_id
    ON app.produto_imagens(produto_id);

-- produto_ingredientes (FK do produto)
CREATE INDEX IF NOT EXISTS idx_produto_ingredientes_produto_id
    ON app.produto_ingredientes(produto_id);
