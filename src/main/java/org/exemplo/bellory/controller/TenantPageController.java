package org.exemplo.bellory.controller;

import org.exemplo.bellory.config.tenant.TenantContext;
import org.exemplo.bellory.model.dto.tenant.PageComponentDTO;
import org.exemplo.bellory.model.dto.tenant.PageDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.tenant.Page;
import org.exemplo.bellory.model.entity.tenant.PageComponent;
import org.exemplo.bellory.service.tenant.PageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller REST para gerenciamento de páginas multi-tenant.
 * Expõe endpoints para buscar páginas baseadas no subdomínio.
 */
@RestController
@RequestMapping("/api/pages")
public class TenantPageController {

    private final PageService pageService;

    public TenantPageController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * Endpoint principal: busca uma página pelo slug.
     * O tenant é automaticamente identificado pelo subdomínio via TenantFilter.
     *
     * @param slug O slug da página
     * @return ResponseEntity com a página encontrada ou erro
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ResponseAPI<PageDTO>> getPageBySlug(@PathVariable String slug) {
        try {
            Optional<Page> pageOpt = pageService.findPageWithComponents(slug);

            if (pageOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseAPI.<PageDTO>builder()
                                .success(false)
                                .message("Página não encontrada: " + slug)
                                .errorCode(404)
                                .build());
            }

            Page page = pageOpt.get();
            PageDTO pageDTO = convertToDTO(page);

            return ResponseEntity.ok(ResponseAPI.<PageDTO>builder()
                    .success(true)
                    .message("Página recuperada com sucesso")
                    .dados(pageDTO)
                    .build());

        } catch (IllegalStateException e) {
            // Erro relacionado ao tenant (não encontrado no contexto)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<PageDTO>builder()
                            .success(false)
                            .message("Tenant não identificado: " + e.getMessage())
                            .errorCode(400)
                            .build());

        } catch (IllegalArgumentException e) {
            // Tenant não encontrado
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<PageDTO>builder()
                            .success(false)
                            .message("Tenant não encontrado: " + e.getMessage())
                            .errorCode(404)
                            .build());

        } catch (Exception e) {
            // Erro interno do servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<PageDTO>builder()
                            .success(false)
                            .message("Erro interno do servidor: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Lista todas as páginas do tenant atual.
     *
     * @return ResponseEntity com a lista de páginas
     */
    @GetMapping
    public ResponseEntity<ResponseAPI<List<PageDTO>>> getAllPages() {
        try {
            List<Page> pages = pageService.findAllPagesForCurrentTenant();

            List<PageDTO> pageDTOs = pages.stream()
                    .map(this::convertToSummaryDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ResponseAPI.<List<PageDTO>>builder()
                    .success(true)
                    .message("Páginas recuperadas com sucesso")
                    .dados(pageDTOs)
                    .build());

        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<List<PageDTO>>builder()
                            .success(false)
                            .message("Erro ao identificar tenant: " + e.getMessage())
                            .errorCode(400)
                            .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<PageDTO>>builder()
                            .success(false)
                            .message("Erro interno do servidor: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Endpoint para obter informações sobre o tenant atual.
     * Útil para debug e verificação do contexto.
     *
     * @return ResponseEntity com informações do tenant
     */
    @GetMapping("/tenant-info")
    public ResponseEntity<ResponseAPI<Object>> getTenantInfo() {
        try {
            String currentTenantId = TenantContext.getCurrentTenant();

            if (currentTenantId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<Object>builder()
                                .success(false)
                                .message("Nenhum tenant encontrado no contexto atual")
                                .errorCode(400)
                                .build());
            }

            // Criar informações básicas do tenant
            var tenantInfo = new java.util.HashMap<String, Object>();
            tenantInfo.put("tenant_id", currentTenantId);
            tenantInfo.put("context_available", TenantContext.hasTenant());

            return ResponseEntity.ok(ResponseAPI.<Object>builder()
                    .success(true)
                    .message("Informações do tenant recuperadas com sucesso")
                    .dados(tenantInfo)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Object>builder()
                            .success(false)
                            .message("Erro interno do servidor: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Converte uma entidade Page para DTO completo com componentes.
     *
     * @param page A página a converter
     * @return O DTO da página
     */
    private PageDTO convertToDTO(Page page) {
        List<PageComponentDTO> componentDTOs = page.getOrderedComponents().stream()
                .map(this::convertComponentToDTO)
                .collect(Collectors.toList());

        return PageDTO.builder()
                .id(page.getId())
                .slug(page.getSlug())
                .title(page.getTitle())
                .description(page.getDescription())
                .active(page.isActive())
                .metaTitle(page.getMetaTitle())
                .metaDescription(page.getMetaDescription())
                .metaKeywords(page.getMetaKeywords())
                .dtCriacao(page.getDtCriacao())
                .dtAtualizacao(page.getDtAtualizacao())
//                .components(componentDTOs)
                .componentCount(componentDTOs.size())
                .tenant(PageDTO.TenantSummaryDTO.builder()
                        .id(page.getTenant().getId())
                        .name(page.getTenant().getName())
                        .subdomain(page.getTenant().getSubdomain())
                        .theme(page.getTenant().getTheme())
                        .build())
                .build();
    }

    /**
     * Converte uma entidade Page para DTO resumido (sem componentes).
     *
     * @param page A página a converter
     * @return O DTO resumido da página
     */
    private PageDTO convertToSummaryDTO(Page page) {
        return PageDTO.builder()
                .id(page.getId())
                .slug(page.getSlug())
                .title(page.getTitle())
                .description(page.getDescription())
                .active(page.isActive())
                .metaTitle(page.getMetaTitle())
                .metaDescription(page.getMetaDescription())
                .metaKeywords(page.getMetaKeywords())
                .dtCriacao(page.getDtCriacao())
                .dtAtualizacao(page.getDtAtualizacao())
                .componentCount(page.getComponents() != null ? page.getComponents().size() : 0)
                .build();
    }

    /**
     * Converte uma entidade PageComponent para DTO.
     *
     * @param component O componente a converter
     * @return O DTO do componente
     */
    private PageComponentDTO convertComponentToDTO(PageComponent component) {
        // Obter informações do tipo de componente
        PageComponentDTO.ComponentTypeInfo typeInfo = null;
        try {
            PageComponent.ComponentType componentType = PageComponent.ComponentType.fromValue(component.getType());
            typeInfo = PageComponentDTO.ComponentTypeInfo.builder()
                    .value(componentType.getValue())
                    .description(componentType.getDescription())
                    .build();
        } catch (IllegalArgumentException e) {
            // Tipo personalizado ou não reconhecido
            typeInfo = PageComponentDTO.ComponentTypeInfo.builder()
                    .value(component.getType())
                    .description("Componente personalizado")
                    .build();
        }

        return PageComponentDTO.builder()
                .id(component.getId())
                .type(component.getType())
                .orderIndex(component.getOrderIndex())
                .active(component.isActive())
                .propsJson(component.getPropsJson())
                .styleConfig(component.getStyleConfig())
                .dtCriacao(component.getDtCriacao())
                .dtAtualizacao(component.getDtAtualizacao())
                .typeInfo(typeInfo)
                .build();
    }
}
