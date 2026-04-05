package org.exemplo.bellory.service.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.exemplo.bellory.model.entity.push.CategoriaNotificacao;
import org.exemplo.bellory.model.entity.push.PrioridadeNotificacao;
import org.exemplo.bellory.model.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificacaoPushEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPushEventListener.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoPushService notificacaoPushService;

    public NotificacaoPushEventListener(NotificacaoPushService notificacaoPushService) {
        this.notificacaoPushService = notificacaoPushService;
    }

    // ==================== AGENDAMENTO CRIADO ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCriado(AgendamentoCriadoEvent event) {
        log.info("Evento: Agendamento criado #{} para cliente {}", event.getAgendamentoId(), event.getNomeCliente());

        String titulo = "Novo Agendamento";
        String descricao = "Novo agendamento de " + event.getNomeCliente()
                + " para " + event.getDtAgendamento().format(FMT_DATA)
                + " às " + event.getDtAgendamento().format(FMT_HORA);

        String detalhe = "Cliente: " + event.getNomeCliente()
                + "\nData: " + event.getDtAgendamento().format(FMT_DATA)
                + "\nHorário: " + event.getDtAgendamento().format(FMT_HORA)
                + "\nServiços: " + event.getServicos()
                + "\nProfissional: " + event.getProfissional()
                + "\nValor: R$ " + event.getValorTotal();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("agendamentoId", event.getAgendamentoId());
        meta.put("clienteId", event.getClienteId());
        meta.put("nomeCliente", event.getNomeCliente());
        meta.put("dtAgendamento", event.getDtAgendamento().toString());
        meta.put("servicos", event.getServicos());
        meta.put("profissional", event.getProfissional());
        meta.put("valorTotal", event.getValorTotal());

        String metadataJson = toJson(meta);
        String urlAcao = "/agendamentos/" + event.getAgendamentoId();

        for (String role : new String[]{"ROLE_ADMIN", "ROLE_GERENTE", "ROLE_RECEPCAO"}) {
            notificacaoPushService.criarEEnviarParaRole(
                    role, event.getOrganizacaoId(),
                    titulo, descricao, "AGENDAMENTO",
                    CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.MEDIA,
                    null, urlAcao, detalhe, metadataJson
            );
        }
    }

    // ==================== AGENDAMENTO CANCELADO ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCancelado(AgendamentoCanceladoEvent event) {
        log.info("Evento: Agendamento cancelado #{}", event.getAgendamentoId());

        String titulo = "Agendamento Cancelado";
        String descricao = "Agendamento de " + event.getNomeCliente()
                + " em " + event.getDtAgendamento().format(FMT_DATA)
                + " às " + event.getDtAgendamento().format(FMT_HORA) + " foi cancelado";

        String detalhe = "Cliente: " + event.getNomeCliente()
                + "\nData: " + event.getDtAgendamento().format(FMT_DATA)
                + "\nHorário: " + event.getDtAgendamento().format(FMT_HORA)
                + "\nServiços: " + event.getServicos()
                + "\nProfissional: " + event.getProfissional();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("agendamentoId", event.getAgendamentoId());
        meta.put("clienteId", event.getClienteId());
        meta.put("nomeCliente", event.getNomeCliente());
        meta.put("dtAgendamento", event.getDtAgendamento().toString());
        meta.put("servicos", event.getServicos());
        meta.put("profissional", event.getProfissional());

        String metadataJson = toJson(meta);
        String urlAcao = "/agendamentos/" + event.getAgendamentoId();

        // Notificar funcionarios responsaveis
        for (Long funcionarioId : event.getFuncionarioIds()) {
            notificacaoPushService.criarEEnviar(
                    funcionarioId, "ROLE_FUNCIONARIO", event.getOrganizacaoId(),
                    titulo, descricao, "AGENDAMENTO",
                    CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.ALTA,
                    null, urlAcao, detalhe, metadataJson
            );
        }

        // Notificar ROLE_ADMIN
        notificacaoPushService.criarEEnviarParaRole(
                "ROLE_ADMIN", event.getOrganizacaoId(),
                titulo, descricao, "AGENDAMENTO",
                CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.ALTA,
                null, urlAcao, detalhe, metadataJson
        );
    }

    // ==================== AGENDAMENTO CONFIRMADO ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoConfirmado(AgendamentoConfirmadoEvent event) {
        log.info("Evento: Agendamento confirmado #{}", event.getAgendamentoId());

        String titulo = "Agendamento Confirmado";
        String descricao = "Agendamento de " + event.getNomeCliente()
                + " em " + event.getDtAgendamento().format(FMT_DATA)
                + " às " + event.getDtAgendamento().format(FMT_HORA) + " foi confirmado";

        String detalhe = "Cliente: " + event.getNomeCliente()
                + "\nData: " + event.getDtAgendamento().format(FMT_DATA)
                + "\nHorário: " + event.getDtAgendamento().format(FMT_HORA)
                + "\nServiços: " + event.getServicos()
                + "\nProfissional: " + event.getProfissional();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("agendamentoId", event.getAgendamentoId());
        meta.put("clienteId", event.getClienteId());
        meta.put("nomeCliente", event.getNomeCliente());
        meta.put("dtAgendamento", event.getDtAgendamento().toString());
        meta.put("servicos", event.getServicos());
        meta.put("profissional", event.getProfissional());

        String metadataJson = toJson(meta);
        String urlAcao = "/agendamentos/" + event.getAgendamentoId();

        for (Long funcionarioId : event.getFuncionarioIds()) {
            notificacaoPushService.criarEEnviar(
                    funcionarioId, "ROLE_FUNCIONARIO", event.getOrganizacaoId(),
                    titulo, descricao, "AGENDAMENTO",
                    CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.MEDIA,
                    null, urlAcao, detalhe, metadataJson
            );
        }
    }

    // ==================== PAGAMENTO RECEBIDO ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPagamentoRecebido(PagamentoRecebidoEvent event) {
        log.info("Evento: Pagamento recebido #{} valor {}", event.getPagamentoId(), event.getValor());

        String forma = event.getFormaPagamento() != null ? event.getFormaPagamento() : "";
        String titulo = "Pagamento Recebido";
        String descricao = "Pagamento de R$ " + event.getValor() + " recebido de " + event.getNomeCliente();

        String detalhe = "Cliente: " + event.getNomeCliente()
                + "\nValor: R$ " + event.getValor()
                + (forma.isEmpty() ? "" : "\nForma: " + forma);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagamentoId", event.getPagamentoId());
        meta.put("clienteId", event.getClienteId());
        meta.put("nomeCliente", event.getNomeCliente());
        meta.put("valor", event.getValor());
        if (!forma.isEmpty()) meta.put("formaPagamento", forma);

        String metadataJson = toJson(meta);

        for (String role : new String[]{"ROLE_ADMIN", "ROLE_GERENTE"}) {
            notificacaoPushService.criarEEnviarParaRole(
                    role, event.getOrganizacaoId(),
                    titulo, descricao, "FINANCEIRO",
                    CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.MEDIA,
                    null, "/financeiro", detalhe, metadataJson
            );
        }
    }

    // ==================== CLIENTE CADASTRADO ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClienteCadastrado(ClienteCadastradoEvent event) {
        log.info("Evento: Novo cliente cadastrado #{} - {}", event.getClienteId(), event.getNomeCliente());

        String titulo = "Novo Cliente";
        String descricao = "Novo cliente cadastrado: " + event.getNomeCliente();

        StringBuilder sb = new StringBuilder();
        sb.append("Nome: ").append(event.getNomeCliente());
        if (event.getTelefone() != null && !event.getTelefone().isBlank()) {
            sb.append("\nTelefone: ").append(event.getTelefone());
        }
        if (event.getEmail() != null && !event.getEmail().isBlank()) {
            sb.append("\nEmail: ").append(event.getEmail());
        }
        String detalhe = sb.toString();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("clienteId", event.getClienteId());
        meta.put("nomeCliente", event.getNomeCliente());
        if (event.getTelefone() != null && !event.getTelefone().isBlank()) {
            meta.put("telefone", event.getTelefone());
        }
        if (event.getEmail() != null && !event.getEmail().isBlank()) {
            meta.put("email", event.getEmail());
        }

        String metadataJson = toJson(meta);

        notificacaoPushService.criarEEnviarParaRole(
                "ROLE_ADMIN", event.getOrganizacaoId(),
                titulo, descricao, "CLIENTE",
                CategoriaNotificacao.SISTEMA, PrioridadeNotificacao.BAIXA,
                null, "/clientes/" + event.getClienteId(), detalhe, metadataJson
        );
    }

    // ==================== ESTOQUE BAIXO ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEstoqueBaixo(EstoqueBaixoEvent event) {
        log.info("Evento: Estoque baixo - produto #{} ({} unidades)", event.getProdutoId(), event.getQuantidadeAtual());

        String titulo = "Estoque Baixo";
        String descricao = "Produto \"" + event.getNomeProduto() + "\" com apenas " + event.getQuantidadeAtual() + " unidades";

        String detalhe = "Produto: " + event.getNomeProduto()
                + "\nQuantidade atual: " + event.getQuantidadeAtual() + " unidades";

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("produtoId", event.getProdutoId());
        meta.put("nomeProduto", event.getNomeProduto());
        meta.put("quantidadeAtual", event.getQuantidadeAtual());

        String metadataJson = toJson(meta);

        for (String role : new String[]{"ROLE_ADMIN", "ROLE_GERENTE"}) {
            notificacaoPushService.criarEEnviarParaRole(
                    role, event.getOrganizacaoId(),
                    titulo, descricao, "ESTOQUE",
                    CategoriaNotificacao.AVISO, PrioridadeNotificacao.ALTA,
                    null, "/produtos/" + event.getProdutoId(), detalhe, metadataJson
            );
        }
    }

    // ==================== HELPERS ====================

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Erro ao serializar metadata", e);
            return null;
        }
    }
}
