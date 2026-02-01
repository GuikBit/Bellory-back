package org.exemplo.bellory.service.notificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO;
import org.exemplo.bellory.model.dto.notificacao.NotificacaoSemInstanciaDTO;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.notificacao.NotificacaoEnviada;
import org.exemplo.bellory.model.entity.notificacao.StatusEnvio;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.notificacao.NotificacaoEnviadaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacaoSchedulerService {

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoEnviadaRepository notificacaoEnviadaRepository;
    private final N8nWebhookClient n8nClient;
    private final NotificacaoAlertService alertService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Janela de tempo padrao para busca de notificacoes (em minutos)
    private static final int JANELA_MINUTOS_ATRAS = 10;
    private static final int JANELA_MINUTOS_FRENTE = 2;

    @Scheduled(fixedRate = 300000) // 5 minutos
    @Transactional
    public void processarNotificacoesPendentes() {
        LocalDateTime agora = LocalDateTime.now();

        log.info("=== Iniciando processamento de notificacoes ===");
        log.info("Horario atual: {}", agora);

        try {
            // Processa CONFIRMACAO (12/24/36/48 horas antes)
            processarConfirmacoes(agora);

            // Processa LEMBRETE (1/2/3/4/5/6 horas antes)
            //processarLembretes(agora);

            log.info("=== Processamento concluido ===");
        } catch (Exception e) {
            log.error("Erro no processamento: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa notificacoes do tipo CONFIRMACAO.
     * CONFIRMACAO: 12/24/36/48 horas antes do agendamento.
     * Somente processa se houver configuracoes ativas do tipo CONFIRMACAO.
     */
    private void processarConfirmacoes(LocalDateTime agora) {

        try {
            // Busca confirmacoes pendentes (apenas configs ativas do tipo CONFIRMACAO)
            List<NotificacaoPendenteDTO> confirmacoes = agendamentoRepository.findConfirmacoesPendentes(agora);

            log.info("Encontradas {} confirmacoes pendentes", confirmacoes.size());

            // Verifica instancias desconectadas para CONFIRMACAO
            //List<NotificacaoSemInstanciaDTO> semInstancia = agendamentoRepository.findNotificacoesSemInstanciaConectadaPorTipo(agora, TipoNotificacao.CONFIRMACAO);

//            if (!semInstancia.isEmpty()) {
//                log.warn("{} confirmacoes sem instancia conectada!", semInstancia.size());
//                alertService.alertarInstanciasDesconectadas(semInstancia);
//            }

            if (!confirmacoes.isEmpty()) {
                processarNotificacoes(confirmacoes, TipoNotificacao.CONFIRMACAO);
            }

            log.info("--- CONFIRMACOES processadas ---");
        } catch (Exception e) {
            log.error("Erro ao processar CONFIRMACOES: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa notificacoes do tipo LEMBRETE.
     * LEMBRETE: 1/2/3/4/5/6 horas antes do agendamento.
     * Somente processa se houver configuracoes ativas do tipo LEMBRETE.
     */
    private void processarLembretes(LocalDateTime agora) {
        LocalDateTime inicioJanela = agora.minusMinutes(JANELA_MINUTOS_ATRAS);
        LocalDateTime fimJanela = agora.plusMinutes(JANELA_MINUTOS_FRENTE);

        log.info("--- Processando LEMBRETES ---");
        log.info("Janela LEMBRETE: {} ate {}", inicioJanela, fimJanela);

        try {
            // Busca lembretes pendentes (apenas configs ativas do tipo LEMBRETE)
            List<NotificacaoPendenteDTO> lembretes = agendamentoRepository
                .findLembretesPendentes(agora, inicioJanela, fimJanela);

            log.info("Encontrados {} lembretes pendentes", lembretes.size());

            // Verifica instancias desconectadas para LEMBRETE
            List<NotificacaoSemInstanciaDTO> semInstancia = agendamentoRepository
                .findNotificacoesSemInstanciaConectadaPorTipo(agora, TipoNotificacao.LEMBRETE);

            if (!semInstancia.isEmpty()) {
                log.warn("{} lembretes sem instancia conectada!", semInstancia.size());
                alertService.alertarInstanciasDesconectadas(semInstancia);
            }

            if (!lembretes.isEmpty()) {
                processarNotificacoes(lembretes, TipoNotificacao.LEMBRETE);
            }

            log.info("--- LEMBRETES processados ---");
        } catch (Exception e) {
            log.error("Erro ao processar LEMBRETES: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa uma lista de notificacoes pendentes.
     * Agrupa por organizacao e processa cada uma individualmente.
     */
    private void processarNotificacoes(List<NotificacaoPendenteDTO> pendentes, TipoNotificacao tipo) {
        Map<Long, List<NotificacaoPendenteDTO>> porOrg = pendentes.stream()
            .collect(Collectors.groupingBy(NotificacaoPendenteDTO::getOrganizacaoId));

        porOrg.forEach((orgId, notificacoes) -> {
            log.info("Processando {} notificacoes {} para org {}",
                notificacoes.size(), tipo, orgId);

            for (NotificacaoPendenteDTO notif : notificacoes) {
                processarNotificacaoIndividual(notif);
            }
        });
    }

    /**
     * Processa uma notificacao individual.
     * Valida, monta mensagem, envia via N8n e registra o resultado.
     */
    private void processarNotificacaoIndividual(NotificacaoPendenteDTO notif) {
        try {
            if (jaFoiEnviada(notif)) {
                log.debug("Notificacao ja enviada, ignorando: tipo={}, horasAntes={}, ag={}",
                    notif.getTipo(), notif.getHorasAntes(), notif.getAgendamentoId());
                return;
            }

            String mensagem = montarMensagem(notif);
            String telefone = formatarTelefone(notif.getTelefoneCliente());

            if (telefone == null || telefone.isBlank()) {
                log.warn("Telefone invalido para agendamento {} (tipo={}, horasAntes={})",
                    notif.getAgendamentoId(), notif.getTipo(), notif.getHorasAntes());
                registrarEnvio(notif, StatusEnvio.FALHA, "Telefone invalido", null);
                return;
            }

            n8nClient.enviarNotificacao(notif.getInstanceName(), telefone, mensagem,
                notif.getAgendamentoId(), notif.getTipo().name());

            registrarEnvio(notif, StatusEnvio.ENVIADO, null, telefone);

            log.info("Notificacao {} enviada: horasAntes={}, ag={}, org={}",
                notif.getTipo(), notif.getHorasAntes(), notif.getAgendamentoId(),
                notif.getOrganizacaoNome());

            Thread.sleep(1000); // Rate limit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Falha ao enviar {} (horasAntes={}) ag={}: {}",
                notif.getTipo(), notif.getHorasAntes(), notif.getAgendamentoId(), e.getMessage());
            registrarEnvio(notif, StatusEnvio.FALHA, e.getMessage(), notif.getTelefoneCliente());
        }
    }

    /**
     * Verifica se uma notificacao ja foi enviada anteriormente.
     * Usa a combinacao (agendamentoId + tipo + horasAntes) como chave unica.
     */
    private boolean jaFoiEnviada(NotificacaoPendenteDTO notif) {
        return notificacaoEnviadaRepository.existsByAgendamentoIdAndTipoAndHorasAntes(
            notif.getAgendamentoId(), notif.getTipo(), notif.getHorasAntes());
    }

    /**
     * Registra o envio de uma notificacao no banco de dados.
     */
    private void registrarEnvio(NotificacaoPendenteDTO notif, StatusEnvio status,
                                String erroMsg, String telefone) {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(notif.getAgendamentoId());

        NotificacaoEnviada registro = NotificacaoEnviada.builder()
            .agendamento(agendamento)
            .tipo(notif.getTipo())
            .horasAntes(notif.getHorasAntes())
            .dtEnvio(LocalDateTime.now())
            .status(status)
            .erroMensagem(erroMsg)
            .telefoneDestino(telefone)
            .build();
        notificacaoEnviadaRepository.save(registro);
    }

    /**
     * Monta a mensagem substituindo os placeholders do template.
     * Se nao houver template customizado, usa o template padrao do tipo.
     */
    private String montarMensagem(NotificacaoPendenteDTO notif) {
        String template = notif.getMensagemTemplate();
        if (template == null || template.isBlank()) {
            template = getTemplatePadrao(notif.getTipo());
        }
        return template
            .replace("{{nomeCliente}}", notif.getNomeCliente() != null ? notif.getNomeCliente() : "Cliente")
            .replace("{{organizacao}}", notif.getOrganizacaoNome() != null ? notif.getOrganizacaoNome() : "")
            .replace("{{data}}", notif.getDtAgendamento().format(DATE_FMT))
            .replace("{{hora}}", notif.getDtAgendamento().format(TIME_FMT));
    }

    /**
     * Retorna o template padrao para cada tipo de notificacao.
     */
    private String getTemplatePadrao(TipoNotificacao tipo) {
        return switch (tipo) {
            case CONFIRMACAO -> """
                Ola, {{nomeCliente}}!

                Voce tem um agendamento na *{{organizacao}}*:
                Data: {{data}}
                Horario: {{hora}}

                Por favor, confirme respondendo:
                *SIM* para confirmar
                *NAO* para cancelar""";
            case LEMBRETE -> """
                Ola, {{nomeCliente}}!

                Lembrete: seu horario na *{{organizacao}}* esta chegando!
                Data: {{data}}
                Horario: {{hora}}

                Te esperamos!""";
        };
    }

    /**
     * Formata o telefone para o padrao internacional brasileiro.
     * Remove caracteres especiais e adiciona prefixo 55 se necessario.
     */
    private String formatarTelefone(String telefone) {
        if (telefone == null) return null;
        String numeros = telefone.replaceAll("[^0-9]", "");
        if (numeros.length() == 11) {
            return "55" + numeros;
        } else if (numeros.length() == 13 && numeros.startsWith("55")) {
            return numeros;
        }
        return numeros;
    }
}
