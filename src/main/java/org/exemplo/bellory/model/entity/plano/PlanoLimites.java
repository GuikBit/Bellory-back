package org.exemplo.bellory.model.entity.plano;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plano_limites", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanoLimites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "limite_cliente")
    private int limiteCliente;

    @Column(name = "limite_agendamento")
    private int limiteAgendamento;

    @Column(name = "limite_colaborador")
    private int limiteColaborador;

    @Column(name = "permite_landing_page")
    private boolean permiteLandingPage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    private Plano plano;
}
