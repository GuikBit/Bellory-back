package org.exemplo.bellory.service.admin;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.admin.AdminOrganizacaoDetalheDTO;
import org.exemplo.bellory.model.dto.admin.AdminOrganizacaoListDTO;
import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.entity.plano.PlanoLimitesBellory;
import org.exemplo.bellory.model.repository.admin.AdminQueryRepository;
import org.exemplo.bellory.model.repository.instance.InstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrganizacaoService {

    private final AdminQueryRepository adminQueryRepository;
    private final InstanceRepository instanceRepository;

    public List<AdminOrganizacaoListDTO> listarOrganizacoes() {
        List<Organizacao> organizacoes = adminQueryRepository.findAllOrganizacoesComPlano();

        return organizacoes.stream().map(org -> {
            Long totalAgendamentos = adminQueryRepository.countAgendamentosByOrganizacao(org.getId());
            Long totalClientes = adminQueryRepository.countClientesByOrganizacao(org.getId());
            Long totalFuncionarios = adminQueryRepository.countFuncionariosByOrganizacao(org.getId());
            Long totalServicos = adminQueryRepository.countServicosByOrganizacao(org.getId());
            List<Instance> instancias = instanceRepository.findByOrganizacaoIdAndDeletadoFalse(org.getId());

            return AdminOrganizacaoListDTO.builder()
                    .id(org.getId())
                    .nomeFantasia(org.getNomeFantasia())
                    .razaoSocial(org.getRazaoSocial())
                    .cnpj(org.getCnpj())
                    .emailPrincipal(org.getEmailPrincipal())
                    .telefone1(org.getTelefone1())
                    .slug(org.getSlug())
                    .ativo(org.getAtivo())
                    .planoNome(org.getPlano() != null ? org.getPlano().getNome() : null)
                    .planoCodigo(org.getPlano() != null ? org.getPlano().getCodigo() : null)
                    .dtCadastro(org.getDtCadastro())
                    .totalAgendamentos(totalAgendamentos)
                    .totalClientes(totalClientes)
                    .totalFuncionarios(totalFuncionarios)
                    .totalServicos(totalServicos)
                    .totalInstancias((long) instancias.size())
                    .build();
        }).collect(Collectors.toList());
    }

    public AdminOrganizacaoDetalheDTO detalharOrganizacao(Long organizacaoId) {
        Organizacao org = adminQueryRepository.findOrganizacaoComDetalhesById(organizacaoId)
                .orElseThrow(() -> new RuntimeException("Organizacao nao encontrada: " + organizacaoId));

        PlanoBellory plano = org.getPlano();
        PlanoLimitesBellory limitesPlano = plano != null ? plano.getLimites() : null;
        PlanoLimitesBellory limitesPersonalizados = org.getLimitesPersonalizados();

        // Metricas
        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime fimMes = LocalDateTime.now();

        AdminOrganizacaoDetalheDTO.MetricasOrganizacao metricas = AdminOrganizacaoDetalheDTO.MetricasOrganizacao.builder()
                .totalAgendamentos(adminQueryRepository.countAgendamentosByOrganizacao(organizacaoId))
                .agendamentosNoMes(adminQueryRepository.countAgendamentosByOrganizacaoNoPeriodo(organizacaoId, inicioMes, fimMes))
                .agendamentosConcluidos(adminQueryRepository.countAgendamentosConcluidosByOrganizacao(organizacaoId))
                .agendamentosCancelados(adminQueryRepository.countAgendamentosCanceladosByOrganizacao(organizacaoId))
                .agendamentosPendentes(adminQueryRepository.countAgendamentosPendentesByOrganizacao(organizacaoId))
                .totalClientes(adminQueryRepository.countClientesByOrganizacao(organizacaoId))
                .clientesAtivos(adminQueryRepository.countClientesAtivosByOrganizacao(organizacaoId))
                .totalFuncionarios(adminQueryRepository.countFuncionariosByOrganizacao(organizacaoId))
                .funcionariosAtivos(adminQueryRepository.countFuncionariosAtivosByOrganizacao(organizacaoId))
                .totalServicos(adminQueryRepository.countServicosByOrganizacao(organizacaoId))
                .servicosAtivos(adminQueryRepository.countServicosAtivosByOrganizacao(organizacaoId))
                .faturamentoTotal(adminQueryRepository.calcularFaturamentoByOrganizacao(organizacaoId))
                .faturamentoMes(adminQueryRepository.calcularFaturamentoByOrganizacaoNoPeriodo(organizacaoId, inicioMes, fimMes))
                .totalCobrancas(adminQueryRepository.countCobrancasByOrganizacao(organizacaoId))
                .cobrancasPagas(adminQueryRepository.countCobrancasPagasByOrganizacao(organizacaoId))
                .cobrancasPendentes(adminQueryRepository.countCobrancasPendentesByOrganizacao(organizacaoId))
                .cobrancasVencidas(adminQueryRepository.countCobrancasVencidasByOrganizacao(organizacaoId))
                .build();

        // Instancias
        List<Instance> instancias = instanceRepository.findByOrganizacaoIdAndDeletadoFalse(organizacaoId);
        List<AdminOrganizacaoDetalheDTO.InstanciaResumoDTO> instanciasList = instancias.stream()
                .map(i -> AdminOrganizacaoDetalheDTO.InstanciaResumoDTO.builder()
                        .id(i.getId())
                        .instanceName(i.getInstanceName())
                        .instanceId(i.getInstanceId())
                        .status(i.getStatus() != null ? i.getStatus().name() : null)
                        .ativo(i.isAtivo())
                        .build())
                .collect(Collectors.toList());

        return AdminOrganizacaoDetalheDTO.builder()
                .id(org.getId())
                .nomeFantasia(org.getNomeFantasia())
                .razaoSocial(org.getRazaoSocial())
                .cnpj(org.getCnpj())
                .emailPrincipal(org.getEmailPrincipal())
                .telefone1(org.getTelefone1())
                .telefone2(org.getTelefone2())
                .whatsapp(org.getWhatsapp())
                .slug(org.getSlug())
                .ativo(org.getAtivo())
                .dtCadastro(org.getDtCadastro())
                .dtAtualizacao(org.getDtAtualizacao())
                .responsavelNome(org.getResponsavel() != null ? org.getResponsavel().getNome() : null)
                .responsavelEmail(org.getResponsavel() != null ? org.getResponsavel().getEmail() : null)
                .responsavelTelefone(org.getResponsavel() != null ? org.getResponsavel().getTelefone() : null)
                .plano(plano != null ? AdminOrganizacaoDetalheDTO.PlanoInfo.builder()
                        .id(plano.getId())
                        .codigo(plano.getCodigo())
                        .nome(plano.getNome())
                        .precoMensal(plano.getPrecoMensal())
                        .precoAnual(plano.getPrecoAnual())
                        .build() : null)
                .limites(limitesPlano != null ? mapLimites(limitesPlano) : null)
                .limitesPersonalizados(limitesPersonalizados != null ? mapLimites(limitesPersonalizados) : null)
                .metricas(metricas)
                .instancias(instanciasList)
                .build();
    }

    private AdminOrganizacaoDetalheDTO.LimitesInfo mapLimites(PlanoLimitesBellory limites) {
        return AdminOrganizacaoDetalheDTO.LimitesInfo.builder()
                .maxAgendamentosMes(limites.getMaxAgendamentosMes())
                .maxUsuarios(limites.getMaxUsuarios())
                .maxClientes(limites.getMaxClientes())
                .maxServicos(limites.getMaxServicos())
                .maxUnidades(limites.getMaxUnidades())
                .permiteAgendamentoOnline(limites.isPermiteAgendamentoOnline())
                .permiteWhatsapp(limites.isPermiteWhatsapp())
                .permiteSite(limites.isPermiteSite())
                .permiteEcommerce(limites.isPermiteEcommerce())
                .permiteRelatoriosAvancados(limites.isPermiteRelatoriosAvancados())
                .permiteApi(limites.isPermiteApi())
                .permiteIntegracaoPersonalizada(limites.isPermiteIntegracaoPersonalizada())
                .suportePrioritario(limites.isSuportePrioritario())
                .suporte24x7(limites.isSuporte24x7())
                .build();
    }
}
