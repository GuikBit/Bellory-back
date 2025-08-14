package org.exemplo.bellory.model.repository;

import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    Optional<Cobranca> findByAgendamento(Agendamento agendamento);

    List<Cobranca> findByCliente(Cliente cliente);

    List<Cobranca> findByStatusCobranca(Status statusCobranca);

    List<Cobranca> findByClienteAndStatusCobranca(Cliente cliente, Status statusCobranca);
}
