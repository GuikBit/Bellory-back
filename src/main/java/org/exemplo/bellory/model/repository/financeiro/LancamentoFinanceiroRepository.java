package org.exemplo.bellory.model.repository.financeiro;

import org.exemplo.bellory.model.entity.financeiro.LancamentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, Long> {

    Optional<LancamentoFinanceiro> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    List<LancamentoFinanceiro> findByOrganizacaoId(Long organizacaoId);

    // Por tipo
    List<LancamentoFinanceiro> findByOrganizacaoIdAndTipo(Long organizacaoId, LancamentoFinanceiro.TipoLancamento tipo);

    // Por período (dt_lancamento)
    @Query("SELECT l FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO' ORDER BY l.dtLancamento")
    List<LancamentoFinanceiro> findEfetivadosByPeriodo(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Todos por período
    @Query("SELECT l FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.dtLancamento BETWEEN :inicio AND :fim ORDER BY l.dtLancamento DESC")
    List<LancamentoFinanceiro> findByOrganizacaoAndPeriodo(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por competência
    @Query("SELECT l FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.dtCompetencia BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO' ORDER BY l.dtCompetencia")
    List<LancamentoFinanceiro> findEfetivadosByCompetencia(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por tipo e período
    @Query("SELECT l FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.tipo = :tipo AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO' ORDER BY l.dtLancamento")
    List<LancamentoFinanceiro> findByTipoAndPeriodo(@Param("orgId") Long organizacaoId, @Param("tipo") LancamentoFinanceiro.TipoLancamento tipo, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por categoria e período
    @Query("SELECT l FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.categoriaFinanceira.id = :catId AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO' ORDER BY l.dtLancamento")
    List<LancamentoFinanceiro> findByCategoriaAndPeriodo(@Param("orgId") Long organizacaoId, @Param("catId") Long categoriaId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por centro de custo e período
    @Query("SELECT l FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.centroCusto.id = :ccId AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO' ORDER BY l.dtLancamento")
    List<LancamentoFinanceiro> findByCentroCustoAndPeriodo(@Param("orgId") Long organizacaoId, @Param("ccId") Long centroCustoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Por conta bancária e período
    @Query("SELECT l FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.contaBancaria.id = :contaId AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO' ORDER BY l.dtLancamento")
    List<LancamentoFinanceiro> findByContaBancariaAndPeriodo(@Param("orgId") Long organizacaoId, @Param("contaId") Long contaBancariaId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Soma receitas por período
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.tipo = 'RECEITA' AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO'")
    BigDecimal sumReceitasByPeriodo(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Soma despesas por período
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.tipo = 'DESPESA' AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO'")
    BigDecimal sumDespesasByPeriodo(@Param("orgId") Long organizacaoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Soma receitas por categoria e período
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.tipo = 'RECEITA' AND l.categoriaFinanceira.id = :catId AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO'")
    BigDecimal sumReceitasByCategoriaAndPeriodo(@Param("orgId") Long organizacaoId, @Param("catId") Long categoriaId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Soma despesas por categoria e período
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.tipo = 'DESPESA' AND l.categoriaFinanceira.id = :catId AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO'")
    BigDecimal sumDespesasByCategoriaAndPeriodo(@Param("orgId") Long organizacaoId, @Param("catId") Long categoriaId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Contagem por tipo e período
    @Query("SELECT COUNT(l) FROM LancamentoFinanceiro l WHERE l.organizacao.id = :orgId AND l.tipo = :tipo AND l.dtLancamento BETWEEN :inicio AND :fim AND l.status = 'EFETIVADO'")
    long countByTipoAndPeriodo(@Param("orgId") Long organizacaoId, @Param("tipo") LancamentoFinanceiro.TipoLancamento tipo, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
