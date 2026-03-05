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
     * Diario as 2:00 AM - Expira trials (migra para gratuito), marca cobrancas vencidas,
     * bloqueia cancelamentos expirados
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void jobDiario() {
        log.info("Iniciando job diario de assinaturas...");
        try {
            assinaturaService.expirarTrials();
            assinaturaService.marcarCobrancasVencidas();
            assinaturaService.bloquearCancelamentoExpirado();
            log.info("Job diario de assinaturas finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro no job diario de assinaturas: {}", e.getMessage(), e);
        }
    }

    /**
     * Diario as 9:00 AM - Notifica trials que expiram nos proximos 6 dias
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void jobNotificarTrialsExpirando() {
        log.info("Iniciando job de notificacao de trials expirando...");
        try {
            assinaturaService.notificarTrialsExpirando(6);
            log.info("Job de notificacao de trials expirando finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro no job de notificacao de trials expirando: {}", e.getMessage(), e);
        }
    }

    /**
     * Mensal no dia 1 as 6:00 AM - Gera cobrancas mensais
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

    /**
     * Diario as 3:00 AM - Verifica renovacoes anuais (gera cobranca 30 dias antes do vencimento)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void jobRenovacaoAnual() {
        log.info("Iniciando job de renovacao anual...");
        try {
            assinaturaService.gerarCobrancasAnuais();
            log.info("Job de renovacao anual finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro no job de renovacao anual: {}", e.getMessage(), e);
        }
    }
}
