package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.entity.config.ApiKey;
import org.exemplo.bellory.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Gerenciamento de chaves de API")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @Operation(summary = "Criar nova API Key")
    @PostMapping
    public ResponseEntity<?> createApiKey(@RequestBody Map<String, Object> request) {

        Long userId = TenantContext.getCurrentUserId();
        String username = TenantContext.getCurrentUsername();

        // Determina o tipo de usuário baseado na role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        ApiKey.UserType userType;
        if (role.contains("ADMIN")) {
            userType = ApiKey.UserType.ADMIN;
        } else if (role.contains("FUNCIONARIO")) {
            userType = ApiKey.UserType.FUNCIONARIO;
        } else if (role.contains("CLIENTE")) {
            userType = ApiKey.UserType.CLIENTE;
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tipo de usuário não identificado"
            ));
        }

        String name = (String) request.get("name");
        String description = (String) request.get("description");

        // Expiração opcional (em dias)
        Integer expiresInDays = (Integer) request.get("expiresInDays");
        LocalDateTime expiresAt = expiresInDays != null
                ? LocalDateTime.now().plusDays(expiresInDays)
                : null;

        Map<String, Object> result = apiKeyService.generateApiKey(
                userId, userType, name, description, expiresAt
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API Key criada com sucesso. Copie agora, não será exibida novamente!",
                "apiKey", result.get("apiKey"), // Chave completa
                "id", ((ApiKey) result.get("entity")).getId(),
                "name", name,
                "userType", userType,
                "expiresAt", expiresAt != null ? expiresAt : "Sem expiração"
        ));
    }

    @Operation(summary = "Listar API Keys da organização")
    @GetMapping
    public ResponseEntity<?> listApiKeys() {
        Long userId = TenantContext.getCurrentUserId();
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        // Determina tipo de usuário
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        ApiKey.UserType userType;
        if (role.contains("ADMIN")) {
            userType = ApiKey.UserType.ADMIN;
        } else if (role.contains("FUNCIONARIO")) {
            userType = ApiKey.UserType.FUNCIONARIO;
        } else {
            userType = ApiKey.UserType.CLIENTE;
        }

        List<ApiKey> apiKeys = apiKeyService.listApiKeysByOrganizacao(organizacaoId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "apiKeys", apiKeys.stream().map(key -> {
                    // Usar HashMap ao invés de Map.of() para permitir valores null
                    Map<String, Object> keyMap = new HashMap<>();
                    keyMap.put("id", key.getId());
                    keyMap.put("name", key.getName());
                    keyMap.put("description", key.getDescription() != null ? key.getDescription() : "");
                    keyMap.put("userType", key.getUserType().toString());
                    keyMap.put("lastUsedAt", key.getLastUsedAt()); // Pode ser null
                    keyMap.put("expiresAt", key.getExpiresAt()); // Pode ser null
                    keyMap.put("createdAt", key.getCreatedAt());
                    return keyMap;
                }).toList()
        ));
    }

    @Operation(summary = "Revogar uma API Key")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeApiKey(@PathVariable Long id) {
        Long userId = TenantContext.getCurrentUserId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        ApiKey.UserType userType;
        if (role.contains("ADMIN")) {
            userType = ApiKey.UserType.ADMIN;
        } else if (role.contains("FUNCIONARIO")) {
            userType = ApiKey.UserType.FUNCIONARIO;
        } else {
            userType = ApiKey.UserType.CLIENTE;
        }

        apiKeyService.revokeApiKey(id, userId, userType);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API Key revogada com sucesso"
        ));
    }
}