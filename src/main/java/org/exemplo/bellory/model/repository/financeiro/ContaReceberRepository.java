package org.exemplo.bellory.model.repository.financeiro;

import org.exemplo.bellory.model.entity.financeiro.ContaReceber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContaReceberRepository extends JpaRepository<ContaReceber, Long> {

    Optional<ContaReceber> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<ContaReceber> findByOrganizacaoId(Long organizacaoId);

    // Por status
    List<ContaReceber> findByOrganizacaoIdAndStatus(Long organizacaoId, ContaReceber.StatusContaReceber status);

    // Vencidas
    @Query("SELECT c FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.dtVencimento < :hoje AND c.status NOT IN ('RECEBIDA', 'CANCELADA')")
    List<ContaReceber> findVencidasByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje);

    // A vencer nos próximos N dias
    @Query("SELECT c FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :hoje AND :dataLimite AND c.status NOT IN ('RECEBIDA', 'CANCELADA')")
    List<ContaReceber> findAVencerByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje, @Param("dataLimite") LocalDate dataLimite);

    // Por período
    @Query("SELECT c FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :inicio AND :fim ORDER BY c.dtVencimento")
    List<ContaReceber> findByOrganizacaoAndPeriodo(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por período e status
    @Query("SELECT c FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :inicio AND :fim AND c.status = :status ORDER BY c.dtVencimento")
    List<ContaReceber> findByOrganizacaoAndPeriodoAndStatus(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, @Param("status") ContaReceber.StatusContaReceber status);

    // Por competência
    @Query("SELECT c FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.dtCompetencia BETWEEN :inicio AND :fim ORDER BY c.dtCompetencia")
    List<ContaReceber> findByOrganizacaoAndCompetencia(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por cliente
    List<ContaReceber> findByOrganizacaoIdAndClienteId(Long organizacaoId, Long clienteId);

    // Por categoria
    List<ContaReceber> findByOrganizacaoIdAndCategoriaFinanceiraId(Long organizacaoId, Long categoriaId);

    // Por centro de custo
    List<ContaReceber> findByOrganizacaoIdAndCentroCustoId(Long organizacaoId, Long centroCustoId);

    // Totais
    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.status = :status")
    BigDecimal sumValorByOrganizacaoAndStatus(@Param("orgId") Long organizacaoId, @Param("status") ContaReceber.StatusContaReceber status);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.status NOT IN ('RECEBIDA', 'CANCELADA') AND c.dtVencimento < :hoje")
    BigDecimal sumValorVencidasByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(c) FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.status NOT IN ('RECEBIDA', 'CANCELADA') AND c.dtVencimento < :hoje")
    int countVencidasByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(c) FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :hoje AND :dataLimite AND c.status NOT IN ('RECEBIDA', 'CANCELADA')")
    int countAVencerByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje, @Param("dataLimite") LocalDate dataLimite);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.dtVencimento BETWEEN :hoje AND :dataLimite AND c.status NOT IN ('RECEBIDA', 'CANCELADA')")
    BigDecimal sumValorAVencerByOrganizacao(@Param("orgId") Long organizacaoId, @Param("hoje") LocalDate hoje, @Param("dataLimite") LocalDate dataLimite);

    // Recebidas por período
    @Query("SELECT COALESCE(SUM(c.valorRecebido), 0) FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.dtRecebimento BETWEEN :inicio AND :fim AND c.status IN ('RECEBIDA', 'PARCIALMENTE_RECEBIDA')")
    BigDecimal sumValorRecebidoByPeriodo(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por cliente
    @Query("SELECT c FROM ContaReceber c WHERE c.organizacao.id = :orgId AND c.cliente.id = :clienteId AND c.status NOT IN ('RECEBIDA', 'CANCELADA')")
    List<ContaReceber> findPendentesByCliente(@Param("orgId") Long organizacaoId, @Param("clienteId") Long clienteId);

    // Por cobrança
    Optional<ContaReceber> findByCobrancaIdAndOrganizacaoId(Long cobrancaId, Long organizacaoId);
}
