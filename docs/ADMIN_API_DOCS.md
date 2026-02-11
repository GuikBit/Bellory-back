# API Administrativa Bellory - Documentacao para Frontend

## Visao Geral

O painel administrativo da Bellory permite visualizar metricas e gerenciar todas as organizacoes (clientes) da plataforma. Todas as rotas estao protegidas e requerem autenticacao com role `ROLE_SUPERADMIN` ou `ROLE_ADMIN`.

**Base URL:** `/api/admin`

localhost:8081 em desenvolvimento.
api-dev.bellory.com.br api de desenvolvimento.
api.bellory.com.br api de produção.

**Autenticacao:** Bearer Token JWT (mesmo fluxo de login existente em `/api/auth/login`)

**Headers obrigatorios:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

---

## Arquitetura de Endpoints

```
/api/admin/
  ├── dashboard              GET  → Dashboard geral consolidado
  ├── organizacoes           GET  → Lista de organizacoes
  │   └── /{id}              GET  → Detalhe de organizacao
  └── metricas/
      ├── agendamentos       GET  → Metricas de agendamentos
      ├── faturamento        GET  → Metricas de faturamento
      ├── servicos           GET  → Metricas de servicos
      ├── funcionarios       GET  → Metricas de funcionarios
      ├── clientes           GET  → Metricas de clientes
      ├── instancias         GET  → Metricas de instancias WhatsApp
      └── planos             GET  → Metricas de planos
```

---

## 1. Dashboard Geral

### `GET /api/admin/dashboard`

Retorna uma visao consolidada de todas as metricas da plataforma em um unico endpoint. Ideal para a pagina inicial do painel admin.

**Response:**
```json
{
  "totalOrganizacoes": 45,
  "organizacoesAtivas": 38,
  "organizacoesInativas": 7,
  "totalAgendamentos": 12500,
  "totalClientes": 8200,
  "totalFuncionarios": 340,
  "totalServicos": 890,
  "totalInstancias": 52,
  "instanciasConectadas": 45,
  "instanciasDesconectadas": 7,
  "faturamentoTotal": 1250000.00,
  "totalCobrancas": 15000,
  "cobrancasPendentes": 320,
  "cobrancasPagas": 14200,
  "distribuicaoPlanos": {
    "gratuito": 12,
    "basico": 15,
    "plus": 10,
    "premium": 8
  }
}
```

**Sugestao de uso no frontend:**
- Cards com numeros grandes para cada metrica principal
- Grafico de pizza/donut para distribuicao de planos
- Indicadores de status para instancias (conectadas vs desconectadas)

---

## 2. Organizacoes

### `GET /api/admin/organizacoes`

Lista todas as organizacoes com contadores resumidos. Ideal para a listagem/tabela principal.

**Response:**
```json
[
  {
    "id": 1,
    "nomeFantasia": "Salao da Maria",
    "razaoSocial": "Maria LTDA",
    "cnpj": "12.345.678/0001-90",
    "emailPrincipal": "contato@salaomaria.com.br",
    "telefone1": "(11) 99999-9999",
    "slug": "salao-da-maria",
    "ativo": true,
    "planoNome": "Plus",
    "planoCodigo": "plus",
    "dtCadastro": "2024-06-15T10:30:00",
    "totalAgendamentos": 350,
    "totalClientes": 120,
    "totalFuncionarios": 8,
    "totalServicos": 25,
    "totalInstancias": 2
  }
]
```

**Sugestao de uso no frontend:**
- Tabela com colunas ordenáveis e filtráveis
- Badge de status (ativo/inativo)
- Badge do plano com cor correspondente
- Link para pagina de detalhe ao clicar na linha
- Busca por nome, CNPJ ou email

---

### `GET /api/admin/organizacoes/{id}`

Retorna detalhes completos de uma organizacao, incluindo plano, limites, metricas e instancias.

**Path Params:**
| Param | Tipo | Descricao |
|-------|------|-----------|
| id | Long | ID da organizacao |

**Response:**
```json
{
  "id": 1,
  "nomeFantasia": "Salao da Maria",
  "razaoSocial": "Maria LTDA",
  "cnpj": "12.345.678/0001-90",
  "emailPrincipal": "contato@salaomaria.com.br",
  "telefone1": "(11) 99999-9999",
  "telefone2": "(11) 88888-8888",
  "whatsapp": "(11) 99999-9999",
  "slug": "salao-da-maria",
  "ativo": true,
  "dtCadastro": "2024-06-15T10:30:00",
  "dtAtualizacao": "2025-01-20T14:00:00",
  "responsavelNome": "Maria Silva",
  "responsavelEmail": "maria@salaomaria.com.br",
  "responsavelTelefone": "(11) 97777-7777",

  "plano": {
    "id": 3,
    "codigo": "plus",
    "nome": "Plus",
    "precoMensal": 149.90,
    "precoAnual": 1438.80
  },

  "limites": {
    "maxAgendamentosMes": 500,
    "maxUsuarios": 15,
    "maxClientes": 1000,
    "maxServicos": 50,
    "maxUnidades": 3,
    "permiteAgendamentoOnline": true,
    "permiteWhatsapp": true,
    "permiteSite": true,
    "permiteEcommerce": true,
    "permiteRelatoriosAvancados": true,
    "permiteApi": false,
    "permiteIntegracaoPersonalizada": false,
    "suportePrioritario": true,
    "suporte24x7": false
  },

  "limitesPersonalizados": null,

  "metricas": {
    "totalAgendamentos": 350,
    "agendamentosNoMes": 42,
    "agendamentosConcluidos": 280,
    "agendamentosCancelados": 30,
    "agendamentosPendentes": 15,
    "totalClientes": 120,
    "clientesAtivos": 95,
    "totalFuncionarios": 8,
    "funcionariosAtivos": 7,
    "totalServicos": 25,
    "servicosAtivos": 22,
    "faturamentoTotal": 45000.00,
    "faturamentoMes": 5200.00,
    "totalCobrancas": 380,
    "cobrancasPagas": 350,
    "cobrancasPendentes": 20,
    "cobrancasVencidas": 10
  },

  "instancias": [
    {
      "id": 1,
      "instanceName": "salao-maria-whatsapp",
      "instanceId": "abc123",
      "status": "CONNECTED",
      "ativo": true
    },
    {
      "id": 2,
      "instanceName": "salao-maria-bot",
      "instanceId": "def456",
      "status": "DISCONNECTED",
      "ativo": true
    }
  ]
}
```

**Sugestao de uso no frontend:**
- Pagina de detalhe com abas: Informacoes, Metricas, Plano, Instancias
- Secao de informacoes basicas com dados da org e responsavel
- Cards de metricas com numeros e indicadores visuais
- Tabela de limites do plano com checkmarks para features
- Lista de instancias com indicadores de status (verde/vermelho)
- Barra de uso: ex. "42 de 500 agendamentos no mes" com barra de progresso

---

## 3. Metricas por Dominio

### `GET /api/admin/metricas/agendamentos`

Metricas detalhadas de agendamentos de toda a plataforma.

**Response:**
```json
{
  "totalGeral": 12500,
  "totalNoMes": 850,
  "concluidos": 9800,
  "cancelados": 1200,
  "pendentes": 450,
  "agendados": 800,
  "naoCompareceu": 250,
  "taxaConclusao": 78.40,
  "taxaCancelamento": 9.60,
  "taxaNoShow": 2.00,

  "porOrganizacao": [
    {
      "organizacaoId": 1,
      "nomeFantasia": "Salao da Maria",
      "total": 350,
      "concluidos": 280,
      "cancelados": 30,
      "pendentes": 15
    }
  ],

  "evolucaoMensal": [
    {
      "mes": "2025-01",
      "total": 750,
      "concluidos": 620,
      "cancelados": 80
    },
    {
      "mes": "2025-02",
      "total": 850,
      "concluidos": 700,
      "cancelados": 90
    }
  ]
}
```

**Sugestao de uso no frontend:**
- Cards: total, concluidos, cancelados, pendentes, no-show
- Taxas em porcentagem com cores (verde para conclusao, vermelho para cancelamento)
- Grafico de linhas para evolucao mensal
- Tabela ranking de organizacoes por volume de agendamentos
- Grafico de barras comparando status por organizacao

---

### `GET /api/admin/metricas/faturamento`

Metricas financeiras e de faturamento da plataforma.

**Response:**
```json
{
  "faturamentoTotalGeral": 1250000.00,
  "faturamentoMesAtual": 95000.00,
  "faturamentoMesAnterior": 88000.00,
  "crescimentoPercentual": 7.95,
  "ticketMedio": 125.00,
  "totalPagamentos": 15000,
  "pagamentosConfirmados": 14200,

  "porOrganizacao": [
    {
      "organizacaoId": 1,
      "nomeFantasia": "Salao da Maria",
      "planoCodigo": "plus",
      "faturamentoTotal": 45000.00,
      "faturamentoMes": 5200.00,
      "totalCobrancas": 380,
      "cobrancasPagas": 350,
      "cobrancasPendentes": 20
    }
  ],

  "evolucaoMensal": [
    {
      "mes": "2025-01",
      "valor": 88000.00,
      "quantidadePagamentos": 1200
    },
    {
      "mes": "2025-02",
      "valor": 95000.00,
      "quantidadePagamentos": 1350
    }
  ]
}
```

**Sugestao de uso no frontend:**
- Card principal com faturamento do mes e indicador de crescimento (seta verde/vermelha)
- Grafico de area para evolucao mensal do faturamento
- Ticket medio em destaque
- Ranking de organizacoes por faturamento em tabela
- Indicadores de cobrancas por organizacao

---

### `GET /api/admin/metricas/servicos`

Metricas sobre servicos cadastrados em toda a plataforma.

**Response:**
```json
{
  "totalServicosGeral": 890,
  "servicosAtivos": 750,
  "servicosInativos": 140,
  "precoMedio": 85.50,

  "porOrganizacao": [
    {
      "organizacaoId": 1,
      "nomeFantasia": "Salao da Maria",
      "totalServicos": 25,
      "servicosAtivos": 22
    }
  ],

  "maisAgendados": []
}
```

**Sugestao de uso no frontend:**
- Cards: total, ativos, inativos, preco medio
- Tabela de servicos por organizacao
- Grafico de barras com top organizacoes por quantidade de servicos

---

### `GET /api/admin/metricas/funcionarios`

Metricas sobre funcionarios de toda a plataforma.

**Response:**
```json
{
  "totalFuncionariosGeral": 340,
  "funcionariosAtivos": 310,
  "funcionariosInativos": 30,
  "mediaFuncionariosPorOrganizacao": 7.56,

  "porOrganizacao": [
    {
      "organizacaoId": 1,
      "nomeFantasia": "Salao da Maria",
      "totalFuncionarios": 8,
      "funcionariosAtivos": 7,
      "totalServicosVinculados": 25
    }
  ]
}
```

**Sugestao de uso no frontend:**
- Cards: total, ativos, inativos, media por org
- Tabela ranking por quantidade de funcionarios
- Indicador de servicos vinculados por funcionario

---

### `GET /api/admin/metricas/clientes`

Metricas sobre clientes finais de toda a plataforma.

**Response:**
```json
{
  "totalClientesGeral": 8200,
  "clientesAtivos": 7100,
  "clientesInativos": 1100,
  "mediaClientesPorOrganizacao": 182.22,

  "porOrganizacao": [
    {
      "organizacaoId": 1,
      "nomeFantasia": "Salao da Maria",
      "totalClientes": 120,
      "clientesAtivos": 95
    }
  ],

  "evolucaoMensal": [
    {
      "mes": "2025-01",
      "novosClientes": 350,
      "totalAcumulado": 7500
    },
    {
      "mes": "2025-02",
      "novosClientes": 400,
      "totalAcumulado": 7900
    }
  ]
}
```

**Sugestao de uso no frontend:**
- Cards: total, ativos, inativos, media por org
- Grafico de linhas para evolucao mensal de novos clientes
- Grafico de area para total acumulado
- Ranking de organizacoes por quantidade de clientes

---

### `GET /api/admin/metricas/instancias`

Metricas sobre instancias WhatsApp/Bot da plataforma.

**Response:**
```json
{
  "totalInstancias": 52,
  "instanciasAtivas": 45,
  "instanciasDeletadas": 3,
  "instanciasConectadas": 45,
  "instanciasDesconectadas": 7,

  "porOrganizacao": [
    {
      "organizacaoId": 1,
      "nomeFantasia": "Salao da Maria",
      "totalInstancias": 2,
      "instanciasAtivas": 2,
      "instanciasConectadas": 1
    }
  ],

  "todasInstancias": [
    {
      "id": 1,
      "instanceName": "salao-maria-whatsapp",
      "instanceId": "abc123",
      "integration": "WHATSAPP-BAILEYS",
      "status": "CONNECTED",
      "ativo": true,
      "organizacaoId": 1,
      "nomeFantasiaOrganizacao": "Salao da Maria"
    }
  ]
}
```

**Sugestao de uso no frontend:**
- Cards com indicadores de status (verde/vermelho) para conectadas/desconectadas
- Lista completa de instancias com status em tempo real
- Agrupamento por organizacao
- Filtro por status (conectada, desconectada, inativa)

---

### `GET /api/admin/metricas/planos`

Distribuicao de organizacoes por plano.

**Response:**
```json
{
  "totalPlanos": 4,
  "planosAtivos": 4,

  "distribuicao": [
    {
      "planoId": 1,
      "codigo": "gratuito",
      "nome": "Gratuito",
      "precoMensal": 0.00,
      "precoAnual": 0.00,
      "totalOrganizacoes": 12,
      "percentualDistribuicao": 26.67,
      "ativo": true,
      "popular": false
    },
    {
      "planoId": 2,
      "codigo": "basico",
      "nome": "Basico",
      "precoMensal": 79.90,
      "precoAnual": 766.80,
      "totalOrganizacoes": 15,
      "percentualDistribuicao": 33.33,
      "ativo": true,
      "popular": false
    },
    {
      "planoId": 3,
      "codigo": "plus",
      "nome": "Plus",
      "precoMensal": 149.90,
      "precoAnual": 1438.80,
      "totalOrganizacoes": 10,
      "percentualDistribuicao": 22.22,
      "ativo": true,
      "popular": true
    },
    {
      "planoId": 4,
      "codigo": "premium",
      "nome": "Premium",
      "precoMensal": 299.90,
      "precoAnual": 2878.80,
      "totalOrganizacoes": 8,
      "percentualDistribuicao": 17.78,
      "ativo": true,
      "popular": false
    }
  ]
}
```

**Sugestao de uso no frontend:**
- Grafico de pizza/donut para distribuicao de planos
- Cards por plano com contagem de organizacoes
- Cores distintas por plano (seguir cores do PlanoBellory.cor)
- Indicador visual de plano popular

---

## Orientacoes Gerais para o Frontend

### Autenticacao

1. Fazer login via `POST /api/auth/login` com usuario admin (ROLE_SUPERADMIN ou ROLE_ADMIN)
2. Armazenar o token JWT retornado
3. Enviar o token em todas as requisicoes ao `/api/admin/**`
4. Implementar refresh token automatico via `POST /api/auth/refresh`

### Estrutura de Paginas Sugerida

```
/admin
  ├── /dashboard          → GET /api/admin/dashboard
  ├── /organizacoes       → GET /api/admin/organizacoes
  │   └── /:id            → GET /api/admin/organizacoes/{id}
  ├── /metricas
  │   ├── /agendamentos   → GET /api/admin/metricas/agendamentos
  │   ├── /faturamento    → GET /api/admin/metricas/faturamento
  │   ├── /servicos       → GET /api/admin/metricas/servicos
  │   ├── /funcionarios   → GET /api/admin/metricas/funcionarios
  │   ├── /clientes       → GET /api/admin/metricas/clientes
  │   ├── /instancias     → GET /api/admin/metricas/instancias
  │   └── /planos         → GET /api/admin/metricas/planos
```

### Bibliotecas de Graficos Recomendadas

Para React:
- **Recharts** - Graficos de linhas, barras, pizza
- **Nivo** - Graficos avancados e animados
- **Chart.js com react-chartjs-2** - Alternativa leve

Para Tailwind CSS / shadcn:
- **tremor.so** - Componentes de dashboard prontos
- **shadcn/ui charts** - Graficos integrados ao shadcn

### Tratamento de Erros

Todos os endpoints podem retornar:

| Status | Descricao |
|--------|-----------|
| 200 | Sucesso |
| 401 | Token invalido ou expirado |
| 403 | Sem permissao (role insuficiente) |
| 404 | Organizacao nao encontrada (para /{id}) |
| 500 | Erro interno do servidor |

### Formatacao de Dados

- **Datas**: Formato `yyyy-MM-dd'T'HH:mm:ss` (ISO 8601 sem timezone)
- **Valores monetarios**: `BigDecimal` com 2 casas decimais (ex: `1250.00`)
- **Percentuais**: `BigDecimal` com 2 casas decimais (ex: `78.40` para 78,40%)
- **Meses na evolucao**: Formato `YYYY-MM` (ex: `2025-01`)

### Cache e Performance

- O dashboard e as metricas podem ser cacheados no frontend por 5 minutos
- A lista de organizacoes pode ser paginada no futuro (endpoint preparado)
- Use loading states/skeletons enquanto os dados carregam

---

## Arquivos do Backend (Referencia)

### Controllers
- `controller/admin/AdminDashboardController.java` - Dashboard geral
- `controller/admin/AdminOrganizacaoController.java` - Gestao de organizacoes
- `controller/admin/AdminMetricasController.java` - Metricas por dominio

### Services
- `service/admin/AdminDashboardService.java` - Logica do dashboard
- `service/admin/AdminOrganizacaoService.java` - Logica de organizacoes
- `service/admin/AdminMetricasService.java` - Logica de metricas

### DTOs
- `model/dto/admin/AdminDashboardDTO.java`
- `model/dto/admin/AdminOrganizacaoListDTO.java`
- `model/dto/admin/AdminOrganizacaoDetalheDTO.java`
- `model/dto/admin/AdminAgendamentoMetricasDTO.java`
- `model/dto/admin/AdminFaturamentoMetricasDTO.java`
- `model/dto/admin/AdminServicoMetricasDTO.java`
- `model/dto/admin/AdminFuncionarioMetricasDTO.java`
- `model/dto/admin/AdminClienteMetricasDTO.java`
- `model/dto/admin/AdminInstanciaMetricasDTO.java`
- `model/dto/admin/AdminPlanoMetricasDTO.java`
- `model/dto/admin/AdminFiltroDTO.java`

### Repository
- `model/repository/admin/AdminQueryRepository.java` - Queries cross-organization

### Seguranca
- Rotas `/api/admin/**` protegidas com `hasAnyRole("SUPERADMIN", "ADMIN")`
- Configurado em `config/SecurityConfig.java`
