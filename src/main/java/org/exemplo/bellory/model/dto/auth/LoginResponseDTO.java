package org.exemplo.bellory.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

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
        OrganizacaoInfoDTO organizacao;
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime expiresAt;
}
