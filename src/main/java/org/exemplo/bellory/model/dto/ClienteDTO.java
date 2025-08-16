package org.exemplo.bellory.model.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDTO {

    private Long id;
    private String nomeCompleto;
    private String email;
    private String telefone;
    private LocalDate dataNascimento;
    private boolean ativo;
    private String role;

    // Você pode adicionar um construtor se quiser, para facilitar a conversão
    public ClienteDTO(Long id, String nomeCompleto, String email, String telefone, LocalDate dataNascimento, String role, boolean ativo) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.ativo = ativo;
        this.role = role;
    }
}
