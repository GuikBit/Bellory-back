package org.exemplo.bellory.model.repository.agendamento;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO;
import org.exemplo.bellory.model.dto.notificacao.NotificacaoSemInstanciaDTO;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // CORRIGIDO: O nome da propriedade é "dtAgendamento"
    Collection<Agendamento> findByClienteAndDtAgendamento(Cliente cliente, LocalDateTime dtAgendamento);
    List<Agendamento> findAllByClienteOrganizacaoId(Long organizacaoId);
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

    Optional<Agendamento> findByIdAndClienteOrganizacaoId(Long id, Long organizacaoId);

    /**
     * Busca agendamentos por cliente e organização
     */
    List<Agendamento> findByClienteIdAndClienteOrganizacaoId(Long clienteId, Long organizacaoId);

    /**
     * Busca agendamentos por funcionário e organização
     */
    @Query("SELECT a FROM Agendamento a " +
            "JOIN a.funcionarios f " +
            "WHERE f.id = :funcionarioId " +
            "AND a.cliente.organizacao.id = :organizacaoId")
    List<Agendamento> findByFuncionariosIdAndClienteOrganizacaoId(
            @Param("funcionarioId") Long funcionarioId,
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Busca agendamentos por data e organização
     */
    List<Agendamento> findByDtAgendamentoBetweenAndClienteOrganizacaoId(
            LocalDateTime inicio,
            LocalDateTime fim,
            Long organizacaoId
    );

    /**
     * Busca agendamentos por status e organização
     */
    @Query("SELECT a FROM Agendamento a " +
            "WHERE a.status = :status " +
            "AND a.cliente.organizacao.id = :organizacaoId")
    List<Agendamento> findByStatusAndOrganizacaoId(
            @Param("status") String status,
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Busca agendamentos com cobranças pendentes por organização
     */
//    @Query("SELECT a FROM Agendamento a " +
//            "JOIN a.cobranca c " +
//            "WHERE c.statusCobranca = 'PENDENTE' " +
//            "AND a.cliente.organizacao.id = :organizacaoId")
//    List<Agendamento> findComCobrancasPendentesByOrganizacaoId(@Param("organizacaoId") Long organizacaoId);

    /**
     * Busca agendamentos vencidos por organização
     */
//    @Query("SELECT a FROM Agendamento a " +
//            "JOIN a.cobranca c " +
//            "WHERE c.statusCobranca = 'PENDENTE' " +
//            "AND c.dtVencimento < :dataAtual " +
//            "AND a.cliente.organizacao.id = :organizacaoId")
//    List<Agendamento> findVencidosByOrganizacaoId(
//            @Param("dataAtual") LocalDateTime dataAtual,
//            @Param("organizacaoId") Long organizacaoId
//    );

    /**
     * Busca agendamentos por funcionário e data
     */
    @Query("SELECT a FROM Agendamento a " +
            "JOIN a.funcionarios f " +
            "WHERE f.id = :funcionarioId " +
            "AND DATE(a.dtAgendamento) = DATE(:data) " +
            "AND a.cliente.organizacao.id = :organizacaoId " +
            "ORDER BY a.dtAgendamento")
    List<Agendamento> findByFuncionarioAndDataAndOrganizacao(
            @Param("funcionarioId") Long funcionarioId,
            @Param("data") LocalDateTime data,
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Busca próximos agendamentos (7 dias) por organização
     */
    @Query("SELECT a FROM Agendamento a " +
            "WHERE a.dtAgendamento BETWEEN :inicio AND :fim " +
            "AND a.cliente.organizacao.id = :organizacaoId " +
            "AND a.status NOT IN ('CANCELADO', 'CONCLUIDO') " +
            "ORDER BY a.dtAgendamento")
    List<Agendamento> findProximosAgendamentosByOrganizacaoId(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Conta agendamentos por status e organização
     */
    @Query("SELECT COUNT(a) FROM Agendamento a " +
            "WHERE a.status = :status " +
            "AND a.cliente.organizacao.id = :organizacaoId")
    Long countByStatusAndOrganizacaoId(
            @Param("status") String status,
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Verifica disponibilidade de horário para um funcionário na organização
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Agendamento a " +
            "JOIN a.funcionarios f " +
            "WHERE f.id = :funcionarioId " +
            "AND a.dtAgendamento = :dataHora " +
            "AND a.status NOT IN ('CANCELADO') " +
            "AND a.cliente.organizacao.id = :organizacaoId")
    boolean existsByFuncionarioAndDataHoraAndOrganizacao(
            @Param("funcionarioId") Long funcionarioId,
            @Param("dataHora") LocalDateTime dataHora,
            @Param("organizacaoId") Long organizacaoId
    );

    List<Agendamento> findByOrganizacaoId(Long organizacaoId);

    List<Agendamento> findByClienteId(Long clienteId);

    List<Agendamento> findByOrganizacaoIdAndStatus(Long organizacaoId, Status status);

    // ==================== QUERIES OTIMIZADAS PARA DASHBOARD ====================

    /**
     * Busca agendamentos por organização e período com cliente
     * Nota: Não pode fazer JOIN FETCH em múltiplas coleções (bags) simultaneamente
     */
    @Query("SELECT DISTINCT a FROM Agendamento a " +
            "LEFT JOIN FETCH a.cliente " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim")
    List<Agendamento> findByOrganizacaoAndPeriodoWithDetails(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta agendamentos por status e organização no período
     */
    @Query("SELECT a.status, COUNT(a) FROM Agendamento a " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim " +
            "GROUP BY a.status")
    List<Object[]> countByStatusAndOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta agendamentos por organização no período
     */
    @Query("SELECT COUNT(a) FROM Agendamento a " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim")
    Long countByOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta atendimentos por funcionário no período
     */
    @Query("SELECT f.id, f.nomeCompleto, COUNT(a) FROM Agendamento a " +
            "JOIN a.funcionarios f " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim " +
            "GROUP BY f.id, f.nomeCompleto " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> countByFuncionarioAndOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta serviços mais vendidos no período
     */
    @Query("SELECT s.id, s.nome, COUNT(a) FROM Agendamento a " +
            "JOIN a.servicos s " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim " +
            "AND a.status NOT IN ('CANCELADO') " +
            "GROUP BY s.id, s.nome " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> countServicosMaisVendidosByOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Busca agendamentos futuros com detalhes (para receita prevista)
     */
    @Query("SELECT a FROM Agendamento a " +
            "LEFT JOIN FETCH a.servicos " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento > :agora " +
            "AND a.status NOT IN ('CANCELADO', 'CONCLUIDO')")
    List<Agendamento> findAgendamentosFuturosByOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("agora") LocalDateTime agora
    );

    /**
     * Conta clientes distintos com agendamentos no período
     */
    @Query("SELECT COUNT(DISTINCT a.cliente.id) FROM Agendamento a " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim")
    Long countClientesDistintosComAgendamentos(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta clientes recorrentes (mais de 1 agendamento no período)
     */
    @Query("SELECT COUNT(DISTINCT a.cliente.id) FROM Agendamento a " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim " +
            "AND a.cliente.id IN (" +
            "   SELECT a2.cliente.id FROM Agendamento a2 " +
            "   WHERE a2.organizacao.id = :organizacaoId " +
            "   AND a2.dtAgendamento BETWEEN :inicio AND :fim " +
            "   GROUP BY a2.cliente.id " +
            "   HAVING COUNT(a2.id) > 1" +
            ")")
    Long countClientesRecorrentesByOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Busca clientes inativos (sem agendamentos após data limite) - EVITA N+1
     */
    @Query("SELECT COUNT(c.id) FROM Cliente c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.ativo = true " +
            "AND c.id NOT IN (" +
            "   SELECT DISTINCT a.cliente.id FROM Agendamento a " +
            "   WHERE a.organizacao.id = :organizacaoId " +
            "   AND a.dtAgendamento >= :dataLimite" +
            ")")
    Long countClientesInativos(
            @Param("organizacaoId") Long organizacaoId,
            @Param("dataLimite") LocalDateTime dataLimite
    );

    /**
     * Vendas (agendamentos concluídos e pagos) no período
     */
    @Query("SELECT COUNT(a) FROM Agendamento a " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim " +
            "AND a.status = 'CONCLUIDO'")
    Long countVendasByOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta vendas por categoria de serviço
     */
    @Query("SELECT s.categoria.label, COUNT(DISTINCT a.id) FROM Agendamento a " +
            "JOIN a.servicos s " +
            "WHERE a.organizacao.id = :organizacaoId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim " +
            "AND a.status = 'CONCLUIDO' " +
            "GROUP BY s.categoria.id, s.categoria.label")
    List<Object[]> countVendasByCategoriaAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    // ==================== QUERIES PARA SISTEMA DE NOTIFICACOES ====================

    /**
     * Query principal do sistema de notificacoes.
     * Busca agendamentos que precisam de notificacao AGORA baseado
     * na configuracao dinamica de cada organizacao.
     */
    @Query("""
        SELECT new org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO(
            a.id,
            a.dtAgendamento,
            c.nomeCompleto,
            c.telefone,
            o.id,
            o.nomeFantasia,
            cn.tipo,
            cn.horasAntes,
            cn.mensagemTemplate,
            i.instanceName
        )
        FROM Agendamento a
        JOIN a.cliente c
        JOIN a.organizacao o
        JOIN ConfigNotificacao cn ON cn.organizacao = o AND cn.ativo = true
        JOIN Instance i ON i.organizacao = o
        WHERE a.status = 'AGENDADO'
          AND a.dtAgendamento > :agora
          AND i.status = 'CONNECTED'
          AND FUNCTION('TIMESTAMPADD', HOUR, -cn.horasAntes, a.dtAgendamento)
              BETWEEN :inicioJanela AND :fimJanela
          AND NOT EXISTS (
              SELECT 1 FROM NotificacaoEnviada ne
              WHERE ne.agendamento = a
                AND ne.tipo = cn.tipo
                AND ne.horasAntes = cn.horasAntes
          )
        ORDER BY o.id, a.dtAgendamento ASC
        """)
    List<NotificacaoPendenteDTO> findNotificacoesPendentes(
        @Param("agora") LocalDateTime agora,
        @Param("inicioJanela") LocalDateTime inicioJanela,
        @Param("fimJanela") LocalDateTime fimJanela
    );

    /**
     * Busca notificacoes sem instancia WhatsApp conectada (para alertar).
     */
    @Query("""
        SELECT new org.exemplo.bellory.model.dto.notificacao.NotificacaoSemInstanciaDTO(
            a.id, o.id, o.nomeFantasia, o.emailPrincipal, cn.tipo, cn.horasAntes, a.dtAgendamento
        )
        FROM Agendamento a
        JOIN a.cliente c
        JOIN a.organizacao o
        JOIN ConfigNotificacao cn ON cn.organizacao = o AND cn.ativo = true
        LEFT JOIN Instance i ON i.organizacao = o
        WHERE a.status = 'AGENDADO'
          AND a.dtAgendamento > :agora
          AND (i IS NULL OR i.status != 'CONNECTED')
          AND FUNCTION('TIMESTAMPADD', HOUR, -cn.horasAntes, a.dtAgendamento)
              BETWEEN :inicioJanela AND :fimJanela
          AND NOT EXISTS (
              SELECT 1 FROM NotificacaoEnviada ne
              WHERE ne.agendamento = a AND ne.tipo = cn.tipo AND ne.horasAntes = cn.horasAntes
          )
        ORDER BY o.id
        """)
    List<NotificacaoSemInstanciaDTO> findNotificacoesSemInstanciaConectada(
        @Param("agora") LocalDateTime agora,
        @Param("inicioJanela") LocalDateTime inicioJanela,
        @Param("fimJanela") LocalDateTime fimJanela
    );

    // ==================== QUERIES SEPARADAS POR TIPO DE NOTIFICACAO ====================

    /**
     * Busca notificacoes pendentes de CONFIRMACAO.
     * CONFIRMACAO: 12/24/36/48 horas antes do agendamento.
     * Filtra apenas configuracoes do tipo CONFIRMACAO que estao ativas.
     */
    @Query("""
        SELECT new org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO(
            a.id,
            a.dtAgendamento,
            c.nomeCompleto,
            c.telefone,
            o.id,
            o.nomeFantasia,
            cn.tipo,
            cn.horasAntes,
            cn.mensagemTemplate,
            i.instanceName
        )
        FROM Agendamento a
        JOIN a.cliente c
        JOIN a.organizacao o
        JOIN ConfigNotificacao cn ON cn.organizacao = o AND cn.ativo = true AND cn.tipo = 'CONFIRMACAO'
        JOIN Instance i ON i.organizacao = o
        WHERE a.status = 'AGENDADO'
          AND a.dtAgendamento > :agora
          AND i.status = 'CONNECTED'
          AND FUNCTION('TIMESTAMPADD', HOUR, -cn.horasAntes, a.dtAgendamento)
              BETWEEN :inicioJanela AND :fimJanela
          AND NOT EXISTS (
              SELECT 1 FROM NotificacaoEnviada ne
              WHERE ne.agendamento = a
                AND ne.tipo = cn.tipo
                AND ne.horasAntes = cn.horasAntes
          )
        ORDER BY o.id, a.dtAgendamento ASC
        """)
    List<NotificacaoPendenteDTO> findConfirmacoesPendentes(
        @Param("agora") LocalDateTime agora,
        @Param("inicioJanela") LocalDateTime inicioJanela,
        @Param("fimJanela") LocalDateTime fimJanela
    );

    /**
     * Busca notificacoes pendentes de LEMBRETE.
     * LEMBRETE: 1/2/3/4/5/6 horas antes do agendamento.
     * Filtra apenas configuracoes do tipo LEMBRETE que estao ativas.
     */
    @Query("""
        SELECT new org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO(
            a.id,
            a.dtAgendamento,
            c.nomeCompleto,
            c.telefone,
            o.id,
            o.nomeFantasia,
            cn.tipo,
            cn.horasAntes,
            cn.mensagemTemplate,
            i.instanceName
        )
        FROM Agendamento a
        JOIN a.cliente c
        JOIN a.organizacao o
        JOIN ConfigNotificacao cn ON cn.organizacao = o AND cn.ativo = true AND cn.tipo = 'LEMBRETE'
        JOIN Instance i ON i.organizacao = o
        WHERE a.status = 'AGENDADO'
          AND a.dtAgendamento > :agora
          AND i.status = 'CONNECTED'
          AND FUNCTION('TIMESTAMPADD', HOUR, -cn.horasAntes, a.dtAgendamento)
              BETWEEN :inicioJanela AND :fimJanela
          AND NOT EXISTS (
              SELECT 1 FROM NotificacaoEnviada ne
              WHERE ne.agendamento = a
                AND ne.tipo = cn.tipo
                AND ne.horasAntes = cn.horasAntes
          )
        ORDER BY o.id, a.dtAgendamento ASC
        """)
    List<NotificacaoPendenteDTO> findLembretesPendentes(
        @Param("agora") LocalDateTime agora,
        @Param("inicioJanela") LocalDateTime inicioJanela,
        @Param("fimJanela") LocalDateTime fimJanela
    );

    /**
     * Busca notificacoes pendentes por tipo especifico.
     * Permite filtrar por CONFIRMACAO ou LEMBRETE dinamicamente.
     */
    @Query("""
        SELECT new org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO(
            a.id,
            a.dtAgendamento,
            c.nomeCompleto,
            c.telefone,
            o.id,
            o.nomeFantasia,
            cn.tipo,
            cn.horasAntes,
            cn.mensagemTemplate,
            i.instanceName
        )
        FROM Agendamento a
        JOIN a.cliente c
        JOIN a.organizacao o
        JOIN ConfigNotificacao cn ON cn.organizacao = o AND cn.ativo = true AND cn.tipo = :tipo
        JOIN Instance i ON i.organizacao = o
        WHERE a.status = 'AGENDADO'
          AND a.dtAgendamento > :agora
          AND i.status = 'CONNECTED'
          AND FUNCTION('TIMESTAMPADD', HOUR, -cn.horasAntes, a.dtAgendamento)
              BETWEEN :inicioJanela AND :fimJanela
          AND NOT EXISTS (
              SELECT 1 FROM NotificacaoEnviada ne
              WHERE ne.agendamento = a
                AND ne.tipo = cn.tipo
                AND ne.horasAntes = cn.horasAntes
          )
        ORDER BY o.id, a.dtAgendamento ASC
        """)
    List<NotificacaoPendenteDTO> findNotificacoesPendentesPorTipo(
        @Param("agora") LocalDateTime agora,
        @Param("inicioJanela") LocalDateTime inicioJanela,
        @Param("fimJanela") LocalDateTime fimJanela,
        @Param("tipo") TipoNotificacao tipo
    );

    /**
     * Busca notificacoes sem instancia WhatsApp conectada por tipo especifico (para alertar).
     */
    @Query("""
        SELECT new org.exemplo.bellory.model.dto.notificacao.NotificacaoSemInstanciaDTO(
            a.id, o.id, o.nomeFantasia, o.emailPrincipal, cn.tipo, cn.horasAntes, a.dtAgendamento
        )
        FROM Agendamento a
        JOIN a.cliente c
        JOIN a.organizacao o
        JOIN ConfigNotificacao cn ON cn.organizacao = o AND cn.ativo = true AND cn.tipo = :tipo
        LEFT JOIN Instance i ON i.organizacao = o
        WHERE a.status = 'AGENDADO'
          AND a.dtAgendamento > :agora
          AND (i IS NULL OR i.status != 'CONNECTED')
          AND FUNCTION('TIMESTAMPADD', HOUR, -cn.horasAntes, a.dtAgendamento)
              BETWEEN :inicioJanela AND :fimJanela
          AND NOT EXISTS (
              SELECT 1 FROM NotificacaoEnviada ne
              WHERE ne.agendamento = a AND ne.tipo = cn.tipo AND ne.horasAntes = cn.horasAntes
          )
        ORDER BY o.id
        """)
    List<NotificacaoSemInstanciaDTO> findNotificacoesSemInstanciaConectadaPorTipo(
        @Param("agora") LocalDateTime agora,
        @Param("inicioJanela") LocalDateTime inicioJanela,
        @Param("fimJanela") LocalDateTime fimJanela,
        @Param("tipo") TipoNotificacao tipo
    );
}
