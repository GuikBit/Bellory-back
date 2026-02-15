# API de Horario de Funcionamento

Base URL: `/api/v1/organizacao/horario-funcionamento`

Todos os endpoints requerem autenticacao JWT. A organizacao e identificada pelo token.

---

## 1. Listar horarios de funcionamento

```
GET /api/v1/organizacao/horario-funcionamento
```

**Response (200):**
```json
{
  "success": true,
  "message": "Horarios de funcionamento recuperados com sucesso.",
  "dados": [
    {
      "id": 1,
      "diaSemana": "SEGUNDA",
      "diaSemanaLabel": "Segunda-feira",
      "ativo": true,
      "periodos": [
        { "horaInicio": "09:00", "horaFim": "12:00" },
        { "horaInicio": "14:00", "horaFim": "18:00" }
      ]
    },
    {
      "id": 2,
      "diaSemana": "TERCA",
      "diaSemanaLabel": "Terca-feira",
      "ativo": true,
      "periodos": [
        { "horaInicio": "09:00", "horaFim": "18:00" }
      ]
    },
    {
      "id": 7,
      "diaSemana": "DOMINGO",
      "diaSemanaLabel": "Domingo",
      "ativo": false,
      "periodos": []
    }
  ]
}
```

> Na primeira chamada, se nenhum horario existir, os 7 dias sao criados automaticamente (todos inativos, sem periodos).

---

## 2. Atualizar todos os dias (bulk update)

```
PUT /api/v1/organizacao/horario-funcionamento
```

**Request Body:**
```json
[
  {
    "diaSemana": "SEGUNDA",
    "ativo": true,
    "periodos": [
      { "horaInicio": "09:00", "horaFim": "12:00" },
      { "horaInicio": "14:00", "horaFim": "18:00" }
    ]
  },
  {
    "diaSemana": "TERCA",
    "ativo": true,
    "periodos": [
      { "horaInicio": "09:00", "horaFim": "18:00" }
    ]
  },
  {
    "diaSemana": "QUARTA",
    "ativo": true,
    "periodos": [
      { "horaInicio": "09:00", "horaFim": "18:00" }
    ]
  },
  {
    "diaSemana": "QUINTA",
    "ativo": true,
    "periodos": [
      { "horaInicio": "09:00", "horaFim": "18:00" }
    ]
  },
  {
    "diaSemana": "SEXTA",
    "ativo": true,
    "periodos": [
      { "horaInicio": "09:00", "horaFim": "18:00" }
    ]
  },
  {
    "diaSemana": "SABADO",
    "ativo": true,
    "periodos": [
      { "horaInicio": "09:00", "horaFim": "13:00" }
    ]
  },
  {
    "diaSemana": "DOMINGO",
    "ativo": false,
    "periodos": []
  }
]
```

**Response (200):** Mesmo formato da listagem.

---

## 3. Atualizar um dia especifico

```
PUT /api/v1/organizacao/horario-funcionamento/{diaSemana}
```

**Path parameter:** `diaSemana` = `SEGUNDA`, `TERCA`, `QUARTA`, `QUINTA`, `SEXTA`, `SABADO` ou `DOMINGO`

**Request Body:**
```json
{
  "diaSemana": "SEGUNDA",
  "ativo": true,
  "periodos": [
    { "horaInicio": "08:00", "horaFim": "12:00" },
    { "horaInicio": "13:00", "horaFim": "17:00" }
  ]
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Horario de funcionamento atualizado com sucesso.",
  "dados": {
    "id": 1,
    "diaSemana": "SEGUNDA",
    "diaSemanaLabel": "Segunda-feira",
    "ativo": true,
    "periodos": [
      { "horaInicio": "08:00", "horaFim": "12:00" },
      { "horaInicio": "13:00", "horaFim": "17:00" }
    ]
  }
}
```

> Este endpoint faz **upsert**: se o dia nao existir, cria; se existir, substitui os periodos.

---

## 4. Ativar/desativar um dia

```
PATCH /api/v1/organizacao/horario-funcionamento/{diaSemana}/status?ativo=true
```

**Query parameter:** `ativo` = `true` ou `false`

**Response (200):**
```json
{
  "success": true,
  "message": "Status do dia atualizado com sucesso.",
  "dados": {
    "id": 1,
    "diaSemana": "SEGUNDA",
    "diaSemanaLabel": "Segunda-feira",
    "ativo": true,
    "periodos": [
      { "horaInicio": "09:00", "horaFim": "12:00" },
      { "horaInicio": "14:00", "horaFim": "18:00" }
    ]
  }
}
```

---

## Codigos de erro

| Codigo | Descricao |
|--------|-----------|
| 400    | Dia da semana invalido ou dados invalidos |
| 403    | Token invalido ou organizacao nao identificada |
| 404    | Horario nao encontrado para o dia (no PATCH) |
| 500    | Erro interno |
