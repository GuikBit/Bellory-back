package org.exemplo.bellory.model.repository.funcionario;

import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório específico para a entidade Funcionario.
 * Permite criar consultas que retornam apenas utilizadores do tipo Funcionario.
 */
@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    // Pode adicionar métodos de consulta específicos para Funcionario aqui no futuro.

    // Método para buscar um usuário pelo seu nome de usuário (username).
    // O Spring Data JPA cria a implementação automaticamente.
    Optional<User> findByUsername(String username);

    List<FuncionarioAgendamento> findAllProjectedBy();
}
