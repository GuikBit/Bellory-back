package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StatusAgendamentoDTO {
    private String codigo;
    private String descricao;
    private String cor;
    private boolean ativo;
}
