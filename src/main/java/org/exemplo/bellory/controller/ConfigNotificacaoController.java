package org.exemplo.bellory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.notificacao.ConfigNotificacaoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.notificacao.ConfigNotificacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config/notificacao")
@RequiredArgsConstructor
public class ConfigNotificacaoController {

    private final ConfigNotificacaoService service;

    @GetMapping
    public ResponseEntity<ResponseAPI<List<ConfigNotificacaoDTO>>> listar() {
        try {
            return ResponseEntity.ok(ResponseAPI.<List<ConfigNotificacaoDTO>>builder()
                .success(true)
                .message("Configuracoes recuperadas")
                .dados(service.listarConfiguracoes())
                .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseAPI.<List<ConfigNotificacaoDTO>>builder()
                    .success(false)
                    .message("Erro: " + e.getMessage())
                    .errorCode(500)
                    .build());
        }
    }

    @GetMapping("/todas")
    public ResponseEntity<ResponseAPI<List<ConfigNotificacaoDTO>>> listarTodas() {
        try {
            return ResponseEntity.ok(ResponseAPI.<List<ConfigNotificacaoDTO>>builder()
                .success(true)
                .message("Todas configuracoes recuperadas")
                .dados(service.listarTodasConfiguracoes())
                .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseAPI.<List<ConfigNotificacaoDTO>>builder()
                    .success(false)
                    .message("Erro: " + e.getMessage())
                    .errorCode(500)
                    .build());
        }
    }

    @PostMapping
    public ResponseEntity<ResponseAPI<ConfigNotificacaoDTO>> criar(
            @RequestBody @Valid ConfigNotificacaoDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseAPI.<ConfigNotificacaoDTO>builder()
                    .success(true)
                    .message("Configuracao criada")
                    .dados(service.criarConfiguracao(dto))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ResponseAPI.<ConfigNotificacaoDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .errorCode(400)
                    .build());
        }
    }

    /**
     * Salva ou atualiza uma configuracao baseada no tipo.
     * Se ja existir configuracao para o tipo, atualiza. Senao, cria nova.
     */
    @PostMapping("/upsert")
    public ResponseEntity<ResponseAPI<ConfigNotificacaoDTO>> salvarOuAtualizar(
            @RequestBody ConfigNotificacaoDTO dto) {
        try {
            return ResponseEntity.ok(ResponseAPI.<ConfigNotificacaoDTO>builder()
                .success(true)
                .message("Configuracao salva com sucesso")
                .dados(service.salvarOuAtualizar(dto))
                .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ResponseAPI.<ConfigNotificacaoDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .errorCode(400)
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<ConfigNotificacaoDTO>> atualizar(
            @PathVariable Long id, @RequestBody @Valid ConfigNotificacaoDTO dto) {
        try {
            return ResponseEntity.ok(ResponseAPI.<ConfigNotificacaoDTO>builder()
                .success(true)
                .message("Configuracao atualizada")
                .dados(service.atualizarConfiguracao(id, dto))
                .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseAPI.<ConfigNotificacaoDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .errorCode(404)
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> deletar(@PathVariable Long id) {
        try {
            service.deletarConfiguracao(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                .success(true)
                .message("Configuracao removida")
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<ResponseAPI<ConfigNotificacaoDTO>> alterarStatus(
            @PathVariable Long id, @RequestParam boolean ativo) {
        try {
            return ResponseEntity.ok(ResponseAPI.<ConfigNotificacaoDTO>builder()
                .success(true)
                .message("Status alterado")
                .dados(service.alterarStatus(id, ativo))
                .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseAPI.<ConfigNotificacaoDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .errorCode(404)
                    .build());
        }
    }
}
