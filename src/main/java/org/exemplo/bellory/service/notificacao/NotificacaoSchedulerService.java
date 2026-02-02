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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
    private final RestTemplate restTemplate;
    private final Random random = new Random();

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
            List<NotificacaoPendenteDTO> confirmacoes = buscarConfirmacoesPendentes(agora);

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

    public List<NotificacaoPendenteDTO> buscarConfirmacoesPendentes(LocalDateTime agora) {
        return agendamentoRepository.findConfirmacoesPendentes(agora).stream()
                .map(row -> new NotificacaoPendenteDTO(
                        ((Number) row[0]).longValue(),                    // agendamentoId
                        ((Number) row[1]).longValue(),                    // organizacaoId
                        (String) row[2],                                  // nomeOrganizacao
                        (String) row[3],                                  // nomeCliente
                        (String) row[4],                                  // telefoneCliente
                        (String) row[5],                                  // nomeServico
                        (String) row[6],                                  // nomeFuncionario
                        ((Timestamp) row[7]).toLocalDateTime(),            // dataAgendamento
                        (BigDecimal) row[8],                              // valor
                        (String) row[9],                                  // endereco
                        TipoNotificacao.valueOf((String) row[10]),        // tipo
                        ((Number) row[11]).intValue(),                    // horasAntes
                        (String) row[12],                                 // instanceName
                        (String) row[13]                                  // mensagemTemplate
                ))
                .toList();
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
//            List<NotificacaoPendenteDTO> lembretes = agendamentoRepository
//                .findLembretesPendentes(agora, inicioJanela, fimJanela);

//            log.info("Encontrados {} lembretes pendentes", lembretes.size());

            // Verifica instancias desconectadas para LEMBRETE
            List<NotificacaoSemInstanciaDTO> semInstancia = agendamentoRepository
                .findNotificacoesSemInstanciaConectadaPorTipo(agora, TipoNotificacao.LEMBRETE);

            if (!semInstancia.isEmpty()) {
                log.warn("{} lembretes sem instancia conectada!", semInstancia.size());
                alertService.alertarInstanciasDesconectadas(semInstancia);
            }

//            if (!lembretes.isEmpty()) {
                //processarNotificacoes(lembretes, TipoNotificacao.LEMBRETE);
//            }

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
            String telefone = formatarTelefone("5532998220082"); //formatarTelefone(notif.getTelefoneCliente());

            if (telefone == null || telefone.isBlank()) {
                log.warn("Telefone invalido para agendamento {} (tipo={}, horasAntes={})",
                    notif.getAgendamentoId(), notif.getTipo(), notif.getHorasAntes());
                registrarEnvio(notif, StatusEnvio.FALHA, "Telefone invalido", null);
                return;
            }

//            n8nClient.enviarNotificacao(notif.getInstanceName(), telefone, mensagem,
//                    notif.getAgendamentoId(), notif.getTipo().name());


            enviarMensagemWhatsApp(notif.getInstanceName(), telefone, mensagem);

            registrarEnvio(notif, StatusEnvio.ENVIADO, null, telefone);

            log.info("Notificacao {} enviada: horasAntes={}, ag={}, org={}",
                    notif.getTipo(), notif.getHorasAntes(), notif.getAgendamentoId(),
                    notif.getNomeOrganizacao());

            // Delay de 3 minutos fixos + random de até 60 segundos
            long delayFixo = 3 * 60 * 1000L;
            long delayRandom = random.nextInt(60) * 1000L;
            long delayTotal = delayFixo + delayRandom;

            log.debug("Aguardando {}ms antes da proxima mensagem", delayTotal);
            Thread.sleep(delayTotal);


            log.info("Notificacao {} enviada: horasAntes={}, ag={}, org={}",
                notif.getTipo(), notif.getHorasAntes(), notif.getAgendamentoId(),
                notif.getNomeOrganizacao());

            Thread.sleep(1000); // Rate limit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Falha ao enviar {} (horasAntes={}) ag={}: {}",
                notif.getTipo(), notif.getHorasAntes(), notif.getAgendamentoId(), e.getMessage());
            registrarEnvio(notif, StatusEnvio.FALHA, e.getMessage(), notif.getTelefoneCliente());
        }
    }

    private void enviarMensagemWhatsApp(String instanceName, String telefone, String mensagem) {
        String url = "https://wa.bellory.com.br/message/sendText/" + instanceName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", "0626f19f09bd356cc21037164c7c3ca51752fef8");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "number", telefone,
                "text", mensagem
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Falha ao enviar mensagem. Status: " + response.getStatusCode());
        }

        log.debug("Mensagem enviada com sucesso para {} via instancia {}", telefone, instanceName);
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
        Agendamento agendamento = agendamentoRepository.findById(notif.getAgendamentoId()).orElse(null);


        NotificacaoEnviada registro = NotificacaoEnviada.builder()
            .agendamento(agendamento)
            .tipo(notif.getTipo())
            .horasAntes(notif.getHorasAntes())
            .dtEnvio(LocalDateTime.now())
            .status(status)
            .erroMensagem(erroMsg)
            .telefoneDestino(telefone)
            .instanceName(notif.getInstanceName())
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
            .replace("{{nome_cliente}}", notif.getNomeCliente() != null ? notif.getNomeCliente() : "Cliente")
            .replace("{{data_agendamento}}", notif.getDataAgendamento().format(DATE_FMT))
            .replace("{{hora_agendamento}}", notif.getDataAgendamento().format(TIME_FMT))
            .replace("{{servico}}", notif.getNomeServico()!= null ? notif.getNomeServico() : "Servico")
            .replace("{{profissional}}", notif.getNomeFuncionario()!= null ? notif.getNomeFuncionario() : "Funcionario")
            .replace("{{local}}", notif.getEndereco() != null ? notif.getEndereco() : "Endereco")
            .replace("{{valor}}", notif.getValor() != null
                        ? String.format("R$ %,.2f", notif.getValor())
                        : "R$ 0,00")
            .replace("{{nome_empresa}}", notif.getNomeOrganizacao() != null ? notif.getNomeOrganizacao() : "Organizacao");
    }

    /**
     * Retorna o template padrao para cada tipo de notificacao.
     */
    private String getTemplatePadrao(TipoNotificacao tipo) {
        return switch (tipo) {
            case CONFIRMACAO -> """
                Ola, {{nome_cliente}}!

                Voce tem um agendamento na *{{nome_empresa}}*:
                Data: {{data_agendamento}}
                Horario: {{hora_agendamento}}

                Por favor, confirme respondendo:
                *SIM* para confirmar
                *NAO* para cancelar""";
            case LEMBRETE -> """
                Ola, {{nome_cliente}}!

                Lembrete: seu horario na *{{nome_empresa}}* esta chegando!
                Data: {{data_agendamento}}
                Horario: {{hora_agendamento}}

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
