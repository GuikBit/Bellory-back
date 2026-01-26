package org.exemplo.bellory.model.repository.site;

import org.exemplo.bellory.model.entity.site.SitePublicoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para acesso à configuração do site público.
 */
@Repository
public interface SitePublicoConfigRepository extends JpaRepository<SitePublicoConfig, Long> {

    /**
     * Busca a configuração do site pela organização
     */
    Optional<SitePublicoConfig> findByOrganizacaoId(Long organizacaoId);

    /**
     * Busca a configuração ativa do site pela organização
     */
    Optional<SitePublicoConfig> findByOrganizacaoIdAndActiveTrue(Long organizacaoId);

    /**
     * Busca a configuração do site pelo slug da organização
     */
    @Query("SELECT s FROM SitePublicoConfig s " +
           "JOIN s.organizacao o " +
           "WHERE o.slug = :slug AND o.ativo = true AND s.active = true")
    Optional<SitePublicoConfig> findByOrganizacaoSlugAndActive(@Param("slug") String slug);

    /**
     * Verifica se existe configuração para a organização
     */
    boolean existsByOrganizacaoId(Long organizacaoId);

    /**
     * Remove configuração por organização
     */
    void deleteByOrganizacaoId(Long organizacaoId);
}
