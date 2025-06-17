package org.exemplo.bellory.model.repository.users;

import org.exemplo.bellory.model.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Método para buscar um usuário pelo seu nome de usuário (username).
    // O Spring Data JPA cria a implementação automaticamente.
    Optional<User> findByUsername(String username);
}
