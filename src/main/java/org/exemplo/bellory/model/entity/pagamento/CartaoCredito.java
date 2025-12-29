package org.exemplo.bellory.model.entity.pagamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.Cliente;

import java.time.LocalDate;

@Entity
@Table(name = "cartao_credito", schema = "app")
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

    @Column(nullable = false, length = 19) // Formato: XXXX-XXXX-XXXX-XXXX
    private String numeroCartao;

    @Column(nullable = false, length = 7) // Formato: MM/YYYY
    private String dataVencimento;

    @Column(nullable = false, length = 4)
    private String cvv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BandeiraCartao bandeira;

    @Column(nullable = false)
    private boolean isPrincipal = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(length = 50)
    private String apelido; // Nome amigável para o cartão

    public enum BandeiraCartao {
        VISA,
        MASTERCARD,
        AMERICAN_EXPRESS,
        ELO,
        HIPERCARD,
        DINERS_CLUB
    }
}
