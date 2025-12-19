package org.exemplo.bellory.model.entity.funcionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jornada_dia", schema = "app")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JornadaDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    @JsonIgnore
    private Funcionario funcionario;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DiaSemana diaSemana;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @OneToMany(mappedBy = "jornadaDia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HorarioTrabalho> horarios = new ArrayList<>();

    // Métodos auxiliares para gerenciar o relacionamento
    public void addHorario(HorarioTrabalho horario) {
        horarios.add(horario);
        horario.setJornadaDia(this);
    }

    public void removeHorario(HorarioTrabalho horario) {
        horarios.remove(horario);
        horario.setJornadaDia(null);
    }
}
