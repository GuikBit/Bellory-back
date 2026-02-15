package org.exemplo.bellory.model.repository.financeiro;

import org.exemplo.bellory.model.entity.financeiro.ContaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {

    List<ContaBancaria> findByOrganizacaoIdAndAtivoTrue(Long organizacaoId);

    List<ContaBancaria> findByOrganizacaoId(Long organizacaoId);

    Optional<ContaBancaria> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    Optional<ContaBancaria> findByOrganizacaoIdAndPrincipalTrue(Long organizacaoId);

    @Query("SELECT COALESCE(SUM(c.saldoAtual), 0) FROM ContaBancaria c WHERE c.organizacao.id = :orgId AND c.ativo = true")
    BigDecimal sumSaldoAtualByOrganizacao(@Param("orgId") Long organizacaoId);
}
