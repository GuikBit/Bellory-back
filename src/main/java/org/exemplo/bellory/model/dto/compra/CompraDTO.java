package org.exemplo.bellory.model.dto.compra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraDTO {
    private Long id;
    private Long clienteId;
    private String nomeCliente;
    private BigDecimal valorTotal;
    private String statusCompra;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
    private List<ItemCompraDTO> itens;
}
