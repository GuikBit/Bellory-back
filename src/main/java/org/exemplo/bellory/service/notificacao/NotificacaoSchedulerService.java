package org.exemplo.bellory.service.notificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.notificacao.NotificacaoEnviada;
import org.exemplo.bellory.model.entity.notificacao.StatusEnvio;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.notificacao.NotificacaoEnviadaRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacaoSchedulerService {

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoEnviadaRepository notificacaoEnviadaRepository;
    private final NotificacaoAlertService alertService;
    private final RestTemplate restTemplate;
    private final NotificacaoTransactionalService transactionalService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Random random = new Random();

    // Configurações de rate limiting
    private static final long DELAY_FIXO_MS = 3 * 60 * 1000L; // 3 minutos
    private static final long DELAY_RANDOM_MAX_MS = 60 * 1000L; // até 60 segundos
    private static final int BATCH_SIZE = 50; // Processa em lotes de 50

    /**
     * Job principal - roda sem transação
     * Delega processamento transacional para outro serviço
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    public void processarNotificacoesPendentes() {
        LocalDateTime agora = LocalDateTime.now();


        log.info("=== Iniciando processamento de notificacoes ===");
        log.info("Horario atual: {}", agora);

        try {
            processarConfirmacoes(agora);
            processarLembretes(agora);
            log.info("=== Processamento concluido ===");
        } catch (Exception e) {
            log.error("Erro no processamento: {}", e.getMessage(), e);
        }
    }

    private void processarConfirmacoes(LocalDateTime agora) {
        try {
            List<NotificacaoPendenteDTO> confirmacoes = buscarConfirmacoesPendentes(agora);
            log.info("Encontradas {} confirmacoes pendentes", confirmacoes.size());

            if (!confirmacoes.isEmpty()) {
                processarNotificacoesEmLotes(confirmacoes, TipoNotificacao.CONFIRMACAO);
            }

            log.info("--- CONFIRMACOES processadas ---");
        } catch (Exception e) {
            log.error("Erro ao processar CONFIRMACOES: {}", e.getMessage(), e);
        }
    }

    private void processarLembretes(LocalDateTime agora) {
        try {
            List<NotificacaoPendenteDTO> lembretes = buscarLembretesPendentes(agora);
            log.info("Encontradas {} lembretes pendentes", lembretes.size());

            if (!lembretes.isEmpty()) {
                processarNotificacoesEmLotes(lembretes, TipoNotificacao.LEMBRETE);
            }

            log.info("--- LEMBRETES processados ---");
        } catch (Exception e) {
            log.error("Erro ao processar LEMBRETES: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa notificações em lotes para otimizar recursos
     */
    private void processarNotificacoesEmLotes(List<NotificacaoPendenteDTO> pendentes, TipoNotificacao tipo) {
        // Divide em lotes
        for (int i = 0; i < pendentes.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, pendentes.size());
            List<NotificacaoPendenteDTO> lote = pendentes.subList(i, end);

            log.info("Processando lote {}/{} ({} notificacoes)",
                    (i / BATCH_SIZE) + 1,
                    (pendentes.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                    lote.size());

            processarLote(lote);
        }
    }

    /**
     * Processa um lote de notificações
     */
    private void processarLote(List<NotificacaoPendenteDTO> lote) {
        for (NotificacaoPendenteDTO notif : lote) {
            try {
                // 1. Verifica se já foi enviada (rápido, só leitura)
                if (jaFoiEnviada(notif)) {
                    log.debug("Notificacao ja enviada: tipo={}, ag={}",
                            notif.getTipo(), notif.getAgendamentoId());
                    continue;
                }

                // 2. Valida telefone
                String telefone = formatarTelefone(notif.getTelefoneCliente()); //formatarTelefone("5532998220082");
                if (telefone == null || telefone.isBlank()) {
                    log.warn("Telefone invalido para agendamento {}", notif.getAgendamentoId());
                    transactionalService.registrarEnvioFalha(notif, "Telefone invalido", null);
                    continue;
                }

                // 3. Monta mensagem
                String mensagem = montarMensagem(notif);

                // 4. Envia mensagem (sem transação, operação externa)
                try {
                    WhatsAppSendResult resultado = enviarMensagemWhatsApp(notif.getInstanceName(), telefone, mensagem);

                    // 5. Registra sucesso em transação separada (commit imediato)
                    transactionalService.registrarEnvioSucesso(notif, telefone, resultado.remoteJid(), resultado.messageId());

                    log.info("Notificacao {} enviada: ag={}, org={}, remoteJid={}",
                            notif.getTipo(), notif.getAgendamentoId(), notif.getNomeOrganizacao(), resultado.remoteJid());

                } catch (Exception e) {
                    log.error("Falha ao enviar notificacao ag={}: {}",
                            notif.getAgendamentoId(), e.getMessage());
                    transactionalService.registrarEnvioFalha(notif, e.getMessage(), telefone);
                }

                // 6. Rate limiting - FORA da transação
                aplicarRateLimit();

            } catch (Exception e) {
                log.error("Erro ao processar notificacao ag={}: {}",
                        notif.getAgendamentoId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Aplica rate limiting entre mensagens
     */
    private void aplicarRateLimit() {
        try {
            long delayTotal = DELAY_FIXO_MS + random.nextInt((int) DELAY_RANDOM_MAX_MS);
            log.debug("Rate limit: aguardando {}ms", delayTotal);
            Thread.sleep(delayTotal);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Rate limit interrompido");
        }
    }

    /**
     * Record para encapsular o resultado do envio WhatsApp
     */
    private record WhatsAppSendResult(String remoteJid, String messageId) {}

    private WhatsAppSendResult enviarMensagemWhatsApp(String instanceName, String telefone, String mensagem) {
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

        // Extrair remoteJid e messageId da resposta do Evolution API
        String remoteJid = null;
        String messageId = null;
        try {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode keyNode = responseJson.path("key");
            if (!keyNode.isMissingNode()) {
                remoteJid = keyNode.path("remoteJid").asText(null);
                messageId = keyNode.path("id").asText(null);
            }
        } catch (Exception e) {
            log.warn("Não foi possível extrair remoteJid/messageId da resposta: {}", e.getMessage());
        }

        log.debug("Mensagem enviada para {} via instancia {} | remoteJid={}, messageId={}",
                telefone, instanceName, remoteJid, messageId);

        return new WhatsAppSendResult(remoteJid, messageId);
    }

    private boolean jaFoiEnviada(NotificacaoPendenteDTO notif) {
        return notificacaoEnviadaRepository.existsByAgendamentoIdAndTipoAndHorasAntes(
                notif.getAgendamentoId(), notif.getTipo(), notif.getHorasAntes());
    }

    private String montarMensagem(NotificacaoPendenteDTO notif) {
        String template = notif.getMensagemTemplate();
        if (template == null || template.isBlank()) {
            template = getTemplatePadrao(notif.getTipo());
        }
        return template
                .replace("{{nome_cliente}}", notif.getNomeCliente() != null ? notif.getNomeCliente() : "Cliente")
                .replace("{{data_agendamento}}", notif.getDataAgendamento().format(DATE_FMT))
                .replace("{{hora_agendamento}}", notif.getDataAgendamento().format(TIME_FMT))
                .replace("{{servico}}", notif.getNomeServico() != null ? notif.getNomeServico() : "Servico")
                .replace("{{profissional}}", notif.getNomeFuncionario() != null ? notif.getNomeFuncionario() : "Funcionario")
                .replace("{{local}}", notif.getEndereco() != null ? notif.getEndereco() : "Endereco")
                .replace("{{valor}}", notif.getValor() != null
                        ? String.format("R$ %,.2f", notif.getValor())
                        : "R$ 0,00")
                .replace("{{nome_empresa}}", notif.getNomeOrganizacao() != null ? notif.getNomeOrganizacao() : "Organizacao");
    }

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

    // Métodos de busca (mantidos iguais)
    public List<NotificacaoPendenteDTO> buscarConfirmacoesPendentes(LocalDateTime agora) {
        return agendamentoRepository.findConfirmacoesPendentes(agora).stream()
                .map(row -> new NotificacaoPendenteDTO(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        ((Timestamp) row[7]).toLocalDateTime(),
                        (BigDecimal) row[8],
                        (String) row[9],
                        TipoNotificacao.valueOf((String) row[10]),
                        ((Number) row[11]).intValue(),
                        (String) row[12],
                        (String) row[13]
                ))
                .toList();
    }

    public List<NotificacaoPendenteDTO> buscarLembretesPendentes(LocalDateTime agora) {
        return agendamentoRepository.findLembretesPendentes(agora).stream()
                .map(row -> new NotificacaoPendenteDTO(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        ((Timestamp) row[7]).toLocalDateTime(),
                        (BigDecimal) row[8],
                        (String) row[9],
                        TipoNotificacao.valueOf((String) row[10]),
                        ((Number) row[11]).intValue(),
                        (String) row[12],
                        (String) row[13]
                ))
                .toList();
    }
}
