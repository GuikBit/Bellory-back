package org.exemplo.bellory.model.repository.organizacao;

import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.entity.plano.PlanoLimitesBellory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface PlanoLimiteBelloryRepository extends JpaRepository<PlanoLimitesBellory, Long> {

    boolean existsByPlanoId(Long id);
}
