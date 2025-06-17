package org.exemplo.bellory.model.entity.servico;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa um serviço oferecido, como um corte de cabelo,
 * manicure, etc.
 */
@Entity
@Table(name = "servico")
@Getter
@Setter
public class Servico {

    @Id
    private String id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String categoria;

    private String genero;

    @Column(length = 1000)
    private String descricao;

    private String tempo; // Ex: "90 min", "1 hora"

    // Usamos BigDecimal para preços para garantir a precisão monetária.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    // Mapeia a lista de nomes de produtos para uma tabela de suporte.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "servico_produtos", joinColumns = @JoinColumn(name = "servico_id"))
    @Column(name = "produto_nome", nullable = false)
    private List<String> produtos;

    @Column(length = 1000)
    private String imageUrl; // URL para a imagem do serviço

    /**
     * Este método é chamado automaticamente pelo JPA antes de a entidade ser guardada
     * pela primeira vez. Ele garante que cada serviço tenha um ID único.
     */
    @PrePersist
    public void prePersist() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
    }
}
