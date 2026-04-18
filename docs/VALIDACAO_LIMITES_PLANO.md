# Validação de Limites por Plano

## Visão Geral

Toda operação de **criação** de recursos no Bellory deve validar se a organização ainda tem cota disponível no plano contratado. A validação é feita pelo `LimiteValidatorService` antes de persistir o recurso.

## Como Funciona

```
Usuário → Controller → Service.create() → LimiteValidatorService.validar() → Persist
                                                    ↓
                                          Plano excedido? → LimitePlanoExcedidoException (HTTP 422)
```

1. O service conta quantos recursos daquele tipo a organização já possui
2. Chama `limiteValidator.validar(orgId, TipoLimite.X, totalAtual + 1)`
3. O validator busca o limite do plano no cache Redis (CachedCustomerStatus)
4. Se `totalAtual + 1 > limite` → lança `LimitePlanoExcedidoException`
5. O `GlobalExceptionHandler` converte para HTTP 422 com payload:

```json
{
  "success": false,
  "message": "Limite de clientes excedido. Plano Básico permite até 50 (tentativa: 51). Faça upgrade para ampliar.",
  "errorCode": 422,
  "data": {
    "limitKey": "cliente",
    "limiteMaximo": 50,
    "usoAtual": 51
  }
}
```

## Tipos de Limite

| Tipo | Key | Tipo Validação | Descrição |
|------|-----|----------------|-----------|
| `NUMBER` | — | Compara `total + 1 <= limite` | Limite numérico (ex: máx 10 funcionários) |
| `BOOLEAN` | — | Verifica `enabled == true` | Feature on/off (ex: site externo habilitado) |
| `UNLIMITED` | — | Sempre liberado | Sem restrição |

## Padrão de Implementação

```java
// No service, ANTES de qualquer persistência:
long totalAtual = repository.countByOrganizacao_IdAndIsDeletadoFalse(organizacaoId);
limiteValidator.validar(organizacaoId, TipoLimite.FUNCIONARIO, (int) (totalAtual + 1));
```

Para features BOOLEAN (on/off):
```java
limiteValidator.validarFeatureHabilitada(organizacaoId, TipoLimite.SITE_EXTERNO);
```

---

## Status por Módulo

### ✅ CLIENTE — Implementado
- **Service:** `ClienteService.createCliente()` (linha 117)
- **Validação:** linhas 121-122
- **Count:** `clienteRepository.countByOrganizacao_Id(organizacaoId)`
- **TipoLimite:** `CLIENTE`

```java
long totalAtual = clienteRepository.countByOrganizacao_Id(organizacaoId);
limiteValidator.validar(organizacaoId, TipoLimite.CLIENTE, (int) (totalAtual + 1));
```

---

### ✅ FUNCIONARIO — Implementado
- **Service:** `FuncionarioService.postNewFuncionario()` (linha 81)
- **Validação:** linhas 89-90
- **Count:** `funcionarioRepository.countByOrganizacao_IdAndIsDeletadoFalse(organizacaoId)`
- **TipoLimite:** `FUNCIONARIO`

```java
long totalAtual = funcionarioRepository.countByOrganizacao_IdAndIsDeletadoFalse(organizacaoId);
limiteValidator.validar(organizacaoId, TipoLimite.FUNCIONARIO, (int) (totalAtual + 1));
```

---

### ✅ AGENDAMENTO — Implementado
- **Service:** `AgendamentoService.createAgendamentoCompleto()` (linha 243)
- **Validação:** linhas 250-251
- **Count:** `agendamentoRepository.countByOrganizacaoAndPeriodo(orgId, inicioMes, fimMes)` (mensal)
- **TipoLimite:** `AGENDAMENTO`

```java
YearMonth mesAtual = YearMonth.now();
LocalDateTime inicioMes = mesAtual.atDay(1).atStartOfDay();
LocalDateTime fimMes = mesAtual.atEndOfMonth().atTime(23, 59, 59);
long totalMesAtual = agendamentoRepository.countByOrganizacaoAndPeriodo(organizacaoId, inicioMes, fimMes);
limiteValidator.validar(organizacaoId, TipoLimite.AGENDAMENTO, (int) (totalMesAtual + 1));
```

> **Nota:** O limite de agendamento é **mensal**, não total.

---

### ✅ SITE_EXTERNO — Implementado
- **Service:** `LandingPageEditorService.create()` (linha 102)
- **Validação:** linhas 106-107
- **Count:** `landingPageRepository.countByOrganizacaoIdAndAtivoTrue(orgId)`
- **TipoLimite:** `SITE_EXTERNO`

```java
long totalAtual = landingPageRepository.countByOrganizacaoIdAndAtivoTrue(orgId);
limiteValidator.validar(orgId, TipoLimite.SITE_EXTERNO, (int) (totalAtual + 1));
```

---

### ✅ AGENTE_VIRTUAL — Implementado
- **Service:** `InstanceService.createInstance()` (linha 74)
- **Validação:** linhas 91-92 (apenas chamadas externas, internas bypassed)
- **Count:** `instanceRepository.countByOrganizacaoIdAndDeletadoFalse(organizacaoId)`
- **TipoLimite:** `AGENTE_VIRTUAL`

```java
if (!interno) {
    long totalAtual = instanceRepository.countByOrganizacaoIdAndDeletadoFalse(organizacaoId);
    limiteValidator.validar(organizacaoId, TipoLimite.AGENTE_VIRTUAL, (int) (totalAtual + 1));
}
```

---

### ❌ SERVICO — Falta implementar
- **Service:** `ServicoService.createServico()` (linha 62)
- **Count disponível:** `servicoRepository.countByOrganizacao_IdAndIsDeletadoFalse(organizacaoId)`
- **TipoLimite:** `SERVICO`

**O que fazer:** Adicionar no início do método `createServico()`, após obter o `organizacaoId`:

```java
// Validar limite do plano
long totalAtual = servicoRepository.countByOrganizacao_IdAndIsDeletadoFalse(organizacaoId);
limiteValidator.validar(organizacaoId, TipoLimite.SERVICO, (int) (totalAtual + 1));
```

**Injetar:** `LimiteValidatorService limiteValidator` no construtor do `ServicoService`.

---

### ❌ API — Falta implementar
- **Service:** `ApiKeyService.generateApiKey()` (linha 42)
- **Count necessário:** Criar `apiKeyRepository.countByOrganizacao_IdAndAtivoTrue(Long orgId)`
- **TipoLimite:** `API`

**O que fazer:**

1. Adicionar no `ApiKeyRepository`:
```java
long countByOrganizacao_IdAndAtivoTrue(Long organizacaoId);
```

2. No início do `generateApiKey()`, após obter `userInfo`:
```java
long totalAtual = apiKeyRepository.countByOrganizacao_IdAndAtivoTrue(userInfo.getOrganizacao().getId());
limiteValidator.validar(userInfo.getOrganizacao().getId(), TipoLimite.API, (int) (totalAtual + 1));
```

**Injetar:** `LimiteValidatorService limiteValidator` no construtor do `ApiKeyService`.

---

### ❌ ARQUIVOS — Falta implementar
- **Service:** `ArquivoStorageService.uploadArquivos()` (linha 84)
- **Count necessário:** Criar `arquivoRepository.countByOrganizacao_IdAndDeletadoFalse(Long orgId)`
- **TipoLimite:** `ARQUIVOS`

**O que fazer:**

1. Adicionar no `ArquivoRepository`:
```java
long countByOrganizacao_IdAndDeletadoFalse(Long organizacaoId);
```

2. No início de `uploadArquivos()`, após `verificarLimiteStorage`:
```java
long totalAtual = arquivoRepository.countByOrganizacao_IdAndDeletadoFalse(organizacaoId);
limiteValidator.validar(organizacaoId, TipoLimite.ARQUIVOS, (int) (totalAtual + files.size()));
```

> **Nota:** Usa `+ files.size()` em vez de `+ 1` porque o upload aceita múltiplos arquivos.

**Injetar:** `LimiteValidatorService limiteValidator` no construtor do `ArquivoStorageService`.

---

### ⚠️ RELATORIOS — Feature toggle (sem contagem)
- **Services:** `RelatorioAgendamentoService`, `RelatorioClienteService`, `RelatorioFaturamentoService`, etc.
- **TipoLimite:** `RELATORIOS` (BOOLEAN)

Os relatórios são gerados on-the-fly, não persistidos. A validação é do tipo **feature gate** (habilitado/desabilitado no plano).

**O que fazer:** No início de cada método `gerarRelatorio()`:
```java
Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
limiteValidator.validarFeatureHabilitada(organizacaoId, TipoLimite.RELATORIOS);
```

---

### ⚠️ UNIDADE — Módulo não implementado
- **TipoLimite:** `UNIDADE`
- **Status:** Não existe entidade/service para filiais no projeto atual. Quando o módulo de filiais for criado, seguir o mesmo padrão.

---

## Checklist de Implementação

| Módulo | Service | Validação | Ação |
|--------|---------|-----------|------|
| Cliente | `ClienteService.createCliente` | ✅ | — |
| Funcionário | `FuncionarioService.postNewFuncionario` | ✅ | — |
| Agendamento | `AgendamentoService.createAgendamentoCompleto` | ✅ | — |
| Site Externo | `LandingPageEditorService.create` | ✅ | — |
| Agente Virtual | `InstanceService.createInstance` | ✅ | — |
| **Serviço** | `ServicoService.createServico` | ❌ | Adicionar validar + injetar validator |
| **API Key** | `ApiKeyService.generateApiKey` | ❌ | Adicionar count no repo + validar |
| **Arquivos** | `ArquivoStorageService.uploadArquivos` | ❌ | Adicionar count no repo + validar |
| **Relatórios** | `Relatorio*Service.gerarRelatorio` | ❌ | Adicionar validarFeatureHabilitada |
| Unidade | — | ⚠️ | Módulo não existe ainda |

## Princípios

- **Fail-open:** Se o cache Redis estiver indisponível, a operação é **liberada** (não bloqueia o usuário).
- **Validar antes de persistir:** A chamada ao `limiteValidator` deve ser a **primeira coisa** após obter o `organizacaoId`.
- **Soft delete conta:** Recursos com `isDeletado = true` **não** são contados (não consomem cota).
- **Agendamento é mensal:** O count de agendamentos considera apenas o mês corrente.
- **Upload em lote:** Para uploads múltiplos, somar `files.size()` em vez de `+ 1`.
