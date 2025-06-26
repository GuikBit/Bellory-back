package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {
    // Você pode adicionar queries customizadas aqui se necessário
}
