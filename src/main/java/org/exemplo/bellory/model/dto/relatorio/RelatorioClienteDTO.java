package org.exemplo.bellory.model.dto.relatorio;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioClienteDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String periodoConsulta;

    // Cadastros
    private CadastrosResumoDTO cadastros;

    // Frequencia
    private FrequenciaResumoDTO frequencia;

    // Valor (LTV)
    private List<ClienteValorDTO> rankingValor;
    private BigDecimal ltvMedio;
    private BigDecimal ticketMedio;

    // Evolucao de cadastros
    private List<CadastroPeriodoDTO> evolucaoCadastros;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CadastrosResumoDTO {
        private Long totalClientes;
        private Long clientesAtivos;
        private Long clientesInativos;
        private Long novosCadastros;
        private Long cadastrosCompletos;
        private Long cadastrosIncompletos;
        private Double taxaCadastroCompleto;
        private Double crescimentoPercentual;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FrequenciaResumoDTO {
        private Long clientesFrequentes; // 3+ agendamentos no periodo
        private Long clientesOcasionais; // 1-2 agendamentos
        private Long clientesInativos; // sem agendamentos ha mais de 90 dias
        private Double taxaRetencao;
        private Long clientesRecorrentes;
        private List<ClienteFrequenciaDTO> detalhamento;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClienteFrequenciaDTO {
        private Long clienteId;
        private String nome;
        private String telefone;
        private Long totalAgendamentos;
        private LocalDateTime ultimoAgendamento;
        private Long diasSemAgendar;
        private String classificacao; // FREQUENTE, OCASIONAL, INATIVO
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClienteValorDTO {
        private Long clienteId;
        private String nome;
        private String telefone;
        private BigDecimal valorTotal;
        private Long totalAgendamentos;
        private BigDecimal ticketMedio;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CadastroPeriodoDTO {
        private String periodo;
        private Long novosCadastros;
        private Long acumulado;
    }
}
