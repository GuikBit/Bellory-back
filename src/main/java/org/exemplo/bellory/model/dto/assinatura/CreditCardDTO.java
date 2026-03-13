package org.exemplo.bellory.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditCardDTO {

    @NotBlank(message = "Nome do portador e obrigatorio")
    private String holderName;

    @NotBlank(message = "Numero do cartao e obrigatorio")
    private String number;

    @NotBlank(message = "Mes de validade e obrigatorio")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Mes invalido (01-12)")
    private String expiryMonth;

    @NotBlank(message = "Ano de validade e obrigatorio")
    @Pattern(regexp = "^\\d{4}$", message = "Ano invalido (4 digitos)")
    private String expiryYear;

    @NotBlank(message = "CVV e obrigatorio")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV invalido (3 ou 4 digitos)")
    private String ccv;
}
