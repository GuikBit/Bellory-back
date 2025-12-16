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
class SendMediaMessageDTO {

    @NotBlank(message = "O número é obrigatório")
    private String number;

    @NotBlank(message = "A URL da mídia é obrigatória")
    private String mediaUrl;

    private String caption;

    private String mediaType = "image"; // image, video, document
}
