package org.exemplo.bellory.model.repository.organizacao;

import org.exemplo.bellory.model.entity.funcionario.DiaSemana;
import org.exemplo.bellory.model.entity.organizacao.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Long> {

    @Query("SELECT h FROM HorarioFuncionamento h LEFT JOIN FETCH h.periodos WHERE h.organizacao.id = :orgId")
    List<HorarioFuncionamento> findByOrganizacaoIdWithPeriodos(@Param("orgId") Long organizacaoId);

    List<HorarioFuncionamento> findByOrganizacaoId(Long organizacaoId);

    Optional<HorarioFuncionamento> findByOrganizacaoIdAndDiaSemana(Long organizacaoId, DiaSemana diaSemana);
}
