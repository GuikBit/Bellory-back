package org.exemplo.bellory.model.dto.relatorio;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioAgendamentoDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String periodoConsulta;

    // Totais gerais
    private Long totalAgendamentos;
    private Long agendamentosConcluidos;
    private Long agendamentosCancelados;
    private Long agendamentosNaoCompareceu;
    private Long agendamentosReagendados;
    private Long agendamentosPendentes;

    // Taxas
    private Double taxaConclusao;
    private Double taxaCancelamento;
    private Double taxaNoShow;
    private Double taxaReagendamento;

    // Distribuicao por status
    private Map<String, Long> porStatus;

    // Distribuicao por dia da semana
    private Map<String, Long> porDiaSemana;

    // Distribuicao por horario (hora do dia)
    private Map<String, Long> porHorario;

    // Evolucao no periodo
    private List<AgendamentoPeriodoDTO> evolucao;

    // Top servicos
    private List<ServicoRankingDTO> servicosMaisAgendados;

    // Top funcionarios
    private List<FuncionarioRankingDTO> funcionariosMaisAtendimentos;

    // Comparativo
    private Long totalPeriodoAnterior;
    private Double percentualVariacao;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgendamentoPeriodoDTO {
        private String periodo;
        private Long total;
        private Long concluidos;
        private Long cancelados;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServicoRankingDTO {
        private Long id;
        private String nome;
        private Long quantidade;
        private Double percentualTotal;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FuncionarioRankingDTO {
        private Long id;
        private String nome;
        private Long totalAtendimentos;
        private Long concluidos;
        private Long cancelados;
        private Double taxaConclusao;
    }
}
