package org.exemplo.bellory.model.entity.servico;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 100)
    private String categoria;

    private String genero;

    @Column(columnDefinition = "TEXT")
    private String descricao;;

    @Column(name = "tempo_estimado_minutos", nullable = false)
    private Integer tempoEstimadoMinutos; // Ex: "90 min", "1 hora"

    // Usamos BigDecimal para preços para garantir a precisão monetária.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    // Mapeia a lista de nomes de produtos para uma tabela de suporte.
    // Lista de produtos usados no serviço
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "servico_produtos", joinColumns = @JoinColumn(name = "servico_id"))
    @Column(name = "produto_nome", nullable = false)
    private List<String> produtos;

    // Lista de URLs de imagens associadas ao serviço
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "servico_imagens", joinColumns = @JoinColumn(name = "servico_id"))
    @Column(name = "url_imagem", nullable = false, length = 1000)
    private List<String> urlsImagens;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(nullable = false)
    private boolean ativo = true;

    private String usuarioAtualizacao;

    /**
     * Este método é chamado automaticamente pelo JPA antes de a entidade ser guardada
     * pela primeira vez. Ele garante que cada serviço tenha um ID único.
     */
//    @PrePersist
//    public void prePersist() {
//        if (id == null || id.isEmpty()) {
//            id = UUID.randomUUID().toString();
//        }
//    }
}
