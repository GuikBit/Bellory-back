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

    /**
     * Conta dias ativos que possuem pelo menos um período configurado.
     * Usado pelo onboarding: 7 placeholders inativos (criados por listar() em HorarioFuncionamentoService)
     * NÃO contam como horário definido.
     */
    @Query("SELECT COUNT(DISTINCT h) FROM HorarioFuncionamento h " +
            "JOIN h.periodos p " +
            "WHERE h.organizacao.id = :orgId AND h.ativo = true")
    long countDiasAtivosComPeriodo(@Param("orgId") Long organizacaoId);
}
