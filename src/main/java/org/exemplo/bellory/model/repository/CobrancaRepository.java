package org.exemplo.bellory.model.repository;

import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    /**
     * Busca cobrança por agendamento
     */
    Optional<Cobranca> findByAgendamento(Agendamento agendamento);

    /**
     * Busca todas as cobranças de um cliente
     */
    List<Cobranca> findByCliente(Cliente cliente);

    /**
     * Busca cobranças por status
     */
    List<Cobranca> findByStatusCobranca(String status);

    /**
     * Busca cobranças pendentes de um cliente
     */
    List<Cobranca> findByClienteAndStatusCobranca(Cliente cliente, String status);
}
