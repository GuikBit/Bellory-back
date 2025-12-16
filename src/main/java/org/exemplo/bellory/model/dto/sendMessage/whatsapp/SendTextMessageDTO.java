package org.exemplo.bellory.model.dto.sendMessage.whatsapp;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para enviar mensagem de texto
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendTextMessageDTO {

    @NotBlank(message = "O número é obrigatório")
    private String number;

    @NotBlank(message = "O texto da mensagem é obrigatório")
    private String text;
}
