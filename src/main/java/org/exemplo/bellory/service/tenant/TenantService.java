package org.exemplo.bellory.service.tenant;

import org.exemplo.bellory.config.tenant.TenantContext;
import org.exemplo.bellory.model.entity.tenant.Tenant;
import org.exemplo.bellory.model.repository.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service para gerenciamento de tenants.
 * Fornece métodos para buscar e validar tenants.
 */
@Service
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Busca um tenant pelo subdomínio.
     * @param subdomain O subdomínio do tenant
     * @return Optional com o tenant encontrado
     */
    public Optional<Tenant> findBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomainAndActiveTrue(subdomain);
    }

    /**
     * Obtém o tenant atual baseado no contexto da thread.
     * @return Optional com o tenant atual
     * @throws IllegalStateException se não houver tenant no contexto
     */
    public Optional<Tenant> getCurrentTenant() {
        String currentTenantId = TenantContext.getCurrentTenant();

        if (currentTenantId == null) {
            throw new IllegalStateException("No tenant found in current context");
        }

        // Se for um tenant público (para rotas sem tenant)
        if ("public".equals(currentTenantId)) {
            return Optional.empty();
        }

        return findBySubdomain(currentTenantId);
    }

    /**
     * Obtém o tenant atual ou lança exceção se não encontrado.
     * @return O tenant atual
     * @throws IllegalArgumentException se o tenant não for encontrado
     */
    public Tenant getCurrentTenantOrThrow() {
        return getCurrentTenant()
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for subdomain: " + TenantContext.getCurrentTenant()));
    }

    /**
     * Verifica se existe um tenant com o subdomínio informado.
     * @param subdomain O subdomínio a verificar
     * @return true se existe um tenant ativo com o subdomínio
     */
    public boolean existsBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomainAndActiveTrue(subdomain).isPresent();
    }

    /**
     * Valida se o tenant está ativo e pode ser usado.
     * @param tenant O tenant a validar
     * @return true se o tenant é válido e ativo
     */
    public boolean isValidTenant(Tenant tenant) {
        return tenant != null && tenant.isActive();
    }

    /**
     * Cria um novo tenant.
     * @param tenant O tenant a ser criado
     * @return O tenant criado
     */
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        // Validar se o subdomínio já existe
        if (tenantRepository.existsBySubdomain(tenant.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists: " + tenant.getSubdomain());
        }

        return tenantRepository.save(tenant);
    }

    /**
     * Atualiza um tenant existente.
     * @param tenant O tenant a ser atualizado
     * @return O tenant atualizado
     */
    @Transactional
    public Tenant updateTenant(Tenant tenant) {
        if (!tenantRepository.existsById(tenant.getId())) {
            throw new IllegalArgumentException("Tenant not found with id: " + tenant.getId());
        }

        return tenantRepository.save(tenant);
    }

    /**
     * Desativa um tenant (soft delete).
     * @param tenantId O ID do tenant a desativar
     */
    @Transactional
    public void deactivateTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + tenantId));

        tenant.setActive(false);
        tenantRepository.save(tenant);
    }
}
