package org.exemplo.bellory.model.repository.landingpage;

import org.exemplo.bellory.model.entity.landingpage.LandingPageSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LandingPageSectionRepository extends JpaRepository<LandingPageSection, Long> {

    /**
     * Lista seções da landing page ordenadas.
     */
    List<LandingPageSection> findByLandingPageIdAndAtivoTrueOrderByOrdemAsc(Long landingPageId);

    /**
     * Busca seção pelo sectionId (UUID).
     */
    Optional<LandingPageSection> findBySectionIdAndAtivoTrue(String sectionId);

    /**
     * Busca seção por ID e landing page.
     */
    Optional<LandingPageSection> findByIdAndLandingPageId(Long id, Long landingPageId);

    /**
     * Busca seção pelo sectionId e landing page.
     */
    Optional<LandingPageSection> findBySectionIdAndLandingPageId(String sectionId, Long landingPageId);

    /**
     * Lista seções visíveis da landing page.
     */
    List<LandingPageSection> findByLandingPageIdAndVisivelTrueAndAtivoTrueOrderByOrdemAsc(Long landingPageId);

    /**
     * Busca seções por tipo.
     */
    List<LandingPageSection> findByLandingPageIdAndTipoAndAtivoTrue(Long landingPageId, String tipo);

    /**
     * Conta seções da landing page.
     */
    int countByLandingPageIdAndAtivoTrue(Long landingPageId);

    /**
     * Obtém a maior ordem das seções.
     */
    @Query("SELECT COALESCE(MAX(s.ordem), 0) FROM LandingPageSection s " +
           "WHERE s.landingPage.id = :landingPageId AND s.ativo = true")
    int findMaxOrdem(@Param("landingPageId") Long landingPageId);

    /**
     * Atualiza ordem de múltiplas seções.
     */
    @Modifying
    @Query("UPDATE LandingPageSection s SET s.ordem = :ordem WHERE s.id = :id")
    void updateOrdem(@Param("id") Long id, @Param("ordem") Integer ordem);

    /**
     * Deleta todas as seções de uma landing page (soft delete).
     */
    @Modifying
    @Query("UPDATE LandingPageSection s SET s.ativo = false WHERE s.landingPage.id = :landingPageId")
    void softDeleteAllByLandingPageId(@Param("landingPageId") Long landingPageId);
}
