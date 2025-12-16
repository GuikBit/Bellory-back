package org.exemplo.bellory.model.dto.organizacao;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoDTO {
    @NotBlank(message = "CEP é obrigatório")
    private String cep;

    // ✅ Aceita "rua" do JSON e mapeia para logradouro
    @NotBlank(message = "Logradouro é obrigatório")
    @JsonProperty("rua")
    private String logradouro;

    @NotBlank(message = "Número é obrigatório")
    private String numero;

    private String complemento;

    @NotBlank(message = "Bairro é obrigatório")
    private String bairro;

    @NotBlank(message = "Cidade é obrigatória")
    private String cidade;

    // ✅ Aceita "estado" do JSON e mapeia para uf
    @NotBlank(message = "Estado é obrigatório")
    @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres")
    @JsonProperty("estado")
    private String uf;
}
