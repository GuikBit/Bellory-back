package org.exemplo.bellory.model.repository.agendamento;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // CORRIGIDO: O nome da propriedade é "dtAgendamento"
    Collection<Agendamento> findByClienteAndDtAgendamento(Cliente cliente, LocalDateTime dtAgendamento);

    // CORRIGIDO: O nome da propriedade é "dtAgendamento"
    Collection<Agendamento> findByFuncionariosContainingAndDtAgendamento(Funcionario funcionario, LocalDateTime dtAgendamento);

    List<Agendamento> findByCliente(Cliente cliente);

    List<Agendamento> findByFuncionariosContaining(Funcionario funcionarios);

    List<Agendamento> findByDtAgendamentoBetween(LocalDateTime dtAgendamentoAfter, LocalDateTime dtAgendamentoBefore);

    List<Agendamento> findByStatus(Status status);

    List<Agendamento> findByFuncionariosContainingAndDtAgendamentoBetween(Funcionario funcionarios, LocalDateTime dtAgendamentoAfter, LocalDateTime dtAgendamentoBefore);

    long countByStatus(Status status);


    @Query("SELECT a FROM Agendamento a WHERE a.status NOT IN ('CANCELADO', 'CONCLUIDO') ORDER BY a.dtAgendamento ASC")
    List<Agendamento> findAgendamentosAtivos();

    /**
     * Busca agendamentos de hoje para um funcionário
     */
    @Query("SELECT a FROM Agendamento a JOIN a.funcionarios f WHERE f.id = :funcionarioId " +
            "AND a.dtAgendamento >= :inicioData AND a.dtAgendamento <= :fimData ORDER BY a.dtAgendamento ASC")
    List<Agendamento> findAgendamentosHojePorFuncionario(@Param("funcionarioId") Long funcionarioId,
                                                         @Param("inicioData") LocalDateTime inicioData,
                                                         @Param("fimData") LocalDateTime fimData);

    /**
     * Busca próximos agendamentos de um cliente
     */
    @Query("SELECT a FROM Agendamento a WHERE a.cliente.id = :clienteId " +
            "AND a.dtAgendamento > :dataAtual AND a.status != :statusCancelado " +
            "ORDER BY a.dtAgendamento ASC")
    List<Agendamento> findProximosAgendamentosCliente(@Param("clienteId") Long clienteId,
                                                      @Param("dataAtual") LocalDateTime dataAtual,
                                                      @Param("statusCancelado") Status statusCancelado);

    long countByDtAgendamentoBetween(LocalDateTime dtAgendamentoAfter, LocalDateTime dtAgendamentoBefore);

    // Métodos novos para dashboard
    @Query("SELECT a FROM Agendamento a WHERE a.dtAgendamento BETWEEN :inicio AND :fim")
    List<Agendamento> findByDataRange(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.dtAgendamento BETWEEN :inicio AND :fim")
    Long countByDataRange(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT a FROM Agendamento a JOIN a.funcionarios f WHERE f.id = :funcionarioId AND a.dtAgendamento BETWEEN :inicio AND :fim")
    List<Agendamento> findByFuncionarioAndDataRange(@Param("funcionarioId") Long funcionarioId,
                                                    @Param("inicio") LocalDateTime inicio,
                                                    @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(a) FROM Agendamento a JOIN a.funcionarios f WHERE f.id = :funcionarioId AND a.dtAgendamento BETWEEN :inicio AND :fim")
    Long countByFuncionarioAndDataRange(@Param("funcionarioId") Long funcionarioId,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.status = :status AND a.dtAgendamento BETWEEN :inicio AND :fim")
    Long countByStatusAndDataRange(@Param("status") Status status,
                                   @Param("inicio") LocalDateTime inicio,
                                   @Param("fim") LocalDateTime fim);

    @Query("SELECT a FROM Agendamento a LEFT JOIN FETCH a.servicos WHERE a.dtAgendamento BETWEEN :inicio AND :fim")
    List<Agendamento> findByDataRangeWithServicos(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT a FROM Agendamento a LEFT JOIN FETCH a.funcionarios WHERE a.dtAgendamento BETWEEN :inicio AND :fim")
    List<Agendamento> findByDataRangeWithFuncionarios(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT a FROM Agendamento a WHERE a.dtAgendamento > :agora AND a.status != 'CANCELADO'")
    List<Agendamento> findAgendamentosFuturos(@Param("agora") LocalDateTime agora);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.cliente.id = :clienteId")
    Long countByCliente(@Param("clienteId") Long clienteId);

    @Query("SELECT MAX(a.dtAgendamento) FROM Agendamento a WHERE a.cliente.id = :clienteId")
    LocalDateTime findLastAgendamentoByCliente(@Param("clienteId") Long clienteId);
}
