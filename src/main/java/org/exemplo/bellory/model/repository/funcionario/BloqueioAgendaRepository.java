package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {

    @Query("SELECT b FROM BloqueioAgenda b WHERE b.funcionario.id = :funcionarioId " +
            "AND b.tipoBloqueio <> 'AGENDAMENTO' " +
            "AND b.inicioBloqueio >= :inicio AND b.fimBloqueio <= :fim " +
            "ORDER BY b.inicioBloqueio ASC")
    List<BloqueioAgenda> findBloqueiosManuaisByFuncionarioIdAndPeriodo(
            @Param("funcionarioId") Long funcionarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT b FROM BloqueioAgenda b WHERE b.funcionario.id = :funcionarioId " +
            "AND b.tipoBloqueio <> 'AGENDAMENTO' " +
            "ORDER BY b.inicioBloqueio ASC")
    List<BloqueioAgenda> findBloqueiosManuaisByFuncionarioId(
            @Param("funcionarioId") Long funcionarioId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BloqueioAgenda b " +
            "WHERE b.funcionario.id = :funcionarioId " +
            "AND b.inicioBloqueio < :fim AND b.fimBloqueio > :inicio")
    boolean existsBloqueioSobreposto(
            @Param("funcionarioId") Long funcionarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
