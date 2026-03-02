-- Tabela de usuarios administradores da plataforma Bellory (global, sem organizacao)
CREATE TABLE admin.usuario_admin (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    nome_completo   VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    ativo           BOOLEAN NOT NULL DEFAULT true,
    role            VARCHAR(50) NOT NULL DEFAULT 'ROLE_PLATFORM_ADMIN',
    dt_criacao      TIMESTAMP NOT NULL DEFAULT NOW(),
    dt_atualizacao  TIMESTAMP
);

-- Seed: usuario admin inicial (senha: admin123 - TROCAR EM PRODUCAO!)
INSERT INTO admin.usuario_admin (username, nome_completo, password, email, role)
VALUES (
    'admin',
    'Administrador Bellory',
    '$2a$10$fK8AZ39JpojT8UgiPOZdBuJAmDuf3DMfxxrUzsO3QXu/axq9PvS/q',
    'admin@bellory.com.br',
    'ROLE_PLATFORM_ADMIN'
);
