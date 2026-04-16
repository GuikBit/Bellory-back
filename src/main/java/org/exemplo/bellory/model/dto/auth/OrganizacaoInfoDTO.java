package org.exemplo.bellory.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.tema.Tema;

import org.exemplo.bellory.model.dto.assinatura.AssinaturaStatusDTO;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizacaoInfoDTO {
    private Long id;
    private String nome;
    private String nomeFantasia;
    private String slug;
    private String emailPrincipal;
    private ConfigSistema configSistema;
    private Tema tema;
    private boolean ativo;
    private LocalDateTime dtCadastro;
    private AssinaturaStatusDTO assinatura;
}
