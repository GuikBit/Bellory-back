package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CobrancaDTO {
    private Long id;
    private BigDecimal valor;
    private Status status;
    private Long clienteId;
    private String nomeCliente;
    private Long agendamentoId;
    private Long compraId;
    private String statusCobranca;
    private LocalDate dtVencimento;
    private LocalDateTime dtCriacao;
    private String tipoTransacao;
    private String descricaoServicos;
    // Adicione apenas os campos da Cobranca que você quer expor na API

    public CobrancaDTO(Cobranca cobranca) {
        if (cobranca != null) {
            this.id = cobranca.getId();
            this.valor = cobranca.getValor();
            this.status = cobranca.getStatusCobranca();
        }
    }
}
