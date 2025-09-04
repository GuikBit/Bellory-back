package org.exemplo.bellory.model.dto.clienteDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteCreateDTO {
    private String nomeCompleto;
    private String email;
    private String username;
    private String password;

    @NotBlank(message = "O CPF não pode ser vazio.")
    @CPF(message = "CPF inválido.")
    private String cpf;
    private String telefone;
    private LocalDate dataNascimento;
}
