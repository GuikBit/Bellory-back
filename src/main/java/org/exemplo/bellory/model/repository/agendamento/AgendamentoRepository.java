package org.exemplo.bellory.model.repository.agendamento;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface  AgendamentoRepository extends JpaRepository<Agendamento, Long> {


    Collection<Object> findByClienteAndDataHoraAgendamento(Cliente cliente, LocalDateTime dataHoraAgendamento);

    Collection<Object> findByFuncionarioAndDtAgendamento(Funcionario funcionario1, LocalDateTime dataHoraAgendamento);
}
