package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.site.SitePublicoConfigDTO;
import org.exemplo.bellory.model.dto.site.request.*;
import org.exemplo.bellory.model.dto.site.request.TransitionConfigRequest;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.SiteConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para gerenciamento das configurações do site público da organização.
 * Requer autenticação - usa TenantContext para identificar a organização.
 */
@RestController
@RequestMapping("/api/v1/site-config")
@RequiredArgsConstructor
@Tag(name = "Configuração do Site Público", description = "Gerenciamento das configurações de personalização do site público")
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    // ==================== GET ====================

    @Operation(summary = "Obter configuração do site da organização")
    @GetMapping
    public ResponseEntity<ResponseAPI<SitePublicoConfigDTO>> getConfig() {
        try {
            SitePublicoConfigDTO config = siteConfigService.buscarConfig();
            return success("Configuração recuperada com sucesso", config);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao recuperar configuração: " + e.getMessage());
        }
    }

    // ==================== PUT (FULL SAVE) ====================

    @Operation(summary = "Salvar/atualizar configuração completa do site")
    @PutMapping
    public ResponseEntity<ResponseAPI<SitePublicoConfigDTO>> saveConfig(
            @RequestBody SitePublicoConfigRequest request) {
        try {
            SitePublicoConfigDTO saved = siteConfigService.salvarConfigCompleta(request);
            return success("Configuração salva com sucesso", saved);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao salvar configuração: " + e.getMessage());
        }
    }

    // ==================== PATCH PER SECTION ====================

    @Operation(summary = "Atualizar seção Hero")
    @PatchMapping("/hero")
    public ResponseEntity<ResponseAPI<HeroSectionRequest>> updateHero(
            @RequestBody HeroSectionRequest request) {
        try {
            HeroSectionRequest updated = siteConfigService.atualizarHero(request);
            return success("Seção hero atualizada com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar seção hero: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar configuração do Header")
    @PatchMapping("/header")
    public ResponseEntity<ResponseAPI<HeaderConfigRequest>> updateHeader(
            @RequestBody HeaderConfigRequest request) {
        try {
            HeaderConfigRequest updated = siteConfigService.atualizarHeader(request);
            return success("Header atualizado com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar header: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar seção Sobre")
    @PatchMapping("/about")
    public ResponseEntity<ResponseAPI<AboutSectionRequest>> updateAbout(
            @RequestBody AboutSectionRequest request) {
        try {
            AboutSectionRequest updated = siteConfigService.atualizarAbout(request);
            return success("Seção sobre atualizada com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar seção sobre: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar configuração do Footer")
    @PatchMapping("/footer")
    public ResponseEntity<ResponseAPI<FooterConfigRequest>> updateFooter(
            @RequestBody FooterConfigRequest request) {
        try {
            FooterConfigRequest updated = siteConfigService.atualizarFooter(request);
            return success("Footer atualizado com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar footer: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar configuração de Serviços")
    @PatchMapping("/services")
    public ResponseEntity<ResponseAPI<ServicesSectionRequest>> updateServices(
            @RequestBody ServicesSectionRequest request) {
        try {
            ServicesSectionRequest updated = siteConfigService.atualizarServices(request);
            return success("Configuração de serviços atualizada com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar configuração de serviços: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar configuração de Produtos")
    @PatchMapping("/products")
    public ResponseEntity<ResponseAPI<ProductsSectionRequest>> updateProducts(
            @RequestBody ProductsSectionRequest request) {
        try {
            ProductsSectionRequest updated = siteConfigService.atualizarProducts(request);
            return success("Configuração de produtos atualizada com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar configuração de produtos: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar configuração da Equipe")
    @PatchMapping("/team")
    public ResponseEntity<ResponseAPI<TeamSectionRequest>> updateTeam(
            @RequestBody TeamSectionRequest request) {
        try {
            TeamSectionRequest updated = siteConfigService.atualizarTeam(request);
            return success("Configuração da equipe atualizada com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar configuração da equipe: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar configuração de Agendamento")
    @PatchMapping("/booking")
    public ResponseEntity<ResponseAPI<BookingSectionRequest>> updateBooking(
            @RequestBody BookingSectionRequest request) {
        try {
            BookingSectionRequest updated = siteConfigService.atualizarBooking(request);
            return success("Configuração de agendamento atualizada com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar configuração de agendamento: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar configurações gerais (ordem das seções, CSS/JS)")
    @PatchMapping("/general")
    public ResponseEntity<ResponseAPI<GeneralSettingsRequest>> updateGeneral(
            @RequestBody GeneralSettingsRequest request) {
        try {
            GeneralSettingsRequest updated = siteConfigService.atualizarGeneral(request);
            return success("Configurações gerais atualizadas com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar configurações gerais: " + e.getMessage());
        }
    }

    @Operation(summary = "Ativar/desativar o site público")
    @PatchMapping("/status")
    public ResponseEntity<ResponseAPI<SitePublicoConfigDTO>> updateStatus(
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean active = request != null ? request.get("active") : null;
            SitePublicoConfigDTO updated = siteConfigService.alterarStatus(active);
            String msg = Boolean.TRUE.equals(updated.getActive())
                    ? "Site público ativado com sucesso."
                    : "Site público desativado com sucesso.";
            return success(msg, updated);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao alterar status do site: " + e.getMessage());
        }
    }

    @Operation(summary = "Atualizar configuração de transições entre seções")
    @PatchMapping("/transitions")
    public ResponseEntity<ResponseAPI<Map<String, TransitionConfigRequest>>> updateTransitions(
            @RequestBody Map<String, TransitionConfigRequest> request) {
        try {
            Map<String, TransitionConfigRequest> updated = siteConfigService.atualizarTransitions(request);
            return success("Transições atualizadas com sucesso", updated);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao atualizar transições: " + e.getMessage());
        }
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
