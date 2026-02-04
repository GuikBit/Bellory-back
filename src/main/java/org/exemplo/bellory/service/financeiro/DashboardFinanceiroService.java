package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.DashboardFinanceiroDTO;
import org.exemplo.bellory.model.entity.financeiro.LancamentoFinanceiro;
import org.exemplo.bellory.model.repository.financeiro.ContaBancariaRepository;
import org.exemplo.bellory.model.repository.financeiro.ContaPagarRepository;
import org.exemplo.bellory.model.repository.financeiro.ContaReceberRepository;
import org.exemplo.bellory.model.repository.financeiro.LancamentoFinanceiroRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardFinanceiroService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;

    public DashboardFinanceiroDTO gerarDashboard() {
        Long orgId = getOrganizacaoId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = YearMonth.from(hoje).atDay(1);
        LocalDate fimMes = YearMonth.from(hoje).atEndOfMonth();

        // Resumo do dia
        BigDecimal receitasHoje = lancamentoRepository.sumReceitasByPeriodo(orgId, hoje, hoje);
        BigDecimal despesasHoje = lancamentoRepository.sumDespesasByPeriodo(orgId, hoje, hoje);

        // Resumo do mês
        BigDecimal receitasMes = lancamentoRepository.sumReceitasByPeriodo(orgId, inicioMes, fimMes);
        BigDecimal despesasMes = lancamentoRepository.sumDespesasByPeriodo(orgId, inicioMes, fimMes);

        // Saldo total contas
        BigDecimal saldoTotalContas = contaBancariaRepository.sumSaldoAtualByOrganizacao(orgId);

        // Contas vencidas
        int cpVencidas = contaPagarRepository.countVencidasByOrganizacao(orgId, hoje);
        BigDecimal valorCPVencidas = contaPagarRepository.sumValorVencidasByOrganizacao(orgId, hoje);
        int crVencidas = contaReceberRepository.countVencidasByOrganizacao(orgId, hoje);
        BigDecimal valorCRVencidas = contaReceberRepository.sumValorVencidasByOrganizacao(orgId, hoje);

        // Contas a vencer (próximos 7 dias)
        LocalDate seteDias = hoje.plusDays(7);
        int cpAVencer = contaPagarRepository.countAVencerByOrganizacao(orgId, hoje, seteDias);
        BigDecimal valorCPAVencer = contaPagarRepository.sumValorAVencerByOrganizacao(orgId, hoje, seteDias);
        int crAVencer = contaReceberRepository.countAVencerByOrganizacao(orgId, hoje, seteDias);
        BigDecimal valorCRAVencer = contaReceberRepository.sumValorAVencerByOrganizacao(orgId, hoje, seteDias);

        // Evolução últimos 12 meses
        List<DashboardFinanceiroDTO.EvolucaoMensalDTO> evolucao = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth mesAno = YearMonth.from(hoje.minusMonths(i));
            LocalDate inicio = mesAno.atDay(1);
            LocalDate fim = mesAno.atEndOfMonth();

            BigDecimal rec = lancamentoRepository.sumReceitasByPeriodo(orgId, inicio, fim);
            BigDecimal desp = lancamentoRepository.sumDespesasByPeriodo(orgId, inicio, fim);

            evolucao.add(DashboardFinanceiroDTO.EvolucaoMensalDTO.builder()
                    .mesAno(mesAno.getMonth().name() + "/" + mesAno.getYear())
                    .receitas(rec)
                    .despesas(desp)
                    .resultado(rec.subtract(desp))
                    .build());
        }

        // Top categorias do mês (despesas e receitas)
        List<LancamentoFinanceiro> lancamentosMes = lancamentoRepository.findEfetivadosByPeriodo(orgId, inicioMes, fimMes);

        Map<Long, BigDecimal> despesasPorCat = new HashMap<>();
        Map<Long, BigDecimal> receitasPorCat = new HashMap<>();
        Map<Long, String> nomesCat = new HashMap<>();

        for (LancamentoFinanceiro l : lancamentosMes) {
            if (l.getCategoriaFinanceira() != null) {
                Long catId = l.getCategoriaFinanceira().getId();
                nomesCat.putIfAbsent(catId, l.getCategoriaFinanceira().getNome());

                if (l.isDespesa()) {
                    despesasPorCat.merge(catId, l.getValor(), BigDecimal::add);
                } else if (l.isReceita()) {
                    receitasPorCat.merge(catId, l.getValor(), BigDecimal::add);
                }
            }
        }

        BigDecimal finalDespesasMes = despesasMes;
        List<DashboardFinanceiroDTO.TopCategoriaDTO> topDespesas = despesasPorCat.entrySet().stream()
                .map(e -> DashboardFinanceiroDTO.TopCategoriaDTO.builder()
                        .categoriaId(e.getKey())
                        .categoriaNome(nomesCat.get(e.getKey()))
                        .valor(e.getValue())
                        .percentual(finalDespesasMes.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(finalDespesasMes, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> b.getValor().compareTo(a.getValor()))
                .limit(10)
                .collect(Collectors.toList());

        BigDecimal finalReceitasMes = receitasMes;
        List<DashboardFinanceiroDTO.TopCategoriaDTO> topReceitas = receitasPorCat.entrySet().stream()
                .map(e -> DashboardFinanceiroDTO.TopCategoriaDTO.builder()
                        .categoriaId(e.getKey())
                        .categoriaNome(nomesCat.get(e.getKey()))
                        .valor(e.getValue())
                        .percentual(finalReceitasMes.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(finalReceitasMes, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> b.getValor().compareTo(a.getValor()))
                .limit(10)
                .collect(Collectors.toList());

        return DashboardFinanceiroDTO.builder()
                .receitasHoje(receitasHoje)
                .despesasHoje(despesasHoje)
                .saldoHoje(receitasHoje.subtract(despesasHoje))
                .receitasMes(receitasMes)
                .despesasMes(despesasMes)
                .saldoMes(receitasMes.subtract(despesasMes))
                .saldoTotalContas(saldoTotalContas)
                .contasPagarVencidas(cpVencidas)
                .valorContasPagarVencidas(valorCPVencidas)
                .contasReceberVencidas(crVencidas)
                .valorContasReceberVencidas(valorCRVencidas)
                .contasPagarAVencer(cpAVencer)
                .valorContasPagarAVencer(valorCPAVencer)
                .contasReceberAVencer(crAVencer)
                .valorContasReceberAVencer(valorCRAVencer)
                .evolucao(evolucao)
                .topCategoriasDespesas(topDespesas)
                .topCategoriasReceitas(topReceitas)
                .build();
    }

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new IllegalStateException("Contexto de organização não encontrado.");
        }
        return orgId;
    }
}
