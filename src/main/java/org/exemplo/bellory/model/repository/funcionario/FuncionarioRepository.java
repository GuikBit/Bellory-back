package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByUsername(String username);

    List<FuncionarioAgendamento> findAllProjectedBy();
}
