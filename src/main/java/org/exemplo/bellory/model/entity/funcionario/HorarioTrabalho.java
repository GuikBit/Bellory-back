package org.exemplo.bellory.model.entity.funcionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "horario_trabalho", schema = "app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HorarioTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jornada_dia_id", nullable = false)
    @JsonIgnore
    private JornadaDia jornadaDia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    public HorarioTrabalho(JornadaDia jornadaDia, LocalTime horaInicio, LocalTime horaFim) {
        this.jornadaDia = jornadaDia;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
    }
}
