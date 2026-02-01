# Fluxo de Confirmação de Agendamento - N8N

## Visão Geral

Este fluxo é responsável por gerenciar a confirmação de agendamentos via WhatsApp. Ele processa três tipos de resposta:

- **SIM**: Confirma o agendamento (status → CONFIRMADO)
- **NAO**: Cancela o agendamento (status → CANCELADO)
- **REAGENDAR**: Inicia fluxo de reagendamento com busca de disponibilidade

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              FLUXO N8N                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐               │
│  │   Webhook    │───►│    Enviar    │───►│   Responder  │               │
│  │   Backend    │    │   WhatsApp   │    │   Webhook    │               │
│  └──────────────┘    └──────────────┘    └──────────────┘               │
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐               │
│  │   Webhook    │───►│   Verificar  │───►│  Processar   │               │
│  │   Resposta   │    │   Pendente   │    │   Resposta   │               │
│  └──────────────┘    └──────────────┘    └──────────────┘               │
│                              │                   │                       │
│                              ▼                   ▼                       │
│                      ┌──────────────┐    ┌──────────────┐               │
│                      │   Ignorar    │    │    Switch    │               │
│                      │  (se não há  │    │   SIM/NAO/   │               │
│                      │  pendente)   │    │  REAGENDAR   │               │
│                      └──────────────┘    └──────────────┘               │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Webhooks

### 1. Disparo do Backend
- **URL**: `https://auto.bellory.com.br/webhook/bellory-confirmacao`
- **Método**: POST
- **Payload**:
```json
{
  "instanceName": "nome-da-instancia",
  "telefone": "5511999999999",
  "mensagem": "Olá! Seu agendamento para amanhã às 14:00 está confirmado?",
  "agendamentoId": 123,
  "tipoNotificacao": "CONFIRMACAO"
}
```

### 2. Resposta do WhatsApp (via Evolution API)
- **URL**: `https://auto.bellory.com.br/webhook/bellory-resposta-confirmacao`
- **Método**: POST
- **Eventos**: `MESSAGES_UPSERT`

### 3. Nova Data (Reagendamento)
- **URL**: `https://auto.bellory.com.br/webhook/bellory-reagendar-data`
- **Método**: POST

### 4. Seleção de Horário
- **URL**: `https://auto.bellory.com.br/webhook/bellory-selecionar-horario`
- **Método**: POST

## Variáveis de Ambiente Necessárias

```env
# API Bellory (Backend)
BELLORY_API_URL=https://api.bellory.com.br

# Evolution API (WhatsApp)
EVOLUTION_API_URL=https://wa.bellory.com.br
EVOLUTION_API_KEY=sua-api-key-aqui
```

## Endpoints do Backend

### Verificar Confirmação Pendente
```
GET /api/webhook/confirmacao-pendente/{telefone}
Header: X-Instance-Name: nome-da-instancia
```

**Resposta**:
```json
{
  "temConfirmacaoPendente": true,
  "agendamentoId": 123,
  "notificacaoId": 456,
  "telefone": "5511999999999",
  "instanceName": "minha-instancia",
  "clienteNome": "João Silva",
  "funcionarioId": 1,
  "servicoIds": [1, 2],
  "organizacaoId": 1
}
```

### Verificar Aguardando Data
```
GET /api/webhook/confirmacao-aguardando-data/{telefone}
Header: X-Instance-Name: nome-da-instancia
```

### Verificar Aguardando Horário
```
GET /api/webhook/confirmacao-aguardando-horario/{telefone}
Header: X-Instance-Name: nome-da-instancia
```

### Marcar Aguardando Data
```
PATCH /api/webhook/confirmacao/{notificacaoId}/aguardando-data
```

### Marcar Aguardando Horário
```
PATCH /api/webhook/confirmacao/{notificacaoId}/aguardando-horario
Body: {
  "dataDesejada": "2026-02-15",
  "horariosDisponiveis": [
    {"horaInicio": "09:00", "horaFim": "10:00"},
    {"horaInicio": "14:00", "horaFim": "15:00"}
  ]
}
```

### Finalizar Confirmação
```
PATCH /api/webhook/confirmacao/{notificacaoId}/concluida
```

## Fluxo de Mensagens Ignoradas

Para evitar processar mensagens aleatórias quando não há confirmação pendente:

1. O N8N verifica no backend se existe confirmação pendente para o telefone
2. Se não existir, a mensagem é ignorada silenciosamente
3. Se existir, processa a resposta

**Estados que indicam mensagem pendente**:
- `AGUARDANDO_RESPOSTA`: Aguardando SIM/NAO/REAGENDAR
- `AGUARDANDO_DATA`: Aguardando cliente informar data
- `AGUARDANDO_HORARIO`: Aguardando cliente selecionar horário

## Fluxo de Reagendamento

1. Cliente responde "REAGENDAR"
2. N8N pergunta nova data desejada
3. Cliente informa data (DD/MM/AAAA)
4. N8N busca disponibilidade via API: `POST /api/agendamento/disponibilidade`
5. N8N apresenta horários disponíveis numerados
6. Cliente seleciona número do horário
7. N8N chama `PATCH /api/agendamento/{id}/reagendar`
8. Confirmação enviada ao cliente

## Como Importar no N8N

1. Acesse seu N8N
2. Vá em **Workflows** → **Import from File**
3. Selecione o arquivo `fluxo-confirmacao-agendamento.json`
4. Configure as credenciais HTTP Header Auth
5. Configure as variáveis de ambiente
6. Ative o workflow

## Tabela de Estados (StatusEnvio)

| Status | Descrição |
|--------|-----------|
| PENDENTE | Aguardando envio |
| ENVIADO | Enviado para o N8N |
| AGUARDANDO_RESPOSTA | Aguardando SIM/NAO/REAGENDAR |
| AGUARDANDO_DATA | Aguardando data para reagendamento |
| AGUARDANDO_HORARIO | Aguardando seleção de horário |
| CONFIRMADO | Cliente confirmou (SIM) |
| CANCELADO_CLIENTE | Cliente cancelou (NAO) |
| REAGENDADO | Reagendamento concluído |
| EXPIRADO | Tempo de resposta expirou |
| FALHA | Erro no envio |
| CANCELADO | Agendamento cancelado antes do envio |

## Troubleshooting

### Mensagens não estão sendo processadas
1. Verifique se o webhook da Evolution API está configurado corretamente
2. Verifique se a instância está conectada
3. Verifique os logs do N8N

### Erro "Nenhuma confirmação pendente"
1. Verifique se a notificação foi criada com status `AGUARDANDO_RESPOSTA`
2. Verifique se o telefone está no formato correto (apenas números)
3. Verifique se o `instanceName` está correto

### Reagendamento falha
1. Verifique se o funcionário tem disponibilidade na data
2. Verifique se os serviços estão ativos
3. Verifique logs da API de disponibilidade
