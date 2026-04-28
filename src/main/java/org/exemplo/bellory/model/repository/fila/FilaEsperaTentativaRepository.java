package org.exemplo.bellory.model.repository.fila;

import org.exemplo.bellory.model.entity.fila.FilaEsperaTentativa;
import org.exemplo.bellory.model.entity.fila.StatusFilaTentativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FilaEsperaTentativaRepository extends JpaRepository<FilaEsperaTentativa, Long> {

    /**
     * Tentativas aguardando resposta cuja janela de timeout ja passou.
     * Usado pelo scheduler para marcar como EXPIRADO e seguir pro proximo.
     */
    @Query("""
            SELECT t FROM FilaEsperaTentativa t
            WHERE t.status = org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.AGUARDANDO_RESPOSTA
              AND t.dtExpira < :now
            """)
    List<FilaEsperaTentativa> findExpiradas(@Param("now") LocalDateTime now);

    /**
     * Verifica se ha tentativa ATIVA (PENDENTE/ENVIADO/AGUARDANDO_RESPOSTA) para um agendamento.
     * Evita oferecer outro slot ao cliente que ja esta com uma oferta aberta.
     */
    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM FilaEsperaTentativa t
            WHERE t.agendamento.id = :agendamentoId
              AND t.status IN (
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.PENDENTE,
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.ENVIADO,
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.AGUARDANDO_RESPOSTA
              )
            """)
    boolean existsAtivaPorAgendamento(@Param("agendamentoId") Long agendamentoId);

    /**
     * Tentativas ativas para um slot/funcionario (utilizado para invalidar concorrentes
     * quando alguem aceita).
     */
    @Query("""
            SELECT t FROM FilaEsperaTentativa t
            WHERE t.funcionarioId = :funcionarioId
              AND t.slotInicio = :slotInicio
              AND t.slotFim = :slotFim
              AND t.status IN (
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.PENDENTE,
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.ENVIADO,
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.AGUARDANDO_RESPOSTA
              )
            """)
    List<FilaEsperaTentativa> findAtivasParaSlot(
            @Param("funcionarioId") Long funcionarioId,
            @Param("slotInicio") LocalDateTime slotInicio,
            @Param("slotFim") LocalDateTime slotFim);

    List<FilaEsperaTentativa> findByAgendamentoIdOrderByDtCriacaoDesc(Long agendamentoId);

    Optional<FilaEsperaTentativa> findFirstByAgendamentoIdAndStatusOrderByDtCriacaoDesc(
            Long agendamentoId, StatusFilaTentativa status);

    /**
     * Tentativas ativas por agendamento cancelado. Util para abortar a corrente
     * caso o agendamento de origem seja revertido.
     */
    List<FilaEsperaTentativa> findByAgendamentoCanceladoIdAndStatusIn(
            Long agendamentoCanceladoId, List<StatusFilaTentativa> statuses);

    /**
     * Tentativas ativas para um agendamento (target). Usado para cleanup quando
     * o cliente cancela seu proprio agendamento.
     */
    @Query("""
            SELECT t FROM FilaEsperaTentativa t
            WHERE t.agendamento.id = :agendamentoId
              AND t.status IN (
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.PENDENTE,
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.ENVIADO,
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.AGUARDANDO_RESPOSTA
              )
            """)
    List<FilaEsperaTentativa> findAtivasPorAgendamento(@Param("agendamentoId") Long agendamentoId);

    /**
     * Tentativas ativas para um funcionario num intervalo de tempo. Usado por
     * getHorariosDisponiveis para bloquear slots durante a oferta da fila.
     */
    @Query("""
            SELECT t FROM FilaEsperaTentativa t
            WHERE t.funcionarioId = :funcionarioId
              AND t.slotInicio < :fim
              AND t.slotFim > :inicio
              AND t.status IN (
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.PENDENTE,
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.ENVIADO,
                  org.exemplo.bellory.model.entity.fila.StatusFilaTentativa.AGUARDANDO_RESPOSTA
              )
            """)
    List<FilaEsperaTentativa> findAtivasNoIntervalo(
            @Param("funcionarioId") Long funcionarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
