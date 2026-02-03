package org.exemplo.bellory.model.repository.notificacao;

import org.exemplo.bellory.model.entity.notificacao.NotificacaoEnviada;
import org.exemplo.bellory.model.entity.notificacao.StatusEnvio;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificacaoEnviadaRepository extends JpaRepository<NotificacaoEnviada, Long> {

    boolean existsByAgendamentoIdAndTipoAndHorasAntes(
        Long agendamentoId, TipoNotificacao tipo, Integer horasAntes);

    List<NotificacaoEnviada> findByAgendamentoId(Long agendamentoId);

    List<NotificacaoEnviada> findByStatus(StatusEnvio status);

    @Query("""
        SELECT ne FROM NotificacaoEnviada ne
        WHERE ne.status = 'FALHA' AND ne.dtEnvio > :desde
        ORDER BY ne.dtEnvio DESC
        """)
    List<NotificacaoEnviada> findFalhasRecentes(@Param("desde") LocalDateTime desde);

    @Modifying
    @Query("""
        UPDATE NotificacaoEnviada ne SET ne.status = :novoStatus
        WHERE ne.agendamento.id = :agendamentoId AND ne.status = 'PENDENTE'
        """)
    int cancelarPendentes(@Param("agendamentoId") Long agendamentoId,
                          @Param("novoStatus") StatusEnvio novoStatus);

    // Buscar confirmação pendente por telefone (aguardando resposta SIM/NAO/REAGENDAR)
    @Query("""
        SELECT ne FROM NotificacaoEnviada ne
        WHERE ne.telefoneDestino = :telefone
        AND ne.tipo = 'CONFIRMACAO'
        AND ne.status = 'AGUARDANDO_RESPOSTA'
        AND ne.instanceName = :instanceName
        ORDER BY ne.dtEnvio DESC
        """)
    Optional<NotificacaoEnviada> findConfirmacaoPendenteByTelefone(
        @Param("telefone") String telefone,
        @Param("instanceName") String instanceName);

    // Buscar notificação aguardando data (reagendamento)
    @Query("""
        SELECT ne FROM NotificacaoEnviada ne
        WHERE ne.telefoneDestino = :telefone
        AND ne.tipo = 'CONFIRMACAO'
        AND ne.status = 'AGUARDANDO_DATA'
        AND ne.instanceName = :instanceName
        ORDER BY ne.dtEnvio DESC
        """)
    Optional<NotificacaoEnviada> findAguardandoDataByTelefone(
        @Param("telefone") String telefone,
        @Param("instanceName") String instanceName);

    // Buscar notificação aguardando seleção de horário
    @Query("""
        SELECT ne FROM NotificacaoEnviada ne
        WHERE ne.telefoneDestino = :telefone
        AND ne.tipo = 'CONFIRMACAO'
        AND ne.status = 'AGUARDANDO_HORARIO'
        AND ne.instanceName = :instanceName
        ORDER BY ne.dtEnvio DESC
        """)
    Optional<NotificacaoEnviada> findAguardandoHorarioByTelefone(
        @Param("telefone") String telefone,
        @Param("instanceName") String instanceName);

    // Buscar qualquer notificação ativa (não finalizada) por telefone
    @Query("""
        SELECT ne FROM NotificacaoEnviada ne
        WHERE ne.telefoneDestino = :telefone
        AND ne.tipo = 'CONFIRMACAO'
        AND ne.status IN ('AGUARDANDO_RESPOSTA', 'AGUARDANDO_DATA', 'AGUARDANDO_HORARIO')
        AND ne.instanceName = :instanceName
        ORDER BY ne.dtEnvio DESC
        """)
    Optional<NotificacaoEnviada> findNotificacaoAtivaByTelefone(
        @Param("telefone") String telefone,
        @Param("instanceName") String instanceName);

    // Expirar notificações antigas (mais de X horas aguardando resposta)
    @Modifying
    @Query("""
        UPDATE NotificacaoEnviada ne SET ne.status = 'EXPIRADO'
        WHERE ne.status IN ('AGUARDANDO_RESPOSTA', 'AGUARDANDO_DATA', 'AGUARDANDO_HORARIO')
        AND ne.dtEnvio < :limite
        """)
    int expirarNotificacoesAntigas(@Param("limite") LocalDateTime limite);

    // ==================== QUERIES PARA RELATÓRIOS ====================

    /**
     * Conta notificações por tipo e status no período (para organização via agendamento)
     */
    @Query("SELECT ne.status, COUNT(ne) FROM NotificacaoEnviada ne " +
            "WHERE ne.tipo = :tipo " +
            "AND ne.agendamento.organizacao.id = :organizacaoId " +
            "AND ne.dtEnvio BETWEEN :inicio AND :fim " +
            "GROUP BY ne.status")
    List<Object[]> countByTipoAndStatusAndOrganizacaoAndPeriodo(
            @Param("tipo") TipoNotificacao tipo,
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Total de notificações por tipo no período
     */
    @Query("SELECT COUNT(ne) FROM NotificacaoEnviada ne " +
            "WHERE ne.tipo = :tipo " +
            "AND ne.agendamento.organizacao.id = :organizacaoId " +
            "AND ne.dtEnvio BETWEEN :inicio AND :fim")
    Long countByTipoAndOrganizacaoAndPeriodo(
            @Param("tipo") TipoNotificacao tipo,
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Busca falhas no período por organização
     */
    @Query("SELECT ne FROM NotificacaoEnviada ne " +
            "WHERE ne.status = 'FALHA' " +
            "AND ne.agendamento.organizacao.id = :organizacaoId " +
            "AND ne.dtEnvio BETWEEN :inicio AND :fim " +
            "ORDER BY ne.dtEnvio DESC")
    List<NotificacaoEnviada> findFalhasByOrganizacaoAndPeriodo(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta erros agrupados por mensagem de erro
     */
    @Query("SELECT ne.erroMensagem, COUNT(ne) FROM NotificacaoEnviada ne " +
            "WHERE ne.status = 'FALHA' " +
            "AND ne.agendamento.organizacao.id = :organizacaoId " +
            "AND ne.dtEnvio BETWEEN :inicio AND :fim " +
            "AND ne.erroMensagem IS NOT NULL " +
            "GROUP BY ne.erroMensagem " +
            "ORDER BY COUNT(ne) DESC")
    List<Object[]> countErrosByMensagemAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta notificações agrupadas por data (para gráfico de evolução)
     */
    @Query("SELECT CAST(ne.dtEnvio AS LocalDate), ne.tipo, COUNT(ne) FROM NotificacaoEnviada ne " +
            "WHERE ne.agendamento.organizacao.id = :organizacaoId " +
            "AND ne.dtEnvio BETWEEN :inicio AND :fim " +
            "GROUP BY CAST(ne.dtEnvio AS LocalDate), ne.tipo " +
            "ORDER BY CAST(ne.dtEnvio AS LocalDate)")
    List<Object[]> countByDataAndTipoAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * IDs de agendamentos que receberam notificação no período
     */
    @Query("SELECT DISTINCT ne.agendamento.id FROM NotificacaoEnviada ne " +
            "WHERE ne.agendamento.organizacao.id = :organizacaoId " +
            "AND ne.dtEnvio BETWEEN :inicio AND :fim")
    List<Long> findAgendamentoIdsComNotificacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}
