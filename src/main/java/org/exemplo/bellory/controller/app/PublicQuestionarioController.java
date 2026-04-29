package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.exemplo.bellory.config.RateLimiter;
import org.exemplo.bellory.model.dto.questionario.QuestionarioDTO;
import org.exemplo.bellory.model.dto.questionario.RespostaQuestionarioCreateDTO;
import org.exemplo.bellory.model.dto.questionario.RespostaQuestionarioDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.questionario.QuestionarioService;
import org.exemplo.bellory.service.questionario.RespostaQuestionarioService;
import org.exemplo.bellory.service.site.PublicSiteGuard;
import org.exemplo.bellory.service.site.PublicSiteGuard.PublicSiteAccess;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller público para resposta de questionários sem necessidade de JWT.
 * A organização é resolvida pelo slug na URL.
 *
 * ENDPOINTS:
 * - GET  /{slug}/questionarios/{id}                              → Buscar questionário público (precisa estar ativo)
 * - POST /{slug}/questionarios/{id}/respostas                    → Registrar resposta
 * - GET  /{slug}/questionarios/{id}/respostas/verificar?clienteId=&agendamentoId= → Verifica se já respondeu (passe agendamentoId em casos de anamnese)
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Questionários Públicos", description = "Endpoints públicos de questionário (sem JWT)")
public class PublicQuestionarioController {

    private static final int RATE_LIMIT_GET = 60;
    private static final int RATE_LIMIT_POST = 10;
    private static final int RATE_WINDOW_MINUTES = 60;

    private final QuestionarioService questionarioService;
    private final RespostaQuestionarioService respostaService;
    private final PublicSiteGuard siteGuard;
    private final RateLimiter rateLimiter;

    public PublicQuestionarioController(QuestionarioService questionarioService,
                                        RespostaQuestionarioService respostaService,
                                        PublicSiteGuard siteGuard,
                                        RateLimiter rateLimiter) {
        this.questionarioService = questionarioService;
        this.respostaService = respostaService;
        this.siteGuard = siteGuard;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "Buscar questionário público por ID",
            description = "Quando informados, os parâmetros clienteId/agendamentoId/funcionarioId fazem o "
                    + "servidor resolver os placeholders {{var}} dos termos de consentimento e devolver "
                    + "PerguntaDTO.textoTermoRenderizado pronto. Sem os parâmetros, retorna texto cru com "
                    + "placeholders.")
    @GetMapping("/{slug}/questionarios/{id}")
    public ResponseEntity<ResponseAPI<QuestionarioDTO>> buscar(
            @PathVariable String slug,
            @PathVariable Long id,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long agendamentoId,
            @RequestParam(required = false) Long funcionarioId,
            HttpServletRequest request) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) return badRequest("Slug é obrigatório");

            if (!rateLimiter.tryAcquire("ip:" + getClientIp(request), RATE_LIMIT_GET, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            PublicSiteAccess access = siteGuard.check(normalizedSlug);
            if (!access.isActive()) {
                return notFound("Organização não encontrada ou site indisponível.");
            }

            QuestionarioDTO dto = questionarioService.buscarPublicoPorSlug(
                    id, access.getOrganizacaoId(), clienteId, agendamentoId, funcionarioId);
            return ResponseEntity.ok(ResponseAPI.<QuestionarioDTO>builder()
                    .success(true)
                    .dados(dto)
                    .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<QuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao buscar questionário: " + e.getMessage());
        }
    }

    @Operation(summary = "Registrar resposta pública para questionário")
    @PostMapping("/{slug}/questionarios/{id}/respostas")
    public ResponseEntity<ResponseAPI<RespostaQuestionarioDTO>> registrar(
            @PathVariable String slug,
            @PathVariable Long id,
            @Valid @RequestBody RespostaQuestionarioCreateDTO dto,
            HttpServletRequest request) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) return badRequest("Slug é obrigatório");

            String ip = getClientIp(request);
            if (!rateLimiter.tryAcquire("ip:" + ip, RATE_LIMIT_POST, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            PublicSiteAccess access = siteGuard.check(normalizedSlug);
            if (!access.isActive()) {
                return notFound("Organização não encontrada ou site indisponível.");
            }

            dto.setQuestionarioId(id);
            if (dto.getUserAgent() == null) dto.setUserAgent(request.getHeader("User-Agent"));

            RespostaQuestionarioDTO response = respostaService.registrarPublico(
                    access.getOrganizacaoId(), dto, ip);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<RespostaQuestionarioDTO>builder()
                            .success(true)
                            .message("Resposta registrada com sucesso!")
                            .dados(response)
                            .build());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao registrar resposta: " + e.getMessage());
        }
    }

    @Operation(summary = "Verificar se cliente já respondeu o questionário (opcionalmente por agendamento)")
    @GetMapping("/{slug}/questionarios/{id}/respostas/verificar")
    public ResponseEntity<ResponseAPI<Map<String, Boolean>>> verificar(
            @PathVariable String slug,
            @PathVariable Long id,
            @RequestParam Long clienteId,
            @RequestParam(required = false) Long agendamentoId,
            HttpServletRequest request) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) return badRequest("Slug é obrigatório");

            if (!rateLimiter.tryAcquire("ip:" + getClientIp(request), RATE_LIMIT_GET, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            PublicSiteAccess access = siteGuard.check(normalizedSlug);
            if (!access.isActive()) {
                return notFound("Organização não encontrada ou site indisponível.");
            }

            // Garante que o questionário pertence à org do slug antes de consultar
            questionarioService.buscarPublicoPorSlug(id, access.getOrganizacaoId());

            // Quando agendamentoId é informado (caso anamnese), checamos por agendamento:
            // o cliente pode ter respondido o questionário em outro agendamento e ainda
            // assim PRECISA responder neste. Sem agendamentoId, mantém a lógica antiga
            // de "cliente já respondeu pelo menos uma vez" (cadastro/pesquisa de satisfação).
            boolean jaRespondeu = (agendamentoId != null)
                    ? respostaService.agendamentoJaAvaliado(id, agendamentoId)
                    : respostaService.clienteJaRespondeu(id, clienteId);

            return ResponseEntity.ok(ResponseAPI.<Map<String, Boolean>>builder()
                    .success(true)
                    .dados(Map.of("respondido", jaRespondeu))
                    .build());
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao verificar resposta: " + e.getMessage());
        }
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) return null;
        return slug.toLowerCase().trim();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) return xRealIp;
        return request.getRemoteAddr();
    }

    private <T> ResponseEntity<ResponseAPI<T>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseAPI.<T>builder().success(false).message(message).errorCode(400).build());
    }

    private <T> ResponseEntity<ResponseAPI<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseAPI.<T>builder().success(false).message(message).errorCode(404).build());
    }

    private <T> ResponseEntity<ResponseAPI<T>> tooManyRequests() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ResponseAPI.<T>builder()
                        .success(false)
                        .message("Muitas requisições. Tente novamente em alguns minutos.")
                        .errorCode(429).build());
    }

    private <T> ResponseEntity<ResponseAPI<T>> serverError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseAPI.<T>builder().success(false).message(message).errorCode(500).build());
    }
}
