package org.exemplo.bellory.model.dto.clienteDTO;

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
public class ResumoFinanceiroClienteDTO {
    private Long clienteId;
    private String nomeCliente;

    // Totais gerais
    private BigDecimal valorTotalGasto;
    private BigDecimal valorTotalServicos;
    private BigDecimal valorTotalProdutos;

    // Contadores
    private Long totalAgendamentos;
    private LocalDateTime ultimoAgendamento;
    private Long totalCompras;
    private Long totalPagamentos;

    // Status de pagamento
    private BigDecimal valorPago;
    private BigDecimal valorPendente;
    private BigDecimal valorVencido;

    // Ticket médio
    private BigDecimal ticketMedioServicos;
    private BigDecimal ticketMedioProdutos;

    // Período de análise
    private String periodoAnalise;
}
