package org.exemplo.bellory.model.dto.plano;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoBelloryResponseDTO {

    private Long id;
    private String codigo;
    private String nome;
    private String tagline;
    private String descricaoCompleta;
    private boolean ativo;
    private boolean popular;

    // Visual/UI
    private String cta;
    private String badge;
    private String icone;
    private String cor;
    private String gradiente;

    // Precos
    private BigDecimal precoMensal;
    private BigDecimal precoAnual;
    private Double descontoPercentualAnual;

    // Promocao mensal
    private boolean promoMensalAtiva;
    private BigDecimal promoMensalPreco;
    private String promoMensalTexto;

    // Features
    private List<PlanoFeatureDTO> features;

    // Limites
    private PlanoLimitesDTO limites;

    // Ordem
    private Integer ordemExibicao;

    // Auditoria
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
    private Long userCriacao;
    private Long userAtualizacao;

    // Metricas
    private Long totalOrganizacoesUsando;
}
