✅ Análise de Viabilidade — alta                                                                                  ─

🎁 O que já existe e está pronto pra usar

1. ConfigCliente já tem campos de fidelidade configurados! (V5 migration):
   - programaFidelidade (Boolean) — feature flag por organização
   - valorGastoUmPonto (BigDecimal) — taxa de conversão R$ → pontos

Isso significa que a configuração por organização já está modelada — só não tem regra de negócio ligada.
2. PagamentoRecebidoEvent já é publicado depois do AFTER_COMMIT, com valor, clienteId, organizacaoId,
   formaPagamento — gancho perfeito pra creditar pontos.
3. Outros eventos disponíveis pra trigger:
   - ClienteCadastradoEvent → bônus de boas-vindas
   - AgendamentoConfirmadoEvent / Status.CONCLUIDO → bônus por serviço
   - dataNascimento em Cliente → bônus de aniversário (job scheduled)
4. Padrão @TransactionalEventListener(phase = AFTER_COMMIT) já em uso em NotificacaoPushEventListener — replicar
   pro fidelidade.
5. Cupons já existem via Payment API externa (AdminCupomController) — pode ser opção de resgate.

  ---
🔒 Como fazer "de forma segura" (você pediu explicitamente)

Pontos de fidelidade são dinheiro virtual — exigem rigor financeiro:

┌──────────────────────────────────────┬───────────────────────────────────────────────────────────────────────┐
│                Risco                 │                               Mitigação                               │
├──────────────────────────────────────┼───────────────────────────────────────────────────────────────────────┤
│ Duplicação de crédito (event         │ idempotency_key único na tabela de transações (ex: PAGAMENTO:{id})    │
│ reentregue)                          │                                                                       │
├──────────────────────────────────────┼───────────────────────────────────────────────────────────────────────┤
│ Race condition no resgate (cliente   │ Lock pessimista ou versão otimista (@Version) no saldo                │
│ clica 2x)                            │                                                                       │
├──────────────────────────────────────┼───────────────────────────────────────────────────────────────────────┤
│ Saldo negativo                       │ Constraint de banco: CHECK saldo >= 0 + validação no service          │
├──────────────────────────────────────┼───────────────────────────────────────────────────────────────────────┤
│ Cliente em múltiplas orgs            │ Saldo escopado por organizacao_id (multi-tenant)                      │
├──────────────────────────────────────┼───────────────────────────────────────────────────────────────────────┤
│ Estorno/cancelamento de pagamento    │ Listener de PagamentoEstornadoEvent reverte os pontos                 │
├──────────────────────────────────────┼───────────────────────────────────────────────────────────────────────┤
│ Manipulação manual indevida          │ Toda alteração via admin gera transação com admin_id registrado       │
├──────────────────────────────────────┼───────────────────────────────────────────────────────────────────────┤
│ Auditoria                            │ Ledger de transações (cada crédito/débito é uma linha imutável) —     │
│                                      │ saldo é sempre SUM(pontos)                                            │
├──────────────────────────────────────┼───────────────────────────────────────────────────────────────────────┤
│ Tenant leak                          │ Filtro por organizacao_id + TenantContext em todos endpoints          │
└──────────────────────────────────────┴───────────────────────────────────────────────────────────────────────┘

  ---
🏗️Modelagem proposta (V35 + V36)

Eu recomendo fortemente um ledger de transações (cada movimento é uma linha) ao invés de só um campo saldo no
Cliente. Por quê? Porque cliente vai disputar saldo ("eu fiz 5 agendamentos, cadê meus pontos?"), e sem histórico
você não tem resposta.

┌─────────────────────────────────────────────────────────────────┐
│  PontoFidelidadeTransacao  (LEDGER — fonte da verdade)          │
├─────────────────────────────────────────────────────────────────┤
│ id, organizacao_id, cliente_id                                  │
│ tipo: CREDITO_PAGAMENTO | CREDITO_BOAS_VINDAS |                 │
│       CREDITO_ANIVERSARIO | CREDITO_BONUS_AGENDAMENTO |         │
│       CREDITO_AJUSTE_ADMIN | DEBITO_RESGATE |                   │
│       DEBITO_ESTORNO | DEBITO_EXPIRACAO | DEBITO_AJUSTE_ADMIN   │
│ pontos (positivo ou negativo)                                   │
│ saldo_apos (snapshot pra auditoria)                             │
│ pagamento_id, agendamento_id, recompensa_id, admin_id (FKs)     │
│ idempotency_key (UNIQUE)  ← evita duplicação                    │
│ dt_criacao, dt_expiracao (nullable)                             │
│ descricao                                                        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  PontoFidelidadeSaldo  (CACHE — opcional, pra performance)      │
├─────────────────────────────────────────────────────────────────┤
│ cliente_id (UNIQUE com organizacao_id)                          │
│ saldo_atual, saldo_total_historico                              │
│ tier (BRONZE | PRATA | OURO) — se houver tiers                  │
│ @Version  ← lock otimista pra evitar race                       │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Recompensa  (CATÁLOGO — admin da org configura)                │
├─────────────────────────────────────────────────────────────────┤
│ id, organizacao_id                                              │
│ nome, descricao, imagem_url                                     │
│ tipo: DESCONTO_PERCENTUAL | DESCONTO_FIXO | SERVICO_GRATIS |    │
│       PRODUTO_GRATIS | CREDITO_LOJA                             │
│ valor (BigDecimal — valor do desconto/crédito)                  │
│ servico_id (FK, nullable)                                       │
│ pontos_necessarios                                              │
│ ativo, estoque (nullable), data_inicio, data_fim                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  ResgateRecompensa  (cupom/voucher após resgate)                │
├─────────────────────────────────────────────────────────────────┤
│ id, organizacao_id, cliente_id, recompensa_id                   │
│ pontos_gastos, codigo_resgate (UNIQUE pra exibir ao cliente)    │
│ status: PENDENTE | UTILIZADO | EXPIRADO | CANCELADO             │
│ agendamento_id (FK, nullable — quando foi consumido)            │
│ dt_criacao, dt_utilizacao, dt_expiracao                         │
└─────────────────────────────────────────────────────────────────┘

Campos novos em ConfigCliente (extensão dos atuais):

programaFidelidade            // já existe ✓
valorGastoUmPonto             // já existe ✓
pontosBoasVindas              // ex: 100 pts ao cadastrar
pontosAniversario             // ex: 50 pts no aniversário
pontosBonusAgendamentoConcluido  // ex: 10 pts por serviço concluído
validadePontosDias            // 0 = não expira; 365 = expira em 1 ano
permitirResgateParcial        // se pode usar pontos como desconto livre

Listeners necessários:

- PontoFidelidadeListener.onPagamentoRecebido → credita pontos
- PontoFidelidadeListener.onPagamentoEstornado → reverte
- PontoFidelidadeListener.onClienteCadastrado → bônus boas-vindas
- PontoFidelidadeListener.onAgendamentoConcluido → bônus serviço
- Job scheduled diário: aniversariantesDoDiaJob + expirarPontosJob

  ---
🤔 Perguntas pra alinhar antes do plano

📐 Modelo de pontuação

1. Como o cliente ganha pontos? Marca todas que se aplicam:
   - Por valor pago (R$ X = 1 ponto) — já configurado, só ativar
   - Bônus fixo por agendamento concluído
   - Bônus de cadastro (boas-vindas)
   - Bônus de aniversário (anual)
   - Indicação de amigo (referral)
2. Os pontos têm validade? Se sim, padrão de quantos dias? (Comum: 12 meses)
3. Tier system (Bronze/Prata/Ouro)? Quer agora ou deixa pra v2?
   - Se sim, baseado em: pontos acumulados? Gasto nos últimos N meses?
   - Tiers diferentes ganham mais pontos? (ex: Ouro = 2x)

🎁 Resgate / Recompensas

4. Quais tipos de recompensa quer suportar? (Defina o catálogo de tipos)
   - Desconto percentual em agendamento (ex: 20% off)
   - Desconto valor fixo (ex: R$ 50 off)
   - Serviço grátis (ex: corte grátis = 500 pts)
   - Produto grátis (ex: shampoo)
   - Crédito acumulável na conta
5. Onde o cliente resgata?
   - No fluxo de agendamento (escolhe recompensa antes de pagar)
   - Tela "Minhas Recompensas" gera código → apresenta no salão
   - Ambos
6. Resgate gera cupom no Payment API externo (o sistema já tem isso) ou gera desconto interno aplicado direto na
   Cobranca?

🛡️Regras críticas

7. Estorno de pagamento → pontos creditados são revertidos automaticamente. Concorda?
8. Cancelamento de agendamento → como tratar?
   - Sinal pago já gerou pontos. Cancelou. Reverte?
   - Bônus de "agendamento concluído" — só conta se status = CONCLUIDO (não PENDENTE/CANCELADO).
9. Cliente rápido (sem cadastro completo): pode acumular pontos? Ou só clientes "completos"?
10. Saldo negativo possível? Não, mas se houver estorno após resgate, o que faz?
    - Bloqueia o estorno?
    - Permite saldo negativo até liquidar?
    - Cobra valor equivalente?

👥 Permissões / Admin

11. Quem configura o programa por organização?
    - Admin da org (Admin extends User) — confirma?
    - Plataforma (UsuarioAdmin) só monitora?
12. Admin pode dar pontos manualmente? (ex: gesto de cortesia, ressarcimento)
    - Se sim, com limite? Aprovação dupla?
13. Admin pode ver/editar saldo do cliente? (visualização sim, mas edição direta NUNCA — sempre via transação
    registrada)

💻 Interface / Integrações

14. Painel cliente: mostrar saldo + extrato + recompensas disponíveis?
15. Painel admin: relatórios (top clientes por pontos, total emitido vs resgatado, ROI do programa)?
16. Notificações automáticas?
    - "Você ganhou X pontos!" após pagamento
    - "Seus pontos vão expirar em 30 dias"
    - "Parabéns pelo aniversário, ganhou Y pontos!"
17. Aplicação automática de desconto: se cliente tem cupom de resgate ativo e faz agendamento, sistema aplica
    sozinho ou ele tem que escolher?

  ---
Me responde e eu monto o plano completo: migrations V35+V36, entidades, services, listeners, endpoints (cliente +
admin), DTOs, e estratégia de testes pra garantir a integridade financeira.
