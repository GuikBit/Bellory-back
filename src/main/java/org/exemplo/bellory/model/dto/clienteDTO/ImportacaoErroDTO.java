package org.exemplo.bellory.model.dto.clienteDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportacaoErroDTO {
    // Numero da linha no CSV (1 = header; primeira linha de dados = 2).
    private Integer linha;
    private String motivo;
}
