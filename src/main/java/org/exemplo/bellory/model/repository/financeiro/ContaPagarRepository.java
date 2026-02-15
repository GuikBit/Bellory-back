package org.exemplo.bellory.model.repository.financeiro;

import org.exemplo.bellory.model.entity.financeiro.ContaPagar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContaPagarRepository extends JpaRepository<ContaPagar, Long> {

    Optional<ContaPagar> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<ContaPagar> findByOrganizacaoId(Long organizacaoId);

    // Por status
    List<ContaPagar> findByOrganizacaoIdAndStatus(Long organizacaoId, ContaPagar.StatusContaPagar status);

    // Vencidas
    @Query("SELECT c FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.dtVencimento < :hoje AND c.status NOT IN ('PAGA', 'CANCELADA')")
    List<ContaPagar> findVencidasByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje);

    // A vencer nos próximos N dias
    @Query("SELECT c FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :hoje AND :dataLimite AND c.status NOT IN ('PAGA', 'CANCELADA')")
    List<ContaPagar> findAVencerByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje, @Param("dataLimite") LocalDate dataLimite);

    // Por período
    @Query("SELECT c FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :inicio AND :fim ORDER BY c.dtVencimento")
    List<ContaPagar> findByOrganizacaoAndPeriodo(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por período e status
    @Query("SELECT c FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :inicio AND :fim AND c.status = :status ORDER BY c.dtVencimento")
    List<ContaPagar> findByOrganizacaoAndPeriodoAndStatus(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, @Param("status") ContaPagar.StatusContaPagar status);

    // Por competência
    @Query("SELECT c FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.dtCompetencia BETWEEN :inicio AND :fim ORDER BY c.dtCompetencia")
    List<ContaPagar> findByOrganizacaoAndCompetencia(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por categoria
    List<ContaPagar> findByOrganizacaoIdAndCategoriaFinanceiraId(Long organizacaoId, Long categoriaId);

    // Por centro de custo
    List<ContaPagar> findByOrganizacaoIdAndCentroCustoId(Long organizacaoId, Long centroCustoId);

    // Totais
    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.status = :status")
    BigDecimal sumValorByOrganizacaoAndStatus(@Param("orgId") Long organizacaoId, @Param("status") ContaPagar.StatusContaPagar status);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.status NOT IN ('PAGA', 'CANCELADA') AND c.dtVencimento < :hoje")
    BigDecimal sumValorVencidasByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(c) FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.status NOT IN ('PAGA', 'CANCELADA') AND c.dtVencimento < :hoje")
    int countVencidasByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(c) FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :hoje AND :dataLimite AND c.status NOT IN ('PAGA', 'CANCELADA')")
    int countAVencerByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje, @Param("dataLimite") LocalDate dataLimite);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :hoje AND :dataLimite AND c.status NOT IN ('PAGA', 'CANCELADA')")
    BigDecimal sumValorAVencerByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje, @Param("dataLimite") LocalDate dataLimite);

    // Pagas por período
    @Query("SELECT COALESCE(SUM(c.valorPago), 0) FROM ContaPagar c WHERE c.organizacao.id = :orgId AND c.dtPagamento BETWEEN :inicio AND :fim AND c.status IN ('PAGA', 'PARCIALMENTE_PAGA')")
    BigDecimal sumValorPagoByPeriodo(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por fornecedor
    @Query("SELECT c FROM ContaPagar c WHERE c.organizacao.id = :orgId AND LOWER(c.fornecedor) LIKE LOWER(CONCAT('%', :fornecedor, '%'))")
    List<ContaPagar> findByOrganizacaoAndFornecedor(@Param("orgId") Long organizacaoId, @Param("fornecedor") String fornecedor);
}
