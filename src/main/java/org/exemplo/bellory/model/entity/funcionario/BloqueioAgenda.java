package org.exemplo.bellory.model.entity.funcionario; // Ou onde você preferir organizar

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;



@Entity
@Table(name = "bloqueio_agenda")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BloqueioAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    @JsonIgnore
    private Funcionario funcionario; // O funcionário cujo tempo está sendo bloqueado

    @Column(name = "inicio_bloqueio", nullable = false)
    private LocalDateTime inicioBloqueio;

    @Column(name = "fim_bloqueio", nullable = false)
    private LocalDateTime fimBloqueio;

    @Column(name = "descricao", length = 255) // Ex: "Agendamento de corte", "Almoço", "Reunião"
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_bloqueio", nullable = false)
    private TipoBloqueio tipoBloqueio; // Para diferenciar se é um agendamento, almoço, etc.

    // Relacionamento opcional com Agendamento
    // Se este bloqueio for resultado de um Agendamento, você pode ligá-los
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")// A coluna que referencia a entidade Agendamento
    @JsonIgnore
    private Agendamento agendamento; // Pode ser nulo se for um bloqueio manual


    public BloqueioAgenda(Funcionario funcionario, LocalDateTime inicioBloqueio, LocalDateTime fimBloqueio, String descricao, TipoBloqueio tipoBloqueio, Agendamento agendamento) {
        this.funcionario = funcionario;
        this.inicioBloqueio = inicioBloqueio;
        this.fimBloqueio = fimBloqueio;
        this.descricao = descricao;
        this.tipoBloqueio = tipoBloqueio;
        this.agendamento = agendamento;
    }
}
