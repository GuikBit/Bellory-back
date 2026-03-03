package org.exemplo.bellory.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.cupom.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.admin.AdminCupomDescontoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/cupons")
@RequiredArgsConstructor
@Tag(name = "Admin - Cupons de Desconto", description = "CRUD de cupons de desconto para assinaturas")
public class AdminCupomDescontoController {

    private final AdminCupomDescontoService service;

    @Operation(summary = "Listar todos os cupons")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<CupomDescontoResponseDTO>>> listarTodos() {
        try {
            List<CupomDescontoResponseDTO> cupons = service.listarTodos();
            return ResponseEntity.ok(ResponseAPI.<List<CupomDescontoResponseDTO>>builder()
                    .success(true)
                    .message("Cupons listados com sucesso")
                    .dados(cupons)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CupomDescontoResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar cupons: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Listar cupons vigentes")
    @GetMapping("/vigentes")
    public ResponseEntity<ResponseAPI<List<CupomDescontoResponseDTO>>> listarVigentes() {
        try {
            List<CupomDescontoResponseDTO> cupons = service.listarVigentes();
            return ResponseEntity.ok(ResponseAPI.<List<CupomDescontoResponseDTO>>builder()
                    .success(true)
                    .message("Cupons vigentes listados com sucesso")
                    .dados(cupons)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CupomDescontoResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar cupons vigentes: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Buscar cupom por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<CupomDescontoResponseDTO>> buscarPorId(
            @Parameter(description = "ID do cupom") @PathVariable Long id) {
        try {
            CupomDescontoResponseDTO cupom = service.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<CupomDescontoResponseDTO>builder()
                    .success(true)
                    .message("Cupom encontrado")
                    .dados(cupom)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CupomDescontoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Criar novo cupom de desconto")
    @PostMapping
    public ResponseEntity<ResponseAPI<CupomDescontoResponseDTO>> criar(
            @Valid @RequestBody CupomDescontoCreateDTO dto) {
        try {
            CupomDescontoResponseDTO cupom = service.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<CupomDescontoResponseDTO>builder()
                            .success(true)
                            .message("Cupom criado com sucesso")
                            .dados(cupom)
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<CupomDescontoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Atualizar cupom de desconto")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<CupomDescontoResponseDTO>> atualizar(
            @Parameter(description = "ID do cupom") @PathVariable Long id,
            @Valid @RequestBody CupomDescontoUpdateDTO dto) {
        try {
            CupomDescontoResponseDTO cupom = service.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<CupomDescontoResponseDTO>builder()
                    .success(true)
                    .message("Cupom atualizado com sucesso")
                    .dados(cupom)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<CupomDescontoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Desativar cupom (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> desativar(
            @Parameter(description = "ID do cupom") @PathVariable Long id) {
        try {
            service.desativar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Cupom desativado com sucesso")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Reativar cupom")
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<ResponseAPI<CupomDescontoResponseDTO>> ativar(
            @Parameter(description = "ID do cupom") @PathVariable Long id) {
        try {
            CupomDescontoResponseDTO cupom = service.ativar(id);
            return ResponseEntity.ok(ResponseAPI.<CupomDescontoResponseDTO>builder()
                    .success(true)
                    .message("Cupom ativado com sucesso")
                    .dados(cupom)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CupomDescontoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Listar utilizacoes de um cupom")
    @GetMapping("/{id}/utilizacoes")
    public ResponseEntity<ResponseAPI<List<CupomUtilizacaoDTO>>> listarUtilizacoes(
            @Parameter(description = "ID do cupom") @PathVariable Long id) {
        try {
            List<CupomUtilizacaoDTO> utilizacoes = service.listarUtilizacoes(id);
            return ResponseEntity.ok(ResponseAPI.<List<CupomUtilizacaoDTO>>builder()
                    .success(true)
                    .message("Utilizacoes listadas com sucesso")
                    .dados(utilizacoes)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<List<CupomUtilizacaoDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
