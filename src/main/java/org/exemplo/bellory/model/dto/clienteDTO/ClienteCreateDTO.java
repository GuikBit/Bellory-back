package org.exemplo.bellory.model.dto.clienteDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String cpf;
    private String telefone;
    private LocalDate dataNascimento;
}
