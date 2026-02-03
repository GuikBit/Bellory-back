package org.exemplo.bellory.model.dto.relatorio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioServicoDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String periodoConsulta;

    // Ranking de servicos
    private List<ServicoDetalhadoDTO> rankingServicos;

    // Desempenho por categoria
    private List<CategoriaDesempenhoDTO> desempenhoPorCategoria;

    // Totais
    private Long totalServicosRealizados;
    private BigDecimal faturamentoTotalServicos;
    private BigDecimal ticketMedioServico;

    // Evolucao
    private List<ServicoPeriodoDTO> evolucao;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServicoDetalhadoDTO {
        private Long id;
        private String nome;
        private String categoria;
        private Long quantidadeRealizada;
        private BigDecimal faturamento;
        private BigDecimal precoMedio;
        private Double percentualDoTotal;
        private Integer tempoEstimadoMinutos;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoriaDesempenhoDTO {
        private String categoria;
        private Long totalServicos;
        private BigDecimal faturamento;
        private Double percentualFaturamento;
        private List<String> topServicos;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServicoPeriodoDTO {
        private String periodo;
        private Long quantidade;
    }
}
