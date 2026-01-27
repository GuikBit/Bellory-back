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

    @Scheduled(fixedRate = 300000) // 5 minutos
    @Transactional
    public void processarNotificacoesPendentes() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioJanela = agora.minusMinutes(10);
        LocalDateTime fimJanela = agora.plusMinutes(2);

        log.info("=== Iniciando processamento de notificacoes ===");
        log.info("Janela: {} ate {}", inicioJanela, fimJanela);

        try {
            List<NotificacaoPendenteDTO> pendentes = agendamentoRepository
                .findNotificacoesPendentes(agora, inicioJanela, fimJanela);
            log.info("Encontradas {} notificacoes pendentes", pendentes.size());

            List<NotificacaoSemInstanciaDTO> semInstancia = agendamentoRepository
                .findNotificacoesSemInstanciaConectada(agora, inicioJanela, fimJanela);

            if (!semInstancia.isEmpty()) {
                log.warn("{} notificacoes sem instancia conectada!", semInstancia.size());
                alertService.alertarInstanciasDesconectadas(semInstancia);
            }

            if (!pendentes.isEmpty()) {
                processarNotificacoes(pendentes);
            }

            log.info("=== Processamento concluido ===");
        } catch (Exception e) {
            log.error("Erro no processamento: {}", e.getMessage(), e);
        }
    }

    private void processarNotificacoes(List<NotificacaoPendenteDTO> pendentes) {
        Map<Long, List<NotificacaoPendenteDTO>> porOrg = pendentes.stream()
            .collect(Collectors.groupingBy(NotificacaoPendenteDTO::getOrganizacaoId));

        porOrg.forEach((orgId, notificacoes) -> {
            log.info("Processando {} notificacoes para org {}", notificacoes.size(), orgId);
            for (NotificacaoPendenteDTO notif : notificacoes) {
                processarNotificacaoIndividual(notif);
            }
        });
    }

    private void processarNotificacaoIndividual(NotificacaoPendenteDTO notif) {
        try {
            if (jaFoiEnviada(notif)) {
                log.debug("Notificacao ja enviada, ignorando: ag={}", notif.getAgendamentoId());
                return;
            }

            String mensagem = montarMensagem(notif);
            String telefone = formatarTelefone(notif.getTelefoneCliente());

            if (telefone == null || telefone.isBlank()) {
                log.warn("Telefone invalido para agendamento {}", notif.getAgendamentoId());
                registrarEnvio(notif, StatusEnvio.FALHA, "Telefone invalido", null);
                return;
            }

            n8nClient.enviarNotificacao(notif.getInstanceName(), telefone, mensagem,
                notif.getAgendamentoId(), notif.getTipo().name());

            registrarEnvio(notif, StatusEnvio.ENVIADO, null, telefone);

            log.info("Notificacao enviada: tipo={}, horasAntes={}, ag={}",
                notif.getTipo(), notif.getHorasAntes(), notif.getAgendamentoId());

            Thread.sleep(1000); // Rate limit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Falha ao enviar ag={}: {}", notif.getAgendamentoId(), e.getMessage());
            registrarEnvio(notif, StatusEnvio.FALHA, e.getMessage(), notif.getTelefoneCliente());
        }
    }

    private boolean jaFoiEnviada(NotificacaoPendenteDTO notif) {
        return notificacaoEnviadaRepository.existsByAgendamentoIdAndTipoAndHorasAntes(
            notif.getAgendamentoId(), notif.getTipo(), notif.getHorasAntes());
    }

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
