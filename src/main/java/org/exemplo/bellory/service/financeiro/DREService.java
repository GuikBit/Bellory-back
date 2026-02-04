package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.DREDTO;
import org.exemplo.bellory.model.entity.financeiro.CategoriaFinanceira;
import org.exemplo.bellory.model.entity.financeiro.LancamentoFinanceiro;
import org.exemplo.bellory.model.repository.financeiro.CategoriaFinanceiraRepository;
import org.exemplo.bellory.model.repository.financeiro.LancamentoFinanceiroRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DREService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;

    public DREDTO gerarDRE(LocalDate dataInicio, LocalDate dataFim) {
        Long orgId = getOrganizacaoId();

        List<LancamentoFinanceiro> lancamentos = lancamentoRepository.findEfetivadosByPeriodo(orgId, dataInicio, dataFim);

        // Separar receitas e despesas
        BigDecimal receitaBruta = BigDecimal.ZERO;
        BigDecimal descontosReceita = BigDecimal.ZERO;
        BigDecimal totalDespesas = BigDecimal.ZERO;

        Map<Long, BigDecimal> receitasPorCategoria = new HashMap<>();
        Map<Long, BigDecimal> despesasPorCategoria = new HashMap<>();
        Map<Long, String> nomesCategoria = new HashMap<>();

        for (LancamentoFinanceiro l : lancamentos) {
            if (l.isReceita()) {
                receitaBruta = receitaBruta.add(l.getValor());
                if (l.getCategoriaFinanceira() != null) {
                    Long catId = l.getCategoriaFinanceira().getId();
                    receitasPorCategoria.merge(catId, l.getValor(), BigDecimal::add);
                    nomesCategoria.putIfAbsent(catId, l.getCategoriaFinanceira().getNome());
                }
            } else if (l.isDespesa()) {
                totalDespesas = totalDespesas.add(l.getValor());
                if (l.getCategoriaFinanceira() != null) {
                    Long catId = l.getCategoriaFinanceira().getId();
                    despesasPorCategoria.merge(catId, l.getValor(), BigDecimal::add);
                    nomesCategoria.putIfAbsent(catId, l.getCategoriaFinanceira().getNome());
                }
            }
        }

        BigDecimal receitaLiquida = receitaBruta.subtract(descontosReceita);
        BigDecimal resultadoOperacional = receitaLiquida.subtract(totalDespesas);
        BigDecimal resultadoLiquido = resultadoOperacional;

        // Margens
        BigDecimal margemOperacional = receitaLiquida.compareTo(BigDecimal.ZERO) > 0
                ? resultadoOperacional.multiply(BigDecimal.valueOf(100)).divide(receitaLiquida, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal margemLiquida = receitaLiquida.compareTo(BigDecimal.ZERO) > 0
                ? resultadoLiquido.multiply(BigDecimal.valueOf(100)).divide(receitaLiquida, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Linhas de receita
        BigDecimal finalReceitaBruta = receitaBruta;
        List<DREDTO.DRELinhaDTO> linhasReceita = receitasPorCategoria.entrySet().stream()
                .map(e -> DREDTO.DRELinhaDTO.builder()
                        .categoriaId(e.getKey())
                        .categoriaNome(nomesCategoria.get(e.getKey()))
                        .tipo("RECEITA")
                        .valor(e.getValue())
                        .percentual(finalReceitaBruta.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(finalReceitaBruta, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> b.getValor().compareTo(a.getValor()))
                .collect(Collectors.toList());

        // Linhas de despesa
        BigDecimal finalTotalDespesas = totalDespesas;
        List<DREDTO.DRELinhaDTO> linhasDespesa = despesasPorCategoria.entrySet().stream()
                .map(e -> DREDTO.DRELinhaDTO.builder()
                        .categoriaId(e.getKey())
                        .categoriaNome(nomesCategoria.get(e.getKey()))
                        .tipo("DESPESA")
                        .valor(e.getValue())
                        .percentual(finalTotalDespesas.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(finalTotalDespesas, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> b.getValor().compareTo(a.getValor()))
                .collect(Collectors.toList());

        return DREDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .receitaBruta(receitaBruta)
                .descontosReceita(descontosReceita)
                .receitaLiquida(receitaLiquida)
                .totalDespesasOperacionais(totalDespesas)
                .totalDespesasAdministrativas(BigDecimal.ZERO)
                .totalDespesasComPessoal(BigDecimal.ZERO)
                .totalOutrasDespesas(BigDecimal.ZERO)
                .totalDespesas(totalDespesas)
                .resultadoOperacional(resultadoOperacional)
                .resultadoLiquido(resultadoLiquido)
                .margemOperacional(margemOperacional)
                .margemLiquida(margemLiquida)
                .receitas(linhasReceita)
                .despesas(linhasDespesa)
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
