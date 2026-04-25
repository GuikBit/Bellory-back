Análise de Viabilidade──────────────────────────────────────────────────────────────────────────────────────────

A ideia é totalmente viável — e na verdade o sistema já tem várias peças prontas que vão acelerar muito a         
implementação:

O que joga a favor

1. Status EM_ESPERA já existe no enum (Status.java) — pode ser que esteja sem uso ativo, dá pra reaproveitar ou
   criar entidade separada.
2. AgendamentoCanceladoEvent já é publicado em AgendamentoService.cancelAgendamento() — gancho perfeito pra
   disparar a lógica da fila.
3. NotificacaoEnviada já existe com infra de WhatsApp + resposta do cliente (AGUARDANDO_RESPOSTA,
   AGUARDANDO_HORARIO, REAGENDADO, whatsappMessageId, respostaCliente) — exatamente o tipo de fluxo que a fila
   precisa.
4. Cálculo de duração já é dinâmico (soma de tempoEstimadoMinutos dos serviços) — comparação por "tempo parecido"
   fica trivial.
5. getHorariosDisponiveis() em AgendamentoService já valida disponibilidade com tolerância — pode ser reutilizado
   pra confirmar que o horário liberado realmente cabe.
6. Multitenancy + TenantContext já estabelecidos.
7. isCadastroIncompleto + cliente rápido — fila precisa decidir se aceita ou não cliente rápido.

Pontos de atenção (riscos técnicos)

1. Race condition: enquanto o sistema espera resposta do 1º da fila, alguém pode reservar o slot pela tela normal
   de agendamento. Precisamos de lock temporário no horário (um BloqueioAgenda provisório, por exemplo).
2. Match "tempo parecido": precisa critério claro. "Tempo igual" é raro — tolerância (±X min) faz mais sentido.
3. Fluxo sequencial vs. paralelo: você falou sequencial, o que é mais justo, mas exige timeout por cliente. Se o
   1º não responde em 30min, passa pro 2º — caso contrário pode ficar parado por horas.
4. Compatibilidade profissional × serviço: se o cancelado era com a Maria e o cliente da fila quer com a João, não
   é match (a menos que aceite "qualquer profissional").

Modelagem sugerida (pra discutir)

Nova entidade FilaEspera (schema app) — separada de Agendamento, porque a semântica é diferente (não é um
agendamento, é uma intenção):

FilaEspera
├─ id
├─ organizacao_id (multitenancy)
├─ cliente_id
├─ servicos (ManyToMany)
├─ funcionarios_preferidos (ManyToMany, opcional)
├─ data_preferida_inicio / data_preferida_fim  ← janela de interesse
├─ status (ATIVO, NOTIFICADO, ACEITO, RECUSADO, EXPIRADO, CANCELADO)
├─ posicao (ou ordenação por dt_criacao)
├─ dt_criacao, dt_expiracao
└─ tentativas (OneToMany → FilaEsperaTentativa)

FilaEsperaTentativa  ← rastreia cada notificação enviada
├─ fila_espera_id
├─ agendamento_cancelado_id  ← qual cancelamento gerou
├─ dt_envio, dt_resposta
├─ resposta (ACEITOU, RECUSOU, TIMEOUT)
└─ canal (WHATSAPP, PUSH, EMAIL)

E um listener @EventListener AgendamentoCanceladoEvent que dispara o "matchmaker".

  ---
🤔 Perguntas pra alinhar antes do plano

Sobre o match (cancelamento ↔ fila):

1. Critério de "tempo parecido": tolerância em minutos (ex: ±15 min)? Ou a duração da fila precisa ser menor ou
   igual à do cancelamento (cabe no slot)?
2. Profissional: o cliente na fila escolhe profissional específico, "qualquer um" ou ambos os modos?
3. Janela de data: o cliente informa "qualquer data até X" ou "só esta semana" ou "qualquer dia/horário"?
4. Mesmos serviços? Match exige os mesmos serviços ou só compatibilidade de duração (ex: cliente quer 60min de
   corte, slot de 60min de manicure libera — vale?)

Sobre o fluxo de notificação:

5. Canal: WhatsApp (já tem infra), Push, Email — qual prioridade? Tenta todos ou só o preferido do cliente?
6. Timeout por cliente na fila: se o 1º não responde, em quanto tempo passa pro 2º? Sugiro 30min — está OK?
7. Bloqueio do slot: enquanto espera resposta, o horário deve ficar bloqueado pra agendamento normal ou continuar
   aberto (quem chegar primeiro leva)?

Sobre regras de negócio:

8. Cliente rápido pode entrar na fila? (Tem telefone mas talvez sem email/cadastro completo)
9. Expiração: depois de quanto tempo na fila o cadastro expira automaticamente? (Ex: 30 dias)
10. Múltiplas filas: o mesmo cliente pode estar em várias filas simultaneamente (ex: corte com Maria + manicure
    com Ana)?
11. Aceite vira agendamento automaticamente? Ou abre um link pra confirmar dados/pagar sinal antes?
12. Sinal/pagamento: como requerSinal é true por padrão, o aceite na fila passa pelo mesmo fluxo de cobrança de
    sinal?

Sobre interface:

13. Onde fica o checkbox "entrar na fila"? Aparece sempre, ou só quando o horário desejado não tem
    disponibilidade?
14. Admin/Funcionário enxerga a fila? Painel de gestão da fila por organização?
15. Cliente pode sair da fila? Onde? (Endpoint na "minhas filas")

Me responde essas e eu monto o plano completo de implementação (entidades + migration V34 + service + listener +
endpoints + integração com WhatsApp).
