package org.exemplo.bellory.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssinaturaStatusDTO {
    private boolean bloqueado;
    private String statusAssinatura;
    private Integer diasRestantesTrial;
    private String mensagem;
    private String planoCodigo;
    private String planoNome;
    private Boolean temCobrancaPendente;
    private BigDecimal valorPendente;
    private LocalDate dtVencimentoProximaCobranca;
}
