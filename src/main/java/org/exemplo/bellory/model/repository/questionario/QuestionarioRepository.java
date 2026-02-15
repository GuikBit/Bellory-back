package org.exemplo.bellory.model.repository.questionario;

import org.exemplo.bellory.model.entity.questionario.Questionario;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionarioRepository extends JpaRepository<Questionario, Long> {

    List<Questionario> findByOrganizacao_IdAndIsDeletadoFalseAndAtivoTrueOrderByDtCriacaoDesc(Long organizacaoId);

    List<Questionario> findByOrganizacao_IdAndIsDeletadoFalseOrderByDtCriacaoDesc(Long organizacaoId);

    Page<Questionario> findByOrganizacao_IdAndIsDeletadoFalse(Long organizacaoId, Pageable pageable);

    List<Questionario> findByOrganizacao_IdAndTipoAndIsDeletadoFalse(Long organizacaoId, TipoQuestionario tipo);

    Optional<Questionario> findByIdAndOrganizacao_IdAndIsDeletadoFalse(Long id, Long organizacaoId);

    @Query("SELECT q FROM Questionario q " +
           "LEFT JOIN FETCH q.perguntas p " +
           "LEFT JOIN FETCH p.opcoes " +
           "WHERE q.id = :id AND q.isDeletado = false")
    Optional<Questionario> findByIdWithPerguntas(@Param("id") Long id);

    @Query("SELECT q FROM Questionario q " +
           "LEFT JOIN FETCH q.perguntas p " +
           "LEFT JOIN FETCH p.opcoes " +
           "WHERE q.id = :id AND q.organizacao.id = :orgId AND q.isDeletado = false")
    Optional<Questionario> findByIdAndOrganizacaoIdWithPerguntas(
            @Param("id") Long id,
            @Param("orgId") Long organizacaoId);

    @Query("SELECT COUNT(r) FROM RespostaQuestionario r WHERE r.questionario.id = :questionarioId")
    Long countRespostasByQuestionarioId(@Param("questionarioId") Long questionarioId);

    @Query("SELECT q FROM Questionario q WHERE q.organizacao.id = :orgId AND q.isDeletado = false AND " +
           "(LOWER(q.titulo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(q.descricao) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Questionario> searchByTituloOrDescricao(
            @Param("orgId") Long organizacaoId,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByIdAndOrganizacao_IdAndIsDeletadoFalse(Long id, Long organizacaoId);
}
