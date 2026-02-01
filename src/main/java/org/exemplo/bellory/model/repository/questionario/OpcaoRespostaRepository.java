package org.exemplo.bellory.model.repository.questionario;

import org.exemplo.bellory.model.entity.questionario.OpcaoResposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpcaoRespostaRepository extends JpaRepository<OpcaoResposta, Long> {

    List<OpcaoResposta> findByPerguntaIdOrderByOrdemAsc(Long perguntaId);

    void deleteByPerguntaId(Long perguntaId);
}
