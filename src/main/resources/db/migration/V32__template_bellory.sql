-- =============================================================================
-- V32: Tabela admin.template_bellory - Templates globais da plataforma
-- =============================================================================

CREATE TABLE IF NOT EXISTS admin.template_bellory (
    id                      BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(50) NOT NULL UNIQUE,
    nome                    VARCHAR(100) NOT NULL,
    descricao               TEXT,
    tipo                    VARCHAR(20) NOT NULL,
    categoria               VARCHAR(30) NOT NULL,
    assunto                 VARCHAR(255),
    conteudo                TEXT NOT NULL,
    variaveis_disponiveis   JSONB,
    ativo                   BOOLEAN NOT NULL DEFAULT true,
    padrao                  BOOLEAN NOT NULL DEFAULT false,
    icone                   VARCHAR(50),
    dt_criacao              TIMESTAMP NOT NULL DEFAULT NOW(),
    dt_atualizacao          TIMESTAMP,
    user_criacao            BIGINT,
    user_atualizacao        BIGINT
);

-- Indices
CREATE INDEX idx_template_tipo_categoria ON admin.template_bellory (tipo, categoria);
CREATE INDEX idx_template_ativo ON admin.template_bellory (ativo);
CREATE UNIQUE INDEX idx_template_padrao ON admin.template_bellory (tipo, categoria) WHERE padrao = true;

-- =============================================================================
-- Seed: Templates padrao
-- =============================================================================

-- 1. WhatsApp - Confirmacao
INSERT INTO admin.template_bellory (codigo, nome, descricao, tipo, categoria, conteudo, variaveis_disponiveis, padrao, icone)
VALUES (
    'whatsapp-confirmacao',
    'Confirmacao de Agendamento',
    'Mensagem padrao de confirmacao de agendamento via WhatsApp',
    'WHATSAPP',
    'CONFIRMACAO',
    E'Ol\u00e1 *{{nome_cliente}}*! \uD83D\uDC4B\n\n\u2705 Seu agendamento est\u00e1 *aguardando confirma\u00e7\u00e3o*!\n\n\uD83D\uDCCB *Detalhes do agendamento:*\n- Servi\u00e7o: {{servico}}\n- Data: {{data_agendamento}}\n- Hor\u00e1rio: {{hora_agendamento}}\n- Profissional: {{profissional}}\n- Local: {{local}}\n- Valor: {{valor}}\n\n\uD83D\uDCCD _{{nome_empresa}}_\n\nPodemos confirmar? Digite: \uD83D\uDE0A\n*Sim* para confirmar \u2705\n*N\u00e3o* para cancelar \u274C\n*Remarcar* para reagendar o servi\u00e7o \uD83D\uDCC5\n\n_Estamos aguardando o seu retorno._',
    '[{"nome":"nome_cliente","descricao":"Nome do cliente","exemplo":"Joao Silva"},{"nome":"data_agendamento","descricao":"Data do agendamento","exemplo":"15/03/2026"},{"nome":"hora_agendamento","descricao":"Horario do agendamento","exemplo":"14:30"},{"nome":"servico","descricao":"Nome do servico","exemplo":"Corte Masculino"},{"nome":"profissional","descricao":"Nome do profissional","exemplo":"Maria Santos"},{"nome":"local","descricao":"Endereco do estabelecimento","exemplo":"Rua das Flores, 123"},{"nome":"valor","descricao":"Valor do servico","exemplo":"R$ 50,00"},{"nome":"nome_empresa","descricao":"Nome da empresa","exemplo":"Barbearia Top"}]',
    true,
    'MessageSquare'
);

-- 2. WhatsApp - Lembrete
INSERT INTO admin.template_bellory (codigo, nome, descricao, tipo, categoria, conteudo, variaveis_disponiveis, padrao, icone)
VALUES (
    'whatsapp-lembrete',
    'Lembrete de Agendamento',
    'Mensagem padrao de lembrete de agendamento via WhatsApp',
    'WHATSAPP',
    'LEMBRETE',
    E'Ol\u00e1 *{{nome_cliente}}*! \uD83D\uDD14\n\n\u23F0 *Lembrete do seu agendamento:*\n\nVoc\u00ea tem um hor\u00e1rio marcado em breve!\n\n\uD83D\uDCCB *Detalhes:*\n- Servi\u00e7o: {{servico}}\n- Data: {{data_agendamento}}\n- Hor\u00e1rio: {{hora_agendamento}}\n- Profissional: {{profissional}}\n- Local: {{local}}\n\n\uD83D\uDCB0 Valor: {{valor}}\n\n\uD83D\uDCCD _{{nome_empresa}}_\n\n~N\u00e3o se atrase!~ \u23F1\uFE0F Chegue com alguns minutos de anteced\u00eancia.\n\nAt\u00e9 logo! \uD83D\uDE0A',
    '[{"nome":"nome_cliente","descricao":"Nome do cliente","exemplo":"Joao Silva"},{"nome":"data_agendamento","descricao":"Data do agendamento","exemplo":"15/03/2026"},{"nome":"hora_agendamento","descricao":"Horario do agendamento","exemplo":"14:30"},{"nome":"servico","descricao":"Nome do servico","exemplo":"Corte Masculino"},{"nome":"profissional","descricao":"Nome do profissional","exemplo":"Maria Santos"},{"nome":"local","descricao":"Endereco do estabelecimento","exemplo":"Rua das Flores, 123"},{"nome":"valor","descricao":"Valor do servico","exemplo":"R$ 50,00"},{"nome":"nome_empresa","descricao":"Nome da empresa","exemplo":"Barbearia Top"}]',
    true,
    'Bell'
);

-- 3. Email - Bem-vindo
INSERT INTO admin.template_bellory (codigo, nome, descricao, tipo, categoria, assunto, conteudo, variaveis_disponiveis, padrao, icone)
VALUES (
    'email-bem-vindo',
    'Bem-vindo a Bellory',
    'Email de boas-vindas enviado ao criar uma nova organizacao',
    'EMAIL',
    'BEM_VINDO',
    'Bem-vindo a Bellory!',
    'bem-vindo-organizacao',
    '[{"nome":"nomeOrganizacao","descricao":"Nome fantasia da organizacao","exemplo":"Barbearia Top"},{"nome":"razaoSocial","descricao":"Razao social","exemplo":"Barbearia Top LTDA"},{"nome":"cnpj","descricao":"CNPJ formatado","exemplo":"12.345.678/0001-90"},{"nome":"slug","descricao":"Identificador unico","exemplo":"barbearia-top-abc12"},{"nome":"email","descricao":"Email principal","exemplo":"contato@barbearia.com"},{"nome":"username","descricao":"Username do admin","exemplo":"admin"},{"nome":"emailAdmin","descricao":"Email do admin","exemplo":"admin@barbearia.com"},{"nome":"urlSistema","descricao":"URL do sistema","exemplo":"https://app.bellory.com.br"}]',
    true,
    'Mail'
);

-- 4. Email - Reset de Senha
INSERT INTO admin.template_bellory (codigo, nome, descricao, tipo, categoria, assunto, conteudo, variaveis_disponiveis, padrao, icone)
VALUES (
    'email-reset-senha',
    'Recuperacao de Senha',
    'Email com codigo de recuperacao de senha',
    'EMAIL',
    'RESET_SENHA',
    'Recuperacao de Senha - Bellory',
    'resetar-senha',
    '[{"nome":"nomeCompleto","descricao":"Nome completo do usuario","exemplo":"Joao Silva"},{"nome":"codigo","descricao":"Codigo de verificacao","exemplo":"123456"},{"nome":"validadeMinutos","descricao":"Tempo de validade em minutos","exemplo":"10"}]',
    true,
    'KeyRound'
);

-- 5. Email - Cobranca Aviso
INSERT INTO admin.template_bellory (codigo, nome, descricao, tipo, categoria, assunto, conteudo, variaveis_disponiveis, padrao, icone)
VALUES (
    'email-cobranca-aviso',
    'Aviso de Nova Cobranca',
    'Email de notificacao sobre nova cobranca gerada',
    'EMAIL',
    'COBRANCA_AVISO',
    'Nova Cobranca - Bellory',
    'cobranca-aviso',
    '[{"nome":"nomeCliente","descricao":"Nome do cliente","exemplo":"Joao Silva"},{"nome":"nomeOrganizacao","descricao":"Nome da organizacao","exemplo":"Barbearia Top"},{"nome":"valorCobranca","descricao":"Valor da cobranca","exemplo":"R$ 150,00"},{"nome":"dataVencimento","descricao":"Data de vencimento","exemplo":"15/04/2026"},{"nome":"descricaoCobranca","descricao":"Descricao da cobranca","exemplo":"Plano Premium - Mensal"},{"nome":"numeroCobranca","descricao":"Numero da cobranca","exemplo":"COB-2026-001"}]',
    true,
    'Receipt'
);

-- 6. Email - Cobranca Lembrete
INSERT INTO admin.template_bellory (codigo, nome, descricao, tipo, categoria, assunto, conteudo, variaveis_disponiveis, padrao, icone)
VALUES (
    'email-cobranca-lembrete',
    'Lembrete de Cobranca Pendente',
    'Email de lembrete sobre cobranca pendente ou vencida',
    'EMAIL',
    'COBRANCA_LEMBRETE',
    'Lembrete de Cobranca - Bellory',
    'cobranca-lembrete',
    '[{"nome":"nomeCliente","descricao":"Nome do cliente","exemplo":"Joao Silva"},{"nome":"nomeOrganizacao","descricao":"Nome da organizacao","exemplo":"Barbearia Top"},{"nome":"valorCobranca","descricao":"Valor da cobranca","exemplo":"R$ 150,00"},{"nome":"dataVencimento","descricao":"Data de vencimento","exemplo":"15/04/2026"},{"nome":"descricaoCobranca","descricao":"Descricao da cobranca","exemplo":"Plano Premium - Mensal"},{"nome":"numeroCobranca","descricao":"Numero da cobranca","exemplo":"COB-2026-001"},{"nome":"diasAtraso","descricao":"Dias em atraso","exemplo":"5"}]',
    true,
    'AlertTriangle'
);
