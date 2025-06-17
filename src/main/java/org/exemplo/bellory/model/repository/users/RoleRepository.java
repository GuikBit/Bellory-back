package org.exemplo.bellory.model.repository.users;

import org.exemplo.bellory.model.entity.users.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade Role.
 * Usado para encontrar e gerir as permissões no sistema.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Encontra uma Role pelo seu nome.
     * @param nome O nome da role (ex: "ROLE_ADMIN").
     * @return Um Optional contendo a role, se encontrada.
     */
    Optional<Role> findByNome(String nome);
}
