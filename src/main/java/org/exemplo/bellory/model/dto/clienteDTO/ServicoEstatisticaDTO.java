package org.exemplo.bellory.model.dto.clienteDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicoEstatisticaDTO {
    private Long servicoId;
    private String nomeServico;
    private Long quantidadeUtilizada;
    private BigDecimal valorTotalGasto;
    private BigDecimal precoMedio;
    private String frequencia; // "Muito Alto", "Alto", "Médio", "Baixo"
}
