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

    Optional<Funcionario> findByUsername(String username);

    List<FuncionarioAgendamento> findAllProjectedBy();

    @Query("SELECT f FROM Funcionario f WHERE f.situacao = 'Ativo'")
    List<Funcionario> findByAtivo(boolean ativo);

    @Query("SELECT COUNT(f) FROM Funcionario f WHERE f.situacao = 'Ativo'")
    Long countByAtivo(boolean ativo);

    Optional<Object> findByCpf(String cpf);

    Optional<Object> findByUsernameAndOrganizacao_Id(String trim, Long organizacaoId);

    List<FuncionarioAgendamento> findAllProjectedByOrganizacao_Id(Long organizacaoId);

    List<Funcionario> findAllByOrganizacao_Id(Long organizacaoId);

    @Query("SELECT f FROM Funcionario f " +
            "JOIN FETCH f.organizacao " +
            "WHERE f.id = :id")
    Optional<Funcionario> findByIdWithOrganizacao(@Param("id") Long id);


}
