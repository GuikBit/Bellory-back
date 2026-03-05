# Bellory - Documentação do Sistema de Tracking & Analytics

> Documentação técnica para implementação do módulo de analytics no Bellory Admin.

---

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura](#2-arquitetura)
3. [Endpoints da API](#3-endpoints-da-api)
4. [Estruturas de Dados (Response DTOs)](#4-estruturas-de-dados)
5. [Módulos do Dashboard](#5-módulos-do-dashboard)
6. [Funil de Conversão](#6-funil-de-conversão)
7. [Métricas em Tempo Real](#7-métricas-em-tempo-real)
8. [Guia de Implementação Frontend](#8-guia-de-implementação-frontend)

---

## 1. Visão Geral

O sistema de tracking coleta dados de tráfego da landing page do Bellory e os disponibiliza via API REST para análise no painel administrativo. O tracking captura:

| Categoria          | O que coleta                                                        |
|--------------------|---------------------------------------------------------------------|
| **Visitantes**     | Identificação por UUID, visitantes novos vs. recorrentes            |
| **Sessões**        | Duração, páginas por sessão, bounce rate, entry/exit pages          |
| **Tráfego**        | Fonte (direct, organic, referral, social), UTM params, referrers    |
| **Comportamento**  | Page views, cliques em CTAs, scroll depth, seções visíveis          |
| **Conversão**      | Funil de cadastro (6 etapas), planos, ciclo de cobrança            |
| **Performance**    | Core Web Vitals (FCP, LCP, FID, CLS, TTFB)                        |
| **Erros**          | Erros client-side, stack traces, status codes                      |
| **Geolocalização** | País, estado, cidade, coordenadas (lat/lng)                        |
| **Dispositivo**    | Tipo (desktop/mobile/tablet), OS, browser, resolução, touch        |

---

## 2. Arquitetura

### Fluxo de Dados

```
Landing Page (JS Tracker)
        │
        ▼
  POST /api/v1/tracking          ← Payload JSON / text/plain (sendBeacon)
        │
        ▼
  TrackingController             ← Rate limit: 60 req/min por IP
        │                          Max payload: 100KB
        ▼
  TrackingProcessingService      ← Processamento assíncrono (@Async)
        │
        ├── Upsert Visitor
        ├── Upsert Session
        ├── Process Events (navigation, interaction, scroll, conversion, error)
        └── Process Performance (Web Vitals)
        │
        ▼
  PostgreSQL (schema: site)      ← 9 tabelas, 33 índices
        │
        ▼
  AdminAnalyticsService          ← Agregações e queries analíticas
        │
        ▼
  GET /api/v1/admin/analytics/*  ← Endpoints protegidos (JWT admin)
        │
        ▼
  Bellory Admin (Frontend)       ← Dashboard de analytics
```

### Schema do Banco (site)

```
site.tracking_visitors              ← Visitantes únicos (UUID PK)
site.tracking_sessions              ← Sessões com device/geo/utm
site.tracking_page_views            ← Page views por sessão
site.tracking_interaction_events    ← Cliques em CTAs/botões
site.tracking_scroll_events         ← Profundidade de scroll
site.tracking_conversion_events     ← Funil de cadastro
site.tracking_error_events          ← Erros client-side
site.tracking_performance_snapshots ← Core Web Vitals
site.tracking_daily_metrics         ← Métricas diárias pré-calculadas
```

---

## 3. Endpoints da API

**Base URL:** `/api/v1/admin/analytics`
**Autenticação:** JWT com `userType: PLATFORM_ADMIN`
**Formato de data:** ISO `YYYY-MM-DD` (query params: `start_date`, `end_date`)

### 3.1 GET /overview

Visão geral com métricas principais do período.

**Parâmetros:**

| Param        | Tipo   | Obrigatório | Exemplo      |
|--------------|--------|-------------|--------------|
| `start_date` | String | Sim         | `2026-01-01` |
| `end_date`   | String | Sim         | `2026-01-31` |

**Response: `AnalyticsOverviewDTO`**

```json
{
  "period": {
    "start": "2026-01-01",
    "end": "2026-01-31"
  },
  "visitors": {
    "total": 1250,
    "newVisitors": 980,
    "returning": 270
  },
  "sessions": {
    "total": 1580,
    "averageDuration": 185000,
    "averagePages": 3.2,
    "bounceRate": 42.5
  },
  "conversions": {
    "cadastroStarted": 120,
    "cadastroCompleted": 45,
    "conversionRate": 3.6
  },
  "topPages": [
    {
      "path": "/",
      "views": 1580,
      "avgTimeOnPage": 45000
    },
    {
      "path": "/precos",
      "views": 620,
      "avgTimeOnPage": 92000
    }
  ]
}
```

### 3.2 GET /traffic

Análise detalhada de fontes de tráfego.

**Parâmetros:** `start_date`, `end_date`

**Response: `AnalyticsTrafficDTO`**

```json
{
  "sources": [
    {
      "source": "organic",
      "visitors": 450,
      "sessions": 520,
      "conversionRate": 4.2
    },
    {
      "source": "direct",
      "visitors": 380,
      "sessions": 410,
      "conversionRate": 2.8
    },
    {
      "source": "referral",
      "visitors": 200,
      "sessions": 230,
      "conversionRate": 5.1
    },
    {
      "source": "social",
      "visitors": 150,
      "sessions": 170,
      "conversionRate": 1.5
    }
  ],
  "campaigns": [
    {
      "campaign": "lancamento-2026",
      "source": "google",
      "medium": "cpc",
      "visitors": 320,
      "conversions": 18
    }
  ],
  "topReferrers": [
    {
      "referrer": "instagram.com",
      "visitors": 120
    },
    {
      "referrer": "google.com",
      "visitors": 95
    }
  ]
}
```

### 3.3 GET /behavior

Análise de comportamento do usuário na landing page.

**Parâmetros:** `start_date`, `end_date`

**Response: `AnalyticsBehaviorDTO`**

```json
{
  "topCTAs": [
    {
      "elementId": "btn-comecar-gratis",
      "label": "Começar Grátis",
      "section": "hero",
      "clicks": 340
    },
    {
      "elementId": "btn-ver-precos",
      "label": "Ver Preços",
      "section": "features",
      "clicks": 210
    }
  ],
  "scrollDepth": {
    "25": 88.5,
    "50": 62.3,
    "75": 41.0,
    "100": 18.7
  },
  "sectionVisibility": [
    {
      "section": "hero",
      "viewRate": 100.0
    },
    {
      "section": "features",
      "viewRate": 78.5
    },
    {
      "section": "pricing",
      "viewRate": 55.2
    },
    {
      "section": "testimonials",
      "viewRate": 38.0
    }
  ],
  "flows": {
    "exitPages": [
      {
        "path": "/precos",
        "exits": 180,
        "exitRate": 29.0
      },
      {
        "path": "/",
        "exits": 420,
        "exitRate": 26.6
      }
    ]
  }
}
```

### 3.4 GET /conversions

Funil de conversão e dados de planos.

**Parâmetros:** `start_date`, `end_date`

**Response: `AnalyticsConversionsDTO`**

```json
{
  "funnel": {
    "totalVisitors": 1250,
    "viewedPricing": 620,
    "startedCadastro": 120,
    "completedStep0_empresa": 110,
    "completedStep1_localizacao": 95,
    "completedStep2_acesso": 88,
    "completedStep3_tema": 72,
    "completedStep4_plano": 55,
    "completedCadastro": 45
  },
  "planDistribution": [
    {
      "planId": "pro",
      "planName": "Profissional",
      "count": 25,
      "percentage": 55.6
    },
    {
      "planId": "basic",
      "planName": "Básico",
      "count": 15,
      "percentage": 33.3
    },
    {
      "planId": "enterprise",
      "planName": "Enterprise",
      "count": 5,
      "percentage": 11.1
    }
  ],
  "billingPreference": {
    "monthly": 28,
    "annual": 17,
    "annualPercentage": 37.8
  },
  "averageTimeToConvert": {
    "fromFirstVisitMs": 259200000,
    "averageSessions": 2.8
  }
}
```

### 3.5 GET /context

Contexto técnico: dispositivos, geo, performance e erros.

**Parâmetros:** `start_date`, `end_date`

**Response: `AnalyticsContextDTO`**

```json
{
  "devices": {
    "desktop": { "visitors": 650, "percentage": 52.0, "conversionRate": 4.5 },
    "mobile":  { "visitors": 500, "percentage": 40.0, "conversionRate": 2.1 },
    "tablet":  { "visitors": 100, "percentage": 8.0,  "conversionRate": 3.0 }
  },
  "browsers": [
    { "browser": "Chrome",  "visitors": 780, "percentage": 62.4 },
    { "browser": "Safari",  "visitors": 250, "percentage": 20.0 },
    { "browser": "Firefox", "visitors": 120, "percentage": 9.6 }
  ],
  "osList": [
    { "os": "Windows", "visitors": 520, "percentage": 41.6 },
    { "os": "iOS",     "visitors": 320, "percentage": 25.6 },
    { "os": "Android", "visitors": 250, "percentage": 20.0 },
    { "os": "macOS",   "visitors": 160, "percentage": 12.8 }
  ],
  "geo": {
    "countries": [
      { "name": "Brasil", "visitors": 1180, "percentage": 94.4 }
    ],
    "states": [
      { "name": "São Paulo", "visitors": 450, "percentage": 36.0 },
      { "name": "Rio de Janeiro", "visitors": 180, "percentage": 14.4 }
    ],
    "cities": [
      { "name": "São Paulo", "visitors": 320, "percentage": 25.6 },
      { "name": "Rio de Janeiro", "visitors": 140, "percentage": 11.2 }
    ]
  },
  "performance": {
    "averages": {
      "pageLoadTime": 2100,
      "fcp": 1200,
      "lcp": 2500,
      "fid": 80,
      "cls": 0.05,
      "ttfb": 350
    },
    "byDevice": {
      "desktop": { "pageLoadTime": 1800, "lcp": 2100 },
      "mobile":  { "pageLoadTime": 2800, "lcp": 3200 }
    },
    "percentiles": {
      "p50": { "lcp": 2200, "fid": 50, "cls": 0.03 },
      "p75": { "lcp": 3500, "fid": 120, "cls": 0.08 },
      "p95": { "lcp": 5800, "fid": 280, "cls": 0.25 }
    }
  },
  "errors": {
    "total": 35,
    "byType": [
      { "type": "TypeError", "count": 15 },
      { "type": "NetworkError", "count": 12 }
    ],
    "topErrors": [
      {
        "message": "Cannot read properties of undefined",
        "count": 8,
        "lastOccurrence": "2026-01-30T14:22:00"
      }
    ]
  }
}
```

### 3.6 GET /realtime

Métricas em tempo real (sem parâmetros de data).

**Response: `AnalyticsRealtimeDTO`**

```json
{
  "activeVisitors": 12,
  "activePages": [
    { "path": "/", "visitors": 7 },
    { "path": "/precos", "visitors": 3 },
    { "path": "/cadastro", "visitors": 2 }
  ],
  "recentEvents": [
    {
      "type": "cadastro_completed",
      "planName": "Profissional",
      "occurredAt": "2026-01-31T15:42:00"
    },
    {
      "type": "click_cta",
      "elementLabel": "Começar Grátis",
      "occurredAt": "2026-01-31T15:40:30"
    }
  ],
  "last30Minutes": {
    "visitors": 28,
    "pageViews": 85,
    "conversions": 2
  }
}
```

---

## 4. Estruturas de Dados

### Tipos de Evento de Conversão

| eventType                  | registrationStep | Descrição                    |
|----------------------------|------------------|------------------------------|
| `plan_viewed`              | —                | Usuário visualizou um plano  |
| `cadastro_started`         | —                | Iniciou o cadastro           |
| `cadastro_step_completed`  | 0                | Completou: Empresa           |
| `cadastro_step_completed`  | 1                | Completou: Localização       |
| `cadastro_step_completed`  | 2                | Completou: Acesso            |
| `cadastro_step_completed`  | 3                | Completou: Tema              |
| `cadastro_step_completed`  | 4                | Completou: Plano             |
| `cadastro_completed`       | —                | Cadastro finalizado          |

### Fontes de Tráfego

| trafficSource | Descrição                              |
|---------------|----------------------------------------|
| `direct`      | Acesso direto (sem referrer)           |
| `organic`     | Busca orgânica (Google, Bing, etc.)    |
| `referral`    | Link de outro site                     |
| `social`      | Redes sociais                          |
| `email`       | Campanhas de email (utm_medium=email)  |
| `paid`        | Tráfego pago (utm_medium=cpc/ppc)     |

### Tipos de Interação

| eventType      | Descrição                        |
|----------------|----------------------------------|
| `click_cta`    | Clique em Call-to-Action         |
| `click_button` | Clique em botão genérico         |
| `click_plan`   | Clique em card de plano          |

### Core Web Vitals - Referência

| Métrica | Bom       | Precisa Melhorar | Ruim     |
|---------|-----------|------------------|----------|
| FCP     | < 1800ms  | 1800-3000ms      | > 3000ms |
| LCP     | < 2500ms  | 2500-4000ms      | > 4000ms |
| FID     | < 100ms   | 100-300ms        | > 300ms  |
| CLS     | < 0.1     | 0.1-0.25         | > 0.25   |
| TTFB    | < 800ms   | 800-1800ms       | > 1800ms |

---

## 5. Módulos do Dashboard

Estrutura recomendada de páginas/tabs para o módulo de Analytics no Bellory Admin:

### 5.1 Dashboard Principal (Overview)

**Endpoint:** `GET /overview`

**Cards de métricas principais:**
- Total de visitantes (com indicador novo vs. recorrente)
- Total de sessões
- Duração média da sessão (converter ms → formato legível: "3m 05s")
- Bounce rate (%)
- Conversões (cadastros completados)
- Taxa de conversão (%)

**Gráficos sugeridos:**
- Line chart: visitantes/sessões ao longo do tempo (necessita de chamadas com ranges diários)
- Bar chart horizontal: top pages mais visitadas
- Donut chart: visitantes novos vs. recorrentes

---

### 5.2 Tráfego (Traffic)

**Endpoint:** `GET /traffic`

**Visualizações:**
- Donut/pie chart: distribuição por fonte de tráfego (com taxa de conversão por fonte)
- Tabela: campanhas UTM (campanha, fonte, meio, visitantes, conversões)
- Tabela: top referrers (site, visitantes)

**Insights úteis:**
- Qual fonte converte mais? → `sources[].conversionRate`
- Quais campanhas trazem mais tráfego? → `campaigns[]`
- De onde vem o tráfego de referência? → `topReferrers[]`

---

### 5.3 Comportamento (Behavior)

**Endpoint:** `GET /behavior`

**Visualizações:**
- Tabela ranqueada: CTAs mais clicados (elemento, seção, cliques)
- Barra horizontal: scroll depth (% de usuários que chegam a 25%, 50%, 75%, 100%)
- Barra horizontal: visibilidade das seções (% de usuários que viram cada seção)
- Tabela: páginas de saída com exit rate

**Insights úteis:**
- Qual CTA converte mais? → `topCTAs[]`
- Onde os usuários param de scrollar? → `scrollDepth`
- Qual a última coisa que veem antes de sair? → `flows.exitPages[]`

---

### 5.4 Conversões (Conversions)

**Endpoint:** `GET /conversions`

**Visualizações:**
- **Funil vertical:** etapas do cadastro com drop-off entre cada etapa
  ```
  Visitantes Totais    ████████████████████  1250
  Viram Preços         █████████████         620  (49.6% do total)
  Iniciaram Cadastro   ████                  120  (9.6%)
  Empresa              ███                   110  (91.7% do anterior)
  Localização          ███                   95   (86.4%)
  Acesso               ██                    88   (92.6%)
  Tema                 ██                    72   (81.8%)
  Plano                █                     55   (76.4%)
  Cadastro Completo    █                     45   (81.8%)
  ```
- Pie chart: distribuição por plano escolhido
- Comparação: mensal vs. anual (billing preference)
- Card: tempo médio para conversão ("3 dias, ~2.8 sessões")

**Insights úteis:**
- Onde está o maior drop-off no funil?
- Qual plano é mais popular?
- Quanto tempo leva para converter?

---

### 5.5 Contexto Técnico (Context)

**Endpoint:** `GET /context`

**Sub-abas:**

#### Dispositivos & Tecnologia
- Donut chart: desktop vs. mobile vs. tablet (com taxa de conversão de cada um)
- Tabela: browsers
- Tabela: sistemas operacionais

#### Geolocalização
- Tabela/mapa: países, estados, cidades
- Se tiver coordenadas: mapa de calor com pontos

#### Performance (Web Vitals)
- Cards com semáforo (verde/amarelo/vermelho) para cada métrica:
  - FCP, LCP, FID, CLS, TTFB
- Comparação desktop vs. mobile
- Tabela de percentis (P50, P75, P95)

#### Erros
- Card: total de erros no período
- Tabela: erros por tipo
- Tabela: top erros (mensagem, contagem, última ocorrência)

---

### 5.6 Tempo Real (Realtime)

**Endpoint:** `GET /realtime`
**Polling recomendado:** a cada 30 segundos

**Visualizações:**
- Número grande animado: visitantes ativos agora
- Lista: páginas ativas com contagem de visitantes
- Feed/timeline: eventos recentes (conversões, cliques)
- Cards: últimos 30 minutos (visitantes, page views, conversões)

---

## 6. Funil de Conversão - Detalhamento

O funil de cadastro tem **8 estágios** rastreados:

```
┌─────────────────────────────────────────────────┐
│  1. Total de Visitantes                         │  ← Todos que acessaram
├─────────────────────────────────────────────────┤
│  2. Visualizaram Preços (viewedPricing)         │  ← Viram seção de planos
├─────────────────────────────────────────────────┤
│  3. Iniciaram Cadastro (startedCadastro)        │  ← Clicaram "Começar"
├─────────────────────────────────────────────────┤
│  4. Step 0 - Empresa (completedStep0_empresa)   │  ← Nome, CNPJ, etc.
├─────────────────────────────────────────────────┤
│  5. Step 1 - Localização (completedStep1_loc)   │  ← Endereço
├─────────────────────────────────────────────────┤
│  6. Step 2 - Acesso (completedStep2_acesso)     │  ← Login/senha
├─────────────────────────────────────────────────┤
│  7. Step 3 - Tema (completedStep3_tema)         │  ← Personalização
├─────────────────────────────────────────────────┤
│  8. Step 4 - Plano (completedStep4_plano)       │  ← Escolha do plano
├─────────────────────────────────────────────────┤
│  9. Cadastro Completo (completedCadastro)       │  ← Finalizado!
└─────────────────────────────────────────────────┘
```

**Cálculos importantes:**
- **Drop-off por etapa** = `(etapa_anterior - etapa_atual) / etapa_anterior × 100`
- **Taxa de conversão geral** = `completedCadastro / totalVisitors × 100`
- **Taxa de conclusão do formulário** = `completedCadastro / startedCadastro × 100`

---

## 7. Métricas em Tempo Real

### Estratégia de Polling

```
GET /api/v1/admin/analytics/realtime
Intervalo: 30 segundos
```

**Janelas temporais:**
- Visitantes ativos = últimos **5 minutos**
- Páginas ativas = últimos **5 minutos**
- Métricas agregadas = últimos **30 minutos**

### Formato dos Eventos Recentes

Os `recentEvents` são uma lista mista de conversões e interações, cada item contendo campos dinâmicos:

```json
// Evento de conversão
{
  "type": "cadastro_completed",
  "planName": "Profissional",
  "occurredAt": "2026-01-31T15:42:00"
}

// Evento de interação
{
  "type": "click_cta",
  "elementLabel": "Começar Grátis",
  "section": "hero",
  "occurredAt": "2026-01-31T15:40:30"
}
```

---

## 8. Guia de Implementação Frontend

### 8.1 Chamadas à API

```typescript
// Exemplo com fetch
const BASE = '/api/v1/admin/analytics';

async function fetchAnalytics(endpoint: string, startDate: string, endDate: string) {
  const params = new URLSearchParams({ start_date: startDate, end_date: endDate });
  const res = await fetch(`${BASE}/${endpoint}?${params}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return res.json();
}

// Uso
const overview    = await fetchAnalytics('overview', '2026-01-01', '2026-01-31');
const traffic     = await fetchAnalytics('traffic', '2026-01-01', '2026-01-31');
const behavior    = await fetchAnalytics('behavior', '2026-01-01', '2026-01-31');
const conversions = await fetchAnalytics('conversions', '2026-01-01', '2026-01-31');
const context     = await fetchAnalytics('context', '2026-01-01', '2026-01-31');

// Realtime (sem datas)
const realtime = await fetch(`${BASE}/realtime`, {
  headers: { 'Authorization': `Bearer ${token}` }
}).then(r => r.json());
```

### 8.2 Conversões de Unidade

```typescript
// Milissegundos → formato legível
function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  if (minutes === 0) return `${seconds}s`;
  return `${minutes}m ${seconds % 60}s`;
}

// Milissegundos → dias (para tempo de conversão)
function formatDays(ms: number): string {
  const days = Math.round(ms / 86400000);
  return days === 1 ? '1 dia' : `${days} dias`;
}

// Percentual formatado
function formatPercent(value: number): string {
  return `${value.toFixed(1)}%`;
}
```

### 8.3 Seletores de Período Recomendados

| Label         | start_date               | end_date         |
|---------------|--------------------------|------------------|
| Hoje          | `hoje`                   | `hoje`           |
| Últimos 7d    | `hoje - 7 dias`          | `hoje`           |
| Últimos 30d   | `hoje - 30 dias`         | `hoje`           |
| Este mês      | `primeiro dia do mês`    | `hoje`           |
| Mês passado   | `primeiro dia mês ant.`  | `último dia ant.`|
| Personalizado | seleção manual           | seleção manual   |

### 8.4 Cores para Web Vitals (Semáforo)

```typescript
type HealthStatus = 'good' | 'needs-improvement' | 'poor';

const WEB_VITALS_THRESHOLDS = {
  fcp:  { good: 1800, poor: 3000 },
  lcp:  { good: 2500, poor: 4000 },
  fid:  { good: 100,  poor: 300 },
  cls:  { good: 0.1,  poor: 0.25 },
  ttfb: { good: 800,  poor: 1800 },
};

function getHealthStatus(metric: string, value: number): HealthStatus {
  const t = WEB_VITALS_THRESHOLDS[metric];
  if (value <= t.good) return 'good';           // Verde
  if (value <= t.poor) return 'needs-improvement'; // Amarelo
  return 'poor';                                  // Vermelho
}
```

### 8.5 Estrutura de Rotas Sugerida

```
/admin/analytics
├── /overview      → Dashboard principal
├── /traffic       → Fontes de tráfego
├── /behavior      → Comportamento do usuário
├── /conversions   → Funil de conversão
├── /context       → Dispositivos, geo, performance, erros
└── /realtime      → Métricas em tempo real
```

---

## Referência Rápida - Mapeamento Endpoint → Página

| Página Admin       | Endpoint            | DTO Response            |
|---------------------|---------------------|-------------------------|
| Dashboard/Overview  | `GET /overview`     | `AnalyticsOverviewDTO`  |
| Tráfego             | `GET /traffic`      | `AnalyticsTrafficDTO`   |
| Comportamento       | `GET /behavior`     | `AnalyticsBehaviorDTO`  |
| Conversões          | `GET /conversions`  | `AnalyticsConversionsDTO` |
| Contexto Técnico    | `GET /context`      | `AnalyticsContextDTO`   |
| Tempo Real          | `GET /realtime`     | `AnalyticsRealtimeDTO`  |
