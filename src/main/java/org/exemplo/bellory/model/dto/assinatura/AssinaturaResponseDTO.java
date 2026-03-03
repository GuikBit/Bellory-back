package org.exemplo.bellory.model.dto.assinatura;

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
public class AssinaturaResponseDTO {
    private Long id;
    private Long organizacaoId;
    private String organizacaoNome;
    private Long planoBelloryId;
    private String planoNome;
    private String planoCodigo;
    private String status;
    private String cicloCobranca;
    private LocalDateTime dtInicioTrial;
    private LocalDateTime dtFimTrial;
    private LocalDateTime dtInicio;
    private LocalDateTime dtProximoVencimento;
    private LocalDateTime dtCancelamento;
    private BigDecimal valorMensal;
    private BigDecimal valorAnual;
    private String assasCustomerId;
    private String assasSubscriptionId;
    private String cupomCodigo;
    private BigDecimal valorDesconto;
    private LocalDateTime dtCriacao;
}
