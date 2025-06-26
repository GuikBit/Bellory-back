package org.exemplo.bellory.model.entity.plano;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.users.Cliente;

import java.time.LocalDateTime;

@Entity
@Table(name = "plano_cliente")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlanoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_servico_id", nullable = false)
    private PlanoServico planoServico;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobranca_id", referencedColumnName = "id")
    private Cobranca cobranca;

    @Column(name = "dt_inicio", nullable = false)
    private LocalDateTime dtInicio;

    @Column(name = "dt_fim")
    private LocalDateTime dtFim;

    @Column(nullable = false, length = 50)
    private String status; // "Ativo", "Expirado", "Cancelado"

    // Getters e Setters
}
