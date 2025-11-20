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
    private String numero;
    private String nome;
    private String validade;
    private String cvv;
}
