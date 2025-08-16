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
public class ClienteAniversarianteDTO {
    private Long id;
    private String nomeCompleto;
    private String telefone;
    private String email;
    private LocalDate dataNascimento;
    private int idade;
    private int diasParaAniversario; // Negativo se já passou
    private boolean aniversarioHoje;
}
