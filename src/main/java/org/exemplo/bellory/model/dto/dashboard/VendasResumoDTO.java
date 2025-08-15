package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendasResumoDTO {
    private Long totalVendas;
    private BigDecimal valorTotalVendas;
    private Long vendasHoje;
    private BigDecimal valorVendasHoje;
    private Long pedidosPendentes;
    private Long pedidosEntregues;
    private Long pedidosCancelados;
    private Double ticketMedioVenda;
    private List<ServicoTopDTO> servicosMaisVendidos;
    private Map<String, Long> vendasPorCategoria;
    private Double crescimentoVendas;
}
