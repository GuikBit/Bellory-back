package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.exemplo.bellory.config.RateLimiter;
import org.exemplo.bellory.model.dto.booking.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.PublicBookingService;
import org.exemplo.bellory.service.PublicBookingService.ClienteJaCadastradoException;
import org.exemplo.bellory.service.PublicBookingService.HorarioIndisponivelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controller público para o fluxo de agendamento na landing page.
 *
 * ENDPOINTS:
 * - GET  /{slug}/booking/cliente?telefone={digits}         → Buscar cliente por telefone
 * - POST /{slug}/booking/cliente                           → Cadastrar novo cliente (auto-cadastro)
 * - GET  /{slug}/booking/horarios?funcionarioId=&data=&servicoIds= → Horários disponíveis
 * - GET  /{slug}/booking/dias-disponiveis?funcionarioId=&mes=      → Dias disponíveis no mês
 * - POST /{slug}/booking                                   → Criar agendamento
 */
@RestController
@RequestMapping("/api/v1/public/site")
@Tag(name = "Booking Público", description = "Endpoints públicos para agendamento via landing page")
public class PublicBookingController {

    private static final int RATE_LIMIT_GENERAL = 60;       // req/hora por IP
    private static final int RATE_LIMIT_CREATE = 5;          // criações/hora por telefone
    private static final int RATE_WINDOW_MINUTES = 60;

    private final PublicBookingService bookingService;
    private final RateLimiter rateLimiter;

    public PublicBookingController(PublicBookingService bookingService, RateLimiter rateLimiter) {
        this.bookingService = bookingService;
        this.rateLimiter = rateLimiter;
    }

    // ==================== 1. BUSCAR CLIENTE ====================

    @Operation(summary = "Buscar cliente por telefone")
    @GetMapping("/{slug}/booking/cliente")
    public ResponseEntity<ResponseAPI<ClientePublicDTO>> buscarCliente(
            @PathVariable String slug,
            @RequestParam String telefone,
            HttpServletRequest request) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            if (!rateLimiter.tryAcquire("ip:" + getClientIp(request), RATE_LIMIT_GENERAL, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            var resultado = bookingService.buscarClientePorTelefone(normalizedSlug, telefone);

            if (resultado == null) {
                return notFound("Organização não encontrada");
            }

            // Retorna 200 com dados null quando cliente não encontrado (front trata como "novo")
            return ResponseEntity.ok(ResponseAPI.<ClientePublicDTO>builder()
                    .success(true)
                    .dados(resultado.orElse(null))
                    .build());

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao buscar cliente: " + e.getMessage());
        }
    }

    // ==================== 2. CRIAR CLIENTE ====================

    @Operation(summary = "Cadastrar novo cliente (auto-cadastro)")
    @PostMapping("/{slug}/booking/cliente")
    public ResponseEntity<ResponseAPI<ClientePublicDTO>> criarCliente(
            @PathVariable String slug,
            @RequestBody ClienteCreatePublicDTO dto,
            HttpServletRequest request) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            String ip = getClientIp(request);
            if (!rateLimiter.tryAcquire("ip:" + ip, RATE_LIMIT_GENERAL, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            // Rate limit por telefone
            String digits = dto.getTelefone() != null ? dto.getTelefone().replaceAll("[^0-9]", "") : "";
            if (!digits.isEmpty() && !rateLimiter.tryAcquire("tel:" + digits, RATE_LIMIT_CREATE, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            ClientePublicDTO resultado = bookingService.criarCliente(normalizedSlug, dto);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ClientePublicDTO>builder()
                            .success(true)
                            .message("Cliente cadastrado com sucesso")
                            .dados(resultado)
                            .build());

        } catch (ClienteJaCadastradoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<ClientePublicDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao cadastrar cliente: " + e.getMessage());
        }
    }

    // ==================== 3. HORÁRIOS DISPONÍVEIS ====================

    @Operation(summary = "Listar horários disponíveis de um profissional em um dia")
    @GetMapping("/{slug}/booking/horarios")
    public ResponseEntity<ResponseAPI<List<String>>> buscarHorarios(
            @PathVariable String slug,
            @RequestParam Long funcionarioId,
            @RequestParam String data,
            @RequestParam List<Long> servicoIds,
            HttpServletRequest request) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            if (!rateLimiter.tryAcquire("ip:" + getClientIp(request), RATE_LIMIT_GENERAL, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            LocalDate dataLocal;
            try {
                dataLocal = LocalDate.parse(data);
            } catch (DateTimeParseException e) {
                return badRequest("Data inválida. Formato esperado: YYYY-MM-DD");
            }

            if (servicoIds == null || servicoIds.isEmpty()) {
                return badRequest("Pelo menos um serviço deve ser informado.");
            }

            List<String> horarios = bookingService.buscarHorariosDisponiveis(
                    normalizedSlug, funcionarioId, dataLocal, servicoIds);

            return ResponseEntity.ok(ResponseAPI.<List<String>>builder()
                    .success(true)
                    .dados(horarios)
                    .build());

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao buscar horários: " + e.getMessage());
        }
    }

    // ==================== 4. DIAS DISPONÍVEIS ====================

    @Operation(summary = "Listar dias disponíveis de um profissional no mês")
    @GetMapping("/{slug}/booking/dias-disponiveis")
    public ResponseEntity<ResponseAPI<List<DiaDisponivelDTO>>> buscarDiasDisponiveis(
            @PathVariable String slug,
            @RequestParam Long funcionarioId,
            @RequestParam String mes,
            HttpServletRequest request) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            if (!rateLimiter.tryAcquire("ip:" + getClientIp(request), RATE_LIMIT_GENERAL, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            YearMonth mesLocal;
            try {
                mesLocal = YearMonth.parse(mes);
            } catch (DateTimeParseException e) {
                return badRequest("Mês inválido. Formato esperado: YYYY-MM");
            }

            List<DiaDisponivelDTO> dias = bookingService.buscarDiasDisponiveis(
                    normalizedSlug, funcionarioId, mesLocal);

            return ResponseEntity.ok(ResponseAPI.<List<DiaDisponivelDTO>>builder()
                    .success(true)
                    .dados(dias)
                    .build());

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao buscar dias disponíveis: " + e.getMessage());
        }
    }

    // ==================== 5. CRIAR AGENDAMENTO ====================

    @Operation(summary = "Criar agendamento")
    @PostMapping("/{slug}/booking")
    public ResponseEntity<ResponseAPI<BookingResponseDTO>> criarAgendamento(
            @PathVariable String slug,
            @RequestBody BookingCreateDTO dto,
            HttpServletRequest request) {
        try {
            String normalizedSlug = normalizeSlug(slug);
            if (normalizedSlug == null) {
                return badRequest("Slug é obrigatório");
            }

            String ip = getClientIp(request);
            if (!rateLimiter.tryAcquire("ip:" + ip, RATE_LIMIT_GENERAL, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            // Rate limit por telefone (via clienteId - usa IP como fallback)
            if (!rateLimiter.tryAcquire("booking-ip:" + ip, RATE_LIMIT_CREATE, RATE_WINDOW_MINUTES)) {
                return tooManyRequests();
            }

            BookingResponseDTO resultado = bookingService.criarAgendamento(normalizedSlug, dto);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<BookingResponseDTO>builder()
                            .success(true)
                            .message("Agendamento criado com sucesso")
                            .dados(resultado)
                            .build());

        } catch (HorarioIndisponivelException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<BookingResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return serverError("Erro ao criar agendamento: " + e.getMessage());
        }
    }

    // ==================== HELPERS ====================

    private String normalizeSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return null;
        }
        return slug.toLowerCase().trim();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
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

    private <T> ResponseEntity<ResponseAPI<T>> tooManyRequests() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ResponseAPI.<T>builder()
                        .success(false)
                        .message("Muitas requisições. Tente novamente em alguns minutos.")
                        .errorCode(429)
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
