package org.exemplo.bellory.model.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteCreatePublicDTO {
    private String nome;
    private String telefone;
    private String email;
}
