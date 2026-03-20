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
     * Diario as 2:00 AM - Bloqueia cancelamentos expirados.
     * REMOVIDO: expirarTrials (trial expirado agora bloqueia o acesso ate o admin escolher um plano)
     * REMOVIDO: marcarCobrancasVencidas (Asaas gerencia status de pagamento)
     * REMOVIDO: gerarCobrancasMensais (Asaas gera automaticamente)
     * REMOVIDO: gerarCobrancasAnuais (Asaas gera automaticamente)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void jobDiario() {
        log.info("Iniciando job diario de assinaturas...");
        try {
            assinaturaService.bloquearCancelamentoExpirado();
            log.info("Job diario de assinaturas finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro no job diario de assinaturas: {}", e.getMessage(), e);
        }
    }

    /**
     * Diario as 3:00 AM - Verifica inadimplentes consultando o Asaas
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void jobVerificarInadimplentes() {
        log.info("Iniciando job de verificacao de inadimplentes...");
        try {
            assinaturaService.verificarInadimplentes();
            log.info("Job de verificacao de inadimplentes finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro no job de verificacao de inadimplentes: {}", e.getMessage(), e);
        }
    }

    /**
     * A cada 2 horas - Efetiva trocas de plano agendadas cujo ciclo ja virou
     * (backup caso o webhook nao dispare)
     */
    @Scheduled(cron = "0 30 */2 * * *")
    public void jobEfetivarTrocasAgendadas() {
        log.info("Iniciando job de efetivacao de trocas de plano agendadas...");
        try {
            assinaturaService.efetivarTrocasAgendadasVencidas();
            log.info("Job de efetivacao de trocas agendadas finalizado com sucesso.");
        } catch (Exception e) {
            log.error("Erro no job de efetivacao de trocas agendadas: {}", e.getMessage(), e);
        }
    }

    /**
     * A cada 2 horas - Sincroniza status local com o Asaas (backup dos webhooks)
     */
    @Scheduled(cron = "0 0 */2 * * *")
    public void jobSincronizarComAsaas() {
        log.info("Iniciando sincronizacao com Asaas...");
        try {
            assinaturaService.sincronizarComAsaas();
            log.info("Sincronizacao com Asaas finalizada com sucesso.");
        } catch (Exception e) {
            log.error("Erro na sincronizacao com Asaas: {}", e.getMessage(), e);
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
}
