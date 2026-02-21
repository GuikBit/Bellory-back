package org.exemplo.bellory.model.dto.instancia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO para criar uma nova instância
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceCreateDTO {

    @NotBlank(message = "O nome da instância é obrigatório")
    @Size(min = 3, max = 100, message = "O nome da instância deve ter entre 3 e 100 caracteres")
    private String instanceName;

    private String instanceNumber;

    private String webhookUrl = "https://auto.bellory.com.br/webhook/bot";

    private Boolean webhookEnabled = true;

    private List<String> webhookEvents;

    private String personality;

    private Boolean rejectCall = false;

    private String msgCall;

    private Boolean groupsIgnore = true;

    private Boolean alwaysOnline = false;

    private Boolean readMessages = false;

    private Boolean readStatus = false;

    private String description;
}



