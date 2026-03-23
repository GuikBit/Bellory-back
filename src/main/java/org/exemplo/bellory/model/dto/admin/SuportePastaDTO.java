package org.exemplo.bellory.model.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuportePastaDTO {
    private String nome;
    private String caminho;
    private int totalImagens;
}
