package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.JornadaDiaCreateUpdateDTO;
import org.exemplo.bellory.model.dto.JornadaDiaDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.JornadaTrabalhoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionario/{funcionarioId}/jornada")
public class JornadaTrabalhoController {

    private final JornadaTrabalhoService jornadaTrabalhoService;

    public JornadaTrabalhoController(JornadaTrabalhoService jornadaTrabalhoService) {
        this.jornadaTrabalhoService = jornadaTrabalhoService;
    }

    /**
     * GET - Buscar todas as jornadas de trabalho de um funcionário
     * Endpoint: GET /api/funcionario/{funcionarioId}/jornada
     */
    @GetMapping
    public ResponseEntity<ResponseAPI<List<JornadaDiaDTO>>> getJornadas(@PathVariable Long funcionarioId) {
        try {
            List<JornadaDiaDTO> jornadas = jornadaTrabalhoService.getJornadasByFuncionario(funcionarioId);

            if (jornadas.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.OK)
                        .body(ResponseAPI.<List<JornadaDiaDTO>>builder()
                                .success(true)
                                .message("Nenhuma jornada de trabalho cadastrada para este funcionário.")
                                .dados(jornadas)
                                .build());
            }

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<List<JornadaDiaDTO>>builder()
                            .success(true)
                            .message("Jornadas de trabalho recuperadas com sucesso.")
                            .dados(jornadas)
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<List<JornadaDiaDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<JornadaDiaDTO>>builder()
                            .success(false)
                            .message("Erro interno ao buscar jornadas: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * POST - Criar ou atualizar uma jornada de trabalho para um dia específico
     * Endpoint: POST /api/funcionario/{funcionarioId}/jornada
     *
     * Body exemplo:
     * {
     *   "diaSemana": "SEGUNDA",
     *   "ativo": true,
     *   "horarios": [
     *     {
     *       "horaInicio": "08:00",
     *       "horaFim": "12:00"
     *     },
     *     {
     *       "horaInicio": "14:00",
     *       "horaFim": "18:00"
     *     }
     *   ]
     * }
     */
    @PostMapping
    public ResponseEntity<ResponseAPI<JornadaDiaDTO>> criarOuAtualizarJornada(
            @PathVariable Long funcionarioId,
            @RequestBody JornadaDiaDTO jornadaDTO) {
        try {
            JornadaDiaDTO jornada = jornadaTrabalhoService.criarOuAtualizarJornada(funcionarioId, jornadaDTO);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<JornadaDiaDTO>builder()
                            .success(true)
                            .message("Jornada de trabalho salva com sucesso.")
                            .dados(jornada)
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<JornadaDiaDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<JornadaDiaDTO>builder()
                            .success(false)
                            .message("Erro interno ao salvar jornada: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * PATCH - Atualizar apenas o status ativo/inativo de um dia
     * Endpoint: PATCH /api/funcionario/{funcionarioId}/jornada/{diaSemana}/status?ativo=true
     */
    @PatchMapping("/{diaSemana}/status")
    public ResponseEntity<ResponseAPI<JornadaDiaDTO>> atualizarStatusDia(
            @PathVariable Long funcionarioId,
            @PathVariable String diaSemana,
            @RequestParam Boolean ativo) {
        try {
            JornadaDiaDTO jornada = jornadaTrabalhoService.atualizarStatusDia(funcionarioId, diaSemana, ativo);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<JornadaDiaDTO>builder()
                            .success(true)
                            .message("Status da jornada atualizado com sucesso.")
                            .dados(jornada)
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<JornadaDiaDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<JornadaDiaDTO>builder()
                            .success(false)
                            .message("Erro interno ao atualizar status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * DELETE - Deletar um horário específico dentro de um dia
     * Endpoint: DELETE /api/funcionario/{funcionarioId}/jornada/{diaSemana}/horario/{horarioId}
     */
    @DeleteMapping("/{diaSemana}/horario/{horarioId}")
    public ResponseEntity<ResponseAPI<Void>> deletarHorario(
            @PathVariable Long funcionarioId,
            @PathVariable String diaSemana,
            @PathVariable String horarioId) {
        try {
            jornadaTrabalhoService.deletarHorario(funcionarioId, diaSemana, horarioId);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<Void>builder()
                            .success(true)
                            .message("Horário deletado com sucesso.")
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro interno ao deletar horário: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * DELETE - Deletar uma jornada completa de um dia
     * Endpoint: DELETE /api/funcionario/{funcionarioId}/jornada/{diaSemana}
     */
    @DeleteMapping("/{diaSemana}")
    public ResponseEntity<ResponseAPI<Void>> deletarJornadaDia(
            @PathVariable Long funcionarioId,
            @PathVariable String diaSemana) {
        try {
            jornadaTrabalhoService.deletarJornadaDia(funcionarioId, diaSemana);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseAPI.<Void>builder()
                            .success(true)
                            .message("Jornada do dia deletada com sucesso.")
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro interno ao deletar jornada: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
