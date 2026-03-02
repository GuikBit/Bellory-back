package org.exemplo.bellory.model.dto.assinatura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscolherPlanoDTO {
    @NotBlank
    private String planoCodigo;

    @NotNull
    private String cicloCobranca;

    @NotNull
    private String formaPagamento;
}
