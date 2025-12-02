package org.exemplo.bellory.model.dto.sendMessage.whatsapp;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class SetWebhookDTO {

    @NotBlank(message = "A URL do webhook é obrigatória")
    private String url;

    private Boolean enabled = true;

    private java.util.List<String> events;
}
