package org.exemplo.bellory.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrganizacaoDetalheDTO {
    private Long id;
    private String nomeFantasia;
    private String razaoSocial;
    private String cnpj;
    private String emailPrincipal;
    private String telefone1;
    private String telefone2;
    private String whatsapp;
    private String slug;
    private Boolean ativo;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dtCadastro;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dtAtualizacao;

    // Responsavel
    private String responsavelNome;
    private String responsavelEmail;
    private String responsavelTelefone;

    // Plano
    private PlanoInfo plano;
    private LimitesInfo limites;
    private LimitesInfo limitesPersonalizados;

    // Metricas
    private MetricasOrganizacao metricas;

    // Instancias
    private List<InstanciaResumoDTO> instancias;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanoInfo {
        private Long id;
        private String codigo;
        private String nome;
        private BigDecimal precoMensal;
        private BigDecimal precoAnual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimitesInfo {
        private Integer maxAgendamentosMes;
        private Integer maxUsuarios;
        private Integer maxClientes;
        private Integer maxServicos;
        private Integer maxUnidades;
        private Boolean permiteAgendamentoOnline;
        private Boolean permiteWhatsapp;
        private Boolean permiteSite;
        private Boolean permiteEcommerce;
        private Boolean permiteRelatoriosAvancados;
        private Boolean permiteApi;
        private Boolean permiteIntegracaoPersonalizada;
        private Boolean suportePrioritario;
        private Boolean suporte24x7;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricasOrganizacao {
        private Long totalAgendamentos;
        private Long agendamentosNoMes;
        private Long agendamentosConcluidos;
        private Long agendamentosCancelados;
        private Long agendamentosPendentes;
        private Long totalClientes;
        private Long clientesAtivos;
        private Long totalFuncionarios;
        private Long funcionariosAtivos;
        private Long totalServicos;
        private Long servicosAtivos;
        private BigDecimal faturamentoTotal;
        private BigDecimal faturamentoMes;
        private Long totalCobrancas;
        private Long cobrancasPagas;
        private Long cobrancasPendentes;
        private Long cobrancasVencidas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanciaResumoDTO {
        private Long id;
        private String instanceName;
        private String instanceId;
        private String status;
        private Boolean ativo;
    }
}
