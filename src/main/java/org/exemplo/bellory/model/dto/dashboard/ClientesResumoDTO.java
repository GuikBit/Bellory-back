package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientesResumoDTO {
    private Long totalClientes;
    private Long clientesAtivos;
    private Long clientesInativos;
    private Long novosClientesHoje;
    private Long novosClientesEsteMes;
    private Long clientesRecorrentes;
    private Double taxaRetencao;
    private Double ticketMedioCliente;
    private List<ClienteTopDTO> topClientes;
    private Long clientesAniversarioHoje;
    private Long clientesAniversarioEstaSemana;
}
