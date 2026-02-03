package org.exemplo.bellory.model.dto.relatorio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioCobrancaDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String periodoConsulta;

    // Totais
    private Long totalCobrancas;
    private BigDecimal valorTotal;
    private BigDecimal valorRecebido;
    private BigDecimal valorPendente;
    private BigDecimal valorVencido;
    private BigDecimal valorCancelado;

    // Taxas
    private Double taxaRecebimento;
    private Double taxaInadimplencia;

    // Por status
    private Map<String, Long> quantidadePorStatus;
    private Map<String, BigDecimal> valorPorStatus;

    // Por tipo
    private Map<String, Long> quantidadePorTipo;
    private Map<String, BigDecimal> valorPorTipo;

    // Sinais
    private SinaisResumoDTO sinais;

    // Cobrancas vencidas detalhadas
    private List<CobrancaVencidaDTO> cobrancasVencidas;

    // Evolucao
    private List<CobrancaPeriodoDTO> evolucao;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SinaisResumoDTO {
        private Long totalSinais;
        private BigDecimal valorTotalSinais;
        private Long sinaisPagos;
        private BigDecimal valorSinaisPagos;
        private Long sinaisPendentes;
        private BigDecimal valorSinaisPendentes;
        private Double percentualMedioSinal;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CobrancaVencidaDTO {
        private Long id;
        private String numeroCobranca;
        private String clienteNome;
        private BigDecimal valor;
        private BigDecimal valorPendente;
        private LocalDate dtVencimento;
        private Long diasAtraso;
        private String tipo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CobrancaPeriodoDTO {
        private String periodo;
        private BigDecimal valorGerado;
        private BigDecimal valorRecebido;
        private Long quantidade;
    }
}
