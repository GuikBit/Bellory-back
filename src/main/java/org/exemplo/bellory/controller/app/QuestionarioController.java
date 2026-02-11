package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.exemplo.bellory.model.dto.questionario.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.questionario.Questionario;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;
import org.exemplo.bellory.service.questionario.QuestionarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questionarios")
@Tag(name = "Questionários", description = "Gerenciamento de questionários")
public class QuestionarioController {

    private final QuestionarioService questionarioService;

    public QuestionarioController(QuestionarioService questionarioService) {
        this.questionarioService = questionarioService;
    }

    @PostMapping
    @Operation(summary = "Criar novo questionário")
    public ResponseEntity<ResponseAPI<QuestionarioDTO>> criar(
            @Valid @RequestBody QuestionarioCreateDTO dto) {
        try {
            Questionario questionario = questionarioService.criar(dto);
            QuestionarioDTO responseDTO = new QuestionarioDTO(questionario);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<QuestionarioDTO>builder()
                            .success(true)
                            .message("Questionário criado com sucesso!")
                            .dados(responseDTO)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<QuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<QuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar questionário existente")
    public ResponseEntity<ResponseAPI<QuestionarioDTO>> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody QuestionarioCreateDTO dto) {
        try {
            Questionario questionario = questionarioService.atualizar(id, dto);
            QuestionarioDTO responseDTO = new QuestionarioDTO(questionario);

            return ResponseEntity.ok(ResponseAPI.<QuestionarioDTO>builder()
                    .success(true)
                    .message("Questionário atualizado com sucesso!")
                    .dados(responseDTO)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<QuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<QuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar questionário")
    public ResponseEntity<ResponseAPI<Void>> deletar(@PathVariable Long id) {
        try {
            questionarioService.deletar(id);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Questionário deletado com sucesso!")
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

    @GetMapping("/{id}")
    @Operation(summary = "Buscar questionário por ID")
    public ResponseEntity<ResponseAPI<QuestionarioDTO>> buscarPorId(@PathVariable Long id) {
        try {
            QuestionarioDTO dto = questionarioService.buscarPorId(id);

            return ResponseEntity.ok(ResponseAPI.<QuestionarioDTO>builder()
                    .success(true)
                    .dados(dto)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<QuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<QuestionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }

    @GetMapping
    @Operation(summary = "Listar todos os questionários da organização (paginado)")
    public ResponseEntity<ResponseAPI<Page<QuestionarioDTO>>> listar(
            @PageableDefault(size = 20, sort = "dtCriacao") Pageable pageable) {
        try {
            Page<QuestionarioDTO> page = questionarioService.listarPorOrganizacao(pageable);

            return ResponseEntity.ok(ResponseAPI.<Page<QuestionarioDTO>>builder()
                    .success(true)
                    .dados(page)
                    .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Page<QuestionarioDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }

    @GetMapping("/ativos")
    @Operation(summary = "Listar questionários ativos")
    public ResponseEntity<ResponseAPI<List<QuestionarioDTO>>> listarAtivos() {
        try {
            List<QuestionarioDTO> list = questionarioService.listarAtivosPorOrganizacao();

            return ResponseEntity.ok(ResponseAPI.<List<QuestionarioDTO>>builder()
                    .success(true)
                    .dados(list)
                    .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<List<QuestionarioDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }

    @GetMapping("/tipo/{tipo}")
    @Operation(summary = "Listar questionários por tipo")
    public ResponseEntity<ResponseAPI<List<QuestionarioDTO>>> listarPorTipo(
            @PathVariable TipoQuestionario tipo) {
        try {
            List<QuestionarioDTO> list = questionarioService.listarPorTipo(tipo);

            return ResponseEntity.ok(ResponseAPI.<List<QuestionarioDTO>>builder()
                    .success(true)
                    .dados(list)
                    .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<List<QuestionarioDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }

    @GetMapping("/pesquisar")
    @Operation(summary = "Pesquisar questionários por título ou descrição")
    public ResponseEntity<ResponseAPI<Page<QuestionarioDTO>>> pesquisar(
            @RequestParam String termo,
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            Page<QuestionarioDTO> page = questionarioService.pesquisar(termo, pageable);

            return ResponseEntity.ok(ResponseAPI.<Page<QuestionarioDTO>>builder()
                    .success(true)
                    .dados(page)
                    .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<Page<QuestionarioDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        }
    }

    @GetMapping("/{id}/total-respostas")
    @Operation(summary = "Contar total de respostas de um questionário")
    public ResponseEntity<ResponseAPI<Long>> contarRespostas(@PathVariable Long id) {
        try {
            Long total = questionarioService.contarRespostas(id);

            return ResponseEntity.ok(ResponseAPI.<Long>builder()
                    .success(true)
                    .dados(total)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Long>builder()
                            .success(false)
                            .message("Erro ao contar respostas.")
                            .errorCode(500)
                            .build());
        }
    }
}
