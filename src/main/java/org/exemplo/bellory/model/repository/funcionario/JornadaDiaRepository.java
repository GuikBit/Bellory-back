package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.entity.funcionario.DiaSemana;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.funcionario.JornadaDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JornadaDiaRepository extends JpaRepository<JornadaDia, Long> {

    Optional<JornadaDia> findByFuncionarioAndDiaSemana(Funcionario funcionario, DiaSemana diaSemana);

    List<JornadaDia> findByFuncionarioId(Long funcionarioId);

    void deleteByFuncionarioIdAndDiaSemana(Long funcionarioId, DiaSemana diaSemana);
}