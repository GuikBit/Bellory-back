package org.exemplo.bellory.model.dto.clienteDTO;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentamentoClienteDTO {
    private String nomeCompleto;
    private String telefone;
    private String email;
    private String cpf;
    private LocalDate dataNascimento;

    public AgentamentoClienteDTO(String nomeCompleto, String email, String telefone, LocalDate dataNascimento, String cpf) {
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.cpf = cpf;
    }

    public AgentamentoClienteDTO(String nomeCompleto, String telefone){
        this.nomeCompleto = nomeCompleto;
        this.telefone = telefone;
    }
}
