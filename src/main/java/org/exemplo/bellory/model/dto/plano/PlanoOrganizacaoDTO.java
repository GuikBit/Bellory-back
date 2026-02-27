package org.exemplo.bellory.model.dto.plano;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoOrganizacaoDTO {

    private String planoAtualCodigo;
    private String planoAtualNome;
    private List<PlanoBelloryPublicDTO> planosDisponiveis;
}
