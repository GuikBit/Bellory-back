package org.exemplo.bellory.service.tenant;

import org.exemplo.bellory.model.entity.tenant.Page;
import org.exemplo.bellory.model.entity.tenant.PageComponent;
import org.exemplo.bellory.model.entity.tenant.Tenant;
import org.exemplo.bellory.model.repository.tenant.PageRepository;
import org.exemplo.bellory.model.repository.tenant.PageComponentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciamento de páginas.
 * Fornece métodos para buscar, criar e gerenciar páginas de tenants.
 */
@Service
@Transactional(readOnly = true)
public class PageService {

    private final PageRepository pageRepository;
    private final PageComponentRepository componentRepository;
    private final TenantService tenantService;

    public PageService(PageRepository pageRepository,
                       PageComponentRepository componentRepository,
                       TenantService tenantService) {
        this.pageRepository = pageRepository;
        this.componentRepository = componentRepository;
        this.tenantService = tenantService;
    }

    /**
     * Busca uma página pelo slug usando o tenant do contexto atual.
     * @param slug O slug da página
     * @return Optional com a página encontrada
     */
    public Optional<Page> findPageBySlug(String slug) {
        Tenant currentTenant = tenantService.getCurrentTenantOrThrow();
        return pageRepository.findBySlugAndTenantAndActiveTrue(slug, currentTenant);
    }

    /**
     * Busca uma página pelo slug e subdomínio.
     * Método alternativo que não depende do contexto do tenant.
     * @param slug O slug da página
     * @param subdomain O subdomínio do tenant
     * @return Optional com a página encontrada
     */
    public Optional<Page> findPageBySlugAndSubdomain(String slug, String subdomain) {
        return pageRepository.findBySlugAndTenantSubdomain(slug, subdomain);
    }

    /**
     * Busca uma página com todos os seus componentes ordenados.
     * @param slug O slug da página
     * @return Optional com a página e seus componentes
     */
    public Optional<Page> findPageWithComponents(String slug) {
        Optional<Page> pageOpt = findPageBySlug(slug);

        if (pageOpt.isPresent()) {
            Page page = pageOpt.get();
            // Os componentes já vêm ordenados devido ao @OrderBy na entidade
            // Mas garantimos que estão carregados com uma busca explícita se necessário
            List<PageComponent> components = componentRepository.findByPageAndActiveTrueOrderByOrderIndexAsc(page);
            // Os componentes já estão associados à página, mas podemos forçar o carregamento
            page.getComponents().size(); // Força o carregamento se lazy
        }

        return pageOpt;
    }

    /**
     * Lista todas as páginas ativas do tenant atual.
     * @return Lista de páginas ativas
     */
    public List<Page> findAllPagesForCurrentTenant() {
        Tenant currentTenant = tenantService.getCurrentTenantOrThrow();
        return pageRepository.findByTenantAndActiveTrueOrderBySlugAsc(currentTenant);
    }

    /**
     * Cria uma nova página para o tenant atual.
     * @param page A página a ser criada
     * @return A página criada
     */
    @Transactional
    public Page createPage(Page page) {
        Tenant currentTenant = tenantService.getCurrentTenantOrThrow();

        // Verificar se já existe uma página com o mesmo slug para este tenant
        if (pageRepository.existsBySlugAndTenant(page.getSlug(), currentTenant)) {
            throw new IllegalArgumentException("Page with slug '" + page.getSlug() + "' already exists for this tenant");
        }

        page.setTenant(currentTenant);
        return pageRepository.save(page);
    }

    /**
     * Atualiza uma página existente.
     * @param pageId O ID da página
     * @param updatedPage Os dados atualizados da página
     * @return A página atualizada
     */
    @Transactional
    public Page updatePage(Long pageId, Page updatedPage) {
        Tenant currentTenant = tenantService.getCurrentTenantOrThrow();

        Page existingPage = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found with id: " + pageId));

        // Verificar se a página pertence ao tenant atual
        if (!existingPage.getTenant().getId().equals(currentTenant.getId())) {
            throw new IllegalArgumentException("Page does not belong to current tenant");
        }

        // Se o slug foi alterado, verificar se não existe outra página com o mesmo slug
        if (!existingPage.getSlug().equals(updatedPage.getSlug()) &&
                pageRepository.existsBySlugAndTenant(updatedPage.getSlug(), currentTenant)) {
            throw new IllegalArgumentException("Page with slug '" + updatedPage.getSlug() + "' already exists for this tenant");
        }

        // Atualizar os campos
        existingPage.setSlug(updatedPage.getSlug());
        existingPage.setTitle(updatedPage.getTitle());
        existingPage.setDescription(updatedPage.getDescription());
        existingPage.setMetaTitle(updatedPage.getMetaTitle());
        existingPage.setMetaDescription(updatedPage.getMetaDescription());
        existingPage.setMetaKeywords(updatedPage.getMetaKeywords());
        existingPage.setActive(updatedPage.isActive());

        return pageRepository.save(existingPage);
    }

    /**
     * Remove uma página (soft delete).
     * @param pageId O ID da página
     */
    @Transactional
    public void deletePage(Long pageId) {
        Tenant currentTenant = tenantService.getCurrentTenantOrThrow();

        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found with id: " + pageId));

        // Verificar se a página pertence ao tenant atual
        if (!page.getTenant().getId().equals(currentTenant.getId())) {
            throw new IllegalArgumentException("Page does not belong to current tenant");
        }

        page.setActive(false);
        pageRepository.save(page);
    }

    /**
     * Adiciona um componente a uma página.
     * @param pageId O ID da página
     * @param component O componente a adicionar
     * @return O componente criado
     */
    @Transactional
    public PageComponent addComponentToPage(Long pageId, PageComponent component) {
        Tenant currentTenant = tenantService.getCurrentTenantOrThrow();

        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found with id: " + pageId));

        // Verificar se a página pertence ao tenant atual
        if (!page.getTenant().getId().equals(currentTenant.getId())) {
            throw new IllegalArgumentException("Page does not belong to current tenant");
        }

        // Se não foi especificado orderIndex, colocar no final
        if (component.getOrderIndex() == null) {
            Integer maxOrder = componentRepository.findMaxOrderIndexByPage(page).orElse(-1);
            component.setOrderIndex(maxOrder + 1);
        }

        component.setPage(page);
        return componentRepository.save(component);
    }

    /**
     * Remove um componente de uma página.
     * @param componentId O ID do componente
     */
    @Transactional
    public void removeComponentFromPage(Long componentId) {
        Tenant currentTenant = tenantService.getCurrentTenantOrThrow();

        PageComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new IllegalArgumentException("Component not found with id: " + componentId));

        // Verificar se o componente pertence a uma página do tenant atual
        if (!component.getPage().getTenant().getId().equals(currentTenant.getId())) {
            throw new IllegalArgumentException("Component does not belong to current tenant");
        }

        componentRepository.delete(component);
    }

    /**
     * Reordena os componentes de uma página.
     * @param pageId O ID da página
     * @param componentIds Lista de IDs dos componentes na nova ordem
     */
    @Transactional
    public void reorderPageComponents(Long pageId, List<Long> componentIds) {
        Tenant currentTenant = tenantService.getCurrentTenantOrThrow();

        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Page not found with id: " + pageId));

        // Verificar se a página pertence ao tenant atual
        if (!page.getTenant().getId().equals(currentTenant.getId())) {
            throw new IllegalArgumentException("Page does not belong to current tenant");
        }

        // Reordenar os componentes
        for (int i = 0; i < componentIds.size(); i++) {
            Long componentId = componentIds.get(i);
            PageComponent component = componentRepository.findById(componentId)
                    .orElseThrow(() -> new IllegalArgumentException("Component not found with id: " + componentId));

            // Verificar se o componente pertence à página
            if (!component.getPage().getId().equals(pageId)) {
                throw new IllegalArgumentException("Component " + componentId + " does not belong to page " + pageId);
            }

            component.setOrderIndex(i);
            componentRepository.save(component);
        }
    }
}
