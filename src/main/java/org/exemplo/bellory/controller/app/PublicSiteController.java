package org.exemplo.bellory.controller.app;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.site.*;
import org.exemplo.bellory.model.dto.tenent.OrganizacaoPublicDTO;
import org.exemplo.bellory.model.dto.tenent.PublicSiteResponseDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.dto.landingpage.LandingPageDTO;
import org.exemplo.bellory.service.PublicSiteService;
import org.exemplo.bellory.service.landingpage.PublicLandingPageService;
import org.exemplo.bellory.service.site.PublicSiteGuard;
import org.exemplo.bellory.service.site.PublicSiteGuard.PublicSiteAccess;
import org.exemplo.bellory.service.site.PublicSitePageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST público para o site/landing page das organizações.
 *
 * IMPORTANTE: Este controller NÃO requer autenticação.
 *
 * Todas as respostas são embrulhadas em {@link PublicSiteResponse}:
 *   - Site ativo COMPLETO → { siteAtivo: true, modo: "COMPLETO", conteudo: {...} }
 *   - Site ativo BÁSICO   → { siteAtivo: true, modo: "BASICO",   conteudo: {...} ou null }
 *   - Site inativo        → { siteAtivo: false, modo: null, conteudo: null }  (HTTP 200)
 *   - Org inexistente / desativada → HTTP 404
 *
 * No modo BÁSICO apenas /home e /booking retornam conteúdo útil. As demais
 * seções individuais respondem com conteudo: null + modo: BASICO.
 */
@RestController
@RequestMapping("/api/v1/public/site")
@Tag(name = "Site Público", description = "Endpoints públicos para renderização do site das organizações")
public class PublicSiteController {

    private final PublicSiteService publicSiteService;
    private final PublicSitePageService publicSitePageService;
    private final PublicLandingPageService publicLandingPageService;
    private final PublicSiteGuard publicSiteGuard;

    public PublicSiteController(PublicSiteService publicSiteService,
                                PublicSitePageService publicSitePageService,
                                PublicLandingPageService publicLandingPageService,
                                PublicSiteGuard publicSiteGuard) {
        this.publicSiteService = publicSiteService;
        this.publicSitePageService = publicSitePageService;
        this.publicLandingPageService = publicLandingPageService;
        this.publicSiteGuard = publicSiteGuard;
    }

    // ==================== HOME PAGE ====================

    /**
     * Endpoint principal da home. Em BÁSICO devolve HomePageBasicaDTO (apenas
     * organização com logo/banner + booking). Em COMPLETO devolve HomePageDTO.
     */
    @Operation(summary = "Obter home page (conteúdo varia conforme o plano)")
    @GetMapping("/{slug}/home")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<Object>>> getHomePage(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequestPublic("Slug é obrigatório");
            }

            PublicSiteAccess access = publicSiteGuard.check(normalizedSlug);
            if (access.getStatus() == PublicSiteGuard.Status.NOT_FOUND) {
                return notFoundPublic("Organização não encontrada");
            }
            if (access.getStatus() == PublicSiteGuard.Status.INACTIVE) {
                return inactive();
            }

            if (access.isBasico()) {
                Optional<HomePageBasicaDTO> basica = publicSitePageService.getHomePageBasica(normalizedSlug);
                if (basica.isEmpty()) {
                    return notFoundPublic("Organização não encontrada");
                }
                return okPublic("Home page (modo básico) recuperada com sucesso",
                        ModoSite.BASICO, basica.get());
            }

            Optional<HomePageDTO> completa = publicSitePageService.getHomePage(normalizedSlug);
            if (completa.isEmpty()) {
                return notFoundPublic("Organização não encontrada");
            }
            return okPublic("Home page recuperada com sucesso",
                    ModoSite.COMPLETO, completa.get());

        } catch (Exception e) {
            return serverErrorPublic("Erro ao processar requisição: " + e.getMessage());
        }
    }

    // ==================== HEADER ====================

    @Operation(summary = "Obter configuração do header (só no modo COMPLETO)")
    @GetMapping("/{slug}/header")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<HeaderConfigDTO>>> getHeader(@PathVariable String slug) {
        return handleCompletoOnly(slug, "Header recuperado com sucesso", "Organização não encontrada",
                publicSitePageService::getHeader);
    }

    // ==================== HERO ====================

    @Operation(summary = "Obter dados do hero/banner (só no modo COMPLETO)")
    @GetMapping("/{slug}/hero")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<HeroSectionDTO>>> getHero(@PathVariable String slug) {
        return handleCompletoOnly(slug, "Hero recuperado com sucesso", "Organização não encontrada",
                publicSitePageService::getHero);
    }

    // ==================== ABOUT ====================

    @Operation(summary = "Obter seção sobre resumida (só no modo COMPLETO)")
    @GetMapping("/{slug}/about")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<AboutSectionDTO>>> getAbout(@PathVariable String slug) {
        return handleCompletoOnly(slug, "Seção sobre recuperada com sucesso", "Organização não encontrada",
                s -> publicSitePageService.getAbout(s, false));
    }

    @Operation(summary = "Obter seção sobre completa (só no modo COMPLETO)")
    @GetMapping("/{slug}/about/full")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<AboutSectionDTO>>> getAboutFull(@PathVariable String slug) {
        return handleCompletoOnly(slug, "Página sobre recuperada com sucesso", "Organização não encontrada",
                s -> publicSitePageService.getAbout(s, true));
    }

    // ==================== FOOTER ====================

    @Operation(summary = "Obter configuração do footer (só no modo COMPLETO)")
    @GetMapping("/{slug}/footer")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<FooterConfigDTO>>> getFooter(@PathVariable String slug) {
        return handleCompletoOnly(slug, "Footer recuperado com sucesso", "Organização não encontrada",
                publicSitePageService::getFooter);
    }

    // ==================== SERVICES ====================

    @Operation(summary = "Serviços em destaque (só no modo COMPLETO)")
    @GetMapping("/{slug}/services/featured")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<ServicesSectionDTO>>> getFeaturedServices(@PathVariable String slug) {
        return handleCompletoOnly(slug, "Serviços em destaque recuperados com sucesso", "Organização não encontrada",
                publicSitePageService::getFeaturedServices);
    }

    @Operation(summary = "Listar serviços (só no modo COMPLETO)")
    @GetMapping("/{slug}/services")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<ServicesSectionDTO>>> getAllServices(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return handleCompletoOnly(slug, "Serviços recuperados com sucesso", "Organização não encontrada",
                s -> publicSitePageService.getAllServices(s, page, size));
    }

    @Operation(summary = "Detalhes de um serviço (só no modo COMPLETO)")
    @GetMapping("/{slug}/services/{id}")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<ServicoDetalhadoDTO>>> getServiceById(
            @PathVariable String slug,
            @PathVariable Long id) {
        return handleCompletoOnly(slug, "Serviço recuperado com sucesso", "Serviço não encontrado",
                s -> publicSitePageService.getServiceById(s, id));
    }

    // ==================== PRODUCTS ====================

    @Operation(summary = "Produtos em destaque (só no modo COMPLETO)")
    @GetMapping("/{slug}/products/featured")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<ProductsSectionDTO>>> getFeaturedProducts(@PathVariable String slug) {
        return handleCompletoOnly(slug, "Produtos em destaque recuperados com sucesso", "Organização não encontrada",
                publicSitePageService::getFeaturedProducts);
    }

    @Operation(summary = "Listar produtos (só no modo COMPLETO)")
    @GetMapping("/{slug}/products")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<ProductsSectionDTO>>> getAllProducts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return handleCompletoOnly(slug, "Produtos recuperados com sucesso", "Organização não encontrada",
                s -> publicSitePageService.getAllProducts(s, page, size));
    }

    @Operation(summary = "Detalhes de um produto (só no modo COMPLETO)")
    @GetMapping("/{slug}/products/{id}")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<ProdutoDetalhadoDTO>>> getProductById(
            @PathVariable String slug,
            @PathVariable Long id) {
        return handleCompletoOnly(slug, "Produto recuperado com sucesso", "Produto não encontrado",
                s -> publicSitePageService.getProductById(s, id));
    }

    // ==================== TEAM ====================

    @Operation(summary = "Equipe (só no modo COMPLETO)")
    @GetMapping("/{slug}/team")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<TeamSectionDTO>>> getTeam(@PathVariable String slug) {
        return handleCompletoOnly(slug, "Equipe recuperada com sucesso", "Organização não encontrada",
                publicSitePageService::getTeam);
    }

    // ==================== BOOKING ====================

    /**
     * Booking funciona tanto no modo BÁSICO quanto no COMPLETO — é o núcleo do site público.
     */
    @Operation(summary = "Obter informações de agendamento (disponível em todos os planos)")
    @GetMapping("/{slug}/booking")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<BookingSectionDTO>>> getBookingInfo(@PathVariable String slug) {
        return handleAmbosModos(slug, "Informações de agendamento recuperadas com sucesso", "Organização não encontrada",
                publicSitePageService::getBookingInfo);
    }

    // ==================== LEGACY/UTILITY ENDPOINTS ====================

    @Operation(summary = "Dados públicos agregados (legado — só no modo COMPLETO)")
    @GetMapping("/{slug}")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<PublicSiteResponseDTO>>> getPublicSiteBySlug(
            @PathVariable String slug) {
        return handleCompletoOnly(slug, "Dados do site recuperados com sucesso", "Organização não encontrada",
                publicSiteService::getPublicSiteBySlug);
    }

    /**
     * Verifica se um slug existe e está disponível.
     * Não passa pelo guard — este endpoint é usado para validar disponibilidade
     * de slug em cadastro/onboarding.
     */
    @Operation(summary = "Verificar se slug existe")
    @GetMapping("/{slug}/exists")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> checkSlugExists(
            @PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                normalizedSlug = "";
            }

            boolean exists = publicSiteService.existsBySlug(normalizedSlug);

            return ResponseEntity.ok(ResponseAPI.<Map<String, Object>>builder()
                    .success(true)
                    .message(exists ? "Slug encontrado" : "Slug não encontrado")
                    .dados(Map.of(
                            "slug", normalizedSlug,
                            "exists", exists,
                            "available", !exists
                    ))
                    .build());

        } catch (Exception e) {
            return serverError("Erro ao verificar slug: " + e.getMessage());
        }
    }

    /**
     * Informações básicas da organização (id, nome, slug, logo, banner).
     * Disponível em ambos os modos — o front geralmente chama primeiro para decidir a navegação.
     */
    @Operation(summary = "Informações básicas da organização (disponível em todos os planos)")
    @GetMapping("/{slug}/basic")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<OrganizacaoPublicDTO>>> getBasicInfo(
            @PathVariable String slug) {
        return handleAmbosModos(slug, "Informações básicas recuperadas com sucesso", "Organização não encontrada",
                publicSiteService::getBasicInfoBySlug);
    }

    // ==================== LANDING PAGES ====================

    @Operation(summary = "Listar landing pages publicadas (só no modo COMPLETO)")
    @GetMapping("/{slug}/pages")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<List<LandingPageDTO>>>> getPublishedPages(
            @PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequestPublic("Slug é obrigatório");
            }

            PublicSiteAccess access = publicSiteGuard.check(normalizedSlug);
            if (access.getStatus() == PublicSiteGuard.Status.NOT_FOUND) {
                return notFoundPublic("Organização não encontrada");
            }
            if (access.getStatus() == PublicSiteGuard.Status.INACTIVE) {
                return inactive();
            }
            if (access.isBasico()) {
                return okPublic("Páginas não disponíveis no plano básico",
                        ModoSite.BASICO, null);
            }

            List<LandingPageDTO> pages = publicLandingPageService.listPublishedPages(normalizedSlug);
            return okPublic("Páginas recuperadas com sucesso", ModoSite.COMPLETO, pages);

        } catch (Exception e) {
            return serverErrorPublic("Erro ao processar requisição: " + e.getMessage());
        }
    }

    @Operation(summary = "Obter landing page publicada (só no modo COMPLETO)")
    @GetMapping("/{slug}/pages/{pageSlug}")
    public ResponseEntity<ResponseAPI<PublicSiteResponse<LandingPageDTO>>> getPublishedPage(
            @PathVariable String slug,
            @PathVariable String pageSlug) {
        String normalizedPageSlug = normalizeSlug(pageSlug);
        if (normalizedPageSlug == null) {
            return badRequestPublic("Slug da página é obrigatório");
        }
        return handleCompletoOnly(slug, "Página recuperada com sucesso", "Página não encontrada",
                normalizedSlug -> publicLandingPageService.getPublishedPage(normalizedSlug, normalizedPageSlug));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Pipeline para endpoints restritos ao modo COMPLETO:
     * - NOT_FOUND → 404
     * - INACTIVE  → siteAtivo=false
     * - ACTIVE+BASICO → siteAtivo=true, modo=BASICO, conteudo=null (sem chamar o service)
     * - ACTIVE+COMPLETO → executa o fetcher normalmente
     */
    private <T> ResponseEntity<ResponseAPI<PublicSiteResponse<T>>> handleCompletoOnly(
            String slug,
            String successMessage,
            String notFoundMessage,
            java.util.function.Function<String, Optional<T>> fetcher) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequestPublic("Slug é obrigatório");
            }

            PublicSiteAccess access = publicSiteGuard.check(normalizedSlug);
            if (access.getStatus() == PublicSiteGuard.Status.NOT_FOUND) {
                return notFoundPublic(notFoundMessage);
            }
            if (access.getStatus() == PublicSiteGuard.Status.INACTIVE) {
                return inactive();
            }
            if (access.isBasico()) {
                return okPublic("Recurso não disponível no plano básico", ModoSite.BASICO, null);
            }

            Optional<T> data = fetcher.apply(normalizedSlug);
            if (data.isEmpty()) {
                return notFoundPublic(notFoundMessage);
            }

            return okPublic(successMessage, ModoSite.COMPLETO, data.get());

        } catch (Exception e) {
            return serverErrorPublic("Erro ao processar requisição: " + e.getMessage());
        }
    }

    /**
     * Pipeline para endpoints disponíveis em AMBOS os modos (ex.: booking, basic info).
     * Retorna o conteúdo com o modo real do plano.
     */
    private <T> ResponseEntity<ResponseAPI<PublicSiteResponse<T>>> handleAmbosModos(
            String slug,
            String successMessage,
            String notFoundMessage,
            java.util.function.Function<String, Optional<T>> fetcher) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequestPublic("Slug é obrigatório");
            }

            PublicSiteAccess access = publicSiteGuard.check(normalizedSlug);
            if (access.getStatus() == PublicSiteGuard.Status.NOT_FOUND) {
                return notFoundPublic(notFoundMessage);
            }
            if (access.getStatus() == PublicSiteGuard.Status.INACTIVE) {
                return inactive();
            }

            Optional<T> data = fetcher.apply(normalizedSlug);
            if (data.isEmpty()) {
                return notFoundPublic(notFoundMessage);
            }

            return okPublic(successMessage, access.getModo(), data.get());

        } catch (Exception e) {
            return serverErrorPublic("Erro ao processar requisição: " + e.getMessage());
        }
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return null;
        }
        return slug.toLowerCase().trim();
    }

    private <T> ResponseEntity<ResponseAPI<PublicSiteResponse<T>>> okPublic(String message, ModoSite modo, T data) {
        return ResponseEntity.ok(ResponseAPI.<PublicSiteResponse<T>>builder()
                .success(true)
                .message(message)
                .dados(PublicSiteResponse.ativo(modo, data))
                .build());
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ResponseAPI<PublicSiteResponse<T>>> inactive() {
        return ResponseEntity.ok(ResponseAPI.<PublicSiteResponse<T>>builder()
                .success(true)
                .message("Site inativo")
                .dados((PublicSiteResponse<T>) PublicSiteResponse.inativo())
                .build());
    }

    private <T> ResponseEntity<ResponseAPI<PublicSiteResponse<T>>> badRequestPublic(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseAPI.<PublicSiteResponse<T>>builder()
                        .success(false)
                        .message(message)
                        .errorCode(400)
                        .build());
    }

    private <T> ResponseEntity<ResponseAPI<PublicSiteResponse<T>>> notFoundPublic(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseAPI.<PublicSiteResponse<T>>builder()
                        .success(false)
                        .message(message)
                        .errorCode(404)
                        .build());
    }

    private <T> ResponseEntity<ResponseAPI<PublicSiteResponse<T>>> serverErrorPublic(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseAPI.<PublicSiteResponse<T>>builder()
                        .success(false)
                        .message(message)
                        .errorCode(500)
                        .build());
    }

    private <T> ResponseEntity<ResponseAPI<T>> serverError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseAPI.<T>builder()
                        .success(false)
                        .message(message)
                        .errorCode(500)
                        .build());
    }
}
