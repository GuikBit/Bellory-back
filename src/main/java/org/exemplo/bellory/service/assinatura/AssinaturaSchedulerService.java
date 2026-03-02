package org.exemplo.bellory.service.assinatura;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssinaturaSchedulerService {

    private final AssinaturaService assinaturaService;

    public AssinaturaSchedulerService(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    /**
     * Diario as 2:00 AM - Expira trials vencidos e marca cobrancas vencidas
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void jobDiario() {
        log.info("Iniciando job diario de assinaturas...");
        try {
            assinaturaService.expirarTrials();
            assinaturaService.marcarCobrancasVencidas();
            log.info("Job diario de assinaturas finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro no job diario de assinaturas: {}", e.getMessage(), e);
        }
    }

    /**
     * Mensal no dia 1 as 6:00 AM - Gera cobrancas do mes
     */
    @Scheduled(cron = "0 0 6 1 * *")
    public void jobMensalCobrancas() {
        log.info("Iniciando job mensal de geracao de cobrancas...");
        try {
            assinaturaService.gerarCobrancasMensais();
            log.info("Job mensal de cobrancas finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro no job mensal de cobrancas: {}", e.getMessage(), e);
        }
    }
}
