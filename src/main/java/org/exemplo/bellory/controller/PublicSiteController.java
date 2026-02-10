package org.exemplo.bellory.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.site.*;
import org.exemplo.bellory.model.dto.tenent.OrganizacaoPublicDTO;
import org.exemplo.bellory.model.dto.tenent.PublicSiteResponseDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.PublicSiteService;
import org.exemplo.bellory.service.site.PublicSitePageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller REST público para o site/landing page das organizações.
 *
 * IMPORTANTE: Este controller NÃO requer autenticação.
 * Os endpoints são públicos e destinados ao acesso externo.
 *
 * Fluxo de uso:
 * 1. Frontend acessa: app.bellory.com.br/barbeariadoje
 * 2. Extrai slug da URL: "barbeariadoje"
 * 3. Chama: GET /api/public/site/barbeariadoje
 * 4. Recebe todos os dados para renderizar a página
 *
 * ENDPOINTS DISPONÍVEIS:
 *
 * HOME PAGE:
 * - GET /{slug}/home - Retorna todos os dados da home page
 *
 * SEÇÕES INDIVIDUAIS (para lazy loading):
 * - GET /{slug}/header - Configuração do header
 * - GET /{slug}/hero - Dados do hero
 * - GET /{slug}/about - Seção sobre (resumida)
 * - GET /{slug}/about/full - Seção sobre (completa)
 * - GET /{slug}/footer - Configuração do footer
 *
 * SERVIÇOS:
 * - GET /{slug}/services/featured - Serviços em destaque
 * - GET /{slug}/services - Lista todos os serviços (paginado)
 * - GET /{slug}/services/{id} - Detalhes de um serviço
 *
 * PRODUTOS:
 * - GET /{slug}/products/featured - Produtos em destaque
 * - GET /{slug}/products - Lista todos os produtos (paginado)
 * - GET /{slug}/products/{id} - Detalhes de um produto
 *
 * EQUIPE:
 * - GET /{slug}/team - Lista a equipe
 *
 * AGENDAMENTO:
 * - GET /{slug}/booking - Informações de agendamento
 *
 * UTILITÁRIOS:
 * - GET /{slug} - Dados completos (legado)
 * - GET /{slug}/exists - Verifica se slug existe
 * - GET /{slug}/basic - Informações básicas
 */
@RestController
@RequestMapping("/api/public/site")
@Tag(name = "Site Público", description = "Endpoints públicos para renderização do site das organizações")
public class PublicSiteController {

    private final PublicSiteService publicSiteService;
    private final PublicSitePageService publicSitePageService;

    public PublicSiteController(PublicSiteService publicSiteService,
                                PublicSitePageService publicSitePageService) {
        this.publicSiteService = publicSiteService;
        this.publicSitePageService = publicSitePageService;
    }

    // ==================== HOME PAGE ====================

    /**
     * Endpoint principal para a home page.
     * Retorna todos os dados necessários para renderizar a página inicial.
     *
     * GET /api/public/site/{slug}/home
     */
    @Operation(summary = "Obter home page completa")
    @GetMapping("/{slug}/home")
    public ResponseEntity<ResponseAPI<HomePageDTO>> getHomePage(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<HomePageDTO> homePage = publicSitePageService.getHomePage(normalizedSlug);

            if (homePage.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Home page recuperada com sucesso", homePage.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar home page: " + e.getMessage());
        }
    }

    // ==================== HEADER ====================

    /**
     * Retorna a configuração do header.
     *
     * GET /api/public/site/{slug}/header
     */
    @Operation(summary = "Obter configuração do header")
    @GetMapping("/{slug}/header")
    public ResponseEntity<ResponseAPI<HeaderConfigDTO>> getHeader(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<HeaderConfigDTO> header = publicSitePageService.getHeader(normalizedSlug);

            if (header.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Header recuperado com sucesso", header.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar header: " + e.getMessage());
        }
    }

    // ==================== HERO ====================

    /**
     * Retorna os dados do hero/banner principal.
     *
     * GET /api/public/site/{slug}/hero
     */
    @Operation(summary = "Obter dados do hero/banner")
    @GetMapping("/{slug}/hero")
    public ResponseEntity<ResponseAPI<HeroSectionDTO>> getHero(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<HeroSectionDTO> hero = publicSitePageService.getHero(normalizedSlug);

            if (hero.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Hero recuperado com sucesso", hero.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar hero: " + e.getMessage());
        }
    }

    // ==================== ABOUT ====================

    /**
     * Retorna a seção sobre (versão resumida para home).
     *
     * GET /api/public/site/{slug}/about
     */
    @Operation(summary = "Obter seção sobre (resumida)")
    @GetMapping("/{slug}/about")
    public ResponseEntity<ResponseAPI<AboutSectionDTO>> getAbout(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<AboutSectionDTO> about = publicSitePageService.getAbout(normalizedSlug, false);

            if (about.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Seção sobre recuperada com sucesso", about.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar seção sobre: " + e.getMessage());
        }
    }

    /**
     * Retorna a seção sobre completa (para página dedicada).
     *
     * GET /api/public/site/{slug}/about/full
     */
    @Operation(summary = "Obter seção sobre (completa)")
    @GetMapping("/{slug}/about/full")
    public ResponseEntity<ResponseAPI<AboutSectionDTO>> getAboutFull(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<AboutSectionDTO> about = publicSitePageService.getAbout(normalizedSlug, true);

            if (about.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Página sobre recuperada com sucesso", about.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar página sobre: " + e.getMessage());
        }
    }

    // ==================== FOOTER ====================

    /**
     * Retorna a configuração do footer.
     *
     * GET /api/public/site/{slug}/footer
     */
    @Operation(summary = "Obter configuração do footer")
    @GetMapping("/{slug}/footer")
    public ResponseEntity<ResponseAPI<FooterConfigDTO>> getFooter(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<FooterConfigDTO> footer = publicSitePageService.getFooter(normalizedSlug);

            if (footer.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Footer recuperado com sucesso", footer.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar footer: " + e.getMessage());
        }
    }

    // ==================== SERVICES ====================

    /**
     * Retorna os serviços em destaque.
     *
     * GET /api/public/site/{slug}/services/featured
     */
    @Operation(summary = "Obter serviços em destaque")
    @GetMapping("/{slug}/services/featured")
    public ResponseEntity<ResponseAPI<ServicesSectionDTO>> getFeaturedServices(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<ServicesSectionDTO> services = publicSitePageService.getFeaturedServices(normalizedSlug);

            if (services.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Serviços em destaque recuperados com sucesso", services.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar serviços em destaque: " + e.getMessage());
        }
    }

    /**
     * Retorna todos os serviços (paginado).
     *
     * GET /api/public/site/{slug}/services?page=0&size=10
     */
    @Operation(summary = "Listar todos os serviços (paginado)")
    @GetMapping("/{slug}/services")
    public ResponseEntity<ResponseAPI<ServicesSectionDTO>> getAllServices(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<ServicesSectionDTO> services = publicSitePageService.getAllServices(normalizedSlug, page, size);

            if (services.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Serviços recuperados com sucesso", services.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar serviços: " + e.getMessage());
        }
    }

    /**
     * Retorna detalhes de um serviço específico.
     *
     * GET /api/public/site/{slug}/services/{id}
     */
    @Operation(summary = "Obter detalhes de um serviço")
    @GetMapping("/{slug}/services/{id}")
    public ResponseEntity<ResponseAPI<ServicoDetalhadoDTO>> getServiceById(
            @PathVariable String slug,
            @PathVariable Long id) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<ServicoDetalhadoDTO> servico = publicSitePageService.getServiceById(normalizedSlug, id);

            if (servico.isEmpty()) {
                return notFound("Serviço não encontrado");
            }

            return success("Serviço recuperado com sucesso", servico.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar serviço: " + e.getMessage());
        }
    }

    // ==================== PRODUCTS ====================

    /**
     * Retorna os produtos em destaque.
     *
     * GET /api/public/site/{slug}/products/featured
     */
    @Operation(summary = "Obter produtos em destaque")
    @GetMapping("/{slug}/products/featured")
    public ResponseEntity<ResponseAPI<ProductsSectionDTO>> getFeaturedProducts(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<ProductsSectionDTO> products = publicSitePageService.getFeaturedProducts(normalizedSlug);

            if (products.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Produtos em destaque recuperados com sucesso", products.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar produtos em destaque: " + e.getMessage());
        }
    }

    /**
     * Retorna todos os produtos (paginado).
     *
     * GET /api/public/site/{slug}/products?page=0&size=12
     */
    @Operation(summary = "Listar todos os produtos (paginado)")
    @GetMapping("/{slug}/products")
    public ResponseEntity<ResponseAPI<ProductsSectionDTO>> getAllProducts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<ProductsSectionDTO> products = publicSitePageService.getAllProducts(normalizedSlug, page, size);

            if (products.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Produtos recuperados com sucesso", products.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar produtos: " + e.getMessage());
        }
    }

    /**
     * Retorna detalhes de um produto específico.
     *
     * GET /api/public/site/{slug}/products/{id}
     */
    @Operation(summary = "Obter detalhes de um produto")
    @GetMapping("/{slug}/products/{id}")
    public ResponseEntity<ResponseAPI<ProdutoDetalhadoDTO>> getProductById(
            @PathVariable String slug,
            @PathVariable Long id) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<ProdutoDetalhadoDTO> produto = publicSitePageService.getProductById(normalizedSlug, id);

            if (produto.isEmpty()) {
                return notFound("Produto não encontrado");
            }

            return success("Produto recuperado com sucesso", produto.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar produto: " + e.getMessage());
        }
    }

    // ==================== TEAM ====================

    /**
     * Retorna a equipe/funcionários.
     *
     * GET /api/public/site/{slug}/team
     */
    @Operation(summary = "Obter equipe/funcionários")
    @GetMapping("/{slug}/team")
    public ResponseEntity<ResponseAPI<TeamSectionDTO>> getTeam(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<TeamSectionDTO> team = publicSitePageService.getTeam(normalizedSlug);

            if (team.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Equipe recuperada com sucesso", team.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar equipe: " + e.getMessage());
        }
    }

    // ==================== BOOKING ====================

    /**
     * Retorna informações para agendamento.
     *
     * GET /api/public/site/{slug}/booking
     */
    @Operation(summary = "Obter informações de agendamento")
    @GetMapping("/{slug}/booking")
    public ResponseEntity<ResponseAPI<BookingSectionDTO>> getBookingInfo(@PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<BookingSectionDTO> booking = publicSitePageService.getBookingInfo(normalizedSlug);

            if (booking.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Informações de agendamento recuperadas com sucesso", booking.get());

        } catch (Exception e) {
            return serverError("Erro ao recuperar informações de agendamento: " + e.getMessage());
        }
    }

    // ==================== LEGACY/UTILITY ENDPOINTS ====================

    /**
     * Endpoint legado: busca todos os dados públicos da organização pelo slug.
     * Mantido para compatibilidade com integrações existentes.
     *
     * GET /api/public/site/{slug}
     */
    @Operation(summary = "Obter todos os dados públicos (legado)")
    @GetMapping("/{slug}")
    public ResponseEntity<ResponseAPI<PublicSiteResponseDTO>> getPublicSiteBySlug(
            @PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<PublicSiteResponseDTO> siteData = publicSiteService.getPublicSiteBySlug(normalizedSlug);

            if (siteData.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Dados do site recuperados com sucesso", siteData.get());

        } catch (Exception e) {
            return serverError("Erro interno do servidor: " + e.getMessage());
        }
    }

    /**
     * Verifica se um slug existe e está disponível.
     *
     * GET /api/public/site/{slug}/exists
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
     * Retorna apenas informações básicas da organização.
     *
     * GET /api/public/site/{slug}/basic
     */
    @Operation(summary = "Obter informações básicas da organização")
    @GetMapping("/{slug}/basic")
    public ResponseEntity<ResponseAPI<OrganizacaoPublicDTO>> getBasicInfo(
            @PathVariable String slug) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            Optional<OrganizacaoPublicDTO> basicInfo = publicSiteService.getBasicInfoBySlug(normalizedSlug);

            if (basicInfo.isEmpty()) {
                return notFound("Organização não encontrada: " + normalizedSlug);
            }

            return success("Informações básicas recuperadas com sucesso", basicInfo.get());

        } catch (Exception e) {
            return serverError("Erro interno do servidor: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    private String normalizeSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return null;
        }
        return slug.toLowerCase().trim();
    }

    private <T> ResponseEntity<ResponseAPI<T>> success(String message, T data) {
        return ResponseEntity.ok(ResponseAPI.<T>builder()
                .success(true)
                .message(message)
                .dados(data)
                .build());
    }

    private <T> ResponseEntity<ResponseAPI<T>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseAPI.<T>builder()
                        .success(false)
                        .message(message)
                        .errorCode(400)
                        .build());
    }

    private <T> ResponseEntity<ResponseAPI<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseAPI.<T>builder()
                        .success(false)
                        .message(message)
                        .errorCode(404)
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
