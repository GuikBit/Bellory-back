package org.exemplo.bellory.model.repository.financeiro;

import org.exemplo.bellory.model.entity.financeiro.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CentroCustoRepository extends JpaRepository<CentroCusto, Long> {

    List<CentroCusto> findByOrganizacaoIdAndAtivoTrue(Long organizacaoId);

    List<CentroCusto> findByOrganizacaoId(Long organizacaoId);

    Optional<CentroCusto> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    boolean existsByNomeAndOrganizacaoId(String nome, Long organizacaoId);

    boolean existsByCodigoAndOrganizacaoId(String codigo, Long organizacaoId);
}
