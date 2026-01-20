package org.exemplo.bellory.model.dto.config;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigColaboradorDTO {
    private Boolean selecionarColaboradorAgendamento;
    private Boolean mostrarNotasComentarioColaborador;
    private Boolean comissaoPadrao;
}
