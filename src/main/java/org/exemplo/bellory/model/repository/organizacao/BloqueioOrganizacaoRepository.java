package org.exemplo.bellory.model.repository.organizacao;

import org.exemplo.bellory.model.entity.organizacao.BloqueioOrganizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloqueioOrganizacaoRepository extends JpaRepository<BloqueioOrganizacao, Long> {

    List<BloqueioOrganizacao> findByOrganizacaoIdOrderByDataInicioAsc(Long organizacaoId);

    List<BloqueioOrganizacao> findByOrganizacaoIdAndAtivoTrueOrderByDataInicioAsc(Long organizacaoId);

    /**
     * Busca bloqueios ativos que cobrem uma data específica
     */
    @Query("SELECT b FROM BloqueioOrganizacao b WHERE b.organizacao.id = :orgId " +
           "AND b.ativo = true AND b.dataInicio <= :data AND b.dataFim >= :data")
    List<BloqueioOrganizacao> findBloqueiosAtivosNaData(
            @Param("orgId") Long organizacaoId,
            @Param("data") LocalDate data);

    /**
     * Busca bloqueios ativos dentro de um período (para exibir no calendário)
     */
    @Query("SELECT b FROM BloqueioOrganizacao b WHERE b.organizacao.id = :orgId " +
           "AND b.ativo = true AND b.dataInicio <= :dataFim AND b.dataFim >= :dataInicio " +
           "ORDER BY b.dataInicio ASC")
    List<BloqueioOrganizacao> findBloqueiosAtivosNoPeriodo(
            @Param("orgId") Long organizacaoId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    /**
     * Verifica se já existe um feriado nacional com o mesmo título e data para o ano
     */
    @Query("SELECT COUNT(b) > 0 FROM BloqueioOrganizacao b WHERE b.organizacao.id = :orgId " +
           "AND b.titulo = :titulo AND b.dataInicio = :dataInicio AND b.anoReferencia = :ano")
    boolean existsFeriadoNacional(
            @Param("orgId") Long organizacaoId,
            @Param("titulo") String titulo,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("ano") Integer ano);

    /**
     * Busca feriados nacionais de um ano específico
     */
    List<BloqueioOrganizacao> findByOrganizacaoIdAndAnoReferenciaOrderByDataInicioAsc(
            Long organizacaoId, Integer anoReferencia);

    /**
     * True quando há ao menos um feriado nacional ATIVO importado no ano informado.
     * Usado pelo onboarding para sugerir importação anual de feriados.
     * Se o admin apagar/desativar todos, a etapa volta a aparecer como pendente.
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM BloqueioOrganizacao b
        WHERE b.organizacao.id = :orgId
          AND b.tipo = org.exemplo.bellory.model.entity.organizacao.TipoBloqueioOrganizacao.FERIADO
          AND b.origem = org.exemplo.bellory.model.entity.organizacao.OrigemBloqueio.NACIONAL
          AND b.anoReferencia = :ano
          AND b.ativo = true
        """)
    boolean existsFeriadoNacionalAtivoNoAno(
            @Param("orgId") Long organizacaoId,
            @Param("ano") Integer anoReferencia);
}
