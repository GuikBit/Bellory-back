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
public class RelatorioFuncionarioDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String periodoConsulta;

    // Resumo geral
    private Long totalFuncionariosAtivos;

    // Ranking de produtividade
    private List<ProdutividadeDTO> rankingProdutividade;

    // Ocupacao de agenda
    private List<OcupacaoAgendaDTO> ocupacaoAgenda;

    // Comissoes
    private List<ComissaoDTO> comissoes;
    private BigDecimal totalComissoes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutividadeDTO {
        private Long funcionarioId;
        private String nome;
        private String cargo;
        private Long totalAtendimentos;
        private Long atendimentosConcluidos;
        private Long atendimentosCancelados;
        private Long naoCompareceu;
        private Double taxaConclusao;
        private Double taxaCancelamento;
        private BigDecimal faturamentoGerado;
        private BigDecimal ticketMedio;
        private Double mediaAtendimentosDia;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OcupacaoAgendaDTO {
        private Long funcionarioId;
        private String nome;
        private Double horasDisponiveis;
        private Double horasOcupadas;
        private Double taxaOcupacao;
        private Map<String, Double> ocupacaoPorDiaSemana;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComissaoDTO {
        private Long funcionarioId;
        private String nome;
        private String tipoComissao; // percentual ou fixo
        private String valorComissao; // valor configurado
        private BigDecimal faturamentoGerado;
        private BigDecimal comissaoCalculada;
        private Long totalAtendimentos;
    }
}
