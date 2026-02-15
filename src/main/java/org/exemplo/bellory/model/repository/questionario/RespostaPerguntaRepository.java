package org.exemplo.bellory.model.repository.questionario;

import org.exemplo.bellory.model.entity.questionario.RespostaPergunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RespostaPerguntaRepository extends JpaRepository<RespostaPergunta, Long> {

    List<RespostaPergunta> findByPerguntaId(Long perguntaId);

    @Query("SELECT AVG(rp.respostaNumero), MIN(rp.respostaNumero), MAX(rp.respostaNumero), " +
           "STDDEV(rp.respostaNumero) " +
           "FROM RespostaPergunta rp " +
           "WHERE rp.pergunta.id = :perguntaId " +
           "AND rp.respostaNumero IS NOT NULL")
    Object[] getEstatisticasNumericas(@Param("perguntaId") Long perguntaId);

    @Query("SELECT CAST(rp.respostaNumero AS integer), COUNT(rp) " +
           "FROM RespostaPergunta rp " +
           "WHERE rp.pergunta.id = :perguntaId " +
           "AND rp.respostaNumero IS NOT NULL " +
           "GROUP BY CAST(rp.respostaNumero AS integer) " +
           "ORDER BY CAST(rp.respostaNumero AS integer)")
    List<Object[]> getDistribuicaoNotas(@Param("perguntaId") Long perguntaId);

    @Query(value = "SELECT ros.opcao_id, COUNT(*) " +
                   "FROM app.resposta_opcoes_selecionadas ros " +
                   "INNER JOIN app.resposta_pergunta rp ON rp.id = ros.resposta_pergunta_id " +
                   "WHERE rp.pergunta_id = :perguntaId " +
                   "GROUP BY ros.opcao_id",
           nativeQuery = true)
    List<Object[]> countOpcoesSelecionadas(@Param("perguntaId") Long perguntaId);

    @Query("SELECT rp.respostaTexto, COUNT(rp) " +
           "FROM RespostaPergunta rp " +
           "WHERE rp.pergunta.id = :perguntaId " +
           "AND rp.respostaTexto IN ('Sim', 'Não') " +
           "GROUP BY rp.respostaTexto")
    List<Object[]> countSimNao(@Param("perguntaId") Long perguntaId);

    @Query("SELECT AVG(LENGTH(rp.respostaTexto)) " +
           "FROM RespostaPergunta rp " +
           "WHERE rp.pergunta.id = :perguntaId " +
           "AND rp.respostaTexto IS NOT NULL")
    Double avgCaracteresTexto(@Param("perguntaId") Long perguntaId);

    @Query("SELECT COUNT(rp) FROM RespostaPergunta rp " +
           "WHERE rp.pergunta.id = :perguntaId " +
           "AND (rp.respostaTexto IS NULL OR rp.respostaTexto = '') " +
           "AND rp.respostaNumero IS NULL " +
           "AND (rp.respostaOpcaoIds IS NULL OR SIZE(rp.respostaOpcaoIds) = 0) " +
           "AND rp.respostaData IS NULL " +
           "AND rp.respostaHora IS NULL")
    Long countRespostasEmBranco(@Param("perguntaId") Long perguntaId);

    Long countByPerguntaId(Long perguntaId);
}
