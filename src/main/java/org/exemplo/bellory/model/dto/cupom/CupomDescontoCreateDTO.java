package org.exemplo.bellory.model.dto.cupom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CupomDescontoCreateDTO {

    @NotBlank(message = "Codigo do cupom e obrigatorio")
    @Size(max = 50, message = "Codigo deve ter no maximo 50 caracteres")
    private String codigo;

    @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres")
    private String descricao;

    @NotNull(message = "Tipo de desconto e obrigatorio")
    private String tipoDesconto; // PERCENTUAL, VALOR_FIXO

    @NotNull(message = "Valor do desconto e obrigatorio")
    @Positive(message = "Valor do desconto deve ser positivo")
    private BigDecimal valorDesconto;

    private LocalDateTime dtInicio;
    private LocalDateTime dtFim;
    private Integer maxUtilizacoes;
    private Integer maxUtilizacoesPorOrg;
    private List<String> planosPermitidos;
    private List<String> segmentosPermitidos;
    private List<Long> organizacoesPermitidas;
    private String cicloCobranca; // MENSAL, ANUAL ou null
}
