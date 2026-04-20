package org.exemplo.bellory.model.dto.home;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumoHomeDTO {
    private Long agendamentosHoje;
    private BigDecimal faturamentoMes;
    private Long clientesAtivos;
    private Double taxaOcupacao;
    private ProximoAgendamentoDTO proximoAgendamento;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProximoAgendamentoDTO {
        private Long id;
        private String clienteNome;
        private String servicoNome;
        private String funcionarioNome;
        private LocalDateTime dtAgendamento;
    }
}
