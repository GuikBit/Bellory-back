package org.exemplo.bellory.model.repository.assinatura;

import org.exemplo.bellory.model.entity.assinatura.CupomDesconto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CupomDescontoRepository extends JpaRepository<CupomDesconto, Long> {

    Optional<CupomDesconto> findByCodigo(String codigo);

    @Query("SELECT c FROM CupomDesconto c WHERE c.codigo = :codigo AND c.ativo = true")
    Optional<CupomDesconto> findByCodigoAtivo(@Param("codigo") String codigo);

    @Query("SELECT c FROM CupomDesconto c WHERE c.ativo = true " +
           "AND (c.dtInicio IS NULL OR c.dtInicio <= CURRENT_TIMESTAMP) " +
           "AND (c.dtFim IS NULL OR c.dtFim >= CURRENT_TIMESTAMP) " +
           "ORDER BY c.dtCriacao DESC")
    List<CupomDesconto> findVigentes();

    List<CupomDesconto> findAllByOrderByDtCriacaoDesc();

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
