# Templates de Notificação — Guia Frontend

Documenta como o frontend deve consumir a API para permitir que cada organização
**edite os textos das mensagens automatizadas** (confirmação, lembrete, anamnese)
enviadas via WhatsApp.

> Antes da V71, a mensagem de **anamnese** era hardcoded no Java. A partir desta
> versão ela segue o mesmo modelo de confirmação/lembrete: editável por tenant,
> com fallback para o template padrão da plataforma.

---

## 1. Visão geral

Cada organização pode ter um **override** de mensagem por `(tipo, horas_antes)`.
Se o override não existir (ou estiver vazio), o backend usa o **template padrão
da plataforma** (`admin.template_bellory`). O frontend só lida com os overrides.

Resolução em runtime (tudo cuidado pelo backend):

```
override do tenant   ──►  template padrão da plataforma  ──►  fallback inline
(app.config_notificacao)  (admin.template_bellory)            (último recurso)
```

Isso significa: se o tenant nunca tocar na configuração, ele usa o texto padrão
da Bellory. Quando ele edita, vira override. Para "voltar ao padrão" basta
**deletar** a configuração (ou enviar `mensagemTemplate = null` via upsert).

---

## 2. Tipos de notificação

| Tipo                | Quando dispara                                          | `horasAntes` válidos | Editável pelo tenant |
| ------------------- | ------------------------------------------------------- | -------------------- | -------------------- |
| `CONFIRMACAO`       | N horas antes do agendamento — pede confirmação SIM/NÃO | `12, 24, 36, 48`     | ✅                    |
| `LEMBRETE`          | N horas antes — apenas lembra                           | `1, 2, 3, 4, 5, 6`   | ✅                    |
| `ANAMNESE`          | Logo após criar o agendamento (event-driven)            | `0` (sentinel)       | ✅                    |
| `FILA_ESPERA_*`     | Event-driven (oferta de adiantamento)                   | —                    | ❌ (ainda hardcoded)  |

> O frontend deve **filtrar para mostrar apenas os tipos editáveis**:
> `CONFIRMACAO`, `LEMBRETE`, `ANAMNESE`. Os tipos de fila de espera retornam erro
> 400 se forem enviados.

---

## 3. Variáveis disponíveis por tipo

Placeholders no formato `{{nome_variavel}}`. Variáveis não preenchidas
permanecem literalmente no texto (ajuda a debugar erros de digitação).

### CONFIRMACAO e LEMBRETE

| Variável               | Descrição                            | Exemplo                  |
| ---------------------- | ------------------------------------ | ------------------------ |
| `{{nome_cliente}}`     | Primeiro nome do cliente             | João                     |
| `{{nome_empresa}}`     | Nome fantasia da organização         | Studio Bellory           |
| `{{data_agendamento}}` | Data do agendamento                  | 15/03/2026               |
| `{{hora_agendamento}}` | Horário do agendamento               | 14:30                    |
| `{{servico}}`          | Nome do serviço                      | Corte Masculino          |
| `{{profissional}}`     | Nome do funcionário                  | Maria Santos             |
| `{{local}}`            | Endereço do estabelecimento          | Rua das Flores, 123      |
| `{{valor}}`            | Valor formatado em BRL               | R$ 50,00                 |

### ANAMNESE

| Variável                  | Descrição                                | Exemplo                         |
| ------------------------- | ---------------------------------------- | ------------------------------- |
| `{{nome_cliente}}`        | Primeiro nome do cliente                 | João                            |
| `{{nome_empresa}}`        | Nome fantasia da organização             | Studio Bellory                  |
| `{{data_agendamento}}`    | Data do agendamento (formato `dd/MM`)    | 15/03                           |
| `{{hora_agendamento}}`    | Horário do agendamento                   | 14:30                           |
| `{{titulo_questionario}}` | Título do questionário/anamnese          | Anamnese padrão                 |
| `{{link_anamnese}}`       | URL pública para responder o questionário | https://app.bellory.com.br/...  |

> ⚠️ Variáveis como `{{servico}}`, `{{profissional}}`, `{{valor}}` **não estão
> disponíveis** no template de anamnese. Se o usuário inserir, ficarão literais
> na mensagem. O editor deve oferecer só os 6 placeholders acima quando o tipo
> for `ANAMNESE`.

---

## 4. Endpoints REST

Base: `/api/v1/config/notificacao`. Auth: JWT do tenant (`Authorization: Bearer ...`).

Todas as respostas seguem o envelope padrão `ResponseAPI`:

```json
{
  "success": true,
  "message": "...",
  "dados": { ... },
  "errorCode": null
}
```

Em caso de erro: `success=false`, `dados=null`, `errorCode` preenchido (400/404/500),
`message` com a descrição.

### 4.1 Listar configurações ativas

```
GET /api/v1/config/notificacao
```

Retorna apenas as configurações **ativas** da organização logada, ordenadas por
`tipo, horasAntes DESC`. Use esta na tela principal.

**Response 200**

```json
{
  "success": true,
  "message": "Configurações recuperadas",
  "dados": [
    {
      "id": 12,
      "tipo": "CONFIRMACAO",
      "horasAntes": 24,
      "ativo": true,
      "mensagemTemplate": "Olá *{{nome_cliente}}*..."
    },
    {
      "id": 13,
      "tipo": "LEMBRETE",
      "horasAntes": 2,
      "ativo": true,
      "mensagemTemplate": null
    }
  ]
}
```

> `mensagemTemplate: null` ⇒ o tenant não personalizou; o backend usa o padrão
> da plataforma. No editor, apresente o template padrão como placeholder/valor
> inicial (ver seção 5.2).

### 4.2 Listar todas (incluindo desativadas)

```
GET /api/v1/config/notificacao/todas
```

Útil para uma aba "configurações inativas". Mesmo formato da 4.1.

### 4.3 Salvar (upsert) — endpoint recomendado para o editor

```
POST /api/v1/config/notificacao/upsert
```

A chave única é `(tenant, tipo, horasAntes)`. Se já existir, atualiza. Senão, cria.

**Request body**

```json
{
  "tipo": "ANAMNESE",
  "horasAntes": 0,
  "mensagemTemplate": "Olá, {{nome_cliente}}! Tudo certo com seu agendamento...",
  "ativo": true
}
```

**Response 200** — ConfigNotificacaoDTO atualizado (com `id`).

**Erros possíveis (400)**:
- `"Horas inválidas para CONFIRMACAO. Permitido: [12, 24, 36, 48]"`
- `"Horas inválidas para ANAMNESE. Permitido: [0]"`
- `"Tipo FILA_ESPERA_OFERTA e disparado por evento, nao pode ser configurado..."`

### 4.4 Criar (POST) — alternativa quando você sabe que não existe

```
POST /api/v1/config/notificacao
```

Mesmo body da 4.3. Retorna **400** se já houver configuração para a chave.
Use `/upsert` se não tem certeza.

### 4.5 Atualizar por ID

```
PUT /api/v1/config/notificacao/{id}
```

Use quando tem o `id` em mãos (após listagem). O body pode mudar `tipo` e
`horasAntes` (revalida a unique).

### 4.6 Deletar

```
DELETE /api/v1/config/notificacao/{id}
```

Remove o override; o tenant volta a usar o template padrão da plataforma.

### 4.7 Toggle ativo/inativo

```
PATCH /api/v1/config/notificacao/{id}/status?ativo=false
```

Desativa sem deletar. Notificações desse tipo **não serão enviadas** enquanto
estiver inativa.

---

## 5. Fluxos UI sugeridos

### 5.1 Tela de listagem

Buscar `GET /api/v1/config/notificacao` e renderizar uma tabela/card por
configuração. Para cada linha:

- Badge do **tipo** (`CONFIRMACAO` / `LEMBRETE` / `ANAMNESE`).
- Quando `ANAMNESE`: ocultar a coluna "horasAntes" (sempre 0, sem significado).
- Para `CONFIRMACAO`/`LEMBRETE`: mostrar "envio X horas antes do agendamento".
- Toggle de ativo/inativo (chama 4.7).
- Botão "Editar" → abre editor (5.2).
- Botão "Restaurar padrão" → confirma e chama DELETE (4.6).

### 5.2 Editor de template

Layout sugerido (split horizontal):

| Esquerda (form)                                                                  | Direita (preview)                                |
| -------------------------------------------------------------------------------- | ------------------------------------------------ |
| Select **tipo** (`CONFIRMACAO`, `LEMBRETE`, `ANAMNESE`)                          | Render do texto com variáveis substituídas pelos `exemplo` |
| Select **horasAntes** (depende do tipo — esconder/desabilitar para `ANAMNESE`)   | Botão "Resetar para padrão" (chama DELETE)       |
| Textarea **mensagemTemplate** com chips clicáveis para inserir placeholders      |                                                  |
| Lista das variáveis disponíveis (filtra pelo tipo selecionado — ver seção 3)     |                                                  |

Salvar = `POST /api/v1/config/notificacao/upsert`.

#### Estado inicial do textarea

Quando `mensagemTemplate` vier `null` na listagem, o tenant ainda usa o template
padrão. Para mostrar como ponto de partida no editor, o frontend precisa do
texto padrão. Duas opções:

1. **Backend retorna o conteúdo resolvido** (não implementado — faria a listagem
   sempre devolver o texto que está sendo usado de fato).
2. **Frontend mantém uma cópia local dos templates padrão** (mais simples por
   enquanto). Os textos padrão hoje vivem em `admin.template_bellory` (códigos
   `whatsapp-confirmacao`, `whatsapp-lembrete`, `whatsapp-anamnese`).

> Recomendação: começar com (2) — copiar os textos da V71/V32 para o frontend.
> Quando virar incômodo, abrir um `GET /api/v1/templates/padrao` no backend que
> exponha `admin.template_bellory` para leitura pública dentro do tenant.

### 5.3 Validações no formulário

Antes de POST:

- `tipo` obrigatório.
- `horasAntes`: valida contra a lista do tipo.
  - `CONFIRMACAO` → `[12, 24, 36, 48]`
  - `LEMBRETE` → `[1, 2, 3, 4, 5, 6]`
  - `ANAMNESE` → `[0]` (forçar oculto, sempre 0)
- `mensagemTemplate`: pode ser vazio (= cair no padrão), mas alertar o usuário
  que enviar vazio remove a personalização.
- (Opcional) parsing do template: detectar placeholders `{{xxx}}` que não estão
  na lista de variáveis daquele tipo e sinalizar como warning.

---

## 6. Schema TypeScript

```ts
// Tipos canônicos
export type TipoNotificacao = 'CONFIRMACAO' | 'LEMBRETE' | 'ANAMNESE';

export interface ConfigNotificacaoDTO {
  id?: number;
  tipo: TipoNotificacao;
  horasAntes: number;            // 0 para ANAMNESE; ver tabela acima
  ativo: boolean;
  mensagemTemplate: string | null;
}

// Lista de horas válidas por tipo (para o select)
export const HORAS_VALIDAS: Record<TipoNotificacao, number[]> = {
  CONFIRMACAO: [12, 24, 36, 48],
  LEMBRETE:    [1, 2, 3, 4, 5, 6],
  ANAMNESE:    [0],
};

// Variáveis disponíveis por tipo (para os chips do editor)
export interface VariavelTemplate {
  nome: string;
  descricao: string;
  exemplo: string;
}

export const VARIAVEIS: Record<TipoNotificacao, VariavelTemplate[]> = {
  CONFIRMACAO: [
    { nome: 'nome_cliente',     descricao: 'Primeiro nome do cliente',     exemplo: 'João' },
    { nome: 'nome_empresa',     descricao: 'Nome fantasia da organização', exemplo: 'Studio Bellory' },
    { nome: 'data_agendamento', descricao: 'Data do agendamento',          exemplo: '15/03/2026' },
    { nome: 'hora_agendamento', descricao: 'Horário do agendamento',       exemplo: '14:30' },
    { nome: 'servico',          descricao: 'Nome do serviço',              exemplo: 'Corte Masculino' },
    { nome: 'profissional',     descricao: 'Nome do funcionário',          exemplo: 'Maria Santos' },
    { nome: 'local',            descricao: 'Endereço',                     exemplo: 'Rua das Flores, 123' },
    { nome: 'valor',            descricao: 'Valor em BRL',                 exemplo: 'R$ 50,00' },
  ],
  LEMBRETE: [
    // mesmas da CONFIRMACAO
  ],
  ANAMNESE: [
    { nome: 'nome_cliente',        descricao: 'Primeiro nome do cliente',         exemplo: 'João' },
    { nome: 'nome_empresa',        descricao: 'Nome fantasia da organização',     exemplo: 'Studio Bellory' },
    { nome: 'data_agendamento',    descricao: 'Data do agendamento (dd/MM)',      exemplo: '15/03' },
    { nome: 'hora_agendamento',    descricao: 'Horário do agendamento',           exemplo: '14:30' },
    { nome: 'titulo_questionario', descricao: 'Título do questionário/anamnese',  exemplo: 'Anamnese padrão' },
    { nome: 'link_anamnese',       descricao: 'URL pública para responder',       exemplo: 'https://app.bellory.com.br/avaliacao/...' },
  ],
};
```

### Helper de preview

```ts
export function renderPreview(template: string, vars: VariavelTemplate[]): string {
  return vars.reduce(
    (texto, v) => texto.replaceAll(`{{${v.nome}}}`, v.exemplo),
    template
  );
}
```

---

## 7. Como adicionar suporte a novos tipos de notificação

Quando o backend introduzir um novo tipo (ex: `BOAS_VINDAS`, `ANIVERSARIO`,
`POS_ATENDIMENTO`):

1. Receber a lista de novos tipos do backend (extender `TipoNotificacao`).
2. Adicionar entrada em `HORAS_VALIDAS` e `VARIAVEIS`.
3. Adicionar opção no select do editor (5.2).
4. Atualizar a UI de listagem se o novo tipo tiver semântica diferente
   (ex: event-driven sem `horasAntes`).

A API REST (`/api/v1/config/notificacao/*`) **não muda** com novos tipos — é
uma feature 100% data-driven do lado do tenant.

---

## 8. Checklist para QA

- [ ] Listar configs ativas e renderizar tabela.
- [ ] Editar texto de CONFIRMACAO 24h → enviar mensagem real → texto chegou personalizado.
- [ ] Editar texto de LEMBRETE 2h → conferir mensagem.
- [ ] Editar texto de ANAMNESE → criar agendamento com serviço que tem questionário → mensagem chegou personalizada.
- [ ] Deletar config → criar novo agendamento → mensagem usa o padrão da plataforma.
- [ ] Toggle inativo → notificações daquele tipo deixam de ser enviadas.
- [ ] Tentar criar com `horasAntes` inválido → erro 400 amigável.
- [ ] Variáveis não preenchidas (ex: digitar `{{xpto}}`) aparecem literais no WhatsApp — comportamento esperado, mas o editor deve avisar.
