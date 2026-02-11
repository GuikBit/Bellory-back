package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.ConfirmacaoPendenteResponse;
import org.exemplo.bellory.model.dto.AtualizarStatusConfirmacaoRequest;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.ConfirmacaoAgendamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@Tag(name = "Webhooks de Confirmação", description = "Endpoints para integração com N8N - confirmações e lembretes via WhatsApp")
public class ConfirmacaoWebhookController {

    private final ConfirmacaoAgendamentoService confirmacaoService;

    public ConfirmacaoWebhookController(ConfirmacaoAgendamentoService confirmacaoService) {
        this.confirmacaoService = confirmacaoService;
    }

    /**
     * Verifica se existe uma confirmação pendente para o telefone
     * Usado pelo N8N para decidir se deve processar a mensagem ou ignorar
     */
    @GetMapping("/confirmacao-pendente/{telefone}")
    @Operation(summary = "Verificar confirmação pendente por telefone")
    public ResponseEntity<ConfirmacaoPendenteResponse> verificarConfirmacaoPendente(
            @PathVariable String telefone,
            @RequestHeader(value = "X-Instance-Name", required = false) String instanceName) {
        try {
            ConfirmacaoPendenteResponse response = confirmacaoService
                    .verificarConfirmacaoPendente(telefone, instanceName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ConfirmacaoPendenteResponse.builder()
                            .temConfirmacaoPendente(false)
                            .build());
        }
    }

    /**
     * Verifica se o cliente está aguardando informar uma data para reagendamento
     */
    @GetMapping("/confirmacao-aguardando-data/{telefone}")
    @Operation(summary = "Verificar se aguarda data para reagendamento")
    public ResponseEntity<ConfirmacaoPendenteResponse> verificarAguardandoData(
            @PathVariable String telefone,
            @RequestHeader(value = "X-Instance-Name", required = false) String instanceName) {
        try {
            ConfirmacaoPendenteResponse response = confirmacaoService
                    .verificarAguardandoData(telefone, instanceName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ConfirmacaoPendenteResponse.builder()
                            .aguardandoData(false)
                            .build());
        }
    }

    /**
     * Verifica se o cliente está aguardando selecionar um horário
     */
    @GetMapping("/confirmacao-aguardando-horario/{telefone}")
    @Operation(summary = "Verificar se aguarda seleção de horário")
    public ResponseEntity<ConfirmacaoPendenteResponse> verificarAguardandoHorario(
            @PathVariable String telefone,
            @RequestHeader(value = "X-Instance-Name", required = false) String instanceName) {
        try {
            ConfirmacaoPendenteResponse response = confirmacaoService
                    .verificarAguardandoHorario(telefone, instanceName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ConfirmacaoPendenteResponse.builder()
                            .aguardandoHorario(false)
                            .build());
        }
    }

    /**
     * Marca a notificação como aguardando data (após cliente responder REAGENDAR)
     */
    @PatchMapping("/confirmacao/{notificacaoId}/aguardando-data")
    @Operation(summary = "Marcar notificação como aguardando data")
    public ResponseEntity<ResponseAPI<Void>> marcarAguardandoData(
            @PathVariable Long notificacaoId) {
        try {
            confirmacaoService.marcarAguardandoData(notificacaoId);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Status atualizado para aguardando data")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao atualizar status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Marca a notificação como aguardando horário e salva os horários disponíveis
     */
    @PatchMapping("/confirmacao/{notificacaoId}/aguardando-horario")
    @Operation(summary = "Marcar notificação como aguardando horário")
    public ResponseEntity<ResponseAPI<Void>> marcarAguardandoHorario(
            @PathVariable Long notificacaoId,
            @RequestBody AtualizarStatusConfirmacaoRequest request) {
        try {
            confirmacaoService.marcarAguardandoHorario(
                    notificacaoId,
                    request.getDataDesejada(),
                    request.getHorariosDisponiveis()
            );
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Status atualizado para aguardando horário")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao atualizar status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Marca a notificação como concluída (confirmação finalizada)
     */
    @PatchMapping("/confirmacao/{notificacaoId}/concluida")
    @Operation(summary = "Marcar confirmação como concluída")
    public ResponseEntity<ResponseAPI<Void>> marcarConcluida(
            @PathVariable Long notificacaoId) {
        try {
            confirmacaoService.marcarConcluida(notificacaoId);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Confirmação finalizada com sucesso")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao finalizar confirmação: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Registra a resposta do cliente (chamado pelo N8N após processar resposta)
     */
    @PostMapping("/confirmacao/{notificacaoId}/resposta")
    @Operation(summary = "Registrar resposta do cliente")
    public ResponseEntity<ResponseAPI<Void>> registrarResposta(
            @PathVariable Long notificacaoId,
            @RequestBody Map<String, String> request) {
        try {
            String resposta = request.get("resposta");
            confirmacaoService.registrarResposta(notificacaoId, resposta);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Resposta registrada com sucesso")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao registrar resposta: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
