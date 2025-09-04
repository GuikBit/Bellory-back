package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoRepository extends JpaRepository<BloqueioAgenda, Long> {
}
