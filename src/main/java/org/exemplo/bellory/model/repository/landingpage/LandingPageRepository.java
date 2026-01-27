package org.exemplo.bellory.model.repository.landingpage;

import org.exemplo.bellory.model.entity.landingpage.LandingPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LandingPageRepository extends JpaRepository<LandingPage, Long> {

    /**
     * Busca landing page pelo slug e organização.
     */
    Optional<LandingPage> findByOrganizacaoIdAndSlug(Long organizacaoId, String slug);

    /**
     * Busca landing page home da organização.
     */
    Optional<LandingPage> findByOrganizacaoIdAndIsHomeTrue(Long organizacaoId);

    /**
     * Lista todas as landing pages da organização.
     */
    List<LandingPage> findByOrganizacaoIdAndAtivoTrueOrderByDtCriacaoDesc(Long organizacaoId);

    /**
     * Lista landing pages com paginação.
     */
    Page<LandingPage> findByOrganizacaoIdAndAtivoTrue(Long organizacaoId, Pageable pageable);

    /**
     * Lista landing pages por status.
     */
    List<LandingPage> findByOrganizacaoIdAndStatusAndAtivoTrue(Long organizacaoId, String status);

    /**
     * Verifica se slug já existe na organização.
     */
    boolean existsByOrganizacaoIdAndSlug(Long organizacaoId, String slug);

    /**
     * Conta landing pages da organização.
     */
    long countByOrganizacaoIdAndAtivoTrue(Long organizacaoId);

    /**
     * Busca landing page com seções (fetch eager).
     */
    @Query("SELECT DISTINCT lp FROM LandingPage lp " +
           "LEFT JOIN FETCH lp.sections s " +
           "WHERE lp.id = :id AND lp.ativo = true " +
           "ORDER BY s.ordem")
    Optional<LandingPage> findByIdWithSections(@Param("id") Long id);

    /**
     * Busca landing page por slug com seções.
     */
    @Query("SELECT DISTINCT lp FROM LandingPage lp " +
           "LEFT JOIN FETCH lp.sections s " +
           "WHERE lp.organizacao.id = :orgId AND lp.slug = :slug AND lp.ativo = true " +
           "ORDER BY s.ordem")
    Optional<LandingPage> findByOrgAndSlugWithSections(
            @Param("orgId") Long organizacaoId,
            @Param("slug") String slug);

    /**
     * Busca a home page com seções.
     */
    @Query("SELECT DISTINCT lp FROM LandingPage lp " +
           "LEFT JOIN FETCH lp.sections s " +
           "WHERE lp.organizacao.id = :orgId AND lp.isHome = true AND lp.ativo = true " +
           "ORDER BY s.ordem")
    Optional<LandingPage> findHomeWithSections(@Param("orgId") Long organizacaoId);

    /**
     * Busca landing pages publicadas.
     */
    @Query("SELECT lp FROM LandingPage lp " +
           "WHERE lp.organizacao.id = :orgId AND lp.status = 'PUBLISHED' AND lp.ativo = true")
    List<LandingPage> findPublishedByOrganizacao(@Param("orgId") Long organizacaoId);
}
