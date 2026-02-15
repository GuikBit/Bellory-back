package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.config.ApiKey;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyUserInfo {
    private Long userId;
    private String username;
    private String nomeCompleto;
    private String email;
    private String role;
    private ApiKey.UserType userType;
    private Long organizacaoId;
    private Organizacao organizacao;
}