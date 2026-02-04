package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.FluxoCaixaDTO;
import org.exemplo.bellory.model.entity.financeiro.LancamentoFinanceiro;
import org.exemplo.bellory.model.repository.financeiro.ContaBancariaRepository;
import org.exemplo.bellory.model.repository.financeiro.LancamentoFinanceiroRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FluxoCaixaService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final ContaBancariaRepository contaBancariaRepository;

    public FluxoCaixaDTO gerarFluxoCaixa(LocalDate dataInicio, LocalDate dataFim) {
        Long orgId = getOrganizacaoId();

        List<LancamentoFinanceiro> lancamentos = lancamentoRepository.findEfetivadosByPeriodo(orgId, dataInicio, dataFim);

        BigDecimal saldoInicial = contaBancariaRepository.sumSaldoAtualByOrganizacao(orgId);
        // Descontar lançamentos do período para obter saldo no início
        BigDecimal receitasPeriodo = BigDecimal.ZERO;
        BigDecimal despesasPeriodo = BigDecimal.ZERO;

        // Agrupar por dia
        Map<LocalDate, List<LancamentoFinanceiro>> porDia = lancamentos.stream()
                .collect(Collectors.groupingBy(LancamentoFinanceiro::getDtLancamento, TreeMap::new, Collectors.toList()));

        // Agrupar por categoria
        Map<Long, BigDecimal> receitasPorCategoria = new HashMap<>();
        Map<Long, BigDecimal> despesasPorCategoria = new HashMap<>();
        Map<Long, String> nomesCategoria = new HashMap<>();

        for (LancamentoFinanceiro l : lancamentos) {
            if (l.isReceita()) {
                receitasPeriodo = receitasPeriodo.add(l.getValor());
                if (l.getCategoriaFinanceira() != null) {
                    Long catId = l.getCategoriaFinanceira().getId();
                    receitasPorCategoria.merge(catId, l.getValor(), BigDecimal::add);
                    nomesCategoria.putIfAbsent(catId, l.getCategoriaFinanceira().getNome());
                }
            } else if (l.isDespesa()) {
                despesasPeriodo = despesasPeriodo.add(l.getValor());
                if (l.getCategoriaFinanceira() != null) {
                    Long catId = l.getCategoriaFinanceira().getId();
                    despesasPorCategoria.merge(catId, l.getValor(), BigDecimal::add);
                    nomesCategoria.putIfAbsent(catId, l.getCategoriaFinanceira().getNome());
                }
            }
        }

        // Calcular saldo inicial real (saldo atual - receitas + despesas do período)
        BigDecimal saldoInicialReal = saldoInicial.subtract(receitasPeriodo).add(despesasPeriodo);

        // Fluxo diário
        List<FluxoCaixaDTO.FluxoCaixaDiaDTO> fluxoDiario = new ArrayList<>();
        BigDecimal saldoAcumulado = saldoInicialReal;

        LocalDate dataAtual = dataInicio;
        while (!dataAtual.isAfter(dataFim)) {
            List<LancamentoFinanceiro> doDia = porDia.getOrDefault(dataAtual, Collections.emptyList());

            BigDecimal receitasDia = BigDecimal.ZERO;
            BigDecimal despesasDia = BigDecimal.ZERO;

            for (LancamentoFinanceiro l : doDia) {
                if (l.isReceita()) {
                    receitasDia = receitasDia.add(l.getValor());
                } else if (l.isDespesa()) {
                    despesasDia = despesasDia.add(l.getValor());
                }
            }

            BigDecimal saldoDia = receitasDia.subtract(despesasDia);
            saldoAcumulado = saldoAcumulado.add(saldoDia);

            fluxoDiario.add(FluxoCaixaDTO.FluxoCaixaDiaDTO.builder()
                    .data(dataAtual)
                    .receitas(receitasDia)
                    .despesas(despesasDia)
                    .saldo(saldoDia)
                    .saldoAcumulado(saldoAcumulado)
                    .build());

            dataAtual = dataAtual.plusDays(1);
        }

        // Categorias de receita
        BigDecimal finalReceitasPeriodo = receitasPeriodo;
        List<FluxoCaixaDTO.FluxoCaixaCategoriaDTO> receitasCat = receitasPorCategoria.entrySet().stream()
                .map(e -> FluxoCaixaDTO.FluxoCaixaCategoriaDTO.builder()
                        .categoriaId(e.getKey())
                        .categoriaNome(nomesCategoria.get(e.getKey()))
                        .valor(e.getValue())
                        .percentual(finalReceitasPeriodo.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(finalReceitasPeriodo, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> b.getValor().compareTo(a.getValor()))
                .collect(Collectors.toList());

        // Categorias de despesa
        BigDecimal finalDespesasPeriodo = despesasPeriodo;
        List<FluxoCaixaDTO.FluxoCaixaCategoriaDTO> despesasCat = despesasPorCategoria.entrySet().stream()
                .map(e -> FluxoCaixaDTO.FluxoCaixaCategoriaDTO.builder()
                        .categoriaId(e.getKey())
                        .categoriaNome(nomesCategoria.get(e.getKey()))
                        .valor(e.getValue())
                        .percentual(finalDespesasPeriodo.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(finalDespesasPeriodo, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> b.getValor().compareTo(a.getValor()))
                .collect(Collectors.toList());

        return FluxoCaixaDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .saldoInicial(saldoInicialReal)
                .totalReceitas(receitasPeriodo)
                .totalDespesas(despesasPeriodo)
                .saldoFinal(saldoAcumulado)
                .saldoPrevisto(saldoAcumulado)
                .fluxoDiario(fluxoDiario)
                .receitasPorCategoria(receitasCat)
                .despesasPorCategoria(despesasCat)
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
