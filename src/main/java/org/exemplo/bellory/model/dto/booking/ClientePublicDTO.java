package org.exemplo.bellory.model.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientePublicDTO {
    private Long id;
    private String nome;
    private String telefone;
    private String email;
}
