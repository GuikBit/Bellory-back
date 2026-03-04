package org.exemplo.bellory.model.dto.cupom;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CupomValidacaoResponseDTO {
    private boolean valido;
    private String mensagem;
    private String tipoDesconto;
    private BigDecimal valorDesconto;
    private BigDecimal valorOriginal;
    private BigDecimal valorComDesconto;
}
