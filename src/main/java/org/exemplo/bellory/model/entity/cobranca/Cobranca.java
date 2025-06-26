package org.exemplo.bellory.model.entity.cobranca;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.compra.Compra;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;
import org.exemplo.bellory.model.entity.users.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cobranca")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", referencedColumnName = "id")
    private Agendamento agendamento;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", referencedColumnName = "id")
    private Compra compra;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "status_cobranca", nullable = false, length = 50)
    private String statusCobranca; // "Pendente", "Paga", "Vencida", "Cancelada"

    @Column(name = "dt_vencimento")
    private LocalDate dtVencimento;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @OneToMany(mappedBy = "cobranca", cascade = CascadeType.ALL)
    @JsonManagedReference("cobranca-pagamentos")
    private List<Pagamento> pagamentos;

    // Getters e Setters
}