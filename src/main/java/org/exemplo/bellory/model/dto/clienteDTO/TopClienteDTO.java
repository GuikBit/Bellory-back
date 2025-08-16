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
public class TopClienteDTO {
    private Long id;
    private String nomeCompleto;
    private String telefone;
    private String email;
    private BigDecimal valorTotalGasto;
    private Long totalAgendamentos;
    private Long totalCompras;
    private LocalDateTime ultimaVisita;
    private String classificacao; // "VIP", "PREMIUM", "REGULAR"
    private int posicaoRanking;
}
