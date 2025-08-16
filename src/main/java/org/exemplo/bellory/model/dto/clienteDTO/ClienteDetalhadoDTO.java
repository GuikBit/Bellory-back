package org.exemplo.bellory.model.dto.clienteDTO;

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
public class ClienteDetalhadoDTO {
    private Long id;
    private String nomeCompleto;
    private String email;
    private String telefone;
    private LocalDate dataNascimento;
    private String role;
    private boolean ativo;
    private LocalDateTime dtCriacao;

    // Estatísticas resumidas
    private Long totalAgendamentos;
    private Long totalCompras;
    private BigDecimal valorTotalGasto;
    private LocalDateTime ultimoAgendamento;
    private LocalDateTime ultimaCompra;
    private Long agendamentosPendentes;
    private Long cobrancasPendentes;
}
