package org.exemplo.bellory.model.dto.organizacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para atualização de endereço (PATCH-like).
 * Mesmos nomes de campo do {@link EnderecoDTO} usado no signup, porém todos opcionais.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateEnderecoDTO {

    private String cep;

    @JsonProperty("rua")
    private String logradouro;

    private String numero;

    private String complemento;

    private String bairro;

    private String cidade;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String referencia;

    @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres")
    @JsonProperty("estado")
    private String uf;
}
