# Fila de Espera — Integração com N8N

Doc complementar ao [`FilaEspera.md`](./FilaEspera.md). Este arquivo descreve **como o agente do N8N deve consumir a fila de espera** — o backend já está pronto (PRs 1–5), mas o agente WhatsApp ainda não foi atualizado pra reconhecer o fluxo. Quando for evoluir o bot, este doc é a referência.

> **Princípio**: o N8N consulta o banco direto (mais leve que webhook). Os endpoints HTTP existem como fallback / pra testes manuais.

---

## 1. Visão geral do fluxo

```
┌─────────────────────────────────────────────────────────────────────┐
│  Cliente A cancela agendamento                                       │
│         │                                                            │
│         ▼                                                            │
│  AgendamentoCanceladoEvent (backend)                                 │
│         │                                                            │
│         ▼                                                            │
│  FilaEsperaListener — busca candidato FIFO compatível                │
│         │                                                            │
│         ▼                                                            │
│  Cria fila_espera_tentativa (PENDENTE) + publica FilaOfertaCriada    │
│         │                                                            │
│         ▼                                                            │
│  FilaEsperaDispatchService → Evolution API → WhatsApp do cliente B   │
│         │                                                            │
│         ▼                                                            │
│  Tentativa vira AGUARDANDO_RESPOSTA (dt_envio + dt_expira definidos) │
│         │                                                            │
│         ▼                                                            │
│ ┌──────────────────────────────────────────────────────────────┐    │
│ │ Cliente B responde via WhatsApp                              │    │
│ │         │                                                    │    │
│ │         ▼                                                    │    │
│ │ N8N recebe MESSAGES_UPSERT                                   │    │
│ │         │                                                    │    │
│ │         ▼                                                    │    │
│ │ N8N consulta banco para identificar contexto                 │    │
│ │         │                                                    │    │
│ │   ┌─────┴───────┬──────────────┬─────────────────┐           │    │
│ │   ▼             ▼              ▼                 ▼           │    │
│ │ confirmacao  fila SIM/NAO   fila tarde      sem contexto     │    │
│ │ pendente     ainda ativa   (já aceitou      → roteia pra     │    │
│ │              (ACEITO/      outro/expirou)    agente normal   │    │
│ │              RECUSADO)     → "perdeu a vez"                  │    │
│ └──────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Tabela `app.fila_espera_tentativa`

Schema completo (V65):

| Coluna | Tipo | Notas |
|---|---|---|
| `id` | BIGSERIAL PK | identificador da tentativa |
| `organizacao_id` | BIGINT NOT NULL | multi-tenancy |
| `agendamento_id` | BIGINT NOT NULL | agendamento do cliente que recebeu a oferta |
| `agendamento_cancelado_id` | BIGINT NOT NULL | raiz da cadeia (cancelamento original) |
| `funcionario_id` | BIGINT NOT NULL | funcionário do slot |
| `notificacao_enviada_id` | BIGINT NULL | reservado para futuro (não usado em PR 4) |
| `cascata_origem_id` | BIGINT NULL | tentativa anterior na cadeia (quando cascata > 1) |
| `cascata_nivel` | INTEGER | 1..filaMaxCascata (default 5) |
| `slot_inicio` | TIMESTAMP | início da nova vaga oferecida |
| `slot_fim` | TIMESTAMP | fim da vaga (slot_inicio + duração total dos serviços) |
| `status` | VARCHAR(30) | enum `StatusFilaTentativa` |
| `dt_envio` | TIMESTAMP NULL | quando o WhatsApp foi enviado |
| `dt_resposta` | TIMESTAMP NULL | quando cliente respondeu |
| `dt_expira` | TIMESTAMP NOT NULL | timeout (dt criação + filaTimeoutMinutos, default 30min) |
| `dt_criacao` | TIMESTAMP | created |
| `dt_atualizacao` | TIMESTAMP | updated |

### Estados (`StatusFilaTentativa`)

```
PENDENTE              ─► tentativa criada, ainda não disparou WhatsApp
ENVIADO               ─► usado em fluxos futuros, hoje vai direto para AGUARDANDO_RESPOSTA
AGUARDANDO_RESPOSTA   ─► cliente recebeu, aguardando SIM/NAO
ACEITO                ─► cliente respondeu SIM, agendamento foi adiantado
RECUSADO              ─► cliente respondeu NAO, mantido na fila
EXPIRADO              ─► 30min sem resposta, próximo da fila foi notificado
SUPERADO              ─► outro cliente aceitou antes / agendamento alvo cancelado
FALHA                 ─► erro no envio (Evolution API caiu, telefone inválido, etc.)
```

---

## 3. Como o N8N decide o contexto da resposta

O cliente pode responder algo enquanto **3 tipos de notificações** estão pendentes simultaneamente:

1. **CONFIRMACAO** (já existente em `notificacao_enviada`) — tabelas `notificacao_enviada` com `status = AGUARDANDO_RESPOSTA`
2. **FILA_ESPERA** (nova) — `fila_espera_tentativa` com `status = AGUARDANDO_RESPOSTA`
3. **REAGENDAMENTO** (já existente) — `notificacao_enviada` com `status = AGUARDANDO_DATA` ou `AGUARDANDO_HORARIO`

### Estratégia: **a mais recente vence**

Quando o N8N receber uma mensagem do cliente, deve consultar todas as 3 fontes e usar a com `dt_envio` mais recente. Query unificada sugerida:

```sql
WITH contexto AS (
    -- Fila de espera (nova)
    SELECT
        'FILA_ESPERA'  AS tipo,
        t.id           AS contexto_id,
        t.dt_envio,
        t.agendamento_id,
        ag.cliente_id,
        c.telefone
    FROM app.fila_espera_tentativa t
    JOIN app.agendamento ag ON ag.id = t.agendamento_id
    JOIN app.cliente c       ON c.id  = ag.cliente_id
    WHERE t.status = 'AGUARDANDO_RESPOSTA'
      AND c.telefone = :telefone

    UNION ALL

    -- Confirmação / reagendamento (existente)
    SELECT
        n.tipo::text   AS tipo,
        n.id           AS contexto_id,
        n.dt_envio,
        n.agendamento_id,
        ag.cliente_id,
        c.telefone
    FROM app.notificacao_enviada n
    JOIN app.agendamento ag ON ag.id = n.agendamento_id
    JOIN app.cliente c       ON c.id  = ag.cliente_id
    WHERE n.status IN ('AGUARDANDO_RESPOSTA','AGUARDANDO_DATA','AGUARDANDO_HORARIO')
      AND c.telefone = :telefone
)
SELECT *
FROM contexto
ORDER BY dt_envio DESC
LIMIT 1;
```

Se vier `tipo = 'FILA_ESPERA'`, o N8N segue o fluxo desta doc. Senão, mantém o fluxo existente.

---

## 4. Processamento da resposta de fila

### 4.1 Identificar a tentativa ativa pelo telefone

```sql
SELECT t.id, t.status, t.dt_expira, t.slot_inicio, t.slot_fim,
       ag.id AS agendamento_id, ag.dt_agendamento AS data_atual,
       c.nome_completo
FROM app.fila_espera_tentativa t
JOIN app.agendamento ag ON ag.id = t.agendamento_id
JOIN app.cliente c       ON c.id  = ag.cliente_id
WHERE c.telefone = :telefone
  AND t.status = 'AGUARDANDO_RESPOSTA'
ORDER BY t.dt_envio DESC
LIMIT 1;
```

Se não vier nada → cliente respondeu tarde, ver seção 4.4.

### 4.2 Cliente respondeu SIM

Chamar o backend:

```http
POST /api/v1/webhook/fila-espera/tentativa/{id}/aceitar
```

Resposta esperada (200):
```json
{
  "success": true,
  "message": "Agendamento adiantado com sucesso.",
  "dados": { /* AgendamentoDTO atualizado */ }
}
```

Possíveis respostas:
- **404** — tentativa não existe
- **409** — tentativa já finalizada (cliente respondeu tarde, mostrar mensagem "perdeu a vez")
- **500** — erro genérico

Mensagem para o cliente em caso de sucesso:
```
Show, {{nome}}! ✅
Seu agendamento foi adiantado para *{{slot_inicio}}*.
Te esperamos!
```

### 4.3 Cliente respondeu NAO

```http
POST /api/v1/webhook/fila-espera/tentativa/{id}/recusar
```

Mensagem:
```
Sem problema, {{nome}}!
Seu horário original em *{{data_atual}}* segue mantido.
```

O backend automaticamente passa a oferta para o próximo da fila.

### 4.4 Cliente respondeu tarde / "perdeu a vez"

Caso a query da seção 4.1 não retorne nada, mas o cliente claramente está respondendo a uma oferta de fila (ex: respondeu "SIM"/"NÃO" sem outro contexto), buscar a última tentativa **finalizada** pra entender:

```sql
SELECT t.id, t.status, t.slot_inicio, ag.dt_agendamento
FROM app.fila_espera_tentativa t
JOIN app.agendamento ag ON ag.id = t.agendamento_id
JOIN app.cliente c       ON c.id  = ag.cliente_id
WHERE c.telefone = :telefone
  AND t.status IN ('EXPIRADO','SUPERADO','RECUSADO')
  AND t.dt_atualizacao > now() - INTERVAL '2 hours'
ORDER BY t.dt_atualizacao DESC
LIMIT 1;
```

Se vier registro:
- `status = SUPERADO` ou `EXPIRADO` → mensagem "perdeu a vez":
  ```
  Oi, {{nome}}! 💔
  Infelizmente o horário das *{{slot_inicio}}* já foi para outro cliente.
  Mas seu agendamento original em *{{data_atual}}* continua ativo!
  ```
- `status = RECUSADO` → cliente respondendo "SIM" depois de já ter respondido "NÃO" — pode ignorar ou enviar lembrete amigável.

### 4.5 Endpoint de status (alternativa explícita)

Em vez de fazer a query da 4.4, o N8N pode chamar:

```http
GET /api/v1/webhook/fila-espera/tentativa/{id}/status
```

Retorna o `FilaEsperaTentativaDTO` completo. Mais explícito, mas requer que o N8N saiba o `id` (precisa ter guardado quando enviou a oferta).

---

## 5. Templates de mensagem

### 5.1 Oferta inicial (já enviada pelo backend, hardcoded em `FilaEsperaDispatchService`)

```
Oi, {{nome}}! 💖

Boa noticia: surgiu um horario antes do seu agendamento na *{{nome_org}}*.

📅 Seu horario atual: *{{data_atual}}*
✨ Vaga disponivel: *{{slot_inicio}}*

Quer adiantar? Responda:
*SIM* — quero adiantar
*NAO* — vou manter o original

Voce tem 30 minutos para responder. Apos esse tempo, a vaga e oferecida ao proximo da fila.
```

### 5.2 Aceite confirmado (N8N envia)

```
Show, {{nome}}! ✅
Seu agendamento foi adiantado para *{{slot_inicio}}*.
Te esperamos!
```

### 5.3 Recusa (N8N envia)

```
Sem problema, {{nome}}!
Seu horário original em *{{data_atual}}* segue mantido.
```

### 5.4 Perdeu a vez (N8N envia)

```
Oi, {{nome}}! 💔
O horário das *{{slot_inicio}}* já foi pra outro cliente.
Mas seu agendamento original em *{{data_atual}}* continua ativo!
```

---

## 6. Endpoints HTTP do backend

| Método | Path | Uso |
|---|---|---|
| `POST` | `/api/v1/webhook/fila-espera/tentativa/{id}/aceitar` | Cliente aceita oferta |
| `POST` | `/api/v1/webhook/fila-espera/tentativa/{id}/recusar` | Cliente recusa oferta |
| `GET`  | `/api/v1/webhook/fila-espera/tentativa/{id}/status` | Consulta estado da tentativa |
| `GET`  | `/api/v1/webhook/fila-espera/agendamento/{agendamentoId}` | Histórico de tentativas do agendamento |

> Por padrão exigem JWT (não estão no `permitlist` do `SecurityConfig`). Quando integrar com N8N de fato, decidir se expõe via apikey próprio (igual `/webhook/assas`, `/webhook/payment`) ou se o N8N usa um JWT de serviço.

---

## 7. Pontos de configuração (admin)

A organização precisa habilitar a feature em `app.config_sistema`:

| Coluna | Default | Significado |
|---|---|---|
| `usar_fila_espera` | `false` | feature flag por org |
| `fila_max_cascata` | `5` | máximo de reagendamentos em cadeia por evento |
| `fila_timeout_minutos` | `30` | quanto tempo cliente tem pra responder |
| `fila_antecedencia_horas` | `3` | só oferta slots ≥ now + Xh |

---

## 8. Edge cases que o N8N precisa saber

1. **Cliente responde algo que não é SIM/NÃO** (ex: "talvez", emoji 🤔):
   - Se houver tentativa ativa → resposta padrão "Por favor, responda SIM ou NÃO".
   - Cooldown de 1 min antes de reenviar.

2. **Cliente responde SIM duas vezes**:
   - Primeira chama `/aceitar`, retorna 200.
   - Segunda chama `/aceitar`, retorna 409 (já finalizada).
   - Mensagem: "Seu adiantamento já foi confirmado, te esperamos!"

3. **Cliente envia mensagem aleatória sem contexto**:
   - Query da seção 3 retorna vazio → roteia pra agente principal.

4. **Tentativa expira enquanto cliente está digitando**:
   - Cliente responde "SIM" às 30:01.
   - Backend já marcou EXPIRADO às 30:00 e ofertou pra próximo.
   - Chamada `/aceitar` retorna 409 → mensagem "perdeu a vez" (seção 4.4).

5. **Cliente cancela o próprio agendamento durante uma oferta ativa**:
   - Backend marca a tentativa como SUPERADO automaticamente (cleanup em `FilaEsperaListener`).
   - Se cliente responder depois → seção 4.4.

---

## 9. Manual test plan

Como o projeto não tem infraestrutura de testes Java configurada, este é o plano de smoke test manual a executar antes de declarar a feature pronta em produção:

### 9.1 Setup pré-teste

1. Habilitar feature na org de teste:
   ```sql
   UPDATE app.config_sistema
   SET usar_fila_espera = true,
       fila_max_cascata = 5,
       fila_timeout_minutos = 30,
       fila_antecedencia_horas = 3
   WHERE organizacao_id = :org_teste;
   ```
2. Confirmar Evolution API conectada e instância da org com `status` = `CONNECTED`/`OPEN`.

### 9.2 Cenários funcionais

| # | Cenário | Esperado |
|---|---|---|
| 1 | Criar agendamento com `entrarFilaEspera = true` no DTO de criação | `app.agendamento.entrou_fila_espera = true` |
| 2 | Criar 2º agendamento (cliente B) `entrouFilaEspera = true` em data anterior | OK, dois agendamentos na fila |
| 3 | Cliente A cancela seu agendamento (data posterior) | Listener acha cliente B, cria tentativa, dispara WhatsApp |
| 4 | Verificar `app.fila_espera_tentativa` | 1 row, `status = AGUARDANDO_RESPOSTA`, `dt_envio` setado |
| 5 | POST `/aceitar/{id}` | Agendamento de B atualizado pra slot novo, `reagendado_por_fila = true`, push enviado |
| 6 | POST `/aceitar/{id}` 2x seguidas | 1ª: 200, 2ª: 409 |
| 7 | POST `/recusar/{id}` | tentativa RECUSADO, próximo da fila notificado (se houver) |
| 8 | Aguardar 30min sem responder | Scheduler marca EXPIRADO, próximo da fila notificado |

### 9.3 Cenários de cascata

| # | Cenário | Esperado |
|---|---|---|
| 9 | 6 clientes na fila com agendamentos posteriores; 1 cancelado | 5 cascatas (`cascata_nivel` 1→5), 6º slot vago fica livre |
| 10 | Validar `cascata_origem_id` | aponta pra tentativa anterior na cadeia |
| 11 | `cascata_nivel = 5` aceito | sem nova tentativa criada (limite atingido) |

### 9.4 Cenários de slot blocking

| # | Cenário | Esperado |
|---|---|---|
| 12 | Tentativa ativa em slot X; cliente C tenta agendar via UI no mesmo slot | Slot não aparece em `getHorariosDisponiveis` |
| 13 | Cliente C tenta POST direto pra criar agendamento naquele slot | `IllegalArgumentException`: "horario reservado para a fila de espera" |
| 14 | Tentativa expira sem aceite e sem próximo da fila | Slot volta a aparecer pra agendamento normal |

### 9.5 Cenários de regra das 3h

| # | Cenário | Esperado |
|---|---|---|
| 15 | Cancelamento abre slot < 3h do agora | Listener loga "muito proximo" e não oferta |
| 16 | Cancelamento abre slot >= 3h | Tentativa criada normalmente |

### 9.6 Cenários de cleanup

| # | Cenário | Esperado |
|---|---|---|
| 17 | Cliente B (com tentativa ativa) cancela próprio agendamento | tentativa vira SUPERADO, próximo da fila notificado |
| 18 | Chega o dia do agendamento B com `entrou_fila_espera = true` | scheduler diário (00:00) zera flag |

---

## 10. Roadmap de evolução do agente N8N

Sequência sugerida pra implementar:

1. **Adicionar a query unificada (seção 3)** no nó "Verificar Pendente" do bot principal — assim ele já roteia certo entre confirmação/fila/reagendamento.
2. **Criar sub-fluxo "Resposta Fila"** com dois branches: SIM (POST aceitar) e NÃO (POST recusar). Adicionar template de retorno (seção 5).
3. **Detectar "perdeu a vez"** consultando tentativas finalizadas recentes (seção 4.4) quando query principal não retornar nada.
4. **Templates customizáveis por org**: hoje a oferta inicial está hardcoded em `FilaEsperaDispatchService.montarMensagem()`. Migrar para `TemplateBellory` com nova `CategoriaTemplate.FILA_ESPERA` (PR de melhoria).
5. **Notificar "perdeu a vez" automaticamente**: hoje o cliente só recebe se mandar mensagem. Backend pode disparar essa mensagem quando uma tentativa vira SUPERADO/EXPIRADO (PR de melhoria).
