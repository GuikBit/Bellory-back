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
class InstanceInfoDTO {

    private String instanceName;
    private String phoneNumber;
    private String profileName;
    private String profilePictureUrl;
    private String status;
}
