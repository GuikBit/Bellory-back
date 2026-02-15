package org.exemplo.bellory.model.entity.produto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Categoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "produto", schema = "app", indexes = {
        @Index(name = "idx_produto_organizacao_id", columnList = "organizacao_id"),
        @Index(name = "idx_produto_categoria_id", columnList = "categoria_id"),
        @Index(name = "idx_produto_status", columnList = "status_produto"),
        @Index(name = "idx_produto_org_ativo", columnList = "organizacao_id, ativo"),
        @Index(name = "idx_produto_dt_criacao", columnList = "dt_criacao"),
        @Index(name = "idx_produto_preco", columnList = "preco")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(nullable = false, length = 255)
    private String nome;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(length = 100)
    private String genero;

    @Column(name = "avaliacao", precision = 3, scale = 2)
    private BigDecimal avaliacao;

    @Column(name = "total_avaliacoes")
    private int totalAvaliacoes;

    @Column(name = "desconto_percentual")
    private Integer descontoPercentual;

    @Column(nullable = false)
    private boolean destaque = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "codigo_barras", length = 50)
    private String codigoBarras;

    @Column(name = "codigo_interno", unique = true, length = 50)
    private String codigoInterno;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "preco_custo", precision = 10, scale = 2)
    private BigDecimal precoCusto;

    @Column(name = "quantidade_estoque")
    private Integer quantidadeEstoque = 0;

    @Column(name = "estoque_minimo")
    private Integer estoqueMinimo = 0;

    @Column(length = 10)
    private String unidade; // UN, KG, L, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "status_produto", nullable = false)
    private StatusProduto status = StatusProduto.ATIVO;

    @Column(length = 100)
    private String marca;

    @Column(length = 50)
    private String modelo;

    @Column(name = "peso_kg", precision = 8, scale = 3)
    private BigDecimal peso;

    // Lista de URLs de imagens do produto
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_imagens", schema = "app", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "url_imagem", nullable = false, length = 1000)
    private List<String> urlsImagens = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_ingredientes", schema = "app", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "ingrediente", nullable = false, columnDefinition = "TEXT")
    private List<String> ingredientes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_como_usar", schema = "app", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "passo", nullable = false, columnDefinition = "TEXT")
    @OrderColumn(name = "passo_ordem") // Mantém a ordem da lista
    private List<String> comoUsar;

    // Mapeia um mapa de Chave-Valor. Ótimo caso de uso para @ElementCollection.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_especificacoes", schema = "app", joinColumns = @JoinColumn(name = "produto_id"))
    @MapKeyColumn(name = "chave", length = 100)
    @Column(name = "valor", columnDefinition = "TEXT")
    private Map<String, String> especificacoes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "produto_relacionados_assoc",
            schema = "app",
            joinColumns = @JoinColumn(name = "produto_id"),
            inverseJoinColumns = @JoinColumn(name = "relacionado_id")
    )
    @JsonIgnore
    private Set<Produto> produtosRelacionados = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "produto_utilizados_assoc",
            schema = "app",
            joinColumns = @JoinColumn(name = "produto_id"),
            inverseJoinColumns = @JoinColumn(name = "utilizado_id")
    )
    @JsonIgnore
    private Set<Produto> produtosUtilizados = new HashSet<>();

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "usuario_criacao", length = 100)
    private String usuarioCriacao;

    @Column(name = "usuario_atualizacao", length = 100)
    private String usuarioAtualizacao;

    // === ENUM ===
    public enum StatusProduto {
        ATIVO("Ativo"),
        INATIVO("Inativo"),
        DESCONTINUADO("Descontinuado"),
        SEM_ESTOQUE("Sem Estoque");

        private final String descricao;

        StatusProduto(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // === MÉTODOS DE CONVENIÊNCIA ===
    public void adicionarImagem(String urlImagem) {
        if (this.urlsImagens == null) {
            this.urlsImagens = new ArrayList<>();
        }
        this.urlsImagens.add(urlImagem);
    }

    public void removerImagem(String urlImagem) {
        if (this.urlsImagens != null) {
            this.urlsImagens.remove(urlImagem);
        }
    }

    // === MÉTODOS DE ESTOQUE ===
    public void adicionarEstoque(Integer quantidade) {
        this.quantidadeEstoque += quantidade;
        if (this.quantidadeEstoque > 0 && this.status == StatusProduto.SEM_ESTOQUE) {
            this.status = StatusProduto.ATIVO;
        }
    }

    public void removerEstoque(Integer quantidade) {
        this.quantidadeEstoque -= quantidade;
        if (this.quantidadeEstoque <= 0) {
            this.quantidadeEstoque = 0;
            this.status = StatusProduto.SEM_ESTOQUE;
        }
    }

    public boolean temEstoque() {
        return this.quantidadeEstoque != null && this.quantidadeEstoque > 0;
    }

    public boolean temEstoqueDisponivel(Integer quantidadeDesejada) {
        return this.quantidadeEstoque != null && this.quantidadeEstoque >= quantidadeDesejada;
    }

    public boolean estoqueAbaixoDoMinimo() {
        return this.quantidadeEstoque != null &&
                this.estoqueMinimo != null &&
                this.quantidadeEstoque <= this.estoqueMinimo;
    }

    // === MÉTODOS DE PREÇO ===
    public BigDecimal calcularMargem() {
        if (this.precoCusto == null || this.precoCusto.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return this.preco.subtract(this.precoCusto)
                .divide(this.precoCusto, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public void ativar() {
        this.status = StatusProduto.ATIVO;
        this.dtAtualizacao = LocalDateTime.now();
    }

    public void inativar() {
        this.status = StatusProduto.INATIVO;
        this.dtAtualizacao = LocalDateTime.now();
    }

    public void descontinuar() {
        this.status = StatusProduto.DESCONTINUADO;
        this.dtAtualizacao = LocalDateTime.now();
    }

    public boolean isAtivo() {
        return this.status == StatusProduto.ATIVO;
    }

    @PrePersist
    public void prePersist() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = StatusProduto.ATIVO;
        }
        if (this.quantidadeEstoque == null) {
            this.quantidadeEstoque = 0;
        }
        if (this.estoqueMinimo == null) {
            this.estoqueMinimo = 0;
        }
        if (this.codigoInterno == null || this.codigoInterno.trim().isEmpty()) {
            gerarCodigoInterno();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }

    private void gerarCodigoInterno() {
        // Gera código interno no formato: PROD + timestamp
        this.codigoInterno = "PROD" + System.currentTimeMillis();
    }

}
