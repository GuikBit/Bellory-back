package org.exemplo.bellory.model.dto.instancia;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO para atualizar uma instância existente
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceUpdateDTO {

    private String webhookUrl;

    private Boolean webhookEnabled;

    private List<String> webhookEvents;

    private Boolean rejectCall;

    private String msgCall;

    private Boolean groupsIgnore;

    private Boolean alwaysOnline;

    private Boolean readMessages;

    private Boolean readStatus;

    private Boolean isActive;

    private String description;
}



