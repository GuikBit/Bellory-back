# Mudanças no Frontend - Sistema de Assinaturas

> Documento gerado com base nas melhorias implementadas no backend do sistema de assinaturas Bellory.

---

## 1. Alterações nos Tipos TypeScript

### 1.1 `AssinaturaResponse` — Campos removidos e adicionados

```diff
interface AssinaturaResponse {
  id: number;
  plano: PlanoResponse;
  status: StatusAssinatura;
  situacao: SituacaoAssinatura;
  cicloCobranca: string;
  dtInicio: string;
  dtProximoVencimento: string | null;
  dtFimTrial: string | null;
  diasRestantesTrial: number | null;
  assasSubscriptionId: string | null;
  cupomAplicado: string | null;
- valorMensal: number | null;
- valorAnual: number | null;
+ creditoProRata: number | null;
+ planoAnteriorCodigo: string | null;
}
```

### 1.2 `StatusAssinatura` — 3 novos valores

```diff
type StatusAssinatura =
  | 'TRIAL'
+ | 'AGUARDANDO_PAGAMENTO'
  | 'ATIVA'
+ | 'UPGRADE_PENDENTE'
+ | 'DOWNGRADE_AGENDADO'
  | 'VENCIDA'
  | 'CANCELADA'
  | 'SUSPENSA';
```

### 1.3 `SituacaoAssinatura` — 3 novos valores

```diff
type SituacaoAssinatura =
  | 'TRIAL_ATIVO'
  | 'TRIAL_EXPIRANDO'
  | 'TRIAL_EXPIRADO'
+ | 'AGUARDANDO_PAGAMENTO'
  | 'ATIVA'
  | 'ATIVA_RENOVACAO_PROXIMA'
+ | 'UPGRADE_PENDENTE'
+ | 'DOWNGRADE_AGENDADO'
  | 'INADIMPLENTE'
  | 'SUSPENSA'
  | 'CANCELADA_ATIVA'
  | 'CANCELADA_ENCERRADA';
```

---

## 2. Novo Endpoint — Preview de Troca de Plano

### `POST /api/v1/assinatura/preview-troca-plano`

**Request:**
```typescript
interface TrocarPlanoDTO {
  codigoPlano: string;
  cicloCobranca: string; // 'MENSAL' | 'ANUAL'
}
```

**Response:**
```typescript
interface ProRataPreviewDTO {
  planoAtualCodigo: string;
  planoAtualNome: string;
  novoPlanoCodigo: string;
  novoPlanoNome: string;
  cicloCobranca: string;
  valorAtualProporcional: number;
  valorNovoProporcional: number;
  valorProRata: number;
  diasRestantesCiclo: number;
  diasTotalCiclo: number;
  isUpgrade: boolean;
  mensagem: string;
}
```

**Service function sugerida:**
```typescript
export async function previewTrocaPlano(data: TrocarPlanoDTO): Promise<ProRataPreviewDTO> {
  const response = await api.post('/assinatura/preview-troca-plano', data);
  return response.data;
}
```

---

## 3. Atualização da Tabela de Status Chips

Adicionar os novos status na renderização de chips/badges:

| Status | Cor sugerida | Label |
|--------|-------------|-------|
| `AGUARDANDO_PAGAMENTO` | `yellow` / `amber` | Aguardando Pagamento |
| `UPGRADE_PENDENTE` | `blue` | Upgrade Pendente |
| `DOWNGRADE_AGENDADO` | `orange` | Downgrade Agendado |

**Exemplo de implementação:**
```typescript
function getStatusChip(status: StatusAssinatura) {
  const config: Record<StatusAssinatura, { color: string; label: string }> = {
    TRIAL: { color: 'purple', label: 'Trial' },
    AGUARDANDO_PAGAMENTO: { color: 'amber', label: 'Aguardando Pagamento' },
    ATIVA: { color: 'green', label: 'Ativa' },
    UPGRADE_PENDENTE: { color: 'blue', label: 'Upgrade Pendente' },
    DOWNGRADE_AGENDADO: { color: 'orange', label: 'Downgrade Agendado' },
    VENCIDA: { color: 'red', label: 'Vencida' },
    CANCELADA: { color: 'gray', label: 'Cancelada' },
    SUSPENSA: { color: 'red', label: 'Suspensa' },
  };
  return config[status];
}
```

---

## 4. Renderização Condicional por `situacao`

### 4.1 Novos casos na UI

| Situação | Componente / Comportamento |
|----------|---------------------------|
| `AGUARDANDO_PAGAMENTO` | Exibir alerta amarelo: "Seu pagamento está sendo processado. O acesso será liberado após a confirmação." Desabilitar botão de trocar plano. |
| `UPGRADE_PENDENTE` | Exibir alerta azul: "Upgrade em processamento. Aguardando confirmação do pagamento pro-rata." Mostrar valor do `creditoProRata` se disponível. Desabilitar botão de trocar plano. |
| `DOWNGRADE_AGENDADO` | Exibir alerta informativo: "Downgrade agendado para o próximo ciclo. Você continuará com o plano atual até {dtProximoVencimento}." Mostrar `planoAnteriorCodigo` → plano destino. Permitir cancelar downgrade (chamar trocar plano de volta para o atual). |

### 4.2 Bloqueio de acesso (Alerta bloqueante)

O `AssinaturaInterceptor` no backend retorna `403` para os status bloqueados. No frontend, tratar o 403 para exibir a tela de bloqueio nos seguintes status:

| Status | Bloqueado? | Mensagem |
|--------|-----------|----------|
| `AGUARDANDO_PAGAMENTO` | **Sim** | "Aguardando confirmação do pagamento para liberar o acesso." |
| `UPGRADE_PENDENTE` | Não | Acesso liberado (mantém plano atual) |
| `DOWNGRADE_AGENDADO` | Não | Acesso liberado (mantém plano atual) |
| `VENCIDA` | **Sim** | "Sua assinatura está vencida." |
| `SUSPENSA` | **Sim** | "Sua assinatura está suspensa." |

---

## 5. Fluxo de Troca de Plano (Modal Atualizado)

O fluxo de troca de plano agora tem uma **etapa de preview** antes da confirmação:

### Fluxo anterior:
```
Selecionar plano → Confirmar → Chamada API trocarPlano
```

### Novo fluxo:
```
Selecionar plano → Preview pro-rata → Confirmar → Chamada API trocarPlano
```

### 5.1 Tela de Preview Pro-Rata

Após o usuário selecionar um novo plano, chamar `POST /preview-troca-plano` e exibir um modal/step com:

```
┌─────────────────────────────────────────────┐
│         Resumo da Troca de Plano            │
├─────────────────────────────────────────────┤
│ Plano atual:  Profissional (Mensal)         │
│ Novo plano:   Empresarial (Mensal)          │
│                                             │
│ Dias restantes no ciclo: 18 de 30           │
│                                             │
│ Crédito proporcional:    R$ 36,00           │
│ Valor proporcional novo: R$ 60,00           │
│ ─────────────────────────────────────────── │
│ Valor pro-rata a pagar:  R$ 24,00           │
│                                             │
│ ℹ️ Uma cobrança avulsa de R$ 24,00 será     │
│    gerada. Após confirmação do pagamento,   │
│    seu plano será atualizado.               │
│                                             │
│        [Cancelar]    [Confirmar Troca]       │
└─────────────────────────────────────────────┘
```

**Para downgrade** (quando `isUpgrade = false`):
```
┌─────────────────────────────────────────────┐
│         Resumo da Troca de Plano            │
├─────────────────────────────────────────────┤
│ Plano atual:  Empresarial (Mensal)          │
│ Novo plano:   Profissional (Mensal)         │
│                                             │
│ ℹ️ O downgrade será aplicado no próximo     │
│    ciclo de cobrança. Você continuará com   │
│    o plano atual até o vencimento.          │
│                                             │
│ Crédito restante: R$ 36,00                  │
│ (será usado como desconto no novo plano)    │
│                                             │
│        [Cancelar]    [Confirmar Downgrade]   │
└─────────────────────────────────────────────┘
```

---

## 6. Remoção de Referências a `valorMensal` / `valorAnual`

Todos os locais no frontend que exibem `assinatura.valorMensal` ou `assinatura.valorAnual` devem ser atualizados:

- **Remover** qualquer exibição desses campos na tela de assinatura
- **Substituir** por valores vindos do objeto `plano` (o `PlanoResponse` já contém `valorMensal` e `valorAnual`)
- Onde antes se fazia `assinatura.valorMensal`, fazer `assinatura.plano.valorMensal`

### Exemplo de migração:
```diff
- <span>R$ {assinatura.valorMensal?.toFixed(2)}/mês</span>
+ <span>R$ {assinatura.plano.valorMensal.toFixed(2)}/mês</span>
```

---

## 7. Painel Admin — Alterações

### 7.1 Tipo `AssinaturaResponse` (Admin)

Mesmas alterações do tipo app: remover `valorMensal`/`valorAnual`, adicionar `creditoProRata` e `planoAnteriorCodigo`.

### 7.2 Tabela de listagem

- Adicionar coluna ou filtro para os novos status (`AGUARDANDO_PAGAMENTO`, `UPGRADE_PENDENTE`, `DOWNGRADE_AGENDADO`)
- Na coluna de status, renderizar os novos chips conforme seção 3

### 7.3 Tela de detalhes da assinatura

- Exibir `creditoProRata` quando presente (campo informativo)
- Exibir `planoAnteriorCodigo` quando em status `DOWNGRADE_AGENDADO` ou `UPGRADE_PENDENTE`
- Remover exibição de `valorMensal` e `valorAnual` da assinatura (usar valores do plano)

---

## 8. Tratamento do Status `AGUARDANDO_PAGAMENTO` pós Escolha de Plano

Quando o usuário escolhe um plano com `billingType` = `BOLETO` ou `PIX`, o backend agora retorna status `AGUARDANDO_PAGAMENTO` ao invés de `ATIVA`.

### Comportamento esperado no frontend:

1. Após chamar `POST /escolher-plano`, verificar o status retornado
2. Se `AGUARDANDO_PAGAMENTO`:
   - Exibir mensagem: "Pagamento gerado com sucesso! Seu acesso será liberado após a confirmação do pagamento."
   - Redirecionar para tela de assinatura mostrando o status de aguardando
   - **Não** exibir funcionalidades que dependem de assinatura ativa
3. Se `ATIVA` (cartão de crédito aprovado imediatamente):
   - Comportamento normal, acesso liberado

---

## 9. Cupom com Recorrência Limitada

O backend agora controla a expiração de cupons com `mesesRecorrencia`. Nenhuma mudança é necessária no frontend para cupons, pois a lógica é toda server-side. Porém, pode ser útil exibir no admin:

- **Admin**: Na listagem/detalhes de cupons, exibir o campo `mesesRecorrencia`:
  - `null` → "Desconto permanente"
  - `N` → "Desconto por N meses"

---

## 10. Checklist de Implementação

- [ ] Atualizar tipo `StatusAssinatura` com 3 novos valores
- [ ] Atualizar tipo `SituacaoAssinatura` com 3 novos valores
- [ ] Remover `valorMensal` e `valorAnual` de `AssinaturaResponse`
- [ ] Adicionar `creditoProRata` e `planoAnteriorCodigo` a `AssinaturaResponse`
- [ ] Criar tipo `ProRataPreviewDTO`
- [ ] Criar service function `previewTrocaPlano()`
- [ ] Atualizar mapeamento de status chips (cores e labels)
- [ ] Adicionar renderização condicional para `AGUARDANDO_PAGAMENTO`
- [ ] Adicionar renderização condicional para `UPGRADE_PENDENTE`
- [ ] Adicionar renderização condicional para `DOWNGRADE_AGENDADO`
- [ ] Implementar modal de preview pro-rata na troca de plano
- [ ] Tratar retorno `AGUARDANDO_PAGAMENTO` no fluxo de escolher plano
- [ ] Migrar referências de `assinatura.valorMensal` → `assinatura.plano.valorMensal`
- [ ] Admin: atualizar tipo e tabela de assinaturas
- [ ] Admin: exibir `mesesRecorrencia` em cupons (opcional)
