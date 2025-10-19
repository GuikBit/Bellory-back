package org.exemplo.bellory.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.plano.PlanoLimites;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizacaoInfoDTO {
    private Long id;
    private String nome;
    private String nomeFantasia;
    private Plano plano;
    private ConfigSistema configSistema;
    private boolean ativo;
    private LocalDateTime dtCadastro;
    private PlanoLimites limitesPersonalizados;
}
