package org.exemplo.bellory.model.dto.instancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.instancia.InstanceStatus;
import org.exemplo.bellory.model.entity.instancia.Tools;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * DTO para retornar dados da instância
 */
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class InstanceDTO {
//
//    // Campos locais da aplicação
//    private Long id;
//    private String instanceId;
//    private String instanceName;
//    private String integration;
//    private String personality;
//    private String description;
//    private String qrcode;
//    private InstanceStatus status;
//    private String phoneNumber;
//    private String profilePictureUrl;
//    private String profileName;
//    private String webhookUrl;
//    private Boolean webhookEnabled;
//    private List<String> webhookEvents;
//    private Boolean rejectCall;
//    private String msgCall;
//    private Boolean groupsIgnore;
//    private Boolean alwaysOnline;
//    private Boolean readMessages;
//    private Boolean readStatus;
//    private Long organizacaoId;
//    private String organizacaoNome;
//    private Boolean isActive;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//    private Tools tools;
//
//    // Campos adicionais da Evolution API
//    private String token;
//    private String clientName;
//    private String businessId;
//    private LocalDateTime disconnectionAt;
//    private Integer messageCount;
//    private Integer contactCount;
//    private Integer chatCount;
//
//    /**
//     * Construtor que converte Entity para DTO
//     */
//    public InstanceDTO(Instance instance) {
//        this.id = instance.getId();
//        this.instanceName = instance.getInstanceName();
//        this.instanceId = instance.getInstanceId();
//        this.integration = instance.getIntegration();
//        this.personality = instance.getPersonality();
//        this.description = instance.getDescription();
//        this.organizacaoNome = instance.getOrganizacao().getNomeFantasia();
//        this.organizacaoId = instance.getOrganizacao().getId();
//        this.tools = instance.getTools();
//
//        // Inicializar com dados do webhookConfig se existir
//        if (instance.getWebhookConfig() != null) {
//            this.webhookUrl = instance.getWebhookConfig().getUrl();
//            this.webhookEnabled = instance.getWebhookConfig().getEnabled();
//            this.webhookEvents = instance.getWebhookConfig().getEvents();
//        }
//    }
//
//
//}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceDTO {

    // Campos locais da aplicação
    private Long id;
    private String instanceId;
    private String instanceName;
    private String integration;
    private String personality;
    private String description;
    private String qrcode;
    private InstanceStatus status;
    private String phoneNumber;
    private String profilePictureUrl;
    private String profileName;

    private Long organizacaoId;
    private String organizacaoNome;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String token;
    private String clientName;
    private String businessId;
    private LocalDateTime disconnectionAt;
    private Integer messageCount;
    private Integer contactCount;
    private Integer chatCount;

    private Tools tools;
    private WebhookSettings webhookSettings;
    private Settings settings;

    public InstanceDTO(Instance instance) {
        this.id = instance.getId();
        this.instanceName = instance.getInstanceName();
        this.instanceId = instance.getInstanceId();
        this.integration = instance.getIntegration();
        this.personality = instance.getPersonality();
        this.description = instance.getDescription();

        if (instance.getOrganizacao() != null) {
            this.organizacaoNome = instance.getOrganizacao().getNomeFantasia();
            this.organizacaoId = instance.getOrganizacao().getId();
        }

        this.tools = instance.getTools();

        // --- Correção da Lógica do Webhook ---
        // Verifica se a configuração existe na entidade antes de mapear
        if (instance.getWebhookConfig() != null) {
            this.webhookSettings = new WebhookSettings();
            this.webhookSettings.setUrl(instance.getWebhookConfig().getUrl());
            this.webhookSettings.setEnabled(instance.getWebhookConfig().getEnabled());
            this.webhookSettings.setEvents(instance.getWebhookConfig().getEvents());
        }

        // --- Mapeamento de Settings (Opcional, assumindo que exista na entidade) ---
        // Exemplo de como mapear se existir na entidade Instance:
        /*
        if (instance.getSettings() != null) {
            this.settings = new Settings();
            this.settings.setRejectCall(instance.getSettings().getRejectCall());
            // ... outros campos
        }
        */
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookSettings {
        private String url;
        private boolean enabled;
        private List<String> events;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Settings {
        private Boolean rejectCall;
        private String msgCall;
        private Boolean groupsIgnore;
        private Boolean alwaysOnline;
        private Boolean readMessages;
        private Boolean readStatus;
    }
}