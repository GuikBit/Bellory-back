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
}
