package org.exemplo.bellory.model.entity.produto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.compra.CompraProduto;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "produto")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore // Evita serialização em loop e exposição desnecessária
    private Organizacao organizacao;

    @Column(nullable = false, length = 255)
    private String nome;

    @Lob // @Lob é uma forma padrão JPA de indicar um objeto grande, geralmente mapeado para TEXT ou BLOB.
    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "qtd_estoque", nullable = false)
    private int qtdEstoque = 0;

    @Column(length = 100)
    private String categoria;

    @Column(length = 100)
    private String genero;

    @Column(length = 100)
    private String marca;

    // Nota: Avaliações são idealmente gerenciadas em uma entidade separada (ex: AvaliacaoProduto)
    // para evitar problemas de concorrência. A média pode ser calculada via query.
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

    // --- COLEÇÕES DE ELEMENTOS SIMPLES ---

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_imagens", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "url_imagem", nullable = false)
    private List<String> urlsImagens;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_beneficios", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "beneficio", nullable = false, columnDefinition = "TEXT")
    private List<String> beneficios;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_ingredientes", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "ingrediente", nullable = false, columnDefinition = "TEXT")
    private List<String> ingredientes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_como_usar", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "passo", nullable = false, columnDefinition = "TEXT")
    @OrderColumn(name = "passo_ordem") // Mantém a ordem da lista
    private List<String> comoUsar;

    // Mapeia um mapa de Chave-Valor. Ótimo caso de uso para @ElementCollection.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_especificacoes", joinColumns = @JoinColumn(name = "produto_id"))
    @MapKeyColumn(name = "chave", length = 100)
    @Column(name = "valor", columnDefinition = "TEXT")
    private Map<String, String> especificacoes;

    // --- RELACIONAMENTOS COM OUTRAS ENTIDADES ---

    @OneToMany(mappedBy = "produto")
    @JsonIgnore // Essencial para evitar loop de serialização na API
    private Set<CompraProduto> compras = new HashSet<>();

    /**
     * Relacionamento Muitos-para-Muitos com a própria entidade Produto.
     * O JPA criará uma tabela de associação (ex: produto_relacionados_assoc)
     * para armazenar os pares de IDs, garantindo a integridade referencial.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "produto_relacionados_assoc",
            joinColumns = @JoinColumn(name = "produto_id"),
            inverseJoinColumns = @JoinColumn(name = "relacionado_id")
    )
    @JsonIgnore
    private Set<Produto> produtosRelacionados = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "produto_utilizados_assoc",
            joinColumns = @JoinColumn(name = "produto_id"),
            inverseJoinColumns = @JoinColumn(name = "utilizado_id")
    )
    @JsonIgnore
    private Set<Produto> produtosUtilizados = new HashSet<>();

    // --- TIMESTAMPS E CALLBACKS DE CICLO DE VIDA ---

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    // --- MÉTODOS AUXILIARES (GETTERS DERIVADOS) ---

    /**
     * Verifica se o produto está em estoque com base na quantidade.
     * Este valor não é persistido, evitando inconsistência de dados.
     * @return true se qtdEstoque for maior que zero.
     */
    public boolean isEmEstoque() {
        return this.qtdEstoque > 0;
    }



    public void adicionarUrlImagem(List<String> imagem) {
        if (this.urlsImagens == null) {
            this.urlsImagens = new ArrayList<>();
        }
        for (String url : imagem) {
            this.urlsImagens.add(url);
        }
    }
}
