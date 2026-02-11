package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.exemplo.bellory.model.dto.questionario.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.questionario.RespostaQuestionarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/questionarios/{questionarioId}/respostas")
@Tag(name = "Respostas de Questionários", description = "Gerenciamento de respostas")
public class RespostaQuestionarioController {

    private final RespostaQuestionarioService respostaService;

    public RespostaQuestionarioController(RespostaQuestionarioService respostaService) {
        this.respostaService = respostaService;
    }

    @PostMapping
    @Operation(summary = "Registrar nova resposta para o questionário")
    public ResponseEntity<ResponseAPI<RespostaQuestionarioDTO>> registrar(
            @PathVariable Long questionarioId,
            @Valid @RequestBody RespostaQuestionarioCreateDTO dto,
            HttpServletRequest request) {
        try {
            dto.setQuestionarioId(questionarioId);
            String ipOrigem = obterIpOrigem(request);

            RespostaQuestionarioDTO response = respostaService.registrar(dto, ipOrigem);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<RespostaQuestionarioDTO>builder()
                            .success(true)
                            .message("Resposta registrada com sucesso!")
                            .dados(response)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RespostaQuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        }
    }

    @GetMapping("/{respostaId}")
    @Operation(summary = "Buscar resposta por ID")
    public ResponseEntity<ResponseAPI<RespostaQuestionarioDTO>> buscarPorId(
            @PathVariable Long questionarioId,
            @PathVariable Long respostaId) {
        try {
            RespostaQuestionarioDTO response = respostaService.buscarPorId(respostaId);

            return ResponseEntity.ok(ResponseAPI.<RespostaQuestionarioDTO>builder()
                    .success(true)
                    .dados(response)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<RespostaQuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @DeleteMapping("/{respostaId}")
    @Operation(summary = "Deletar resposta")
    public ResponseEntity<ResponseAPI<Void>> deletar(
            @PathVariable Long questionarioId,
            @PathVariable Long respostaId) {
        try {
            respostaService.deletar(respostaId);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Resposta deletada com sucesso!")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @GetMapping
    @Operation(summary = "Listar respostas do questionário")
    public ResponseEntity<ResponseAPI<Page<RespostaQuestionarioDTO>>> listar(
            @PathVariable Long questionarioId,
            @PageableDefault(size = 20, sort = "dtResposta") Pageable pageable) {
        try {
            Page<RespostaQuestionarioDTO> page = respostaService.listarPorQuestionario(questionarioId, pageable);

            return ResponseEntity.ok(ResponseAPI.<Page<RespostaQuestionarioDTO>>builder()
                    .success(true)
                    .dados(page)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Page<RespostaQuestionarioDTO>>builder()
                            .success(false)
                            .message("Erro ao listar respostas.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/periodo")
    @Operation(summary = "Listar respostas por período")
    public ResponseEntity<ResponseAPI<Page<RespostaQuestionarioDTO>>> listarPorPeriodo(
            @PathVariable Long questionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @PageableDefault(size = 20, sort = "dtResposta") Pageable pageable) {
        try {
            Page<RespostaQuestionarioDTO> page = respostaService.listarPorQuestionarioEPeriodo(
                    questionarioId, inicio, fim, pageable);

            return ResponseEntity.ok(ResponseAPI.<Page<RespostaQuestionarioDTO>>builder()
                    .success(true)
                    .dados(page)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Page<RespostaQuestionarioDTO>>builder()
                            .success(false)
                            .message("Erro ao listar respostas por período.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/verificar")
    @Operation(summary = "Verificar se cliente já respondeu")
    public ResponseEntity<ResponseAPI<Map<String, Boolean>>> verificarClienteJaRespondeu(
            @PathVariable Long questionarioId,
            @RequestParam Long clienteId) {
        try {
            boolean jaRespondeu = respostaService.clienteJaRespondeu(questionarioId, clienteId);

            return ResponseEntity.ok(ResponseAPI.<Map<String, Boolean>>builder()
                    .success(true)
                    .dados(Map.of("respondido", jaRespondeu))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Boolean>>builder()
                            .success(false)
                            .message("Erro ao verificar resposta.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/verificar-agendamento")
    @Operation(summary = "Verificar se agendamento já foi avaliado")
    public ResponseEntity<ResponseAPI<Map<String, Boolean>>> verificarAgendamentoAvaliado(
            @PathVariable Long questionarioId,
            @RequestParam Long agendamentoId) {
        try {
            boolean avaliado = respostaService.agendamentoJaAvaliado(questionarioId, agendamentoId);

            return ResponseEntity.ok(ResponseAPI.<Map<String, Boolean>>builder()
                    .success(true)
                    .dados(Map.of("avaliado", avaliado))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Boolean>>builder()
                            .success(false)
                            .message("Erro ao verificar agendamento.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Obter estatísticas das respostas")
    public ResponseEntity<ResponseAPI<EstatisticasQuestionarioDTO>> obterEstatisticas(
            @PathVariable Long questionarioId) {
        try {
            EstatisticasQuestionarioDTO stats = respostaService.obterEstatisticas(questionarioId);

            return ResponseEntity.ok(ResponseAPI.<EstatisticasQuestionarioDTO>builder()
                    .success(true)
                    .dados(stats)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<EstatisticasQuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @GetMapping("/nps")
    @Operation(summary = "Calcular NPS (Net Promoter Score)")
    public ResponseEntity<ResponseAPI<Map<String, Double>>> calcularNPS(
            @PathVariable Long questionarioId,
            @RequestParam Long perguntaId) {
        try {
            Double nps = respostaService.calcularNPS(questionarioId, perguntaId);

            return ResponseEntity.ok(ResponseAPI.<Map<String, Double>>builder()
                    .success(true)
                    .dados(Map.of("nps", nps != null ? nps : 0.0))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Double>>builder()
                            .success(false)
                            .message("Erro ao calcular NPS.")
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/relatorio")
    @Operation(summary = "Gerar relatório de respostas")
    public ResponseEntity<ResponseAPI<RelatorioRespostasDTO>> gerarRelatorio(
            @PathVariable Long questionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        try {
            RelatorioRespostasDTO relatorio = respostaService.gerarRelatorio(questionarioId, inicio, fim);

            return ResponseEntity.ok(ResponseAPI.<RelatorioRespostasDTO>builder()
                    .success(true)
                    .dados(relatorio)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<RelatorioRespostasDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    private String obterIpOrigem(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
