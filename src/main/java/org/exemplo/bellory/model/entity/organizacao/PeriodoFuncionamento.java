package org.exemplo.bellory.model.entity.organizacao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "periodo_funcionamento", schema = "app", indexes = {
        @Index(name = "idx_periodo_func_horario_id", columnList = "horario_funcionamento_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PeriodoFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_funcionamento_id", nullable = false)
    @JsonIgnore
    private HorarioFuncionamento horarioFuncionamento;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    public PeriodoFuncionamento(HorarioFuncionamento horarioFuncionamento, LocalTime horaInicio, LocalTime horaFim) {
        this.horarioFuncionamento = horarioFuncionamento;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
    }
}
