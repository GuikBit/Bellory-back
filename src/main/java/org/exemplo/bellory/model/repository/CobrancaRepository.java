package org.exemplo.bellory.model.repository;

import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    Optional<Cobranca> findByAgendamento(Agendamento agendamento);

    List<Cobranca> findByCliente(Cliente cliente);

    List<Cobranca> findByStatusCobranca(Status statusCobranca);

    List<Cobranca> findByClienteAndStatusCobranca(Cliente cliente, Status statusCobranca);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c WHERE c.statusCobranca = org.exemplo.bellory.model.entity.agendamento.Status.PAGO AND c.dtCriacao BETWEEN :inicio AND :fim")
    BigDecimal sumReceitaByPeriod(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c WHERE c.statusCobranca = :status AND c.dtCriacao BETWEEN :inicio AND :fim")
    BigDecimal sumReceitaByStatusAndPeriod(@Param("status") Status status,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Cobranca c WHERE c.dtCriacao BETWEEN :inicio AND :fim")
    List<Cobranca> findByPeriod(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c WHERE c.cliente.id = :clienteId AND c.statusCobranca = org.exemplo.bellory.model.entity.agendamento.Status.PAGO")
    BigDecimal sumByCliente(@Param("clienteId") Long clienteId);
}
