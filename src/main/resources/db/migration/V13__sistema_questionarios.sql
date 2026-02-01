-- ============================================
-- V13: Sistema de Questionários
-- ============================================

-- Tabela de Questionários
CREATE TABLE IF NOT EXISTS app.questionario (
    id BIGSERIAL PRIMARY KEY,
    organizacao_id BIGINT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(1000),
    tipo VARCHAR(50) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE NOT NULL,
    obrigatorio BOOLEAN DEFAULT FALSE,
    anonimo BOOLEAN DEFAULT FALSE,
    url_imagem VARCHAR(500),
    cor_tema VARCHAR(7),
    dt_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dt_atualizacao TIMESTAMP,
    usuario_atualizacao VARCHAR(255),
    is_deletado BOOLEAN DEFAULT FALSE NOT NULL,
    usuario_deletado VARCHAR(255),
    dt_deletado TIMESTAMP,

    CONSTRAINT fk_questionario_organizacao
        FOREIGN KEY (organizacao_id)
        REFERENCES app.organizacao(id)
        ON DELETE CASCADE
);

-- Índices para Questionário
CREATE INDEX IF NOT EXISTS idx_questionario_organizacao ON app.questionario(organizacao_id);
CREATE INDEX IF NOT EXISTS idx_questionario_tipo ON app.questionario(tipo);
CREATE INDEX IF NOT EXISTS idx_questionario_ativo ON app.questionario(ativo);
CREATE INDEX IF NOT EXISTS idx_questionario_deletado ON app.questionario(is_deletado);

-- Tabela de Perguntas
CREATE TABLE IF NOT EXISTS app.pergunta (
    id BIGSERIAL PRIMARY KEY,
    questionario_id BIGINT NOT NULL,
    texto VARCHAR(500) NOT NULL,
    descricao VARCHAR(1000),
    tipo VARCHAR(50) NOT NULL,
    obrigatoria BOOLEAN DEFAULT FALSE NOT NULL,
    ordem INTEGER NOT NULL,
    escala_min INTEGER,
    escala_max INTEGER,
    label_min VARCHAR(100),
    label_max VARCHAR(100),
    min_caracteres INTEGER,
    max_caracteres INTEGER,
    min_valor DECIMAL(15,4),
    max_valor DECIMAL(15,4),

    CONSTRAINT fk_pergunta_questionario
        FOREIGN KEY (questionario_id)
        REFERENCES app.questionario(id)
        ON DELETE CASCADE
);

-- Índices para Pergunta
CREATE INDEX IF NOT EXISTS idx_pergunta_questionario ON app.pergunta(questionario_id);
CREATE INDEX IF NOT EXISTS idx_pergunta_ordem ON app.pergunta(questionario_id, ordem);

-- Tabela de Opções de Resposta
CREATE TABLE IF NOT EXISTS app.opcao_resposta (
    id BIGSERIAL PRIMARY KEY,
    pergunta_id BIGINT NOT NULL,
    texto VARCHAR(255) NOT NULL,
    valor VARCHAR(255),
    ordem INTEGER NOT NULL,

    CONSTRAINT fk_opcao_pergunta
        FOREIGN KEY (pergunta_id)
        REFERENCES app.pergunta(id)
        ON DELETE CASCADE
);

-- Índices para Opção de Resposta
CREATE INDEX IF NOT EXISTS idx_opcao_pergunta ON app.opcao_resposta(pergunta_id);
CREATE INDEX IF NOT EXISTS idx_opcao_ordem ON app.opcao_resposta(pergunta_id, ordem);

-- Tabela de Respostas do Questionário
CREATE TABLE IF NOT EXISTS app.resposta_questionario (
    id BIGSERIAL PRIMARY KEY,
    questionario_id BIGINT NOT NULL,
    cliente_id BIGINT,
    colaborador_id BIGINT,
    agendamento_id BIGINT,
    dt_resposta TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_origem VARCHAR(45),
    dispositivo VARCHAR(100),
    tempo_preenchimento_segundos INTEGER,
    user_agent VARCHAR(500),

    CONSTRAINT fk_resposta_questionario
        FOREIGN KEY (questionario_id)
        REFERENCES app.questionario(id)
        ON DELETE CASCADE
);

-- Índices para Resposta do Questionário
CREATE INDEX IF NOT EXISTS idx_resposta_questionario ON app.resposta_questionario(questionario_id);
CREATE INDEX IF NOT EXISTS idx_resposta_cliente ON app.resposta_questionario(cliente_id);
CREATE INDEX IF NOT EXISTS idx_resposta_agendamento ON app.resposta_questionario(agendamento_id);
CREATE INDEX IF NOT EXISTS idx_resposta_dt_resposta ON app.resposta_questionario(dt_resposta);

-- Índice composto para verificação de duplicidade
CREATE INDEX IF NOT EXISTS idx_resposta_questionario_cliente
    ON app.resposta_questionario(questionario_id, cliente_id);

-- Tabela de Respostas por Pergunta
CREATE TABLE IF NOT EXISTS app.resposta_pergunta (
    id BIGSERIAL PRIMARY KEY,
    resposta_questionario_id BIGINT NOT NULL,
    pergunta_id BIGINT NOT NULL,
    resposta_texto TEXT,
    resposta_numero DECIMAL(15,4),
    resposta_data DATE,
    resposta_hora TIME,

    CONSTRAINT fk_resposta_pergunta_questionario
        FOREIGN KEY (resposta_questionario_id)
        REFERENCES app.resposta_questionario(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_resposta_pergunta_pergunta
        FOREIGN KEY (pergunta_id)
        REFERENCES app.pergunta(id)
        ON DELETE CASCADE
);

-- Índices para Resposta por Pergunta
CREATE INDEX IF NOT EXISTS idx_resposta_pergunta_questionario
    ON app.resposta_pergunta(resposta_questionario_id);
CREATE INDEX IF NOT EXISTS idx_resposta_pergunta_pergunta
    ON app.resposta_pergunta(pergunta_id);

-- Tabela de Opções Selecionadas (para múltipla escolha)
CREATE TABLE IF NOT EXISTS app.resposta_opcoes_selecionadas (
    resposta_pergunta_id BIGINT NOT NULL,
    opcao_id BIGINT NOT NULL,

    PRIMARY KEY (resposta_pergunta_id, opcao_id),
    CONSTRAINT fk_opcoes_selecionadas_resposta
        FOREIGN KEY (resposta_pergunta_id)
        REFERENCES app.resposta_pergunta(id)
        ON DELETE CASCADE
);

-- Índices para Opções Selecionadas
CREATE INDEX IF NOT EXISTS idx_opcoes_selecionadas_resposta
    ON app.resposta_opcoes_selecionadas(resposta_pergunta_id);
CREATE INDEX IF NOT EXISTS idx_opcoes_selecionadas_opcao
    ON app.resposta_opcoes_selecionadas(opcao_id);

-- Comentários nas tabelas
COMMENT ON TABLE app.questionario IS 'Tabela principal de questionários';
COMMENT ON TABLE app.pergunta IS 'Perguntas pertencentes aos questionários';
COMMENT ON TABLE app.opcao_resposta IS 'Opções de resposta para perguntas de seleção';
COMMENT ON TABLE app.resposta_questionario IS 'Registro de cada resposta completa a um questionário';
COMMENT ON TABLE app.resposta_pergunta IS 'Respostas individuais para cada pergunta';
COMMENT ON TABLE app.resposta_opcoes_selecionadas IS 'Opções selecionadas em perguntas de múltipla escolha';

-- Comentários nas colunas principais
COMMENT ON COLUMN app.questionario.tipo IS 'Tipo do questionário: CLIENTE, COLABORADOR, AVALIACAO_DESEMPENHO, FEEDBACK_ATENDIMENTO, FEEDBACK_AGENDAMENTO, FEEDBACK_BOT, FEEDBACK_GERAL, PESQUISA_SATISFACAO, OUTRO';
COMMENT ON COLUMN app.pergunta.tipo IS 'Tipo da pergunta: TEXTO_CURTO, TEXTO_LONGO, NUMERO, SELECAO_UNICA, SELECAO_MULTIPLA, ESCALA, DATA, HORA, AVALIACAO_ESTRELAS, SIM_NAO';
COMMENT ON COLUMN app.questionario.anonimo IS 'Se true, permite múltiplas respostas do mesmo cliente';
COMMENT ON COLUMN app.resposta_questionario.tempo_preenchimento_segundos IS 'Tempo em segundos que o usuário levou para preencher o questionário';
