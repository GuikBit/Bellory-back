package org.exemplo.bellory.model.dto.plano;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoLimitesDTO {
    private Integer maxAgendamentosMes;
    private Integer maxUsuarios;
    private Integer maxClientes;
    private Integer maxServicos;
    private Integer maxUnidades;
    private boolean permiteAgendamentoOnline;
    private boolean permiteWhatsapp;
    private boolean permiteSite;
    private boolean permiteEcommerce;
    private boolean permiteRelatoriosAvancados;
    private boolean permiteApi;
    private boolean permiteIntegracaoPersonalizada;
    private boolean suportePrioritario;
    private boolean suporte24x7;
}
