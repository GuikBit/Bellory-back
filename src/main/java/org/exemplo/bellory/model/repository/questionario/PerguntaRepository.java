package org.exemplo.bellory.model.repository.questionario;

import org.exemplo.bellory.model.entity.questionario.Pergunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerguntaRepository extends JpaRepository<Pergunta, Long> {

    List<Pergunta> findByQuestionarioIdOrderByOrdemAsc(Long questionarioId);

    @Query("SELECT p FROM Pergunta p LEFT JOIN FETCH p.opcoes WHERE p.questionario.id = :questionarioId ORDER BY p.ordem")
    List<Pergunta> findByQuestionarioIdWithOpcoes(@Param("questionarioId") Long questionarioId);

    void deleteByQuestionarioId(Long questionarioId);
}
