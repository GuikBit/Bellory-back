package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CobrancaDTO {
    private Long id;
    private BigDecimal valor;
    private Status status;
    // Adicione apenas os campos da Cobranca que você quer expor na API

    public CobrancaDTO(Cobranca cobranca) {
        if (cobranca != null) {
            this.id = cobranca.getId();
            this.valor = cobranca.getValor();
            this.status = cobranca.getStatusCobranca();
        }
    }
}
