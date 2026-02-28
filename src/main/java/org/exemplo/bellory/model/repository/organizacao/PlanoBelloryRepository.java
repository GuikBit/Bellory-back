package org.exemplo.bellory.model.repository.organizacao;

import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanoBelloryRepository extends JpaRepository<PlanoBellory, Long> {

    Optional<PlanoBellory> findByNome(String nome);

    Optional<PlanoBellory> findByCodigo(String codigo);

    List<PlanoBellory> findByAtivoTrueOrderByOrdemExibicaoAsc();

    List<PlanoBellory> findAllByOrderByOrdemExibicaoAsc();

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    @Query("SELECT COUNT(o) FROM Organizacao o WHERE o.plano.id = :planoId AND o.ativo = true")
    Long countOrganizacoesByPlanoId(@Param("planoId") Long planoId);
}
