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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceDTO {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Tools tools;
    /**
     * Construtor que converte Entity para DTO
     */
    public InstanceDTO(Instance instance) {
        this.id = instance.getId();
        this.instanceName = instance.getInstanceName();
        this.instanceId = instance.getInstanceId();
        this.integration = instance.getIntegration();
        this.personality = instance.getPersonality();
        this.description = instance.getDescription();
        this.organizacaoNome = instance.getOrganizacao().getNomeFantasia();
        this.organizacaoId = instance.getOrganizacao().getId();

        this.tools = instance.getTools();




    }
}


