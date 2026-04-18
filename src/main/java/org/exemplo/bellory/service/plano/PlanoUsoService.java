package org.exemplo.bellory.service.plano;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.dto.CachedCustomerStatus;
import org.exemplo.bellory.client.payment.dto.PaymentPlanLimitType;
import org.exemplo.bellory.client.payment.dto.PlanLimitDto;
import org.exemplo.bellory.model.dto.assinatura.LimiteUsoDTO;
import org.exemplo.bellory.model.dto.assinatura.PlanoUsoDTO;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.instance.InstanceRepository;
import org.exemplo.bellory.model.repository.landingpage.LandingPageRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
public class PlanoUsoService {

    private final AssinaturaCacheService cacheService;
    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final InstanceRepository instanceRepository;
    private final LandingPageRepository landingPageRepository;

    public PlanoUsoService(AssinaturaCacheService cacheService,
                           FuncionarioRepository funcionarioRepository,
                           ClienteRepository clienteRepository,
                           ServicoRepository servicoRepository,
                           AgendamentoRepository agendamentoRepository,
                           InstanceRepository instanceRepository,
                           LandingPageRepository landingPageRepository) {
        this.cacheService = cacheService;
        this.funcionarioRepository = funcionarioRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.instanceRepository = instanceRepository;
        this.landingPageRepository = landingPageRepository;
    }

    public PlanoUsoDTO getUso(Long organizacaoId) {
        CachedCustomerStatus status = cacheService.getCachedByOrganizacao(organizacaoId);
        if (status == null) {
            log.warn("PlanoUsoService: sem cache para org={}", organizacaoId);
            return PlanoUsoDTO.builder()
                    .limites(Collections.emptyList())
                    .features(Collections.emptyList())
                    .build();
        }

        Map<String, Supplier<Long>> contadores = buildContadores(organizacaoId);

        List<LimiteUsoDTO> limites = buildLimites(status.getLimits(), contadores);
        List<LimiteUsoDTO> features = buildFeatures(status.getFeatures(), contadores);

        return PlanoUsoDTO.builder()
                .planoCodigo(status.getPlanCodigo())
                .planoNome(status.getPlanName())
                .planoGratuito(status.isPlanIsFree())
                .limites(limites)
                .features(features)
                .build();
    }

    private Map<String, Supplier<Long>> buildContadores(Long orgId) {
        YearMonth mesAtual = YearMonth.now();
        LocalDateTime inicioMes = mesAtual.atDay(1).atStartOfDay();
        LocalDateTime fimMes = mesAtual.atEndOfMonth().atTime(23, 59, 59);

        return Map.of(
                "funcionario", () -> funcionarioRepository.countByOrganizacao_IdAndIsDeletadoFalse(orgId),
                "cliente", () -> clienteRepository.countByOrganizacao_IdAndAtivoTrue(orgId),
                "servicos", () -> servicoRepository.countByOrganizacao_IdAndIsDeletadoFalse(orgId),
                "agendamento", () -> agendamentoRepository.countByOrganizacaoAndPeriodo(orgId, inicioMes, fimMes),
                "agente_virtual", () -> instanceRepository.countByOrganizacaoIdAndDeletadoFalse(orgId),
                "site_externo", () -> landingPageRepository.countByOrganizacaoIdAndAtivoTrue(orgId)
        );
    }

    private List<LimiteUsoDTO> buildLimites(List<PlanLimitDto> planLimits, Map<String, Supplier<Long>> contadores) {
        if (planLimits == null || planLimits.isEmpty()) return Collections.emptyList();

        List<LimiteUsoDTO> result = new ArrayList<>();
        for (PlanLimitDto limit : planLimits) {
            LimiteUsoDTO.LimiteUsoDTOBuilder builder = LimiteUsoDTO.builder()
                    .key(limit.getKey())
                    .label(limit.getLabel())
                    .tipo(limit.getType().name());

            if (limit.getType() == PaymentPlanLimitType.UNLIMITED) {
                Long usado = contarUso(limit.getKey(), contadores);
                builder.usado(usado);
            } else if (limit.getType() == PaymentPlanLimitType.NUMBER) {
                Long maximo = limit.getValue() != null ? limit.getValue() : 0L;
                Long usado = contarUso(limit.getKey(), contadores);
                builder.limite(maximo)
                       .usado(usado);
                if (usado != null) {
                    builder.disponivel(Math.max(0, maximo - usado));
                }
            } else if (limit.getType() == PaymentPlanLimitType.BOOLEAN) {
                builder.habilitado(Boolean.TRUE.equals(limit.getEnabled()));
            }

            result.add(builder.build());
        }
        return result;
    }

    private List<LimiteUsoDTO> buildFeatures(List<PlanLimitDto> planFeatures, Map<String, Supplier<Long>> contadores) {
        if (planFeatures == null || planFeatures.isEmpty()) return Collections.emptyList();

        List<LimiteUsoDTO> result = new ArrayList<>();
        for (PlanLimitDto feature : planFeatures) {
            result.add(LimiteUsoDTO.builder()
                    .key(feature.getKey())
                    .label(feature.getLabel())
                    .tipo(feature.getType().name())
                    .habilitado(feature.getType() == PaymentPlanLimitType.UNLIMITED
                            || Boolean.TRUE.equals(feature.getEnabled()))
                    .build());
        }
        return result;
    }

    private Long contarUso(String key, Map<String, Supplier<Long>> contadores) {
        Supplier<Long> contador = contadores.get(key);
        if (contador == null) return null;
        try {
            Long valor = contador.get();
            return valor != null ? valor : 0L;
        } catch (Exception e) {
            log.warn("Erro ao contar uso para key='{}': {}", key, e.getMessage());
            return null;
        }
    }
}
