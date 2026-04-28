package org.exemplo.bellory.service.filaespera;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Schedulers da Fila de Espera.
 *
 * <ul>
 *   <li><b>processarTimeouts</b> (1 min): expira tentativas AGUARDANDO_RESPOSTA
 *       e avanca para o proximo candidato.</li>
 *   <li><b>limparFilaDiariamente</b> (00:00 diario): remove a flag {@code entrouFilaEspera}
 *       de agendamentos cuja data ja chegou.</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FilaEsperaScheduler {

    private final FilaEsperaService filaEsperaService;

    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void processarTimeouts() {
        try {
            filaEsperaService.expirarTentativasPendentes();
        } catch (Exception e) {
            log.error("[Fila] Erro no scheduler de timeouts", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void limparFilaDiariamente() {
        try {
            filaEsperaService.removerDaFilaPorAgendamentoVencido();
        } catch (Exception e) {
            log.error("[Fila] Erro no cleanup diario da fila", e);
        }
    }
}
