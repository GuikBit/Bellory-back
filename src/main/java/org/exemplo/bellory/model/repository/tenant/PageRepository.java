package org.exemplo.bellory.model.repository.tenant;

import org.exemplo.bellory.model.entity.tenant.Page;
import org.exemplo.bellory.model.entity.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Page.
 * Fornece métodos para buscar páginas por diferentes critérios.
 */
@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    /**
     * Busca uma página pelo slug e tenant.
     * @param slug O slug da página
     * @param tenant O tenant proprietário da página
     * @return Optional com a página encontrada
     */
    Optional<Page> findBySlugAndTenant(String slug, Tenant tenant);

    /**
     * Busca uma página ativa pelo slug e tenant.
     * @param slug O slug da página
     * @param tenant O tenant proprietário da página
     * @return Optional com a página ativa encontrada
     */
    Optional<Page> findBySlugAndTenantAndActiveTrue(String slug, Tenant tenant);

    /**
     * Busca uma página pelo slug e ID do tenant.
     * Método alternativo que usa o ID do tenant diretamente.
     * @param slug O slug da página
     * @param tenantId O ID do tenant
     * @return Optional com a página encontrada
     */
    @Query("SELECT p FROM Page p WHERE p.slug = :slug AND p.tenant.id = :tenantId AND p.active = true")
    Optional<Page> findBySlugAndTenantId(@Param("slug") String slug, @Param("tenantId") Long tenantId);

    /**
     * Busca uma página pelo slug e subdomínio do tenant.
     * Método que combina a busca por slug e subdomínio em uma única query.
     * @param slug O slug da página
     * @param subdomain O subdomínio do tenant
     * @return Optional com a página encontrada
     */
    @Query("SELECT p FROM Page p JOIN p.tenant t WHERE p.slug = :slug AND t.subdomain = :subdomain AND p.active = true AND t.active = true")
    Optional<Page> findBySlugAndTenantSubdomain(@Param("slug") String slug, @Param("subdomain") String subdomain);

    /**
     * Busca todas as páginas ativas de um tenant.
     * @param tenant O tenant
     * @return Lista de páginas ativas
     */
    List<Page> findByTenantAndActiveTrueOrderBySlugAsc(Tenant tenant);

    /**
     * Busca todas as páginas de um tenant por ID.
     * @param tenantId O ID do tenant
     * @return Lista de páginas
     */
    @Query("SELECT p FROM Page p WHERE p.tenant.id = :tenantId ORDER BY p.slug ASC")
    List<Page> findByTenantIdOrderBySlugAsc(@Param("tenantId") Long tenantId);

    /**
     * Verifica se existe uma página com o slug para o tenant.
     * @param slug O slug da página
     * @param tenant O tenant
     * @return true se existe uma página com o slug
     */
    boolean existsBySlugAndTenant(String slug, Tenant tenant);

    /**
     * Conta o número de páginas ativas de um tenant.
     * @param tenant O tenant
     * @return Número de páginas ativas
     */
    long countByTenantAndActiveTrue(Tenant tenant);

    /**
     * Busca páginas por título (busca parcial, case-insensitive).
     * @param tenantId O ID do tenant
     * @param title O título ou parte do título
     * @return Lista de páginas encontradas
     */
    @Query("SELECT p FROM Page p WHERE p.tenant.id = :tenantId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%')) AND p.active = true")
    List<Page> findByTenantIdAndTitleContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("title") String title);
}
