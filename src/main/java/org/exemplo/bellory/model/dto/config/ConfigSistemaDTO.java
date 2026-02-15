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


    // Adicione aqui outros DTOs de configuração quando criar
    // private ConfigClienteDTO configCliente;
    // private ConfigServicoDTO configServico;
    // private ConfigColaboradorDTO configColaborador;
}
