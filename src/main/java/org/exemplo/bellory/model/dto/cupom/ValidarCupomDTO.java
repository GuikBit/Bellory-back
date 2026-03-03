package org.exemplo.bellory.model.dto.cupom;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCupomDTO {

    @NotBlank(message = "Codigo do cupom e obrigatorio")
    private String codigoCupom;

    @NotBlank(message = "Codigo do plano e obrigatorio")
    private String planoCodigo;

    @NotBlank(message = "Ciclo de cobranca e obrigatorio")
    private String cicloCobranca;
}
