package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.relatorio.*;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.relatorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatórios", description = "Geração de relatórios gerenciais e analíticos")
public class RelatorioController {

    private final RelatorioFaturamentoService faturamentoService;
    private final RelatorioAgendamentoService agendamentoService;
    private final RelatorioNotificacaoService notificacaoService;
    private final RelatorioCobrancaService cobrancaService;
    private final RelatorioFuncionarioService funcionarioService;
    private final RelatorioClienteService clienteService;
    private final RelatorioServicoService servicoService;
    private final RelatorioDashboardService dashboardService;

    public RelatorioController(
            RelatorioFaturamentoService faturamentoService,
            RelatorioAgendamentoService agendamentoService,
            RelatorioNotificacaoService notificacaoService,
            RelatorioCobrancaService cobrancaService,
            RelatorioFuncionarioService funcionarioService,
            RelatorioClienteService clienteService,
            RelatorioServicoService servicoService,
            RelatorioDashboardService dashboardService) {
        this.faturamentoService = faturamentoService;
        this.agendamentoService = agendamentoService;
        this.notificacaoService = notificacaoService;
        this.cobrancaService = cobrancaService;
        this.funcionarioService = funcionarioService;
        this.clienteService = clienteService;
        this.servicoService = servicoService;
        this.dashboardService = dashboardService;
    }

    // ==================== DASHBOARD EXECUTIVO (DETALHADO) ====================

    @PostMapping("/dashboard")
    @Operation(summary = "Gerar dashboard executivo")
    public ResponseEntity<ResponseAPI<RelatorioDashboardDTO>> getDashboardExecutivo(
            @RequestBody RelatorioFiltroDTO filtro) {
        try {
            RelatorioDashboardDTO relatorio = dashboardService.gerarRelatorio(filtro);

            return ResponseEntity.ok(ResponseAPI.<RelatorioDashboardDTO>builder()
                    .success(true)
                    .message("Dashboard executivo gerado com sucesso.")
                    .dados(relatorio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RelatorioDashboardDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<RelatorioDashboardDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<RelatorioDashboardDTO>builder()
                            .success(false)
                            .message("Erro interno ao gerar dashboard executivo: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== FATURAMENTO ====================

    @PostMapping("/faturamento")
    @Operation(summary = "Gerar relatório de faturamento")
    public ResponseEntity<ResponseAPI<RelatorioFaturamentoDTO>> getRelatorioFaturamento(
            @RequestBody RelatorioFiltroDTO filtro) {
        try {
            RelatorioFaturamentoDTO relatorio = faturamentoService.gerarRelatorio(filtro);

            return ResponseEntity.ok(ResponseAPI.<RelatorioFaturamentoDTO>builder()
                    .success(true)
                    .message("Relatório de faturamento gerado com sucesso.")
                    .dados(relatorio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RelatorioFaturamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<RelatorioFaturamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<RelatorioFaturamentoDTO>builder()
                            .success(false)
                            .message("Erro interno ao gerar relatório de faturamento: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== AGENDAMENTOS ====================

    @PostMapping("/agendamentos")
    @Operation(summary = "Gerar relatório de agendamentos")
    public ResponseEntity<ResponseAPI<RelatorioAgendamentoDTO>> getRelatorioAgendamentos(
            @RequestBody RelatorioFiltroDTO filtro) {
        try {
            RelatorioAgendamentoDTO relatorio = agendamentoService.gerarRelatorio(filtro);

            return ResponseEntity.ok(ResponseAPI.<RelatorioAgendamentoDTO>builder()
                    .success(true)
                    .message("Relatório de agendamentos gerado com sucesso.")
                    .dados(relatorio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RelatorioAgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<RelatorioAgendamentoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<RelatorioAgendamentoDTO>builder()
                            .success(false)
                            .message("Erro interno ao gerar relatório de agendamentos: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== NOTIFICAÇÕES (CONFIRMAÇÕES E LEMBRETES) ====================

    @PostMapping("/notificacoes")
    @Operation(summary = "Gerar relatório de notificações")
    public ResponseEntity<ResponseAPI<RelatorioNotificacaoDTO>> getRelatorioNotificacoes(
            @RequestBody RelatorioFiltroDTO filtro) {
        try {
            RelatorioNotificacaoDTO relatorio = notificacaoService.gerarRelatorio(filtro);

            return ResponseEntity.ok(ResponseAPI.<RelatorioNotificacaoDTO>builder()
                    .success(true)
                    .message("Relatório de notificações gerado com sucesso.")
                    .dados(relatorio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RelatorioNotificacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<RelatorioNotificacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<RelatorioNotificacaoDTO>builder()
                            .success(false)
                            .message("Erro interno ao gerar relatório de notificações: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== COBRANÇAS E INADIMPLÊNCIA ====================

    @PostMapping("/cobrancas")
    @Operation(summary = "Gerar relatório de cobranças")
    public ResponseEntity<ResponseAPI<RelatorioCobrancaDTO>> getRelatorioCobrancas(
            @RequestBody RelatorioFiltroDTO filtro) {
        try {
            RelatorioCobrancaDTO relatorio = cobrancaService.gerarRelatorio(filtro);

            return ResponseEntity.ok(ResponseAPI.<RelatorioCobrancaDTO>builder()
                    .success(true)
                    .message("Relatório de cobranças gerado com sucesso.")
                    .dados(relatorio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RelatorioCobrancaDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<RelatorioCobrancaDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<RelatorioCobrancaDTO>builder()
                            .success(false)
                            .message("Erro interno ao gerar relatório de cobranças: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== FUNCIONÁRIOS (PRODUTIVIDADE, OCUPAÇÃO, COMISSÕES) ====================

    @PostMapping("/funcionarios")
    @Operation(summary = "Gerar relatório de funcionários")
    public ResponseEntity<ResponseAPI<RelatorioFuncionarioDTO>> getRelatorioFuncionarios(
            @RequestBody RelatorioFiltroDTO filtro) {
        try {
            RelatorioFuncionarioDTO relatorio = funcionarioService.gerarRelatorio(filtro);

            return ResponseEntity.ok(ResponseAPI.<RelatorioFuncionarioDTO>builder()
                    .success(true)
                    .message("Relatório de funcionários gerado com sucesso.")
                    .dados(relatorio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RelatorioFuncionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<RelatorioFuncionarioDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<RelatorioFuncionarioDTO>builder()
                            .success(false)
                            .message("Erro interno ao gerar relatório de funcionários: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== CLIENTES (CADASTROS, FREQUÊNCIA, LTV) ====================

    @PostMapping("/clientes")
    @Operation(summary = "Gerar relatório de clientes")
    public ResponseEntity<ResponseAPI<RelatorioClienteDTO>> getRelatorioClientes(
            @RequestBody RelatorioFiltroDTO filtro) {
        try {
            RelatorioClienteDTO relatorio = clienteService.gerarRelatorio(filtro);

            return ResponseEntity.ok(ResponseAPI.<RelatorioClienteDTO>builder()
                    .success(true)
                    .message("Relatório de clientes gerado com sucesso.")
                    .dados(relatorio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RelatorioClienteDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<RelatorioClienteDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<RelatorioClienteDTO>builder()
                            .success(false)
                            .message("Erro interno ao gerar relatório de clientes: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    // ==================== SERVIÇOS (RANKING, CATEGORIAS) ====================

    @PostMapping("/servicos")
    @Operation(summary = "Gerar relatório de serviços")
    public ResponseEntity<ResponseAPI<RelatorioServicoDTO>> getRelatorioServicos(
            @RequestBody RelatorioFiltroDTO filtro) {
        try {
            RelatorioServicoDTO relatorio = servicoService.gerarRelatorio(filtro);

            return ResponseEntity.ok(ResponseAPI.<RelatorioServicoDTO>builder()
                    .success(true)
                    .message("Relatório de serviços gerado com sucesso.")
                    .dados(relatorio)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<RelatorioServicoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseAPI.<RelatorioServicoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(403)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<RelatorioServicoDTO>builder()
                            .success(false)
                            .message("Erro interno ao gerar relatório de serviços: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
