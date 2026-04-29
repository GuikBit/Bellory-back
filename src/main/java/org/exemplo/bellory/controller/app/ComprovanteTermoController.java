package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.entity.questionario.RespostaQuestionario;
import org.exemplo.bellory.service.questionario.ComprovanteTermoPdfService;
import org.exemplo.bellory.service.questionario.RespostaQuestionarioService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint que retorna o comprovante PDF de termo de consentimento + assinatura.
 * Permitido para ADMIN/SUPERADMIN do tenant ou cliente dono da resposta.
 */
@RestController
@RequestMapping("/api/v1/resposta-questionario")
@Tag(name = "Comprovante de Termo", description = "Geração do PDF de comprovante de termo aceito + assinatura digital")
public class ComprovanteTermoController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_SUPERADMIN = "ROLE_SUPERADMIN";

    private final RespostaQuestionarioService respostaService;
    private final ComprovanteTermoPdfService pdfService;

    public ComprovanteTermoController(RespostaQuestionarioService respostaService,
                                      ComprovanteTermoPdfService pdfService) {
        this.respostaService = respostaService;
        this.pdfService = pdfService;
    }

    @GetMapping(value = "/{id}/comprovante.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download do comprovante PDF da resposta",
            description = "Retorna PDF com termos aceitos, assinaturas embutidas e linha de auditoria. "
                    + "Permite ADMIN/SUPERADMIN do tenant ou cliente dono.")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id) {
        try {
            RespostaQuestionario resposta = respostaService.buscarParaComprovante(id);
            exigirAdminOuCliente(resposta.getClienteId());

            byte[] pdf = pdfService.gerarPdf(resposta);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename("comprovante-" + id + ".pdf")
                    .build());
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
}
