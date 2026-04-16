package org.exemplo.bellory.service.plano;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.dto.CachedCustomerStatus;
import org.exemplo.bellory.client.payment.dto.PaymentPlanLimitType;
import org.exemplo.bellory.client.payment.dto.PlanLimitDto;
import org.exemplo.bellory.exception.LimitePlanoExcedidoException;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Valida limites de plano da organizacao consultando o cache da Payment API.
 *
 * <p>Uso tipico: chamar {@link #validar(Long, TipoLimite, int)} ANTES de persistir
 * um recurso novo (cliente, agendamento, funcionario, etc.). Se o limite for
 * {@code UNLIMITED} ou o plano nao possuir a chave, libera; se for {@code NUMBER}
 * compara {@code novoTotalAposOperacao <= value}; se for {@code BOOLEAN}, respeita
 * {@code enabled} (usado para features on/off como landing page e API).</p>
 *
 * <p>Fail-open: se a Payment API estiver indisponivel e sem cache stale,
 * {@link AssinaturaCacheService#getCachedByOrganizacao(Long)} retorna {@code null}
 * e este validator libera a operacao (mesma filosofia do {@code AssinaturaInterceptor}).</p>
 */
@Slf4j
@Service
public class LimiteValidatorService {

    private final AssinaturaCacheService cacheService;

    public LimiteValidatorService(AssinaturaCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Valida um limite numerico. Lanca {@link LimitePlanoExcedidoException} se excedido.
     *
     * @param organizacaoId tenant
     * @param tipo tipo do limite (mapeia para a key do plano)
     * @param totalAposOperacao quantidade total DEPOIS da operacao (ex.: count+1 ao cadastrar)
     */
    public void validar(Long organizacaoId, TipoLimite tipo, int totalAposOperacao) {
        if (organizacaoId == null || tipo == null) return;

        CachedCustomerStatus status = cacheService.getCachedByOrganizacao(organizacaoId);
        if (status == null) {
            log.warn("LimiteValidator fail-open: sem cache para org={} (tipo={})", organizacaoId, tipo);
            return;
        }

        Optional<PlanLimitDto> limitOpt = findLimit(status, tipo.getKey());
        if (limitOpt.isEmpty()) {
            log.debug("Limite '{}' nao definido no plano '{}' — liberado", tipo.getKey(), status.getPlanCodigo());
            return;
        }
        PlanLimitDto limit = limitOpt.get();

        if (limit.getType() == PaymentPlanLimitType.UNLIMITED) {
            return;
        }

        if (limit.getType() == PaymentPlanLimitType.BOOLEAN) {
            // NUMBER-ish: tratado como feature on/off. Se desabilitado, bloqueia qualquer total > 0.
            if (!Boolean.TRUE.equals(limit.getEnabled()) && totalAposOperacao > 0) {
                throw new LimitePlanoExcedidoException(
                        buildMsg(tipo, status.getPlanName(), null, totalAposOperacao),
                        tipo.getKey(), 0L, totalAposOperacao);
            }
            return;
        }

        // NUMBER
        Long maximo = limit.getValue();
        if (maximo == null) {
            log.warn("Limite NUMBER '{}' sem value no plano '{}' — tratando como 0", tipo.getKey(), status.getPlanCodigo());
            maximo = 0L;
        }
        if (totalAposOperacao > maximo) {
            throw new LimitePlanoExcedidoException(
                    buildMsg(tipo, status.getPlanName(), maximo, totalAposOperacao),
                    tipo.getKey(), maximo, totalAposOperacao);
        }
    }

    /**
     * Checa se uma feature BOOLEAN esta habilitada. Retorna true se UNLIMITED ou
     * BOOLEAN+enabled, e false se desabilitada. Key nao encontrada = false (conservador).
     * Use para gates on/off (permite API, permite agendamento online, etc.).
     * Fail-open: sem cache → retorna true.
     */
    public boolean podeUsar(Long organizacaoId, TipoLimite tipo) {
        if (organizacaoId == null || tipo == null) return true;
        CachedCustomerStatus status = cacheService.getCachedByOrganizacao(organizacaoId);
        if (status == null) return true;
        return findLimit(status, tipo.getKey())
                .map(l -> switch (l.getType()) {
                    case UNLIMITED -> true;
                    case BOOLEAN -> Boolean.TRUE.equals(l.getEnabled());
                    case NUMBER -> l.getValue() != null && l.getValue() > 0;
                })
                .orElse(false);
    }

    /**
     * Forca pode-usar com exception (equivalente a {@link #podeUsar} + throw).
     */
    public void validarFeatureHabilitada(Long organizacaoId, TipoLimite tipo) {
        if (!podeUsar(organizacaoId, tipo)) {
            throw new LimitePlanoExcedidoException(
                    "Recurso '" + tipo.getLabel() + "' nao esta disponivel no seu plano. Faca upgrade para liberar.",
                    tipo.getKey());
        }
    }

    private Optional<PlanLimitDto> findLimit(CachedCustomerStatus status, String key) {
        List<PlanLimitDto> limits = status.getLimits();
        if (limits == null || limits.isEmpty()) return Optional.empty();
        return limits.stream()
                .filter(l -> key.equalsIgnoreCase(l.getKey()))
                .findFirst();
    }

    private String buildMsg(TipoLimite tipo, String planName, Long maximo, int totalAposOperacao) {
        String plano = planName != null ? planName : "atual";
        if (maximo == null) {
            return "Recurso '" + tipo.getLabel() + "' nao esta disponivel no plano " + plano
                    + ". Faca upgrade para liberar.";
        }
        return "Limite de " + tipo.getLabel() + " excedido. Plano " + plano
                + " permite ate " + maximo + " (tentativa: " + totalAposOperacao + "). Faca upgrade para ampliar.";
    }

    /**
     * Mapeia conceitos de negocio do Bellory para as keys configuradas nos planos
     * da Payment API. Manter sincronizado com as keys acordadas.
     */
    public enum TipoLimite {
        CLIENTE("cliente", "clientes"),
        AGENDAMENTO("agendamento", "agendamentos"),
        FUNCIONARIO("funcionario", "funcionarios"),
        SERVICO("servicos", "servicos"),
        UNIDADE("unidade", "unidades"),
        ARQUIVOS("arquivos", "arquivos"),
        SITE_EXTERNO("site_externo", "site externo"),
        AGENTE_VIRTUAL("agente_virtual", "agentes virtuais"),
        API("api", "API"),
        RELATORIOS("relatorios", "relatorios");

        private final String key;
        private final String label;

        TipoLimite(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
    }
}
