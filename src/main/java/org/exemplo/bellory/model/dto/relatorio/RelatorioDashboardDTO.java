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
public class RelatorioDashboardDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String periodoConsulta;

    // KPIs principais
    private KpisDTO kpis;

    // Comparativo com periodo anterior
    private ComparativoDTO comparativo;

    // Indicadores de saude do negocio
    private SaudeNegocioDTO saudeNegocio;

    // Rankings
    private RankingsDTO rankings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpisDTO {
        private BigDecimal faturamentoTotal;
        private Long totalAgendamentos;
        private Long novosClientes;
        private Double taxaOcupacao;
        private BigDecimal ticketMedio;
        private Double taxaConclusaoAgendamentos;
        private Double taxaNoShow;
        private Long totalNotificacoesEnviadas;
        private Double taxaConfirmacaoNotificacoes;
        private BigDecimal valorPendente;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComparativoDTO {
        private String periodoAnterior;
        private BigDecimal faturamentoAnterior;
        private Double variacaoFaturamento;
        private Long agendamentosAnterior;
        private Double variacaoAgendamentos;
        private Long novosClientesAnterior;
        private Double variacaoNovosClientes;
        private BigDecimal ticketMedioAnterior;
        private Double variacaoTicketMedio;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaudeNegocioDTO {
        private Double taxaRetencaoClientes;
        private Double taxaInadimplencia;
        private Double taxaCancelamento;
        private Double taxaFalhaNotificacoes;
        private Long instanciasAtivas;
        private Long instanciasDesconectadas;
        private Long produtosEstoqueBaixo;
        private BigDecimal receitaPrevista;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RankingsDTO {
        private List<Map<String, Object>> topServicos;
        private List<Map<String, Object>> topFuncionarios;
        private List<Map<String, Object>> topClientes;
    }
}
