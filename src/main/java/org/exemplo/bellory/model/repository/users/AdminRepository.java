package org.exemplo.bellory.model.repository.users;


import org.exemplo.bellory.model.entity.users.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório específico para a entidade Admin.
 * Permite criar consultas que retornam apenas utilizadores do tipo Admin.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    // Pode adicionar métodos de consulta específicos para Admin aqui no futuro.
}
