package org.exemplo.bellory.service.filaespera;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.config.ConfigAgendamento;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.fila.FilaEsperaTentativa;
import org.exemplo.bellory.model.entity.fila.StatusFilaTentativa;
import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.event.AgendamentoAdiantadoFilaEvent;
import org.exemplo.bellory.model.event.FilaOfertaCriadaEvent;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.config.ConfigSistemaRepository;
import org.exemplo.bellory.model.repository.fila.FilaEsperaTentativaRepository;
import org.exemplo.bellory.model.repository.funcionario.BloqueioAgendaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Orquestra a Fila de Espera: ao receber um slot liberado por cancelamento,
 * encontra o proximo cliente FIFO compativel e cria uma tentativa que sera
 * disparada via WhatsApp (PR 4) e respondida via endpoint (PR 3).
 *
 * <p>Cascata (re-disparo a partir do slot original quando alguem aceita) e
 * implementada em PR 5.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FilaEsperaService {

    private final AgendamentoRepository agendamentoRepository;
    private final FilaEsperaTentativaRepository filaTentativaRepository;
    private final ConfigSistemaRepository configSistemaRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final List<Status> STATUS_ELEGIVEIS = List.of(
            Status.AGENDADO, Status.CONFIRMADO, Status.AGUARDANDO_CONFIRMACAO);

    // ====================================================================
    // ORQUESTRACAO DO SLOT LIBERADO
    // ====================================================================

    /**
     * Entry point para slot liberado por cancelamento (nivel 1, sem cascata previa).
     */
    @Transactional
    public void processarSlotLiberado(Long organizacaoId,
                                      Long agendamentoCanceladoId,
                                      Long funcionarioId,
                                      LocalDateTime slotInicio,
                                      LocalDateTime slotFim) {
        processarSlotLiberado(organizacaoId, agendamentoCanceladoId, funcionarioId,
                slotInicio, slotFim, null, 1);
    }

    /**
     * Avalia um slot liberado e cria uma tentativa de fila para o proximo candidato FIFO.
     * Suporta cascata: {@code cascataNivel} > 1 quando re-oferta o slot vago apos um aceite anterior.
     *
     * <p>Roda em transacao propria (chamada apos AFTER_COMMIT do listener).
     */
    @Transactional
    public void processarSlotLiberado(Long organizacaoId,
                                      Long agendamentoCanceladoId,
                                      Long funcionarioId,
                                      LocalDateTime slotInicio,
                                      LocalDateTime slotFim,
                                      Long cascataOrigemId,
                                      int cascataNivel) {

        ConfigAgendamento cfg = carregarConfigAgendamento(organizacaoId);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getUsarFilaEspera())) {
            log.debug("[Fila] Org {} nao tem fila habilitada, ignorando", organizacaoId);
            return;
        }

        int maxCascata = orDefault(cfg.getFilaMaxCascata(), 5);
        if (cascataNivel > maxCascata) {
            log.info("[Fila] Limite de cascata atingido ({}/{}) - slot {} fica livre para agendamento normal",
                    cascataNivel, maxCascata, slotInicio);
            return;
        }

        int antecedenciaHoras = orDefault(cfg.getFilaAntecedenciaHoras(), 3);
        LocalDateTime minimoSlot = LocalDateTime.now().plusHours(antecedenciaHoras);
        if (slotInicio.isBefore(minimoSlot)) {
            log.info("[Fila] Slot {} muito proximo (< {}h antecedencia), nao oferecera",
                    slotInicio, antecedenciaHoras);
            return;
        }

        long duracaoSlotMin = ChronoUnit.MINUTES.between(slotInicio, slotFim);
        int tolerancia = orDefault(cfg.getToleranciaAgendamento(), 0);

        Agendamento candidato = encontrarProximoCandidato(
                organizacaoId, funcionarioId, slotInicio, agendamentoCanceladoId,
                duracaoSlotMin, tolerancia);

        if (candidato == null) {
            log.info("[Fila] Sem candidato compativel para slot {}-{} func {} (nivel {})",
                    slotInicio, slotFim, funcionarioId, cascataNivel);
            return;
        }

        int timeoutMin = orDefault(cfg.getFilaTimeoutMinutos(), 30);

        FilaEsperaTentativa tentativa = FilaEsperaTentativa.builder()
                .organizacao(candidato.getOrganizacao())
                .agendamento(candidato)
                .agendamentoCanceladoId(agendamentoCanceladoId)
                .funcionarioId(funcionarioId)
                .slotInicio(slotInicio)
                .slotFim(slotFim)
                .status(StatusFilaTentativa.PENDENTE)
                .dtExpira(LocalDateTime.now().plusMinutes(timeoutMin))
                .cascataOrigemId(cascataOrigemId)
                .cascataNivel(cascataNivel)
                .build();

        FilaEsperaTentativa salva = filaTentativaRepository.save(tentativa);
        log.info("[Fila] Tentativa {} criada (nivel {}) para agendamento {} no slot {} (cliente {})",
                salva.getId(), cascataNivel, candidato.getId(), slotInicio,
                candidato.getCliente() != null ? candidato.getCliente().getId() : null);

        // FilaEsperaDispatchService dispara via WhatsApp em listener AFTER_COMMIT/async
        eventPublisher.publishEvent(new FilaOfertaCriadaEvent(this, salva.getId(), organizacaoId));
    }

    /**
     * Avanca para o proximo candidato apos uma tentativa ser RECUSADA/EXPIRADA.
     * Mantem o mesmo nivel de cascata (mesmo slot, proximo da fila).
     */
    @Transactional
    public void processarProximoCandidato(FilaEsperaTentativa anterior) {
        processarSlotLiberado(
                anterior.getOrganizacao().getId(),
                anterior.getAgendamentoCanceladoId(),
                anterior.getFuncionarioId(),
                anterior.getSlotInicio(),
                anterior.getSlotFim(),
                anterior.getCascataOrigemId(),
                orDefault(anterior.getCascataNivel(), 1));
    }

    // ====================================================================
    // RESPOSTAS DO CLIENTE
    // ====================================================================

    /**
     * Cliente aceitou a oferta: UPDATE no agendamento existente para o novo slot.
     */
    @Transactional
    public Agendamento aceitarOferta(Long tentativaId) {
        FilaEsperaTentativa tentativa = filaTentativaRepository.findById(tentativaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tentativa de fila " + tentativaId + " nao encontrada"));

        if (tentativa.isFinalizada()) {
            throw new IllegalStateException(
                    "Tentativa " + tentativaId + " ja finalizada (" + tentativa.getStatus() + ")");
        }

        Agendamento agendamento = tentativa.getAgendamento();
        LocalDateTime slotInicio = tentativa.getSlotInicio();
        LocalDateTime slotFim = tentativa.getSlotFim();

        // Capturar dt antiga ANTES do update — esse e o slot que vai abrir e dispara cascata.
        // Diferente de dtOriginalFila (que persiste a primeira data e e usada para historico).
        LocalDateTime dtSlotVago = agendamento.getDtAgendamento();

        // Snapshot da data original (preserva apenas no primeiro reagendamento por fila)
        if (agendamento.getDtOriginalFila() == null) {
            agendamento.setDtOriginalFila(agendamento.getDtAgendamento());
        }

        agendamento.setDtAgendamento(slotInicio);
        agendamento.setReagendadoPorFila(true);
        agendamento.setEntrouFilaEspera(false);
        agendamento.setDtAtualizacao(LocalDateTime.now());

        // Move o BloqueioAgenda do agendamento para o novo slot
        BloqueioAgenda bloqueio = agendamento.getBloqueioAgenda();
        if (bloqueio != null) {
            bloqueio.setInicioBloqueio(slotInicio);
            bloqueio.setFimBloqueio(slotFim);
            bloqueioAgendaRepository.save(bloqueio);
        }

        agendamentoRepository.save(agendamento);

        tentativa.setStatus(StatusFilaTentativa.ACEITO);
        tentativa.setDtResposta(LocalDateTime.now());
        filaTentativaRepository.save(tentativa);

        // Marca tentativas concorrentes (mesmo slot/func) como SUPERADO
        List<FilaEsperaTentativa> concorrentes = filaTentativaRepository.findAtivasParaSlot(
                tentativa.getFuncionarioId(), slotInicio, slotFim);
        for (FilaEsperaTentativa c : concorrentes) {
            if (!c.getId().equals(tentativaId)) {
                c.setStatus(StatusFilaTentativa.SUPERADO);
                c.setDtAtualizacao(LocalDateTime.now());
                filaTentativaRepository.save(c);
            }
        }

        log.info("[Fila] Aceito tentativa {} (nivel {}) - agendamento {} adiantado de {} para {}",
                tentativaId, tentativa.getCascataNivel(), agendamento.getId(), dtSlotVago, slotInicio);

        // Push admin + funcionario via NotificacaoPushEventListener
        eventPublisher.publishEvent(buildAdiantadoEvent(agendamento));

        // CASCATA: o slot vago (dtSlotVago) pode ser oferecido ao proximo da fila.
        dispararCascataParaSlotVago(agendamento, dtSlotVago, tentativa);

        return agendamento;
    }

    /**
     * Apos um aceite, oferece o slot vago (dt original do agendamento que se moveu)
     * ao proximo candidato da fila, incrementando o nivel de cascata. Limite definido
     * por {@code ConfigAgendamento.filaMaxCascata} (default 5).
     */
    private void dispararCascataParaSlotVago(Agendamento agendamentoMovido,
                                             LocalDateTime dtSlotVago,
                                             FilaEsperaTentativa tentativaAceita) {
        Long orgId = agendamentoMovido.getOrganizacao().getId();
        ConfigAgendamento cfg = carregarConfigAgendamento(orgId);
        if (cfg == null || !Boolean.TRUE.equals(cfg.getUsarFilaEspera())) {
            return;
        }

        int maxCascata = orDefault(cfg.getFilaMaxCascata(), 5);
        int nivelAtual = orDefault(tentativaAceita.getCascataNivel(), 1);
        if (nivelAtual >= maxCascata) {
            log.info("[Fila] Limite de cascata atingido ({}/{}) — slot {} fica livre para agendamento normal",
                    nivelAtual, maxCascata, dtSlotVago);
            return;
        }

        int proximoNivel = nivelAtual + 1;
        if (agendamentoMovido.getServicos() == null || agendamentoMovido.getServicos().isEmpty()
                || agendamentoMovido.getFuncionarios() == null || agendamentoMovido.getFuncionarios().isEmpty()) {
            log.warn("[Fila] Cascata abortada: agendamento {} sem servicos/funcionarios", agendamentoMovido.getId());
            return;
        }

        int duracao = agendamentoMovido.getServicos().stream()
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();
        LocalDateTime slotFimVago = dtSlotVago.plusMinutes(duracao);
        Long rootCanceladoId = tentativaAceita.getAgendamentoCanceladoId();
        Long origemId = tentativaAceita.getId();

        for (Funcionario f : agendamentoMovido.getFuncionarios()) {
            try {
                processarSlotLiberado(
                        orgId, rootCanceladoId, f.getId(),
                        dtSlotVago, slotFimVago,
                        origemId, proximoNivel);
            } catch (Exception e) {
                log.error("[Fila] Erro na cascata nivel {} func {}: {}",
                        proximoNivel, f.getId(), e.getMessage(), e);
            }
        }
    }

    private AgendamentoAdiantadoFilaEvent buildAdiantadoEvent(Agendamento ag) {
        java.util.List<Long> funcIds = ag.getFuncionarios() == null ? java.util.List.of()
                : ag.getFuncionarios().stream().map(Funcionario::getId).collect(Collectors.toList());
        String nomeServicos = ag.getServicos() == null ? "" : ag.getServicos().stream()
                .map(Servico::getNome).collect(Collectors.joining(", "));
        String nomeProf = ag.getFuncionarios() == null ? "" : ag.getFuncionarios().stream()
                .map(Funcionario::getNomeCompleto).collect(Collectors.joining(", "));
        return new AgendamentoAdiantadoFilaEvent(
                this,
                ag.getId(),
                ag.getCliente() != null ? ag.getCliente().getId() : null,
                ag.getCliente() != null ? ag.getCliente().getNomeCompleto() : null,
                funcIds,
                ag.getOrganizacao().getId(),
                ag.getDtOriginalFila(),
                ag.getDtAgendamento(),
                nomeServicos,
                nomeProf
        );
    }

    /**
     * Cliente recusou a oferta: mantem na fila, dispara para o proximo.
     */
    @Transactional
    public void recusarOferta(Long tentativaId) {
        FilaEsperaTentativa tentativa = filaTentativaRepository.findById(tentativaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tentativa de fila " + tentativaId + " nao encontrada"));

        if (tentativa.isFinalizada()) {
            throw new IllegalStateException(
                    "Tentativa " + tentativaId + " ja finalizada (" + tentativa.getStatus() + ")");
        }

        tentativa.setStatus(StatusFilaTentativa.RECUSADO);
        tentativa.setDtResposta(LocalDateTime.now());
        filaTentativaRepository.save(tentativa);

        log.info("[Fila] Recusado tentativa {} - avaliando proximo da fila", tentativaId);

        // Avanca para o proximo candidato em transacao separada (apos commit)
        processarProximoCandidato(tentativa);
    }

    // ====================================================================
    // SCHEDULER HOOKS
    // ====================================================================

    /**
     * Marca tentativas AGUARDANDO_RESPOSTA cujo timeout expirou e avanca
     * para o proximo candidato FIFO.
     */
    @Transactional
    public void expirarTentativasPendentes() {
        List<FilaEsperaTentativa> expiradas = filaTentativaRepository.findExpiradas(LocalDateTime.now());

        if (expiradas.isEmpty()) {
            return;
        }

        log.info("[Fila] Expirando {} tentativas sem resposta", expiradas.size());

        for (FilaEsperaTentativa t : expiradas) {
            try {
                t.setStatus(StatusFilaTentativa.EXPIRADO);
                t.setDtAtualizacao(LocalDateTime.now());
                filaTentativaRepository.save(t);

                processarProximoCandidato(t);
            } catch (Exception e) {
                log.error("[Fila] Erro ao expirar tentativa {}", t.getId(), e);
            }
        }
    }

    /**
     * Remove a flag de fila em agendamentos cuja data ja chegou.
     * Roda no scheduler diario.
     */
    @Transactional
    public int removerDaFilaPorAgendamentoVencido() {
        LocalDateTime amanha = LocalDate.now().plusDays(1).atStartOfDay();
        int afetados = agendamentoRepository.removerFilaEsperaPorDataChegada(amanha);
        if (afetados > 0) {
            log.info("[Fila] {} agendamentos saiu da fila por chegada do dia", afetados);
        }
        return afetados;
    }

    /**
     * Quando um cliente cancela seu proprio agendamento, qualquer tentativa
     * ativa que tenha esse agendamento como target deve ser invalidada — ele
     * nao e mais um candidato valido. Marcamos como SUPERADO (semantica de
     * "nao acionavel mais") e avancamos para o proximo da fila.
     */
    @Transactional
    public void cancelarTentativasPorAgendamento(Long agendamentoId) {
        List<FilaEsperaTentativa> ativas = filaTentativaRepository.findAtivasPorAgendamento(agendamentoId);
        if (ativas.isEmpty()) {
            return;
        }
        log.info("[Fila] Invalidando {} tentativa(s) do agendamento cancelado {}",
                ativas.size(), agendamentoId);
        for (FilaEsperaTentativa t : ativas) {
            t.setStatus(StatusFilaTentativa.SUPERADO);
            t.setDtAtualizacao(LocalDateTime.now());
            filaTentativaRepository.save(t);
            try {
                processarProximoCandidato(t);
            } catch (Exception e) {
                log.error("[Fila] Erro ao avancar fila apos invalidacao da tentativa {}", t.getId(), e);
            }
        }
    }

    // ====================================================================
    // CONSULTAS (controller)
    // ====================================================================

    @Transactional(readOnly = true)
    public FilaEsperaTentativa buscarTentativa(Long tentativaId) {
        return filaTentativaRepository.findById(tentativaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tentativa de fila " + tentativaId + " nao encontrada"));
    }

    @Transactional(readOnly = true)
    public List<FilaEsperaTentativa> listarTentativasDoAgendamento(Long agendamentoId) {
        return filaTentativaRepository.findByAgendamentoIdOrderByDtCriacaoDesc(agendamentoId);
    }

    // ====================================================================
    // HELPERS
    // ====================================================================

    private ConfigAgendamento carregarConfigAgendamento(Long organizacaoId) {
        ConfigSistema config = configSistemaRepository.findByOrganizacaoId(organizacaoId).orElse(null);
        if (config == null) return null;
        return config.getConfigAgendamento();
    }

    private Agendamento encontrarProximoCandidato(Long organizacaoId,
                                                   Long funcionarioId,
                                                   LocalDateTime slotInicio,
                                                   Long agendamentoCanceladoId,
                                                   long duracaoSlotMin,
                                                   int tolerancia) {

        List<Agendamento> candidatos = agendamentoRepository.findCandidatosFilaEspera(
                organizacaoId, funcionarioId, slotInicio, agendamentoCanceladoId, STATUS_ELEGIVEIS);

        for (Agendamento a : candidatos) {
            int duracao = a.getServicos().stream()
                    .mapToInt(Servico::getTempoEstimadoMinutos)
                    .sum();
            if (duracao + tolerancia > duracaoSlotMin) {
                continue;
            }
            if (filaTentativaRepository.existsAtivaPorAgendamento(a.getId())) {
                continue;
            }
            return a;
        }
        return null;
    }

    private static int orDefault(Integer v, int def) {
        return v == null ? def : v;
    }
}
