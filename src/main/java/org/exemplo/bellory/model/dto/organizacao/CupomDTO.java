package org.exemplo.bellory.model.dto.organizacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CupomDTO {
    private String codigo;
    private BigDecimal valorOriginal;
    private BigDecimal valorFinal;
    private BigDecimal valorDesconto;
    private String tipoDesconto;
    private BigDecimal percentualDesconto;
    private BigDecimal valorDescontoFixo;
    private String tipoAplicacao;
}
