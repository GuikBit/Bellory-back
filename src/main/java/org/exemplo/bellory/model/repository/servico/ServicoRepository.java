package org.exemplo.bellory.model.repository.servico;

import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<ServicoAgendamento> findAllProjectedBy();

    boolean existsByNome(String nome);

    List<Servico> findAllByOrderByNomeAsc();
}
