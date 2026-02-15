package org.exemplo.bellory.model.dto.config;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigServicoDTO {
   private Boolean mostrarValorAgendamento;
   private Boolean unicoServicoAgendamento;
   private Boolean mostrarAvaliacao;
}
