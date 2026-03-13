package org.exemplo.bellory.model.entity.pagamento;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DadosCartao {
    // Apenas dados seguros para exibicao
    private String nome;
    private String ultimosQuatroDigitos;
    private String bandeira;
}
