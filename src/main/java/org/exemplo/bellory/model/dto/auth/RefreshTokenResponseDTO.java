package org.exemplo.bellory.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponseDTO {
        private boolean success;
        private String message;
        private String newToken;
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        private LocalDateTime expiresAt;
}