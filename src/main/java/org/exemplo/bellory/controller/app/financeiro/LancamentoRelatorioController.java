package org.exemplo.bellory.controller.financeiro;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.financeiro.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.financeiro.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/financeiro")
@RequiredArgsConstructor
@Tag(name = "Financeiro - Lançamentos e Relatórios", description = "Lançamentos financeiros, fluxo de caixa, DRE e balanço")
public class LancamentoRelatorioController {

    private final LancamentoFinanceiroService lancamentoService;
    private final FluxoCaixaService fluxoCaixaService;
    private final DREService dreService;
    private final BalancoFinanceiroService balancoService;
    private final DashboardFinanceiroService dashboardService;

    // ========================================
    // LANÇAMENTOS FINANCEIROS
    // ========================================

    @PostMapping("/lancamentos")
    @Operation(summary = "Criar lançamento financeiro")
    public ResponseEntity<ResponseAPI<LancamentoFinanceiroResponseDTO>> criarLancamento(
            @RequestBody LancamentoFinanceiroCreateDTO dto) {
        try {
            LancamentoFinanceiroResponseDTO resultado = lancamentoService.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(true)
                            .message("Lançamento financeiro criado com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message("Erro ao criar lançamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PutMapping("/lancamentos/{id}")
    @Operation(summary = "Atualizar lançamento financeiro")
    public ResponseEntity<ResponseAPI<LancamentoFinanceiroResponseDTO>> atualizarLancamento(
            @PathVariable Long id, @RequestBody LancamentoFinanceiroUpdateDTO dto) {
        try {
            LancamentoFinanceiroResponseDTO resultado = lancamentoService.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                    .success(true)
                    .message("Lançamento financeiro atualizado com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar lançamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/lancamentos")
    @Operation(summary = "Listar lançamentos financeiros")
    public ResponseEntity<ResponseAPI<List<LancamentoFinanceiroResponseDTO>>> listarLancamentos(
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long categoriaFinanceiraId,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long contaBancariaId) {
        try {
            FiltroFinanceiroDTO filtro = new FiltroFinanceiroDTO();
            filtro.setDataInicio(dataInicio);
            filtro.setDataFim(dataFim);
            filtro.setTipo(tipo);
            filtro.setCategoriaFinanceiraId(categoriaFinanceiraId);
            filtro.setCentroCustoId(centroCustoId);
            filtro.setContaBancariaId(contaBancariaId);

            List<LancamentoFinanceiroResponseDTO> lancamentos = lancamentoService.listar(filtro);
            return ResponseEntity.ok(ResponseAPI.<List<LancamentoFinanceiroResponseDTO>>builder()
                    .success(true)
                    .message("Lançamentos listados com sucesso.")
                    .dados(lancamentos)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<LancamentoFinanceiroResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar lançamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/lancamentos/{id}")
    @Operation(summary = "Buscar lançamento por ID")
    public ResponseEntity<ResponseAPI<LancamentoFinanceiroResponseDTO>> buscarLancamento(@PathVariable Long id) {
        try {
            LancamentoFinanceiroResponseDTO lancamento = lancamentoService.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                    .success(true)
                    .message("Lançamento encontrado.")
                    .dados(lancamento)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message("Erro ao buscar lançamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @PostMapping("/lancamentos/{id}/efetivar")
    @Operation(summary = "Efetivar lançamento financeiro")
    public ResponseEntity<ResponseAPI<LancamentoFinanceiroResponseDTO>> efetivarLancamento(@PathVariable Long id) {
        try {
            LancamentoFinanceiroResponseDTO resultado = lancamentoService.efetivar(id);
            return ResponseEntity.ok(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                    .success(true)
                    .message("Lançamento efetivado com sucesso.")
                    .dados(resultado)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(409)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<LancamentoFinanceiroResponseDTO>builder()
                            .success(false)
                            .message("Erro ao efetivar lançamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @DeleteMapping("/lancamentos/{id}")
    @Operation(summary = "Cancelar lançamento financeiro")
    public ResponseEntity<ResponseAPI<Void>> cancelarLancamento(@PathVariable Long id) {
        try {
            lancamentoService.cancelar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Lançamento cancelado com sucesso.")
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
                            .message("Erro ao cancelar lançamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ========================================
    // RELATÓRIOS FINANCEIROS
    // ========================================

    @GetMapping("/dashboard")
    @Operation(summary = "Obter dashboard financeiro")
    public ResponseEntity<ResponseAPI<DashboardFinanceiroDTO>> getDashboard() {
        try {
            DashboardFinanceiroDTO dashboard = dashboardService.gerarDashboard();
            return ResponseEntity.ok(ResponseAPI.<DashboardFinanceiroDTO>builder()
                    .success(true)
                    .message("Dashboard financeiro gerado com sucesso.")
                    .dados(dashboard)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<DashboardFinanceiroDTO>builder()
                            .success(false)
                            .message("Erro ao gerar dashboard: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/relatorios/fluxo-caixa")
    @Operation(summary = "Gerar relatório de fluxo de caixa")
    public ResponseEntity<ResponseAPI<FluxoCaixaDTO>> getFluxoCaixa(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim) {
        try {
            FluxoCaixaDTO fluxo = fluxoCaixaService.gerarFluxoCaixa(dataInicio, dataFim);
            return ResponseEntity.ok(ResponseAPI.<FluxoCaixaDTO>builder()
                    .success(true)
                    .message("Fluxo de caixa gerado com sucesso.")
                    .dados(fluxo)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<FluxoCaixaDTO>builder()
                            .success(false)
                            .message("Erro ao gerar fluxo de caixa: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/relatorios/dre")
    @Operation(summary = "Gerar DRE (Demonstração do Resultado)")
    public ResponseEntity<ResponseAPI<DREDTO>> getDRE(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim) {
        try {
            DREDTO dre = dreService.gerarDRE(dataInicio, dataFim);
            return ResponseEntity.ok(ResponseAPI.<DREDTO>builder()
                    .success(true)
                    .message("DRE gerado com sucesso.")
                    .dados(dre)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<DREDTO>builder()
                            .success(false)
                            .message("Erro ao gerar DRE: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/relatorios/balanco")
    @Operation(summary = "Gerar balanço financeiro")
    public ResponseEntity<ResponseAPI<BalancoFinanceiroDTO>> getBalanco(
            @RequestParam(required = false) LocalDate dataReferencia) {
        try {
            BalancoFinanceiroDTO balanco = balancoService.gerarBalanco(dataReferencia);
            return ResponseEntity.ok(ResponseAPI.<BalancoFinanceiroDTO>builder()
                    .success(true)
                    .message("Balanço financeiro gerado com sucesso.")
                    .dados(balanco)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<BalancoFinanceiroDTO>builder()
                            .success(false)
                            .message("Erro ao gerar balanço: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
