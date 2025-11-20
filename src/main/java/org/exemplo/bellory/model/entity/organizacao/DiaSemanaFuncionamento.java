package org.exemplo.bellory.model.entity.organizacao;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaSemanaFuncionamento {
    // NOTA: O campo 'id' não deve existir em um @Embeddable
    // Se precisar de ID, transforme em uma entidade separada

    private String diaSemana;
    private String horaInicio;
    private String horaFim;
    private Boolean ativo;  // Mudado de String para Boolean
}
