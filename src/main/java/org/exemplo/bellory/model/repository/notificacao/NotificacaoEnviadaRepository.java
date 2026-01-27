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
}
