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

    Page<RespostaQuestionario> findByQuestionarioId(Long questionarioId, Pageable pageable);

    @Query("SELECT r FROM RespostaQuestionario r " +
           "LEFT JOIN FETCH r.respostas rp " +
           "LEFT JOIN FETCH rp.pergunta " +
           "WHERE r.id = :id")
    Optional<RespostaQuestionario> findByIdWithRespostas(@Param("id") Long id);

    boolean existsByQuestionarioIdAndClienteId(Long questionarioId, Long clienteId);

    List<RespostaQuestionario> findByClienteIdOrderByDtRespostaDesc(Long clienteId);

    Optional<RespostaQuestionario> findByQuestionarioIdAndAgendamentoId(Long questionarioId, Long agendamentoId);

    @Query("SELECT r FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.dtResposta BETWEEN :inicio AND :fim " +
           "ORDER BY r.dtResposta DESC")
    List<RespostaQuestionario> findByQuestionarioIdAndPeriodo(
            @Param("questionarioId") Long questionarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT r FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.dtResposta BETWEEN :inicio AND :fim")
    Page<RespostaQuestionario> findByQuestionarioIdAndPeriodoPaged(
            @Param("questionarioId") Long questionarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);

    Long countByQuestionarioId(Long questionarioId);

    @Query("SELECT COUNT(r) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.dtResposta >= :desde")
    Long countByQuestionarioIdAndDtRespostaAfter(
            @Param("questionarioId") Long questionarioId,
            @Param("desde") LocalDateTime desde);

    @Query("SELECT FUNCTION('DATE', r.dtResposta) as data, COUNT(r) as total " +
           "FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.dtResposta >= :desde " +
           "GROUP BY FUNCTION('DATE', r.dtResposta) " +
           "ORDER BY FUNCTION('DATE', r.dtResposta)")
    List<Object[]> countByQuestionarioIdGroupByDay(
            @Param("questionarioId") Long questionarioId,
            @Param("desde") LocalDateTime desde);

    @Query("SELECT AVG(r.tempoPreenchimentoSegundos) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId " +
           "AND r.tempoPreenchimentoSegundos IS NOT NULL")
    Double avgTempoPreenchimento(@Param("questionarioId") Long questionarioId);

    @Query("SELECT MIN(r.dtResposta) FROM RespostaQuestionario r WHERE r.questionario.id = :questionarioId")
    LocalDateTime findPrimeiraResposta(@Param("questionarioId") Long questionarioId);

    @Query("SELECT MAX(r.dtResposta) FROM RespostaQuestionario r WHERE r.questionario.id = :questionarioId")
    LocalDateTime findUltimaResposta(@Param("questionarioId") Long questionarioId);

    @Query("SELECT COUNT(DISTINCT r.clienteId) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId AND r.clienteId IS NOT NULL")
    Long countDistinctClientesByQuestionarioId(@Param("questionarioId") Long questionarioId);

    @Query("SELECT COUNT(DISTINCT r.agendamentoId) FROM RespostaQuestionario r " +
           "WHERE r.questionario.id = :questionarioId AND r.agendamentoId IS NOT NULL")
    Long countDistinctAgendamentosByQuestionarioId(@Param("questionarioId") Long questionarioId);
}
