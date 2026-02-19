package org.exemplo.bellory.service.push;

import org.exemplo.bellory.model.entity.push.CategoriaNotificacao;
import org.exemplo.bellory.model.entity.push.PrioridadeNotificacao;
import org.exemplo.bellory.model.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoPushEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPushEventListener.class);

    private final NotificacaoPushService notificacaoPushService;

    public NotificacaoPushEventListener(NotificacaoPushService notificacaoPushService) {
        this.notificacaoPushService = notificacaoPushService;
    }

    @Async
    @EventListener
    public void onAgendamentoCriado(AgendamentoCriadoEvent event) {
        log.info("Evento: Agendamento criado #{} para cliente {}", event.getAgendamentoId(), event.getNomeCliente());

        String titulo = "Novo Agendamento";
        String descricao = "Novo agendamento criado para " + event.getNomeCliente();

        // Notificar ROLE_ADMIN e ROLE_GERENTE
        for (String role : new String[]{"ROLE_ADMIN", "ROLE_GERENTE", "ROLE_RECEPCAO"}) {
            notificacaoPushService.criarEEnviarParaRole(
                    role, event.getOrganizacaoId(),
                    titulo, descricao, "AGENDAMENTO",
                    CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.MEDIA,
                    null, "/agendamentos/" + event.getAgendamentoId()
            );
        }
    }

    @Async
    @EventListener
    public void onAgendamentoCancelado(AgendamentoCanceladoEvent event) {
        log.info("Evento: Agendamento cancelado #{}", event.getAgendamentoId());

        String titulo = "Agendamento Cancelado";
        String descricao = "Agendamento de " + event.getNomeCliente() + " foi cancelado";

        // Notificar funcionarios responsaveis
        for (Long funcionarioId : event.getFuncionarioIds()) {
            notificacaoPushService.criarEEnviar(
                    funcionarioId, "ROLE_FUNCIONARIO", event.getOrganizacaoId(),
                    titulo, descricao, "AGENDAMENTO",
                    CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.ALTA,
                    null, "/agendamentos/" + event.getAgendamentoId()
            );
        }

        // Notificar ROLE_ADMIN
        notificacaoPushService.criarEEnviarParaRole(
                "ROLE_ADMIN", event.getOrganizacaoId(),
                titulo, descricao, "AGENDAMENTO",
                CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.ALTA,
                null, "/agendamentos/" + event.getAgendamentoId()
        );
    }

    @Async
    @EventListener
    public void onAgendamentoConfirmado(AgendamentoConfirmadoEvent event) {
        log.info("Evento: Agendamento confirmado #{}", event.getAgendamentoId());

        String titulo = "Agendamento Confirmado";
        String descricao = "Agendamento de " + event.getNomeCliente() + " foi confirmado";

        // Notificar funcionarios responsaveis
        for (Long funcionarioId : event.getFuncionarioIds()) {
            notificacaoPushService.criarEEnviar(
                    funcionarioId, "ROLE_FUNCIONARIO", event.getOrganizacaoId(),
                    titulo, descricao, "AGENDAMENTO",
                    CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.MEDIA,
                    null, "/agendamentos/" + event.getAgendamentoId()
            );
        }
    }

    @Async
    @EventListener
    public void onPagamentoRecebido(PagamentoRecebidoEvent event) {
        log.info("Evento: Pagamento recebido #{} valor {}", event.getPagamentoId(), event.getValor());

        String titulo = "Pagamento Recebido";
        String descricao = "Pagamento de R$ " + event.getValor() + " recebido de " + event.getNomeCliente();

        for (String role : new String[]{"ROLE_ADMIN", "ROLE_GERENTE"}) {
            notificacaoPushService.criarEEnviarParaRole(
                    role, event.getOrganizacaoId(),
                    titulo, descricao, "FINANCEIRO",
                    CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.MEDIA,
                    null, "/financeiro"
            );
        }
    }

    @Async
    @EventListener
    public void onClienteCadastrado(ClienteCadastradoEvent event) {
        log.info("Evento: Novo cliente cadastrado #{} - {}", event.getClienteId(), event.getNomeCliente());

        String titulo = "Novo Cliente";
        String descricao = "Novo cliente cadastrado: " + event.getNomeCliente();

        notificacaoPushService.criarEEnviarParaRole(
                "ROLE_ADMIN", event.getOrganizacaoId(),
                titulo, descricao, "CLIENTE",
                CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.BAIXA,
                null, "/clientes/" + event.getClienteId()
        );
    }

    @Async
    @EventListener
    public void onEstoqueBaixo(EstoqueBaixoEvent event) {
        log.info("Evento: Estoque baixo - produto #{} ({} unidades)", event.getProdutoId(), event.getQuantidadeAtual());

        String titulo = "Estoque Baixo";
        String descricao = "Produto \"" + event.getNomeProduto() + "\" com apenas " + event.getQuantidadeAtual() + " unidades";

        for (String role : new String[]{"ROLE_ADMIN", "ROLE_GERENTE"}) {
            notificacaoPushService.criarEEnviarParaRole(
                    role, event.getOrganizacaoId(),
                    titulo, descricao, "ESTOQUE",
                    CategoriaNotificacao.AVISO, PrioridadeNotificacao.ALTA,
                    null, "/produtos/" + event.getProdutoId()
            );
        }
    }
}
