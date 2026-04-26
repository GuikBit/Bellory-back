package org.exemplo.bellory.model.repository.agendamento;

import org.exemplo.bellory.model.entity.agendamento.AgendamentoQuestionario;
import org.exemplo.bellory.model.entity.agendamento.StatusQuestionarioAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoQuestionarioRepository extends JpaRepository<AgendamentoQuestionario, Long> {

    Optional<AgendamentoQuestionario> findByAgendamentoIdAndQuestionarioId(Long agendamentoId, Long questionarioId);

    List<AgendamentoQuestionario> findByAgendamentoIdAndStatus(Long agendamentoId, StatusQuestionarioAgendamento status);
}
