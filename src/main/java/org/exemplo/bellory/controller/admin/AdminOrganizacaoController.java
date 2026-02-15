package org.exemplo.bellory.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.admin.AdminOrganizacaoDetalheDTO;
import org.exemplo.bellory.model.dto.admin.AdminOrganizacaoListDTO;
import org.exemplo.bellory.service.admin.AdminOrganizacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/organizacoes")
@RequiredArgsConstructor
@Tag(name = "Admin - Organizacoes", description = "Gestao e visualizacao de todas as organizacoes/clientes da plataforma")
public class AdminOrganizacaoController {

    private final AdminOrganizacaoService adminOrganizacaoService;

    @Operation(summary = "Listar todas as organizacoes", description = "Retorna lista de todas as organizacoes com contadores resumidos de agendamentos, clientes, funcionarios, servicos e instancias")
    @GetMapping
    public ResponseEntity<List<AdminOrganizacaoListDTO>> listarOrganizacoes() {
        List<AdminOrganizacaoListDTO> organizacoes = adminOrganizacaoService.listarOrganizacoes();
        return ResponseEntity.ok(organizacoes);
    }

    @Operation(summary = "Detalhar organizacao", description = "Retorna detalhes completos de uma organizacao incluindo metricas, plano, limites e instancias")
    @GetMapping("/{id}")
    public ResponseEntity<AdminOrganizacaoDetalheDTO> detalharOrganizacao(
            @Parameter(description = "ID da organizacao") @PathVariable Long id) {
        AdminOrganizacaoDetalheDTO detalhe = adminOrganizacaoService.detalharOrganizacao(id);
        return ResponseEntity.ok(detalhe);
    }
}
