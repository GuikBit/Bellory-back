package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HorarioFuncionamentoDTO {
    private String diaSemana;           // "SEGUNDA"
    private String diaSemanaLabel;      // "Segunda-feira"
    private Boolean aberto;
    private String horaAbertura;        // "09:00"
    private String horaFechamento;      // "18:00"
    private String observacao;          // "Fechado para almoço das 12h às 13h"
}
