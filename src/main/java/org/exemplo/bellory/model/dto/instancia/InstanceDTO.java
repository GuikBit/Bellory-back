package org.exemplo.bellory.model.dto.instancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.instancia.InstanceStatus;
import org.exemplo.bellory.model.entity.instancia.Tools;
import org.exemplo.bellory.model.entity.instancia.KnowledgeBase.KnowledgeType;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
    private boolean ativo;

    private Tools tools;
    private WebhookSettings webhookSettings;
    private Settings settings;
    private List<KnowledgeBaseDTO> knowledgeBase;

    public InstanceDTO(Instance instance) {
        this.id = instance.getId();
        this.instanceName = instance.getInstanceName();
        this.instanceId = instance.getInstanceId();
        this.integration = instance.getIntegration();
        this.personality = instance.getPersonality();
        this.description = instance.getDescription();
        this.ativo = instance.isAtivo();

        if (instance.getOrganizacao() != null) {
            this.organizacaoNome = instance.getOrganizacao().getNomeFantasia();
            this.organizacaoId = instance.getOrganizacao().getId();
        }

        this.tools = instance.getTools();

        if(instance.getSettings() != null){
            this.settings = new Settings();
            this.settings.alwaysOnline = instance.getSettings().getAlwaysOnline();
            this.settings.groupsIgnore = instance.getSettings().getGroupsIgnore();
            this.settings.msgCall = instance.getSettings().getMsgCall();
            this.settings.readMessages = instance.getSettings().getReadMessages();
            this.settings.rejectCall = instance.getSettings().getRejectCall();
            this.settings.readStatus = instance.getSettings().getReadStatus();
        }

        if(instance.getKnowledgeBase() != null){
            this.knowledgeBase = new ArrayList<>();
        }

        if (instance.getWebhookConfig() != null) {
            this.webhookSettings = new WebhookSettings();
            this.webhookSettings.setUrl(instance.getWebhookConfig().getUrl());
            this.webhookSettings.setEnabled(instance.getWebhookConfig().getEnabled());
            this.webhookSettings.setEvents(instance.getWebhookConfig().getEvents());
        }

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
    public static class KnowledgeBase {
        private String title;
        private String type;
        private String content;
        private String fileName;
        private Number fileSize;
        private String fileType;
        private String fileUrl;
        private String createdAt;
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class KnowledgeBaseDTO {

        private Long id;
        private String title;
        private KnowledgeType type;
        private String content;
        private String fileName;
        private Long fileSize;
        private String fileType;
        private String fileUrl;
        private LocalDateTime createdAt;
    }
}
