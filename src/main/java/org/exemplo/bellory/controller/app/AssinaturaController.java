package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.assinatura.*;
import org.exemplo.bellory.model.dto.cupom.CupomValidacaoResponseDTO;
import org.exemplo.bellory.model.dto.cupom.ValidarCupomDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.assinatura.AssinaturaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assinatura")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Assinatura", description = "Gestao da assinatura da organizacao")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @GetMapping("/status")
    @Operation(summary = "Status da minha assinatura")
    public ResponseEntity<ResponseAPI<AssinaturaStatusDTO>> getStatus() {
        try {
            Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
            if (organizacaoId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseAPI.<AssinaturaStatusDTO>builder()
                                .success(false)
                                .message("Organizacao nao identificada no token")
                                .errorCode(401)
                                .build());
            }

            AssinaturaStatusDTO status = assinaturaService.getStatusAssinatura(organizacaoId);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaStatusDTO>builder()
                    .success(true)
                    .dados(status)
                    .build());
        } catch (Exception e) {
            log.error("Erro ao buscar status da assinatura: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AssinaturaStatusDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/cobrancas")
    @Operation(summary = "Minhas cobrancas")
    public ResponseEntity<ResponseAPI<List<CobrancaPlataformaDTO>>> getCobrancas(
            @RequestParam(required = false) String status) {
        try {
            List<CobrancaPlataformaDTO> cobrancas = assinaturaService.getMinhasCobrancas(status);
            return ResponseEntity.ok(ResponseAPI.<List<CobrancaPlataformaDTO>>builder()
                    .success(true)
                    .dados(cobrancas)
                    .build());
        } catch (Exception e) {
            log.error("Erro ao buscar cobrancas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CobrancaPlataformaDTO>>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/validar-cupom")
    @Operation(summary = "Validar cupom de desconto antes de escolher plano")
    public ResponseEntity<ResponseAPI<CupomValidacaoResponseDTO>> validarCupom(
            @RequestBody @Valid ValidarCupomDTO dto) {
        try {
            CupomValidacaoResponseDTO result = assinaturaService.validarCupomParaOrganizacao(dto);
            return ResponseEntity.ok(ResponseAPI.<CupomValidacaoResponseDTO>builder()
                    .success(true)
                    .dados(result)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<CupomValidacaoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao validar cupom: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CupomValidacaoResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/escolher-plano")
    @Operation(summary = "Escolher plano (primeira vez ou pos-trial)")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> escolherPlano(
            @RequestBody @Valid EscolherPlanoDTO dto) {
        try {
            AssinaturaResponseDTO result = assinaturaService.escolherPlano(dto);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .message("Plano escolhido com sucesso")
                    .dados(result)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao escolher plano: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/trocar-plano")
    @Operation(summary = "Upgrade ou downgrade de plano com calculo pro-rata")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> trocarPlano(
            @RequestBody @Valid EscolherPlanoDTO dto) {
        try {
            AssinaturaResponseDTO result = assinaturaService.trocarPlano(dto);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .message("Plano alterado com sucesso")
                    .dados(result)
                    .build());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao trocar plano: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/cancelar")
    @Operation(summary = "Cancelar assinatura (acesso mantido ate fim do periodo)")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> cancelarAssinatura() {
        try {
            AssinaturaResponseDTO result = assinaturaService.cancelarAssinatura();
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .message("Assinatura cancelada. Voce pode continuar usando ate o fim do periodo atual.")
                    .dados(result)
                    .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
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

    @PostMapping("/reativar")
    @Operation(summary = "Reativar assinatura cancelada ou vencida")
    public ResponseEntity<ResponseAPI<AssinaturaResponseDTO>> reativarAssinatura(
            @RequestBody @Valid EscolherPlanoDTO dto) {
        try {
            AssinaturaResponseDTO result = assinaturaService.reativarAssinatura(dto);
            return ResponseEntity.ok(ResponseAPI.<AssinaturaResponseDTO>builder()
                    .success(true)
                    .message("Assinatura reativada com sucesso")
                    .dados(result)
                    .build());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<AssinaturaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
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
