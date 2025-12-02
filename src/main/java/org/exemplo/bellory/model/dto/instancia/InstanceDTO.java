package org.exemplo.bellory.model.dto.instancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.instancia.InstanceStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * DTO para retornar dados da instância
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceDTO {

    private Long id;
    private String instanceName;
    private String qrcode;
    private InstanceStatus status;
    private String phoneNumber;
    private String profilePictureUrl;
    private String profileName;
    private String webhookUrl;
    private Boolean webhookEnabled;
    private List<String> webhookEvents;
    private Boolean rejectCall;
    private String msgCall;
    private Boolean groupsIgnore;
    private Boolean alwaysOnline;
    private Boolean readMessages;
    private Boolean readStatus;
    private Long organizacaoId;
    private String organizacaoNome;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Construtor que converte Entity para DTO
     */
    public InstanceDTO(Instance instance) {
        this.id = instance.getId();
        this.instanceName = instance.getInstanceName();
        this.qrcode = instance.getQrcode();
        this.status = instance.getStatus();
        this.phoneNumber = instance.getPhoneNumber();
        this.profilePictureUrl = instance.getProfilePictureUrl();
        this.profileName = instance.getProfileName();
        this.webhookUrl = instance.getWebhookUrl();
        this.webhookEnabled = instance.getWebhookEnabled();

        // Converter string JSON para lista
        if (instance.getWebhookEvents() != null && !instance.getWebhookEvents().isEmpty()) {
            this.webhookEvents = Arrays.asList(
                    instance.getWebhookEvents()
                            .replace("[", "")
                            .replace("]", "")
                            .replace("\"", "")
                            .split(",")
            );
        }

        this.rejectCall = instance.getRejectCall();
        this.msgCall = instance.getMsgCall();
        this.groupsIgnore = instance.getGroupsIgnore();
        this.alwaysOnline = instance.getAlwaysOnline();
        this.readMessages = instance.getReadMessages();
        this.readStatus = instance.getReadStatus();
        this.organizacaoId = instance.getOrganizacao().getId();
        this.organizacaoNome = instance.getOrganizacao().getNomeFantasia();
        this.isActive = instance.getIsActive();
        this.description = instance.getDescription();
        this.createdAt = instance.getCreatedAt();
        this.updatedAt = instance.getUpdatedAt();
    }
}


