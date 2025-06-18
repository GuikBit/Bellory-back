package org.exemplo.bellory.model.entity.produto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade que representa os detalhes completos de um produto.
 * Esta estrutura é flexível para acomodar vários tipos de informações de produto,
 * desde textos simples a listas e especificações técnicas.
 */
@Entity
@Table(name = "produto")
@Getter
@Setter
public class Produto {

    @Id
    private String id;

    @Column(nullable = false)
    private String nome;

    // Usamos BigDecimal para preços para evitar problemas de arredondamento de ponto flutuante.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(length = 1000)
    private String descricao;

    @Lob // Para textos muito longos
    @Column(columnDefinition = "TEXT")
    private String descricaoCompleta;

    // @ElementCollection mapeia uma coleção de tipos básicos (como String)
    // para uma tabela separada no banco de dados.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_imagens", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "imagem_url", nullable = false, length = 1000)
    private List<String> imagens;

    private String categoria;
    private String genero;
    private String marca;

    private double avaliacao;
    private int totalAvaliacoes;
    private Integer desconto;
    private Boolean destaque;
    private boolean emEstoque = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_como_usar", joinColumns = @JoinColumn(name = "produto_id"))
    @Lob
    @Column(name = "passo", nullable = false, columnDefinition = "TEXT")
    private List<String> comoUsar;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_informacoes_importantes", joinColumns = @JoinColumn(name = "produto_id"))
    @Lob
    @Column(name = "informacao", nullable = false, columnDefinition = "TEXT")
    private List<String> informacoesImportantes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_ingredientes", joinColumns = @JoinColumn(name = "produto_id"))
    @Lob
    @Column(name = "ingrediente", nullable = false, columnDefinition = "TEXT")
    private List<String> ingredientes;

    // Mapeia um mapa de Chave-Valor para uma tabela de especificações
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_especificacoes", joinColumns = @JoinColumn(name = "produto_id"))
    @MapKeyColumn(name = "chave")
    @Lob
    @Column(name = "valor", columnDefinition = "TEXT")
    private Map<String, String> especificacoes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_relacionados", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "produto_relacionado_id")
    private List<String> produtosRelacionados;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_utilizados", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "produto_utilizado_id")
    private List<String> produtosUtilizados;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_beneficios", joinColumns = @JoinColumn(name = "produto_id"))
    @Lob
    @Column(name = "beneficio", nullable = false, columnDefinition = "TEXT")
    private List<String> beneficios;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_resultados", joinColumns = @JoinColumn(name = "produto_id"))
    @Lob
    @Column(name = "resultado", nullable = false, columnDefinition = "TEXT")
    private List<String> resultados;

    /**
     * Este método é chamado automaticamente pelo JPA antes de a entidade ser guardada
     * pela primeira vez. Ele garante que cada produto tenha um ID único.
     */
    @PrePersist
    public void prePersist() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
    }
}
