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
    private List<GraficoDTO> graficos;
    private List<TendenciaDTO> tendencias;

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
        private BigDecimal valorPendente;
        private BigDecimal valorVencido;
        private Double percentualRecebimento;
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
        private Long novosClientesHoje;
        private Long novosClientesEsteMes;
        private Long clientesRecorrentes;
        private Double taxaRetencao;
        private Double ticketMedioCliente;
        private List<ClienteTopDTO> topClientes;
        private Long clientesAniversarioHoje;
        private Long clientesAniversarioEstaSemana;
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
        private Long pedidosCancelados;
        private Double ticketMedioVenda;
        private List<ServicoTopDTO> servicosMaisVendidos;
        private Map<String, Long> vendasPorCategoria;
        private Double crescimentoVendas;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GraficoDTO {
        private String tipo; // "linha", "barra", "pizza"
        private String titulo;
        private List<String> labels;
        private List<SerieGraficoDTO> series;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SerieGraficoDTO {
        private String nome;
        private List<Object> dados;
        private String cor;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TendenciaDTO {
        private String metrica;
        private BigDecimal valorAtual;
        private BigDecimal valorAnterior;
        private Double percentualMudanca;
        private String tendencia; // "ALTA", "BAIXA", "ESTAVEL"
        private String periodo;
    }
}
