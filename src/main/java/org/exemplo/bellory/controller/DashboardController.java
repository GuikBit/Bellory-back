package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.dto.dashboard.DashboardDTO;
import org.exemplo.bellory.model.dto.dashboard.DashboardFiltroDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/geral")
    public ResponseEntity<ResponseAPI<DashboardDTO>> getDashboardGeral(@RequestBody DashboardFiltroDTO filtro) {
        try {
            DashboardDTO dashboard = dashboardService.getDashboardGeral(filtro);

            return ResponseEntity.ok(ResponseAPI.<DashboardDTO>builder()
                    .success(true)
                    .message("Dashboard recuperado com sucesso.")
                    .dados(dashboard)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<DashboardDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<DashboardDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno ao buscar o dashboard: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/resumo-hoje")
    public ResponseEntity<ResponseAPI<DashboardDTO>> getDashboardHoje() {
        try {
            DashboardFiltroDTO filtroHoje = DashboardFiltroDTO.builder()
                    .dataInicio(LocalDate.now())
                    .dataFim(LocalDate.now())
                    .build();

            DashboardDTO dashboard = dashboardService.getDashboardGeral(filtroHoje);

            return ResponseEntity.ok(ResponseAPI.<DashboardDTO>builder()
                    .success(true)
                    .message("Resumo de hoje recuperado com sucesso.")
                    .dados(dashboard)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<DashboardDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/resumo-mes")
    public ResponseEntity<ResponseAPI<DashboardDTO>> getDashboardMesAtual() {
        try {
            LocalDate hoje = LocalDate.now();
            DashboardFiltroDTO filtroMes = DashboardFiltroDTO.builder()
                    .dataInicio(hoje.withDayOfMonth(1))
                    .dataFim(hoje.withDayOfMonth(hoje.lengthOfMonth()))
                    .build();

            DashboardDTO dashboard = dashboardService.getDashboardGeral(filtroMes);

            return ResponseEntity.ok(ResponseAPI.<DashboardDTO>builder()
                    .success(true)
                    .message("Resumo do mês atual recuperado com sucesso.")
                    .dados(dashboard)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<DashboardDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/resumo-ano/{ano}")
    public ResponseEntity<ResponseAPI<DashboardDTO>> getDashboardAno(@PathVariable int ano) {
        try {
            DashboardFiltroDTO filtroAno = DashboardFiltroDTO.builder()
                    .dataInicio(LocalDate.of(ano, 1, 1))
                    .dataFim(LocalDate.of(ano, 12, 31))
                    .build();

            DashboardDTO dashboard = dashboardService.getDashboardGeral(filtroAno);

            return ResponseEntity.ok(ResponseAPI.<DashboardDTO>builder()
                    .success(true)
                    .message("Resumo do ano " + ano + " recuperado com sucesso.")
                    .dados(dashboard)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<DashboardDTO>builder()
                            .success(false)
                            .message("Ocorreu um erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/comparativo")
    public ResponseEntity<ResponseAPI<Object>> getDashboardComparativo(
            @RequestParam String dataInicioAtual,
            @RequestParam String dataFimAtual,
            @RequestParam String dataInicioAnterior,
            @RequestParam String dataFimAnterior) {
        try {
            LocalDate inicioAtual = LocalDate.parse(dataInicioAtual);
            LocalDate fimAtual = LocalDate.parse(dataFimAtual);
            LocalDate inicioAnterior = LocalDate.parse(dataInicioAnterior);
            LocalDate fimAnterior = LocalDate.parse(dataFimAnterior);

            Object comparativo = dashboardService.getDashboardComparativo(
                    inicioAtual, fimAtual, inicioAnterior, fimAnterior);

            return ResponseEntity.ok(ResponseAPI.<Object>builder()
                    .success(true)
                    .message("Comparativo recuperado com sucesso.")
                    .dados(comparativo)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Object>builder()
                            .success(false)
                            .message("Formato de data inválido. Use o formato: yyyy-MM-dd")
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Object>builder()
                            .success(false)
                            .message("Ocorreu um erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }

    @GetMapping("/metricas-funcionario/{funcionarioId}")
    public ResponseEntity<ResponseAPI<Object>> getMetricasFuncionario(
            @PathVariable Long funcionarioId,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim) {
        try {
            LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().withDayOfMonth(1);
            LocalDate fim = dataFim != null ? LocalDate.parse(dataFim) : LocalDate.now();

            Object metricas = dashboardService.getMetricasFuncionario(funcionarioId, inicio, fim);

            return ResponseEntity.ok(ResponseAPI.<Object>builder()
                    .success(true)
                    .message("Métricas do funcionário recuperadas com sucesso.")
                    .dados(metricas)
                    .build());

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Object>builder()
                            .success(false)
                            .message("Formato de data inválido. Use o formato: yyyy-MM-dd")
                            .errorCode(400)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Object>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<Object>builder()
                            .success(false)
                            .message("Ocorreu um erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
