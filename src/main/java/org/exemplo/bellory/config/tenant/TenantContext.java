package org.exemplo.bellory.config.tenant;

/**
 * Classe utilitária para gerenciar o contexto do tenant atual usando ThreadLocal.
 * Permite que qualquer parte da aplicação acesse o tenant_id da requisição atual.
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Define o tenant atual para a thread atual.
     * @param tenantId O ID do tenant (subdomínio)
     */
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Obtém o tenant atual da thread atual.
     * @return O ID do tenant atual ou null se não estiver definido
     */
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Remove o tenant da thread atual.
     * Importante chamar ao final da requisição para evitar vazamentos de memória.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Verifica se existe um tenant definido para a thread atual.
     * @return true se existe um tenant definido
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}
