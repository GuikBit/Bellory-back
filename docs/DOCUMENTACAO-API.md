# DOCUMENTAÇÃO COMPLETA - BELLORY API

> **Versão:** 1.0 | **Data:** 17/02/2026 | **Framework:** Spring Boot 3.3.1 / Java 21

---

## Índice

1. [Visão Geral do Sistema](#1-visão-geral-do-sistema)
2. [Arquitetura e Segurança](#2-arquitetura-e-segurança)
3. [Endpoints da API](#3-endpoints-da-api)
4. [Regras de Negócio](#4-regras-de-negócio)
5. [Modelo de Dados](#5-modelo-de-dados)
6. [Migrações do Banco de Dados](#6-migrações-do-banco-de-dados)
7. [Integrações Externas](#7-integrações-externas)
8. [Tarefas Agendadas](#8-tarefas-agendadas)
9. [Configuração](#9-configuração)

---

## 1. Visão Geral do Sistema

O Bellory é uma plataforma SaaS multi-tenant para gestão de salões de beleza e negócios de serviços. A API REST gerencia agendamentos, clientes, funcionários, serviços, produtos, cobranças, pagamentos, notificações via WhatsApp e muito mais.

### Stack Tecnológico

| Componente | Tecnologia |
|---|---|
| Framework | Spring Boot 3.3.1 |
| Linguagem | Java 21 |
| Banco de Dados | PostgreSQL (Flyway migrations) |
| Schemas | `app`, `auto`, `site`, `admin` |
| Autenticação | JWT (HMAC256) + API Keys |
| WhatsApp | Evolution API (`wa.bellory.com.br`) |
| Automação | n8n (`auto.bellory.com.br`) |
| Email | Gmail SMTP |
| Documentação | OpenAPI 3.0 / Swagger |

### Estatísticas do Projeto

| Métrica | Quantidade |
|---|---|
| Controllers | 33 |
| Endpoints | 435+ |
| Services | 54+ |
| Entidades | 68+ |
| Repositórios | 50+ |
| DTOs | 100+ |
| Enums | 25+ |
| Migrations | 21 |

---

## 2. Arquitetura e Segurança

### 2.1 Autenticação JWT

O sistema utiliza JWT com algoritmo **HMAC256**.

**Claims do Token:**

| Claim | Tipo | Descrição |
|---|---|---|
| `sub` | String | Username |
| `userId` | Long | ID do usuário |
| `organizacaoId` | Long | ID da organização (multi-tenancy) |
| `role` | String | Papel do usuário |
| `nomeCompleto` | String | Nome completo |
| `exp` | Timestamp | Expiração (padrão: 10 horas) |
| `iss` | String | Issuer: "bellory-api" |

### 2.2 Autenticação por API Key

- **Prefixo:** `bly_`
- **Geração:** 32 bytes aleatórios em Base64 URL
- **Armazenamento:** Hash SHA-256 (chave nunca armazenada em texto)
- **Header:** `X-API-Key`
- **Prioridade:** API Key é verificada **antes** do JWT

**Tipos de Usuário para API Key:** ADMIN, FUNCIONARIO, CLIENTE, SISTEMA

### 2.3 Multi-tenancy

Cada request possui um `TenantContext` que isola os dados por organização:

```
JWT Token → Extract organizacaoId → TenantContext.setContext() → Business Logic → TenantContext.clear()
```

### 2.4 Hierarquia de Papéis

```
ROLE_SUPERADMIN > ROLE_ADMIN > ROLE_FUNCIONARIO > ROLE_CLIENTE
```

### 2.5 Prioridade de Busca de Usuários

1. **Admin** (maior prioridade)
2. **Funcionário**
3. **Cliente** (menor prioridade)

### 2.6 CORS

**Origens Permitidas:**
- `https://bellory.com.br` e subdomínios (`*.bellory.com.br`)
- `https://bellory.vercel.app` e subdomínios
- `http://localhost:*` e `https://localhost:*`

**Métodos:** GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH

### 2.7 Endpoints Públicos (sem autenticação)

```
/api/auth/**                           Autenticação
/api/test/**                           Teste
/api/servico/**                        Catálogo de serviços
/api/funcionario/**                    Info de funcionários
/api/agendamento/**                    Agendamentos
/api/produto/**                        Catálogo de produtos
/api/cliente/**                        Operações de cliente
/api/dashboard/**                      Dashboard
/api/public/site/**                    Site público
/api/tracking                          Tracking anônimo
/api/instances/by-name/**              Busca instância por nome
/api/organizacao                       Criação de organização
/api/organizacao/verificar-cnpj/**     Verificação CNPJ
/api/organizacao/verificar-email/**    Verificação email
/api/organizacao/verificar-username/** Verificação username
/api/email/teste                       Teste de email
/api/pages/**                          Páginas multi-tenant
/v3/api-docs/**                        Documentação OpenAPI
/swagger-ui/**                         Swagger UI
/actuator/**                           Spring Actuator
```

### 2.8 Endpoints Protegidos

```
/admin/**              → Requer ROLE_ADMIN
/api/admin/**          → Requer ROLE_SUPERADMIN ou ROLE_ADMIN
Demais endpoints       → Requer autenticação (JWT ou API Key)
```

### 2.9 Rate Limiting

- Endpoint `/api/tracking`: 60 requests/minuto por IP
- Reset automático a cada 1 minuto

### 2.10 Senhas

- Codificação: **BCryptPasswordEncoder**
- Mínimo 6 caracteres

---

## 3. Endpoints da API

> Todos os endpoints retornam `ResponseAPI<T>` como wrapper genérico.
> Status HTTP padrão: 200 (OK), 201 (CREATED), 400 (BAD_REQUEST), 404 (NOT_FOUND), 409 (CONFLICT), 500 (INTERNAL_SERVER_ERROR)

---

### 3.1 Autenticação (`/api/auth`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/login` | Login do usuário | `LoginRequestDTO` (body), `withStats` (query), `HttpServletRequest` |
| 2 | POST | `/validate` | Validar token | `Authorization` (header) |
| 3 | POST | `/refresh` | Renovar token | `RefreshTokenRequestDTO` (body) |
| 4 | POST | `/logout` | Logout | `HttpServletRequest` |
| 5 | GET | `/me` | Dados do usuário logado | `Authorization` (header), `withStats` (query) |

---

### 3.2 Agendamentos (`/api/agendamento`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/disponibilidade` | Consultar disponibilidade | `DisponibilidadeRequest` (body) |
| 2 | POST | `/` | Criar agendamento | `AgendamentoCreateDTO` (body) |
| 3 | GET | `/` | Listar agendamentos | - |
| 4 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 5 | PUT | `/{id}` | Atualizar agendamento | `id` (path), `AgendamentoUpdateDTO` (body) |
| 6 | GET | `/cobrancas-pendentes` | Agendamentos com cobranças pendentes | - |
| 7 | GET | `/vencidos` | Agendamentos vencidos | - |
| 8 | POST | `/{id}/pagamento` | Processar pagamento | `id` (path), `PagamentoAgendamentoDTO` (body) |
| 9 | POST | `/{id}/dividir-pagamento` | Dividir pagamento | `id` (path), `DividirPagamentoDTO` (body) |
| 10 | DELETE | `/{id}` | Cancelar agendamento | `id` (path) |
| 11 | POST | `/{id}/estorno` | Estornar pagamento | `id` (path), `Map { motivo }` (body) |
| 12 | GET | `/status-disponiveis` | Listar status disponíveis | - |
| 13 | PATCH | `/{id}/status/{status}` | Alterar status | `id`, `status` (path) |
| 14 | GET | `/cliente/{clienteId}` | Agendamentos por cliente | `clienteId` (path) |
| 15 | GET | `/funcionario/{funcionarioId}` | Agendamentos por funcionário | `funcionarioId` (path) |
| 16 | GET | `/data/{data}` | Agendamentos por data | `data` (path) |
| 17 | GET | `/status/{status}` | Agendamentos por status | `status` (path) |
| 18 | PATCH | `/{id}/reagendar` | Reagendar | `id` (path), `Map { novaDataHora }` (body) |
| 19 | GET | `/estatisticas` | Estatísticas de agendamentos | - |
| 20 | GET | `/hoje` | Agendamentos de hoje | - |
| 21 | GET | `/proximos` | Próximos agendamentos | - |
| 22 | POST | `/filtrar` | Filtrar agendamentos | `AgendamentoFiltroDTO` (body) |
| 23 | GET | `/funcionario/{id}/agenda/{data}` | Agenda do dia do funcionário | `funcionarioId`, `data` (path) |
| 24 | POST | `/consultar-relacionamentos` | Consultar relacionamentos | `ConsultaRelacionamentoRequest` (body) |
| 25 | GET | `/funcionarios-por-servicos` | Funcionários por serviços | `servicoIds` (query, List) |
| 26 | GET | `/servicos-por-funcionarios` | Serviços por funcionários | `funcionarioIds` (query, List) |

---

### 3.3 Clientes (`/api/cliente`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/` | Listar clientes | `sortBy`, `sortDir`, `nome`, `email`, `telefone`, `ativo` (query) |
| 2 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 3 | POST | `/` | Criar cliente | `ClienteCreateDTO` (body) |
| 4 | GET | `/validar-username` | Validar username | `username` (query) |
| 5 | POST | `/verificar-cpf` | Verificar CPF existente | `RequestCpfDTO` (body) |
| 6 | PUT | `/{id}` | Atualizar cliente | `id` (path), `ClienteUpdateDTO` (body) |
| 7 | DELETE | `/{id}` | Excluir cliente | `id` (path) |
| 8 | GET | `/{id}/agendamentos` | Agendamentos do cliente | `id` (path), `status`, `dataInicio`, `dataFim` (query) |
| 9 | GET | `/{id}/agendamentos/proximos` | Próximos agendamentos | `id` (path) |
| 10 | GET | `/{id}/compras` | Compras do cliente | `id` (path), `status`, `dataInicio`, `dataFim` (query) |
| 11 | GET | `/{id}/cobrancas` | Cobranças do cliente | `id` (path), `status` (query) |
| 12 | GET | `/{id}/pagamentos` | Pagamentos do cliente | `id` (path), `metodo` (query) |
| 13 | GET | `/{id}/historico` | Histórico do cliente | `id` (path), `tipo`, `dataInicio`, `dataFim` (query) |
| 14 | GET | `/{id}/resumo-financeiro` | Resumo financeiro | `id` (path) |
| 15 | GET | `/{id}/servicos-favoritos` | Serviços favoritos | `id` (path) |
| 16 | GET | `/buscar` | Buscar clientes | `termo` (query) |
| 17 | GET | `/aniversariantes` | Aniversariantes | `mes`, `ano` (query) |
| 18 | GET | `/top-clientes` | Top clientes | `limite` (query, default=10) |
| 19 | PATCH | `/{id}/status` | Alterar status | `id` (path), `Map { ativo }` (body) |
| 20 | GET | `/estatisticas` | Estatísticas de clientes | - |

---

### 3.4 Funcionários (`/api/funcionario`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/` | Listar funcionários | - |
| 2 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 3 | GET | `/agendamento` | Lista para agendamento | - |
| 4 | POST | `/` | Criar funcionário | `FuncionarioCreateDTO` (body) |
| 5 | PUT | `/{id}` | Atualizar funcionário | `id` (path), `FuncionarioUpdateDTO` (body) |
| 6 | DELETE | `/{id}` | Excluir funcionário | `id` (path) |
| 7 | GET | `/validar-username` | Validar username | `username` (query) |
| 8 | POST | `/cargo` | Criar cargo | `CargoDTO` (body) |
| 9 | GET | `/cargo` | Listar cargos | - |
| 10 | GET | `/cargo/{id}` | Buscar cargo por ID | `id` (path) |
| 11 | PUT | `/cargo/{id}` | Atualizar cargo | `id` (path), `CargoDTO` (body) |
| 12 | DELETE | `/cargo/{id}` | Excluir cargo | `id` (path) |
| 13 | POST | `/{id}/foto-perfil` | Upload de foto | `id` (path), `MultipartFile` |
| 14 | GET | `/{id}/foto-perfil` | Download da foto | `id` (path) |
| 15 | DELETE | `/{id}/foto-perfil` | Excluir foto | `id` (path) |

---

### 3.5 Serviços (`/api/servico`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/` | Listar serviços | - |
| 2 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 3 | GET | `/agendamento` | Lista para agendamento | - |
| 4 | POST | `/` | Criar serviço | `ServicoCreateDTO` (body) |
| 5 | PUT | `/{id}` | Atualizar serviço | `id` (path), `ServicoCreateDTO` (body) |
| 6 | DELETE | `/{id}` | Excluir serviço | `id` (path) |
| 7 | GET | `/categorias` | Listar categorias | - |
| 8 | POST | `/categoria` | Criar categoria | `Categoria` (body) |
| 9 | PUT | `/categoria/{id}` | Atualizar categoria | `id` (path), `Categoria` (body) |
| 10 | DELETE | `/categoria/{id}` | Excluir categoria | `id` (path) |

---

### 3.6 Produtos (`/api/produto`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Criar produto | `ProdutoCreateDTO` (body) |
| 2 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 3 | GET | `/` | Listar produtos | - |
| 4 | PUT | `/{id}` | Atualizar produto | `id` (path), `ProdutoUpdateDTO` (body) |
| 5 | DELETE | `/{id}` | Excluir produto | `id` (path) |
| 6 | GET | `/categoria/{categoriaId}` | Por categoria | `categoriaId` (path) |
| 7 | GET | `/destaque` | Produtos em destaque | - |
| 8 | GET | `/preco` | Por faixa de preço | `precoMinimo`, `precoMaximo` (query) |
| 9 | GET | `/pesquisar` | Pesquisar produtos | `termo`, `page`, `size` (query) |
| 10 | PATCH | `/{id}/estoque/adicionar` | Adicionar estoque | `id` (path), `quantidade` (query) |
| 11 | PATCH | `/{id}/estoque/remover` | Remover estoque | `id` (path), `quantidade` (query) |
| 12 | GET | `/estoque/baixo` | Estoque baixo | `limite` (query, default=10) |
| 13 | GET | `/estoque/zerado` | Sem estoque | - |
| 14 | POST | `/{id}/imagens` | Adicionar imagem | `id` (path), `Map { urlImagem }` (body) |
| 15 | DELETE | `/{id}/imagens` | Remover imagem | `id` (path), `urlImagem` (query) |
| 16 | PATCH | `/{id}/ativar` | Ativar produto | `id` (path) |
| 17 | PATCH | `/{id}/inativar` | Inativar produto | `id` (path) |
| 18 | PATCH | `/{id}/descontinuar` | Descontinuar produto | `id` (path) |
| 19 | PATCH | `/{id}/destaque` | Alternar destaque | `id` (path) |
| 20 | POST | `/{id}/relacionados/{relId}` | Adicionar relacionado | `id`, `relacionadoId` (path) |
| 21 | DELETE | `/{id}/relacionados/{relId}` | Remover relacionado | `id`, `relacionadoId` (path) |
| 22 | GET | `/{id}/relacionados` | Listar relacionados | `id` (path) |
| 23 | GET | `/dashboard` | Dashboard de produtos | - |
| 24 | GET | `/mais-vendidos` | Mais vendidos | `limite` (query, default=10) |
| 25 | GET | `/validar-codigo-barras` | Validar código de barras | `codigoBarras` (query) |
| 26 | GET | `/codigo-barras/{codigo}` | Buscar por código | `codigoBarras` (path) |
| 27 | POST | `/{id}/duplicar` | Duplicar produto | `id` (path) |

---

### 3.7 Organizações (`/api/organizacao`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Criar organização | `CreateOrganizacaoDTO` (body) |
| 2 | GET | `/` | Listar todas | - |
| 3 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 4 | PUT | `/{id}` | Atualizar | `id` (path), `UpdateOrganizacaoDTO` (body) |
| 5 | DELETE | `/{id}` | Excluir | `id` (path) |
| 6 | GET | `/cnpj/{cnpj}` | Buscar por CNPJ | `cnpj` (path) |
| 7 | PATCH | `/{id}/ativar` | Ativar organização | `id` (path) |
| 8 | GET | `/verificar-cnpj/{cnpj}` | Verificar CNPJ | `cnpj` (path) |
| 9 | GET | `/verificar-username/{username}` | Verificar username | `username` (path) |
| 10 | GET | `/verificar-email/{email}` | Verificar email | `email` (path) |
| 11 | GET | `/verificar-slug/{slug}` | Verificar slug | `slug` (path) |

---

### 3.8 Dashboard (`/api/dashboard`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/geral` | Dashboard geral | `DashboardFiltroDTO` (body) |
| 2 | GET | `/resumo-hoje` | Resumo de hoje | - |
| 3 | GET | `/resumo-mes` | Resumo do mês | - |
| 4 | GET | `/resumo-ano/{ano}` | Resumo do ano | `ano` (path) |
| 5 | GET | `/comparativo` | Comparativo entre períodos | `dataInicioAtual`, `dataFimAtual`, `dataInicioAnterior`, `dataFimAnterior` (query) |
| 6 | GET | `/metricas-funcionario/{id}` | Métricas de funcionário | `funcionarioId` (path), `dataInicio`, `dataFim` (query) |

---

### 3.9 Pagamentos (`/api/pagamentos`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Processar pagamento | `PagamentoCreateDTO` (body) |
| 2 | GET | `/cobranca/{cobrancaId}` | Buscar cobrança | `cobrancaId` (path) |
| 3 | GET | `/agendamento/{id}/cobrancas` | Cobranças por agendamento | `agendamentoId` (path) |
| 4 | DELETE | `/{cobrancaId}` | Cancelar cobrança | `cobrancaId` (path), `motivo` (query) |

---

### 3.10 Instâncias WhatsApp (`/api/instances`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Criar instância | `InstanceCreateDTO` (body) |
| 2 | GET | `/` | Listar instâncias | - |
| 3 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 4 | PUT | `/{id}` | Atualizar instância | `id` (path), `InstanceUpdateDTO` (body) |
| 5 | DELETE | `/{id}` | Excluir instância | `id` (path) |
| 6 | GET | `/{id}/qrcode` | Obter QR Code | `id` (path) |
| 7 | GET | `/{id}/status` | Status de conexão | `id` (path) |
| 8 | POST | `/{id}/logout` | Logout WhatsApp | `id` (path) |
| 9 | POST | `/{id}/restart` | Reiniciar instância | `id` (path) |
| 10 | POST | `/{id}/send-text` | Enviar mensagem | `id` (path), `SendTextMessageDTO` (body) |
| 11 | GET | `/by-name/{name}` | Buscar por nome | `instanceName` (path) |

---

### 3.11 Relatórios (`/api/relatorios`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/dashboard` | Dashboard executivo | `RelatorioFiltroDTO` (body) |
| 2 | POST | `/faturamento` | Relatório de faturamento | `RelatorioFiltroDTO` (body) |
| 3 | POST | `/agendamentos` | Relatório de agendamentos | `RelatorioFiltroDTO` (body) |
| 4 | POST | `/notificacoes` | Relatório de notificações | `RelatorioFiltroDTO` (body) |
| 5 | POST | `/cobrancas` | Relatório de cobranças | `RelatorioFiltroDTO` (body) |
| 6 | POST | `/funcionarios` | Relatório de funcionários | `RelatorioFiltroDTO` (body) |
| 7 | POST | `/clientes` | Relatório de clientes | `RelatorioFiltroDTO` (body) |
| 8 | POST | `/servicos` | Relatório de serviços | `RelatorioFiltroDTO` (body) |

---

### 3.12 Jornada de Trabalho (`/api/funcionario/{funcionarioId}/jornada`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/` | Listar jornadas | `funcionarioId` (path) |
| 2 | POST | `/` | Criar/atualizar jornada | `funcionarioId` (path), `JornadaDiaDTO` (body) |
| 3 | PATCH | `/{diaSemana}/status` | Alterar status do dia | `funcionarioId`, `diaSemana` (path), `ativo` (query) |
| 4 | DELETE | `/{diaSemana}/horario/{horarioId}` | Excluir horário | `funcionarioId`, `diaSemana`, `horarioId` (path) |
| 5 | DELETE | `/{diaSemana}` | Excluir jornada do dia | `funcionarioId`, `diaSemana` (path) |

---

### 3.13 Configurações do Sistema (`/api/configuracao`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/` | Buscar configuração | - |
| 2 | PUT | `/` | Atualizar configuração geral | `ConfigSistemaDTO` (body) |
| 3 | PUT | `/agendamento` | Config de agendamento | `ConfigAgendamento` (body) |
| 4 | PUT | `/servico` | Config de serviço | `ConfigServico` (body) |
| 5 | PUT | `/cliente` | Config de cliente | `ConfigCliente` (body) |
| 6 | PUT | `/colaborador` | Config de colaborador | `ConfigColaborador` (body) |
| 7 | PUT | `/notificacao` | Config de notificação | `ConfigNotificacao` (body) |

---

### 3.14 Questionários (`/api/questionarios`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Criar questionário | `QuestionarioCreateDTO` (body) |
| 2 | PUT | `/{id}` | Atualizar | `id` (path), `QuestionarioCreateDTO` (body) |
| 3 | DELETE | `/{id}` | Excluir | `id` (path) |
| 4 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 5 | GET | `/` | Listar (paginado) | `page`, `size`, `sort` (query) |
| 6 | GET | `/ativos` | Listar ativos | - |
| 7 | GET | `/tipo/{tipo}` | Listar por tipo | `tipo` (path) |
| 8 | GET | `/pesquisar` | Pesquisar | `termo` (query), `Pageable` |
| 9 | GET | `/{id}/total-respostas` | Total de respostas | `id` (path) |

---

### 3.15 Configuração de Notificações (`/api/config/notificacao`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/` | Listar ativas | - |
| 2 | GET | `/todas` | Listar todas | - |
| 3 | POST | `/` | Criar config | `ConfigNotificacaoDTO` (body) |
| 4 | POST | `/upsert` | Salvar ou atualizar | `ConfigNotificacaoDTO` (body) |
| 5 | PUT | `/{id}` | Atualizar | `id` (path), `ConfigNotificacaoDTO` (body) |
| 6 | DELETE | `/{id}` | Excluir | `id` (path) |
| 7 | PATCH | `/{id}/status` | Alterar status | `id` (path), `ativo` (query) |

---

### 3.16 Email (`/api/email`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/teste` | Enviar email de teste | `destinatario` (query) |

---

### 3.17 Bloqueios e Feriados (`/api/bloqueio-organizacao`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/` | Listar todos | - |
| 2 | GET | `/ativos` | Listar ativos | - |
| 3 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 4 | GET | `/periodo` | Listar por período | `dataInicio`, `dataFim` (query) |
| 5 | GET | `/verificar/{data}` | Verificar data bloqueada | `data` (path) |
| 6 | POST | `/` | Criar bloqueio | `BloqueioOrganizacaoCreateDTO` (body) |
| 7 | PUT | `/{id}` | Atualizar bloqueio | `id` (path), `BloqueioOrganizacaoUpdateDTO` (body) |
| 8 | PATCH | `/{id}/toggle` | Alternar ativo/inativo | `id` (path) |
| 9 | DELETE | `/{id}` | Remover bloqueio | `id` (path) |
| 10 | POST | `/importar-feriados` | Importar feriados nacionais | `ano` (query, opcional) |

---

### 3.18 API Keys (`/api/api-keys`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Criar API Key | `Map { name, description, expiresInDays }` (body) |
| 2 | GET | `/` | Listar API Keys | - |
| 3 | DELETE | `/{id}` | Revogar API Key | `id` (path) |

---

### 3.19 Site Público (`/api/public/site`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/{slug}/home` | Página inicial | `slug` (path) |
| 2 | GET | `/{slug}/header` | Header do site | `slug` (path) |
| 3 | GET | `/{slug}/hero` | Seção hero | `slug` (path) |
| 4 | GET | `/{slug}/about` | Sobre (resumo) | `slug` (path) |
| 5 | GET | `/{slug}/about/full` | Sobre (completo) | `slug` (path) |
| 6 | GET | `/{slug}/footer` | Footer do site | `slug` (path) |
| 7 | GET | `/{slug}/services/featured` | Serviços em destaque | `slug` (path) |
| 8 | GET | `/{slug}/services` | Todos os serviços | `slug` (path), `page`, `size` (query) |
| 9 | GET | `/{slug}/services/{id}` | Serviço por ID | `slug`, `id` (path) |
| 10 | GET | `/{slug}/products/featured` | Produtos em destaque | `slug` (path) |
| 11 | GET | `/{slug}/products` | Todos os produtos | `slug` (path), `page`, `size` (query) |
| 12 | GET | `/{slug}/products/{id}` | Produto por ID | `slug`, `id` (path) |
| 13 | GET | `/{slug}/team` | Equipe | `slug` (path) |
| 14 | GET | `/{slug}/booking` | Info de agendamento | `slug` (path) |
| 15 | GET | `/{slug}` | Site completo por slug | `slug` (path) |
| 16 | GET | `/{slug}/exists` | Verificar se slug existe | `slug` (path) |
| 17 | GET | `/{slug}/basic` | Informações básicas | `slug` (path) |

---

### 3.20 Páginas de Tenant (`/api/pages`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/{slug}` | Página por slug | `slug` (path) |
| 2 | GET | `/` | Listar todas | - |
| 3 | GET | `/tenant-info` | Info do tenant | - |

---

### 3.21 Webhook de Confirmação (`/api/webhook`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/confirmacao-pendente/{tel}` | Confirmação pendente por telefone | `telefone` (path), `X-Instance-Name` (header) |
| 2 | GET | `/confirmacao-aguardando-data/{tel}` | Aguardando data | `telefone` (path), `X-Instance-Name` (header) |
| 3 | GET | `/confirmacao-aguardando-horario/{tel}` | Aguardando horário | `telefone` (path), `X-Instance-Name` (header) |
| 4 | PATCH | `/confirmacao/{id}/aguardando-data` | Marcar aguardando data | `notificacaoId` (path) |
| 5 | PATCH | `/confirmacao/{id}/aguardando-horario` | Marcar aguardando horário | `notificacaoId` (path), `AtualizarStatusConfirmacaoRequest` (body) |
| 6 | PATCH | `/confirmacao/{id}/concluida` | Marcar concluída | `notificacaoId` (path) |
| 7 | POST | `/confirmacao/{id}/resposta` | Registrar resposta | `notificacaoId` (path), `Map { resposta }` (body) |

---

### 3.22 Landing Pages (`/api/landing-pages`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/` | Listar todas | - |
| 2 | GET | `/paginated` | Listar paginado | `page`, `size` (query) |
| 3 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 4 | GET | `/by-slug/{slug}` | Buscar por slug | `slug` (path) |
| 5 | POST | `/` | Criar landing page | `CreateLandingPageRequest` (body) |
| 6 | PUT | `/{id}` | Atualizar | `id` (path), `UpdateLandingPageRequest` (body) |
| 7 | POST | `/{id}/publish` | Publicar | `id` (path) |
| 8 | POST | `/{id}/unpublish` | Despublicar | `id` (path) |
| 9 | POST | `/{id}/duplicate` | Duplicar | `id` (path), `novoNome` (query) |
| 10 | DELETE | `/{id}` | Excluir | `id` (path) |
| 11 | POST | `/{id}/sections` | Adicionar seção | `landingPageId` (path), `AddSectionRequest` (body) |
| 12 | PUT | `/{id}/sections/{sId}` | Atualizar seção | `landingPageId`, `sectionId` (path), `UpdateSectionRequest` (body) |
| 13 | PUT | `/{id}/sections/reorder` | Reordenar seções | `landingPageId` (path), `ReorderSectionsRequest` (body) |
| 14 | POST | `/{id}/sections/{sId}/duplicate` | Duplicar seção | `landingPageId`, `sectionId` (path) |
| 15 | DELETE | `/{id}/sections/{sId}` | Excluir seção | `landingPageId`, `sectionId` (path) |
| 16 | GET | `/{id}/versions` | Listar versões | `landingPageId` (path) |
| 17 | POST | `/{id}/versions/{v}/restore` | Restaurar versão | `landingPageId`, `versao` (path) |
| 18 | GET | `/metadata/section-types` | Tipos de seção | - |
| 19 | GET | `/metadata/element-types` | Tipos de elemento | - |

---

### 3.23 Respostas de Questionários (`/api/questionarios/{questionarioId}/respostas`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Registrar resposta | `questionarioId` (path), `RespostaQuestionarioCreateDTO` (body) |
| 2 | GET | `/{respostaId}` | Buscar por ID | `questionarioId`, `respostaId` (path) |
| 3 | DELETE | `/{respostaId}` | Excluir resposta | `questionarioId`, `respostaId` (path) |
| 4 | GET | `/` | Listar (paginado) | `questionarioId` (path), `Pageable` |
| 5 | GET | `/periodo` | Listar por período | `questionarioId` (path), `inicio`, `fim` (query) |
| 6 | GET | `/verificar` | Verificar se já respondeu | `questionarioId` (path), `clienteId` (query) |
| 7 | GET | `/verificar-agendamento` | Verificar agendamento avaliado | `questionarioId` (path), `agendamentoId` (query) |
| 8 | GET | `/estatisticas` | Estatísticas | `questionarioId` (path) |
| 9 | GET | `/nps` | Calcular NPS | `questionarioId` (path), `perguntaId` (query) |
| 10 | GET | `/relatorio` | Gerar relatório | `questionarioId` (path), `inicio`, `fim` (query) |

---

### 3.24 Admin - Dashboard (`/api/admin/dashboard`)

| # | Método | Endpoint | Descrição |
|---|--------|----------|-----------|
| 1 | GET | `/` | Dashboard administrativo |

---

### 3.25 Admin - Métricas (`/api/admin/metricas`)

| # | Método | Endpoint | Descrição |
|---|--------|----------|-----------|
| 1 | GET | `/agendamentos` | Métricas de agendamentos |
| 2 | GET | `/faturamento` | Métricas de faturamento |
| 3 | GET | `/servicos` | Métricas de serviços |
| 4 | GET | `/funcionarios` | Métricas de funcionários |
| 5 | GET | `/clientes` | Métricas de clientes |
| 6 | GET | `/instancias` | Métricas de instâncias |
| 7 | GET | `/planos` | Métricas de planos |

---

### 3.26 Admin - Organizações (`/api/admin/organizacoes`)

| # | Método | Endpoint | Descrição |
|---|--------|----------|-----------|
| 1 | GET | `/` | Listar organizações |
| 2 | GET | `/{id}` | Detalhar organização |

---

### 3.27 Tracking (`/api/tracking`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Receber dados de tracking | `rawBody` (body), `HttpServletRequest` |

---

### 3.28 Admin - Analytics (`/api/admin/analytics`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | GET | `/overview` | Visão geral | `start_date`, `end_date` (query) |
| 2 | GET | `/traffic` | Tráfego | `start_date`, `end_date` (query) |
| 3 | GET | `/behavior` | Comportamento | `start_date`, `end_date` (query) |
| 4 | GET | `/conversions` | Conversões | `start_date`, `end_date` (query) |
| 5 | GET | `/context` | Contexto | `start_date`, `end_date` (query) |
| 6 | GET | `/realtime` | Tempo real | - |

---

### 3.29 Financeiro - Contas Bancárias e Centros de Custo (`/api/financeiro`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/contas-bancarias` | Criar conta bancária | `ContaBancariaCreateDTO` (body) |
| 2 | PUT | `/contas-bancarias/{id}` | Atualizar conta | `id` (path), `ContaBancariaCreateDTO` (body) |
| 3 | GET | `/contas-bancarias` | Listar contas | - |
| 4 | GET | `/contas-bancarias/{id}` | Buscar conta | `id` (path) |
| 5 | GET | `/contas-bancarias/saldo-total` | Saldo total | - |
| 6 | PATCH | `/contas-bancarias/{id}/desativar` | Desativar conta | `id` (path) |
| 7 | POST | `/centros-custo` | Criar centro de custo | `CentroCustoCreateDTO` (body) |
| 8 | PUT | `/centros-custo/{id}` | Atualizar centro | `id` (path), `CentroCustoCreateDTO` (body) |
| 9 | GET | `/centros-custo` | Listar centros | - |
| 10 | GET | `/centros-custo/{id}` | Buscar centro | `id` (path) |
| 11 | PATCH | `/centros-custo/{id}/desativar` | Desativar centro | `id` (path) |
| 12 | PATCH | `/centros-custo/{id}/ativar` | Ativar centro | `id` (path) |

---

### 3.30 Financeiro - Categorias (`/api/financeiro/categorias`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/` | Criar categoria | `CategoriaFinanceiraCreateDTO` (body) |
| 2 | PUT | `/{id}` | Atualizar | `id` (path), `CategoriaFinanceiraCreateDTO` (body) |
| 3 | GET | `/` | Listar categorias | `tipo` (query, opcional) |
| 4 | GET | `/arvore` | Listar em árvore | `tipo` (query, opcional) |
| 5 | GET | `/{id}` | Buscar por ID | `id` (path) |
| 6 | PATCH | `/{id}/desativar` | Desativar | `id` (path) |
| 7 | PATCH | `/{id}/ativar` | Ativar | `id` (path) |

---

### 3.31 Financeiro - Contas a Pagar/Receber (`/api/financeiro`)

**Contas a Pagar:**

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/contas-pagar` | Criar | `ContaPagarCreateDTO` (body) |
| 2 | PUT | `/contas-pagar/{id}` | Atualizar | `id` (path), `ContaPagarUpdateDTO` (body) |
| 3 | POST | `/contas-pagar/{id}/pagar` | Pagar | `id` (path), `PagamentoContaDTO` (body) |
| 4 | GET | `/contas-pagar` | Listar | `dataInicio`, `dataFim`, `status` (query) |
| 5 | GET | `/contas-pagar/{id}` | Buscar | `id` (path) |
| 6 | GET | `/contas-pagar/vencidas` | Vencidas | - |
| 7 | GET | `/contas-pagar/a-vencer` | A vencer | `dias` (query, default=7) |
| 8 | DELETE | `/contas-pagar/{id}` | Cancelar | `id` (path) |

**Contas a Receber:**

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 9 | POST | `/contas-receber` | Criar | `ContaReceberCreateDTO` (body) |
| 10 | PUT | `/contas-receber/{id}` | Atualizar | `id` (path), `ContaReceberUpdateDTO` (body) |
| 11 | POST | `/contas-receber/{id}/receber` | Receber | `id` (path), `PagamentoContaDTO` (body) |
| 12 | GET | `/contas-receber` | Listar | `dataInicio`, `dataFim`, `status`, `clienteId` (query) |
| 13 | GET | `/contas-receber/{id}` | Buscar | `id` (path) |
| 14 | GET | `/contas-receber/vencidas` | Vencidas | - |
| 15 | GET | `/contas-receber/a-vencer` | A vencer | `dias` (query, default=7) |
| 16 | GET | `/contas-receber/cliente/{id}/pendentes` | Pendentes por cliente | `clienteId` (path) |
| 17 | DELETE | `/contas-receber/{id}` | Cancelar | `id` (path) |

---

### 3.32 Financeiro - Lançamentos e Relatórios (`/api/financeiro`)

| # | Método | Endpoint | Descrição | Parâmetros |
|---|--------|----------|-----------|------------|
| 1 | POST | `/lancamentos` | Criar lançamento | `LancamentoFinanceiroCreateDTO` (body) |
| 2 | PUT | `/lancamentos/{id}` | Atualizar | `id` (path), `LancamentoFinanceiroUpdateDTO` (body) |
| 3 | GET | `/lancamentos` | Listar | `dataInicio`, `dataFim`, `tipo`, `categoriaFinanceiraId`, `centroCustoId`, `contaBancariaId` (query) |
| 4 | GET | `/lancamentos/{id}` | Buscar | `id` (path) |
| 5 | POST | `/lancamentos/{id}/efetivar` | Efetivar | `id` (path) |
| 6 | DELETE | `/lancamentos/{id}` | Cancelar | `id` (path) |
| 7 | GET | `/dashboard` | Dashboard financeiro | - |
| 8 | GET | `/relatorios/fluxo-caixa` | Fluxo de caixa | `dataInicio`, `dataFim` (query) |
| 9 | GET | `/relatorios/dre` | DRE | `dataInicio`, `dataFim` (query) |
| 10 | GET | `/relatorios/balanco` | Balanço | `dataReferencia` (query) |

---

## 4. Regras de Negócio

### 4.1 Agendamentos

#### Máquina de Estados

```
PENDENTE → [AGENDADO, CANCELADO]
AGENDADO → [AGUARDANDO_CONFIRMACAO, REAGENDADO, CANCELADO]
AGUARDANDO_CONFIRMACAO → [CONFIRMADO, REAGENDADO, CANCELADO, NAO_COMPARECEU]
CONFIRMADO → [EM_ESPERA, REAGENDADO, CANCELADO, NAO_COMPARECEU]
EM_ESPERA → [EM_ANDAMENTO, CANCELADO]
EM_ANDAMENTO → [CONCLUIDO, CANCELADO]
REAGENDADO → [AGENDADO]
CONCLUIDO → [Estado Final]
CANCELADO → [Estado Final]
NAO_COMPARECEU → [Estado Final]
```

#### Validação de Criação (3 Níveis)

**Nível 1 - Bloqueio da Organização:**
- Verifica `BloqueioOrganizacao` ativo para a data
- Rejeita se a data é feriado ou bloqueada

**Nível 2 - Jornada do Funcionário:**
- Funcionário deve ter `JornadaDia` ativa para o dia da semana
- Horário deve estar dentro dos períodos de trabalho definidos
- Suporta múltiplos períodos por dia (ex: 09:00-12:00, 14:00-18:00)

**Nível 3 - Detecção de Conflitos:**
- Verifica `BloqueioAgenda` existentes
- Calcula sobreposição: `inicioAgendamento < fimBloqueio && fimAgendamento > inicioBloqueio`

#### Cálculo de Duração

```
duracaoTotal = soma(duracao de cada servico) + 15 minutos (buffer)
```

O buffer de 15 minutos é sempre adicionado para transição entre clientes.

#### Efeitos Colaterais na Criação

1. Cria `BloqueioAgenda` para cada funcionário (tipo: AGENDAMENTO)
2. Cria `Cobranca` via `TransacaoService` (INTEGRAL ou SINAL + RESTANTE)
3. Estabelece relacionamento bidirecional Agendamento ↔ Cobranca

#### Algoritmo de Horários Disponíveis

1. Busca `toleranciaAgendamento` da config do sistema
2. Calcula duração necessária = soma dos serviços + tolerância
3. Valida dia da semana na jornada do funcionário
4. Coleta todos os bloqueios (agendamentos, almoço, pessoais)
5. Adiciona bloqueios virtuais (antes do expediente, após expediente, horas passadas)
6. Para cada lacuna entre bloqueios: gera slots espaçados pela tolerância

#### Regras de Cancelamento

- Não é possível cancelar agendamento CONCLUIDO
- Remove `BloqueioAgenda` associados
- Cancela todas as cobranças pendentes (não pagas)
- Se sinal foi cobrado: cancela sinal e restante

#### Regras de Reagendamento

- Não pode reagendar CONCLUIDO ou CANCELADO
- Nova data deve ser no futuro
- Remove bloqueios antigos
- Executa validação completa de disponibilidade
- Recalcula vencimento da cobrança

---

### 4.2 Transações e Cobranças

#### Tipos de Cobrança

| Tipo | Subtipo | Descrição |
|------|---------|-----------|
| AGENDAMENTO | INTEGRAL | Pagamento integral do serviço |
| AGENDAMENTO | SINAL | Sinal/entrada (padrão 30%) |
| AGENDAMENTO | RESTANTE | Restante após sinal |
| COMPRA | - | Pagamento de compra |

#### Cálculo do Sinal

```
Percentual padrão: 30%
valorSinal = valorTotal × percentual ÷ 100  (HALF_UP, 2 decimais)
valorRestante = valorTotal - valorSinal
```

**Sem sinal (requerSinal=false):** 1 cobrança INTEGRAL
**Com sinal (requerSinal=true):** 2 cobranças vinculadas (SINAL + RESTANTE)

#### Regra Especial: Pagamento do Sinal

Quando o SINAL é pago integralmente, o agendamento é **automaticamente confirmado**.

#### Processamento de Pagamento

1. Verifica se cobrança existe
2. Verifica se permite novo pagamento
3. Valor deve ser > 0 e ≤ valor restante
4. Registra pagamento com status CONFIRMADO
5. Recalcula valores da cobrança

#### Status de Cobrança

```
PENDENTE → PARCIALMENTE_PAGO → PAGO
PENDENTE → VENCIDA
PENDENTE → CANCELADA
PAGO → ESTORNADA
```

#### Cancelamento de Cobrança

- Não é possível cancelar cobrança já paga (usar estorno)
- Se cancelar SINAL: cancela RESTANTE automaticamente

#### Estorno

- Só cobranças pagas podem ser estornadas
- Todos os pagamentos são marcados como ESTORNADO

---

### 4.3 Clientes

#### Criação

- **Email padrão** (se vazio): `"cliente_rapido@gmail.com"`
- **Role:** sempre `ROLE_CLIENTE`
- **Senha:** codificada com BCrypt
- **CPF:** limpo (somente números), único por organização
- **Telefone:** único por organização
- **Email:** único por organização

#### Classificação de Clientes

| Classificação | Critério |
|---|---|
| VIP | Total gasto >= R$ 5.000,00 |
| PREMIUM | Total gasto >= R$ 2.000,00 |
| REGULAR | Total gasto < R$ 2.000,00 |

#### Ticket Médio

```
ticketMedio = valorTotalGasto / totalAgendamentos  (HALF_UP, 2 decimais)
```

#### Estatísticas

- Total de clientes, ativos, inativos
- Novos clientes este mês/ano
- Aniversariantes hoje/esta semana
- Clientes recorrentes

---

### 4.4 Serviços

#### Cálculo de Preço

```
valorDesconto = preco × desconto ÷ 100  (HALF_UP, 2 decimais)
precoFinal = preco - valorDesconto
```

#### Exclusão Suave (Soft Delete)

- `isDeletado = true`
- Registra usuário e data da exclusão
- Queries filtram por `isDeletadoFalse`

#### Processamento de Imagens

- **Base64** (`data:image/...`): salvas via FileStorageService
- **URLs externas** (`http/https`): mantidas como estão
- **Lista vazia:** remove todas as imagens

---

### 4.5 Funcionários

#### Validações na Criação

- **Organização:** ID do contexto deve corresponder ao DTO
- **Username:** único por organização
- **Email:** único por organização
- **CPF:** único por organização
- **Senha:** mínimo 6 caracteres
- **Cargo:** deve existir
- **Plano de horários:** validado se fornecido

#### Foto de Perfil

- Aceita formato base64
- Salva via FileStorageService
- Armazena caminho relativo na entidade

---

### 4.6 Sistema de Notificações

#### Job Agendado

- **Frequência:** a cada 5 minutos (`fixedRate = 300000`)
- **Batch:** 50 notificações por ciclo
- **Tipos:** Confirmações e Lembretes

#### Rate Limiting

```
delay = 3 minutos (fixo) + random(0-60 segundos)
```

#### Formatação de Telefone

```
Entrada: "(32) 99822-0082" ou "32998220082" ou "5532998220082"
Limpa: Remove não-dígitos
11 dígitos: Adiciona prefixo "55"
13 dígitos começando com "55": Usa como está
Inválido/Vazio: Log + registra falha
```

#### Templates de Mensagem

**Confirmação:**
```
Olá, {{nome_cliente}}!

Você tem um agendamento na *{{nome_empresa}}*:
Data: {{data_agendamento}}
Horário: {{hora_agendamento}}

Por favor, confirme respondendo:
*SIM* para confirmar
*NAO* para cancelar
```

**Lembrete:**
```
Olá, {{nome_cliente}}!

Lembrete: seu horário na *{{nome_empresa}}* está chegando!
Data: {{data_agendamento}}
Horário: {{hora_agendamento}}

Te esperamos!
```

**Placeholders disponíveis:**

| Placeholder | Substituição |
|---|---|
| `{{nome_cliente}}` | Nome completo do cliente |
| `{{data_agendamento}}` | Data (dd/MM/yyyy) |
| `{{hora_agendamento}}` | Hora (HH:mm) |
| `{{servico}}` | Nome do serviço |
| `{{profissional}}` | Nome do funcionário |
| `{{local}}` | Endereço da organização |
| `{{valor}}` | Valor (R$ X.XXX,XX) |
| `{{nome_empresa}}` | Nome da organização |

---

### 4.7 Confirmação de Agendamento via WhatsApp

#### Máquina de Estados

```
ENVIADO → AGUARDANDO_RESPOSTA
  ├→ SIM → CONFIRMADO
  ├→ NAO → CANCELADO_CLIENTE
  └→ REAGENDAR → AGUARDANDO_DATA
       └→ AGUARDANDO_HORARIO
            └→ REAGENDADO
```

#### Respostas Aceitas (case-insensitive)

| Resposta | Ação |
|---|---|
| `SIM` | Confirma agendamento |
| `NAO` | Cancela agendamento |
| `REAGENDAR` | Inicia fluxo de reagendamento |

---

### 4.8 Configurações do Sistema

#### ConfigAgendamento

| Campo | Tipo | Descrição |
|---|---|---|
| `toleranciaAgendamento` | int (minutos) | Intervalo entre slots |
| `minDiasAgendamento` | int | Mínimo de dias de antecedência |
| `maxDiasAgendamento` | int | Máximo de dias (janela de agendamento) |
| `cancelamentoCliente` | boolean | Permitir cancelamento pelo cliente |
| `tempoCancelamentoCliente` | int (minutos) | Antecedência mínima para cancelamento |
| `aprovarAgendamento` | boolean | Admin deve aprovar agendamentos |
| `cobrarSinal` | boolean | Cobrar sinal/depósito |
| `porcentSinal` | BigDecimal | Percentual do sinal (0-100) |
| `ocultarFimSemana` | boolean | Ocultar sábado e domingo |
| `ocultarDomingo` | boolean | Ocultar apenas domingo |

#### ConfigServico

| Campo | Tipo | Descrição |
|---|---|---|
| `unicoServicoAgendamento` | boolean | Apenas um serviço por agendamento |
| `mostrarValorAgendamento` | boolean | Exibir preço no agendamento público |
| `mostrarAvaliacao` | boolean | Exibir avaliações |

#### ConfigCliente

| Campo | Tipo | Descrição |
|---|---|---|
| `precisaCadastroAgendar` | boolean | Requer cadastro completo para agendar |
| `programaFidelidade` | boolean | Habilitar programa de fidelidade |
| `valorGastoUmPonto` | BigDecimal | Valor gasto = 1 ponto de fidelidade |

#### ConfigColaborador

| Campo | Tipo | Descrição |
|---|---|---|
| `selecionarColaboradorAgendamento` | boolean | Cliente pode escolher o profissional |
| `mostrarNotasComentarioColaborador` | boolean | Exibir notas do colaborador |
| `comissaoPadrao` | BigDecimal | Percentual de comissão padrão |

#### ConfigNotificacao

| Campo | Tipo | Descrição |
|---|---|---|
| `enviarConfirmacaoWhatsapp` | boolean | Enviar confirmações via WhatsApp |
| `enviarLembreteWhatsapp` | boolean | Enviar lembretes via WhatsApp |
| `enviarLembreteEmail` | boolean | Enviar lembretes por email |
| `enviarLembreteSMS` | boolean | Enviar lembretes por SMS |
| `enviarConfirmacaoForaHorario` | boolean | Enviar fora do horário comercial |
| `tempoParaConfirmacao` | int (horas) | Antecedência para confirmação |
| `tempoLembretePosConfirmacao` | int (horas) | Antecedência para lembrete |
| `mensagemTemplateConfirmacao` | String | Template de confirmação |
| `mensagemTemplateLembrete` | String | Template de lembrete |

---

### 4.9 Instâncias WhatsApp

#### Criação

1. Envia `POST` para Evolution API: `/instance/create`
2. Recebe `instanceId` e `instanceName` da resposta
3. Salva no banco com status `DISCONNECTED` e `ativo = false`
4. Configura webhook para `https://auto.bellory.com.br/webhook/whatsapp`
5. Eventos: `MESSAGES_UPSERT`

#### Tools (Permissões do Bot)

| Permissão | Descrição |
|---|---|
| `getServices` | Consultar serviços disponíveis |
| `getProfessional` | Consultar profissionais |
| `getProducts` | Consultar produtos |
| `getAvaliableSchedules` | Verificar horários disponíveis |
| `postScheduling` | Criar agendamentos |
| `sendTextMessage` | Enviar mensagens de texto |
| `sendMediaMessage` | Enviar mídia |
| `postConfirmations` | Enviar confirmações |
| `postCancellations` | Enviar cancelamentos |

#### Status da Instância

```
CONNECTED | CONNECTING | DISCONNECTED | QRCODE | ERROR | OPEN
```

---

### 4.10 API Keys

- **Prefixo:** `bly_`
- **Geração:** 32 bytes aleatórios codificados em Base64 URL
- **Armazenamento:** Hash SHA-256 (chave nunca é armazenada em texto)
- **Chave retornada apenas uma vez** na criação
- **Validação:** Verifica ativo + expiração + atualiza `lastUsedAt`
- **Revogação:** Requer `userId` + `userType` compatíveis

---

### 4.11 Bloqueios e Feriados

#### Tipos

| Tipo | Descrição |
|---|---|
| FERIADO | Feriado nacional/estadual |
| BLOQUEIO | Bloqueio personalizado |

#### Importação de Feriados

- **Fonte:** BrasilAPI (`brasilapi.com.br/api/feriados/v1/{ano}`)
- **Deduplicação:** Verifica se já foi importado antes de salvar
- **Origem:** NACIONAL (da API) ou MANUAL (criado pelo usuário)

---

### 4.12 Organizações

#### Criação

1. **CNPJ:** Limpa formatação, valida algoritmo, verifica unicidade
2. **Admin:** Cria administrador com username, email, senha (min 6 chars)
3. **Plano:** Atribui primeiro plano disponível
4. **Slug:** Gerado a partir do `nomeFantasia` (até 10 tentativas)

---

## 5. Modelo de Dados

### 5.1 Entidades Principais

#### Usuários (MappedSuperclass: User)

| Entidade | Tabela (schema: app) | Campos Específicos |
|---|---|---|
| **Admin** | `admin` | role (ROLE_SUPERADMIN), dtCriacao |
| **Funcionario** | `funcionario` | fotoPerfil, cpf, telefone, dataNasc, cargo, salário, comissão, documentos, endereço, dados bancários, jornadas, serviços (M:N) |
| **Cliente** | `cliente` | telefone, dataNascimento, cpf, isCadastroIncompleto, endereços (1:N), cartões (1:N), agendamentos (1:N), compras (1:N) |

#### Agendamento

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | Long | PK |
| `dtAgendamento` | LocalDateTime | Data/hora do agendamento |
| `status` | Enum (Status) | Estado atual |
| `requerSinal` | Boolean | Requer sinal (default: true) |
| `percentualSinal` | BigDecimal | Percentual do sinal |
| `servicos` | M:N Servico | Via `agendamento_servico` |
| `funcionarios` | M:N Funcionario | Via `agendamento_funcionario` |
| `cobrancas` | 1:N Cobranca | Sinal + Restante ou Integral |
| `cliente` | N:1 Cliente | Cliente do agendamento |

#### Instance (WhatsApp)

| Campo | Tipo | Descrição |
|---|---|---|
| `instanceId` | String | ID na Evolution API |
| `instanceName` | String | Nome único |
| `integration` | String | WHATSAPP-BAILEYS |
| `personality` | String | Personalidade do bot |
| `status` | Enum | CONNECTED, DISCONNECTED, etc. |
| `tools` | 1:1 Tools | Permissões do bot |
| `webhookConfig` | 1:1 WebhookConfig | Configuração do webhook |
| `settings` | 1:1 Settings | Configurações da instância |
| `knowledgeBase` | 1:N KnowledgeBase | Base de conhecimento |

#### Cobranca

| Campo | Tipo | Descrição |
|---|---|---|
| `valor` | BigDecimal | Valor da cobrança |
| `valorPago` | BigDecimal | Valor já pago |
| `statusCobranca` | Enum | PENDENTE, PAGO, etc. |
| `tipoCobranca` | Enum | AGENDAMENTO, COMPRA, etc. |
| `subtipoCobrancaAgendamento` | Enum | SINAL, RESTANTE, INTEGRAL |
| `dtVencimento` | LocalDate | Data de vencimento |
| `cobrancaRelacionada` | 1:1 Cobranca | Vínculo SINAL ↔ RESTANTE |
| `pagamentos` | 1:N Pagamento | Pagamentos realizados |

#### Pagamento

| Campo | Tipo | Descrição |
|---|---|---|
| `valor` | BigDecimal | Valor do pagamento |
| `formaPagamento` | Enum | DINHEIRO, PIX, CARTAO, etc. |
| `statusPagamento` | Enum | PENDENTE, CONFIRMADO, etc. |
| `numeroTransacao` | String | Número único gerado |

---

### 5.2 Enums Completos

| Enum | Valores |
|------|---------|
| **Status** (Agendamento) | PENDENTE, AGENDADO, CONFIRMADO, AGUARDANDO_CONFIRMACAO, EM_ESPERA, CONCLUIDO, CANCELADO, EM_ANDAMENTO, NAO_COMPARECEU, REAGENDADO, VENCIDA, PAGO |
| **StatusEnvio** (Notificação) | PENDENTE, ENVIADO, ENTREGUE, AGUARDANDO_RESPOSTA, AGUARDANDO_DATA, AGUARDANDO_HORARIO, CONFIRMADO, CANCELADO_CLIENTE, REAGENDADO, FALHA, CANCELADO, EXPIRADO |
| **TipoNotificacao** | CONFIRMACAO, LEMBRETE |
| **InstanceStatus** | CONNECTED, CONNECTING, DISCONNECTED, QRCODE, ERROR, OPEN |
| **KnowledgeType** | TEXT, FILE |
| **StatusCompra** | CARRINHO, AGUARDANDO_PAGAMENTO, PAGO, PROCESSANDO, PRONTO, ENTREGUE, CANCELADA |
| **TipoCompra** | ONLINE, BALCAO, TELEFONE |
| **StatusCobranca** | PENDENTE, PARCIALMENTE_PAGO, PAGO, VENCIDA, CANCELADA, ESTORNADA |
| **TipoCobranca** | AGENDAMENTO, COMPRA, TAXA_ADICIONAL, MULTA |
| **SubtipoCobrancaAgendamento** | SINAL, RESTANTE, INTEGRAL |
| **StatusProduto** | ATIVO, INATIVO, DESCONTINUADO, SEM_ESTOQUE |
| **FormaPagamento** | DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX, TRANSFERENCIA, BOLETO, CHEQUE |
| **MetodoPagamento** | CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO, PIX, TRANSFERENCIA_BANCARIA, BOLETO |
| **StatusPagamento** | PENDENTE, PROCESSANDO, CONFIRMADO, RECUSADO, CANCELADO, ESTORNADO |
| **DiaSemana** | SEGUNDA, TERCA, QUARTA, QUINTA, SEXTA, SABADO, DOMINGO |
| **TipoEndereco** | RESIDENCIAL, COMERCIAL, CORRESPONDENCIA, OUTRO |
| **BandeiraCartao** | VISA, MASTERCARD, AMERICAN_EXPRESS, ELO, HIPERCARD, DINERS_CLUB |
| **TipoCategoria** | SERVICO, PRODUTO |
| **RoleEnum** | ROLE_CLIENTE, ROLE_FUNCIONARIO, ROLE_ADMIN, ROLE_SUPERADMIN |

---

### 5.3 Diagrama de Relacionamentos

```
Organizacao (Hub Central 1:N)
├── Instance (Bot WhatsApp)
│   ├── Tools (permissões)
│   ├── WebhookConfig
│   ├── Settings
│   └── KnowledgeBase (1:N)
│
├── Usuários
│   ├── Cliente (1:N)
│   │   ├── Endereco (1:N)
│   │   ├── CartaoCredito (1:N)
│   │   ├── Agendamento (1:N)
│   │   ├── Compra (1:N)
│   │   ├── Cobranca (1:N)
│   │   └── Pagamento (1:N)
│   ├── Funcionario (1:N)
│   │   ├── Cargo (N:1)
│   │   ├── JornadaDia (1:N)
│   │   │   └── HorarioTrabalho (1:N)
│   │   ├── BloqueioAgenda (1:N)
│   │   └── Servico (M:N)
│   └── Admin (1:N)
│
├── Agendamento (transação central)
│   ├── Cliente (N:1)
│   ├── Servico (M:N)
│   ├── Funcionario (M:N)
│   ├── Cobranca (1:N)
│   │   ├── Pagamento (1:N)
│   │   └── CobrancaRelacionada (1:1)
│   └── NotificacaoEnviada (1:N)
│
├── Compra (1:N)
│   ├── CompraProduto (1:N)
│   └── Cobranca (1:1)
│
├── Servico (1:N)
│   └── Categoria (N:1)
│
├── Produto (1:N)
│   ├── Categoria (N:1)
│   └── ProdutosRelacionados (M:N)
│
├── ConfigSistema (1:1)
├── PlanoBellory (N:1, schema: admin)
├── Questionario (1:N)
│   └── Pergunta (1:N)
│       └── OpcaoResposta (1:N)
├── LandingPage (1:N)
│   └── LandingPageSection (1:N)
└── BloqueioOrganizacao (1:N)

Tracking (schema: site)
├── TrackingVisitor
│   └── TrackingSession (1:N)
│       ├── TrackingPageView (1:N)
│       ├── TrackingScrollEvent (1:N)
│       ├── TrackingInteractionEvent (1:N)
│       └── TrackingPerformanceSnapshot (1:N)
├── TrackingErrorEvent
└── TrackingConversionEvent
```

---

## 6. Migrações do Banco de Dados

| Versão | Arquivo | Descrição |
|--------|---------|-----------|
| V1 | `V1__inicial_create.sql` | Criação inicial (1.055 linhas) - todas as tabelas core |
| V2 | `V2__apikey.sql` | Tabela `api_keys` |
| V3 | `V3__config_agendamento.sql` | Campos de configuração de agendamento |
| V4 | `V4__config_servico.sql` | Campos de configuração de serviço |
| V5 | `V5__config_cliente.sql` | Campos de configuração de cliente |
| V6 | `V6__config_notificacao.sql` | Campos de configuração de notificação |
| V7 | `V7__apikey_addcolumn.sql` | Coluna `apikey` na tabela de API Keys |
| V8 | `V8__config_confirmacao.sql` | Configuração de confirmação WhatsApp |
| V9 | `V9__config_template_mensage.sql` | Templates de mensagens |
| V10 | `V10__site_publico_config.sql` | Configuração do site público |
| V11 | `V11__landing_page_editor.sql` | Editor de landing pages |
| V12 | `V12__sistema_notificacoes.sql` | Sistema completo de notificações (62 linhas) |
| V13 | `V13__sistema_questionarios.sql` | Sistema de questionários (168 linhas) |
| V14 | `V14__confirmacao_agendamento_campos.sql` | Campos de confirmação de agendamento |
| V15 | `V15__instance_settings.sql` | Campo `out_of_hours` em settings |
| V16 | `V16__modulo_financeiro.sql` | Módulo financeiro completo (176 linhas) |
| V17 | `V17__ajuste_instancia.sql` | Ajustes na instância |
| V18 | `V18__add_remote_jid_notificacao.sql` | Campo `remote_jid` em notificações |
| V19 | `V19__bloqueio_organizacao.sql` | Bloqueios de organização |
| V20 | `V20__config_agendamento_cancelado.sql` | Config de agendamento cancelado |
| V21 | `V21__tracking_analytics.sql` | Sistema de analytics (187 linhas) |

**Schemas criados:** `app`, `auto`, `site`, `admin`

---

## 7. Integrações Externas

### 7.1 Evolution API (WhatsApp)

| Config | Valor |
|---|---|
| URL | `https://wa.bellory.com.br` |
| Header de autenticação | `apikey` |
| Timeout de conexão | 5 segundos |
| Timeout de leitura | 30 segundos |

**Endpoints utilizados:**

| Método | Endpoint | Uso |
|---|---|---|
| POST | `/instance/create` | Criar instância WhatsApp |
| POST | `/message/sendText/{instanceName}` | Enviar mensagem de texto |
| GET | `/instance/connectionState/{instanceName}` | Status de conexão |
| GET | `/instance/connect/{instanceName}` | Obter QR Code |

### 7.2 N8N Webhooks

| Config | Valor |
|---|---|
| URL | `https://auto.bellory.com.br/webhook/bellory-notificacao` |
| Timeout | 10 segundos |
| Protocolo | WebClient (reativo) |

**Payload enviado:**

```json
{
  "instanceName": "nome-da-instancia",
  "telefone": "5532998220082",
  "mensagem": "texto da mensagem",
  "agendamentoId": 123,
  "tipoNotificacao": "CONFIRMACAO",
  "timestamp": 1708012345000
}
```

### 7.3 Gmail SMTP

| Config | Valor |
|---|---|
| Host | `smtp.gmail.com` |
| Porta | 587 |
| TLS | Habilitado |
| Remetente | `app.bellory@gmail.com` |
| Nome | `Bellory Sistema` |

### 7.4 BrasilAPI

| Config | Valor |
|---|---|
| Endpoint | `https://brasilapi.com.br/api/feriados/v1/{ano}` |
| Uso | Importação de feriados nacionais |

---

## 8. Tarefas Agendadas

### 8.1 NotificacaoSchedulerService

| Config | Valor |
|---|---|
| Frequência | A cada 5 minutos (`fixedRate = 300000`) |
| Batch size | 50 notificações |
| Delay fixo | 3 minutos entre mensagens |
| Delay aleatório | 0-60 segundos |

**Processo:**
1. Busca confirmações pendentes
2. Busca lembretes pendentes
3. Processa em lotes de 50
4. Para cada notificação: verifica duplicata → valida telefone → monta mensagem → envia via WhatsApp → registra resultado
5. Aplica rate limit entre envios

### 8.2 TrackingRateLimiterService

| Config | Valor |
|---|---|
| Frequência | A cada 1 minuto (`fixedRate = 60000`) |
| Limite | 60 requests/minuto por IP |
| Ação | Limpa contadores de requisições |

---

## 9. Configuração

### 9.1 Propriedades Principais

| Propriedade | Valor | Descrição |
|---|---|---|
| `api.security.token.expiration` | 36000 | Expiração JWT (10 horas) |
| `evolution.api.url` | `https://wa.bellory.com.br` | URL da Evolution API |
| `bellory.n8n.webhook-url` | `https://auto.bellory.com.br/webhook/bellory-notificacao` | Webhook n8n |
| `bellory.n8n.timeout` | 10 | Timeout n8n (segundos) |
| `spring.datasource.hikari.maximum-pool-size` | 10 (dev) / 20 (prod) | Pool de conexões |
| `spring.servlet.multipart.max-file-size` | 5MB | Tamanho máximo de upload |
| `spring.task.scheduling.pool.size` | 2 | Threads de agendamento |
| `dashboard.cache.ttl` | 300 | Cache do dashboard (5 min) |
| `dashboard.max.periodo.dias` | 365 | Período máximo de relatórios |

### 9.2 JPA/Hibernate

```
ddl-auto: validate (nunca altera schema automaticamente)
dialect: PostgreSQLDialect
batch_size: 20
order_inserts: true
order_updates: true
```

### 9.3 Jackson (Serialização JSON)

```
date-format: HH:mm
time-zone: America/Sao_Paulo
write-dates-as-timestamps: false
accept-case-insensitive-enums: true
```

### 9.4 Perfil de Desenvolvimento (application-dev.properties)

```
Banco: localhost:5432/postgres
Porta: 8081
SQL logging: Habilitado (DEBUG + TRACE)
Flyway: out-of-order=true
```

### 9.5 Perfil de Produção (application-prod.properties)

```
Banco: host.docker.internal:5432/bellory_prod
Porta: 8080
SQL logging: Desabilitado
Flyway: out-of-order=false, clean-disabled=true
Pool: 20 conexões
```

---

> **Gerado automaticamente** em 17/02/2026 por análise completa do código-fonte da Bellory API.
> **33 controllers** | **435+ endpoints** | **54+ services** | **68+ entidades** | **21 migrations**
