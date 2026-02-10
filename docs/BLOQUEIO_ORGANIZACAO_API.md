# Bellory - API de Feriados e Bloqueios da Organizacao

## Descricao

Esta API permite gerenciar feriados nacionais e bloqueios personalizados (dias de folga, eventos, etc.) a nivel da organizacao. Quando um dia esta bloqueado, nenhum agendamento pode ser criado ou alterado para essa data. A lista de dias bloqueados deve ser utilizada pelo front-end para bloquear os dias no calendario de agendamento.

## Base URL

```
/api/bloqueio-organizacao
```

> **Autenticacao:** Todos os endpoints requerem JWT valido no header `Authorization: Bearer {token}`.
> A organizacao e identificada automaticamente pelo token.

---

## Envelope de Resposta Padrao

Todas as respostas seguem o formato `ResponseAPI<T>`:

```json
{
  "success": true,
  "message": "Mensagem descritiva",
  "dados": { },
  "errorCode": null,
  "errors": null
}
```

---

## Tipos de Dados

### BloqueioOrganizacaoDTO (Resposta)

```typescript
interface BloqueioOrganizacao {
  id: number
  titulo: string
  dataInicio: string        // formato: "yyyy-MM-dd"
  dataFim: string           // formato: "yyyy-MM-dd"
  tipo: "FERIADO" | "BLOQUEIO"
  descricao: string | null
  ativo: boolean
  origem: "NACIONAL" | "MANUAL"
  anoReferencia: number | null
  dtCriacao: string         // formato: "yyyy-MM-dd'T'HH:mm:ss"
  dtAtualizacao: string | null
}
```

### BloqueioOrganizacaoCreateDTO (Criacao)

```typescript
interface BloqueioOrganizacaoCreate {
  titulo: string            // obrigatorio
  dataInicio: string        // obrigatorio, formato: "yyyy-MM-dd"
  dataFim: string | null    // opcional (se null, usa dataInicio)
  tipo: "FERIADO" | "BLOQUEIO"  // opcional (default: "BLOQUEIO")
  descricao: string | null  // opcional
}
```

### BloqueioOrganizacaoUpdateDTO (Atualizacao)

```typescript
interface BloqueioOrganizacaoUpdate {
  titulo: string | null
  dataInicio: string | null   // formato: "yyyy-MM-dd"
  dataFim: string | null      // formato: "yyyy-MM-dd"
  tipo: "FERIADO" | "BLOQUEIO" | null
  descricao: string | null
  ativo: boolean | null
}
```

---

## Endpoints

### 1. Listar Todos os Bloqueios

```
GET /api/bloqueio-organizacao
```

Retorna todos os bloqueios/feriados da organizacao (ativos e inativos).

**Resposta (200):**
```json
{
  "success": true,
  "message": "Bloqueios recuperados com sucesso.",
  "dados": [
    {
      "id": 1,
      "titulo": "Ano Novo",
      "dataInicio": "2025-01-01",
      "dataFim": "2025-01-01",
      "tipo": "FERIADO",
      "descricao": "Feriado Nacional",
      "ativo": true,
      "origem": "NACIONAL",
      "anoReferencia": 2025,
      "dtCriacao": "2025-01-15T10:30:00",
      "dtAtualizacao": null
    }
  ]
}
```

---

### 2. Listar Bloqueios Ativos

```
GET /api/bloqueio-organizacao/ativos
```

Retorna apenas os bloqueios/feriados ativos. Ideal para uso no calendario do front-end.

**Resposta (200):**
```json
{
  "success": true,
  "message": "Bloqueios ativos recuperados com sucesso.",
  "dados": [ /* array de BloqueioOrganizacao */ ]
}
```

---

### 3. Buscar Bloqueio por ID

```
GET /api/bloqueio-organizacao/{id}
```

| Parametro | Tipo   | Descricao           |
|-----------|--------|---------------------|
| `id`      | number | ID do bloqueio      |

**Resposta (200):**
```json
{
  "success": true,
  "message": "Bloqueio encontrado com sucesso.",
  "dados": { /* BloqueioOrganizacao */ }
}
```

**Erros:**
- `404` - Bloqueio nao encontrado
- `403` - Sem permissao (organizacao diferente)

---

### 4. Listar Bloqueios por Periodo (Calendario)

```
GET /api/bloqueio-organizacao/periodo?dataInicio={dataInicio}&dataFim={dataFim}
```

Retorna bloqueios ativos que se sobrepoem ao periodo informado. Use este endpoint para carregar os dias bloqueados no calendario de agendamento.

| Parametro    | Tipo   | Formato      | Descricao                      |
|--------------|--------|--------------|--------------------------------|
| `dataInicio` | string | `yyyy-MM-dd` | Data inicial do periodo        |
| `dataFim`    | string | `yyyy-MM-dd` | Data final do periodo          |

**Exemplo de uso no front-end:**
```
GET /api/bloqueio-organizacao/periodo?dataInicio=2025-01-01&dataFim=2025-12-31
```

**Resposta (200):**
```json
{
  "success": true,
  "message": "Bloqueios do periodo recuperados com sucesso.",
  "dados": [
    {
      "id": 1,
      "titulo": "Ano Novo",
      "dataInicio": "2025-01-01",
      "dataFim": "2025-01-01",
      "tipo": "FERIADO",
      "descricao": "Feriado Nacional",
      "ativo": true,
      "origem": "NACIONAL",
      "anoReferencia": 2025
    },
    {
      "id": 5,
      "titulo": "Ferias Coletivas",
      "dataInicio": "2025-12-23",
      "dataFim": "2025-12-31",
      "tipo": "BLOQUEIO",
      "descricao": "Ferias coletivas de fim de ano",
      "ativo": true,
      "origem": "MANUAL",
      "anoReferencia": null
    }
  ]
}
```

**Erros:**
- `400` - Formato de data invalido ou dataFim anterior a dataInicio

---

### 5. Verificar se uma Data esta Bloqueada

```
GET /api/bloqueio-organizacao/verificar/{data}
```

Verifica se uma data especifica esta bloqueada. Pode ser usado para validacao rapida antes de abrir o formulario de agendamento.

| Parametro | Tipo   | Formato      | Descricao             |
|-----------|--------|--------------|-----------------------|
| `data`    | string | `yyyy-MM-dd` | Data a verificar      |

**Exemplo:**
```
GET /api/bloqueio-organizacao/verificar/2025-12-25
```

**Resposta (200):**
```json
{
  "success": true,
  "message": "Data bloqueada.",
  "dados": {
    "data": "2025-12-25",
    "bloqueada": true
  }
}
```

---

### 6. Criar Novo Bloqueio/Feriado Manual

```
POST /api/bloqueio-organizacao
```

**Body (JSON):**
```json
{
  "titulo": "Ferias Coletivas",
  "dataInicio": "2025-12-23",
  "dataFim": "2025-12-31",
  "tipo": "BLOQUEIO",
  "descricao": "Ferias coletivas de fim de ano"
}
```

| Campo        | Tipo   | Obrigatorio | Descricao                            |
|--------------|--------|-------------|--------------------------------------|
| `titulo`     | string | Sim         | Nome do bloqueio/feriado             |
| `dataInicio` | string | Sim         | Data de inicio (yyyy-MM-dd)          |
| `dataFim`    | string | Nao         | Data de fim (default = dataInicio)   |
| `tipo`       | string | Nao         | "FERIADO" ou "BLOQUEIO" (default)    |
| `descricao`  | string | Nao         | Descricao opcional                   |

**Resposta (201):**
```json
{
  "success": true,
  "message": "Bloqueio criado com sucesso.",
  "dados": { /* BloqueioOrganizacao criado */ }
}
```

**Erros:**
- `400` - Titulo vazio, data invalida, dataFim anterior a dataInicio

---

### 7. Atualizar Bloqueio/Feriado

```
PUT /api/bloqueio-organizacao/{id}
```

Apenas os campos enviados serao atualizados (campos null sao ignorados).

**Body (JSON):**
```json
{
  "titulo": "Novo titulo",
  "dataInicio": "2025-12-24",
  "dataFim": "2025-12-31",
  "tipo": "BLOQUEIO",
  "descricao": "Descricao atualizada",
  "ativo": true
}
```

**Resposta (200):**
```json
{
  "success": true,
  "message": "Bloqueio atualizado com sucesso.",
  "dados": { /* BloqueioOrganizacao atualizado */ }
}
```

**Erros:**
- `400` - Validacao falhou
- `403` - Sem permissao
- `404` - Bloqueio nao encontrado

---

### 8. Alternar Status Ativo/Inativo

```
PATCH /api/bloqueio-organizacao/{id}/toggle
```

Alterna o campo `ativo` entre `true` e `false`. Quando inativo, o bloqueio nao impede agendamentos.

**Resposta (200):**
```json
{
  "success": true,
  "message": "Status do bloqueio alterado com sucesso.",
  "dados": {
    "id": 1,
    "titulo": "Ano Novo",
    "ativo": false
  }
}
```

---

### 9. Remover Bloqueio/Feriado

```
DELETE /api/bloqueio-organizacao/{id}
```

Remove permanentemente o bloqueio. Para desativar sem remover, use o endpoint de toggle.

**Resposta (200):**
```json
{
  "success": true,
  "message": "Bloqueio removido com sucesso."
}
```

**Erros:**
- `403` - Sem permissao
- `404` - Bloqueio nao encontrado

---

### 10. Importar Feriados Nacionais

```
POST /api/bloqueio-organizacao/importar-feriados?ano={ano}
```

Importa feriados nacionais brasileiros do ano especificado via BrasilAPI (`https://brasilapi.com.br/api/feriados/v1/{ano}`). Feriados que ja existem para a organizacao nao sao duplicados.

| Parametro | Tipo   | Obrigatorio | Descricao                          |
|-----------|--------|-------------|------------------------------------|
| `ano`     | number | Nao         | Ano para importar (default: atual) |

**Exemplo:**
```
POST /api/bloqueio-organizacao/importar-feriados?ano=2025
```

**Resposta (200):**
```json
{
  "success": true,
  "message": "9 feriado(s) importado(s) com sucesso!",
  "dados": {
    "ano": 2025,
    "importados": 9
  }
}
```

**Se ja importados:**
```json
{
  "success": true,
  "message": "Todos os feriados ja estao cadastrados.",
  "dados": {
    "ano": 2025,
    "importados": 0
  }
}
```

**Erros:**
- `400` - Erro ao acessar API externa de feriados

---

## Integracao com Agendamentos

### Validacao Automatica

O sistema valida automaticamente se o dia esta bloqueado nos seguintes momentos:

1. **Criar agendamento** (`POST /api/agendamento`) - Se o dia esta bloqueado, retorna erro `400`:
   ```json
   {
     "success": false,
     "message": "Nao e possivel agendar para o dia 2025-12-25. Motivo: Natal - Feriado Nacional",
     "errorCode": 400
   }
   ```

2. **Atualizar agendamento** (`PUT /api/agendamento/{id}`) - Se a nova data esta bloqueada, retorna o mesmo erro.

3. **Reagendar** (`PATCH /api/agendamento/{id}/reagendar`) - Valida a nova data contra bloqueios.

4. **Consultar disponibilidade** (`POST /api/agendamento/disponibilidade`) - Se o dia esta bloqueado, retorna lista vazia de horarios disponiveis (sem erro).

### Bloqueio no Calendario do Front-End

Para bloquear os dias no calendario de agendamento:

1. Ao carregar o calendario, chame:
   ```
   GET /api/bloqueio-organizacao/periodo?dataInicio=2025-01-01&dataFim=2025-12-31
   ```

2. Extraia as datas de cada bloqueio (incluindo o range de `dataInicio` ate `dataFim`).

3. Desabilite esses dias no componente de calendario.

**Exemplo de implementacao no front-end:**

```typescript
// Buscar bloqueios para o ano
const response = await fetch(`/api/bloqueio-organizacao/periodo?dataInicio=${anoInicio}&dataFim=${anoFim}`, {
  headers: { 'Authorization': `Bearer ${token}` }
})
const { dados: bloqueios } = await response.json()

// Gerar lista de datas bloqueadas
const datasBloqueadas: string[] = []
bloqueios.forEach((bloqueio: BloqueioOrganizacao) => {
  const inicio = new Date(bloqueio.dataInicio + "T00:00:00")
  const fim = new Date(bloqueio.dataFim + "T00:00:00")

  const current = new Date(inicio)
  while (current <= fim) {
    datasBloqueadas.push(current.toISOString().split('T')[0])
    current.setDate(current.getDate() + 1)
  }
})

// Usar datasBloqueadas para desabilitar dias no calendario
```

---

## Fluxo Recomendado

```
1. Admin acessa a tela de "Feriados e Bloqueios"
2. Clica em "Importar Feriados" → POST /importar-feriados?ano=2025
3. Os feriados nacionais sao importados automaticamente
4. Admin pode adicionar bloqueios manuais → POST /
5. Admin pode editar, ativar/desativar ou remover bloqueios
6. No calendario de agendamento, o front-end carrega os bloqueios ativos
   → GET /periodo?dataInicio=...&dataFim=...
7. Os dias bloqueados ficam desabilitados no calendario
8. Se o usuario tentar agendar em um dia bloqueado via API,
   o back-end rejeita com mensagem explicativa
```

---

## Codigos HTTP

| Codigo | Situacao                                      |
|--------|-----------------------------------------------|
| `200`  | Sucesso                                       |
| `201`  | Bloqueio criado com sucesso                   |
| `400`  | Validacao falhou ou dados invalidos           |
| `403`  | Sem permissao (organizacao diferente)         |
| `404`  | Bloqueio nao encontrado                       |
| `500`  | Erro interno do servidor                      |
