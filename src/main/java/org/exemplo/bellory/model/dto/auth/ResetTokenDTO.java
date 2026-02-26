package org.exemplo.bellory.model.dto.auth;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetTokenDTO {
    private String resetToken;
}
