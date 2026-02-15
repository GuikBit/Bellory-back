package org.exemplo.bellory.model.entity.endereco;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.users.Cliente;

@Entity
@Table(name = "endereco", schema = "app", indexes = {
    @Index(name = "idx_endereco_cliente_id", columnList = "cliente_id"),
    @Index(name = "idx_endereco_tipo", columnList = "tipo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false, length = 255)
    private String logradouro;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 10)
    private String cep;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(columnDefinition = "TEXT")
    private String referencia;

    @Column(length = 255)
    private String complemento;

    @Column(nullable = false)
    private boolean isPrincipal = false;

    @Column(nullable = true)
    private Coordenadas coordenadas;

    @Column(nullable = false)
    private boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoEndereco tipo;

    public enum TipoEndereco {
        RESIDENCIAL,
        COMERCIAL,
        CORRESPONDENCIA,
        OUTRO
    }
}
