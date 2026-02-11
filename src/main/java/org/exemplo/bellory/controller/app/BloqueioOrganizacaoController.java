package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.BloqueioOrganizacaoCreateDTO;
import org.exemplo.bellory.model.dto.BloqueioOrganizacaoDTO;
import org.exemplo.bellory.model.dto.BloqueioOrganizacaoUpdateDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.BloqueioOrganizacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bloqueio-organizacao")
@Tag(name = "Bloqueios e Feriados", description = "Gerenciamento de bloqueios de datas e feriados da organização")
public class BloqueioOrganizacaoController {

    private final BloqueioOrganizacaoService bloqueioService;

    public BloqueioOrganizacaoController(BloqueioOrganizacaoService bloqueioService) {
        this.bloqueioService = bloqueioService;
    }

    /**
     * Lista todos os bloqueios/feriados da organização
     */
    @GetMapping
    @Operation(summary = "Listar todos os bloqueios/feriados")
    public ResponseEntity<ResponseAPI<List<BloqueioOrganizacaoDTO>>> listarTodos() {
        try {
            List<BloqueioOrganizacaoDTO> bloqueios = bloqueioService.listarTodos();

            return ResponseEntity.ok(ResponseAPI.<List<BloqueioOrganizacaoDTO>>builder()
                    .success(true)
                    .message("Bloqueios recuperados com sucesso.")
                    .dados(bloqueios)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<BloqueioOrganizacaoDTO>>builder()
                            .success(false)
                            .message("Erro ao buscar bloqueios: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Lista apenas bloqueios/feriados ativos da organização
     */
    @GetMapping("/ativos")
    @Operation(summary = "Listar bloqueios ativos")
    public ResponseEntity<ResponseAPI<List<BloqueioOrganizacaoDTO>>> listarAtivos() {
        try {
            List<BloqueioOrganizacaoDTO> bloqueios = bloqueioService.listarAtivos();

            return ResponseEntity.ok(ResponseAPI.<List<BloqueioOrganizacaoDTO>>builder()
                    .success(true)
                    .message("Bloqueios ativos recuperados com sucesso.")
                    .dados(bloqueios)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<BloqueioOrganizacaoDTO>>builder()
                            .success(false)
                            .message("Erro ao buscar bloqueios ativos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Busca um bloqueio específico por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar bloqueio por ID")
    public ResponseEntity<ResponseAPI<BloqueioOrganizacaoDTO>> buscarPorId(@PathVariable Long id) {
        try {
            BloqueioOrganizacaoDTO bloqueio = bloqueioService.buscarPorId(id);

            return ResponseEntity.ok(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                    .success(true)
                    .message("Bloqueio encontrado com sucesso.")
                    .dados(bloqueio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message("Erro ao buscar bloqueio: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Lista bloqueios ativos dentro de um período (para o calendário)
     * Parâmetros: dataInicio e dataFim no formato yyyy-MM-dd
     */
    @GetMapping("/periodo")
    @Operation(summary = "Listar bloqueios por período")
    public ResponseEntity<ResponseAPI<List<BloqueioOrganizacaoDTO>>> listarPorPeriodo(
            @RequestParam String dataInicio,
            @RequestParam String dataFim) {
        try {
            LocalDate inicio = LocalDate.parse(dataInicio);
            LocalDate fim = LocalDate.parse(dataFim);

            if (fim.isBefore(inicio)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseAPI.<List<BloqueioOrganizacaoDTO>>builder()
                                .success(false)
                                .message("A data de fim não pode ser anterior à data de início.")
                                .errorCode(400)
                                .build());
            }

            List<BloqueioOrganizacaoDTO> bloqueios = bloqueioService.listarPorPeriodo(inicio, fim);

            return ResponseEntity.ok(ResponseAPI.<List<BloqueioOrganizacaoDTO>>builder()
                    .success(true)
                    .message("Bloqueios do período recuperados com sucesso.")
                    .dados(bloqueios)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<List<BloqueioOrganizacaoDTO>>builder()
                            .success(false)
                            .message("Formato de data inválido. Use o formato: yyyy-MM-dd")
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<BloqueioOrganizacaoDTO>>builder()
                            .success(false)
                            .message("Erro ao buscar bloqueios do período: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Verifica se uma data específica está bloqueada
     */
    @GetMapping("/verificar/{data}")
    @Operation(summary = "Verificar se data está bloqueada")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> verificarData(@PathVariable String data) {
        try {
            LocalDate dataVerificar = LocalDate.parse(data);
            boolean bloqueada = bloqueioService.isDataBloqueada(
                    org.exemplo.bellory.context.TenantContext.getCurrentOrganizacaoId(),
                    dataVerificar);

            Map<String, Object> resultado = Map.of(
                    "data", data,
                    "bloqueada", bloqueada
            );

            return ResponseEntity.ok(ResponseAPI.<Map<String, Object>>builder()
                    .success(true)
                    .message(bloqueada ? "Data bloqueada." : "Data disponível.")
                    .dados(resultado)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message("Formato de data inválido. Use o formato: yyyy-MM-dd")
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message("Erro ao verificar data: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Cria um novo bloqueio/feriado manual
     */
    @PostMapping
    @Operation(summary = "Criar novo bloqueio/feriado")
    public ResponseEntity<ResponseAPI<BloqueioOrganizacaoDTO>> criar(
            @RequestBody BloqueioOrganizacaoCreateDTO dto) {
        try {
            BloqueioOrganizacaoDTO bloqueio = bloqueioService.criar(dto);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(true)
                            .message("Bloqueio criado com sucesso.")
                            .dados(bloqueio)
                            .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message("Erro ao criar bloqueio: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Atualiza um bloqueio/feriado existente
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar bloqueio/feriado")
    public ResponseEntity<ResponseAPI<BloqueioOrganizacaoDTO>> atualizar(
            @PathVariable Long id,
            @RequestBody BloqueioOrganizacaoUpdateDTO dto) {
        try {
            BloqueioOrganizacaoDTO bloqueio = bloqueioService.atualizar(id, dto);

            return ResponseEntity.ok(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                    .success(true)
                    .message("Bloqueio atualizado com sucesso.")
                    .dados(bloqueio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar bloqueio: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Alterna o status ativo/inativo de um bloqueio
     */
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Alternar status ativo/inativo")
    public ResponseEntity<ResponseAPI<BloqueioOrganizacaoDTO>> toggleAtivo(@PathVariable Long id) {
        try {
            BloqueioOrganizacaoDTO bloqueio = bloqueioService.toggleAtivo(id);

            return ResponseEntity.ok(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                    .success(true)
                    .message("Status do bloqueio alterado com sucesso.")
                    .dados(bloqueio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<BloqueioOrganizacaoDTO>builder()
                            .success(false)
                            .message("Erro ao alterar status: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Remove um bloqueio/feriado
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover bloqueio/feriado")
    public ResponseEntity<ResponseAPI<Void>> remover(@PathVariable Long id) {
        try {
            bloqueioService.remover(id);

            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Bloqueio removido com sucesso.")
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao remover bloqueio: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    /**
     * Importa feriados nacionais de um ano via BrasilAPI
     * Se o ano não for informado, usa o ano atual
     */
    @PostMapping("/importar-feriados")
    @Operation(summary = "Importar feriados nacionais via BrasilAPI")
    public ResponseEntity<ResponseAPI<Map<String, Object>>> importarFeriados(
            @RequestParam(required = false) Integer ano) {
        try {
            int importados = bloqueioService.importarFeriadosNacionais(ano);

            Map<String, Object> resultado = Map.of(
                    "ano", ano != null ? ano : LocalDate.now().getYear(),
                    "importados", importados
            );

            String mensagem = importados > 0
                    ? importados + " feriado(s) importado(s) com sucesso!"
                    : "Todos os feriados já estão cadastrados.";

            return ResponseEntity.ok(ResponseAPI.<Map<String, Object>>builder()
                    .success(true)
                    .message(mensagem)
                    .dados(resultado)
                    .build());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Map<String, Object>>builder()
                            .success(false)
                            .message("Erro ao importar feriados: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
