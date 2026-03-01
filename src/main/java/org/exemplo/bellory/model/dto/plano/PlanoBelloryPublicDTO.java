package org.exemplo.bellory.model.dto.plano;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoBelloryPublicDTO {

    private String id;          // codigo
    private String name;        // nome
    private String tagline;
    private String description; // descricaoCompleta
    private boolean popular;

    // Visual/UI
    private String cta;
    private String badge;
    private String icon;        // icone
    private String color;       // cor
    private String gradient;    // gradiente

    // Pricing
    private BigDecimal price;        // precoMensal
    private BigDecimal yearlyPrice;  // precoAnual
    private Double yearlyDiscount;   // descontoPercentualAnual

    // Promo mensal
    private boolean promoMensalAtiva;
    private BigDecimal promoMensalPreco;
    private String promoMensalTexto;

    // Features
    private List<PlanoFeatureDTO> features;

    // Limites
    private PlanoLimitesDTO limits;
}
