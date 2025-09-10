package org.exemplo.bellory.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
        boolean success;
        String message;
        String token;
        UserInfoDTO user;
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime expiresAt;
}
