package org.exemplo.bellory.model.dto.dashboard;

import lombok.*;
import org.exemplo.bellory.model.dto.dashboard.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {

    // === INFORMAÇÕES GERAIS ===
    private LocalDate dataConsulta;
    private String periodoConsulta;

    // === AGENDAMENTOS ===
    private AgendamentosResumoDTO agendamentos;

    // === FINANCEIRO ===
    private FinanceiroResumoDTO financeiro;

    // === CLIENTES ===
    private ClientesResumoDTO clientes;

    // === PRODUTOS E ESTOQUE ===
    private EstoqueResumoDTO estoque;

    // === FUNCIONÁRIOS ===
    private FuncionariosResumoDTO funcionarios;

    // === VENDAS E PEDIDOS ===
    private VendasResumoDTO vendas;

    // === GRÁFICOS E TENDÊNCIAS ===
    private GraficosDTO graficos;
    private TendenciasDTO tendencias;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgendamentosResumoDTO {
        private Long total;
        private Long hoje;
        private Long estaSemana;
        private Long esteMes;
        private Map<String, Long> porStatus;
        private Long pendentes;
        private Long agendados;
        private Long concluidos;
        private Long cancelados;
        private Long emEspera;
        private Double taxaOcupacao;
        private Double taxaCancelamento;
        private Long proximosAgendamentos;
        private List<AgendamentoRecenteDTO> recentes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinanceiroResumoDTO {
        private BigDecimal receitaTotal;
        private BigDecimal receitaHoje;
        private BigDecimal receitaEsteMes;
        private BigDecimal receitaEsteAno;
        private BigDecimal receitaPrevista;
        private BigDecimal ticketMedio;
        private Map<String, BigDecimal> receitaPorServico;
        private Map<String, BigDecimal> receitaPorFuncionario;
        private BigDecimal receitaProdutos;
        private BigDecimal receitaServicos;
        private Long totalCobrancas;
        private Long cobrancasPagas;
        private Long cobrancasPendentes;
        private Long cobrancasVencidas;
        private BigDecimal contasReceber;
        private BigDecimal contasVencidas;
        private BigDecimal valorPendente;
        private BigDecimal valorVencido;
        private Double percentualRecebimento;
        private Long totalTransacoes;
        private Map<String, Long> formasPagamento;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClientesResumoDTO {
        private Long totalClientes;
        private Long clientesAtivos;
        private Long clientesInativos;
        private Long novosClientes;
        private Long novosClientesHoje;
        private Long novosClientesEsteMes;
        private Long clientesRecorrentes;
        private Double taxaRetencao;
        private Double ticketMedioCliente;
        private List<ClienteTopDTO> topClientes;
        private Long clientesAniversarioHoje;
        private Long clientesAniversarioEstaSemana;
        private Long aniversariantesMes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstoqueResumoDTO {
        private Long totalProdutos;
        private Long produtosAtivos;
        private Long produtosEstoqueBaixo;
        private Long produtosSemEstoque;
        private BigDecimal valorTotalEstoque;
        private List<ProdutoEstoqueDTO> produtosBaixoEstoque;
        private List<ProdutoTopDTO> produtosMaisVendidos;
        private Double giroEstoque;
        private BigDecimal valorEstoqueParado;
        private Long alertasEstoque;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FuncionariosResumoDTO {
        private Long totalFuncionarios;
        private Long funcionariosAtivos;
        private Double produtividadeMedia;
        private List<FuncionarioPerformanceDTO> topPerformers;
        private Map<String, Long> agendamentosPorFuncionario;
        private Map<String, BigDecimal> receitaPorFuncionario;
        private Double ocupacaoMediaFuncionarios;
        private FuncionarioTopDTO funcionarioMaisAtendimentos;
        private FuncionarioTopDTO funcionarioMaisReceita;
        private Double taxaOcupacaoMedia;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VendasResumoDTO {
        private Long totalVendas;
        private BigDecimal valorTotalVendas;
        private Long vendasHoje;
        private BigDecimal valorVendasHoje;
        private Long pedidosPendentes;
        private Long pedidosEntregues;
        private BigDecimal valorTotalVendido;
        private Long pedidosCancelados;
        private Double ticketMedioVenda;
        private ServicoTopDTO servicosMaisVendidos;
        private Map<String, Long> vendasPorCategoria;
        private Double crescimentoVendas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraficosDTO {
        private List<GraficoReceitaDTO> receitaPorPeriodo;
        private Map<String, Long> agendamentosPorStatus;
        private Map<String, Long> servicosMaisProcurados;
    }

    /**
     * Tendências do Dashboard
     * Contém análises de tendências comparativas
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TendenciasDTO {
        private List<TendenciaDTO> tendencias;
    }

    /**
     * Tendência individual (receita, agendamentos, clientes, etc)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TendenciaDTO {
        private String metrica;                 // Ex: "Receita", "Agendamentos", "Novos Clientes"
        private BigDecimal valorAtual;          // Valor do período atual
        private BigDecimal valorAnterior;       // Valor do período anterior
        private Double percentualMudanca;       // Percentual de mudança (positivo = crescimento)
        private String tendencia;               // "ALTA", "BAIXA", "ESTAVEL"
        private String periodo;                 // Descrição do período comparado
    }
}
