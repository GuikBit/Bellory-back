package org.exemplo.bellory.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.admin.*;
import org.exemplo.bellory.service.admin.AdminMetricasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/metricas")
@RequiredArgsConstructor
@Tag(name = "Admin - Metricas", description = "Endpoints de metricas detalhadas por dominio: agendamentos, faturamento, servicos, funcionarios, clientes, instancias e planos")
public class AdminMetricasController {

    private final AdminMetricasService adminMetricasService;

    @Operation(summary = "Metricas de agendamentos", description = "Totais, taxas de conclusao/cancelamento/no-show, agendamentos por organizacao e evolucao mensal dos ultimos 12 meses")
    @GetMapping("/agendamentos")
    public ResponseEntity<AdminAgendamentoMetricasDTO> getMetricasAgendamentos() {
        return ResponseEntity.ok(adminMetricasService.getMetricasAgendamentos());
    }

    @Operation(summary = "Metricas de faturamento", description = "Faturamento total e mensal, crescimento percentual, ticket medio, faturamento por organizacao e evolucao mensal")
    @GetMapping("/faturamento")
    public ResponseEntity<AdminFaturamentoMetricasDTO> getMetricasFaturamento() {
        return ResponseEntity.ok(adminMetricasService.getMetricasFaturamento());
    }

    @Operation(summary = "Metricas de servicos", description = "Total de servicos ativos/inativos, preco medio, servicos por organizacao e mais agendados")
    @GetMapping("/servicos")
    public ResponseEntity<AdminServicoMetricasDTO> getMetricasServicos() {
        return ResponseEntity.ok(adminMetricasService.getMetricasServicos());
    }

    @Operation(summary = "Metricas de funcionarios", description = "Total de funcionarios ativos/inativos, media por organizacao e detalhamento por organizacao")
    @GetMapping("/funcionarios")
    public ResponseEntity<AdminFuncionarioMetricasDTO> getMetricasFuncionarios() {
        return ResponseEntity.ok(adminMetricasService.getMetricasFuncionarios());
    }

    @Operation(summary = "Metricas de clientes", description = "Total de clientes ativos/inativos, media por organizacao, clientes por organizacao e evolucao mensal")
    @GetMapping("/clientes")
    public ResponseEntity<AdminClienteMetricasDTO> getMetricasClientes() {
        return ResponseEntity.ok(adminMetricasService.getMetricasClientes());
    }

    @Operation(summary = "Metricas de instancias WhatsApp", description = "Total de instancias ativas/deletadas/conectadas, instancias por organizacao e detalhes de todas as instancias")
    @GetMapping("/instancias")
    public ResponseEntity<AdminInstanciaMetricasDTO> getMetricasInstancias() {
        return ResponseEntity.ok(adminMetricasService.getMetricasInstancias());
    }

    @Operation(summary = "Metricas de planos", description = "Distribuicao de organizacoes por plano, percentual de adesao e detalhes de cada plano")
    @GetMapping("/planos")
    public ResponseEntity<AdminPlanoMetricasDTO> getMetricasPlanos() {
        return ResponseEntity.ok(adminMetricasService.getMetricasPlanos());
    }
}
