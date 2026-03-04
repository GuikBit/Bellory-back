package org.exemplo.bellory.model.dto.cupom;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CupomDescontoResponseDTO {
    private Long id;
    private String codigo;
    private String descricao;
    private String tipoDesconto;
    private BigDecimal valorDesconto;
    private LocalDateTime dtInicio;
    private LocalDateTime dtFim;
    private Integer maxUtilizacoes;
    private Integer maxUtilizacoesPorOrg;
    private Integer totalUtilizado;
    private List<String> planosPermitidos;
    private List<String> segmentosPermitidos;
    private List<Long> organizacoesPermitidas;
    private String cicloCobranca;
    private Boolean ativo;
    private boolean vigente;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
}
