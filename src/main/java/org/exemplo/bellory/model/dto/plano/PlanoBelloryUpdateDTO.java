package org.exemplo.bellory.model.dto.plano;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoBelloryUpdateDTO {

    @Size(max = 50, message = "Codigo deve ter no maximo 50 caracteres")
    private String codigo;

    @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
    private String nome;

    @Size(max = 255, message = "Tagline deve ter no maximo 255 caracteres")
    private String tagline;

    private String descricaoCompleta;

    private Boolean popular;

    @Size(max = 200, message = "CTA deve ter no maximo 200 caracteres")
    private String cta;

    @Size(max = 100, message = "Badge deve ter no maximo 100 caracteres")
    private String badge;

    @Size(max = 50, message = "Icone deve ter no maximo 50 caracteres")
    private String icone;

    @Size(max = 7, message = "Cor deve ter no maximo 7 caracteres")
    private String cor;

    @Size(max = 100, message = "Gradiente deve ter no maximo 100 caracteres")
    private String gradiente;

    @DecimalMin(value = "0.00", message = "Preco mensal deve ser maior ou igual a zero")
    private BigDecimal precoMensal;

    @DecimalMin(value = "0.00", message = "Preco anual deve ser maior ou igual a zero")
    private BigDecimal precoAnual;

    private Double descontoPercentualAnual;

    private List<PlanoFeatureDTO> features;

    private Integer ordemExibicao;

    private PlanoLimitesDTO limites;
}
