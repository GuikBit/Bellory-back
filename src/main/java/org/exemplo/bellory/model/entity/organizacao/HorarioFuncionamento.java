package org.exemplo.bellory.model.entity.organizacao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.funcionario.DiaSemana;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "horario_funcionamento", schema = "app",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_horario_func_org_dia",
                columnNames = {"organizacao_id", "dia_semana"}
        ),
        indexes = {
                @Index(name = "idx_horario_func_org_id", columnList = "organizacao_id"),
                @Index(name = "idx_horario_func_org_dia", columnList = "organizacao_id, dia_semana")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HorarioFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DiaSemana diaSemana;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @OneToMany(mappedBy = "horarioFuncionamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PeriodoFuncionamento> periodos = new ArrayList<>();

    public void addPeriodo(PeriodoFuncionamento periodo) {
        periodos.add(periodo);
        periodo.setHorarioFuncionamento(this);
    }

    public void removePeriodo(PeriodoFuncionamento periodo) {
        periodos.remove(periodo);
        periodo.setHorarioFuncionamento(null);
    }
}
