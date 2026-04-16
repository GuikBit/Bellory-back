package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.assinatura.AssinaturaStatusDTO;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints relacionados ao cache de status da assinatura consumido via Payment API.
 *
 * Ponto chave: o cache (Redis, fresh 5min + stale 24h) so e invalidado explicitamente.
 * O frontend deve chamar POST /refresh-cache apos qualquer acao feita diretamente na
 * Payment API (troca de plano, pagamento confirmado, etc.) para que o Bellory enxergue
 * o novo estado imediatamente.
 */
@RestController
@RequestMapping("/api/v1/assinatura")
@CrossOrigin(origins = {"https://bellory.vercel.app", "https://*.vercel.app", "http://localhost:*"})
@Tag(name = "Assinatura", description = "Operacoes de assinatura / cache Payment API")
@Slf4j
public class AssinaturaCacheController {

    private final AssinaturaCacheService assinaturaCacheService;

    public AssinaturaCacheController(AssinaturaCacheService assinaturaCacheService) {
        this.assinaturaCacheService = assinaturaCacheService;
    }

    @Operation(summary = "Invalida o cache e refaz o fetch do status na Payment API")
    @PostMapping("/refresh-cache")
    public ResponseEntity<AssinaturaStatusDTO> refreshCache() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AssinaturaStatusDTO status = assinaturaCacheService.refreshByOrganizacao(organizacaoId);
        return ResponseEntity.ok(status);
    }
}
