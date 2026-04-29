package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.questionario.AuditoriaTermoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.questionario.RespostaQuestionarioService;
import org.exemplo.bellory.service.questionario.RespostaQuestionarioService.AssinaturaDownload;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de auditoria e download de assinatura. O projeto nao tem
 * {@code @EnableMethodSecurity}, portanto a verificacao de role e feita
 * manualmente via {@link TenantContext#getCurrentRole()}.
 */
@RestController
@RequestMapping("/api/v1/resposta-questionario")
@Tag(name = "Auditoria de Termos", description = "Auditoria de termos aceitos e download de assinaturas")
public class AuditoriaTermoController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_SUPERADMIN = "ROLE_SUPERADMIN";

    private final RespostaQuestionarioService respostaService;

    public AuditoriaTermoController(RespostaQuestionarioService respostaService) {
        this.respostaService = respostaService;
    }

    @GetMapping("/{id}/auditoria")
    @Operation(summary = "Relatório de auditoria de termos e assinaturas",
            description = "Recalcula o hash do termo aceito e compara com o armazenado. Requer role ADMIN.")
    public ResponseEntity<ResponseAPI<AuditoriaTermoDTO>> auditoria(@PathVariable Long id) {
        try {
            exigirAdmin();
            AuditoriaTermoDTO dto = respostaService.obterAuditoria(id);
            return ResponseEntity.ok(ResponseAPI.<AuditoriaTermoDTO>builder()
                    .success(true)
                    .dados(dto)
                    .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<AuditoriaTermoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<AuditoriaTermoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @GetMapping(value = "/{id}/assinatura/{tipo}", produces = {MediaType.IMAGE_PNG_VALUE, "image/svg+xml"})
    @Operation(summary = "Download da imagem de assinatura",
            description = "Devolve PNG/SVG da assinatura do cliente ou profissional. Requer role ADMIN ou cliente dono da resposta.")
    public ResponseEntity<byte[]> baixarAssinatura(@PathVariable Long id,
                                                   @PathVariable String tipo,
                                                   @RequestParam Long perguntaId) {
        if (!"cliente".equalsIgnoreCase(tipo) && !"profissional".equalsIgnoreCase(tipo)) {
            return ResponseEntity.badRequest().build();
        }
        boolean profissional = "profissional".equalsIgnoreCase(tipo);

        try {
            AssinaturaDownload download = respostaService.buscarBytesAssinatura(id, perguntaId, profissional);
            exigirAdminOuCliente(download.clienteId());

            MediaType contentType = detectarContentType(download.bytes());
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(download.bytes());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ===== helpers =====

    private void exigirAdmin() {
        String role = TenantContext.getCurrentRole();
        if (!ROLE_ADMIN.equals(role) && !ROLE_SUPERADMIN.equals(role)) {
            throw new SecurityException("Acesso negado: requer role ADMIN.");
        }
    }

    private void exigirAdminOuCliente(Long clienteIdResposta) {
        String role = TenantContext.getCurrentRole();
        if (ROLE_ADMIN.equals(role) || ROLE_SUPERADMIN.equals(role)) {
            return;
        }
        Long userId = TenantContext.getCurrentUserId();
        if (userId != null && userId.equals(clienteIdResposta)) {
            return;
        }
        throw new SecurityException("Acesso negado.");
    }

    /**
     * Detecta content-type lendo magic number — evita servir SVG como PNG e vice-versa.
     */
    private MediaType detectarContentType(byte[] bytes) {
        if (bytes != null && bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return MediaType.IMAGE_PNG;
        }
        return MediaType.valueOf("image/svg+xml");
    }
}
