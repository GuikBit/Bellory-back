-- =============================================
-- Modulo Webhook Payment API
-- =============================================

-- Token de autenticacao do webhook (um por instalacao)
CREATE TABLE IF NOT EXISTS admin.webhook_config (
    id              BIGSERIAL PRIMARY KEY,
    token           VARCHAR(500) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    descricao       VARCHAR(255),
    dt_criacao      TIMESTAMP NOT NULL DEFAULT NOW(),
    dt_atualizacao  TIMESTAMP
);

-- Configuracao por tipo de evento (quais acoes disparar)
CREATE TABLE IF NOT EXISTS admin.webhook_event_config (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL UNIQUE,
    descricao       VARCHAR(255),
    push_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    invalidar_cache BOOLEAN NOT NULL DEFAULT FALSE,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    dt_criacao      TIMESTAMP NOT NULL DEFAULT NOW(),
    dt_atualizacao  TIMESTAMP
);

-- Log de eventos recebidos (historico + idempotencia)
CREATE TABLE IF NOT EXISTS admin.webhook_event_log (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(100) NOT NULL,
    delivery_id     VARCHAR(100),
    event_type      VARCHAR(100) NOT NULL,
    company_id      BIGINT,
    organizacao_id  BIGINT,
    resource_type   VARCHAR(50),
    resource_id     VARCHAR(50),
    payload         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    error_message   TEXT,
    dt_recebido     TIMESTAMP NOT NULL DEFAULT NOW(),
    dt_processado   TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_webhook_event_log_event_id ON admin.webhook_event_log(event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_event_log_type ON admin.webhook_event_log(event_type);
CREATE INDEX IF NOT EXISTS idx_webhook_event_log_org ON admin.webhook_event_log(organizacao_id);
CREATE INDEX IF NOT EXISTS idx_webhook_event_log_status ON admin.webhook_event_log(status);
CREATE INDEX IF NOT EXISTS idx_webhook_event_log_dt ON admin.webhook_event_log(dt_recebido DESC);

-- Seed: configuracao padrao dos eventos
INSERT INTO admin.webhook_event_config (event_type, descricao, push_enabled, email_enabled, invalidar_cache) VALUES
    ('ChargeCreatedEvent',              'Cobranca criada',                          TRUE,  TRUE,  FALSE),
    ('ChargePaidEvent',                 'Pagamento confirmado',                     TRUE,  TRUE,  TRUE),
    ('ChargeCanceledEvent',             'Cobranca cancelada',                       TRUE,  FALSE, FALSE),
    ('ChargeRefundedEvent',             'Estorno realizado',                        TRUE,  TRUE,  FALSE),
    ('SubscriptionCreatedEvent',        'Assinatura criada',                        FALSE, FALSE, FALSE),
    ('SubscriptionPausedEvent',         'Assinatura pausada',                       TRUE,  TRUE,  TRUE),
    ('SubscriptionResumedEvent',        'Assinatura retomada',                      TRUE,  TRUE,  TRUE),
    ('SubscriptionSuspendedEvent',      'Assinatura suspensa por inadimplencia',    TRUE,  TRUE,  TRUE),
    ('SubscriptionCanceledEvent',       'Assinatura cancelada',                     TRUE,  TRUE,  TRUE),
    ('PlanChangeScheduledEvent',        'Troca de plano agendada',                  TRUE,  FALSE, FALSE),
    ('PlanChangePendingPaymentEvent',   'Troca de plano aguardando pagamento',      TRUE,  FALSE, FALSE),
    ('PlanChangedEvent',                'Troca de plano efetivada',                 TRUE,  TRUE,  TRUE),
    ('CustomerCreatedEvent',            'Cliente criado na Payment API',            FALSE, FALSE, FALSE),
    ('WebhookTestEvent',                'Evento de teste (ping)',                   FALSE, FALSE, FALSE)
ON CONFLICT (event_type) DO NOTHING;
