package org.exemplo.bellory.controller.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.financeiro.CategoriaFinanceiraCreateDTO;
import org.exemplo.bellory.model.dto.financeiro.CategoriaFinanceiraResponseDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.financeiro.CategoriaFinanceiraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financeiro/categorias")
@RequiredArgsConstructor
public class CategoriaFinanceiraController {

    private final CategoriaFinanceiraService categoriaService;

    @PostMapping
    public ResponseEntity<ResponseAPI<CategoriaFinanceiraResponseDTO>> criar(
            @RequestBody CategoriaFinanceiraCreateDTO dto) {
        try {
            CategoriaFinanceiraResponseDTO resultado = categoriaService.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                            .success(true)
                            .message("Categoria financeira criada com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                            .success(false)
                            .message("Erro ao criar categoria: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<CategoriaFinanceiraResponseDTO>> atualizar(
            @PathVariable Long id, @RequestBody CategoriaFinanceiraCreateDTO dto) {
        try {
            CategoriaFinanceiraResponseDTO resultado = categoriaService.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                    .success(true)
                    .message("Categoria financeira atualizada com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar categoria: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<ResponseAPI<List<CategoriaFinanceiraResponseDTO>>> listar(
            @RequestParam(required = false) String tipo) {
        try {
            List<CategoriaFinanceiraResponseDTO> categorias;
            if (tipo != null) {
                categorias = categoriaService.listarPorTipo(tipo);
            } else {
                categorias = categoriaService.listarTodas();
            }
            return ResponseEntity.ok(ResponseAPI.<List<CategoriaFinanceiraResponseDTO>>builder()
                    .success(true)
                    .message("Categorias listadas com sucesso.")
                    .dados(categorias)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CategoriaFinanceiraResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar categorias: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/arvore")
    public ResponseEntity<ResponseAPI<List<CategoriaFinanceiraResponseDTO>>> listarArvore(
            @RequestParam(required = false) String tipo) {
        try {
            List<CategoriaFinanceiraResponseDTO> categorias;
            if (tipo != null) {
                categorias = categoriaService.listarArvorePorTipo(tipo);
            } else {
                categorias = categoriaService.listarArvore();
            }
            return ResponseEntity.ok(ResponseAPI.<List<CategoriaFinanceiraResponseDTO>>builder()
                    .success(true)
                    .message("Árvore de categorias listada com sucesso.")
                    .dados(categorias)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CategoriaFinanceiraResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar árvore de categorias: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<CategoriaFinanceiraResponseDTO>> buscarPorId(@PathVariable Long id) {
        try {
            CategoriaFinanceiraResponseDTO categoria = categoriaService.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                    .success(true)
                    .message("Categoria encontrada.")
                    .dados(categoria)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CategoriaFinanceiraResponseDTO>builder()
                            .success(false)
                            .message("Erro ao buscar categoria: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<ResponseAPI<Void>> desativar(@PathVariable Long id) {
        try {
            categoriaService.desativar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Categoria desativada com sucesso.")
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
                            .message("Erro ao desativar categoria: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<ResponseAPI<Void>> ativar(@PathVariable Long id) {
        try {
            categoriaService.ativar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Categoria ativada com sucesso.")
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
                            .message("Erro ao ativar categoria: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
