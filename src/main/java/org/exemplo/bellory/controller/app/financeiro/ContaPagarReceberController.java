package org.exemplo.bellory.controller.financeiro;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.financeiro.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.financeiro.ContaPagarService;
import org.exemplo.bellory.service.financeiro.ContaReceberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
@Tag(name = "Financeiro - Contas a Pagar/Receber", description = "Gerenciamento de contas a pagar e contas a receber")
public class ContaPagarReceberController {

    private final ContaPagarService contaPagarService;
    private final ContaReceberService contaReceberService;

    // ========================================
    // CONTAS A PAGAR
    // ========================================

    @PostMapping("/contas-pagar")
    @Operation(summary = "Criar conta a pagar")
    public ResponseEntity<ResponseAPI<ContaPagarResponseDTO>> criarContaPagar(
            @RequestBody ContaPagarCreateDTO dto) {
        try {
            ContaPagarResponseDTO resultado = contaPagarService.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(true)
                            .message("Conta a pagar criada com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message("Erro ao criar conta a pagar: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/contas-pagar/{id}")
    @Operation(summary = "Atualizar conta a pagar")
    public ResponseEntity<ResponseAPI<ContaPagarResponseDTO>> atualizarContaPagar(
            @PathVariable Long id, @RequestBody ContaPagarUpdateDTO dto) {
        try {
            ContaPagarResponseDTO resultado = contaPagarService.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<ContaPagarResponseDTO>builder()
                    .success(true)
                    .message("Conta a pagar atualizada com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar conta a pagar: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/contas-pagar/{id}/pagar")
    @Operation(summary = "Registrar pagamento de conta a pagar")
    public ResponseEntity<ResponseAPI<ContaPagarResponseDTO>> pagarContaPagar(
            @PathVariable Long id, @RequestBody PagamentoContaDTO dto) {
        try {
            ContaPagarResponseDTO resultado = contaPagarService.pagar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<ContaPagarResponseDTO>builder()
                    .success(true)
                    .message("Pagamento registrado com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message("Erro ao registrar pagamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-pagar")
    @Operation(summary = "Listar contas a pagar")
    public ResponseEntity<ResponseAPI<List<ContaPagarResponseDTO>>> listarContasPagar(
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String status) {
        try {
            FiltroFinanceiroDTO filtro = new FiltroFinanceiroDTO();
            filtro.setDataInicio(dataInicio);
            filtro.setDataFim(dataFim);
            filtro.setStatus(status);

            List<ContaPagarResponseDTO> contas = contaPagarService.listar(filtro);
            return ResponseEntity.ok(ResponseAPI.<List<ContaPagarResponseDTO>>builder()
                    .success(true)
                    .message("Contas a pagar listadas com sucesso.")
                    .dados(contas)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ContaPagarResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar contas a pagar: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-pagar/{id}")
    @Operation(summary = "Buscar conta a pagar por ID")
    public ResponseEntity<ResponseAPI<ContaPagarResponseDTO>> buscarContaPagar(@PathVariable Long id) {
        try {
            ContaPagarResponseDTO conta = contaPagarService.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<ContaPagarResponseDTO>builder()
                    .success(true)
                    .message("Conta a pagar encontrada.")
                    .dados(conta)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaPagarResponseDTO>builder()
                            .success(false)
                            .message("Erro ao buscar conta a pagar: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-pagar/vencidas")
    @Operation(summary = "Listar contas a pagar vencidas")
    public ResponseEntity<ResponseAPI<List<ContaPagarResponseDTO>>> listarContasPagarVencidas() {
        try {
            List<ContaPagarResponseDTO> contas = contaPagarService.listarVencidas();
            return ResponseEntity.ok(ResponseAPI.<List<ContaPagarResponseDTO>>builder()
                    .success(true)
                    .message("Contas vencidas listadas com sucesso.")
                    .dados(contas)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ContaPagarResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar contas vencidas: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-pagar/a-vencer")
    @Operation(summary = "Listar contas a pagar a vencer")
    public ResponseEntity<ResponseAPI<List<ContaPagarResponseDTO>>> listarContasPagarAVencer(
            @RequestParam(defaultValue = "7") int dias) {
        try {
            List<ContaPagarResponseDTO> contas = contaPagarService.listarAVencer(dias);
            return ResponseEntity.ok(ResponseAPI.<List<ContaPagarResponseDTO>>builder()
                    .success(true)
                    .message("Contas a vencer listadas com sucesso.")
                    .dados(contas)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ContaPagarResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar contas a vencer: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @DeleteMapping("/contas-pagar/{id}")
    @Operation(summary = "Cancelar conta a pagar")
    public ResponseEntity<ResponseAPI<Void>> cancelarContaPagar(@PathVariable Long id) {
        try {
            contaPagarService.cancelar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Conta a pagar cancelada com sucesso.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao cancelar conta a pagar: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ========================================
    // CONTAS A RECEBER
    // ========================================

    @PostMapping("/contas-receber")
    @Operation(summary = "Criar conta a receber")
    public ResponseEntity<ResponseAPI<ContaReceberResponseDTO>> criarContaReceber(
            @RequestBody ContaReceberCreateDTO dto) {
        try {
            ContaReceberResponseDTO resultado = contaReceberService.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(true)
                            .message("Conta a receber criada com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message("Erro ao criar conta a receber: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/contas-receber/{id}")
    @Operation(summary = "Atualizar conta a receber")
    public ResponseEntity<ResponseAPI<ContaReceberResponseDTO>> atualizarContaReceber(
            @PathVariable Long id, @RequestBody ContaReceberUpdateDTO dto) {
        try {
            ContaReceberResponseDTO resultado = contaReceberService.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<ContaReceberResponseDTO>builder()
                    .success(true)
                    .message("Conta a receber atualizada com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar conta a receber: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/contas-receber/{id}/receber")
    @Operation(summary = "Registrar recebimento")
    public ResponseEntity<ResponseAPI<ContaReceberResponseDTO>> receberContaReceber(
            @PathVariable Long id, @RequestBody PagamentoContaDTO dto) {
        try {
            ContaReceberResponseDTO resultado = contaReceberService.receber(id, dto);
            return ResponseEntity.ok(ResponseAPI.<ContaReceberResponseDTO>builder()
                    .success(true)
                    .message("Recebimento registrado com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message("Erro ao registrar recebimento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-receber")
    @Operation(summary = "Listar contas a receber")
    public ResponseEntity<ResponseAPI<List<ContaReceberResponseDTO>>> listarContasReceber(
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long clienteId) {
        try {
            FiltroFinanceiroDTO filtro = new FiltroFinanceiroDTO();
            filtro.setDataInicio(dataInicio);
            filtro.setDataFim(dataFim);
            filtro.setStatus(status);
            filtro.setClienteId(clienteId);

            List<ContaReceberResponseDTO> contas = contaReceberService.listar(filtro);
            return ResponseEntity.ok(ResponseAPI.<List<ContaReceberResponseDTO>>builder()
                    .success(true)
                    .message("Contas a receber listadas com sucesso.")
                    .dados(contas)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ContaReceberResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar contas a receber: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-receber/{id}")
    @Operation(summary = "Buscar conta a receber por ID")
    public ResponseEntity<ResponseAPI<ContaReceberResponseDTO>> buscarContaReceber(@PathVariable Long id) {
        try {
            ContaReceberResponseDTO conta = contaReceberService.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<ContaReceberResponseDTO>builder()
                    .success(true)
                    .message("Conta a receber encontrada.")
                    .dados(conta)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<ContaReceberResponseDTO>builder()
                            .success(false)
                            .message("Erro ao buscar conta a receber: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-receber/vencidas")
    @Operation(summary = "Listar contas a receber vencidas")
    public ResponseEntity<ResponseAPI<List<ContaReceberResponseDTO>>> listarContasReceberVencidas() {
        try {
            List<ContaReceberResponseDTO> contas = contaReceberService.listarVencidas();
            return ResponseEntity.ok(ResponseAPI.<List<ContaReceberResponseDTO>>builder()
                    .success(true)
                    .message("Contas vencidas listadas com sucesso.")
                    .dados(contas)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ContaReceberResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar contas vencidas: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-receber/a-vencer")
    @Operation(summary = "Listar contas a receber a vencer")
    public ResponseEntity<ResponseAPI<List<ContaReceberResponseDTO>>> listarContasReceberAVencer(
            @RequestParam(defaultValue = "7") int dias) {
        try {
            List<ContaReceberResponseDTO> contas = contaReceberService.listarAVencer(dias);
            return ResponseEntity.ok(ResponseAPI.<List<ContaReceberResponseDTO>>builder()
                    .success(true)
                    .message("Contas a vencer listadas com sucesso.")
                    .dados(contas)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ContaReceberResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar contas a vencer: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/contas-receber/cliente/{clienteId}/pendentes")
    @Operation(summary = "Listar pendentes por cliente")
    public ResponseEntity<ResponseAPI<List<ContaReceberResponseDTO>>> listarPendentesPorCliente(
            @PathVariable Long clienteId) {
        try {
            List<ContaReceberResponseDTO> contas = contaReceberService.listarPendentesPorCliente(clienteId);
            return ResponseEntity.ok(ResponseAPI.<List<ContaReceberResponseDTO>>builder()
                    .success(true)
                    .message("Contas pendentes do cliente listadas com sucesso.")
                    .dados(contas)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<ContaReceberResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar contas pendentes: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @DeleteMapping("/contas-receber/{id}")
    @Operation(summary = "Cancelar conta a receber")
    public ResponseEntity<ResponseAPI<Void>> cancelarContaReceber(@PathVariable Long id) {
        try {
            contaReceberService.cancelar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Conta a receber cancelada com sucesso.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message("Erro ao cancelar conta a receber: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
