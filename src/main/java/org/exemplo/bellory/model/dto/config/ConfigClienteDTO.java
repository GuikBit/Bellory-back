package org.exemplo.bellory.model.dto.config;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigClienteDTO {
    private Boolean precisaCadastroAgendar = false;
    private Boolean programaFidelidade = false;
    private BigDecimal valorGastoUmPonto;
}
