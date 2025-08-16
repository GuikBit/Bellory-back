package org.exemplo.bellory.model.dto.compra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CobrancaDTO {
    private Long id;
    private Long clienteId;
    private String nomeCliente;
    private Long agendamentoId;
    private Long compraId;
    private BigDecimal valor;
    private String statusCobranca;
    private LocalDate dtVencimento;
    private LocalDateTime dtCriacao;
    private String tipoTransacao; // "AGENDAMENTO" ou "COMPRA"
    private String descricaoServicos; // Para agendamentos
}
