package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.auth.ForgotPasswordRequestDTO;
import org.exemplo.bellory.model.dto.auth.MaskedEmailDTO;
import org.exemplo.bellory.model.dto.auth.ResetPasswordRequestDTO;
import org.exemplo.bellory.model.dto.auth.ResetTokenDTO;
import org.exemplo.bellory.model.dto.auth.VerifyCodeRequestDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.ForgotPasswordRateLimiterService;
import org.exemplo.bellory.service.ForgotPasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/forgot-password")
@CrossOrigin(origins = {"https://bellory.vercel.app", "https://*.vercel.app", "http://localhost:*"})
@Tag(name = "Recuperação de Senha", description = "Endpoints públicos para recuperação de senha")
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;
    private final ForgotPasswordRateLimiterService rateLimiterService;

    @Operation(summary = "Solicitar código de recuperação de senha")
    @PostMapping("/request")
    public ResponseEntity<ResponseAPI<MaskedEmailDTO>> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIp(httpRequest);
            if (!rateLimiterService.allowRequest(clientIp)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ResponseAPI.<MaskedEmailDTO>builder()
                                .success(false)
                                .message("Muitas tentativas. Aguarde alguns minutos e tente novamente.")
                                .errorCode(429)
                                .build());
            }

            MaskedEmailDTO result = forgotPasswordService.requestPasswordReset(request.getIdentifier());

            return ResponseEntity.ok(ResponseAPI.<MaskedEmailDTO>builder()
                    .success(true)
                    .message("Código de recuperação enviado para o e-mail cadastrado")
                    .dados(result)
                    .build());

        } catch (RuntimeException e) {
            log.warn("Erro ao solicitar recuperação de senha: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ResponseAPI.<MaskedEmailDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Verificar código de recuperação")
    @PostMapping("/verify")
    public ResponseEntity<ResponseAPI<ResetTokenDTO>> verifyCode(
            @Valid @RequestBody VerifyCodeRequestDTO request) {
        try {
            ResetTokenDTO result = forgotPasswordService.verifyCode(
                    request.getIdentifier(), request.getCode());

            return ResponseEntity.ok(ResponseAPI.<ResetTokenDTO>builder()
                    .success(true)
                    .message("Código verificado com sucesso")
                    .dados(result)
                    .build());

        } catch (RuntimeException e) {
            log.warn("Erro ao verificar código: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ResponseAPI.<ResetTokenDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Redefinir senha com token")
    @PostMapping("/reset")
    public ResponseEntity<ResponseAPI<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {
        try {
            forgotPasswordService.resetPassword(request.getResetToken(), request.getNewPassword());

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Senha redefinida com sucesso")
                    .build());

        } catch (RuntimeException e) {
            log.warn("Erro ao redefinir senha: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
