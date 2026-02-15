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

    // ==================== QUERIES OTIMIZADAS PARA DASHBOARD ====================

    /**
     * Busca cobranças por período e organização (evita filtro em memória)
     */
    @Query("SELECT c FROM Cobranca c WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim")
    List<Cobranca> findByPeriodAndOrganizacao(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Soma receita paga por período e organização
     */
    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.statusCobranca = 'PAGO' " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim")
    BigDecimal sumReceitaPagaByPeriodAndOrganizacao(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Conta cobranças por status e organização no período
     */
    @Query("SELECT c.statusCobranca, COUNT(c) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "GROUP BY c.statusCobranca")
    List<Object[]> countByStatusAndOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Soma valores por status no período
     */
    @Query("SELECT c.statusCobranca, COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "GROUP BY c.statusCobranca")
    List<Object[]> sumValorByStatusAndOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Busca cobranças vencidas por organização
     */
    @Query("SELECT c FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtVencimento < CURRENT_DATE " +
            "AND c.statusCobranca IN ('PENDENTE', 'PARCIALMENTE_PAGO')")
    List<Cobranca> findVencidasByOrganizacao(@Param("organizacaoId") Long organizacaoId);

    /**
     * Soma valor de cobranças vencidas por organização
     */
    @Query("SELECT COALESCE(SUM(c.valorPendente), 0) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtVencimento < CURRENT_DATE " +
            "AND c.statusCobranca IN ('PENDENTE', 'PARCIALMENTE_PAGO')")
    BigDecimal sumValorVencidoByOrganizacao(@Param("organizacaoId") Long organizacaoId);

    /**
     * Soma valor pendente por organização
     */
    @Query("SELECT COALESCE(SUM(c.valorPendente), 0) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.statusCobranca IN ('PENDENTE', 'PARCIALMENTE_PAGO')")
    BigDecimal sumValorPendenteByOrganizacao(@Param("organizacaoId") Long organizacaoId);

    /**
     * Conta cobranças por forma de pagamento (através dos pagamentos)
     */
    @Query("SELECT p.formaPagamento, COUNT(DISTINCT c.id) FROM Cobranca c " +
            "JOIN c.pagamentos p " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "AND p.statusPagamento = 'CONFIRMADO' " +
            "GROUP BY p.formaPagamento")
    List<Object[]> countByFormaPagamentoAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Receita por serviço (através dos agendamentos)
     */
    @Query("SELECT s.nome, COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "JOIN c.agendamento a " +
            "JOIN a.servicos s " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.statusCobranca = 'PAGO' " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "GROUP BY s.id, s.nome " +
            "ORDER BY SUM(c.valor) DESC")
    List<Object[]> sumReceitaByServicoAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Receita por funcionário
     */
    @Query("SELECT f.nomeCompleto, COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "JOIN c.agendamento a " +
            "JOIN a.funcionarios f " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.statusCobranca = 'PAGO' " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "GROUP BY f.id, f.nomeCompleto " +
            "ORDER BY SUM(c.valor) DESC")
    List<Object[]> sumReceitaByFuncionarioAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Receita de serviços vs produtos
     */
    @Query("SELECT c.tipoCobranca, COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.statusCobranca = 'PAGO' " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "GROUP BY c.tipoCobranca")
    List<Object[]> sumReceitaByTipoAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    // ==================== QUERIES PARA RELATÓRIOS ====================

    /**
     * Soma valores por forma de pagamento no período
     */
    @Query("SELECT p.formaPagamento, COALESCE(SUM(p.valor), 0) FROM Cobranca c " +
            "JOIN c.pagamentos p " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "AND p.statusPagamento = 'CONFIRMADO' " +
            "GROUP BY p.formaPagamento")
    List<Object[]> sumValorByFormaPagamentoAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Soma receita por data (para gráfico de evolução)
     */
    @Query(value = "SELECT CAST(c.dt_criacao AS DATE), " +
            "COALESCE(SUM(CASE WHEN c.status_cobranca = 'PAGO' THEN c.valor ELSE 0 END), 0), " +
            "COUNT(c.id) " +
            "FROM app.cobranca c " +
            "WHERE c.organizacao_id = :organizacaoId " +
            "AND c.dt_criacao BETWEEN :inicio AND :fim " +
            "GROUP BY CAST(c.dt_criacao AS DATE) " +
            "ORDER BY CAST(c.dt_criacao AS DATE)", nativeQuery = true)
    List<Object[]> sumReceitaByDataAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Cobranças vencidas detalhadas com dados do cliente
     */
    @Query("SELECT c.id, c.numeroCobranca, c.cliente.nomeCompleto, c.valor, c.valorPendente, " +
            "c.dtVencimento, c.tipoCobranca " +
            "FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.dtVencimento < CURRENT_DATE " +
            "AND c.statusCobranca IN ('PENDENTE', 'PARCIALMENTE_PAGO') " +
            "ORDER BY c.dtVencimento ASC")
    List<Object[]> findCobrancasVencidasDetalhadasByOrganizacao(
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Resumo de sinais por organização e período
     */
    @Query("SELECT c.statusCobranca, COUNT(c), COALESCE(SUM(c.valor), 0) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.subtipoCobrancaAgendamento = 'SINAL' " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "GROUP BY c.statusCobranca")
    List<Object[]> countSinaisByStatusAndOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Média do percentual de sinal por organização
     */
    @Query("SELECT COALESCE(AVG(c.percentualSinal), 0) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.subtipoCobrancaAgendamento = 'SINAL' " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim")
    Double avgPercentualSinalByOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Total de valor gasto por cliente (para LTV)
     */
    @Query("SELECT c.cliente.id, c.cliente.nomeCompleto, c.cliente.telefone, " +
            "COALESCE(SUM(c.valor), 0), COUNT(DISTINCT c.agendamento.id) " +
            "FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.statusCobranca = 'PAGO' " +
            "GROUP BY c.cliente.id, c.cliente.nomeCompleto, c.cliente.telefone " +
            "ORDER BY SUM(c.valor) DESC")
    List<Object[]> findClientesLtvByOrganizacao(
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Soma total de descontos aplicados em cobranças do período
     */
    @Query("SELECT COALESCE(SUM(c.valor - c.valorPago), 0) FROM Cobranca c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.statusCobranca = 'PAGO' " +
            "AND c.dtCriacao BETWEEN :inicio AND :fim " +
            "AND c.valorPago < c.valor")
    BigDecimal sumDescontosAplicadosByOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}
