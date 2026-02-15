package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BloqueioOrganizacaoUpdateDTO {
    private String titulo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String tipo;       // FERIADO ou BLOQUEIO
    private String descricao;
    private Boolean ativo;
}
