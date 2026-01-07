package org.exemplo.bellory.model.dto.instancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.instancia.KnowledgeBase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO customizado para endpoint by-name
 * Estrutura simplificada e aninhada conforme solicitado
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceByNameDTO {

    private InstanceData instancia;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanceData {
        private Long id;
        private String instanceId;
        private String instanceName;
        private String integration;
        private String personality;
        private String description;
        private String phoneNumber;
        private String profilePictureUrl;
        private String profileName;
        private Long organizacaoId;
        private String organizacaoNome;
        private ToolsData tools;
        private WebhookSettingsData webhookSettings;
        private SettingsData settings;
        private List<KnowledgeBaseData> knowledgeBase;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolsData {
        private Long id;
        private Boolean getServices;
        private Boolean getProfessional;
        private Boolean getProducts;
        private Boolean getAvaliableSchedules;
        private Boolean postScheduling;
        private Boolean sendTextMessage;
        private Boolean sendMediaMessage;
        private Boolean postConfirmations;
        private Boolean postCancellations;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookSettingsData {
        private String url;
        private Boolean enabled;
        private List<String> events;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsData {
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
    public static class KnowledgeBaseData {
        private Long id;
        private String title;
        private String type;
        private String content;
        private String fileName;
        private Long fileSize;
        private String fileType;
        private String fileUrl;
    }

    /**
     * Construtor que converte Instance para InstanceByNameDTO
     */
    public InstanceByNameDTO(Instance instance) {
        this.instancia = new InstanceData();

        // Dados básicos da instância
        this.instancia.setId(instance.getId());
        this.instancia.setInstanceId(instance.getInstanceId());
        this.instancia.setInstanceName(instance.getInstanceName());
        this.instancia.setIntegration(instance.getIntegration());
        this.instancia.setPersonality(instance.getPersonality());
        this.instancia.setDescription(instance.getDescription());

        // Dados da organização
        if (instance.getOrganizacao() != null) {
            this.instancia.setOrganizacaoId(instance.getOrganizacao().getId());
            this.instancia.setOrganizacaoNome(instance.getOrganizacao().getNomeFantasia());
        }

        // Tools
        if (instance.getTools() != null) {
            ToolsData tools = new ToolsData();
            tools.setId(instance.getTools().getId());
            tools.setGetServices(instance.getTools().isGetServices());
            tools.setGetProfessional(instance.getTools().isGetProfessional());
            tools.setGetProducts(instance.getTools().isGetProducts());
            tools.setGetAvaliableSchedules(instance.getTools().isGetAvaliableSchedules());
            tools.setPostScheduling(instance.getTools().isPostScheduling());
            tools.setSendTextMessage(instance.getTools().isSendTextMessage());
            tools.setSendMediaMessage(instance.getTools().isSendMediaMessage());
            tools.setPostConfirmations(instance.getTools().isPostConfirmations());
            tools.setPostCancellations(instance.getTools().isPostCancellations());
            this.instancia.setTools(tools);
        }

        // Webhook Settings
        if (instance.getWebhookConfig() != null) {
            WebhookSettingsData webhook = new WebhookSettingsData();
            webhook.setUrl(instance.getWebhookConfig().getUrl());
            webhook.setEnabled(instance.getWebhookConfig().getEnabled());
            webhook.setEvents(instance.getWebhookConfig().getEvents());
            this.instancia.setWebhookSettings(webhook);
        }

        // Settings
        if (instance.getSettings() != null) {
            SettingsData settings = new SettingsData();
            settings.setRejectCall(instance.getSettings().getRejectCall());
            settings.setMsgCall(instance.getSettings().getMsgCall());
            settings.setGroupsIgnore(instance.getSettings().getGroupsIgnore());
            settings.setAlwaysOnline(instance.getSettings().getAlwaysOnline());
            settings.setReadMessages(instance.getSettings().getReadMessages());
            settings.setReadStatus(instance.getSettings().getReadStatus());
            this.instancia.setSettings(settings);
        }

        // Knowledge Base
        if (instance.getKnowledgeBase() != null) {
            this.instancia.setKnowledgeBase(
                    instance.getKnowledgeBase().stream()
                            .map(kb -> {
                                KnowledgeBaseData kbData = new KnowledgeBaseData();
                                kbData.setId(kb.getId());
                                kbData.setTitle(kb.getTitle());
                                kbData.setType(kb.getType().name());
                                kbData.setContent(kb.getContent());
                                kbData.setFileName(kb.getFileName());
                                kbData.setFileSize(kb.getFileSize());
                                kbData.setFileType(kb.getFileType());
                                kbData.setFileUrl(kb.getFileUrl());
                                return kbData;
                            })
                            .collect(Collectors.toList())
            );
        }
    }

    /**
     * Método para atualizar dados do Evolution API
     */
    public void updateFromEvolutionData(String phoneNumber, String profileName, String profilePictureUrl) {
        if (this.instancia != null) {
            this.instancia.setPhoneNumber(phoneNumber);
            this.instancia.setProfileName(profileName);
            this.instancia.setProfilePictureUrl(profilePictureUrl);
        }
    }
}