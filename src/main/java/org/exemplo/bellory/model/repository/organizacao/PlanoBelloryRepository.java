package org.exemplo.bellory.model.repository.organizacao;

import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanoBelloryRepository extends JpaRepository<PlanoBellory, Long> {
    Optional<PlanoBellory> findByNome(String nome);

//    PlanoBellory findByCodigo(String id);

    Optional<PlanoBellory> findByCodigo(String codigo);
//
//    PlanoBellory findByCodigo(String codigo);
}


