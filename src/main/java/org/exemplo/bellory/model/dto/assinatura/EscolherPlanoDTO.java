package org.exemplo.bellory.model.dto.assinatura;

import jakarta.validation.Valid;
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

    private String codigoCupom;

    // Dados do cartao de credito (obrigatorio quando formaPagamento = CARTAO_CREDITO)
    // Esses dados sao enviados diretamente ao Asaas e NUNCA armazenados no banco
    @Valid
    private CreditCardDTO creditCard;

    // Token do cartao Asaas (alternativa ao envio dos dados completos)
    // Gerado via Asaas.js no frontend
    private String creditCardToken;
}
