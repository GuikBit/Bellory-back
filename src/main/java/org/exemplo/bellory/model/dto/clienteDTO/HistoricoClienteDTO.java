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
public class HistoricoClienteDTO {
    private Long transacaoId;
    private String tipoTransacao; // "AGENDAMENTO" ou "COMPRA"
    private String descricao;
    private BigDecimal valor;
    private String status;
    private LocalDateTime dataTransacao;
    private String detalhes; // Serviços ou produtos
}

