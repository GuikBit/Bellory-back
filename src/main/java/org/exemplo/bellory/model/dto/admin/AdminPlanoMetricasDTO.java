package org.exemplo.bellory.model.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPlanoMetricasDTO {
    private Long totalPlanos;
    private Long planosAtivos;

    private List<PlanoDistribuicao> distribuicao;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanoDistribuicao {
        private Long planoId;
        private String codigo;
        private String nome;
        private BigDecimal precoMensal;
        private BigDecimal precoAnual;
        private Long totalOrganizacoes;
        private Double percentualDistribuicao;
        private Boolean ativo;
        private Boolean popular;
    }
}
