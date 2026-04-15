package org.exemplo.bellory.model.entity.plano;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plano_bellory", schema = "admin",
    indexes = {
        @Index(name = "idx_plano_bellory_ativo_ordem", columnList = "ativo, ordem_exibicao")
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlanoBellory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código único do plano (ex: "gratuito", "basico", "plus", "premium")
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "tagline", length = 255)
    private String tagline; // "Experimente sem compromisso"

    @Column(columnDefinition = "TEXT")
    private String descricaoCompleta;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean popular = false; // Se é o plano mais popular

    // Visual/UI
    @Column(length = 200)
    private String cta; // Call-to-action: "Começar grátis"

    @Column(length = 100)
    private String badge; // "🔥 Mais popular"

    @Column(length = 50)
    private String icone; // Nome do ícone (Gift, Zap, Sparkles, Crown)

    @Column(length = 7)
    private String cor; // "#4f6f64"

    @Column(length = 100)
    private String gradiente; // "from-[#4f6f64] to-[#3d574f]"

    // Preços
    @Column(name = "preco_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMensal;

    @Column(name = "preco_anual", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoAnual;

    @Column(name = "desconto_percentual_anual")
    private Double descontoPercentualAnual; // Ex: 20.0 (20%)

    // Promocao mensal
    @Column(name = "promo_mensal_ativa", nullable = false)
    private boolean promoMensalAtiva = false;

    @Column(name = "promo_mensal_preco", precision = 10, scale = 2)
    private BigDecimal promoMensalPreco;

    @Column(name = "promo_mensal_texto", length = 100)
    private String promoMensalTexto; // Ex: "Black Friday"

    // Features - OPÇÃO 1: JSONB (mais flexível)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String features; // Armazena JSON: [{"text": "...", "included": true}]

    // Features - OPÇÃO 2: Relacionamento (mais estruturado)
    // @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // private List<PlanoFeature> features = new ArrayList<>();

    // Limites do plano
    @OneToOne(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PlanoLimitesBellory limites;

    // Auditoria
    @CreationTimestamp
    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @UpdateTimestamp
    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "user_criacao")
    private Long userCriacao;

    @Column(name = "user_atualizacao")
    private Long userAtualizacao;

    // Ordem de exibição
    @Column(name = "ordem_exibicao")
    private Integer ordemExibicao; // Para controlar a ordem na tela

    // Métodos auxiliares
    @PrePersist
    protected void onCreate() {
        if (dtCriacao == null) {
            dtCriacao = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }
}
