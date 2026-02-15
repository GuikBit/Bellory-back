package org.exemplo.bellory.context;

import org.exemplo.bellory.model.entity.config.ConfigSistema;

/**
 * Context holder para armazenar informações do tenant (organização)
 * da requisição atual usando ThreadLocal.
 *
 * Garante que cada thread tenha seu próprio contexto isolado.
 */
public class TenantContext {

    private static final ThreadLocal<Long> currentOrganizacaoId = new ThreadLocal<>();
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUsername = new ThreadLocal<>();
    private static final ThreadLocal<String> currentRole = new ThreadLocal<>();
    private static final ThreadLocal<ConfigSistema> currentConfigSistema = new ThreadLocal<>();

    /**
     * Define o ID da organização para a requisição atual
     */
    public static void setCurrentOrganizacaoId(Long organizacaoId) {
        currentOrganizacaoId.set(organizacaoId);
    }

    /**
     * Retorna o ID da organização da requisição atual
     */
    public static Long getCurrentOrganizacaoId() {
        return currentOrganizacaoId.get();
    }

    /**
     * Define o ID do usuário para a requisição atual
     */
    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }

    /**
     * Retorna o ID do usuário da requisição atual
     */
    public static Long getCurrentUserId() {
        return currentUserId.get();
    }

    /**
     * Define o username para a requisição atual
     */
    public static void setCurrentUsername(String username) {
        currentUsername.set(username);
    }

    /**
     * Retorna o username da requisição atual
     */
    public static String getCurrentUsername() {
        return currentUsername.get();
    }

    /**
     * Define a role para a requisição atual
     */
    public static void setCurrentRole(String role) {
        currentRole.set(role);
    }

    /**
     * Retorna a role da requisição atual
     */
    public static String getCurrentRole() {
        return currentRole.get();
    }

    /**
     * Define a ConfigSistema para a requisição atual
     */
    public static void setCurrentConfigSistema(ConfigSistema configSistema) {
        currentConfigSistema.set(configSistema);
    }

    /**
     * Retorna a ConfigSistema da requisição atual
     */
    public static ConfigSistema getCurrentConfigSistema() {
        return currentConfigSistema.get();
    }

    /**
     * Limpa todas as informações do contexto
     * IMPORTANTE: Deve ser chamado após cada requisição
     */
    public static void clear() {
        currentOrganizacaoId.remove();
        currentUserId.remove();
        currentUsername.remove();
        currentRole.remove();
        currentConfigSistema.remove();
    }

    /**
     * Define todas as informações do contexto de uma vez
     */
    public static void setContext(Long organizacaoId, Long userId, String username, String role) {
        setCurrentOrganizacaoId(organizacaoId);
        setCurrentUserId(userId);
        setCurrentUsername(username);
        setCurrentRole(role);
    }
}