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
    // Status principal
    private boolean bloqueado;
    private String statusAssinatura;
    private String situacao;          // SituacaoAssinatura - enum semantico para o frontend
    private String mensagem;

    // Plano
    private String planoCodigo;
    private String planoNome;
    private boolean planoGratuito;

    // Trial
    private Integer diasRestantesTrial;
    private LocalDate dtFimTrial;

    // Cobranca pendente
    private Boolean temCobrancaPendente;
    private BigDecimal valorPendente;
    private LocalDate dtVencimentoProximaCobranca;

    // Cancelamento
    private LocalDate dtAcessoAte;

    // Ciclo
    private String cicloCobranca;
    private LocalDate dtProximoVencimento;
}
