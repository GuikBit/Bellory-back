package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {
    List<BloqueioAgenda> findByFuncionarioAndInicioBloqueioBetween(Funcionario funcionario, LocalDateTime inicioBloqueioAfter, LocalDateTime inicioBloqueioBefore);
}
