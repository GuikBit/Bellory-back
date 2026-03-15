package org.exemplo.bellory.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormaPagamentoResponseDTO {
    private String formaPagamento; // PIX, BOLETO, CARTAO_CREDITO
    private boolean possuiFormaPagamento;

    // Dados do cartao (somente quando formaPagamento = CARTAO_CREDITO)
    private String ultimosQuatroDigitos;
    private String bandeira;
    private String nomePortador;
    private String creditCardToken;
}
