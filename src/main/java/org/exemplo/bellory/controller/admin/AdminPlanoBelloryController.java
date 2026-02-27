package org.exemplo.bellory.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.plano.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.admin.AdminPlanoBelloryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/planos")
@RequiredArgsConstructor
@Tag(name = "Admin - Planos", description = "CRUD de planos da plataforma Bellory")
public class AdminPlanoBelloryController {

    private final AdminPlanoBelloryService service;

    @Operation(summary = "Listar todos os planos", description = "Retorna todos os planos incluindo inativos, ordenados por ordem de exibicao")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<PlanoBelloryResponseDTO>>> listarTodos() {
        try {
            List<PlanoBelloryResponseDTO> planos = service.listarTodos();
            return ResponseEntity.ok(ResponseAPI.<List<PlanoBelloryResponseDTO>>builder()
                    .success(true)
                    .message("Planos listados com sucesso")
                    .dados(planos)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<PlanoBelloryResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar planos: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Buscar plano por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<PlanoBelloryResponseDTO>> buscarPorId(
            @Parameter(description = "ID do plano") @PathVariable Long id) {
        try {
            PlanoBelloryResponseDTO plano = service.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<PlanoBelloryResponseDTO>builder()
                    .success(true)
                    .message("Plano encontrado")
                    .dados(plano)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<PlanoBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Criar novo plano")
    @PostMapping
    public ResponseEntity<ResponseAPI<PlanoBelloryResponseDTO>> criar(
            @Valid @RequestBody PlanoBelloryCreateDTO dto) {
        try {
            PlanoBelloryResponseDTO plano = service.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<PlanoBelloryResponseDTO>builder()
                            .success(true)
                            .message("Plano criado com sucesso")
                            .dados(plano)
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<PlanoBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Atualizar plano")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<PlanoBelloryResponseDTO>> atualizar(
            @Parameter(description = "ID do plano") @PathVariable Long id,
            @Valid @RequestBody PlanoBelloryUpdateDTO dto) {
        try {
            PlanoBelloryResponseDTO plano = service.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<PlanoBelloryResponseDTO>builder()
                    .success(true)
                    .message("Plano atualizado com sucesso")
                    .dados(plano)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<PlanoBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Desativar plano (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> desativar(
            @Parameter(description = "ID do plano") @PathVariable Long id) {
        try {
            service.desativar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Plano desativado com sucesso")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Ativar plano")
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<ResponseAPI<PlanoBelloryResponseDTO>> ativar(
            @Parameter(description = "ID do plano") @PathVariable Long id) {
        try {
            PlanoBelloryResponseDTO plano = service.ativar(id);
            return ResponseEntity.ok(ResponseAPI.<PlanoBelloryResponseDTO>builder()
                    .success(true)
                    .message("Plano ativado com sucesso")
                    .dados(plano)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<PlanoBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Reordenar planos")
    @PutMapping("/reordenar")
    public ResponseEntity<ResponseAPI<Void>> reordenar(
            @Valid @RequestBody ReordenarPlanosDTO dto) {
        try {
            service.reordenar(dto);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Planos reordenados com sucesso")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
