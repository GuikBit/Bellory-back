package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.cupom.CupomValidacaoResponseDTO;
import org.exemplo.bellory.model.dto.cupom.CupomValidacaoResult;
import org.exemplo.bellory.model.dto.cupom.ValidarCupomDTO;
import org.exemplo.bellory.model.dto.plano.PlanoBelloryPublicDTO;
import org.exemplo.bellory.model.entity.assinatura.CicloCobranca;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.repository.organizacao.PlanoBelloryRepository;
import org.exemplo.bellory.service.admin.AdminPlanoBelloryService;
import org.exemplo.bellory.service.assinatura.CupomDescontoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/planos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Público - Planos", description = "Listagem pública de planos e validação de cupons para o site externo")
public class PublicPlanosController {

    private final AdminPlanoBelloryService service;
    private final CupomDescontoService cupomDescontoService;
    private final PlanoBelloryRepository planoBelloryRepository;

    @Operation(summary = "Listar planos ativos", description = "Retorna os planos ativos para exibição no site público. Não requer autenticação.")
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

    @Operation(summary = "Validar cupom de desconto (publico)", description = "Valida um cupom de desconto durante o cadastro da organização. Não requer autenticação.")
    @PostMapping("/validar-cupom")
    public ResponseEntity<ResponseAPI<CupomValidacaoResponseDTO>> validarCupomPublico(
            @RequestBody @Valid ValidarCupomDTO dto) {
        try {
            PlanoBellory plano = planoBelloryRepository.findByCodigo(dto.getPlanoCodigo())
                    .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + dto.getPlanoCodigo()));

            CicloCobranca ciclo = CicloCobranca.valueOf(dto.getCicloCobranca());
            BigDecimal valorOriginal = ciclo == CicloCobranca.ANUAL ? plano.getPrecoAnual() : plano.getPrecoMensal();

            CupomValidacaoResult result = cupomDescontoService.validarCupomPublico(
                    dto.getCodigoCupom(), dto.getPlanoCodigo(), dto.getCicloCobranca(), valorOriginal);

            CupomValidacaoResponseDTO response = CupomValidacaoResponseDTO.builder()
                    .valido(result.isValido())
                    .mensagem(result.getMensagem())
                    .tipoDesconto(result.getCupom() != null ? result.getCupom().getTipoDesconto().name() : null)
                    .tipoAplicacao(result.getCupom() != null ? result.getCupom().getTipoAplicacao().name() : null)
                    .percentualDesconto(result.getCupom() != null ? result.getCupom().getValorDesconto() : null)
                    .valorDesconto(result.getValorDesconto())
                    .valorOriginal(result.getValorOriginal())
                    .valorComDesconto(result.getValorComDesconto())
                    .build();

            return ResponseEntity.ok(ResponseAPI.<CupomValidacaoResponseDTO>builder()
                    .success(true)
                    .dados(response)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<CupomValidacaoResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(400)
                            .build());
        } catch (Exception e) {
            log.error("Erro ao validar cupom público: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<CupomValidacaoResponseDTO>builder()
                            .success(false)
                            .message("Erro interno: " + e.getMessage())
                            .errorCode(500)
                            .build());
        }
    }
}
