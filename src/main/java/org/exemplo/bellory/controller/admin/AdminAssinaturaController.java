package org.exemplo.bellory.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.assinatura.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.admin.AdminAssinaturaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/assinaturas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Assinaturas", description = "Gestao de assinaturas da plataforma")
public class AdminAssinaturaController {

    private final AdminAssinaturaService adminAssinaturaService;

    // ==================== LISTAGEM GERAL ====================

    @GetMapping
    @Operation(summary = "Listar todas as assinaturas")
    public ResponseEntity<ResponseAPI<List<AssinaturaResponseDTO>>> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String planoCodigo) {
        try {
            AdminAssinaturaFiltroDTO filtro = AdminAssinaturaFiltroDTO.builder()
                    .status(status)
                    .planoCodigo(planoCodigo)
                    .build();

            List<AssinaturaResponseDTO> assinaturas = adminAssinaturaService.listarAssinaturas(filtro);
            return ResponseEntity.ok(ResponseAPI.<List<AssinaturaResponseDTO>>builder()
                    .success(true)
                    .message("Assinaturas listadas com sucesso")
                    .dados(assinaturas)
                    .build());
        } catch (Exception e) {
            log.error("Erro ao listar assinaturas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<AssinaturaResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar assinaturas: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Metricas de billing")
    public ResponseEntity<ResponseAPI<AdminBillingDashboardDTO>> dashboard() {
        try {
            AdminBillingDashboardDTO metricas = adminAssinaturaService.getDashboardMetricas();
            return ResponseEntity.ok(ResponseAPI.<AdminBillingDashboardDTO>builder()
                    .success(true)
                    .dados(metricas)
                    .build());
        } catch (Exception e) {
            log.error("Erro ao buscar metricas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AdminBillingDashboardDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== POR ASSINATURA ID ====================

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma assinatura por ID")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> detalhar(
            @Parameter(description = "ID da assinatura") @PathVariable Long id) {
        try {
            AssinaturaResponseDTO assinatura = adminAssinaturaService.detalharAssinatura(id);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .dados(assinatura)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao detalhar assinatura: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/{id}/cobrancas")
    @Operation(summary = "Listar cobrancas de uma assinatura")
    public ResponseEntity<ResponseAPI<List<CobrancaPlataformaDTO>>> listarCobrancasAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable Long id) {
        try {
            List<CobrancaPlataformaDTO> cobrancas = adminAssinaturaService.listarCobrancasAssinatura(id);
            return ResponseEntity.ok(ResponseAPI.<List<CobrancaPlataformaDTO>>builder()
                    .success(true)
                    .dados(cobrancas)
                    .build());
        } catch (Exception e) {
            log.error("Erro ao listar cobrancas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CobrancaPlataformaDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== POR ORGANIZACAO ID ====================

    @GetMapping("/organizacao/{orgId}")
    @Operation(summary = "Buscar assinatura por ID da organizacao",
               description = "Retorna a assinatura vinculada a uma organizacao especifica")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> buscarPorOrganizacao(
            @Parameter(description = "ID da organizacao") @PathVariable Long orgId) {
        try {
            AssinaturaResponseDTO assinatura = adminAssinaturaService.buscarPorOrganizacaoId(orgId);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .dados(assinatura)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao buscar assinatura por organizacao: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/organizacao/{orgId}/cobrancas")
    @Operation(summary = "Listar cobrancas de uma organizacao",
               description = "Retorna todas as cobrancas da plataforma para uma organizacao, ordenadas por vencimento")
    public ResponseEntity<ResponseAPI<List<CobrancaPlataformaDTO>>> listarCobrancasOrganizacao(
            @Parameter(description = "ID da organizacao") @PathVariable Long orgId) {
        try {
            List<CobrancaPlataformaDTO> cobrancas = adminAssinaturaService.listarCobrancasPorOrganizacao(orgId);
            return ResponseEntity.ok(ResponseAPI.<List<CobrancaPlataformaDTO>>builder()
                    .success(true)
                    .dados(cobrancas)
                    .build());
        } catch (Exception e) {
            log.error("Erro ao listar cobrancas da organizacao: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CobrancaPlataformaDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/organizacao/{orgId}/pagamentos")
    @Operation(summary = "Listar pagamentos de uma organizacao",
               description = "Retorna todos os pagamentos confirmados/pendentes de uma organizacao")
    public ResponseEntity<ResponseAPI<List<PagamentoPlataformaDTO>>> listarPagamentosOrganizacao(
            @Parameter(description = "ID da organizacao") @PathVariable Long orgId) {
        try {
            List<PagamentoPlataformaDTO> pagamentos = adminAssinaturaService.listarPagamentosPorOrganizacao(orgId);
            return ResponseEntity.ok(ResponseAPI.<List<PagamentoPlataformaDTO>>builder()
                    .success(true)
                    .dados(pagamentos)
                    .build());
        } catch (Exception e) {
            log.error("Erro ao listar pagamentos da organizacao: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<PagamentoPlataformaDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== PAGAMENTOS POR COBRANCA ====================

    @GetMapping("/cobrancas/{cobrancaId}/pagamentos")
    @Operation(summary = "Listar pagamentos de uma cobranca especifica")
    public ResponseEntity<ResponseAPI<List<PagamentoPlataformaDTO>>> listarPagamentosCobranca(
            @Parameter(description = "ID da cobranca") @PathVariable Long cobrancaId) {
        try {
            List<PagamentoPlataformaDTO> pagamentos = adminAssinaturaService.listarPagamentosCobranca(cobrancaId);
            return ResponseEntity.ok(ResponseAPI.<List<PagamentoPlataformaDTO>>builder()
                    .success(true)
                    .dados(pagamentos)
                    .build());
        } catch (Exception e) {
            log.error("Erro ao listar pagamentos da cobranca: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<PagamentoPlataformaDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== ACOES ====================

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar assinatura")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> cancelar(
            @Parameter(description = "ID da assinatura") @PathVariable Long id) {
        try {
            AssinaturaResponseDTO result = adminAssinaturaService.cancelarAssinatura(id);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .message("Assinatura cancelada com sucesso")
                    .dados(result)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao cancelar assinatura: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/{id}/suspender")
    @Operation(summary = "Suspender assinatura")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> suspender(
            @Parameter(description = "ID da assinatura") @PathVariable Long id) {
        try {
            AssinaturaResponseDTO result = adminAssinaturaService.suspenderAssinatura(id);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .message("Assinatura suspensa com sucesso")
                    .dados(result)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao suspender assinatura: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/{id}/reativar")
    @Operation(summary = "Reativar assinatura")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> reativar(
            @Parameter(description = "ID da assinatura") @PathVariable Long id) {
        try {
            AssinaturaResponseDTO result = adminAssinaturaService.reativarAssinatura(id);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .message("Assinatura reativada com sucesso")
                    .dados(result)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao reativar assinatura: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
