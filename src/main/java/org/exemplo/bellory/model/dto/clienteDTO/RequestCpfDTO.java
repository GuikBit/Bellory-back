package org.exemplo.bellory.model.dto.clienteDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestCpfDTO {
    @NotBlank(message = "O CPF é obrigatório.")
    @CPF(message = "Formato de CPF inválido.") // Validação extra, muito útil!
    private String cpf;
}
