package org.exemplo.bellory.model.entity.funcionario; // Ou onde você preferir organizar

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.funcionario.DiaSemana;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;

import java.time.LocalTime; // Para representar apenas a hora

@Entity
@Table(name = "jornada_trabalho", schema = "app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JornadaTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    @JsonIgnore
    private Funcionario funcionario; // Relaciona com o funcionário

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DiaSemana diaSemana; // Enum para o dia da semana

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "ativo")
    private Boolean ativo;

    public JornadaTrabalho(Funcionario funcionario, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFim) {
        this.funcionario = funcionario;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
    }
}
