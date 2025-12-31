package org.exemplo.bellory.model.repository.Transacao;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    // === QUERIES BÁSICAS ===

    Optional<Cobranca> findByAgendamento(Agendamento agendamento);

    List<Cobranca> findByCliente(Cliente cliente);

    List<Cobranca> findByClienteId(Long clienteId);

    List<Cobranca> findByStatusCobranca(Cobranca.StatusCobranca statusCobranca);

    List<Cobranca> findByClienteAndStatusCobranca(Cliente cliente, Cobranca.StatusCobranca statusCobranca);

    List<Cobranca> findByClienteIdAndStatusCobrancaIn(Long clienteId, List<Cobranca.StatusCobranca> status);

    // === QUERIES PARA AGENDAMENTO ===

    List<Cobranca> findByAgendamentoId(Long agendamentoId);

    // NOVO: Buscar cobrança de sinal por agendamento
    @Query("SELECT c FROM Cobranca c WHERE c.agendamento.id = :agendamentoId " +
            "AND c.subtipoCobrancaAgendamento = 'SINAL'")
    Optional<Cobranca> findCobrancaSinalByAgendamentoId(@Param("agendamentoId") Long agendamentoId);

    // NOVO: Buscar cobrança do restante por agendamento
    @Query("SELECT c FROM Cobranca c WHERE c.agendamento.id = :agendamentoId " +
            "AND c.subtipoCobrancaAgendamento = 'RESTANTE'")
    Optional<Cobranca> findCobrancaRestanteByAgendamentoId(@Param("agendamentoId") Long agendamentoId);

    // NOVO: Buscar cobrança integral por agendamento
    @Query("SELECT c FROM Cobranca c WHERE c.agendamento.id = :agendamentoId " +
            "AND c.subtipoCobrancaAgendamento = 'INTEGRAL'")
    Optional<Cobranca> findCobrancaIntegralByAgendamentoId(@Param("agendamentoId") Long agendamentoId);

    // === QUERIES DE RECEITA E PERÍODO ===

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "WHERE c.statusCobranca = :status1 " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim")
    BigDecimal sumReceitaByPeriod(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status1") Cobranca.StatusCobranca status1
    );

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "WHERE c.statusCobranca = :status " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim")
    BigDecimal sumReceitaByStatusAndPeriod(
            @Param("status") Cobranca.StatusCobranca status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Query("SELECT c FROM Cobranca c WHERE c.dtCriacao BETWEEN :inicio AND :fim")
    List<Cobranca> findByPeriod(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "WHERE c.cliente.id = :clienteId " +
            "AND c.statusCobranca = :status1")
    BigDecimal sumByCliente(
            @Param("clienteId") Long clienteId,
            @Param("status1") Cobranca.StatusCobranca status1
    );

    // === QUERIES DE COBRANÇAS VENCIDAS ===

    @Query("SELECT c FROM Cobranca c " +
            "WHERE c.dtVencimento < CURRENT_DATE " +
            "AND c.statusCobranca IN (:status1, :status2)")
    List<Cobranca> findVencidas(
            @Param("status1") Cobranca.StatusCobranca status1,
            @Param("status2") Cobranca.StatusCobranca status2
    );

    // NOVO: Buscar cobranças vencidas (simplificado)
    @Query("SELECT c FROM Cobranca c " +
            "WHERE c.dtVencimento < CURRENT_DATE " +
            "AND c.statusCobranca IN ('PENDENTE', 'PARCIALMENTE_PAGO')")
    List<Cobranca> findCobrancasVencidas();

    // === QUERIES POR ORGANIZAÇÃO ===

    List<Cobranca> findByOrganizacaoId(Long organizacaoId);

    List<Cobranca> findByOrganizacaoIdAndTipoCobranca(
            Long organizacaoId,
            Cobranca.TipoCobranca tipoCobranca
    );

    // NOVO: Buscar total de valores pendentes por organização
    @Query("SELECT SUM(c.valorPendente) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.statusCobranca IN ('PENDENTE', 'PARCIALMENTE_PAGO')")
    BigDecimal findTotalValorPendenteByOrganizacaoId(@Param("organizacaoId") Long organizacaoId);

    // === QUERIES PARA GATEWAY DE PAGAMENTO ===

    // NOVO: Buscar cobrança pelo PaymentIntent do Stripe (para integração futura)
    Optional<Cobranca> findByGatewayPaymentIntentId(String paymentIntentId);

    // === QUERIES AVANÇADAS ===

    // NOVO: Buscar cobranças com vencimento próximo (útil para notificações)
    @Query("SELECT c FROM Cobranca c " +
            "WHERE c.dtVencimento BETWEEN :dataInicio AND :dataFim " +
            "AND c.statusCobranca IN ('PENDENTE', 'PARCIALMENTE_PAGO') " +
            "AND c.organizacao.id = :organizacaoId")
    List<Cobranca> findCobrancasComVencimentoProximo(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("organizacaoId") Long organizacaoId
    );

    // NOVO: Buscar cobranças de um período específico
    @Query("SELECT c FROM Cobranca c " +
            "WHERE c.dtCriacao BETWEEN :dataInicio AND :dataFim " +
            "AND c.organizacao.id = :organizacaoId")
    List<Cobranca> findCobrancasPorPeriodo(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("organizacaoId") Long organizacaoId
    );

    // NOVO: Buscar todas as cobranças relacionadas (sinal + restante)
    @Query("SELECT c FROM Cobranca c " +
            "WHERE c.id = :cobrancaId OR c.cobrancaRelacionada.id = :cobrancaId")
    List<Cobranca> findCobrancasRelacionadas(@Param("cobrancaId") Long cobrancaId);

    // NOVO: Buscar sinais pagos de uma organização
    @Query("SELECT c FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.subtipoCobrancaAgendamento = 'SINAL' " +
            "AND c.statusCobranca = 'PAGO'")
    List<Cobranca> findSinaisPagos(@Param("organizacaoId") Long organizacaoId);

    // NOVO: Estatísticas de cobranças por tipo
    @Query("SELECT c.tipoCobranca, COUNT(c), SUM(c.valor), SUM(c.valorPago) " +
            "FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtCriacao BETWEEN :dataInicio AND :dataFim " +
            "GROUP BY c.tipoCobranca")
    List<Object[]> findEstatisticasPorTipo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );
}
