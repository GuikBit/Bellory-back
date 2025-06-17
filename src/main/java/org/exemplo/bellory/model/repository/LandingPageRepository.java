package org.exemplo.bellory.model.repository;

import org.exemplo.bellory.model.entity.LandingPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LandingPageRepository extends JpaRepository<LandingPage, Long> {

    /**
     * Encontra uma LandingPage pelo seu slug único.
     * O Spring Data JPA cria a implementação automaticamente.
     */
    Optional<LandingPage> findBySlug(String slug);
}
