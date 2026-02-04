# Módulo Financeiro - Instruções para Construção do Front-end

## Visão Geral

O módulo financeiro do Bellory contempla gestão completa de finanças do negócio com as seguintes funcionalidades:

- **Dashboard Financeiro** - Visão geral com indicadores, gráficos e alertas
- **Contas a Pagar** - Gestão de despesas e obrigações financeiras
- **Contas a Receber** - Gestão de receitas e recebíveis
- **Lançamentos Financeiros** - Livro caixa com todas as movimentações
- **Contas Bancárias** - Gestão de contas e saldos
- **Categorias Financeiras** - Organização hierárquica de receitas e despesas
- **Centros de Custo** - Alocação de custos por setor/área
- **Relatórios** - Fluxo de Caixa, DRE e Balanço Financeiro

---

## Base URL da API

Todos os endpoints do módulo financeiro estão sob:
```
/api/financeiro
```

---

## 1. Dashboard Financeiro

### Endpoint
```
GET /api/financeiro/dashboard
```

### Componentes sugeridos
- **Cards resumo**: Receitas hoje, Despesas hoje, Saldo hoje, Saldo mês
- **Card saldo total**: Soma de todas as contas bancárias
- **Alertas**: Contas a pagar vencidas (vermelho), contas a vencer em 7 dias (amarelo)
- **Gráfico de barras/linha**: Evolução mensal receitas vs despesas (últimos 12 meses)
- **Gráfico de pizza**: Top categorias de despesas e receitas do mês

### Estrutura do response `DashboardFinanceiroDTO`:
```json
{
  "receitasHoje": 1500.00,
  "despesasHoje": 300.00,
  "saldoHoje": 1200.00,
  "receitasMes": 45000.00,
  "despesasMes": 28000.00,
  "saldoMes": 17000.00,
  "saldoTotalContas": 125000.00,
  "contasPagarVencidas": 3,
  "valorContasPagarVencidas": 4500.00,
  "contasReceberVencidas": 1,
  "valorContasReceberVencidas": 2000.00,
  "contasPagarAVencer": 5,
  "valorContasPagarAVencer": 12000.00,
  "contasReceberAVencer": 8,
  "valorContasReceberAVencer": 18000.00,
  "evolucao": [
    { "mesAno": "JANUARY/2026", "receitas": 40000, "despesas": 25000, "resultado": 15000 }
  ],
  "topCategoriasDespesas": [
    { "categoriaId": 1, "categoriaNome": "Aluguel", "valor": 5000, "percentual": 17.85 }
  ],
  "topCategoriasReceitas": [
    { "categoriaId": 2, "categoriaNome": "Serviços", "valor": 30000, "percentual": 66.67 }
  ]
}
```

---

## 2. Contas a Pagar

### Endpoints
| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/financeiro/contas-pagar` | Criar conta a pagar |
| PUT | `/api/financeiro/contas-pagar/{id}` | Atualizar conta a pagar |
| GET | `/api/financeiro/contas-pagar` | Listar (filtros: dataInicio, dataFim, status) |
| GET | `/api/financeiro/contas-pagar/{id}` | Buscar por ID |
| POST | `/api/financeiro/contas-pagar/{id}/pagar` | Registrar pagamento |
| GET | `/api/financeiro/contas-pagar/vencidas` | Listar vencidas |
| GET | `/api/financeiro/contas-pagar/a-vencer?dias=7` | Listar a vencer |
| DELETE | `/api/financeiro/contas-pagar/{id}` | Cancelar |

### Telas sugeridas
1. **Listagem** com filtros por período, status, categoria, fornecedor
   - Tabela com: descrição, fornecedor, valor, vencimento, status, dias atraso
   - Status com badges coloridos: PENDENTE (amarelo), PAGA (verde), VENCIDA (vermelho), CANCELADA (cinza)
   - Ações: Ver, Editar, Pagar, Cancelar
2. **Formulário de criação/edição**
   - Campos: descrição, fornecedor, valor, vencimento, competência, categoria, centro custo, conta bancária
   - Toggle: recorrente + seleção de periodicidade
   - Campo: número de parcelas (gera parcelamento automático)
3. **Modal de pagamento** (`PagamentoContaDTO`)
   - Campos: valor, data pagamento, conta bancária, forma pagamento, juros, multa, desconto

### Status possíveis: `PENDENTE`, `PAGA`, `VENCIDA`, `CANCELADA`, `PARCIALMENTE_PAGA`

### Payload de criação (`ContaPagarCreateDTO`):
```json
{
  "categoriaFinanceiraId": 1,
  "centroCustoId": 1,
  "contaBancariaId": 1,
  "descricao": "Aluguel Janeiro",
  "fornecedor": "Imobiliária XYZ",
  "documento": "NF-001",
  "valor": 3500.00,
  "dtVencimento": "2026-01-15",
  "dtCompetencia": "2026-01-01",
  "formaPagamento": "BOLETO",
  "recorrente": true,
  "periodicidade": "MENSAL",
  "totalParcelas": 12,
  "observacoes": "Contrato anual"
}
```

### Payload de pagamento (`PagamentoContaDTO`):
```json
{
  "valor": 3500.00,
  "dataPagamento": "2026-01-15",
  "contaBancariaId": 1,
  "formaPagamento": "BOLETO",
  "valorDesconto": 0,
  "valorJuros": 0,
  "valorMulta": 0
}
```

---

## 3. Contas a Receber

### Endpoints
| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/financeiro/contas-receber` | Criar conta a receber |
| PUT | `/api/financeiro/contas-receber/{id}` | Atualizar |
| GET | `/api/financeiro/contas-receber` | Listar (filtros: dataInicio, dataFim, status, clienteId) |
| GET | `/api/financeiro/contas-receber/{id}` | Buscar por ID |
| POST | `/api/financeiro/contas-receber/{id}/receber` | Registrar recebimento |
| GET | `/api/financeiro/contas-receber/vencidas` | Listar vencidas |
| GET | `/api/financeiro/contas-receber/a-vencer?dias=7` | Listar a vencer |
| GET | `/api/financeiro/contas-receber/cliente/{clienteId}/pendentes` | Pendentes por cliente |
| DELETE | `/api/financeiro/contas-receber/{id}` | Cancelar |

### Telas sugeridas (similar a contas a pagar, mas para receitas)
1. **Listagem** com filtros por período, status, cliente, categoria
   - Tabela com: descrição, cliente, valor, vencimento, status, dias atraso
2. **Formulário de criação/edição** com vinculação a cliente
3. **Modal de recebimento** (mesmo DTO de PagamentoContaDTO)

### Status possíveis: `PENDENTE`, `RECEBIDA`, `VENCIDA`, `CANCELADA`, `PARCIALMENTE_RECEBIDA`

---

## 4. Lançamentos Financeiros

### Endpoints
| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/financeiro/lancamentos` | Criar lançamento |
| PUT | `/api/financeiro/lancamentos/{id}` | Atualizar |
| GET | `/api/financeiro/lancamentos` | Listar (filtros: dataInicio, dataFim, tipo, categoriaFinanceiraId, centroCustoId, contaBancariaId) |
| GET | `/api/financeiro/lancamentos/{id}` | Buscar por ID |
| POST | `/api/financeiro/lancamentos/{id}/efetivar` | Efetivar lançamento pendente |
| DELETE | `/api/financeiro/lancamentos/{id}` | Cancelar |

### Telas sugeridas
1. **Listagem** (extrato/livro caixa)
   - Tabela com: data, tipo (ícone/cor), descrição, categoria, valor, conta bancária, status
   - RECEITA = verde/seta para cima, DESPESA = vermelho/seta para baixo, TRANSFERÊNCIA = azul/setas
   - Filtros por período, tipo, categoria, centro de custo, conta bancária
2. **Formulário de criação**
   - Seleção de tipo: Receita, Despesa, Transferência
   - Transferência mostra: conta origem + conta destino
   - Campos: valor, data lançamento, competência, categoria, centro custo, forma pagamento

### Tipos: `RECEITA`, `DESPESA`, `TRANSFERENCIA`
### Status: `EFETIVADO`, `PENDENTE`, `CANCELADO`

### Payload de criação (`LancamentoFinanceiroCreateDTO`):
```json
{
  "tipo": "DESPESA",
  "descricao": "Compra de materiais",
  "valor": 450.00,
  "dtLancamento": "2026-01-10",
  "dtCompetencia": "2026-01-01",
  "categoriaFinanceiraId": 3,
  "centroCustoId": 1,
  "contaBancariaId": 1,
  "formaPagamento": "PIX",
  "status": "EFETIVADO"
}
```

### Payload de transferência:
```json
{
  "tipo": "TRANSFERENCIA",
  "descricao": "Transferência entre contas",
  "valor": 5000.00,
  "dtLancamento": "2026-01-10",
  "contaBancariaId": 1,
  "contaBancariaDestinoId": 2,
  "status": "EFETIVADO"
}
```

---

## 5. Contas Bancárias

### Endpoints
| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/financeiro/contas-bancarias` | Criar conta |
| PUT | `/api/financeiro/contas-bancarias/{id}` | Atualizar |
| GET | `/api/financeiro/contas-bancarias` | Listar ativas |
| GET | `/api/financeiro/contas-bancarias/{id}` | Buscar por ID |
| GET | `/api/financeiro/contas-bancarias/saldo-total` | Saldo total |
| PATCH | `/api/financeiro/contas-bancarias/{id}/desativar` | Desativar |

### Telas sugeridas
1. **Listagem** em cards (visual tipo app de banco)
   - Card por conta: nome, banco, tipo, saldo atual (destaque), ícone/cor
   - Card principal com destaque visual
   - Total geral no topo
2. **Formulário de criação/edição**
   - Campos: nome, tipo conta, banco, agência, número, saldo inicial, cor, ícone
   - Toggle: conta principal

### Tipos de conta: `CONTA_CORRENTE`, `POUPANCA`, `CAIXA`, `CARTEIRA_DIGITAL`

---

## 6. Categorias Financeiras

### Endpoints
| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/financeiro/categorias` | Criar categoria |
| PUT | `/api/financeiro/categorias/{id}` | Atualizar |
| GET | `/api/financeiro/categorias?tipo=RECEITA` | Listar (filtro tipo) |
| GET | `/api/financeiro/categorias/arvore?tipo=DESPESA` | Listar em árvore |
| GET | `/api/financeiro/categorias/{id}` | Buscar por ID |
| PATCH | `/api/financeiro/categorias/{id}/desativar` | Desativar |
| PATCH | `/api/financeiro/categorias/{id}/ativar` | Ativar |

### Telas sugeridas
1. **Listagem em árvore** (TreeView)
   - Separação por abas: Receitas / Despesas
   - Cada item com: cor (bolinha), ícone, nome, ações (editar, subcategorias, desativar)
   - Botão "Nova Categoria" e "Nova Subcategoria"
2. **Modal/Formulário de criação**
   - Campos: nome, tipo (RECEITA/DESPESA), descrição, cor (color picker), ícone, categoria pai

### Tipos: `RECEITA`, `DESPESA`

---

## 7. Centros de Custo

### Endpoints
| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/financeiro/centros-custo` | Criar |
| PUT | `/api/financeiro/centros-custo/{id}` | Atualizar |
| GET | `/api/financeiro/centros-custo` | Listar |
| GET | `/api/financeiro/centros-custo/{id}` | Buscar por ID |
| PATCH | `/api/financeiro/centros-custo/{id}/desativar` | Desativar |
| PATCH | `/api/financeiro/centros-custo/{id}/ativar` | Ativar |

### Telas sugeridas
1. **Listagem** simples em tabela/cards
2. **Modal de criação/edição**: nome, código, descrição

---

## 8. Relatórios

### Endpoints
| Método | URL | Parâmetros | Descrição |
|--------|-----|------------|-----------|
| GET | `/api/financeiro/relatorios/fluxo-caixa` | `dataInicio`, `dataFim` | Fluxo de Caixa |
| GET | `/api/financeiro/relatorios/dre` | `dataInicio`, `dataFim` | DRE |
| GET | `/api/financeiro/relatorios/balanco` | `dataReferencia` (opcional) | Balanço Financeiro |

### 8.1 Fluxo de Caixa
- **Seletor de período** (data início / data fim)
- **Cards resumo**: Saldo inicial, Total receitas, Total despesas, Saldo final
- **Gráfico de linha**: Saldo acumulado diário
- **Gráfico de barras**: Receitas vs Despesas por dia
- **Tabela detalhada**: Dia a dia com receitas, despesas, saldo, acumulado
- **Pizza**: Receitas por categoria / Despesas por categoria

### 8.2 DRE (Demonstrativo de Resultados)
- **Seletor de período** (mês/trimestre/ano)
- **Estrutura hierárquica**:
  ```
  (+) Receita Bruta
  (-) Descontos
  (=) Receita Líquida
  (-) Despesas Operacionais
    - Categoria 1: R$ X
    - Categoria 2: R$ X
  (=) Resultado Operacional
  (=) Resultado Líquido
  ```
- **Indicadores**: Margem Operacional (%), Margem Líquida (%)
- **Gráfico de barras horizontal**: Receitas e despesas por categoria

### 8.3 Balanço Financeiro
- **Cards**: Total ativos, Contas a receber, Contas a pagar, Saldo líquido
- **Tabela de contas bancárias** com saldo atual
- **Resumo de contas a receber**: pendentes, vencidas, recebidas (qtd + valor)
- **Resumo de contas a pagar**: pendentes, vencidas, pagas (qtd + valor)
- **Indicadores**: Índice de liquidez, Ticket médio receitas/despesas, Receita média diária
- **Gráfico**: Evolução mensal (últimos 6 meses)

---

## Padrões de Response da API

Todas as respostas seguem o padrão `ResponseAPI<T>`:
```json
{
  "success": true,
  "message": "Mensagem de sucesso ou erro",
  "dados": { /* objeto ou lista */ },
  "errorCode": null,
  "errors": null
}
```

### Códigos HTTP utilizados
- `200` - Sucesso
- `201` - Criado com sucesso
- `400` - Erro de validação / dados inválidos
- `404` - Recurso não encontrado
- `409` - Conflito de estado (ex: pagar conta já paga)
- `500` - Erro interno

---

## Estrutura de Navegação Sugerida

```
Financeiro (menu lateral)
├── Dashboard
├── Contas a Pagar
│   ├── Listagem (com filtros)
│   └── Nova Conta a Pagar
├── Contas a Receber
│   ├── Listagem (com filtros)
│   └── Nova Conta a Receber
├── Lançamentos
│   ├── Extrato/Livro Caixa
│   └── Novo Lançamento
├── Contas Bancárias
├── Relatórios
│   ├── Fluxo de Caixa
│   ├── DRE
│   └── Balanço Financeiro
└── Configurações
    ├── Categorias Financeiras
    └── Centros de Custo
```

---

## Enums Reutilizáveis no Front-end

### Formas de Pagamento
```typescript
const FORMAS_PAGAMENTO = [
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de Crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de Débito' },
  { value: 'PIX', label: 'PIX' },
  { value: 'TRANSFERENCIA', label: 'Transferência' },
  { value: 'BOLETO', label: 'Boleto' },
  { value: 'CHEQUE', label: 'Cheque' }
];
```

### Periodicidade (Recorrência)
```typescript
const PERIODICIDADES = [
  { value: 'SEMANAL', label: 'Semanal' },
  { value: 'QUINZENAL', label: 'Quinzenal' },
  { value: 'MENSAL', label: 'Mensal' },
  { value: 'BIMESTRAL', label: 'Bimestral' },
  { value: 'TRIMESTRAL', label: 'Trimestral' },
  { value: 'SEMESTRAL', label: 'Semestral' },
  { value: 'ANUAL', label: 'Anual' }
];
```

### Tipos de Conta Bancária
```typescript
const TIPOS_CONTA = [
  { value: 'CONTA_CORRENTE', label: 'Conta Corrente' },
  { value: 'POUPANCA', label: 'Poupança' },
  { value: 'CAIXA', label: 'Caixa' },
  { value: 'CARTEIRA_DIGITAL', label: 'Carteira Digital' }
];
```

---

## Permissões (Roles)

O módulo financeiro deve ser acessível pelas roles:
- `ROLE_ADMIN` - Acesso total
- `ROLE_FINANCEIRO` - Acesso total ao módulo financeiro
- `ROLE_GERENTE` - Visualização de relatórios e dashboard

Configurar guards/middlewares no front-end para verificar a role do usuário autenticado.

---

## Observações de Implementação

1. **Multi-tenant**: Todas as APIs já filtram por organização automaticamente via `TenantContext`. O front-end não precisa enviar organizacaoId.

2. **Datas**: Usar formato `YYYY-MM-DD` para datas nos query params e body.

3. **Valores monetários**: A API retorna e espera `BigDecimal` (number no JSON). Formatar no front como moeda brasileira (R$ X.XXX,XX).

4. **Lançamentos automáticos**: Ao pagar uma conta a pagar ou receber uma conta a receber, o sistema cria automaticamente um lançamento financeiro e atualiza o saldo da conta bancária. Não é necessário criar lançamentos manualmente para esses casos.

5. **Parcelamento**: Ao criar uma conta a pagar/receber com `totalParcelas > 1`, o sistema gera automaticamente N parcelas com vencimentos mensais.

6. **Categorias hierárquicas**: Use o endpoint `/arvore` para obter a estrutura completa em árvore. O endpoint padrão retorna lista plana.
