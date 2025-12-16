package org.exemplo.bellory.controller;


import org.exemplo.bellory.model.dto.tenent.OrganizacaoPublicDTO;
import org.exemplo.bellory.model.dto.tenent.PublicSiteResponseDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.PublicSiteService;
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
 */
@RestController
@RequestMapping("/api/public/site")
public class PublicSiteController {

    private final PublicSiteService publicSiteService;

    public PublicSiteController(PublicSiteService publicSiteService) {
        this.publicSiteService = publicSiteService;
    }

    /**
     * Endpoint principal: busca todos os dados públicos da organização pelo slug.
     *
     * Exemplo de uso:
     * GET /api/public/site/barbeariadoje
     *
     * @param slug Identificador único da organização na URL
     * @return ResponseEntity com todos os dados públicos da organização
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ResponseAPI<PublicSiteResponseDTO>> getPublicSiteBySlug(
            @PathVariable String slug) {
        try {
            // Validar slug
            if (slug == null || slug.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<PublicSiteResponseDTO>builder()
                                .success(false)
                                .message("Slug é obrigatório")
                                .errorCode(400)
                                .build());
            }

            // Normalizar slug (lowercase, trim)
            String normalizedSlug = slug.toLowerCase().trim();

            // Buscar dados
            Optional<PublicSiteResponseDTO> siteData = publicSiteService.getPublicSiteBySlug(normalizedSlug);

            if (siteData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseAPI.<PublicSiteResponseDTO>builder()
                                .success(false)
                                .message("Organização não encontrada: " + normalizedSlug)
                                .errorCode(404)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<PublicSiteResponseDTO>builder()
                    .success(true)
                    .message("Dados do site recuperados com sucesso")
                    .dados(siteData.get())
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<PublicSiteResponseDTO>builder()
                            .success(false)
                            .message("Erro interno do servidor: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Verifica se um slug existe e está disponível.
     * Útil para validação rápida sem carregar todos os dados.
     *
     * GET /api/public/site/barbeariadoje/exists
     *
     * @param slug Identificador a verificar
     * @return ResponseEntity com boolean indicando existência
     */
    @GetMapping("/{slug}/exists")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> checkSlugExists(
            @PathVariable String slug) {
        try {
            String normalizedSlug = slug.toLowerCase().trim();
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message("Erro ao verificar slug: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Retorna apenas informações básicas da organização.
     * Útil para pré-carregamento/preview rápido.
     *
     * GET /api/public/site/barbeariadoje/basic
     *
     * @param slug Identificador da organização
     * @return ResponseEntity com dados básicos
     */
    @GetMapping("/{slug}/basic")
    public ResponseEntity<ResponseAPI<OrganizacaoPublicDTO>> getBasicInfo(
            @PathVariable String slug) {
        try {
            String normalizedSlug = slug.toLowerCase().trim();
            Optional<OrganizacaoPublicDTO> basicInfo = publicSiteService.getBasicInfoBySlug(normalizedSlug);

            if (basicInfo.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseAPI.<OrganizacaoPublicDTO>builder()
                                .success(false)
                                .message("Organização não encontrada: " + normalizedSlug)
                                .errorCode(404)
                                .build());
            }

            return ResponseEntity.ok(ResponseAPI.<OrganizacaoPublicDTO>builder()
                    .success(true)
                    .message("Informações básicas recuperadas com sucesso")
                    .dados(basicInfo.get())
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<OrganizacaoPublicDTO>builder()
                            .success(false)
                            .message("Erro interno do servidor: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
