# Termo de Consentimento + Assinatura Digital — Especificação e Planejamento de Back-end

> **Status:** planejamento finalizado, pronto para implementação. Todas as decisões de arquitetura foram tomadas em conjunto com o time. Fatiado em 6 PRs incrementais.
>
> **Documentos relacionados:**
> - `docs/API_QUESTIONARIO_SPRINGBOOT.md` (estrutura existente do questionário)
> - `docs/API_RESPOSTA_QUESTIONARIO_SPRINGBOOT.md`
> - `docs/QUESTIONARIOS_FRONTEND_GUIDE.md`

---

## 1. Visão geral da feature

Permitir que um questionário contenha **blocos de termo de responsabilidade/consentimento** (ex.: ficha de anamnese pré-procedimento) e **blocos de assinatura digital** capturados via canvas no front (PNG base64). O cliente lê o termo, marca aceite explícito, assina, e o sistema persiste tudo com **prova legal**: snapshot do texto exato que o cliente viu, hash SHA-256, IP e User-Agent (reaproveitados do registro global da resposta), timestamp do aceite e arquivos de assinatura armazenados em pasta de sistema fora da cota do tenant.

### Casos de uso principais

1. **Ficha de anamnese** — cliente preenche perguntas de saúde + assina termo declarando veracidade. Encaixa diretamente no vínculo já existente `Servico.anamnese_id → Questionario` (V62).
2. **Termo pré-procedimento químico** — descreve riscos, cliente aceita ciência e autoriza.
3. **Autorização de uso de imagem** — opt-in/opt-out fotos antes/depois.
4. **Comprovante pós-atendimento** — cliente confirma recebimento de orientações pós.

### Decisão de design — substituição de variáveis (atualizada PR7)

**O servidor resolve as variáveis no GET do questionário** quando o front passa `?clienteId=&agendamentoId=&funcionarioId=` na URL. O `PerguntaDTO` retorna o campo `textoTermoRenderizado` populado com o texto pronto. O front exibe esse texto direto e devolve no POST.

- **Quando os IDs são passados:** servidor carrega entidades, valida ownership (mesmo tenant, agendamento pertence ao cliente, etc.), monta o mapa de variáveis e substitui `{{var}}` em todas as perguntas tipo TERMO_CONSENTIMENTO. Se algum recurso pertence a outro tenant → `403 Forbidden`.
- **Quando os IDs são ausentes:** comportamento legado — backend devolve `textoTermo` cru com placeholders. Mantido por compatibilidade.

**Por que mudou:** elimina o helper de variáveis no front, evita endpoints extras pra dados de cliente/serviço, centraliza joins no JPA. A semântica de prova legal não muda — front devolve o `textoTermoRenderizado` recebido, servidor calcula `sha256(textoTermoRenderizado)` no POST e congela.

**Implementado em:** `service/questionario/TemplateContextoBuilder.java` (carregamento + ownership + render); `QuestionarioService.buscarPorId(id, clienteId, agendamentoId, funcionarioId)` e `QuestionarioService.buscarPublicoPorSlug(...)` aceitam os IDs opcionais.

---

## 2. Decisões consolidadas

| # | Tema | Decisão |
|---|------|---------|
| 1 | Validação `textoTermo` | `@Size(min = 10, max = 10000)` (sanity check, sem regra de 50 caracteres). |
| 2 | Storage da assinatura | Pasta de sistema `_termos_assinaturas/` **fora da cota do plano**, via extensão do `ArquivoStorageService` aceitando `byte[]`. |
| 3 | Tipo de `dataAceite` | `LocalDateTime` (consistência com `RespostaQuestionario.dtResposta` e resto do projeto). |
| 4 | IP/UA por-pergunta | **Descartados.** Reusar `RespostaQuestionario.ipOrigem` e `userAgent` (globais por resposta). Manter apenas `dataAceite` por pergunta. |
| 5 | Soft-delete em `RespostaQuestionario` | **Implementar.** Adicionar `is_deletado`, `usuario_deletado`, `dt_deletado`. Hard-delete proibido quando há resposta com termo aceito. |
| 6 | PDF de comprovante | **Fase 1**, com **PDFBox** (Apache 2.0). Texto plano + imagem da assinatura. |
| 7 | Templates de termo | **Catálogo estático** (`TemplateTermoCatalog` in-memory). CRUD em tabela só na fase 2. |
| 8 | Imutabilidade dos campos de prova | **Sem trigger de banco.** `RespostaQuestionarioService` não tem método de update; cobrir por design + teste. |
| 9 | Status no `AgendamentoQuestionario` | **Dois campos separados** (opção A): `statusResposta` (existente) e novo `statusAssinatura` (`NAO_REQUERIDA`/`PENDENTE`/`ASSINADA`). Permite rastrear "respondido mas não assinou ainda". |
| 10 | `OpcaoResposta` para TERMO/ASSINATURA | Rejeitar. Tipos novos não aceitam opções. |
| 11 | Termo em questionário anônimo | Rejeitar. Anonimato é incompatível com termo legalmente válido. |
| 12 | Role para auditoria | `@PreAuthorize("hasRole('ADMIN')")` (padrão já adotado no `OrganizacaoController`). |
| 13 | Webhooks/N8N pós-assinatura | Fase 2, via `bellory-notificacao` (já existente). |

---

## 3. Mudanças no domínio

### 3.1. Enums

#### `TipoPergunta.java` — adicionar dois valores
```java
public enum TipoPergunta {
    // ... existentes
    TERMO_CONSENTIMENTO("Termo de Consentimento"),
    ASSINATURA("Assinatura Digital");
}
```

#### Novo `FormatoAssinatura.java`
```java
public enum FormatoAssinatura {
    PNG_BASE64,
    SVG
}
```

#### Novo `TipoTemplateTermo.java`
```java
public enum TipoTemplateTermo {
    PADRAO_BELLORY,
    PADRAO_PROCEDIMENTO,
    PADRAO_PROCEDIMENTO_QUIMICO,
    PADRAO_USO_IMAGEM,
    CUSTOM
}
```

#### Novo `StatusAssinatura.java`
```java
public enum StatusAssinatura {
    NAO_REQUERIDA,  // questionário não tem pergunta tipo ASSINATURA
    PENDENTE,       // pergunta de assinatura existe mas ainda não foi capturada
    ASSINADA        // assinatura capturada com sucesso
}
```

Todos mapeados com `@Enumerated(EnumType.STRING)`.

### 3.2. `Pergunta` — colunas novas

| Campo Java | Coluna SQL | Tipo | Obs. |
|---|---|---|---|
| `String textoTermo` | `texto_termo` | `TEXT` | Markdown com placeholders. Só populado quando `tipo = TERMO_CONSENTIMENTO`. |
| `TipoTemplateTermo templateTermoId` | `template_termo_id` | `VARCHAR(50)` | Auditoria do template de origem. |
| `Boolean requerAceiteExplicito` | `requer_aceite_explicito` | `BOOLEAN DEFAULT FALSE` | Se true, exige checkbox "Li e concordo". |
| `FormatoAssinatura formatoAssinatura` | `formato_assinatura` | `VARCHAR(20)` | Default `PNG_BASE64`. |
| `Integer larguraAssinatura` | `largura_assinatura` | `INTEGER` | Default 600 (range 200–1200). |
| `Integer alturaAssinatura` | `altura_assinatura` | `INTEGER` | Default 200 (range 100–600). |
| `Boolean exigirAssinaturaProfissional` | `exigir_assinatura_profissional` | `BOOLEAN DEFAULT FALSE` | Captura também a assinatura do funcionário. |

### 3.3. `RespostaPergunta` — colunas novas

| Campo Java | Coluna SQL | Tipo | Obs. |
|---|---|---|---|
| `Boolean aceitouTermo` | `aceitou_termo` | `BOOLEAN` | True quando o cliente marcou o aceite. |
| `LocalDateTime dataAceite` | `data_aceite` | `TIMESTAMP` | **Setada pelo servidor**, ignorar valor enviado pelo cliente. |
| `String textoTermoRenderizado` | `texto_termo_renderizado` | `TEXT` | Snapshot do termo com variáveis substituídas. NUNCA recalcular. |
| `Long arquivoAssinaturaClienteId` | `arquivo_assinatura_cliente_id` | `BIGINT` | FK lógico para `app.arquivo`. |
| `Long arquivoAssinaturaProfissionalId` | `arquivo_assinatura_profissional_id` | `BIGINT` | Quando `exigirAssinaturaProfissional = true`. |
| `String hashTermo` | `hash_termo` | `VARCHAR(64)` | SHA-256 de `textoTermoRenderizado`. |

> IP e User-Agent **não são duplicados aqui**. Já existem em `RespostaQuestionario.ipOrigem` e `userAgent`.

### 3.4. `AgendamentoQuestionario` — novo campo de status

| Campo Java | Coluna SQL | Tipo | Obs. |
|---|---|---|---|
| `StatusAssinatura statusAssinatura` | `status_assinatura` | `VARCHAR(20) NOT NULL DEFAULT 'NAO_REQUERIDA'` | Estado da assinatura, independente de `statusResposta`. |
| `LocalDateTime dtAssinatura` | `dt_assinatura` | `TIMESTAMP` | Marcada quando `statusAssinatura = ASSINADA`. |

O campo existente `status` (renomeado conceitualmente para `statusResposta`) continua com os 4 valores `PENDENTE`/`ENVIADO`/`RESPONDIDO`/`FALHOU`.

### 3.5. `RespostaQuestionario` — soft-delete

| Campo Java | Coluna SQL | Tipo |
|---|---|---|
| `boolean isDeletado` | `is_deletado` | `BOOLEAN DEFAULT FALSE NOT NULL` |
| `String usuarioDeletado` | `usuario_deletado` | `VARCHAR(255)` |
| `LocalDateTime dtDeletado` | `dt_deletado` | `TIMESTAMP` |

---

## 4. Validações de domínio (service)

### Na criação de pergunta (`QuestionarioService.criarPergunta`)
1. Se `tipo IN (TERMO_CONSENTIMENTO, ASSINATURA)` e `dto.opcoes` não-vazio → **400** `"Tipos TERMO/ASSINATURA não aceitam opções"`.
2. Se `questionario.anonimo == true` e `tipo IN (TERMO_CONSENTIMENTO, ASSINATURA)` → **400** `"Questionário anônimo não pode conter termo ou assinatura"`.
3. Se `tipo == TERMO_CONSENTIMENTO`:
   - `textoTermo` obrigatório, validado por `@Size(min=10, max=10000)`.
4. Se `tipo == ASSINATURA`:
   - `formatoAssinatura` obrigatório (default `PNG_BASE64` se nulo).
   - `larguraAssinatura` ∈ [200, 1200] (default 600).
   - `alturaAssinatura` ∈ [100, 600] (default 200).

### No registro de resposta (`RespostaQuestionarioService.registrarInterno`)
1. Para `tipo == TERMO_CONSENTIMENTO`:
   - Se `pergunta.requerAceiteExplicito == true` e `r.aceitouTermo != true` → **400**.
   - `r.textoTermoRenderizado` obrigatório.
   - Servidor seta: `dataAceite = LocalDateTime.now()`, `hashTermo = sha256(textoTermoRenderizado)`, `aceitouTermo`, `textoTermoRenderizado`.
2. Para `tipo == ASSINATURA`:
   - Se `pergunta.obrigatoria == true` e `r.assinaturaClienteBase64` vazio → **400**.
   - Se presente: decodificar+validar, gravar via `ArquivoStorageService.salvarAssinatura(...)`, setar `arquivoAssinaturaClienteId`.
   - Se `pergunta.exigirAssinaturaProfissional == true`: idem para profissional, obrigatório.
3. Atualização do tracking `AgendamentoQuestionario`:
   - Se questionário tem alguma pergunta tipo ASSINATURA:
     - Todas as assinaturas obrigatórias presentes → `statusAssinatura = ASSINADA`, `dtAssinatura = now()`.
     - Caso contrário → `statusAssinatura = PENDENTE`.
   - Se não tem → `statusAssinatura = NAO_REQUERIDA`.

### Validação de imagem (helper estático)
- Aceitar `data:image/png;base64,...` ou `data:image/svg+xml;base64,...`.
- Limite **200KB de bytes brutos** (descodificados).
- Validar **magic number**:
  - PNG: `89 50 4E 47 0D 0A 1A 0A`.
  - SVG: começa com `<svg` ou `<?xml`.
- SVG: sanitizar removendo `<script>`, `onload=`, `onerror=` (XSS).
- Rejeitar qualquer outro tipo.

---

## 5. Catálogo estático de templates

Em fase 1 não há tabela de templates. O `TemplateTermoCatalog` é um `@Component` com `Map<TipoTemplateTermo, TemplateInfo>` populado in-memory.

```java
record TemplateInfo(
    String nome,
    String descricao,
    String conteudo,           // Markdown com {{placeholders}}
    List<String> variaveis,
    boolean editavel           // false para templates Bellory
) {}
```

### Endpoint
```
GET /api/v1/questionarios/templates-termo
```

**Resposta:**
```json
[
  {
    "id": "PADRAO_PROCEDIMENTO",
    "nome": "Termo de Responsabilidade e Consentimento (Padrão)",
    "descricao": "Template padrão para procedimentos estéticos",
    "conteudo": "**TERMO DE RESPONSABILIDADE E CONSENTIMENTO**\n\nEu, **{{nomeCliente}}**, ...",
    "variaveis": ["nomeCliente", "cpfCliente", "nomeProfissional", "nomeEstabelecimento", "nomeProcedimento", "dataAtendimento"],
    "editavel": false
  }
]
```

### Variáveis suportadas (canônicas)

| Variável | Origem | Obrigatória |
|---|---|---|
| `{{nomeCliente}}` | Cliente vinculado à resposta | Sim |
| `{{cpfCliente}}` | Cliente | Sim |
| `{{rgCliente}}` | Cliente | Não |
| `{{telefoneCliente}}` | Cliente | Não |
| `{{nomeProfissional}}` | Colaborador do agendamento | Não |
| `{{nomeEstabelecimento}}` | Organização | Sim |
| `{{cnpjEstabelecimento}}` | Organização | Não |
| `{{enderecoEstabelecimento}}` | Organização | Não |
| `{{nomeProcedimento}}` | Serviço(s) do agendamento | Não |
| `{{dataAtendimento}}` | Agendamento | Não |
| `{{horarioAtendimento}}` | Agendamento | Não |

---

## 6. Storage da assinatura — pasta de sistema fora da cota

### Decisão arquitetural
Extender `ArquivoStorageService` para gravar em `_termos_assinaturas/` que **não conta na cota do plano** do tenant e **não aparece** na file-explorer UI. Cada assinatura grava em `{uploadDir}/{orgId}/_termos_assinaturas/{respostaPerguntaId}/{cliente|profissional}.png`.

### API nova no `ArquivoStorageService`
```java
public Arquivo salvarAssinatura(byte[] bytes, String extensao, Long organizacaoId, Long respostaPerguntaId, boolean profissional);
public byte[] lerAssinatura(Long arquivoId);
```

- Não invoca `LimiteValidatorService` (skip da cota).
- Persiste registro na tabela `app.arquivo` com `pasta_id = null` e flag interna que esconde da listagem normal do tenant.

---

## 7. Lógica do servidor no POST de resposta

Pseudocódigo do `RespostaQuestionarioService.registrarInterno(...)`:

```java
for (RespostaPerguntaCreateDTO r : dto.getRespostas()) {
    Pergunta p = perguntasMap.get(r.getPerguntaId());
    RespostaPergunta rp = new RespostaPergunta();
    // mapeamento normal...

    if (p.getTipo() == TipoPergunta.TERMO_CONSENTIMENTO) {
        if (Boolean.TRUE.equals(p.getRequerAceiteExplicito())
            && !Boolean.TRUE.equals(r.getAceitouTermo())) {
            throw new IllegalArgumentException("Aceite obrigatório do termo: " + p.getTexto());
        }
        if (StringUtils.isBlank(r.getTextoTermoRenderizado())) {
            throw new IllegalArgumentException("textoTermoRenderizado obrigatório");
        }
        rp.setAceitouTermo(r.getAceitouTermo());
        rp.setTextoTermoRenderizado(r.getTextoTermoRenderizado());
        rp.setHashTermo(sha256(r.getTextoTermoRenderizado()));
        rp.setDataAceite(LocalDateTime.now());
    }

    if (p.getTipo() == TipoPergunta.ASSINATURA) {
        if (StringUtils.isBlank(r.getAssinaturaClienteBase64())) {
            if (Boolean.TRUE.equals(p.getObrigatoria())) {
                throw new IllegalArgumentException("Assinatura do cliente obrigatória");
            }
        } else {
            byte[] bytes = decodificarEValidarAssinatura(r.getAssinaturaClienteBase64());
            Arquivo arq = arquivoStorageService.salvarAssinatura(
                bytes, "png", orgId, rp.getId(), false);
            rp.setArquivoAssinaturaClienteId(arq.getId());
        }
        if (Boolean.TRUE.equals(p.getExigirAssinaturaProfissional())) {
            if (StringUtils.isBlank(r.getAssinaturaProfissionalBase64())) {
                throw new IllegalArgumentException("Assinatura do profissional obrigatória");
            }
            byte[] bytes = decodificarEValidarAssinatura(r.getAssinaturaProfissionalBase64());
            Arquivo arq = arquivoStorageService.salvarAssinatura(
                bytes, "png", orgId, rp.getId(), true);
            rp.setArquivoAssinaturaProfissionalId(arq.getId());
        }
    }
}

// IP/UA/dispositivo já são capturados no nível de RespostaQuestionario (existente).
// Atualizar status do AgendamentoQuestionario após salvar:
if (saved.getAgendamentoId() != null) {
    boolean temAssinatura = questionario.getPerguntas().stream()
        .anyMatch(p -> p.getTipo() == TipoPergunta.ASSINATURA);
    boolean todasAssinaturasOk = !temAssinatura || verificarTodasAssinaturas(saved);

    StatusAssinatura novoStatus = !temAssinatura ? StatusAssinatura.NAO_REQUERIDA
        : todasAssinaturasOk ? StatusAssinatura.ASSINADA
        : StatusAssinatura.PENDENTE;

    aq.setStatusResposta(StatusQuestionarioAgendamento.RESPONDIDO);
    aq.setStatusAssinatura(novoStatus);
    if (novoStatus == StatusAssinatura.ASSINADA) {
        aq.setDtAssinatura(LocalDateTime.now());
    }
}
```

---

## 8. Endpoint de auditoria

```
GET /api/v1/resposta-questionario/{id}/auditoria
```

- Anotação: `@PreAuthorize("hasRole('ADMIN')")`.
- Carrega resposta + perguntas tipo TERMO/ASSINATURA.
- Recalcula `sha256(textoTermoRenderizado)` e compara com `hashTermo` salvo.
- Retorna DTO com:
  - `dataAceite` (por pergunta de termo)
  - `ipOrigem`, `userAgent`, `dispositivo` (do `RespostaQuestionario`)
  - `hashEsperado`, `hashCalculado`, `integridadeOk` (boolean)
  - URLs de download das assinaturas

### Endpoint auxiliar de download da imagem
```
GET /api/v1/resposta-questionario/{id}/assinatura/{cliente|profissional}
```
- Valida tenant + role (ADMIN ou cliente dono).
- Devolve PNG (`Content-Type: image/png`) lido via `arquivoStorageService.lerAssinatura(...)`.

---

## 9. Endpoint de PDF do comprovante

```
GET /api/v1/resposta-questionario/{id}/comprovante.pdf
```

### Implementação — Apache PDFBox 3.0.3 (Apache 2.0)

Dependência no `pom.xml`:
```xml
<dependency>
  <groupId>org.apache.pdfbox</groupId>
  <artifactId>pdfbox</artifactId>
  <version>3.0.3</version>
</dependency>
```

### Conteúdo do PDF
- Cabeçalho: logo da organização (se `organizacao.getLogoUrl()` existir) + nome do estabelecimento.
- Para cada `RespostaPergunta` tipo TERMO_CONSENTIMENTO:
  - Texto do termo (`textoTermoRenderizado`) — texto plano em fase 1; render Markdown completo fica para fase 2.
- Para cada tipo ASSINATURA:
  - Imagem da assinatura embedada (lida via `arquivoStorageService.lerAssinatura(arquivoAssinaturaClienteId)`).
- Linha de auditoria por bloco: "Aceito em DD/MM/YYYY HH:MM:SS · IP {ipOrigem} · Hash {hashTermo}".
- Rodapé: "Documento gerado eletronicamente pelo sistema Bellory."

### Permissões
- ADMIN do tenant; ou
- Cliente dono da resposta (validar `clienteId == TenantContext.getCurrentUserId()`).

---

## 10. LGPD e retenção

### Soft-delete obrigatório
Quando uma `RespostaQuestionario` contém ao menos uma pergunta tipo TERMO/ASSINATURA respondida, o método `RespostaQuestionarioService.deletar(...)`:
1. Verifica se há `RespostaPergunta` com `aceitou_termo = true` ou `arquivo_assinatura_*_id != null`.
2. Se houver → **soft-delete** (set `is_deletado=true`, `usuario_deletado=usuarioCorrente`, `dt_deletado=now()`).
3. Se não houver → mantém o hard-delete atual (back-compat com fluxos sem termo).

Todos os `findBy*` no repository devem filtrar `is_deletado = false`.

### Retenção
- Mínimo **5 anos** para documentos com termo aceito (cumprimento de obrigação legal — LGPD Art. 16, II).
- Endpoint admin de purga manual fica para fase 2; em fase 1 não há limpeza automática.

### Imutabilidade
- Sem trigger de banco em fase 1.
- Garantia por design: `RespostaQuestionarioService` não tem método público de update; só `criar()` e `deletar()` (soft).
- Cobrir com teste explícito: tentar invocar update via reflection ou repository direto deve falhar / não existir.

### Exportação para o cliente (LGPD Art. 18)
Fora de escopo da fase 1. Quando implementado, será reuso do endpoint de comprovante.pdf agregando todos os termos do cliente.

---

## 11. Compatibilidade retroativa

- Todas as colunas novas em `pergunta`, `resposta_pergunta`, `agendamento_questionario` e `resposta_questionario` são nullable (ou têm DEFAULT).
- Questionários antigos seguem funcionando sem alterações.
- Front antigo que envia `RespostaQuestionarioCreateDTO` sem os campos novos continua válido (servidor ignora ausência).
- Os tipos novos `TERMO_CONSENTIMENTO`/`ASSINATURA` só aparecem em questionários criados pós-deploy.

---

## 12. Itens fora de escopo (fase 2)

- CRUD de templates customizados por organização (tabela `template_termo`).
- Trigger de banco para imutabilidade dos campos de prova.
- Notificações via `bellory-notificacao` webhook (WhatsApp/email com PDF anexo).
- Exportação consolidada via `GET /cliente/{id}/termos-assinados`.
- Render Markdown completo no PDF (negrito, listas, tabelas).
- Criptografia por coluna em repouso (TDE Postgres).
- Endpoint admin de purga manual de respostas vencidas.
- Rate limit dinâmico no `PublicQuestionarioController` quando há pergunta tipo ASSINATURA.

---

## 13. Plano de PRs

**Branch base:** `dev`. **Migration de partida:** `V66__questionario_termo_assinatura.sql`.

### PR1 — Fundação: enums, schema e validações de criação (~1 dia)

**Objetivo:** permitir cadastrar perguntas tipo TERMO/ASSINATURA sem ainda processar respostas.

**Arquivos novos:**
- `model/entity/questionario/enums/FormatoAssinatura.java`
- `model/entity/questionario/enums/TipoTemplateTermo.java`

**Arquivos alterados:**
- `enums/TipoPergunta.java` — adicionar `TERMO_CONSENTIMENTO`, `ASSINATURA`.
- `entity/questionario/Pergunta.java` — adicionar campos da seção §3.2.
- `dto/questionario/PerguntaCreateDTO.java` — espelhar campos com `@Size(min=10, max=10000)` em `textoTermo`.
- `dto/questionario/PerguntaDTO.java` — espelhar campos.
- `service/questionario/QuestionarioService.criarPergunta()` — validações da seção §4 (criação).

**Migration `V66__questionario_termo_assinatura.sql`:**
```sql
ALTER TABLE app.pergunta
  ADD COLUMN texto_termo TEXT,
  ADD COLUMN template_termo_id VARCHAR(50),
  ADD COLUMN requer_aceite_explicito BOOLEAN DEFAULT FALSE,
  ADD COLUMN formato_assinatura VARCHAR(20),
  ADD COLUMN largura_assinatura INTEGER,
  ADD COLUMN altura_assinatura INTEGER,
  ADD COLUMN exigir_assinatura_profissional BOOLEAN DEFAULT FALSE;
```

**Checklist:**
- [ ] Enums novos criados
- [ ] `TipoPergunta` estendido
- [ ] Migration V66 aplicada e roda em ambiente dev
- [ ] Entidade `Pergunta` com novos campos
- [ ] DTOs atualizados
- [ ] Validação rejeita `opcoes` para TERMO/ASSINATURA
- [ ] Validação rejeita TERMO/ASSINATURA em questionário anônimo
- [ ] Validação dos ranges de largura/altura
- [ ] Testes: cadastro válido + 3 cenários de rejeição

---

### PR2 — Catálogo estático de templates (~0.5 dia)

**Objetivo:** front consegue listar e usar templates pré-definidos.

**Arquivos novos:**
- `service/questionario/TemplateTermoCatalog.java` — `@Component` com `Map<TipoTemplateTermo, TemplateInfo>`.
- `dto/questionario/TemplateTermoDTO.java`
- `controller/app/TemplateTermoController.java` — `GET /api/v1/questionarios/templates-termo`.

**Conteúdo dos 4 templates Bellory** escrito em Markdown com placeholders. Editável = false.

**Checklist:**
- [ ] Catálogo populado com 4 templates Bellory
- [ ] Endpoint retorna lista (autenticado, qualquer role)
- [ ] Sem autenticação → 401

**Depende de:** PR1 (enums).

---

### PR3 — Storage de assinatura em pasta de sistema (~1.5 dia)

**Objetivo:** extender `ArquivoStorageService` para aceitar `byte[]` em pasta `_termos_assinaturas/` fora da cota.

**Migration `V67__arquivo_is_sistema.sql`:**
```sql
ALTER TABLE app.arquivo
  ADD COLUMN is_sistema BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_arquivo_is_sistema ON app.arquivo(is_sistema);
```

> Coluna `is_sistema` marca registros internos do sistema (assinaturas) que não aparecem no file explorer do tenant nem contam na cota. Substitui a ideia inicial de "deixar pasta_id=null e filtrar pelo caminho", que era frágil.

**Arquivos alterados:**
- `entity/arquivo/Arquivo.java` — adicionar `boolean isSistema = false` com `@Builder.Default`.
- `repository/arquivo/ArquivoRepository`:
  - `findAllByOrganizacao_IdAndPastaIsNullAndIsSistemaFalseOrderByDtCriacaoDesc(Long)`
  - `findAllByOrganizacaoNaoSistema(Long)` (`@Query`)
  - `contarArquivosNaoSistema(Long)` (`@Query`)
- `service/ArquivoStorageService.java`:
  - Constante `PASTA_SISTEMA_TERMOS_ASSINATURAS = "_termos_assinaturas"`.
  - `Set<String> SUBDIRETORIOS_FORA_COTA` — clientes + `_termos_assinaturas` (excluídos do `walk` de disco em `calcularTamanhoDiretorio`/`contarArquivosDiretorio`).
  - `salvarAssinatura(byte[], String extensao, String contentType, Long orgId, Long respostaPerguntaId, boolean profissional, Long criadoPor) → Arquivo` — grava em `_termos_assinaturas/{respostaPerguntaId}/{cliente|profissional}.png`, persiste `Arquivo` com `is_sistema=true` e `pasta=null`.
  - `lerAssinatura(Long arquivoId, Long orgId) → byte[]` — valida que é arquivo de sistema antes de devolver bytes.
  - Helper privado `garantirNaoSistema(Arquivo)` — chamado em `deletarArquivo`/`moverArquivo`/`renomearArquivo` para impedir que rota normal de arquivos toque em assinaturas.
  - Listagens trocadas para versões "NaoSistema" / "IsSistemaFalse".

**Helper estático em `service/questionario/AssinaturaImagemValidator.java`:**
- `decodificarEValidar(String dataUrl) → Resultado(bytes, formato)`.
- Aceita `data:image/png;base64,...` e `data:image/svg+xml;base64,...`.
- Limite 200KB descodificados.
- Valida magic number PNG (`89 50 4E 47 0D 0A 1A 0A`).
- Aceita SVG começando com `<svg` ou `<?xml`.
- Sanitiza SVG removendo `<script>`, event handlers `on*=`, `javascript:` URIs e `<foreignObject>`.

**Checklist:**
- [x] Migration V67 aplicada
- [x] Coluna `is_sistema` em `app.arquivo`
- [x] Repository com queries que filtram `is_sistema = false`
- [x] Métodos novos no `ArquivoStorageService` (`salvarAssinatura`, `lerAssinatura`)
- [x] Helper `AssinaturaImagemValidator`
- [x] Pasta de sistema criada automaticamente
- [x] `_termos_assinaturas` excluída do walk de disco (não conta na cota)
- [x] Listagens normais não retornam arquivos `is_sistema=true`
- [x] `deletar/mover/renomear` rejeitam arquivos `is_sistema=true`

**Depende de:** independente, pode ir em paralelo com PR1/PR2.

---

### PR4 — Service de resposta + status duplo no `AgendamentoQuestionario` (~3 dias)

**Objetivo:** POST de resposta processa termo/assinatura corretamente, com auditoria e status duplo.

**Arquivos novos:**
- `entity/agendamento/StatusAssinatura.java` — enum.

**Arquivos alterados:**
- `entity/agendamento/AgendamentoQuestionario.java` — campos `statusAssinatura`, `dtAssinatura`.
- `entity/questionario/RespostaPergunta.java` — campos da seção §3.3.
- `dto/questionario/RespostaPerguntaCreateDTO.java` — adicionar `aceitouTermo`, `textoTermoRenderizado`, `assinaturaClienteBase64`, `assinaturaProfissionalBase64`.
- `dto/questionario/RespostaPerguntaDTO.java` — adicionar campos de leitura (URLs via endpoint download, não base64 inline).
- `service/questionario/RespostaQuestionarioService`:
  - `registrarInterno()` — lógica completa da §7.
  - `validarRespostaPorTipo()` — adicionar `case TERMO_CONSENTIMENTO` e `case ASSINATURA`.
  - Atualização do tracking do `AgendamentoQuestionario` com status duplo.

**Migration `V68__resposta_pergunta_termo.sql`:**
```sql
ALTER TABLE app.resposta_pergunta
  ADD COLUMN aceitou_termo BOOLEAN,
  ADD COLUMN data_aceite TIMESTAMP,
  ADD COLUMN texto_termo_renderizado TEXT,
  ADD COLUMN arquivo_assinatura_cliente_id BIGINT,
  ADD COLUMN arquivo_assinatura_profissional_id BIGINT,
  ADD COLUMN hash_termo VARCHAR(64);

ALTER TABLE app.agendamento_questionario
  ADD COLUMN status_assinatura VARCHAR(20) NOT NULL DEFAULT 'NAO_REQUERIDA',
  ADD COLUMN dt_assinatura TIMESTAMP;
```

**Checklist:**
- [x] Migration V68 aplicada
- [x] `RespostaPergunta` com novos campos
- [x] `AgendamentoQuestionario` com `statusAssinatura` e `dtAssinatura`
- [x] DTOs atualizados (`RespostaPerguntaCreateDTO` recebe payloads; `RespostaPerguntaDTO` expõe IDs de arquivo + dataAceite + hashTermo)
- [x] Enum `StatusAssinatura` (`NAO_REQUERIDA`, `PENDENTE`, `ASSINADA`)
- [x] Service trata TERMO: hash SHA-256 calculado pelo servidor, `dataAceite` setado via `LocalDateTime.now()`, aceite explicito validado quando `requerAceiteExplicito = true`
- [x] Service trata ASSINATURA: `AssinaturaImagemValidator` decodifica base64, persiste via `ArquivoStorageService.salvarAssinatura()` com `is_sistema=true`
- [x] Persistencia em duas fases (`saveAndFlush` para gerar IDs, depois grava arquivos sob `_termos_assinaturas/{respostaPerguntaId}/`)
- [x] Status duplo correto pos-POST: `NAO_REQUERIDA` (sem pergunta ASSINATURA), `ASSINADA` (todas obrigatorias capturadas) ou `PENDENTE` (incompleto)
- [x] `formatarResposta` no DTO de leitura cobre TERMO/ASSINATURA

**Depende de:** PR1 + PR3.

---

### PR5 — Soft-delete LGPD + endpoint de auditoria (~1.5 dia)

**Objetivo:** cumprir retenção legal e expor consulta auditável.

**Migration `V69__resposta_questionario_soft_delete.sql`:**
```sql
ALTER TABLE app.resposta_questionario
  ADD COLUMN is_deletado BOOLEAN DEFAULT FALSE NOT NULL,
  ADD COLUMN usuario_deletado VARCHAR(255),
  ADD COLUMN dt_deletado TIMESTAMP;

CREATE INDEX idx_resposta_questionario_deletado
  ON app.resposta_questionario(is_deletado);
```

**Arquivos alterados:**
- `entity/questionario/RespostaQuestionario.java` — campos soft-delete.
- `repository/questionario/RespostaQuestionarioRepository` — todas queries de listagem filtram `is_deletado = false`.
- `service/questionario/RespostaQuestionarioService.deletar(Long id, String usuario)`:
  - Soft-delete quando há termo/assinatura presente; hard-delete caso contrário.
- `controller/app/RespostaQuestionarioController` — propagar usuário corrente para o `deletar`.

**Arquivos novos:**
- `dto/questionario/AuditoriaTermoDTO.java` — com sub-records `TermoAceito` e `AssinaturaCapturada`.
- `controller/app/AuditoriaTermoController.java`:
  - `GET /api/v1/resposta-questionario/{id}/auditoria` — validação de `ROLE_ADMIN`/`ROLE_SUPERADMIN` manual via `TenantContext.getCurrentRole()`.
  - `GET /api/v1/resposta-questionario/{id}/assinatura/{cliente|profissional}?perguntaId={id}` — download da imagem (PNG/SVG detectado por magic number); ADMIN ou cliente dono.

> **Nota sobre `@PreAuthorize`:** o projeto não tem `@EnableMethodSecurity`, então `@PreAuthorize` em controllers do projeto inteiro é decoração inerte. Validação de role é feita manualmente via `TenantContext.getCurrentRole()` para evitar regressão em outros controllers que dependeriam de uma habilitação global.

**Checklist:**
- [x] Migration V69 aplicada
- [x] Soft-delete funciona quando há termo aceito ou assinatura presente
- [x] Hard-delete preservado para respostas sem termo nem assinatura
- [x] Listagens do `RespostaQuestionarioRepository` filtram `is_deletado = false` (exceto `findByIdWithRespostas` — auditoria precisa enxergar soft-deleted)
- [x] Endpoint de auditoria recalcula hash SHA-256 e retorna `integridadeOk`
- [x] `GET /auditoria` sem ADMIN → 403 (validação manual no controller)
- [x] Download de assinatura valida tenant + permissão (ADMIN ou cliente dono)
- [x] `RespostaQuestionarioController.deletar` propaga `usuarioDeletado` via `TenantContext.getCurrentUsername()`
- [x] Idempotência: deletar resposta já soft-deletada é no-op

**Depende de:** PR4.

---

### PR6 — Geração de PDF com PDFBox (~2 dias)

**Objetivo:** endpoint que gera PDF do comprovante para arquivamento e LGPD Art. 18.

**Dependência nova (`pom.xml`):**
```xml
<dependency>
  <groupId>org.apache.pdfbox</groupId>
  <artifactId>pdfbox</artifactId>
  <version>3.0.3</version>
</dependency>
```

**Arquivos novos:**
- `service/questionario/ComprovanteTermoPdfService.java`:
  - `byte[] gerarPdf(RespostaQuestionario resposta)`:
    - Cabeçalho com logo + nome.
    - Texto plano dos termos (sem render Markdown completo em fase 1).
    - Imagem da assinatura embedada.
    - Linha de auditoria por bloco.
    - Rodapé com identificação do sistema.
- `controller/app/ComprovanteTermoController.java`:
  - `GET /api/v1/resposta-questionario/{id}/comprovante.pdf`.
  - Permissão: ADMIN do tenant ou cliente dono.
  - Content-Type: `application/pdf`.

**Checklist:**
- [x] Dependência PDFBox 3.0.3 no pom.xml
- [x] `ComprovanteTermoPdfService` gera PDF com PDFBox: cabeçalho org (nome+CNPJ), termos como texto plano com word-wrap, assinaturas embutidas, linha de auditoria por bloco e rodapé
- [x] Conversão Markdown básica → texto plano (negrito, itálico, headings, listas)
- [x] Sanitização de caracteres fora de WinAnsi para evitar exception de encoding
- [x] Quebra de página automática quando conteúdo excede A4
- [x] Embedding via `PDImageXObject.createFromByteArray` (apenas PNG suportado em fase 1; SVG mostra placeholder de texto)
- [x] `ComprovanteTermoController` com `GET /api/v1/resposta-questionario/{id}/comprovante.pdf` retornando `application/pdf` inline
- [x] Permissão validada manualmente (ADMIN/SUPERADMIN ou cliente dono via `TenantContext`)
- [x] Resposta inexistente → 404; sem permissão → 403
- [x] Comprovante acessível mesmo após soft-delete (importante para LGPD)

> **Observação:** logo da organização (`Organizacao.logoUrl`) **não é embutida** em fase 1 — implicaria fetch HTTP externo (SSRF) ou resolver caminho local complexo. Cabeçalho usa apenas nome fantasia + CNPJ. Logo fica como TODO.

**Depende de:** PR3 + PR4.

---

### PR7 — Resolução server-side das variáveis no GET (~0.5 dia)

**Objetivo:** simplificar o front-end transferindo a substituição de placeholders `{{var}}` para o backend. Quando o front passa `?clienteId=&agendamentoId=&funcionarioId=` no GET do questionário, o servidor resolve e devolve `PerguntaDTO.textoTermoRenderizado` pronto.

**Sem migration.**

**Arquivos novos:**
- `service/questionario/TemplateContextoBuilder.java` — `@Service` que carrega `Cliente`/`Agendamento`/`Funcionario`/`Organizacao`, valida ownership (mesmo tenant + agendamento pertence ao cliente) e retorna `Map<String,String>` com as 11 variáveis canônicas. Helper `renderizar(String, Map)` usa `Pattern.compile("\\{\\{(\\w+)\\}\\}")`.

**Arquivos alterados:**
- `dto/questionario/PerguntaDTO.java` — campo `textoTermoRenderizado` (opcional, populado pelo servidor).
- `service/questionario/QuestionarioService` — overloads `buscarPorId(id, clienteId, agendamentoId, funcionarioId)` e `buscarPublicoPorSlug(id, orgId, clienteId, agendamentoId, funcionarioId)`. Helper privado `aplicarRenderizacaoTermo(...)` que itera perguntas TERMO e popula `textoTermoRenderizado`. Chamadas legadas (`buscarPorId(id)`) viraram delegations com IDs null — sem breaking change.
- `controller/app/PublicQuestionarioController` e `controller/app/QuestionarioController` — endpoints `GET` aceitam `@RequestParam(required = false) Long clienteId`, `agendamentoId`, `funcionarioId`. Tratam `SecurityException` → 403.

**Variáveis canônicas resolvidas:**
| Variável | Origem |
|---|---|
| `{{nomeCliente}}` | `Cliente.nomeCompleto` |
| `{{cpfCliente}}` | `Cliente.cpf` |
| `{{telefoneCliente}}` | `Cliente.telefone` |
| `{{rgCliente}}` | (resolve para vazio — modelo atual não tem campo) |
| `{{nomeProfissional}}` | `Funcionario.nomeCompleto` (informado ou único do agendamento) |
| `{{nomeEstabelecimento}}` | `Organizacao.nomeFantasia` |
| `{{cnpjEstabelecimento}}` | `Organizacao.cnpj` |
| `{{enderecoEstabelecimento}}` | `Organizacao.enderecoPrincipal` formatado |
| `{{nomeProcedimento}}` | nomes dos serviços do agendamento (joined com `, `) |
| `{{dataAtendimento}}` | `Agendamento.dtAgendamento` formatada `dd/MM/yyyy` |
| `{{horarioAtendimento}}` | `Agendamento.dtAgendamento` formatada `HH:mm` |

**Validações de ownership:**
- Cliente pertence à organização do questionário → senão 403.
- Agendamento pertence à organização do questionário → senão 403.
- Se `clienteId` e `agendamentoId` forem ambos passados, agendamento.cliente.id deve bater com clienteId → senão 403.
- Funcionário pertence à organização do questionário → senão 403.

**Compatibilidade retroativa:** chamadas sem os params seguem retornando texto cru (placeholder). Front antigo continua funcionando.

**Checklist:**
- [x] `TemplateContextoBuilder` com builder + renderizador
- [x] `PerguntaDTO.textoTermoRenderizado`
- [x] Overloads em `QuestionarioService`
- [x] Params opcionais nos 2 controllers
- [x] Validações de ownership lançam `SecurityException` → 403

**Depende de:** PR1 (estrutura de TERMO_CONSENTIMENTO).

---

## 14. Ordem de merge e cronograma

| Ordem | PR | Estimativa | Depende de |
|---|---|---|---|
| 1 | PR1 — Fundação | 1 dia | — |
| 2 | PR2 — Templates | 0.5 dia | PR1 |
| 3 | PR3 — Storage | 1.5 dia | independente, paralelo |
| 4 | PR4 — Service de resposta | 3 dias | PR1 + PR3 |
| 5 | PR5 — Soft-delete + auditoria | 1.5 dia | PR4 |
| 6 | PR6 — PDF | 2 dias | PR3 + PR4 |
| 7 | PR7 — Resolução server-side | 0.5 dia | PR1 |

**Total estimado:** ~10 dias de implementação + revisões.

PR2 e PR3 podem rodar em paralelo enquanto PR1 ainda não mergeou de fato (PR2 só precisa dos enums de PR1, e PR3 é totalmente independente).

---

## 15. Considerações finais

1. **Validade jurídica:** prova baseada em IP + UA + timestamp + hash + assinatura é aceita pela MP 2.200-2/2001 e LGPD para fins de comprovação de aceite, mas **não substitui assinatura digital ICP-Brasil** para documentos oficiais. Para a maioria dos salões/barbearias, é mais que suficiente.

2. **GDPR/LGPD:** assinatura é dado biométrico-comportamental. Recomenda-se ativar encryption-at-rest do provedor (RDS, GCP CloudSQL — todos têm por padrão). Criptografia por coluna fica para fase 2.

3. **Backup:** garantir que `resposta_pergunta`, `resposta_questionario` e a pasta `_termos_assinaturas` estão nos backups com retenção mínima de 5 anos.

4. **Performance:** assinaturas armazenadas como arquivo (não inline em base64) preservam performance das queries de listagem. Não é necessário criar `RespostaPerguntaResumoDTO` específico para isso.

5. **Vínculo com anamnese existente:** a feature encaixa diretamente no `Servico.anamnese_id → Questionario` (V62). Basta o admin criar perguntas TERMO/ASSINATURA no questionário linkado. Sem alteração estrutural necessária.
