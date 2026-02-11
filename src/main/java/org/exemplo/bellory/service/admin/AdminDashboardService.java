package org.exemplo.bellory.service.admin;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.admin.AdminDashboardDTO;
import org.exemplo.bellory.model.repository.admin.AdminQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminQueryRepository adminQueryRepository;

    public AdminDashboardDTO getDashboard() {
        Long totalOrganizacoes = adminQueryRepository.count();
        Long organizacoesAtivas = adminQueryRepository.countOrganizacoesAtivas();
        Long organizacoesInativas = adminQueryRepository.countOrganizacoesInativas();

        Long totalAgendamentos = adminQueryRepository.countTotalAgendamentos();
        Long totalClientes = adminQueryRepository.countTotalClientes();
        Long totalFuncionarios = adminQueryRepository.countTotalFuncionarios();
        Long totalServicos = adminQueryRepository.countTotalServicos();
        Long totalInstancias = adminQueryRepository.countTotalInstancias();
        Long instanciasAtivas = adminQueryRepository.countInstanciasAtivas();

        BigDecimal faturamentoTotal = adminQueryRepository.calcularFaturamentoTotal();
        Long totalCobrancas = adminQueryRepository.countTotalCobrancas();
        Long cobrancasPendentes = adminQueryRepository.countCobrancasPendentes();
        Long cobrancasPagas = adminQueryRepository.countCobrancasPagas();

        // Distribuicao de planos
        List<Object[]> planosDist = adminQueryRepository.contarOrganizacoesPorPlano();
        AdminDashboardDTO.DistribuicaoPlanos distribuicao = buildDistribuicaoPlanos(planosDist);

        return AdminDashboardDTO.builder()
                .totalOrganizacoes(totalOrganizacoes)
                .organizacoesAtivas(organizacoesAtivas)
                .organizacoesInativas(organizacoesInativas)
                .totalAgendamentos(totalAgendamentos)
                .totalClientes(totalClientes)
                .totalFuncionarios(totalFuncionarios)
                .totalServicos(totalServicos)
                .totalInstancias(totalInstancias)
                .instanciasConectadas(instanciasAtivas)
                .instanciasDesconectadas(totalInstancias - instanciasAtivas)
                .faturamentoTotal(faturamentoTotal)
                .totalCobrancas(totalCobrancas)
                .cobrancasPendentes(cobrancasPendentes)
                .cobrancasPagas(cobrancasPagas)
                .distribuicaoPlanos(distribuicao)
                .build();
    }

    private AdminDashboardDTO.DistribuicaoPlanos buildDistribuicaoPlanos(List<Object[]> dados) {
        long gratuito = 0, basico = 0, plus = 0, premium = 0;
        for (Object[] row : dados) {
            String codigo = (String) row[1];
            Long count = (Long) row[7];
            switch (codigo != null ? codigo.toLowerCase() : "") {
                case "gratuito" -> gratuito = count;
                case "basico" -> basico = count;
                case "plus" -> plus = count;
                case "premium" -> premium = count;
            }
        }
        return AdminDashboardDTO.DistribuicaoPlanos.builder()
                .gratuito(gratuito)
                .basico(basico)
                .plus(plus)
                .premium(premium)
                .build();
    }
}
