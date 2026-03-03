package org.exemplo.bellory.model.dto.cupom;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CupomUtilizacaoDTO {
    private Long id;
    private Long cupomId;
    private String cupomCodigo;
    private Long organizacaoId;
    private Long assinaturaId;
    private Long cobrancaId;
    private BigDecimal valorOriginal;
    private BigDecimal valorDesconto;
    private BigDecimal valorFinal;
    private String planoCodigo;
    private String cicloCobranca;
    private LocalDateTime dtUtilizacao;
}
