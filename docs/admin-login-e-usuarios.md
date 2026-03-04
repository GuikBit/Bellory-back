
# Documentacao: Login Admin Separado + Modulo de Usuarios Admin

## Indice

1. [Visao Geral](#1-visao-geral)
2. [Mudanca 1 - Login Separado para o Painel Admin](#2-mudanca-1---login-separado-para-o-painel-admin)
3. [Mudanca 2 - Modulo CRUD de Usuarios Admin](#3-mudanca-2---modulo-crud-de-usuarios-admin)
4. [DTOs de Referencia](#4-dtos-de-referencia)
5. [Codigos de Erro](#5-codigos-de-erro)
6. [Usuario Seed (Inicial)](#6-usuario-seed-inicial)
7. [Regras de Isolamento](#7-regras-de-isolamento)

---

## 1. Visao Geral

Foram implementadas duas funcionalidades complementares:

| # | Funcionalidade | Descricao |
|---|----------------|-----------|
| 1 | **Login apartado** | Os usuarios da equipe Bellory (administradores da plataforma) agora possuem um endpoint de login exclusivo (`/api/v1/admin/auth/**`), separado do login dos usuarios do app (`/api/v1/auth/**`) |
| 2 | **CRUD de usuarios admin** | Novo modulo para gerenciar os usuarios que acessam o painel administrativo da plataforma (`/api/v1/admin/usuarios/**`) |

### Como funciona o isolamento

- O JWT agora carrega um claim `userType` que vale `"APP"` (usuarios do app) ou `"PLATFORM_ADMIN"` (equipe Bellory)
- O filtro de autenticacao (`JwtAuthFilter`) bifurca o fluxo com base nesse claim
- Usuarios do app **nao conseguem** logar no endpoint admin e vice-versa
- Tokens antigos (sem o claim `userType`) sao tratados como `APP` automaticamente, garantindo retrocompatibilidade

---

## 2. Mudanca 1 - Login Separado para o Painel Admin

**Base URL:** `/api/v1/admin/auth`

### 2.1 POST `/api/v1/admin/auth/login`

Realiza o login de um usuario admin da plataforma.

**Autenticacao:** Publica (nao requer token)

**Request Body:**

```json
{
  "username": "admin",
  "password": "admin123"
}
```

| Campo | Tipo | Obrigatorio | Validacao |
|-------|------|:-----------:|-----------|
| `username` | string | Sim | Min 3, Max 50 caracteres |
| `password` | string | Sim | Min 6 caracteres |

**Response 200 (Sucesso):**

```json
{
  "success": true,
  "message": "Login realizado com sucesso",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "nomeCompleto": "Administrador Bellory",
    "email": "admin@bellory.com.br",
    "role": "ROLE_PLATFORM_ADMIN",
    "ativo": true,
    "dtCriacao": "01/03/2026 10:00:00"
  },
  "expiresAt": "01/03/2026 20:00:00"
}
```

**Response 401 (Credenciais invalidas):**

```json
{
  "success": false,
  "message": "Credenciais inválidas",
  "errorCode": "INVALID_CREDENTIALS",
  "timestamp": "01/03/2026 10:00:00",
  "path": "/api/v1/admin/auth/login"
}
```

**Response 403 (Conta desativada):**

```json
{
  "success": false,
  "message": "Conta desativada",
  "errorCode": "ACCOUNT_DISABLED",
  "timestamp": "01/03/2026 10:00:00",
  "path": "/api/v1/admin/auth/login"
}
```

---

### 2.2 POST `/api/v1/admin/auth/validate`

Valida se um token JWT e valido e pertence a um admin da plataforma.

**Autenticacao:** Publica (token enviado no header)

**Request Header:**

```
Authorization: Bearer <token>
```

**Response 200 (Token valido):**

```json
{
  "valid": true,
  "username": "admin",
  "userId": 1,
  "userType": "PLATFORM_ADMIN",
  "roles": ["ROLE_PLATFORM_ADMIN"],
  "expiresAt": "01/03/2026 20:00:00"
}
```

**Erros possiveis:**

| HTTP | errorCode | Descricao |
|------|-----------|-----------|
| 400 | `MISSING_TOKEN` | Header Authorization ausente ou formato invalido |
| 401 | `TOKEN_EXPIRED` | Token expirado |
| 401 | `INVALID_TOKEN_TYPE` | Token nao e de admin (e de usuario do app) |
| 401 | `INVALID_TOKEN` | Token invalido |

---

### 2.3 POST `/api/v1/admin/auth/refresh`

Renova um token admin antes ou apos expirar.

**Autenticacao:** Publica

**Request Body:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| Campo | Tipo | Obrigatorio | Validacao |
|-------|------|:-----------:|-----------|
| `token` | string | Sim | Nao pode ser vazio |

**Response 200 (Sucesso):**

```json
{
  "success": true,
  "message": "Token renovado com sucesso",
  "newToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "01/03/2026 20:00:00"
}
```

**Erros possiveis:**

| HTTP | errorCode | Descricao |
|------|-----------|-----------|
| 401 | `INVALID_TOKEN_TYPE` | Token nao e de admin |
| 401 | `REFRESH_FAILED` | Falha ao renovar (token invalido/corrompido) |

---

### 2.4 GET `/api/v1/admin/auth/me`

Retorna os dados do admin logado.

**Autenticacao:** Requer token admin no header

**Request Header:**

```
Authorization: Bearer <token>
```

**Response 200 (Sucesso):**

```json
{
  "id": 1,
  "username": "admin",
  "nomeCompleto": "Administrador Bellory",
  "email": "admin@bellory.com.br",
  "role": "ROLE_PLATFORM_ADMIN",
  "ativo": true,
  "dtCriacao": "01/03/2026 10:00:00"
}
```

**Erros possiveis:**

| HTTP | errorCode | Descricao |
|------|-----------|-----------|
| 400 | `MISSING_TOKEN` | Header Authorization ausente |
| 401 | `INVALID_TOKEN` | Token invalido |
| 500 | `USER_INFO_ERROR` | Erro interno ao buscar dados |

---

## 3. Mudanca 2 - Modulo CRUD de Usuarios Admin

**Base URL:** `/api/v1/admin/usuarios`

**Autenticacao:** Todos os endpoints requerem token com role `PLATFORM_ADMIN`, `SUPERADMIN` ou `ADMIN`.

---

### 3.1 GET `/api/v1/admin/usuarios`

Lista todos os usuarios admin da plataforma.

**Response 200:**

```json
[
  {
    "id": 1,
    "username": "admin",
    "nomeCompleto": "Administrador Bellory",
    "email": "admin@bellory.com.br",
    "role": "ROLE_PLATFORM_ADMIN",
    "ativo": true,
    "dtCriacao": "01/03/2026 10:00:00"
  },
  {
    "id": 2,
    "username": "joao.suporte",
    "nomeCompleto": "Joao da Silva",
    "email": "joao@bellory.com.br",
    "role": "ROLE_PLATFORM_ADMIN",
    "ativo": true,
    "dtCriacao": "05/03/2026 14:30:00"
  }
]
```

---

### 3.2 GET `/api/v1/admin/usuarios/{id}`

Busca um usuario admin pelo ID.

**Path Parameter:**

| Parametro | Tipo | Descricao |
|-----------|------|-----------|
| `id` | Long | ID do usuario admin |

**Response 200:**

```json
{
  "id": 1,
  "username": "admin",
  "nomeCompleto": "Administrador Bellory",
  "email": "admin@bellory.com.br",
  "role": "ROLE_PLATFORM_ADMIN",
  "ativo": true,
  "dtCriacao": "01/03/2026 10:00:00"
}
```

**Response 404 (Nao encontrado):**

```json
{
  "success": false,
  "message": "Usuário admin não encontrado"
}
```

---

### 3.3 POST `/api/v1/admin/usuarios`

Cria um novo usuario admin.

**Request Body:**

```json
{
  "username": "maria.suporte",
  "nomeCompleto": "Maria Oliveira",
  "password": "senha123",
  "email": "maria@bellory.com.br"
}
```

| Campo | Tipo | Obrigatorio | Validacao |
|-------|------|:-----------:|-----------|
| `username` | string | Sim | Min 3, Max 50 caracteres |
| `nomeCompleto` | string | Sim | Nao pode ser vazio |
| `password` | string | Sim | Min 6 caracteres |
| `email` | string | Sim | Formato email valido |

**Response 201 (Criado):**

```json
{
  "id": 3,
  "username": "maria.suporte",
  "nomeCompleto": "Maria Oliveira",
  "email": "maria@bellory.com.br",
  "role": "ROLE_PLATFORM_ADMIN",
  "ativo": true,
  "dtCriacao": "10/03/2026 09:00:00"
}
```

**Response 400 (Erro de validacao):**

```json
{
  "success": false,
  "message": "Username já existe"
}
```

```json
{
  "success": false,
  "message": "Email já existe"
}
```

---

### 3.4 PUT `/api/v1/admin/usuarios/{id}`

Atualiza um usuario admin existente. Apenas os campos enviados serao atualizados (partial update).

**Path Parameter:**

| Parametro | Tipo | Descricao |
|-----------|------|-----------|
| `id` | Long | ID do usuario admin |

**Request Body (todos os campos sao opcionais):**

```json
{
  "username": "maria.admin",
  "nomeCompleto": "Maria Oliveira Santos",
  "password": "novaSenha456",
  "email": "maria.santos@bellory.com.br"
}
```

| Campo | Tipo | Obrigatorio | Validacao |
|-------|------|:-----------:|-----------|
| `username` | string | Nao | Min 3, Max 50 caracteres |
| `nomeCompleto` | string | Nao | - |
| `password` | string | Nao | Min 6 caracteres |
| `email` | string | Nao | Formato email valido |

**Response 200 (Atualizado):**

```json
{
  "id": 3,
  "username": "maria.admin",
  "nomeCompleto": "Maria Oliveira Santos",
  "email": "maria.santos@bellory.com.br",
  "role": "ROLE_PLATFORM_ADMIN",
  "ativo": true,
  "dtCriacao": "10/03/2026 09:00:00"
}
```

**Response 400 (Erro):**

```json
{
  "success": false,
  "message": "Username já existe"
}
```

---

### 3.5 DELETE `/api/v1/admin/usuarios/{id}`

Desativa um usuario admin (soft delete - marca como `ativo = false`).

**Path Parameter:**

| Parametro | Tipo | Descricao |
|-----------|------|-----------|
| `id` | Long | ID do usuario admin |

**Response 200 (Desativado):**

```json
{
  "success": true,
  "message": "Usuário desativado com sucesso"
}
```

**Response 404 (Nao encontrado):**

```json
{
  "success": false,
  "message": "Usuário admin não encontrado"
}
```

---

## 4. DTOs de Referencia

### AdminLoginResponseDTO

| Campo | Tipo | Formato | Descricao |
|-------|------|---------|-----------|
| `success` | boolean | - | Se o login foi bem sucedido |
| `message` | string | - | Mensagem descritiva |
| `token` | string | JWT | Token de acesso |
| `user` | AdminUserInfoDTO | objeto | Dados do usuario admin |
| `expiresAt` | string | `dd/MM/yyyy HH:mm:ss` | Data de expiracao do token |

### AdminUserInfoDTO

| Campo | Tipo | Formato | Descricao |
|-------|------|---------|-----------|
| `id` | number | - | ID do usuario |
| `username` | string | - | Username unico |
| `nomeCompleto` | string | - | Nome completo |
| `email` | string | - | Email unico |
| `role` | string | - | Sempre `ROLE_PLATFORM_ADMIN` |
| `ativo` | boolean | - | Se o usuario esta ativo |
| `dtCriacao` | string | `dd/MM/yyyy HH:mm:ss` | Data de criacao |

### Estrutura do Token JWT (Admin)

| Claim | Tipo | Valor |
|-------|------|-------|
| `sub` | string | Username do admin |
| `userId` | number | ID do admin |
| `role` | string | `ROLE_PLATFORM_ADMIN` |
| `nomeCompleto` | string | Nome completo |
| `userType` | string | `PLATFORM_ADMIN` |
| `iss` | string | `bellory-api` |
| `exp` | number | Timestamp de expiracao |

> **Nota:** O token do admin **nao possui** o claim `organizacaoId` (diferente do token do app).

---

## 5. Codigos de Erro

| errorCode | HTTP | Descricao |
|-----------|------|-----------|
| `INVALID_CREDENTIALS` | 401 | Username ou senha incorretos |
| `ACCOUNT_DISABLED` | 403 | Conta do admin esta desativada |
| `MISSING_TOKEN` | 400 | Header Authorization nao enviado ou formato incorreto |
| `TOKEN_EXPIRED` | 401 | Token JWT expirado |
| `INVALID_TOKEN` | 401 | Token JWT invalido ou corrompido |
| `INVALID_TOKEN_TYPE` | 401 | Token nao e de admin (tentou usar token do app) |
| `REFRESH_FAILED` | 401 | Nao foi possivel renovar o token |
| `VALIDATION_ERROR` | 500 | Erro interno na validacao |
| `USER_INFO_ERROR` | 500 | Erro interno ao buscar dados do usuario |

---

## 6. Usuario Seed (Inicial)

A migration V33 cria automaticamente o primeiro usuario admin:

| Campo | Valor |
|-------|-------|
| Username | `admin` |
| Senha | `admin123` |
| Nome | Administrador Bellory |
| Email | admin@bellory.com.br |
| Role | ROLE_PLATFORM_ADMIN |

> **IMPORTANTE:** Trocar a senha do usuario seed em producao!

---

## 7. Regras de Isolamento

### O que muda no login do app (`/api/v1/auth/login`)?

- Os tokens gerados agora incluem o claim `userType: "APP"`
- Tokens antigos (sem `userType`) continuam funcionando normalmente
- **Nenhuma mudanca e necessaria no frontend do app**

### O que garante a separacao?

| Cenario | Resultado |
|---------|-----------|
| Admin loga em `/api/v1/admin/auth/login` | Sucesso - token com `userType: PLATFORM_ADMIN` |
| Admin tenta logar em `/api/v1/auth/login` (app) | Falha - nao encontra o usuario nas tabelas do app |
| Usuario do app tenta logar em `/api/v1/admin/auth/login` | Falha - nao encontra o usuario na tabela admin |
| Token admin usado em endpoint do app (ex: `/api/v1/cliente/**`) | Falha - sem `organizacaoId`, endpoints tenant-scoped nao funcionam |
| Token do app usado em `/api/v1/admin/auth/validate` | Retorna erro `INVALID_TOKEN_TYPE` |
| Token do app usado em endpoints `/api/v1/admin/**` | Funciona apenas se tiver role `SUPERADMIN` ou `ADMIN` |

### Fluxo de autenticacao (JwtAuthFilter)

```
Requisicao chega
  |
  v
Tem header X-API-Key?
  |-- Sim --> Fluxo API Key (sem mudanca)
  |-- Nao --> Tem header Authorization Bearer?
                |-- Nao --> Segue sem autenticacao
                |-- Sim --> Valida token
                              |
                              v
                    Extrai claim "userType"
                      |
                      |-- "PLATFORM_ADMIN"
                      |     |-> Busca em admin.usuario_admin
                      |     |-> TenantContext sem organizacaoId
                      |     |-> Authorities: ROLE_PLATFORM_ADMIN
                      |
                      |-- "APP" ou ausente
                            |-> Busca em app.admin / app.funcionario / app.cliente
                            |-> TenantContext com organizacaoId
                            |-> Authorities: role do usuario
```
