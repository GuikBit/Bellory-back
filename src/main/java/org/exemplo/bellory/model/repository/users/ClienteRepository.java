package org.exemplo.bellory.model.repository.users;

import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório específico para a entidade Cliente.
 * Permite criar consultas que retornam apenas utilizadores do tipo Cliente.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // Pode adicionar métodos de consulta específicos para Cliente aqui no futuro.
}
