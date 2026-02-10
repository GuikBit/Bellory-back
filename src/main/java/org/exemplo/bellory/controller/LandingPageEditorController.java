package org.exemplo.bellory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.exemplo.bellory.model.dto.landingpage.*;
import org.exemplo.bellory.model.dto.landingpage.request.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.landingpage.enums.ElementType;
import org.exemplo.bellory.model.entity.landingpage.enums.SectionType;
import org.exemplo.bellory.service.landingpage.LandingPageEditorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller para o Editor de Landing Pages.
 *
 * Este controller gerencia todas as operações CRUD para:
 * - Landing Pages (criar, editar, publicar, duplicar, deletar)
 * - Seções (adicionar, editar, reordenar, deletar)
 * - Versões (histórico, restauração)
 *
 * BASE URL: /api/landing-pages
 *
 * AUTENTICAÇÃO: Requer token JWT ou API Key válido.
 */
@RestController
@RequestMapping("/api/landing-pages")
@Tag(name = "Editor de Landing Pages", description = "Gerenciamento de landing pages, seções e versões")
public class LandingPageEditorController {

    private final LandingPageEditorService editorService;

    public LandingPageEditorController(LandingPageEditorService editorService) {
        this.editorService = editorService;
    }

    // ==================== LANDING PAGE ENDPOINTS ====================

    /**
     * Lista todas as landing pages da organização.
     *
     * GET /api/landing-pages
     */
    @Operation(summary = "Listar todas as landing pages")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<LandingPageDTO>>> listAll() {
        try {
            List<LandingPageDTO> pages = editorService.listAll();
            return success("Landing pages recuperadas com sucesso", pages);
        } catch (Exception e) {
            return serverError("Erro ao listar landing pages: " + e.getMessage());
        }
    }

    /**
     * Lista landing pages com paginação.
     *
     * GET /api/landing-pages/paginated?page=0&size=10
     */
    @Operation(summary = "Listar landing pages com paginação")
    @GetMapping("/paginated")
    public ResponseEntity<ResponseAPI<Page<LandingPageDTO>>> listPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<LandingPageDTO> pages = editorService.listPaginated(
                    PageRequest.of(page, size, Sort.by("dtCriacao").descending()));
            return success("Landing pages recuperadas com sucesso", pages);
        } catch (Exception e) {
            return serverError("Erro ao listar landing pages: " + e.getMessage());
        }
    }

    /**
     * Busca landing page por ID com todas as seções.
     *
     * GET /api/landing-pages/{id}
     */
    @Operation(summary = "Buscar landing page por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<LandingPageDTO>> getById(@PathVariable Long id) {
        try {
            return editorService.getById(id)
                    .map(dto -> success("Landing page recuperada com sucesso", dto))
                    .orElse(notFound("Landing page não encontrada"));
        } catch (Exception e) {
            return serverError("Erro ao buscar landing page: " + e.getMessage());
        }
    }

    /**
     * Busca landing page por slug.
     *
     * GET /api/landing-pages/by-slug/{slug}
     */
    @Operation(summary = "Buscar landing page por slug")
    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<ResponseAPI<LandingPageDTO>> getBySlug(@PathVariable String slug) {
        try {
            return editorService.getBySlug(slug)
                    .map(dto -> success("Landing page recuperada com sucesso", dto))
                    .orElse(notFound("Landing page não encontrada"));
        } catch (Exception e) {
            return serverError("Erro ao buscar landing page: " + e.getMessage());
        }
    }

    /**
     * Cria uma nova landing page.
     *
     * POST /api/landing-pages
     */
    @Operation(summary = "Criar nova landing page")
    @PostMapping
    public ResponseEntity<ResponseAPI<LandingPageDTO>> create(
            @Valid @RequestBody CreateLandingPageRequest request) {
        try {
            LandingPageDTO dto = editorService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<LandingPageDTO>builder()
                            .success(true)
                            .message("Landing page criada com sucesso")
                            .dados(dto)
                            .build());
        } catch (Exception e) {
            return serverError("Erro ao criar landing page: " + e.getMessage());
        }
    }

    /**
     * Atualiza uma landing page.
     *
     * PUT /api/landing-pages/{id}
     */
    @Operation(summary = "Atualizar landing page")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<LandingPageDTO>> update(
            @PathVariable Long id,
            @RequestBody UpdateLandingPageRequest request) {
        try {
            LandingPageDTO dto = editorService.update(id, request);
            return success("Landing page atualizada com sucesso", dto);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar landing page: " + e.getMessage());
        }
    }

    /**
     * Publica uma landing page.
     *
     * POST /api/landing-pages/{id}/publish
     */
    @Operation(summary = "Publicar landing page")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ResponseAPI<LandingPageDTO>> publish(@PathVariable Long id) {
        try {
            LandingPageDTO dto = editorService.publish(id);
            return success("Landing page publicada com sucesso", dto);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao publicar landing page: " + e.getMessage());
        }
    }

    /**
     * Despublica uma landing page.
     *
     * POST /api/landing-pages/{id}/unpublish
     */
    @Operation(summary = "Despublicar landing page")
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ResponseAPI<LandingPageDTO>> unpublish(@PathVariable Long id) {
        try {
            LandingPageDTO dto = editorService.unpublish(id);
            return success("Landing page despublicada com sucesso", dto);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao despublicar landing page: " + e.getMessage());
        }
    }

    /**
     * Duplica uma landing page.
     *
     * POST /api/landing-pages/{id}/duplicate
     */
    @Operation(summary = "Duplicar landing page")
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ResponseAPI<LandingPageDTO>> duplicate(
            @PathVariable Long id,
            @RequestParam(required = false) String novoNome) {
        try {
            LandingPageDTO dto = editorService.duplicate(id, novoNome);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<LandingPageDTO>builder()
                            .success(true)
                            .message("Landing page duplicada com sucesso")
                            .dados(dto)
                            .build());
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao duplicar landing page: " + e.getMessage());
        }
    }

    /**
     * Deleta uma landing page.
     *
     * DELETE /api/landing-pages/{id}
     */
    @Operation(summary = "Deletar landing page")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> delete(@PathVariable Long id) {
        try {
            editorService.delete(id);
            return success("Landing page deletada com sucesso", null);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao deletar landing page: " + e.getMessage());
        }
    }

    // ==================== SECTION ENDPOINTS ====================

    /**
     * Adiciona uma nova seção.
     *
     * POST /api/landing-pages/{landingPageId}/sections
     */
    @Operation(summary = "Adicionar seção")
    @PostMapping("/{landingPageId}/sections")
    public ResponseEntity<ResponseAPI<LandingPageSectionDTO>> addSection(
            @PathVariable Long landingPageId,
            @Valid @RequestBody AddSectionRequest request) {
        try {
            LandingPageSectionDTO dto = editorService.addSection(landingPageId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<LandingPageSectionDTO>builder()
                            .success(true)
                            .message("Seção adicionada com sucesso")
                            .dados(dto)
                            .build());
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao adicionar seção: " + e.getMessage());
        }
    }

    /**
     * Atualiza uma seção.
     *
     * PUT /api/landing-pages/{landingPageId}/sections/{sectionId}
     */
    @Operation(summary = "Atualizar seção")
    @PutMapping("/{landingPageId}/sections/{sectionId}")
    public ResponseEntity<ResponseAPI<LandingPageSectionDTO>> updateSection(
            @PathVariable Long landingPageId,
            @PathVariable String sectionId,
            @RequestBody UpdateSectionRequest request) {
        try {
            LandingPageSectionDTO dto = editorService.updateSection(landingPageId, sectionId, request);
            return success("Seção atualizada com sucesso", dto);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar seção: " + e.getMessage());
        }
    }

    /**
     * Reordena seções.
     *
     * PUT /api/landing-pages/{landingPageId}/sections/reorder
     */
    @Operation(summary = "Reordenar seções")
    @PutMapping("/{landingPageId}/sections/reorder")
    public ResponseEntity<ResponseAPI<List<LandingPageSectionDTO>>> reorderSections(
            @PathVariable Long landingPageId,
            @Valid @RequestBody ReorderSectionsRequest request) {
        try {
            List<LandingPageSectionDTO> sections = editorService.reorderSections(landingPageId, request);
            return success("Seções reordenadas com sucesso", sections);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao reordenar seções: " + e.getMessage());
        }
    }

    /**
     * Duplica uma seção.
     *
     * POST /api/landing-pages/{landingPageId}/sections/{sectionId}/duplicate
     */
    @Operation(summary = "Duplicar seção")
    @PostMapping("/{landingPageId}/sections/{sectionId}/duplicate")
    public ResponseEntity<ResponseAPI<LandingPageSectionDTO>> duplicateSection(
            @PathVariable Long landingPageId,
            @PathVariable String sectionId) {
        try {
            LandingPageSectionDTO dto = editorService.duplicateSection(landingPageId, sectionId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<LandingPageSectionDTO>builder()
                            .success(true)
                            .message("Seção duplicada com sucesso")
                            .dados(dto)
                            .build());
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao duplicar seção: " + e.getMessage());
        }
    }

    /**
     * Deleta uma seção.
     *
     * DELETE /api/landing-pages/{landingPageId}/sections/{sectionId}
     */
    @Operation(summary = "Deletar seção")
    @DeleteMapping("/{landingPageId}/sections/{sectionId}")
    public ResponseEntity<ResponseAPI<Void>> deleteSection(
            @PathVariable Long landingPageId,
            @PathVariable String sectionId) {
        try {
            editorService.deleteSection(landingPageId, sectionId);
            return success("Seção deletada com sucesso", null);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao deletar seção: " + e.getMessage());
        }
    }

    // ==================== VERSION ENDPOINTS ====================

    /**
     * Lista versões de uma landing page.
     *
     * GET /api/landing-pages/{landingPageId}/versions
     */
    @Operation(summary = "Listar versões")
    @GetMapping("/{landingPageId}/versions")
    public ResponseEntity<ResponseAPI<List<LandingPageVersionDTO>>> listVersions(
            @PathVariable Long landingPageId) {
        try {
            List<LandingPageVersionDTO> versions = editorService.listVersions(landingPageId);
            return success("Versões recuperadas com sucesso", versions);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao listar versões: " + e.getMessage());
        }
    }

    /**
     * Restaura uma versão anterior.
     *
     * POST /api/landing-pages/{landingPageId}/versions/{versao}/restore
     */
    @Operation(summary = "Restaurar versão anterior")
    @PostMapping("/{landingPageId}/versions/{versao}/restore")
    public ResponseEntity<ResponseAPI<LandingPageDTO>> restoreVersion(
            @PathVariable Long landingPageId,
            @PathVariable Integer versao) {
        try {
            LandingPageDTO dto = editorService.restoreVersion(landingPageId, versao);
            return success("Versão restaurada com sucesso", dto);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao restaurar versão: " + e.getMessage());
        }
    }

    // ==================== METADATA ENDPOINTS ====================

    /**
     * Retorna os tipos de seção disponíveis.
     *
     * GET /api/landing-pages/metadata/section-types
     */
    @Operation(summary = "Obter tipos de seção disponíveis")
    @GetMapping("/metadata/section-types")
    public ResponseEntity<ResponseAPI<List<Map<String, String>>>> getSectionTypes() {
        List<Map<String, String>> types = Arrays.stream(SectionType.values())
                .map(type -> Map.of(
                        "value", type.name(),
                        "label", type.getLabel(),
                        "description", type.getDescription(),
                        "icon", type.getIcon()
                ))
                .collect(Collectors.toList());
        return success("Tipos de seção recuperados com sucesso", types);
    }

    /**
     * Retorna os tipos de elemento disponíveis.
     *
     * GET /api/landing-pages/metadata/element-types
     */
    @Operation(summary = "Obter tipos de elemento disponíveis")
    @GetMapping("/metadata/element-types")
    public ResponseEntity<ResponseAPI<List<Map<String, String>>>> getElementTypes() {
        List<Map<String, String>> types = Arrays.stream(ElementType.values())
                .map(type -> Map.of(
                        "value", type.name(),
                        "type", type.getType(),
                        "label", type.getLabel(),
                        "description", type.getDescription()
                ))
                .collect(Collectors.toList());
        return success("Tipos de elemento recuperados com sucesso", types);
    }

    // ==================== HELPER METHODS ====================

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
