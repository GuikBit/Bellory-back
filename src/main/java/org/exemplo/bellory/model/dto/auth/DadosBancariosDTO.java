package org.exemplo.bellory.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DadosBancariosDTO {
    private String banco;
    private String agencia;
    private String conta;
    private String operacao;
}

