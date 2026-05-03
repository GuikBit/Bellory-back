package org.exemplo.bellory.service.filaespera;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.event.AgendamentoCanceladoEvent;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reage a cancelamentos de agendamento avaliando se o slot liberado pode ser
 * oferecido a alguem na fila de espera.
 *
 * <p>Roda async, AFTER_COMMIT do cancelamento. Cada funcionario do agendamento
 * cancelado e processado independentemente.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FilaEsperaListener {

    private final AgendamentoRepository agendamentoRepository;
    private final FilaEsperaService filaEsperaService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAgendamentoCancelado(AgendamentoCanceladoEvent event) {
        log.info("[Fila] Avaliando slot liberado pelo agendamento {}", event.getAgendamentoId());

        // Cleanup: se o agendamento cancelado era TARGET de tentativas ativas
        // (cliente que estava na fila cancelou o proprio agendamento), invalida
        // as ofertas e avanca pra proxima da fila.
        try {
            filaEsperaService.cancelarTentativasPorAgendamento(event.getAgendamentoId());
        } catch (Exception e) {
            log.error("[Fila] Erro no cleanup de tentativas do agendamento {}",
                    event.getAgendamentoId(), e);
        }

        Agendamento cancelado = agendamentoRepository.findById(event.getAgendamentoId()).orElse(null);
        if (cancelado == null) {
            log.warn("[Fila] Agendamento {} nao encontrado, ignorando", event.getAgendamentoId());
            return;
        }

        List<Servico> servicos = cancelado.getServicos();
        if (servicos == null || servicos.isEmpty()) {
            log.warn("[Fila] Agendamento {} sem servicos, sem como calcular slot", event.getAgendamentoId());
            return;
        }

        int duracaoMin = servicos.stream()
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();

        LocalDateTime slotInicio = event.getDtAgendamento();
        LocalDateTime slotFim = slotInicio.plusMinutes(duracaoMin);

        List<Long> funcIds = event.getFuncionarioIds();
        if (funcIds == null || funcIds.isEmpty()) {
            log.warn("[Fila] Agendamento {} sem funcionarios, nao havera match", event.getAgendamentoId());
            return;
        }

        for (Long funcionarioId : funcIds) {
            try {
                filaEsperaService.processarSlotLiberado(
                        event.getOrganizacaoId(),
                        event.getAgendamentoId(),
                        funcionarioId,
                        slotInicio,
                        slotFim);
            } catch (Exception e) {
                log.error("[Fila] Erro ao processar slot funcionario={} agendamento={}",
                        funcionarioId, event.getAgendamentoId(), e);
            }
        }
    }
}
