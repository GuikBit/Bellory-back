package org.exemplo.bellory.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
class ClienteResumoDTO {
    private Long id;
    private String nome;
    private String email;
    private String telefone;
}
