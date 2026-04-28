package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.AgendamentoDTO;
import org.exemplo.bellory.model.dto.filaespera.FilaEsperaTentativaDTO;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.fila.FilaEsperaTentativa;
import org.exemplo.bellory.service.filaespera.FilaEsperaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints da fila de espera consumidos pelo N8N e pelo painel admin
 * (a parte admin entra em PR posterior).
 *
 * <p>Convencao de paths igual a {@code ConfirmacaoWebhookController}:
 * {@code /api/v1/webhook/...} para integracao com N8N.
 */
@RestController
@RequestMapping("/api/v1/webhook/fila-espera")
@Tag(name = "Webhooks da Fila de Espera",
        description = "Endpoints para integracao com N8N - aceitar/recusar/consultar oferta de adiantamento")
public class FilaEsperaWebhookController {

    private final FilaEsperaService filaEsperaService;

    public FilaEsperaWebhookController(FilaEsperaService filaEsperaService) {
        this.filaEsperaService = filaEsperaService;
    }

    @Operation(summary = "Cliente aceita o adiantamento (chamado pelo N8N quando recebe SIM)")
    @PostMapping("/tentativa/{id}/aceitar")
    public ResponseEntity<ResponseAPI<AgendamentoDTO>> aceitar(@PathVariable Long id) {
        try {
            Agendamento agendamento = filaEsperaService.aceitarOferta(id);
            return ResponseEntity.ok(ResponseAPI.<AgendamentoDTO>builder()
                    .success(true)
                    .message("Agendamento adiantado com sucesso.")
                    .dados(new AgendamentoDTO(agendamento))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<AgendamentoDTO>builder()
                            .success(false)
                            .message("Erro ao aceitar adiantamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Cliente recusa o adiantamento (chamado pelo N8N quando recebe NAO)")
    @PostMapping("/tentativa/{id}/recusar")
    public ResponseEntity<ResponseAPI<Void>> recusar(@PathVariable Long id) {
        try {
            filaEsperaService.recusarOferta(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Recusa registrada. Avancando para o proximo da fila.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao registrar recusa: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Consulta status da tentativa (N8N usa para detectar 'perdeu a vez')")
    @GetMapping("/tentativa/{id}/status")
    public ResponseEntity<ResponseAPI<FilaEsperaTentativaDTO>> status(@PathVariable Long id) {
        try {
            FilaEsperaTentativa tentativa = filaEsperaService.buscarTentativa(id);
            return ResponseEntity.ok(ResponseAPI.<FilaEsperaTentativaDTO>builder()
                    .success(true)
                    .message("Status da tentativa recuperado.")
                    .dados(FilaEsperaTentativaDTO.from(tentativa))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<FilaEsperaTentativaDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<FilaEsperaTentativaDTO>builder()
                            .success(false)
                            .message("Erro ao consultar status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @Operation(summary = "Lista tentativas (historico) de um agendamento")
    @GetMapping("/agendamento/{agendamentoId}")
    public ResponseEntity<ResponseAPI<List<FilaEsperaTentativaDTO>>> listarPorAgendamento(
            @PathVariable Long agendamentoId) {
        try {
            List<FilaEsperaTentativaDTO> dtos = filaEsperaService
                    .listarTentativasDoAgendamento(agendamentoId)
                    .stream()
                    .map(FilaEsperaTentativaDTO::from)
                    .toList();
            return ResponseEntity.ok(ResponseAPI.<List<FilaEsperaTentativaDTO>>builder()
                    .success(true)
                    .message("Tentativas recuperadas.")
                    .dados(dtos)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<FilaEsperaTentativaDTO>>builder()
                            .success(false)
                            .message("Erro ao listar tentativas: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
