-- =============================================
-- V16 - Módulo Financeiro Completo
-- =============================================

-- 1. Categoria Financeira (hierárquica)
CREATE TABLE app.categoria_financeira (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id),
    categoria_pai_id BIGINT REFERENCES app.categoria_financeira(id),
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    tipo VARCHAR(20) NOT NULL, -- RECEITA, DESPESA
    cor VARCHAR(7),
    icone VARCHAR(50),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    dt_criacao TIMESTAMP DEFAULT now(),
    dt_atualizacao TIMESTAMP
);

CREATE INDEX idx_catfin_organizacao ON app.categoria_financeira(organizacao_id);
CREATE INDEX idx_catfin_tipo ON app.categoria_financeira(tipo);
CREATE INDEX idx_catfin_pai ON app.categoria_financeira(categoria_pai_id);

-- 2. Centro de Custo
CREATE TABLE app.centro_custo (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id),
    nome VARCHAR(100) NOT NULL,
    codigo VARCHAR(20),
    descricao VARCHAR(255),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    dt_criacao TIMESTAMP DEFAULT now(),
    dt_atualizacao TIMESTAMP
);

CREATE INDEX idx_cc_organizacao ON app.centro_custo(organizacao_id);

-- 3. Conta Bancária
CREATE TABLE app.conta_bancaria (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id),
    nome VARCHAR(100) NOT NULL,
    tipo_conta VARCHAR(30) NOT NULL, -- CONTA_CORRENTE, POUPANCA, CAIXA, CARTEIRA_DIGITAL
    banco VARCHAR(100),
    agencia VARCHAR(20),
    numero_conta VARCHAR(30),
    saldo_inicial DECIMAL(14,2) NOT NULL DEFAULT 0,
    saldo_atual DECIMAL(14,2) NOT NULL DEFAULT 0,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    cor VARCHAR(7),
    icone VARCHAR(50),
    dt_criacao TIMESTAMP DEFAULT now(),
    dt_atualizacao TIMESTAMP
);

CREATE INDEX idx_cb_organizacao ON app.conta_bancaria(organizacao_id);

-- 4. Conta a Pagar
CREATE TABLE app.conta_pagar (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id),
    categoria_financeira_id BIGINT REFERENCES app.categoria_financeira(id),
    centro_custo_id BIGINT REFERENCES app.centro_custo(id),
    conta_bancaria_id BIGINT REFERENCES app.conta_bancaria(id),
    descricao VARCHAR(255) NOT NULL,
    fornecedor VARCHAR(200),
    documento VARCHAR(50),
    numero_nota VARCHAR(50),
    valor DECIMAL(14,2) NOT NULL,
    valor_pago DECIMAL(14,2) DEFAULT 0,
    valor_desconto DECIMAL(14,2) DEFAULT 0,
    valor_juros DECIMAL(14,2) DEFAULT 0,
    valor_multa DECIMAL(14,2) DEFAULT 0,
    dt_emissao DATE,
    dt_vencimento DATE NOT NULL,
    dt_pagamento DATE,
    dt_competencia DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, PAGA, VENCIDA, CANCELADA, PARCIALMENTE_PAGA
    forma_pagamento VARCHAR(30),
    recorrente BOOLEAN NOT NULL DEFAULT FALSE,
    periodicidade VARCHAR(20), -- MENSAL, QUINZENAL, SEMANAL, ANUAL
    parcela_atual INTEGER,
    total_parcelas INTEGER,
    conta_pagar_origem_id BIGINT REFERENCES app.conta_pagar(id),
    observacoes TEXT,
    dt_criacao TIMESTAMP DEFAULT now(),
    dt_atualizacao TIMESTAMP
);

CREATE INDEX idx_cp_organizacao ON app.conta_pagar(organizacao_id);
CREATE INDEX idx_cp_status ON app.conta_pagar(status);
CREATE INDEX idx_cp_vencimento ON app.conta_pagar(dt_vencimento);
CREATE INDEX idx_cp_categoria ON app.conta_pagar(categoria_financeira_id);
CREATE INDEX idx_cp_centro_custo ON app.conta_pagar(centro_custo_id);
CREATE INDEX idx_cp_competencia ON app.conta_pagar(dt_competencia);

-- 5. Conta a Receber
CREATE TABLE app.conta_receber (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id),
    categoria_financeira_id BIGINT REFERENCES app.categoria_financeira(id),
    centro_custo_id BIGINT REFERENCES app.centro_custo(id),
    conta_bancaria_id BIGINT REFERENCES app.conta_bancaria(id),
    cliente_id BIGINT REFERENCES app.cliente(id),
    cobranca_id BIGINT REFERENCES app.cobranca(id),
    descricao VARCHAR(255) NOT NULL,
    documento VARCHAR(50),
    numero_nota VARCHAR(50),
    valor DECIMAL(14,2) NOT NULL,
    valor_recebido DECIMAL(14,2) DEFAULT 0,
    valor_desconto DECIMAL(14,2) DEFAULT 0,
    valor_juros DECIMAL(14,2) DEFAULT 0,
    valor_multa DECIMAL(14,2) DEFAULT 0,
    dt_emissao DATE,
    dt_vencimento DATE NOT NULL,
    dt_recebimento DATE,
    dt_competencia DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, RECEBIDA, VENCIDA, CANCELADA, PARCIALMENTE_RECEBIDA
    forma_pagamento VARCHAR(30),
    recorrente BOOLEAN NOT NULL DEFAULT FALSE,
    periodicidade VARCHAR(20),
    parcela_atual INTEGER,
    total_parcelas INTEGER,
    conta_receber_origem_id BIGINT REFERENCES app.conta_receber(id),
    observacoes TEXT,
    dt_criacao TIMESTAMP DEFAULT now(),
    dt_atualizacao TIMESTAMP
);

CREATE INDEX idx_cr_organizacao ON app.conta_receber(organizacao_id);
CREATE INDEX idx_cr_status ON app.conta_receber(status);
CREATE INDEX idx_cr_vencimento ON app.conta_receber(dt_vencimento);
CREATE INDEX idx_cr_categoria ON app.conta_receber(categoria_financeira_id);
CREATE INDEX idx_cr_centro_custo ON app.conta_receber(centro_custo_id);
CREATE INDEX idx_cr_cliente ON app.conta_receber(cliente_id);
CREATE INDEX idx_cr_competencia ON app.conta_receber(dt_competencia);

-- 6. Lançamento Financeiro (Livro Caixa / Razão)
CREATE TABLE app.lancamento_financeiro (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL REFERENCES app.organizacao(id),
    categoria_financeira_id BIGINT REFERENCES app.categoria_financeira(id),
    centro_custo_id BIGINT REFERENCES app.centro_custo(id),
    conta_bancaria_id BIGINT REFERENCES app.conta_bancaria(id),
    conta_bancaria_destino_id BIGINT REFERENCES app.conta_bancaria(id),
    conta_pagar_id BIGINT REFERENCES app.conta_pagar(id),
    conta_receber_id BIGINT REFERENCES app.conta_receber(id),
    tipo VARCHAR(20) NOT NULL, -- RECEITA, DESPESA, TRANSFERENCIA
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(14,2) NOT NULL,
    dt_lancamento DATE NOT NULL,
    dt_competencia DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'EFETIVADO', -- EFETIVADO, PENDENTE, CANCELADO
    forma_pagamento VARCHAR(30),
    documento VARCHAR(50),
    numero_nota VARCHAR(50),
    observacoes TEXT,
    dt_criacao TIMESTAMP DEFAULT now(),
    dt_atualizacao TIMESTAMP
);

CREATE INDEX idx_lf_organizacao ON app.lancamento_financeiro(organizacao_id);
CREATE INDEX idx_lf_tipo ON app.lancamento_financeiro(tipo);
CREATE INDEX idx_lf_dt_lancamento ON app.lancamento_financeiro(dt_lancamento);
CREATE INDEX idx_lf_dt_competencia ON app.lancamento_financeiro(dt_competencia);
CREATE INDEX idx_lf_status ON app.lancamento_financeiro(status);
CREATE INDEX idx_lf_categoria ON app.lancamento_financeiro(categoria_financeira_id);
CREATE INDEX idx_lf_centro_custo ON app.lancamento_financeiro(centro_custo_id);
CREATE INDEX idx_lf_conta_bancaria ON app.lancamento_financeiro(conta_bancaria_id);
CREATE INDEX idx_lf_conta_pagar ON app.lancamento_financeiro(conta_pagar_id);
CREATE INDEX idx_lf_conta_receber ON app.lancamento_financeiro(conta_receber_id);

-- Inserir categorias financeiras padrão (serão associadas à organização via seed)
-- As categorias padrão são criadas pelo DatabaseSeederService
