# API de Notificacoes Push

Documentacao completa do modulo de notificacoes push do Bellory.

---

## Endpoints

Base path: `/api/v1/notificacao-push`  
Autenticacao: JWT (todos os endpoints)

---

### 1. Listar Notificacoes

```
GET /api/v1/notificacao-push?page=0&size=20
```

**Query Params:**

| Param | Tipo | Default | Descricao |
|-------|------|---------|-----------|
| page  | int  | 0       | Pagina atual |
| size  | int  | 20      | Itens por pagina |

**Response 200:**

```json
{
  "success": true,
  "message": "...",
  "dados": {
    "content": [
      {
        "id": 1,
        "titulo": "Novo Agendamento",
        "descricao": "Novo agendamento de Joao Silva para 07/04/2026 as 09:00",
        "origem": "AGENDAMENTO",
        "detalhe": "Cliente: Joao Silva\nData: 07/04/2026\nHorario: 09:00\nServicos: Corte, Barba\nProfissional: Carlos\nValor: R$ 80.00",
        "prioridade": "MEDIA",
        "categoria": "SISTEMA",
        "lido": false,
        "icone": null,
        "urlAcao": "/agendamentos/42",
        "metadata": {
          "agendamentoId": 42,
          "clienteId": 10,
          "nomeCliente": "Joao Silva",
          "dtAgendamento": "2026-04-07T09:00:00",
          "servicos": "Corte, Barba",
          "profissional": "Carlos",
          "valorTotal": 80.00
        },
        "dtCadastro": "2026-04-05T14:30:00",
        "dtRead": null
      }
    ],
    "totalElements": 15,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

Ordenado por `dtCadastro` decrescente (mais recentes primeiro).

---

### 2. Contar Nao Lidas

```
GET /api/v1/notificacao-push/nao-lidas/count
```

**Response 200:**

```json
{
  "success": true,
  "message": "Contagem de notificacoes nao lidas",
  "dados": { "count": 5 }
}
```

---

### 3. Marcar como Lida

```
PUT /api/v1/notificacao-push/{id}/lida
```

**Response 200:** Retorna a notificacao atualizada com `lido: true` e `dtRead` preenchido.

**Response 404:** `"Notificacao nao encontrada"`  
**Response 403:** `"Acesso negado a esta notificacao"`

---

### 4. Deletar Notificacao

```
DELETE /api/v1/notificacao-push/{id}
```

**Response 200:** `"Notificacao deletada com sucesso"`  
**Response 404 / 403:** Mesmos erros acima.

---

## Estrutura do DTO (NotificacaoPushDTO)

| Campo       | Tipo                | Descricao |
|-------------|---------------------|-----------|
| id          | Long                | ID da notificacao |
| titulo      | String              | Titulo curto (para lista) |
| descricao   | String              | Resumo em uma linha (para lista/preview) |
| origem      | String              | Modulo de origem: `AGENDAMENTO`, `FINANCEIRO`, `CLIENTE`, `ESTOQUE` |
| detalhe     | String              | Descricao formatada com quebras de linha (para view de detalhe) |
| prioridade  | enum                | `BAIXA`, `MEDIA`, `ALTA` |
| categoria   | enum                | `CHAT`, `SISTEMA`, `TELEFONIA`, `AVISO` |
| lido        | Boolean             | Se foi lida |
| icone       | String              | Icone customizado (opcional) |
| urlAcao     | String              | Rota de navegacao no front-end |
| metadata    | Map<String, Object> | Dados estruturados para acoes no front-end (ver abaixo) |
| dtCadastro  | LocalDateTime       | Data/hora de criacao |
| dtRead      | LocalDateTime       | Data/hora da leitura (null se nao lida) |

---

## Eventos e Metadata por Tipo

### AGENDAMENTO - Novo Agendamento

**Quando:** Um agendamento e criado (interno ou externo/booking).  
**Roles notificadas:** `ROLE_ADMIN`, `ROLE_GERENTE`, `ROLE_RECEPCAO`  
**Prioridade:** MEDIA

**descricao:** `"Novo agendamento de {nomeCliente} para {dd/MM/yyyy} as {HH:mm}"`

**detalhe:**
```
Cliente: Joao Silva
Data: 07/04/2026
Horario: 09:00
Servicos: Corte, Barba
Profissional: Carlos
Valor: R$ 80.00
```

**metadata:**
```json
{
  "agendamentoId": 42,
  "clienteId": 10,
  "nomeCliente": "Joao Silva",
  "dtAgendamento": "2026-04-07T09:00:00",
  "servicos": "Corte, Barba",
  "profissional": "Carlos",
  "valorTotal": 80.00
}
```

**urlAcao:** `/agendamentos/{agendamentoId}`

---

### AGENDAMENTO - Agendamento Cancelado

**Quando:** Um agendamento e cancelado.  
**Roles notificadas:** Funcionarios responsaveis (`ROLE_FUNCIONARIO`) + `ROLE_ADMIN`  
**Prioridade:** ALTA

**descricao:** `"Agendamento de {nomeCliente} em {dd/MM/yyyy} as {HH:mm} foi cancelado"`

**detalhe:**
```
Cliente: Joao Silva
Data: 07/04/2026
Horario: 09:00
Servicos: Corte, Barba
Profissional: Carlos
```

**metadata:**
```json
{
  "agendamentoId": 42,
  "clienteId": 10,
  "nomeCliente": "Joao Silva",
  "dtAgendamento": "2026-04-07T09:00:00",
  "servicos": "Corte, Barba",
  "profissional": "Carlos"
}
```

**urlAcao:** `/agendamentos/{agendamentoId}`

---

### AGENDAMENTO - Agendamento Confirmado

**Quando:** Um agendamento muda para status CONFIRMADO.  
**Roles notificadas:** Funcionarios responsaveis (`ROLE_FUNCIONARIO`)  
**Prioridade:** MEDIA

**descricao:** `"Agendamento de {nomeCliente} em {dd/MM/yyyy} as {HH:mm} foi confirmado"`

**detalhe:**
```
Cliente: Joao Silva
Data: 07/04/2026
Horario: 09:00
Servicos: Corte, Barba
Profissional: Carlos
```

**metadata:**
```json
{
  "agendamentoId": 42,
  "clienteId": 10,
  "nomeCliente": "Joao Silva",
  "dtAgendamento": "2026-04-07T09:00:00",
  "servicos": "Corte, Barba",
  "profissional": "Carlos"
}
```

**urlAcao:** `/agendamentos/{agendamentoId}`

---

### FINANCEIRO - Pagamento Recebido

**Quando:** Um pagamento e registrado.  
**Roles notificadas:** `ROLE_ADMIN`, `ROLE_GERENTE`  
**Prioridade:** MEDIA

**descricao:** `"Pagamento de R$ {valor} recebido de {nomeCliente}"`

**detalhe:**
```
Cliente: Joao Silva
Valor: R$ 80.00
Forma: PIX
```

**metadata:**
```json
{
  "pagamentoId": 5,
  "clienteId": 10,
  "nomeCliente": "Joao Silva",
  "valor": 80.00,
  "formaPagamento": "PIX"
}
```

**urlAcao:** `/financeiro`

---

### CLIENTE - Novo Cliente Cadastrado

**Quando:** Um novo cliente e cadastrado no sistema.  
**Roles notificadas:** `ROLE_ADMIN`  
**Prioridade:** BAIXA

**descricao:** `"Novo cliente cadastrado: {nomeCliente}"`

**detalhe:**
```
Nome: Maria Santos
Telefone: (32) 99822-0083
Email: maria@email.com
```

**metadata:**
```json
{
  "clienteId": 15,
  "nomeCliente": "Maria Santos",
  "telefone": "(32) 99822-0083",
  "email": "maria@email.com"
}
```

**urlAcao:** `/clientes/{clienteId}`

---

### ESTOQUE - Estoque Baixo

**Quando:** A quantidade de um produto fica abaixo do minimo.  
**Roles notificadas:** `ROLE_ADMIN`, `ROLE_GERENTE`  
**Prioridade:** ALTA  
**Categoria:** AVISO

**descricao:** `"Produto \"{nomeProduto}\" com apenas {qtd} unidades"`

**detalhe:**
```
Produto: Shampoo Profissional
Quantidade atual: 3 unidades
```

**metadata:**
```json
{
  "produtoId": 7,
  "nomeProduto": "Shampoo Profissional",
  "quantidadeAtual": 3
}
```

**urlAcao:** `/produtos/{produtoId}`

---

## Guia de Uso no Front-End

### Badge de notificacoes (sino)

Polling periodico em `GET /nao-lidas/count` para atualizar o contador.

### Lista/dropdown de notificacoes

`GET /` com paginacao. Usar `descricao` para preview curto na lista.

### Detalhe da notificacao

Ao expandir/clicar, mostrar o campo `detalhe` (texto formatado com `\n`).

### Navegacao por acao

Ao clicar na notificacao, usar `urlAcao` para redirecionar:
- `/agendamentos/42` -> tela do agendamento
- `/clientes/15` -> tela do cliente
- `/financeiro` -> tela financeira
- `/produtos/7` -> tela do produto

### Acoes usando metadata

O campo `metadata` fornece IDs e dados estruturados para acoes no front-end:

```javascript
// Exemplo: navegar para o agendamento
const agendamentoId = notificacao.metadata?.agendamentoId;
if (agendamentoId) {
  router.push(`/agendamentos/${agendamentoId}`);
}

// Exemplo: abrir perfil do cliente
const clienteId = notificacao.metadata?.clienteId;
if (clienteId) {
  router.push(`/clientes/${clienteId}`);
}

// Exemplo: mostrar valor na UI
const valor = notificacao.metadata?.valorTotal;
```

### Styling por prioridade

| Prioridade | Sugestao visual |
|------------|-----------------|
| BAIXA      | Cinza / sem destaque |
| MEDIA      | Azul / normal |
| ALTA       | Vermelho / badge de destaque |

### Styling por categoria

| Categoria  | Sugestao de icone |
|------------|-------------------|
| SISTEMA    | Icone de engrenagem / sino |
| AVISO      | Icone de alerta / triangulo |
| CHAT       | Icone de mensagem |
| TELEFONIA  | Icone de telefone |

### Marcar como lida

`PUT /{id}/lida` ao clicar/visualizar a notificacao. Usar `lido` e `dtRead` para diferenciar visualmente.

### Deletar

`DELETE /{id}` no swipe ou botao de remover.

---

## Enums

### PrioridadeNotificacao
- `BAIXA`
- `MEDIA`
- `ALTA`

### CategoriaNotificacao
- `CHAT`
- `SISTEMA`
- `TELEFONIA`
- `AVISO`

### Origens (String)
- `AGENDAMENTO`
- `FINANCEIRO`
- `CLIENTE`
- `ESTOQUE`
