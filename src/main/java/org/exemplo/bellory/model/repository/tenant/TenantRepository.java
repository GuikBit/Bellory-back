package org.exemplo.bellory.model.repository.tenant;

import org.exemplo.bellory.model.entity.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade Tenant.
 * Fornece métodos para buscar tenants por diferentes critérios.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Busca um tenant pelo subdomínio.
     * @param subdomain O subdomínio do tenant
     * @return Optional com o tenant encontrado
     */
    Optional<Tenant> findBySubdomain(String subdomain);

    /**
     * Busca um tenant ativo pelo subdomínio.
     * @param subdomain O subdomínio do tenant
     * @return Optional com o tenant ativo encontrado
     */
    Optional<Tenant> findBySubdomainAndActiveTrue(String subdomain);

    /**
     * Verifica se existe um tenant com o subdomínio informado.
     * @param subdomain O subdomínio a verificar
     * @return true se existe um tenant com o subdomínio
     */
    boolean existsBySubdomain(String subdomain);

    /**
     * Busca um tenant pelo email.
     * @param email O email do tenant
     * @return Optional com o tenant encontrado
     */
    Optional<Tenant> findByEmail(String email);

    /**
     * Busca tenants por nome (busca parcial, case-insensitive).
     * @param name O nome ou parte do nome
     * @return Lista de tenants encontrados
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) AND t.active = true")
    java.util.List<Tenant> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Conta o número total de tenants ativos.
     * @return Número de tenants ativos
     */
    long countByActiveTrue();
}
