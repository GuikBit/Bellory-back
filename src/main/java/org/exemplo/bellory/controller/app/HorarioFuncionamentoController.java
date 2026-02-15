package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.organizacao.HorarioFuncionamentoCreateDTO;
import org.exemplo.bellory.model.dto.organizacao.HorarioFuncionamentoResponseDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.HorarioFuncionamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizacao/horario-funcionamento")
@Tag(name = "Horário de Funcionamento", description = "Gerenciamento dos horários de funcionamento da organização")
public class HorarioFuncionamentoController {

    private final HorarioFuncionamentoService horarioFuncionamentoService;

    public HorarioFuncionamentoController(HorarioFuncionamentoService horarioFuncionamentoService) {
        this.horarioFuncionamentoService = horarioFuncionamentoService;
    }

    @GetMapping
    @Operation(summary = "Listar horários de funcionamento de todos os dias da semana")
    public ResponseEntity<ResponseAPI<List<HorarioFuncionamentoResponseDTO>>> listar() {
        try {
            List<HorarioFuncionamentoResponseDTO> horarios = horarioFuncionamentoService.listar();

            return ResponseEntity.ok(ResponseAPI.<List<HorarioFuncionamentoResponseDTO>>builder()
                    .success(true)
                    .message("Horários de funcionamento recuperados com sucesso.")
                    .dados(horarios)
                    .build());

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<List<HorarioFuncionamentoResponseDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<HorarioFuncionamentoResponseDTO>>builder()
                            .success(false)
                            .message("Erro interno ao buscar horários: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping
    @Operation(summary = "Atualizar horários de funcionamento de todos os dias (bulk update)")
    public ResponseEntity<ResponseAPI<List<HorarioFuncionamentoResponseDTO>>> atualizarTodos(
            @RequestBody List<HorarioFuncionamentoCreateDTO> dtos) {
        try {
            List<HorarioFuncionamentoResponseDTO> horarios = horarioFuncionamentoService.atualizarTodos(dtos);

            return ResponseEntity.ok(ResponseAPI.<List<HorarioFuncionamentoResponseDTO>>builder()
                    .success(true)
                    .message("Horários de funcionamento atualizados com sucesso.")
                    .dados(horarios)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<List<HorarioFuncionamentoResponseDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<List<HorarioFuncionamentoResponseDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<HorarioFuncionamentoResponseDTO>>builder()
                            .success(false)
                            .message("Erro interno ao atualizar horários: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/{diaSemana}")
    @Operation(summary = "Atualizar horário de funcionamento de um dia específico")
    public ResponseEntity<ResponseAPI<HorarioFuncionamentoResponseDTO>> atualizarDia(
            @PathVariable String diaSemana,
            @RequestBody HorarioFuncionamentoCreateDTO dto) {
        try {
            HorarioFuncionamentoResponseDTO horario = horarioFuncionamentoService.atualizarDia(diaSemana, dto);

            return ResponseEntity.ok(ResponseAPI.<HorarioFuncionamentoResponseDTO>builder()
                    .success(true)
                    .message("Horário de funcionamento atualizado com sucesso.")
                    .dados(horario)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<HorarioFuncionamentoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<HorarioFuncionamentoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<HorarioFuncionamentoResponseDTO>builder()
                            .success(false)
                            .message("Erro interno ao atualizar horário: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PatchMapping("/{diaSemana}/status")
    @Operation(summary = "Ativar ou desativar um dia da semana")
    public ResponseEntity<ResponseAPI<HorarioFuncionamentoResponseDTO>> toggleDia(
            @PathVariable String diaSemana,
            @RequestParam Boolean ativo) {
        try {
            HorarioFuncionamentoResponseDTO horario = horarioFuncionamentoService.toggleDia(diaSemana, ativo);

            return ResponseEntity.ok(ResponseAPI.<HorarioFuncionamentoResponseDTO>builder()
                    .success(true)
                    .message("Status do dia atualizado com sucesso.")
                    .dados(horario)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<HorarioFuncionamentoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<HorarioFuncionamentoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<HorarioFuncionamentoResponseDTO>builder()
                            .success(false)
                            .message("Erro interno ao atualizar status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
