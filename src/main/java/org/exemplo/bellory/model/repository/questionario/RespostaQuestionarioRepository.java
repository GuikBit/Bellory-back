package org.exemplo.bellory.model.repository.questionario;

import org.exemplo.bellory.model.entity.questionario.RespostaQuestionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RespostaQuestionarioRepository extends JpaRepository<RespostaQuestionario, Long> {

    @Query("SELECT r FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.isDeletado = false")
    Page<RespostaQuestionario> findByQuestionarioId(@Param("questionarioId") Long questionarioId, Pageable pageable);

    /**
     * Carrega resposta com perguntas/respostas SEM filtrar soft-delete: usado pela auditoria
     * que precisa enxergar respostas removidas para fins de comprovacao legal.
     */
    @Query("SELECT r FROM RespostaQuestionario r " +
           "LEFT JOIN FETCH r.respostas rp " +
           "LEFT JOIN FETCH rp.pergunta " +
           "WHERE r.id = :id")
    Optional<RespostaQuestionario> findByIdWithRespostas(@Param("id") Long id);

    @Query("SELECT COUNT(r) > 0 FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.clienteId = :clienteId " +
           "AND r.isDeletado = false")
    boolean existsByQuestionarioIdAndClienteId(@Param("questionarioId") Long questionarioId,
                                               @Param("clienteId") Long clienteId);

    @Query("SELECT DISTINCT r FROM RespostaQuestionario r " +
           "LEFT JOIN FETCH r.respostas rp " +
           "LEFT JOIN FETCH rp.pergunta " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.clienteId = :clienteId " +
           "AND r.isDeletado = false " +
           "ORDER BY r.dtResposta DESC")
    List<RespostaQuestionario> findHistoricoByQuestionarioIdAndClienteId(
            @Param("questionarioId") Long questionarioId,
            @Param("clienteId") Long clienteId);

    @Query("SELECT r FROM RespostaQuestionario r " +
           "WHERE r.clienteId = :clienteId " +
           "AND r.isDeletado = false " +
           "ORDER BY r.dtResposta DESC")
    List<RespostaQuestionario> findByClienteIdOrderByDtRespostaDesc(@Param("clienteId") Long clienteId);

    @Query("SELECT r FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.agendamentoId = :agendamentoId " +
           "AND r.isDeletado = false")
    Optional<RespostaQuestionario> findByQuestionarioIdAndAgendamentoId(
            @Param("questionarioId") Long questionarioId,
            @Param("agendamentoId") Long agendamentoId);

    @Query("SELECT r FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.dtResposta BETWEEN :inicio AND :fim " +
           "AND r.isDeletado = false " +
           "ORDER BY r.dtResposta DESC")
    List<RespostaQuestionario> findByQuestionarioIdAndPeriodo(
            @Param("questionarioId") Long questionarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT r FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.dtResposta BETWEEN :inicio AND :fim " +
           "AND r.isDeletado = false")
    Page<RespostaQuestionario> findByQuestionarioIdAndPeriodoPaged(
            @Param("questionarioId") Long questionarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.isDeletado = false")
    Long countByQuestionarioId(@Param("questionarioId") Long questionarioId);

    @Query("SELECT COUNT(r) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.dtResposta >= :desde " +
           "AND r.isDeletado = false")
    Long countByQuestionarioIdAndDtRespostaAfter(
            @Param("questionarioId") Long questionarioId,
            @Param("desde") LocalDateTime desde);

    @Query("SELECT FUNCTION('DATE', r.dtResposta) as data, COUNT(r) as total " +
           "FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.dtResposta >= :desde " +
           "AND r.isDeletado = false " +
           "GROUP BY FUNCTION('DATE', r.dtResposta) " +
           "ORDER BY FUNCTION('DATE', r.dtResposta)")
    List<Object[]> countByQuestionarioIdGroupByDay(
            @Param("questionarioId") Long questionarioId,
            @Param("desde") LocalDateTime desde);

    @Query(value = "SELECT EXTRACT(HOUR FROM r.dt_resposta)::int as hora, COUNT(*) as total " +
                   "FROM app.resposta_questionario r " +
                   "WHERE r.questionario_id = :questionarioId " +
                   "AND r.is_deletado = false " +
                   "GROUP BY EXTRACT(HOUR FROM r.dt_resposta) " +
                   "ORDER BY EXTRACT(HOUR FROM r.dt_resposta)",
           nativeQuery = true)
    List<Object[]> countByQuestionarioIdGroupByHour(@Param("questionarioId") Long questionarioId);

    @Query("SELECT AVG(r.tempoPreenchimentoSegundos) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.tempoPreenchimentoSegundos IS NOT NULL " +
           "AND r.isDeletado = false")
    Double avgTempoPreenchimento(@Param("questionarioId") Long questionarioId);

    @Query("SELECT MIN(r.dtResposta) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.isDeletado = false")
    LocalDateTime findPrimeiraResposta(@Param("questionarioId") Long questionarioId);

    @Query("SELECT MAX(r.dtResposta) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.isDeletado = false")
    LocalDateTime findUltimaResposta(@Param("questionarioId") Long questionarioId);

    @Query("SELECT COUNT(DISTINCT r.clienteId) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.clienteId IS NOT NULL " +
           "AND r.isDeletado = false")
    Long countDistinctClientesByQuestionarioId(@Param("questionarioId") Long questionarioId);

    @Query("SELECT COUNT(DISTINCT r.agendamentoId) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.agendamentoId IS NOT NULL " +
           "AND r.isDeletado = false")
    Long countDistinctAgendamentosByQuestionarioId(@Param("questionarioId") Long questionarioId);
}
