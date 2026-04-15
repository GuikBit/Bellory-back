package org.exemplo.bellory.model.entity.pagamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.Cliente;

import java.time.LocalDateTime;

@Entity
@Table(name = "cartao_credito", schema = "app", indexes = {
    @Index(name = "idx_cartao_cliente_id", columnList = "cliente_id"),
    @Index(name = "idx_cartao_organizacao_id", columnList = "organizacao_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaoCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id")
    @JsonIgnore
    private Organizacao organizacao;

    @Column(nullable = false, length = 100)
    private String nomePortador;

    // Apenas os ultimos 4 digitos (ex: "1234")
    @Column(name = "ultimos_quatro_digitos", length = 4)
    private String ultimosQuatroDigitos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BandeiraCartao bandeira;

    @Column(nullable = false)
    private boolean isPrincipal = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(length = 50)
    private String apelido;

    // Token do cartao no Asaas (referencia segura para cobrar sem dados sensíveis)
    @Column(name = "assas_credit_card_token", length = 255)
    private String assasCreditCardToken;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    public void prePersist() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }

    public enum BandeiraCartao {
        VISA,
        MASTERCARD,
        AMERICAN_EXPRESS,
        ELO,
        HIPERCARD,
        DINERS_CLUB
    }
}
