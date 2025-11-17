package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.exemplo.bellory.model.entity.funcionario.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CargoRepository extends JpaRepository<Cargo, Long> {
    //Collection<Cargo> findAllByOrganizacao_IdAndAtivoTrue(Long organizacaoId);
    List<Cargo> findAllByOrganizacao_IdAndAtivoTrue(Long organizacaoId);
}
