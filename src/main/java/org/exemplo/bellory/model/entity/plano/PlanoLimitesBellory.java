package org.exemplo.bellory.model.entity.plano;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plano_limites_bellory", schema = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanoLimitesBellory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_bellory_id", nullable = false, unique = true)
    @JsonIgnore
    private PlanoBellory plano; // ← Este é o nome usado no mappedBy

    // Limites de recursos
    @Column(name = "max_agendamentos_mes")
    private Integer maxAgendamentosMes; // null = ilimitado

    @Column(name = "max_usuarios")
    private Integer maxUsuarios; // null = ilimitado

    @Column(name = "max_clientes")
    private Integer maxClientes; // null = ilimitado

    @Column(name = "max_servicos")
    private Integer maxServicos; // null = ilimitado

    @Column(name = "max_unidades")
    private Integer maxUnidades;

    // Features booleanas
    @Column(name = "permite_agendamento_online")
    private boolean permiteAgendamentoOnline = false;

    @Column(name = "permite_whatsapp")
    private boolean permiteWhatsapp = false;

    @Column(name = "permite_site")
    private boolean permiteSite = false;

    @Column(name = "permite_ecommerce")
    private boolean permiteEcommerce = false;

    @Column(name = "permite_relatorios_avancados")
    private boolean permiteRelatoriosAvancados = false;

    @Column(name = "permite_api")
    private boolean permiteApi = false;

    @Column(name = "permite_integracao_personalizada")
    private boolean permiteIntegracaoPersonalizada = false;

    @Column(name = "suporte_prioritario")
    private boolean suportePrioritario = false;

    @Column(name = "suporte_24x7")
    private boolean suporte24x7 = false;
}
