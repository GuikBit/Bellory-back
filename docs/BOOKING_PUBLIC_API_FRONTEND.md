# Booking Publico - Guia de Integracao Frontend

## Visao Geral

API publica para o fluxo de agendamento na landing page do cliente final. Permite que visitantes do site busquem/cadastrem-se, consultem disponibilidade e criem agendamentos sem autenticacao.

**Base URL:** `/api/v1/public/site`
**Autenticacao:** Nenhuma (endpoints 100% publicos)
**Rate Limiting:** 60 req/hora por IP | 5 criacao/hora por telefone

---

## Envelope de Resposta Padrao

Todas as respostas seguem o formato `ResponseAPI<T>`:

```json
{
  "success": true,
  "message": "Mensagem descritiva",
  "dados": { /* payload tipado */ },
  "errorCode": null,
  "errors": null
}
```

### Codigos HTTP retornados

| Codigo | Situacao |
|--------|----------|
| `200` | Sucesso |
| `201` | Recurso criado com sucesso |
| `400` | Dados invalidos / validacao falhou |
| `404` | Organizacao ou recurso nao encontrado |
| `409` | Conflito (cliente duplicado / horario ocupado) |
| `429` | Rate limit excedido |
| `500` | Erro interno do servidor |

---

## Fluxo Recomendado para o Frontend

```
1. Usuario acessa: app.bellory.com.br/{slug}
2. Frontend extrai o slug da URL
3. Carrega dados do site via GET /api/v1/public/site/{slug}/home (endpoint existente)
4. Usuario clica em "Agendar"
5. Pede o telefone do usuario
6. GET /{slug}/booking/cliente?telefone=11999998888
7. Se retornar dados → usuario existente, preenche automaticamente
8. Se dados = null → formulario de cadastro
9. POST /{slug}/booking/cliente (se novo)
10. Usuario escolhe profissional
11. GET /{slug}/booking/dias-disponiveis?funcionarioId=1&mes=2026-03
12. Usuario escolhe dia no calendario
13. GET /{slug}/booking/horarios?funcionarioId=1&data=2026-03-28&servicoIds=1,2
14. Usuario escolhe horario
15. POST /{slug}/booking → agendamento criado
```

---

## Interfaces TypeScript

```typescript
// Envelope padrao de resposta
interface ResponseAPI<T> {
  success: boolean;
  message?: string;
  dados: T;
  errorCode?: number;
  errors?: Record<string, string>;
}

// Dados publicos do cliente
interface ClientePublicDTO {
  id: number;
  nome: string;
  telefone: string;
  email?: string;
}

// Request para criar cliente
interface ClienteCreatePublicDTO {
  nome: string;       // 3-100 chars, so letras e espacos
  telefone: string;   // 10-11 digitos (com ou sem mascara, backend sanitiza)
  email?: string;     // opcional, formato email valido
}

// Dia disponivel no calendario
interface DiaDisponivelDTO {
  data: string;        // "YYYY-MM-DD"
  disponivel: boolean;
}

// Request para criar agendamento
interface BookingCreateDTO {
  clienteId: number;
  funcionarioId: number;
  servicoIds: number[];    // pelo menos 1
  data: string;            // "YYYY-MM-DD"
  horario: string;         // "HH:mm"
  observacao?: string;
}

// Resposta do agendamento criado
interface BookingResponseDTO {
  id: number;
  status: string;              // "PENDENTE"
  dtAgendamento: string;       // "2026-03-28T14:00:00"
  valorTotal: number;          // 85.00
  profissional: string;        // "Carlos"
  servicos: string[];          // ["Corte", "Barba"]
  requerSinal?: boolean;
  percentualSinal?: number;
}
```

---

## Endpoints

### 1. Buscar Cliente por Telefone

**`GET /{slug}/booking/cliente?telefone={digits}`**

Busca cliente existente pelo telefone. O frontend usa para preencher dados automaticamente ou redirecionar para o cadastro.

**Parametros:**

| Parametro | Tipo | Obrigatorio | Descricao |
|-----------|------|-------------|-----------|
| `slug` | `string` (path) | Sim | Slug da organizacao |
| `telefone` | `string` (query) | Sim | Telefone com ou sem mascara (backend sanitiza) |

**Exemplo:**
```
GET /api/v1/public/site/barbeariadoje/booking/cliente?telefone=11999998888
```

**Response 200 - Cliente encontrado:**
```json
{
  "success": true,
  "dados": {
    "id": 1,
    "nome": "Joao Silva",
    "telefone": "11999998888",
    "email": "joao@email.com"
  }
}
```

**Response 200 - Cliente nao encontrado:**
```json
{
  "success": true,
  "dados": null
}
```

**Response 400 - Telefone invalido:**
```json
{
  "success": false,
  "message": "Telefone invalido. Deve conter entre 10 e 11 digitos.",
  "errorCode": 400
}
```

**Logica do frontend:**
```typescript
async function buscarCliente(slug: string, telefone: string) {
  const res = await fetch(
    `${API_URL}/api/v1/public/site/${slug}/booking/cliente?telefone=${telefone}`
  );
  const data: ResponseAPI<ClientePublicDTO | null> = await res.json();

  if (data.success && data.dados) {
    // Cliente existente - preencher formulario automaticamente
    setCliente(data.dados);
    avancarParaSelecaoServico();
  } else if (data.success && data.dados === null) {
    // Cliente novo - mostrar formulario de cadastro
    mostrarFormularioCadastro(telefone);
  }
}
```

> **Importante:** O campo `telefone` aceita qualquer formato (com mascara, espacos, hifen). O backend remove tudo que nao for digito antes de validar.

---

### 2. Cadastrar Novo Cliente (Auto-cadastro)

**`POST /{slug}/booking/cliente`**

Cria um novo cliente na organizacao. Nao gera credenciais de acesso (login/senha).

**Request Body:**

| Campo | Tipo | Obrigatorio | Validacao |
|-------|------|-------------|-----------|
| `nome` | `string` | Sim | 3-100 chars, apenas letras e espacos |
| `telefone` | `string` | Sim | 10-11 digitos (backend sanitiza) |
| `email` | `string` | Nao | Formato email valido se informado |

**Exemplo:**
```
POST /api/v1/public/site/barbeariadoje/booking/cliente
Content-Type: application/json

{
  "nome": "Maria Oliveira",
  "telefone": "(11) 99999-7777",
  "email": "maria@email.com"
}
```

**Response 201 - Criado com sucesso:**
```json
{
  "success": true,
  "message": "Cliente cadastrado com sucesso",
  "dados": {
    "id": 42,
    "nome": "Maria Oliveira",
    "telefone": "11999997777",
    "email": "maria@email.com"
  }
}
```

**Response 409 - Telefone ja cadastrado:**
```json
{
  "success": false,
  "message": "Cliente ja cadastrado com este telefone",
  "errorCode": 409
}
```

**Response 400 - Validacao falhou:**
```json
{
  "success": false,
  "message": "Nome deve ter entre 3 e 100 caracteres.",
  "errorCode": 400
}
```

**Logica do frontend:**
```typescript
async function cadastrarCliente(slug: string, dados: ClienteCreatePublicDTO) {
  const res = await fetch(`${API_URL}/api/v1/public/site/${slug}/booking/cliente`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados),
  });
  const data: ResponseAPI<ClientePublicDTO> = await res.json();

  if (res.status === 201 && data.success) {
    // Sucesso - salvar clienteId e avancar
    setCliente(data.dados);
    avancarParaSelecaoServico();
  } else if (res.status === 409) {
    // Telefone ja cadastrado - redirecionar para busca
    mostrarMensagem('Este telefone ja esta cadastrado. Buscando seus dados...');
    buscarCliente(slug, dados.telefone);
  } else {
    mostrarErro(data.message);
  }
}
```

> **Nota:** Se o cliente existir mas estiver inativo, o backend reativa automaticamente e retorna 201 (nao 409).

---

### 3. Horarios Disponiveis

**`GET /{slug}/booking/horarios?funcionarioId={id}&data={YYYY-MM-DD}&servicoIds=1,2,3`**

Retorna os horarios livres de um profissional em um dia especifico, considerando a duracao total dos servicos selecionados.

**Parametros:**

| Parametro | Tipo | Obrigatorio | Descricao |
|-----------|------|-------------|-----------|
| `slug` | `string` (path) | Sim | Slug da organizacao |
| `funcionarioId` | `number` (query) | Sim | ID do profissional |
| `data` | `string` (query) | Sim | Data no formato `YYYY-MM-DD` |
| `servicoIds` | `number[]` (query) | Sim | IDs dos servicos (separados por virgula) |

**Exemplo:**
```
GET /api/v1/public/site/barbeariadoje/booking/horarios?funcionarioId=5&data=2026-03-28&servicoIds=1,2
```

**Response 200 - Horarios disponiveis:**
```json
{
  "success": true,
  "dados": ["09:00", "09:30", "10:00", "14:00", "14:30", "15:00"]
}
```

**Response 200 - Nenhum horario:**
```json
{
  "success": true,
  "dados": []
}
```

**Response 400 - Data no passado:**
```json
{
  "success": false,
  "message": "Data nao pode ser no passado.",
  "errorCode": 400
}
```

**Logica do frontend:**
```typescript
async function buscarHorarios(
  slug: string,
  funcionarioId: number,
  data: string,        // "2026-03-28"
  servicoIds: number[]
) {
  const ids = servicoIds.join(',');
  const res = await fetch(
    `${API_URL}/api/v1/public/site/${slug}/booking/horarios` +
    `?funcionarioId=${funcionarioId}&data=${data}&servicoIds=${ids}`
  );
  const data: ResponseAPI<string[]> = await res.json();

  if (data.success) {
    if (data.dados.length === 0) {
      mostrarMensagem('Nenhum horario disponivel neste dia. Escolha outra data.');
    } else {
      renderizarHorarios(data.dados); // Ex: grid de botoes "09:00", "09:30", ...
    }
  }
}
```

**Como o backend calcula:**
1. Busca a jornada de trabalho do profissional para o dia da semana
2. Gera slots de 30 em 30 minutos dentro dos periodos de trabalho
3. Remove slots fora do horario de funcionamento da organizacao
4. Remove slots que conflitam com bloqueios (almoco, reuniao, ferias, etc.)
5. Remove slots que conflitam com agendamentos existentes (PENDENTE, AGENDADO, etc.)
6. Se a data for hoje, remove slots antes de `agora + toleranciaAgendamento` (default 15 min)
7. Retorna os slots restantes ordenados

> **Importante:** Os horarios consideram a **duracao total** de todos os servicos selecionados. Se o cliente mudar os servicos, os horarios devem ser reconsultados.

---

### 4. Dias Disponiveis no Mes

**`GET /{slug}/booking/dias-disponiveis?funcionarioId={id}&mes={YYYY-MM}`**

Retorna todos os dias do mes com flag de disponibilidade. Usado para renderizar o calendario e desabilitar dias sem horarios.

**Parametros:**

| Parametro | Tipo | Obrigatorio | Descricao |
|-----------|------|-------------|-----------|
| `slug` | `string` (path) | Sim | Slug da organizacao |
| `funcionarioId` | `number` (query) | Sim | ID do profissional |
| `mes` | `string` (query) | Sim | Mes no formato `YYYY-MM` |

**Exemplo:**
```
GET /api/v1/public/site/barbeariadoje/booking/dias-disponiveis?funcionarioId=5&mes=2026-03
```

**Response 200:**
```json
{
  "success": true,
  "dados": [
    { "data": "2026-03-01", "disponivel": false },
    { "data": "2026-03-02", "disponivel": true },
    { "data": "2026-03-03", "disponivel": true },
    { "data": "2026-03-04", "disponivel": false },
    { "data": "2026-03-05", "disponivel": true },
    "..."
  ]
}
```

**Logica do frontend:**
```typescript
async function buscarDiasDisponiveis(
  slug: string,
  funcionarioId: number,
  mes: string  // "2026-03"
) {
  const res = await fetch(
    `${API_URL}/api/v1/public/site/${slug}/booking/dias-disponiveis` +
    `?funcionarioId=${funcionarioId}&mes=${mes}`
  );
  const data: ResponseAPI<DiaDisponivelDTO[]> = await res.json();

  if (data.success) {
    // Atualizar calendario
    data.dados.forEach(dia => {
      const elemento = getCalendarDay(dia.data);
      if (!dia.disponivel) {
        elemento.classList.add('disabled');
        elemento.setAttribute('disabled', 'true');
      }
    });
  }
}
```

**Quando um dia esta indisponivel:**
- Dia ja passou (passado)
- Dia excede o limite de antecedencia (default 90 dias)
- Organizacao fechada naquele dia da semana
- Profissional nao trabalha naquele dia da semana
- Profissional tem bloqueio integral (ferias/folga) no dia

> **Nota:** Este endpoint faz uma verificacao leve (nao calcula slots). E possivel que um dia marcado como `disponivel: true` retorne `[]` no endpoint de horarios se todos os slots estiverem ocupados. O frontend deve tratar esse caso mostrando "Nenhum horario disponivel".

---

### 5. Criar Agendamento

**`POST /{slug}/booking`**

Cria o agendamento final. Re-valida toda a disponibilidade para evitar conflitos por concorrencia.

**Request Body:**

| Campo | Tipo | Obrigatorio | Descricao |
|-------|------|-------------|-----------|
| `clienteId` | `number` | Sim | ID do cliente (obtido no passo 1 ou 2) |
| `funcionarioId` | `number` | Sim | ID do profissional selecionado |
| `servicoIds` | `number[]` | Sim | IDs dos servicos (pelo menos 1) |
| `data` | `string` | Sim | Data no formato `YYYY-MM-DD` |
| `horario` | `string` | Sim | Horario no formato `HH:mm` |
| `observacao` | `string` | Nao | Observacao livre do cliente |

**Exemplo:**
```
POST /api/v1/public/site/barbeariadoje/booking
Content-Type: application/json

{
  "clienteId": 42,
  "funcionarioId": 5,
  "servicoIds": [1, 3],
  "data": "2026-03-28",
  "horario": "14:00",
  "observacao": "Prefiro corte mais curto nas laterais"
}
```

**Response 201 - Agendamento criado:**
```json
{
  "success": true,
  "message": "Agendamento criado com sucesso",
  "dados": {
    "id": 123,
    "status": "PENDENTE",
    "dtAgendamento": "2026-03-28T14:00:00",
    "valorTotal": 85.00,
    "profissional": "Carlos",
    "servicos": ["Corte", "Barba"],
    "requerSinal": true,
    "percentualSinal": 30
  }
}
```

**Response 409 - Horario nao disponivel:**
```json
{
  "success": false,
  "message": "Horario nao esta mais disponivel",
  "errorCode": 409
}
```

**Response 400 - Validacao:**
```json
{
  "success": false,
  "message": "O profissional nao atende o servico: Massagem",
  "errorCode": 400
}
```

**Logica do frontend:**
```typescript
async function criarAgendamento(slug: string, booking: BookingCreateDTO) {
  const res = await fetch(`${API_URL}/api/v1/public/site/${slug}/booking`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(booking),
  });
  const data: ResponseAPI<BookingResponseDTO> = await res.json();

  if (res.status === 201 && data.success) {
    // Sucesso - mostrar confirmacao
    mostrarConfirmacao({
      id: data.dados.id,
      profissional: data.dados.profissional,
      servicos: data.dados.servicos,
      dataHora: data.dados.dtAgendamento,
      valor: data.dados.valorTotal,
      requerSinal: data.dados.requerSinal,
      percentualSinal: data.dados.percentualSinal,
    });

    if (data.dados.requerSinal) {
      // Mostrar instrucoes de pagamento do sinal
      mostrarPagamentoSinal(data.dados);
    }
  } else if (res.status === 409) {
    // Horario ocupado - voltar para selecao de horario
    mostrarErro('Este horario foi ocupado por outro cliente. Por favor, escolha outro horario.');
    recarregarHorarios();
  } else {
    mostrarErro(data.message);
  }
}
```

---

## Tratamento de Erros Global

Recomendamos criar um wrapper para todas as chamadas da API de booking:

```typescript
const BOOKING_API = (slug: string) =>
  `${API_URL}/api/v1/public/site/${slug}/booking`;

async function bookingFetch<T>(url: string, options?: RequestInit): Promise<ResponseAPI<T>> {
  try {
    const res = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    });

    const data: ResponseAPI<T> = await res.json();

    // Rate limit
    if (res.status === 429) {
      mostrarToast('Muitas requisicoes. Aguarde alguns minutos.', 'warning');
      throw new Error('RATE_LIMITED');
    }

    // Erro de servidor
    if (res.status >= 500) {
      mostrarToast('Erro no servidor. Tente novamente.', 'error');
      throw new Error('SERVER_ERROR');
    }

    return data;
  } catch (error) {
    if (error instanceof TypeError) {
      // Erro de rede (offline, CORS, etc.)
      mostrarToast('Sem conexao com o servidor.', 'error');
    }
    throw error;
  }
}
```

---

## Componentes Sugeridos (React)

### Estrutura de paginas/componentes

```
src/
  pages/
    booking/
      BookingPage.tsx              # Pagina principal (wizard/stepper)
  components/
    booking/
      PhoneStep.tsx                # Passo 1: Input de telefone
      ClienteFormStep.tsx          # Passo 2: Formulario de cadastro (se novo)
      ServiceSelectStep.tsx        # Passo 3: Selecao de servicos
      ProfessionalSelectStep.tsx   # Passo 4: Selecao do profissional
      CalendarStep.tsx             # Passo 5: Calendario com dias disponiveis
      TimeSlotStep.tsx             # Passo 6: Grid de horarios
      ConfirmationStep.tsx         # Passo 7: Resumo e confirmacao
      BookingSuccess.tsx           # Passo 8: Tela de sucesso
  hooks/
    useBookingApi.ts               # Hook com todas as chamadas da API
  types/
    booking.ts                     # Interfaces TypeScript
```

### Hook `useBookingApi`

```typescript
// hooks/useBookingApi.ts
import { useState } from 'react';

export function useBookingApi(slug: string) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const baseUrl = `${API_URL}/api/v1/public/site/${slug}/booking`;

  const buscarCliente = async (telefone: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${baseUrl}/cliente?telefone=${encodeURIComponent(telefone)}`);
      const data = await res.json();
      return data.dados; // ClientePublicDTO | null
    } catch (e) {
      setError('Erro ao buscar cliente');
      return null;
    } finally {
      setLoading(false);
    }
  };

  const cadastrarCliente = async (dados: ClienteCreatePublicDTO) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${baseUrl}/cliente`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(dados),
      });
      const data = await res.json();
      if (!data.success) {
        setError(data.message);
        return { success: false, status: res.status, data };
      }
      return { success: true, status: res.status, data: data.dados };
    } catch (e) {
      setError('Erro ao cadastrar cliente');
      return { success: false, status: 500 };
    } finally {
      setLoading(false);
    }
  };

  const buscarDiasDisponiveis = async (funcionarioId: number, mes: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(
        `${baseUrl}/dias-disponiveis?funcionarioId=${funcionarioId}&mes=${mes}`
      );
      const data = await res.json();
      return data.dados as DiaDisponivelDTO[];
    } catch (e) {
      setError('Erro ao buscar dias');
      return [];
    } finally {
      setLoading(false);
    }
  };

  const buscarHorarios = async (
    funcionarioId: number,
    data: string,
    servicoIds: number[]
  ) => {
    setLoading(true);
    setError(null);
    try {
      const ids = servicoIds.join(',');
      const res = await fetch(
        `${baseUrl}/horarios?funcionarioId=${funcionarioId}&data=${data}&servicoIds=${ids}`
      );
      const d = await res.json();
      return d.dados as string[];
    } catch (e) {
      setError('Erro ao buscar horarios');
      return [];
    } finally {
      setLoading(false);
    }
  };

  const criarAgendamento = async (booking: BookingCreateDTO) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(baseUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(booking),
      });
      const data = await res.json();
      if (!data.success) {
        setError(data.message);
        return { success: false, status: res.status, data };
      }
      return { success: true, status: res.status, data: data.dados };
    } catch (e) {
      setError('Erro ao criar agendamento');
      return { success: false, status: 500 };
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,
    error,
    buscarCliente,
    cadastrarCliente,
    buscarDiasDisponiveis,
    buscarHorarios,
    criarAgendamento,
  };
}
```

---

## Dados Necessarios de Endpoints Existentes

Para montar a tela de booking, o frontend tambem precisa de dados de endpoints **ja existentes**:

| Dado | Endpoint existente | Quando carregar |
|------|--------------------|-----------------|
| Lista de servicos | `GET /api/v1/public/site/{slug}/services` | Ao entrar na tela de booking |
| Lista de profissionais | `GET /api/v1/public/site/{slug}/team` | Ao entrar na tela de booking |
| Config de agendamento | `GET /api/v1/public/site/{slug}/booking` | Ao entrar na tela de booking |

O endpoint `GET /{slug}/booking` (existente) retorna `BookingSectionDTO` com:
- `servicosDisponiveis[]` - servicos com preco, duracao, nome
- `profissionaisDisponiveis[]` - profissionais com nome, foto, servicos que atendem
- `config.requiresDeposit` - se exige sinal
- `config.depositPercentage` - percentual do sinal
- `config.minAdvanceHours` - antecedencia minima
- `config.maxAdvanceDays` - antecedencia maxima

> Use esses dados para alimentar os steps de selecao de servicos e profissionais. Os novos endpoints de booking sao usados para os passos seguintes (calendario, horarios, criacao).

---

## Checklist de Implementacao

- [ ] Criar service `useBookingApi.ts` com todas as chamadas
- [ ] Criar tipos TypeScript em `types/booking.ts`
- [ ] Implementar `PhoneStep` - input com mascara de telefone brasileiro
- [ ] Implementar `ClienteFormStep` - formulario nome + telefone + email
- [ ] Implementar `ServiceSelectStep` - cards/chips de servicos (usando endpoint existente)
- [ ] Implementar `ProfessionalSelectStep` - cards de profissionais (usando endpoint existente)
- [ ] Implementar `CalendarStep` - calendario mensal com dias habilitados/desabilitados
- [ ] Implementar `TimeSlotStep` - grid de botoes com horarios disponiveis
- [ ] Implementar `ConfirmationStep` - resumo com servicos, profissional, data/hora, valor
- [ ] Implementar `BookingSuccess` - tela de sucesso com dados do agendamento
- [ ] Tratar erro 409 no POST booking (voltar para selecao de horario)
- [ ] Tratar erro 409 no POST cliente (redirecionar para busca)
- [ ] Tratar erro 429 (rate limit) com mensagem amigavel
- [ ] Recarregar horarios quando usuario mudar servicos selecionados
- [ ] Recarregar dias disponiveis quando usuario mudar de mes no calendario
- [ ] Mostrar informacao de sinal/deposito quando `requerSinal = true`
- [ ] Mascara de telefone: `(XX) XXXXX-XXXX` ou `(XX) XXXX-XXXX`
- [ ] Loading states em todas as chamadas
- [ ] Responsividade mobile (maioria dos usuarios acessa pelo celular)
