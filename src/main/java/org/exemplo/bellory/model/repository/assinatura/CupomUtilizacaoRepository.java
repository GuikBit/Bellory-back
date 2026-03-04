package org.exemplo.bellory.model.repository.assinatura;

import org.exemplo.bellory.model.entity.assinatura.CupomUtilizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CupomUtilizacaoRepository extends JpaRepository<CupomUtilizacao, Long> {

    List<CupomUtilizacao> findByCupomIdOrderByDtUtilizacaoDesc(Long cupomId);

    long countByCupomIdAndOrganizacaoId(Long cupomId, Long organizacaoId);
}
