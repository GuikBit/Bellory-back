package org.exemplo.bellory.model.dto.plano;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReordenarPlanosDTO {

    @NotNull(message = "Lista de ordenacao e obrigatoria")
    private List<PlanoOrdemDTO> planos;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanoOrdemDTO {
        @NotNull(message = "ID do plano e obrigatorio")
        private Long id;

        @NotNull(message = "Ordem de exibicao e obrigatoria")
        private Integer ordemExibicao;
    }
}
