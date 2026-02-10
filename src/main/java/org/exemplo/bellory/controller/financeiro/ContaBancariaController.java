package org.exemplo.bellory.controller.financeiro;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.financeiro.CentroCustoCreateDTO;
import org.exemplo.bellory.model.dto.financeiro.CentroCustoResponseDTO;
import org.exemplo.bellory.model.dto.financeiro.ContaBancariaCreateDTO;
import org.exemplo.bellory.model.dto.financeiro.ContaBancariaResponseDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.financeiro.CentroCustoService;
import org.exemplo.bellory.service.financeiro.ContaBancariaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
@Tag(name = "Financeiro - Contas e Centros de Custo", description = "Gerenciamento de contas bancárias e centros de custo")
public class ContaBancariaController {

    private final ContaBancariaService contaBancariaService;
    private final CentroCustoService centroCustoService;

    // ========================================
    // CONTAS BANCÁRIAS
    // ========================================

    @PostMapping("/contas-bancarias")
    @Operation(summary = "Criar conta bancária")
    public ResponseEntity<ResponseAPI<ContaBancariaResponseDTO>> criarContaBancaria(
            @RequestBody ContaBancariaCreateDTO dto) {
        try {
            ContaBancariaResponseDTO resultado = contaBancariaService.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ContaBancariaResponseDTO>builder()
                            .success(true)
                            .message("Conta bancária criada com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ContaBancariaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaBancariaResponseDTO>builder()
                            .success(false)
                            .message("Erro ao criar conta bancária: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/contas-bancarias/{id}")
    @Operation(summary = "Atualizar conta bancária")
    public ResponseEntity<ResponseAPI<ContaBancariaResponseDTO>> atualizarContaBancaria(
            @PathVariable Long id, @RequestBody ContaBancariaCreateDTO dto) {
        try {
            ContaBancariaResponseDTO resultado = contaBancariaService.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<ContaBancariaResponseDTO>builder()
                    .success(true)
                    .message("Conta bancária atualizada com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ContaBancariaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaBancariaResponseDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar conta bancária: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-bancarias")
    @Operation(summary = "Listar contas bancárias")
    public ResponseEntity<ResponseAPI<List<ContaBancariaResponseDTO>>> listarContasBancarias() {
        try {
            List<ContaBancariaResponseDTO> contas = contaBancariaService.listarTodas();
            return ResponseEntity.ok(ResponseAPI.<List<ContaBancariaResponseDTO>>builder()
                    .success(true)
                    .message("Contas bancárias listadas com sucesso.")
                    .dados(contas)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ContaBancariaResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar contas bancárias: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-bancarias/{id}")
    @Operation(summary = "Buscar conta bancária por ID")
    public ResponseEntity<ResponseAPI<ContaBancariaResponseDTO>> buscarContaBancaria(@PathVariable Long id) {
        try {
            ContaBancariaResponseDTO conta = contaBancariaService.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<ContaBancariaResponseDTO>builder()
                    .success(true)
                    .message("Conta bancária encontrada.")
                    .dados(conta)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ContaBancariaResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaBancariaResponseDTO>builder()
                            .success(false)
                            .message("Erro ao buscar conta bancária: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-bancarias/saldo-total")
    @Operation(summary = "Obter saldo total")
    public ResponseEntity<ResponseAPI<BigDecimal>> getSaldoTotal() {
        try {
            BigDecimal saldo = contaBancariaService.getSaldoTotal();
            return ResponseEntity.ok(ResponseAPI.<BigDecimal>builder()
                    .success(true)
                    .message("Saldo total calculado.")
                    .dados(saldo)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<BigDecimal>builder()
                            .success(false)
                            .message("Erro ao calcular saldo: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PatchMapping("/contas-bancarias/{id}/desativar")
    @Operation(summary = "Desativar conta bancária")
    public ResponseEntity<ResponseAPI<Void>> desativarContaBancaria(@PathVariable Long id) {
        try {
            contaBancariaService.desativar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Conta bancária desativada com sucesso.")
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
                            .message("Erro ao desativar conta bancária: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ========================================
    // CENTROS DE CUSTO
    // ========================================

    @PostMapping("/centros-custo")
    @Operation(summary = "Criar centro de custo")
    public ResponseEntity<ResponseAPI<CentroCustoResponseDTO>> criarCentroCusto(
            @RequestBody CentroCustoCreateDTO dto) {
        try {
            CentroCustoResponseDTO resultado = centroCustoService.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<CentroCustoResponseDTO>builder()
                            .success(true)
                            .message("Centro de custo criado com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<CentroCustoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CentroCustoResponseDTO>builder()
                            .success(false)
                            .message("Erro ao criar centro de custo: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/centros-custo/{id}")
    @Operation(summary = "Atualizar centro de custo")
    public ResponseEntity<ResponseAPI<CentroCustoResponseDTO>> atualizarCentroCusto(
            @PathVariable Long id, @RequestBody CentroCustoCreateDTO dto) {
        try {
            CentroCustoResponseDTO resultado = centroCustoService.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<CentroCustoResponseDTO>builder()
                    .success(true)
                    .message("Centro de custo atualizado com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CentroCustoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CentroCustoResponseDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar centro de custo: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/centros-custo")
    @Operation(summary = "Listar centros de custo")
    public ResponseEntity<ResponseAPI<List<CentroCustoResponseDTO>>> listarCentrosCusto() {
        try {
            List<CentroCustoResponseDTO> centros = centroCustoService.listarTodos();
            return ResponseEntity.ok(ResponseAPI.<List<CentroCustoResponseDTO>>builder()
                    .success(true)
                    .message("Centros de custo listados com sucesso.")
                    .dados(centros)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<CentroCustoResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar centros de custo: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/centros-custo/{id}")
    @Operation(summary = "Buscar centro de custo por ID")
    public ResponseEntity<ResponseAPI<CentroCustoResponseDTO>> buscarCentroCusto(@PathVariable Long id) {
        try {
            CentroCustoResponseDTO centro = centroCustoService.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<CentroCustoResponseDTO>builder()
                    .success(true)
                    .message("Centro de custo encontrado.")
                    .dados(centro)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<CentroCustoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CentroCustoResponseDTO>builder()
                            .success(false)
                            .message("Erro ao buscar centro de custo: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PatchMapping("/centros-custo/{id}/desativar")
    @Operation(summary = "Desativar centro de custo")
    public ResponseEntity<ResponseAPI<Void>> desativarCentroCusto(@PathVariable Long id) {
        try {
            centroCustoService.desativar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Centro de custo desativado com sucesso.")
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
                            .message("Erro ao desativar centro de custo: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PatchMapping("/centros-custo/{id}/ativar")
    @Operation(summary = "Ativar centro de custo")
    public ResponseEntity<ResponseAPI<Void>> ativarCentroCusto(@PathVariable Long id) {
        try {
            centroCustoService.ativar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Centro de custo ativado com sucesso.")
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
                            .message("Erro ao ativar centro de custo: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
