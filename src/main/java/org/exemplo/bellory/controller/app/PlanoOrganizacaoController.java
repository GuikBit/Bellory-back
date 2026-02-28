package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.plano.PlanoOrganizacaoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.admin.AdminPlanoBelloryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planos")
@RequiredArgsConstructor
@Tag(name = "Planos - Organizacao", description = "Planos disponiveis e plano atual da organizacao autenticada")
public class PlanoOrganizacaoController {

    private final AdminPlanoBelloryService service;

    @Operation(summary = "Listar planos disponiveis e plano atual", description = "Retorna o plano atual da organizacao e a lista de planos disponiveis. Requer autenticacao JWT.")
    @GetMapping
    public ResponseEntity<ResponseAPI<PlanoOrganizacaoDTO>> listarPlanosOrganizacao() {
        try {
            PlanoOrganizacaoDTO resultado = service.listarPlanosOrganizacao();
            return ResponseEntity.ok(ResponseAPI.<PlanoOrganizacaoDTO>builder()
                    .success(true)
                    .message("Planos da organizacao listados com sucesso")
                    .dados(resultado)
                    .build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseAPI.<PlanoOrganizacaoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<PlanoOrganizacaoDTO>builder()
                            .success(false)
                            .message("Erro ao listar planos: " + e.getMessage())
                            .build());
        }
    }
}
