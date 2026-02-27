package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.plano.PlanoBelloryPublicDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.admin.AdminPlanoBelloryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/planos")
@RequiredArgsConstructor
@Tag(name = "Publico - Planos", description = "Listagem publica de planos para o site externo")
public class PublicPlanosController {

    private final AdminPlanoBelloryService service;

    @Operation(summary = "Listar planos ativos", description = "Retorna os planos ativos para exibicao no site publico. Nao requer autenticacao.")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<PlanoBelloryPublicDTO>>> listarPlanosPublicos() {
        try {
            List<PlanoBelloryPublicDTO> planos = service.listarPlanosPublicos();
            return ResponseEntity.ok(ResponseAPI.<List<PlanoBelloryPublicDTO>>builder()
                    .success(true)
                    .message("Planos listados com sucesso")
                    .dados(planos)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<PlanoBelloryPublicDTO>>builder()
                            .success(false)
                            .message("Erro ao listar planos: " + e.getMessage())
                            .build());
        }
    }
}
