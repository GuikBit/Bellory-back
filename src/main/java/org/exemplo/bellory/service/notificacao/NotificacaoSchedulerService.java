package org.exemplo.bellory.service.notificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.exemplo.bellory.model.entity.template.CategoriaTemplate;
import org.exemplo.bellory.model.entity.template.TemplateBellory;
import org.exemplo.bellory.model.entity.template.TipoTemplate;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.notificacao.NotificacaoEnviadaRepository;
import org.exemplo.bellory.model.repository.template.TemplateBelloryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacaoSchedulerService {

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoEnviadaRepository notificacaoEnviadaRepository;
    private final NotificacaoAlertService alertService;
    private final NotificacaoTransactionalService transactionalService;
    private final TemplateBelloryRepository templateBelloryRepository;
    private final MessageTemplateRenderer templateRenderer;
    private final EvolutionApiClient evolutionApiClient;

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
                String telefone = formatarTelefone(notif.getTelefoneCliente());
                if (telefone == null || telefone.isBlank()) {
                    log.warn("Telefone inválido para agendamento {}", notif.getAgendamentoId());
                    transactionalService.registrarEnvioFalha(notif, "Telefone inválido", null);
                    continue;
                }

                // 3. Monta mensagem
                String mensagem = montarMensagem(notif);

                // 4. Envia mensagem (sem transação, operação externa)
                try {
                    EvolutionApiClient.SendResult resultado =
                            evolutionApiClient.sendText(notif.getInstanceName(), telefone, mensagem);

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

    private boolean jaFoiEnviada(NotificacaoPendenteDTO notif) {
        return notificacaoEnviadaRepository.existsByAgendamentoIdAndTipoAndHorasAntes(
                notif.getAgendamentoId(), notif.getTipo(), notif.getHorasAntes());
    }

    private String montarMensagem(NotificacaoPendenteDTO notif) {
        String template = notif.getMensagemTemplate();
        if (template == null || template.isBlank()) {
            template = getTemplatePadrao(notif.getTipo());
        }

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nome_cliente", notif.getNomeCliente() != null ? notif.getNomeCliente() : "Cliente");
        vars.put("data_agendamento", notif.getDataAgendamento().format(DATE_FMT));
        vars.put("hora_agendamento", notif.getDataAgendamento().format(TIME_FMT));
        vars.put("servico", notif.getNomeServico() != null ? notif.getNomeServico() : "Servico");
        vars.put("profissional", notif.getNomeFuncionario() != null ? notif.getNomeFuncionario() : "Funcionario");
        vars.put("local", notif.getEndereco() != null ? notif.getEndereco() : "Endereco");
        vars.put("valor", notif.getValor() != null
                ? String.format("R$ %,.2f", notif.getValor())
                : "R$ 0,00");
        vars.put("nome_empresa", notif.getNomeOrganizacao() != null ? notif.getNomeOrganizacao() : "Organizacao");

        return templateRenderer.render(template, vars);
    }

    private String getTemplatePadrao(TipoNotificacao tipo) {
        CategoriaTemplate cat = tipo == TipoNotificacao.CONFIRMACAO
                ? CategoriaTemplate.CONFIRMACAO
                : CategoriaTemplate.LEMBRETE;

        return templateBelloryRepository
                .findByTipoAndCategoriaAndPadraoTrue(TipoTemplate.WHATSAPP, cat)
                .map(TemplateBellory::getConteudo)
                .orElseGet(() -> getFallbackTemplate(tipo));
    }

    private String getFallbackTemplate(TipoNotificacao tipo) {
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
            case ANAMNESE, FILA_ESPERA_OFERTA, FILA_ESPERA_PERDEU_VEZ -> throw new IllegalStateException(
                    "Tipo " + tipo + " nao e processado pelo NotificacaoSchedulerService (event-driven, ver AnamneseWhatsAppService / FilaEsperaService).");
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
