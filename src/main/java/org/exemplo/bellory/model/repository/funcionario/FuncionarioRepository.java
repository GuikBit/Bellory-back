package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    // Usado apenas para autenticação (login/JWT) - não usar para validações de negócio
    Optional<Funcionario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<Funcionario> findByCpf(String cpf);

    Optional<Funcionario> findByCpfAndOrganizacao_Id(String cpf, Long organizacaoId);

    Optional<Funcionario> findByUsernameAndOrganizacao_Id(String trim, Long organizacaoId);

    List<FuncionarioAgendamento> findAllProjectedByOrganizacao_Id(Long organizacaoId);

    List<Funcionario> findAllByOrganizacao_Id(Long organizacaoId);

    @Query("SELECT f FROM Funcionario f " +
            "JOIN FETCH f.organizacao " +
            "WHERE f.id = :id")
    Optional<Funcionario> findByIdWithOrganizacao(@Param("id") Long id);

    List<Funcionario> findAllByOrganizacao_IdAndAtivoTrueAndIsVisivelExternoTrue(Long organizacaoId);

    /**
     * Busca funcionários com fetch dos serviços que atendem.
     * Evita N+1 queries.
     */
    @Query("SELECT DISTINCT f FROM Funcionario f " +
            "LEFT JOIN FETCH f.servicos " +
            "LEFT JOIN FETCH f.jornadasDia jd " +
            "LEFT JOIN FETCH jd.horarios " +
            "WHERE f.organizacao.id = :orgId " +
            "AND f.ativo = true " +
            "AND f.isVisivelExterno = true " +
            "ORDER BY f.nomeCompleto")
    List<Funcionario> findAllForPublicSite(@Param("orgId") Long organizacaoId);

}
