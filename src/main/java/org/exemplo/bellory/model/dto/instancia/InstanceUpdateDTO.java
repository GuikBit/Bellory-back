package org.exemplo.bellory.model.dto.instancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO para atualizar uma instância existente
 * ✅ FIX: Agora inclui Tools, Personality e campos completos de webhook
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstanceUpdateDTO {

    // Campos básicos
    private String description;
    private String personality;

    // Settings
    private Boolean rejectCall;
    private String msgCall;
    private Boolean groupsIgnore;
    private Boolean alwaysOnline;
    private Boolean readMessages;
    private Boolean readStatus;
    private Boolean isActive;

    // Webhook
    private String webhookUrl;
    private Boolean webhookEnabled;
    private List<String> webhookEvents;

    // ✅ NOVO: Tools
    private ToolsDTO tools;

    private Boolean ativo;
    private Boolean deletado;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolsDTO {
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
}
