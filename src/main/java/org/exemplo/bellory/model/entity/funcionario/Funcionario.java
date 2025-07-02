package org.exemplo.bellory.model.entity.funcionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.users.User;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "funcionario")
@Getter
@Setter
public class Funcionario extends User {

    private String cargo;

    // --- NOVO RELACIONAMENTO: Jornada de Trabalho ---
    // Um funcionário tem um conjunto de jornadas de trabalho (uma para cada dia da semana que trabalha).
    // Cascade.ALL: Se salvar o funcionário, salva a jornada junto.
    // orphanRemoval=true: Se remover uma jornada da lista do funcionário, ela é apagada do banco.
    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<JornadaTrabalho> jornadaDeTrabalho = new HashSet<>();

    // --- NOVO RELACIONAMENTO: Bloqueios de Agenda ---
    // Um funcionário pode ter múltiplos bloqueios na sua agenda.
    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<BloqueioAgenda> bloqueiosAgenda = new HashSet<>();

    // Métodos para ajudar a gerenciar a sincronia dos relacionamentos (opcional, mas boa prática)
    public void addJornada(JornadaTrabalho jornada) {
        jornadaDeTrabalho.add(jornada);
        jornada.setFuncionario(this);
    }

    public void removeJornada(JornadaTrabalho jornada) {
        jornadaDeTrabalho.remove(jornada);
        jornada.setFuncionario(null);
    }

    public void addBloqueio(BloqueioAgenda bloqueio) {
        bloqueiosAgenda.add(bloqueio);
        bloqueio.setFuncionario(this);
    }
}
