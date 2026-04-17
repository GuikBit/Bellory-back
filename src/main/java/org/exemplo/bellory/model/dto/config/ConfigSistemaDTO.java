package org.exemplo.bellory.model.dto.config;

import lombok.*;
import org.exemplo.bellory.model.entity.config.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigSistemaDTO {
    private Long id;
    private Long organizacaoId;
    private Boolean usaEcommerce;
    private Boolean usaGestaoProdutos;
    private Boolean usaPlanosParaClientes;
    private Boolean disparaNotificacoesPush;
    private String urlAcesso;
    private String tenantId;
    private ConfigAgendamento configAgendamento;
    private ConfigServico configServico;
    private ConfigCliente configCliente;
    private ConfigNotificacao configNotificacao;
    private ConfigColaborador configColaborador;

    public static ConfigSistemaDTO fromEntity(ConfigSistema config) {
        if (config == null) return null;
        return ConfigSistemaDTO.builder()
                .id(config.getId())
                .organizacaoId(config.getOrganizacao() != null ? config.getOrganizacao().getId() : null)
                .usaEcommerce(config.isUsaEcommerce())
                .usaGestaoProdutos(config.isUsaGestaoProdutos())
                .usaPlanosParaClientes(config.isUsaPlanosParaClientes())
                .disparaNotificacoesPush(config.isDisparaNotificacoesPush())
                .urlAcesso(config.getUrlAcesso())
                .tenantId(config.getTenantId())
                .configAgendamento(config.getConfigAgendamento())
                .configServico(config.getConfigServico())
                .configCliente(config.getConfigCliente())
                .configColaborador(config.getConfigColaborador())
                .configNotificacao(config.getConfigNotificacao())
                .build();
    }
}
