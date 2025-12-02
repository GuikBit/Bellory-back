package org.exemplo.bellory.model.entity.instancia;

public enum InstanceStatus {
    /**
     * Instância desconectada
     */
    DISCONNECTED,

    /**
     * Instância em processo de conexão
     */
    CONNECTING,

    /**
     * Instância conectada
     */
    CONNECTED,

    /**
     * Instância aberta e pronta para uso
     */
    OPEN
}
