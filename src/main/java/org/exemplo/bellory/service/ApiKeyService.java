package org.exemplo.bellory.service;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.ApiKeyUserInfo;
import org.exemplo.bellory.model.entity.config.ApiKey;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.users.Admin;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.entity.users.User;
import org.exemplo.bellory.model.repository.ApiKeyRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.users.AdminRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final AdminRepository adminRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;

    private static final String API_KEY_PREFIX = "bly_"; // bellory prefix
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Gera uma nova API Key a partir de qualquer tipo de usuário
     */
    @Transactional
    public Map<String, Object> generateApiKey(
            Long userId,
            ApiKey.UserType userType,
            String name,
            String description,
            LocalDateTime expiresAt) {

        // Busca informações do usuário baseado no tipo
        ApiKeyUserInfo userInfo = getUserInfo(userId, userType);

        if (userInfo == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }

        // Gera chave aleatória
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawKey = API_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Cria hash da chave
        String keyHash = hashApiKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .userId(userId)
                .userType(userType)
                .username(userInfo.getUsername())
                .organizacao(userInfo.getOrganizacao())
                .keyHash(keyHash)
                .name(name)
                .description(description)
                .expiresAt(expiresAt)
                .ativo(true)
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);

        return Map.of(
                "apiKey", rawKey,  // IMPORTANTE: Retornar apenas uma vez!
                "entity", saved
        );
    }

    /**
     * Valida API Key e retorna informações do usuário
     */
    @Transactional
    public ApiKeyUserInfo validateApiKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(API_KEY_PREFIX)) {
            return null;
        }

        String keyHash = hashApiKey(rawKey);
        ApiKey apiKey = apiKeyRepository.findByKeyHashAndAtivoTrue(keyHash)
                .orElse(null);

        if (apiKey == null || apiKey.isExpired()) {
            return null;
        }

        // Atualiza último uso
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        // Retorna informações completas do usuário
        return getUserInfo(apiKey.getUserId(), apiKey.getUserType());
    }

    /**
     * Busca informações do usuário baseado no tipo
     */
    private ApiKeyUserInfo getUserInfo(Long userId, ApiKey.UserType userType) {
        switch (userType) {
            case ADMIN:
                return adminRepository.findById(userId)
                        .map(this::mapAdminToUserInfo)
                        .orElse(null);

            case FUNCIONARIO:
                return funcionarioRepository.findById(userId)
                        .map(this::mapFuncionarioToUserInfo)
                        .orElse(null);

            case CLIENTE:
                return clienteRepository.findById(userId)
                        .map(this::mapClienteToUserInfo)
                        .orElse(null);

            default:
                return null;
        }
    }

    private ApiKeyUserInfo mapAdminToUserInfo(Admin admin) {
        return ApiKeyUserInfo.builder()
                .userId(admin.getId())
                .username(admin.getUsername())
                .nomeCompleto(admin.getNomeCompleto())
                .email(admin.getEmail())
                .role(admin.getRole())
                .userType(ApiKey.UserType.ADMIN)
                .organizacaoId(admin.getOrganizacao().getId())
                .organizacao(admin.getOrganizacao())
                .build();
    }

    private ApiKeyUserInfo mapFuncionarioToUserInfo(Funcionario funcionario) {
        return ApiKeyUserInfo.builder()
                .userId(funcionario.getId())
                .username(funcionario.getUsername())
                .nomeCompleto(funcionario.getNomeCompleto())
                .email(funcionario.getEmail())
                .role(funcionario.getRole())
                .userType(ApiKey.UserType.FUNCIONARIO)
                .organizacaoId(funcionario.getOrganizacao().getId())
                .organizacao(funcionario.getOrganizacao())
                .build();
    }

    private ApiKeyUserInfo mapClienteToUserInfo(Cliente cliente) {
        return ApiKeyUserInfo.builder()
                .userId(cliente.getId())
                .username(cliente.getUsername())
                .nomeCompleto(cliente.getNomeCompleto())
                .email(cliente.getEmail())
                .role(cliente.getRole())
                .userType(ApiKey.UserType.CLIENTE)
                .organizacaoId(cliente.getOrganizacao().getId())
                .organizacao(cliente.getOrganizacao())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listUserApiKeys(Long userId, ApiKey.UserType userType) {
        return apiKeyRepository.findByUserIdAndUserTypeAndAtivoTrue(userId, userType);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listApiKeysByOrganizacao(Long organizacaoId) {
        return apiKeyRepository.findByOrganizacaoIdAndAtivoTrue(organizacaoId);
    }


    @Transactional
    public void revokeApiKey(Long apiKeyId, Long userId, ApiKey.UserType userType) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API Key não encontrada"));

        if (!apiKey.getUserId().equals(userId) || !apiKey.getUserType().equals(userType)) {
            throw new IllegalArgumentException("Sem permissão para revogar esta API Key");
        }

        apiKey.setAtivo(false);
        apiKeyRepository.save(apiKey);
    }

    /**
     * Gera hash SHA-256 da API Key
     */
    private String hashApiKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash da API Key", e);
        }
    }
}