package org.exemplo.bellory.model.repository.users;

import org.exemplo.bellory.model.entity.users.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    // Usado para autenticação (login/JWT) e criação de organização - busca global
    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByUsernameAndOrganizacao_Id(String username, Long organizacaoId);

    // Usado na criação de organização - verifica globalmente para evitar ambiguidade no login
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndOrganizacao_Id(String username, Long organizacaoId);

    boolean existsByEmailAndOrganizacao_Id(String email, Long organizacaoId);

    @Query("SELECT a FROM Admin a WHERE LOWER(a.email) = LOWER(:email)")
    List<Admin> findAllByEmailIgnoreCase(@Param("email") String email);
}
