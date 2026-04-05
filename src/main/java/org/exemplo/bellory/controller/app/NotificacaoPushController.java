package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.push.NotificacaoPushDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.push.NotificacaoPushService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notificacao-push")
@Tag(name = "Notificações Push", description = "Gerenciamento de notificações push in-app")
public class NotificacaoPushController {

    private final NotificacaoPushService notificacaoPushService;

    public NotificacaoPushController(NotificacaoPushService notificacaoPushService) {
        this.notificacaoPushService = notificacaoPushService;
    }

    @Operation(summary = "Listar notificações do usuário autenticado")
    @GetMapping
    public ResponseEntity<ResponseAPI<Page<NotificacaoPushDTO>>> listarNotificacoes(
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            Page<NotificacaoPushDTO> page = notificacaoPushService.listarNotificacoes(pageable);
            return ResponseEntity.ok(ResponseAPI.<Page<NotificacaoPushDTO>>builder()
                    .success(true)
                    .message("Notificações listadas com sucesso")
                    .dados(page)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Page<NotificacaoPushDTO>>builder()
                            .success(false)
                            .message("Erro ao listar notificações: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Contar notificações não lidas")
    @GetMapping("/nao-lidas/count")
    public ResponseEntity<ResponseAPI<Map<String, Long>>> contarNaoLidas() {
        try {
            long count = notificacaoPushService.contarNaoLidas();
            return ResponseEntity.ok(ResponseAPI.<Map<String, Long>>builder()
                    .success(true)
                    .message("Contagem de notificações não lidas")
                    .dados(Map.of("count", count))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Long>>builder()
                            .success(false)
                            .message("Erro ao contar notificações: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Marcar notificação como lida")
    @PutMapping("/{id}/lida")
    public ResponseEntity<ResponseAPI<NotificacaoPushDTO>> marcarComoLida(@PathVariable Long id) {
        try {
            NotificacaoPushDTO dto = notificacaoPushService.marcarComoLida(id);
            return ResponseEntity.ok(ResponseAPI.<NotificacaoPushDTO>builder()
                    .success(true)
                    .message("Notificação marcada como lida")
                    .dados(dto)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<NotificacaoPushDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<NotificacaoPushDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }

    @Operation(summary = "Deletar notificação")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> deletar(@PathVariable Long id) {
        try {
            notificacaoPushService.deletar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Notificação deletada com sucesso")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }
}
