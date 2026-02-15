package org.exemplo.bellory.model.repository.landingpage;

import org.exemplo.bellory.model.entity.landingpage.LandingPageVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LandingPageVersionRepository extends JpaRepository<LandingPageVersion, Long> {

    /**
     * Lista versões de uma landing page.
     */
    List<LandingPageVersion> findByLandingPageIdOrderByVersaoDesc(Long landingPageId);

    /**
     * Lista versões com paginação.
     */
    Page<LandingPageVersion> findByLandingPageIdOrderByVersaoDesc(Long landingPageId, Pageable pageable);

    /**
     * Busca versão específica.
     */
    Optional<LandingPageVersion> findByLandingPageIdAndVersao(Long landingPageId, Integer versao);

    /**
     * Busca última versão.
     */
    Optional<LandingPageVersion> findFirstByLandingPageIdOrderByVersaoDesc(Long landingPageId);

    /**
     * Obtém o número da última versão.
     */
    @Query("SELECT COALESCE(MAX(v.versao), 0) FROM LandingPageVersion v " +
           "WHERE v.landingPage.id = :landingPageId")
    int findMaxVersao(@Param("landingPageId") Long landingPageId);

    /**
     * Conta versões de uma landing page.
     */
    long countByLandingPageId(Long landingPageId);

    /**
     * Deleta versões antigas (mantém as últimas N).
     */
    @Query("DELETE FROM LandingPageVersion v WHERE v.landingPage.id = :landingPageId " +
           "AND v.versao < (SELECT MAX(v2.versao) - :keepLast FROM LandingPageVersion v2 " +
           "WHERE v2.landingPage.id = :landingPageId)")
    void deleteOldVersions(@Param("landingPageId") Long landingPageId, @Param("keepLast") int keepLast);
}
