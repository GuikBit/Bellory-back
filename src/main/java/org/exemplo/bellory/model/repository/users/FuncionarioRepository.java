package org.exemplo.bellory.model.repository.users;

import org.exemplo.bellory.model.entity.users.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório específico para a entidade Funcionario.
 * Permite criar consultas que retornam apenas utilizadores do tipo Funcionario.
 */
@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    // Pode adicionar métodos de consulta específicos para Funcionario aqui no futuro.
}
