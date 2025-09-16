package org.exemplo.bellory.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.dashboard.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.produtos.ProdutoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final AgendamentoRepository agendamentoRepository;
    private final CobrancaRepository cobrancaRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ServicoRepository servicoRepository;
    // Assumindo que você criará esses repositories


    @Transactional
    public DashboardDTO getDashboardGeral(DashboardFiltroDTO filtro) {
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().minusDays(30);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();

        return DashboardDTO.builder()
                .dataConsulta(LocalDate.now())
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .agendamentos(getAgendamentosResumo(dataInicio, dataFim, filtro))
                .financeiro(getFinanceiroResumo(dataInicio, dataFim, filtro))
                .clientes(getClientesResumo(dataInicio, dataFim, filtro))
                .estoque(getEstoqueResumo(filtro))
                .funcionarios(getFuncionariosResumo(dataInicio, dataFim, filtro))
                .vendas(getVendasResumo(dataInicio, dataFim, filtro))
                .graficos(getGraficos(dataInicio, dataFim, filtro))
                .tendencias(getTendencias(dataInicio, dataFim, filtro))
                .build();
    }


    private DashboardDTO.AgendamentosResumoDTO getAgendamentosResumo(LocalDate dataInicio, LocalDate dataFim, DashboardFiltroDTO filtro) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        List<Agendamento> agendamentos;
        if (filtro.getFuncionarioId() != null) {
            agendamentos = agendamentoRepository.findByFuncionarioAndDataRange(
                    filtro.getFuncionarioId(), inicioDateTime, fimDateTime);
        } else {
            agendamentos = agendamentoRepository.findByDataRange(inicioDateTime, fimDateTime);
        }

        // Contadores por status
        Map<String, Long> porStatus = agendamentos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus().name(),
                        Collectors.counting()
                ));

        // Agendamentos hoje
        LocalDateTime hojeBaixo = LocalDate.now().atStartOfDay();
        LocalDateTime hojeAlto = LocalDate.now().atTime(23, 59, 59);
        Long agendamentosHoje = agendamentoRepository.countByDataRange(hojeBaixo, hojeAlto);

        // Agendamentos esta semana
        LocalDate inicioSemana = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate fimSemana = inicioSemana.plusDays(6);
        Long agendamentosEstaSemana = agendamentoRepository.countByDataRange(
                inicioSemana.atStartOfDay(), fimSemana.atTime(23, 59, 59));

        // Agendamentos este mês
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        Long agendamentosEsteMes = agendamentoRepository.countByDataRange(
                inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59));

        // Taxa de ocupação (simulada - você precisará implementar baseado na jornada dos funcionários)
        Double taxaOcupacao = calcularTaxaOcupacao(agendamentos);

        // Taxa de cancelamento
        long totalAgendamentos = agendamentos.size();
        long cancelados = porStatus.getOrDefault("CANCELADO", 0L);
        Double taxaCancelamento = totalAgendamentos > 0 ?
                (cancelados * 100.0) / totalAgendamentos : 0.0;

        // Próximos agendamentos (próximos 7 dias)
        LocalDateTime proximaSemana = LocalDateTime.now().plusDays(7).withHour(23).withMinute(59);
        Long proximosAgendamentos = agendamentoRepository.countByDataRange(
                LocalDateTime.now(), proximaSemana);

        // Agendamentos recentes
        List<AgendamentoRecenteDTO> recentes = agendamentos.stream()
                .sorted((a1, a2) -> a2.getDtAgendamento().compareTo(a1.getDtAgendamento()))
                .limit(10)
                .map(this::mapToAgendamentoRecente)
                .collect(Collectors.toList());

        return DashboardDTO.AgendamentosResumoDTO.builder()
                .total((long) agendamentos.size())
                .hoje(agendamentosHoje)
                .estaSemana(agendamentosEstaSemana)
                .esteMes(agendamentosEsteMes)
                .porStatus(porStatus)
                .pendentes(porStatus.getOrDefault("PENDENTE", 0L))
                .agendados(porStatus.getOrDefault("AGENDADO", 0L))
                .concluidos(porStatus.getOrDefault("CONCLUIDO", 0L))
                .cancelados(porStatus.getOrDefault("CANCELADO", 0L))
                .emEspera(porStatus.getOrDefault("EM_ESPERA", 0L))
                .taxaOcupacao(taxaOcupacao)
                .taxaCancelamento(taxaCancelamento)
                .proximosAgendamentos(proximosAgendamentos)
                .recentes(recentes)
                .build();
    }

    private DashboardDTO.FinanceiroResumoDTO getFinanceiroResumo(LocalDate dataInicio, LocalDate dataFim, DashboardFiltroDTO filtro) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Receita total do período
        BigDecimal receitaTotal = cobrancaRepository.sumReceitaByPeriod(inicioDateTime, fimDateTime, Cobranca.StatusCobranca.PAGO);
        if (receitaTotal == null) receitaTotal = BigDecimal.ZERO;

        // Receita hoje
        LocalDateTime hojeBaixo = LocalDate.now().atStartOfDay();
        LocalDateTime hojeAlto = LocalDate.now().atTime(23, 59, 59);
        BigDecimal receitaHoje = cobrancaRepository.sumReceitaByPeriod(hojeBaixo, hojeAlto, Cobranca.StatusCobranca.PAGO);
        if (receitaHoje == null) receitaHoje = BigDecimal.ZERO;

        // Receita este mês
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        BigDecimal receitaEsteMes = cobrancaRepository.sumReceitaByPeriod(
                inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59 ), Cobranca.StatusCobranca.PAGO);
        if (receitaEsteMes == null) receitaEsteMes = BigDecimal.ZERO;

        // Receita este ano
        LocalDate inicioAno = LocalDate.now().withDayOfYear(1);
        LocalDate fimAno = inicioAno.plusYears(1).minusDays(1);
        BigDecimal receitaEsteAno = cobrancaRepository.sumReceitaByPeriod(
                inicioAno.atStartOfDay(), fimAno.atTime(23, 59, 59),Cobranca.StatusCobranca.PAGO);
        if (receitaEsteAno == null) receitaEsteAno = BigDecimal.ZERO;

        // Receita prevista (agendamentos futuros)
        BigDecimal receitaPrevista = calcularReceitaPrevista();

        // Ticket médio
        Long totalAgendamentos = agendamentoRepository.countByDataRange(inicioDateTime, fimDateTime);
        BigDecimal ticketMedio = totalAgendamentos > 0 ?
                receitaTotal.divide(new BigDecimal(totalAgendamentos), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Receita por serviço
        Map<String, BigDecimal> receitaPorServico = calcularReceitaPorServico(inicioDateTime, fimDateTime);

        // Receita por funcionário
        Map<String, BigDecimal> receitaPorFuncionario = calcularReceitaPorFuncionario(inicioDateTime, fimDateTime);

        // Separação receita produtos vs serviços (você precisará implementar essa lógica)
        BigDecimal receitaProdutos = calcularReceitaProdutos(inicioDateTime, fimDateTime);
        BigDecimal receitaServicos = receitaTotal.subtract(receitaProdutos);

        // Informações de cobrança
        List<Cobranca> cobrancas = cobrancaRepository.findByPeriod(inicioDateTime, fimDateTime);
        Long totalCobrancas = (long) cobrancas.size();
        Long cobrancasPagas = cobrancas.stream()
                .mapToLong(c -> (c.getStatusCobranca().equals(Status.PAGO)) ? 1 : 0)
                .sum();
        Long cobrancasPendentes = cobrancas.stream()
                .mapToLong(c -> c.getStatusCobranca().equals(Status.PENDENTE)? 1 : 0)
                .sum();
        Long cobrancasVencidas = cobrancas.stream()
                .mapToLong(c -> c.getStatusCobranca().equals( Status.VENCIDA) ? 1 : 0)
                .sum();

        BigDecimal valorPendente = cobrancas.stream()
                .filter(c -> c.getStatusCobranca().equals(Status.PENDENTE))
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorVencido = cobrancas.stream()
                .filter(c -> c.getStatusCobranca().equals(Status.VENCIDA))
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double percentualRecebimento = totalCobrancas > 0 ?
                (cobrancasPagas * 100.0) / totalCobrancas : 0.0;

        return DashboardDTO.FinanceiroResumoDTO.builder()
                .receitaTotal(receitaTotal)
                .receitaHoje(receitaHoje)
                .receitaEsteMes(receitaEsteMes)
                .receitaEsteAno(receitaEsteAno)
                .receitaPrevista(receitaPrevista)
                .ticketMedio(ticketMedio)
                .receitaPorServico(receitaPorServico)
                .receitaPorFuncionario(receitaPorFuncionario)
                .receitaProdutos(receitaProdutos)
                .receitaServicos(receitaServicos)
                .totalCobrancas(totalCobrancas)
                .cobrancasPagas(cobrancasPagas)
                .cobrancasPendentes(cobrancasPendentes)
                .cobrancasVencidas(cobrancasVencidas)
                .valorPendente(valorPendente)
                .valorVencido(valorVencido)
                .percentualRecebimento(percentualRecebimento)
                .build();
    }
    private DashboardDTO.ClientesResumoDTO getClientesResumo(LocalDate dataInicio, LocalDate dataFim, DashboardFiltroDTO filtro) {
        // Total de clientes
        Long totalClientes = clienteRepository.count();
        Long clientesAtivos = clienteRepository.countByAtivo(true);
        Long clientesInativos = totalClientes - clientesAtivos;

        // Novos clientes
        LocalDateTime hojeBaixo = LocalDate.now().atStartOfDay();
        LocalDateTime hojeAlto = LocalDate.now().atTime(23, 59, 59);
        Long novosClientesHoje = clienteRepository.countByDataCriacaoBetween(hojeBaixo, hojeAlto);

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        Long novosClientesEsteMes = clienteRepository.countByDataCriacaoBetween(
                inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59));

        // Clientes recorrentes (com mais de 1 agendamento)
        Long clientesRecorrentes = clienteRepository.countClientesRecorrentes();

        // Taxa de retenção
        Double taxaRetencao = totalClientes > 0 ?
                (clientesRecorrentes * 100.0) / totalClientes : 0.0;

        // Ticket médio por cliente
        BigDecimal receitaTotal = cobrancaRepository.sumReceitaByPeriod(
                dataInicio.atStartOfDay(), dataFim.atTime(23, 59, 59), Cobranca.StatusCobranca.PAGO);
        if (receitaTotal == null) receitaTotal = BigDecimal.ZERO;

        Double ticketMedioCliente = clientesAtivos > 0 ?
                receitaTotal.divide(new BigDecimal(clientesAtivos), 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        // Top clientes
        List<ClienteTopDTO> topClientes = clienteRepository.findTopClientes()
                .stream()
                .limit(10)
                .map(this::mapToClienteTop)
                .collect(Collectors.toList());

        // CORREÇÃO: Usar LocalDate para aniversariantes
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // Aniversariantes
        Long clientesAniversarioHoje = clienteRepository.countAniversariantesHoje();
        // CORREÇÃO: Agora passando LocalDate em vez de LocalDateTime
        Long clientesAniversarioEstaSemana = clienteRepository.countAniversariantesEstaSemana(inicioSemana, fimSemana);

        return DashboardDTO.ClientesResumoDTO.builder()
                .totalClientes(totalClientes)
                .clientesAtivos(clientesAtivos)
                .clientesInativos(clientesInativos)
                .novosClientesHoje(novosClientesHoje)
                .novosClientesEsteMes(novosClientesEsteMes)
                .clientesRecorrentes(clientesRecorrentes)
                .taxaRetencao(taxaRetencao)
                .ticketMedioCliente(ticketMedioCliente)
                .topClientes(topClientes)
                .clientesAniversarioHoje(clientesAniversarioHoje)
                .clientesAniversarioEstaSemana(clientesAniversarioEstaSemana)
                .build();
    }

    private DashboardDTO.EstoqueResumoDTO getEstoqueResumo(DashboardFiltroDTO filtro) {
        List<Produto> produtos = produtoRepository.findByAtivo(true);

        Long totalProdutos = (long) produtos.size();
        Long produtosAtivos = produtoRepository.countByAtivo(true);

        // Produtos com estoque baixo (menos de 10 unidades - configurável)
        int limiteEstoqueBaixo = 10;
        Long produtosEstoqueBaixo = produtos.stream()
                .mapToLong(p -> p.getQuantidadeEstoque() > 0 && p.getQuantidadeEstoque() <= limiteEstoqueBaixo ? 1 : 0)
                .sum();

        Long produtosSemEstoque = produtos.stream()
                .mapToLong(p -> p.getQuantidadeEstoque() == 0 ? 1 : 0)
                .sum();

        // Valor total do estoque
        BigDecimal valorTotalEstoque = produtos.stream()
                .map(p -> p.getPreco().multiply(new BigDecimal(p.getQuantidadeEstoque())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Produtos com baixo estoque para alertas
        List<ProdutoEstoqueDTO> produtosBaixoEstoque = produtos.stream()
                .filter(p -> p.getQuantidadeEstoque() <= limiteEstoqueBaixo)
                .map(this::mapToProdutoEstoque)
                .collect(Collectors.toList());

        // Produtos mais vendidos (simulado - você precisará implementar com base nas vendas reais)
        List<ProdutoTopDTO> produtosMaisVendidos = getProdutosMaisVendidos();

        // Giro de estoque (simulado)
        Double giroEstoque = calcularGiroEstoque();

        // Valor do estoque parado (produtos sem movimento há mais de 90 dias)
        BigDecimal valorEstoqueParado = calcularEstoqueParado();

        Long alertasEstoque = produtosEstoqueBaixo + produtosSemEstoque;

        return DashboardDTO.EstoqueResumoDTO.builder()
                .totalProdutos(totalProdutos)
                .produtosAtivos(produtosAtivos)
                .produtosEstoqueBaixo(produtosEstoqueBaixo)
                .produtosSemEstoque(produtosSemEstoque)
                .valorTotalEstoque(valorTotalEstoque)
                .produtosBaixoEstoque(produtosBaixoEstoque)
                .produtosMaisVendidos(produtosMaisVendidos)
                .giroEstoque(giroEstoque)
                .valorEstoqueParado(valorEstoqueParado)
                .alertasEstoque(alertasEstoque)
                .build();
    }

    private DashboardDTO.FuncionariosResumoDTO getFuncionariosResumo(LocalDate dataInicio, LocalDate dataFim, DashboardFiltroDTO filtro) {
        List<Funcionario> funcionarios = funcionarioRepository.findByAtivo(true);

        Long totalFuncionarios = (long) funcionarios.size();
        Long funcionariosAtivos = funcionarioRepository.countByAtivo(true);

        // Produtividade média (agendamentos por funcionário)
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        Long totalAgendamentos = agendamentoRepository.countByDataRange(inicioDateTime, fimDateTime);
        Double produtividadeMedia = funcionariosAtivos > 0 ?
                totalAgendamentos.doubleValue() / funcionariosAtivos : 0.0;

        // Top performers
        List<FuncionarioPerformanceDTO> topPerformers = funcionarios.stream()
                .map(f -> mapToFuncionarioPerformance(f, inicioDateTime, fimDateTime))
                .sorted((f1, f2) -> f2.getReceitaGerada().compareTo(f1.getReceitaGerada()))
                .limit(5)
                .collect(Collectors.toList());

        // Agendamentos por funcionário
        Map<String, Long> agendamentosPorFuncionario = funcionarios.stream()
                .collect(Collectors.toMap(
                        Funcionario::getNomeCompleto,
                        f -> agendamentoRepository.countByFuncionarioAndDataRange(
                                f.getId(), inicioDateTime, fimDateTime)
                ));

        // Receita por funcionário
        Map<String, BigDecimal> receitaPorFuncionario = funcionarios.stream()
                .collect(Collectors.toMap(
                        Funcionario::getNomeCompleto,
                        f -> calcularReceitaFuncionario(f.getId(), inicioDateTime, fimDateTime)
                ));

        // Ocupação média dos funcionários
        Double ocupacaoMediaFuncionarios = calcularOcupacaoMediaFuncionarios(funcionarios, inicioDateTime, fimDateTime);

        return DashboardDTO.FuncionariosResumoDTO.builder()
                .totalFuncionarios(totalFuncionarios)
                .funcionariosAtivos(funcionariosAtivos)
                .produtividadeMedia(produtividadeMedia)
                .topPerformers(topPerformers)
                .agendamentosPorFuncionario(agendamentosPorFuncionario)
                .receitaPorFuncionario(receitaPorFuncionario)
                .ocupacaoMediaFuncionarios(ocupacaoMediaFuncionarios)
                .build();
    }

    private DashboardDTO.VendasResumoDTO getVendasResumo(LocalDate dataInicio, LocalDate dataFim, DashboardFiltroDTO filtro) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Vendas totais (considerando agendamentos concluídos como vendas)
        Long totalVendas = agendamentoRepository.countByStatusAndDataRange(
                Status.CONCLUIDO, inicioDateTime, fimDateTime);

        BigDecimal valorTotalVendas = cobrancaRepository.sumReceitaByStatusAndPeriod(
                Status.PAGO, inicioDateTime, fimDateTime);
        if (valorTotalVendas == null) valorTotalVendas = BigDecimal.ZERO;

        // Vendas hoje
        LocalDateTime hojeBaixo = LocalDate.now().atStartOfDay();
        LocalDateTime hojeAlto = LocalDate.now().atTime(23, 59, 59);
        Long vendasHoje = agendamentoRepository.countByStatusAndDataRange(
                Status.CONCLUIDO, hojeBaixo, hojeAlto);

        BigDecimal valorVendasHoje = cobrancaRepository.sumReceitaByStatusAndPeriod(
                Status.PAGO, hojeBaixo, hojeAlto);
        if (valorVendasHoje == null) valorVendasHoje = BigDecimal.ZERO;

        // Pedidos por status
        Long pedidosPendentes = agendamentoRepository.countByStatusAndDataRange(
                Status.PENDENTE, inicioDateTime, fimDateTime);
        Long pedidosEntregues = agendamentoRepository.countByStatusAndDataRange(
                Status.CONCLUIDO, inicioDateTime, fimDateTime);
        Long pedidosCancelados = agendamentoRepository.countByStatusAndDataRange(
                Status.CANCELADO, inicioDateTime, fimDateTime);

        // Ticket médio
        Double ticketMedioVenda = totalVendas > 0 ?
                valorTotalVendas.divide(new BigDecimal(totalVendas), 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        // Serviços mais vendidos
        List<ServicoTopDTO> servicosMaisVendidos = getServicosMaisVendidos(inicioDateTime, fimDateTime);

        // Vendas por categoria
        Map<String, Long> vendasPorCategoria = getVendasPorCategoria(inicioDateTime, fimDateTime);

        // Crescimento das vendas (comparando com período anterior)
        Double crescimentoVendas = calcularCrescimentoVendas(dataInicio, dataFim);

        return DashboardDTO.VendasResumoDTO.builder()
                .totalVendas(totalVendas)
                .valorTotalVendas(valorTotalVendas)
                .vendasHoje(vendasHoje)
                .valorVendasHoje(valorVendasHoje)
                .pedidosPendentes(pedidosPendentes)
                .pedidosEntregues(pedidosEntregues)
                .pedidosCancelados(pedidosCancelados)
                .ticketMedioVenda(ticketMedioVenda)
                .servicosMaisVendidos(servicosMaisVendidos)
                .vendasPorCategoria(vendasPorCategoria)
                .crescimentoVendas(crescimentoVendas)
                .build();
    }

    private List<DashboardDTO.GraficoDTO> getGraficos(LocalDate dataInicio, LocalDate dataFim, DashboardFiltroDTO filtro) {
        List<DashboardDTO.GraficoDTO> graficos = new ArrayList<>();

        // Gráfico de receita por dia
        graficos.add(criarGraficoReceitaPorDia(dataInicio, dataFim));

        // Gráfico de agendamentos por status
        graficos.add(criarGraficoAgendamentosPorStatus(dataInicio, dataFim));

        // Gráfico de serviços mais vendidos
        graficos.add(criarGraficoServicosMaisVendidos(dataInicio, dataFim));

        // Gráfico de performance dos funcionários
        graficos.add(criarGraficoPerformanceFuncionarios(dataInicio, dataFim));

        return graficos;
    }

    private List<DashboardDTO.TendenciaDTO> getTendencias(LocalDate dataInicio, LocalDate dataFim, DashboardFiltroDTO filtro) {
        List<DashboardDTO.TendenciaDTO> tendencias = new ArrayList<>();

        // Período anterior para comparação
        long diasPeriodo = dataFim.toEpochDay() - dataInicio.toEpochDay() + 1;
        LocalDate dataInicioAnterior = dataInicio.minusDays(diasPeriodo);
        LocalDate dataFimAnterior = dataInicio.minusDays(1);

        // Tendência de receita
        tendencias.add(criarTendenciaReceita(dataInicio, dataFim, dataInicioAnterior, dataFimAnterior));

        // Tendência de agendamentos
        tendencias.add(criarTendenciaAgendamentos(dataInicio, dataFim, dataInicioAnterior, dataFimAnterior));

        // Tendência de novos clientes
        tendencias.add(criarTendenciaNovosClientes(dataInicio, dataFim, dataInicioAnterior, dataFimAnterior));

        return tendencias;
    }

    @Transactional
    public DashboardComparativoDTO getDashboardComparativo(LocalDate inicioAtual, LocalDate fimAtual,
                                                           LocalDate inicioAnterior, LocalDate fimAnterior) {
        DashboardFiltroDTO filtroAtual = DashboardFiltroDTO.builder()
                .dataInicio(inicioAtual)
                .dataFim(fimAtual)
                .build();

        DashboardFiltroDTO filtroAnterior = DashboardFiltroDTO.builder()
                .dataInicio(inicioAnterior)
                .dataFim(fimAnterior)
                .build();

        DashboardDTO dashAtual = getDashboardGeral(filtroAtual);
        DashboardDTO dashAnterior = getDashboardGeral(filtroAnterior);

        List<DashboardDTO.TendenciaDTO> comparativos = criarComparativos(dashAtual, dashAnterior);

        String descricaoPeriodos = String.format("Comparando %s a %s com %s a %s",
                inicioAtual.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                fimAtual.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                inicioAnterior.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                fimAnterior.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        return DashboardComparativoDTO.builder()
                .periodoAtual(dashAtual)
                .periodoAnterior(dashAnterior)
                .comparativos(comparativos)
                .descricaoPeriodos(descricaoPeriodos)
                .build();
    }
    @Transactional
    public Object getMetricasFuncionario(Long funcionarioId, LocalDate dataInicio, LocalDate dataFim) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        DashboardFiltroDTO filtro = DashboardFiltroDTO.builder()
                .funcionarioId(funcionarioId)
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .build();

        return getDashboardGeral(filtro);
    }

    // Métodos auxiliares de validação e formatação
    private void validarFiltro(DashboardFiltroDTO filtro) {
        if (filtro.getDataInicio() != null && filtro.getDataFim() != null) {
            if (filtro.getDataInicio().isAfter(filtro.getDataFim())) {
                throw new IllegalArgumentException("Data de início não pode ser posterior à data de fim");
            }
        }
    }

    private String formatarPeriodo(LocalDate inicio, LocalDate fim) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return inicio.format(formatter) + " a " + fim.format(formatter);
    }

    private Double calcularTaxaOcupacao(List<Agendamento> agendamentos) {
        // Simulação - você precisará implementar baseado na jornada dos funcionários
        // Considerar: horas trabalhadas vs horas disponíveis
        return 75.0; // Exemplo: 75% de ocupação
    }

    private BigDecimal calcularReceitaPrevista() {
        // Soma dos valores dos agendamentos futuros que ainda não foram pagos
        LocalDateTime agora = LocalDateTime.now();
        return agendamentoRepository.findAgendamentosFuturos(agora)
                .stream()
                .filter(a -> a.getCobranca() != null && !a.getCobranca().getStatusCobranca().equals(Status.PAGO))
                .map(a -> a.getCobranca().getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> calcularReceitaPorServico(LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByDataRangeWithServicos(inicio, fim)
                .stream()
                .flatMap(a -> a.getServicos().stream())
                .collect(Collectors.groupingBy(
                        s -> s.getNome(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                s -> s.getPreco(),
                                BigDecimal::add
                        )
                ));
    }

    private Map<String, BigDecimal> calcularReceitaPorFuncionario(LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByDataRangeWithFuncionarios(inicio, fim)
                .stream()
                .filter(a -> a.getCobranca() != null && a.getCobranca().getStatusCobranca().equals(Status.PAGO))
                .flatMap(a -> a.getFuncionarios().stream())
                .collect(Collectors.groupingBy(
                        f -> f.getNomeCompleto(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                f -> calcularReceitaFuncionario(f.getId(), inicio, fim),
                                BigDecimal::add
                        )
                ));
    }

    private BigDecimal calcularReceitaProdutos(LocalDateTime inicio, LocalDateTime fim) {
        // Implementar quando tiver vendas de produtos
        return BigDecimal.ZERO;
    }

    private BigDecimal calcularReceitaFuncionario(Long funcionarioId, LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByFuncionarioAndDataRange(funcionarioId, inicio, fim)
                .stream()
                .filter(a -> a.getCobranca() != null && a.getCobranca().getStatusCobranca().equals(Status.PAGO))
                .map(a -> a.getCobranca().getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ProdutoTopDTO> getProdutosMaisVendidos() {
        // Simulação - implementar quando tiver sistema de vendas de produtos
        return new ArrayList<>();
    }

    private Double calcularGiroEstoque() {
        // Simulação - implementar fórmula: Custo dos produtos vendidos / Valor médio do estoque
        return 2.5; // Exemplo: giro de 2.5 vezes por período
    }

    private BigDecimal calcularEstoqueParado() {
        // Produtos sem movimento nos últimos 90 dias
        LocalDateTime limite = LocalDateTime.now().minusDays(90);
        // Implementar lógica para identificar produtos parados
        return BigDecimal.valueOf(5000); // Exemplo
    }

    private Double calcularOcupacaoMediaFuncionarios(List<Funcionario> funcionarios,
                                                     LocalDateTime inicio, LocalDateTime fim) {
        if (funcionarios.isEmpty()) return 0.0;

        double somaOcupacao = funcionarios.stream()
                .mapToDouble(f -> calcularOcupacaoFuncionario(f, inicio, fim))
                .sum();

        return somaOcupacao / funcionarios.size();
    }

    private double calcularOcupacaoFuncionario(Funcionario funcionario, LocalDateTime inicio, LocalDateTime fim) {
        // Implementar cálculo baseado na jornada de trabalho do funcionário
        // vs tempo efetivamente ocupado com agendamentos
        return 70.0; // Exemplo: 70% de ocupação
    }

    private List<ServicoTopDTO> getServicosMaisVendidos(LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByDataRangeWithServicos(inicio, fim)
                .stream()
                .flatMap(a -> a.getServicos().stream())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<org.exemplo.bellory.model.entity.servico.Servico, Long>comparingByValue().reversed())
                .limit(10)
                .map(this::mapToServicoTop)
                .collect(Collectors.toList());
    }

    private Map<String, Long> getVendasPorCategoria(LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByDataRangeWithServicos(inicio, fim)
                .stream()
                .flatMap(a -> a.getServicos().stream())
                .collect(Collectors.groupingBy(
                        s -> s.getCategoria().getLabel(),
                        Collectors.counting()
                ));
    }

    private Double calcularCrescimentoVendas(LocalDate dataInicio, LocalDate dataFim) {
        long diasPeriodo = dataFim.toEpochDay() - dataInicio.toEpochDay() + 1;
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo);
        LocalDate fimAnterior = dataInicio.minusDays(1);

        Long vendasAtual = agendamentoRepository.countByStatusAndDataRange(
                Status.CONCLUIDO, dataInicio.atStartOfDay(), dataFim.atTime(23, 59, 59));

        Long vendasAnterior = agendamentoRepository.countByStatusAndDataRange(
                Status.CONCLUIDO, inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59));

        if (vendasAnterior == 0) return vendasAtual > 0 ? 100.0 : 0.0;

        return ((vendasAtual - vendasAnterior) * 100.0) / vendasAnterior;
    }

    // Métodos de mapeamento para DTOs
    private AgendamentoRecenteDTO mapToAgendamentoRecente(Agendamento agendamento) {
        return AgendamentoRecenteDTO.builder()
                .id(agendamento.getId())
                .clienteNome(agendamento.getCliente().getNomeCompleto())
                .funcionarioNome(agendamento.getFuncionarios().isEmpty() ?
                        "N/A" : agendamento.getFuncionarios().get(0).getNomeCompleto())
                .servicoNome(agendamento.getServicos().isEmpty() ?
                        "N/A" : agendamento.getServicos().get(0).getNome())
                .dataHora(agendamento.getDtAgendamento())
                .status(agendamento.getStatus().getDescricao())
                .valor(agendamento.getCobranca() != null ?
                        agendamento.getCobranca().getValor() : BigDecimal.ZERO)
                .build();
    }

    @Transactional
    protected ClienteTopDTO mapToClienteTop(Cliente cliente) {
        Long totalAgendamentos = agendamentoRepository.countByCliente(cliente.getId());
        BigDecimal valorGasto = cobrancaRepository.sumByCliente(cliente.getId(), Cobranca.StatusCobranca.PAGO);
        LocalDateTime ultimoAgendamento = agendamentoRepository.findLastAgendamentoByCliente(cliente.getId());

        return ClienteTopDTO.builder()
                .id(cliente.getId())
                .nome(cliente.getNomeCompleto())
                .totalAgendamentos(totalAgendamentos)
                .valorGasto(valorGasto != null ? valorGasto : BigDecimal.ZERO)
                .ultimoAgendamento(ultimoAgendamento)
                .build();
    }

    @Transactional
    protected ProdutoEstoqueDTO mapToProdutoEstoque(Produto produto) {
        String status;
        if (produto.getQuantidadeEstoque() == 0) {
            status = "CRITICO";
        } else if (produto.getQuantidadeEstoque() <= 5) {
            status = "BAIXO";
        } else {
            status = "OK";
        }

        return ProdutoEstoqueDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .quantidadeAtual(produto.getQuantidadeEstoque())
                .estoqueMinimo(5) // Configurável
                .categoria(produto.getCategoria().getLabel())
                .status(status)
                .build();
    }

    private FuncionarioPerformanceDTO mapToFuncionarioPerformance(Funcionario funcionario,
                                                                  LocalDateTime inicio, LocalDateTime fim) {
        Long totalAgendamentos = agendamentoRepository.countByFuncionarioAndDataRange(
                funcionario.getId(), inicio, fim);
        BigDecimal receitaGerada = calcularReceitaFuncionario(funcionario.getId(), inicio, fim);
        Double ocupacao = calcularOcupacaoFuncionario(funcionario, inicio, fim);

        return FuncionarioPerformanceDTO.builder()
                .id(funcionario.getId())
                .nome(funcionario.getNomeCompleto())
                .totalAgendamentos(totalAgendamentos)
                .receitaGerada(receitaGerada)
                .avaliacaoMedia(4.5) // Simulado - implementar sistema de avaliações
                .ocupacao(ocupacao)
                .status("ATIVO")
                .build();
    }

    private ServicoTopDTO mapToServicoTop(Map.Entry<org.exemplo.bellory.model.entity.servico.Servico, Long> entry) {
        org.exemplo.bellory.model.entity.servico.Servico servico = entry.getKey();
        Long quantidade = entry.getValue();

        return ServicoTopDTO.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .quantidadeVendida(quantidade)
                .valorTotal(servico.getPreco().multiply(new BigDecimal(quantidade)))
                .categoria(servico.getCategoria().getLabel())
                .tempoMedio(servico.getTempoEstimadoMinutos())
                .build();
    }

    // Métodos para criação de gráficos
    private DashboardDTO.GraficoDTO criarGraficoReceitaPorDia(LocalDate dataInicio, LocalDate dataFim) {
        List<String> labels = new ArrayList<>();
        List<Object> dados = new ArrayList<>();

        LocalDate dataAtual = dataInicio;
        while (!dataAtual.isAfter(dataFim)) {
            labels.add(dataAtual.format(DateTimeFormatter.ofPattern("dd/MM")));

            BigDecimal receita = cobrancaRepository.sumReceitaByPeriod(
                    dataAtual.atStartOfDay(), dataAtual.atTime(23, 59, 59), Cobranca.StatusCobranca.PAGO);
            dados.add(receita != null ? receita : BigDecimal.ZERO);

            dataAtual = dataAtual.plusDays(1);
        }

        DashboardDTO.SerieGraficoDTO serie = DashboardDTO.SerieGraficoDTO.builder()
                .nome("Receita Diária")
                .dados(dados)
                .cor("#2563eb")
                .build();

        return DashboardDTO.GraficoDTO.builder()
                .tipo("linha")
                .titulo("Receita por Dia")
                .labels(labels)
                .series(Collections.singletonList(serie))
                .build();
    }

    private DashboardDTO.GraficoDTO criarGraficoAgendamentosPorStatus(LocalDate dataInicio, LocalDate dataFim) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        List<Agendamento> agendamentos = agendamentoRepository.findByDataRange(inicio, fim);

        Map<String, Long> statusCount = agendamentos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus().getDescricao(),
                        Collectors.counting()
                ));

        List<String> labels = new ArrayList<>(statusCount.keySet());
        List<Object> dados = new ArrayList<>(statusCount.values());

        DashboardDTO.SerieGraficoDTO serie = DashboardDTO.SerieGraficoDTO.builder()
                .nome("Agendamentos")
                .dados(dados)
                .cor("#10b981")
                .build();

        return DashboardDTO.GraficoDTO.builder()
                .tipo("pizza")
                .titulo("Agendamentos por Status")
                .labels(labels)
                .series(Collections.singletonList(serie))
                .build();
    }

    private DashboardDTO.GraficoDTO criarGraficoServicosMaisVendidos(LocalDate dataInicio, LocalDate dataFim) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        List<ServicoTopDTO> servicosTop = getServicosMaisVendidos(inicio, fim);

        List<String> labels = servicosTop.stream()
                .map(ServicoTopDTO::getNome)
                .collect(Collectors.toList());

        List<Object> dados = servicosTop.stream()
                .map(ServicoTopDTO::getQuantidadeVendida)
                .collect(Collectors.toList());

        DashboardDTO.SerieGraficoDTO serie = DashboardDTO.SerieGraficoDTO.builder()
                .nome("Quantidade Vendida")
                .dados(dados)
                .cor("#f59e0b")
                .build();

        return DashboardDTO.GraficoDTO.builder()
                .tipo("barra")
                .titulo("Serviços Mais Vendidos")
                .labels(labels)
                .series(Collections.singletonList(serie))
                .build();
    }

    private DashboardDTO.GraficoDTO criarGraficoPerformanceFuncionarios(LocalDate dataInicio, LocalDate dataFim) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        List<Funcionario> funcionarios = funcionarioRepository.findByAtivo(true);

        List<String> labels = funcionarios.stream()
                .map(Funcionario::getNomeCompleto)
                .collect(Collectors.toList());

        List<Object> dadosAgendamentos = funcionarios.stream()
                .map(f -> agendamentoRepository.countByFuncionarioAndDataRange(f.getId(), inicio, fim))
                .collect(Collectors.toList());

        List<Object> dadosReceita = funcionarios.stream()
                .map(f -> calcularReceitaFuncionario(f.getId(), inicio, fim))
                .collect(Collectors.toList());

        List<DashboardDTO.SerieGraficoDTO> series = Arrays.asList(
                DashboardDTO.SerieGraficoDTO.builder()
                        .nome("Agendamentos")
                        .dados(dadosAgendamentos)
                        .cor("#3b82f6")
                        .build(),
                DashboardDTO.SerieGraficoDTO.builder()
                        .nome("Receita")
                        .dados(dadosReceita)
                        .cor("#10b981")
                        .build()
        );

        return DashboardDTO.GraficoDTO.builder()
                .tipo("barra")
                .titulo("Performance dos Funcionários")
                .labels(labels)
                .series(series)
                .build();
    }

    // Métodos para criação de tendências
    private DashboardDTO.TendenciaDTO criarTendenciaReceita(LocalDate inicioAtual, LocalDate fimAtual,
                                                            LocalDate inicioAnterior, LocalDate fimAnterior) {
        BigDecimal receitaAtual = cobrancaRepository.sumReceitaByPeriod(
                inicioAtual.atStartOfDay(), fimAtual.atTime(23, 59, 59), Cobranca.StatusCobranca.PAGO);
        BigDecimal receitaAnterior = cobrancaRepository.sumReceitaByPeriod(
                inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59), Cobranca.StatusCobranca.PAGO);

        if (receitaAtual == null) receitaAtual = BigDecimal.ZERO;
        if (receitaAnterior == null) receitaAnterior = BigDecimal.ZERO;

        Double percentualMudanca = 0.0;
        String tendencia = "ESTAVEL";

        if (receitaAnterior.compareTo(BigDecimal.ZERO) > 0) {
            percentualMudanca = receitaAtual.subtract(receitaAnterior)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(receitaAnterior, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            if (percentualMudanca > 5) {
                tendencia = "ALTA";
            } else if (percentualMudanca < -5) {
                tendencia = "BAIXA";
            }
        }

        return DashboardDTO.TendenciaDTO.builder()
                .metrica("Receita")
                .valorAtual(receitaAtual)
                .valorAnterior(receitaAnterior)
                .percentualMudanca(percentualMudanca)
                .tendencia(tendencia)
                .periodo(formatarPeriodo(inicioAtual, fimAtual))
                .build();
    }

    private DashboardDTO.TendenciaDTO criarTendenciaAgendamentos(LocalDate inicioAtual, LocalDate fimAtual,
                                                                 LocalDate inicioAnterior, LocalDate fimAnterior) {
        Long agendamentosAtual = agendamentoRepository.countByDataRange(
                inicioAtual.atStartOfDay(), fimAtual.atTime(23, 59, 59));
        Long agendamentosAnterior = agendamentoRepository.countByDataRange(
                inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59));

        Double percentualMudanca = 0.0;
        String tendencia = "ESTAVEL";

        if (agendamentosAnterior > 0) {
            percentualMudanca = ((agendamentosAtual - agendamentosAnterior) * 100.0) / agendamentosAnterior;

            if (percentualMudanca > 10) {
                tendencia = "ALTA";
            } else if (percentualMudanca < -10) {
                tendencia = "BAIXA";
            }
        }

        return DashboardDTO.TendenciaDTO.builder()
                .metrica("Agendamentos")
                .valorAtual(BigDecimal.valueOf(agendamentosAtual))
                .valorAnterior(BigDecimal.valueOf(agendamentosAnterior))
                .percentualMudanca(percentualMudanca)
                .tendencia(tendencia)
                .periodo(formatarPeriodo(inicioAtual, fimAtual))
                .build();
    }

    private DashboardDTO.TendenciaDTO criarTendenciaNovosClientes(LocalDate inicioAtual, LocalDate fimAtual,
                                                                  LocalDate inicioAnterior, LocalDate fimAnterior) {
        Long clientesAtual = clienteRepository.countByDataCriacaoBetween(
                inicioAtual.atStartOfDay(), fimAtual.atTime(23, 59, 59));
        Long clientesAnterior = clienteRepository.countByDataCriacaoBetween(
                inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59));

        Double percentualMudanca = 0.0;
        String tendencia = "ESTAVEL";

        if (clientesAnterior > 0) {
            percentualMudanca = ((clientesAtual - clientesAnterior) * 100.0) / clientesAnterior;

            if (percentualMudanca > 15) {
                tendencia = "ALTA";
            } else if (percentualMudanca < -15) {
                tendencia = "BAIXA";
            }
        }

        return DashboardDTO.TendenciaDTO.builder()
                .metrica("Novos Clientes")
                .valorAtual(BigDecimal.valueOf(clientesAtual))
                .valorAnterior(BigDecimal.valueOf(clientesAnterior))
                .percentualMudanca(percentualMudanca)
                .tendencia(tendencia)
                .periodo(formatarPeriodo(inicioAtual, fimAtual))
                .build();
    }

    private List<DashboardDTO.TendenciaDTO> criarComparativos(DashboardDTO atual, DashboardDTO anterior) {
        List<DashboardDTO.TendenciaDTO> comparativos = new ArrayList<>();

        // Comparativo de receita total
        BigDecimal receitaAtual = atual.getFinanceiro().getReceitaTotal();
        BigDecimal receitaAnterior = anterior.getFinanceiro().getReceitaTotal();

        Double percentualReceita = calcularPercentualMudanca(receitaAtual, receitaAnterior);

        comparativos.add(DashboardDTO.TendenciaDTO.builder()
                .metrica("Receita Total")
                .valorAtual(receitaAtual)
                .valorAnterior(receitaAnterior)
                .percentualMudanca(percentualReceita)
                .tendencia(definirTendencia(percentualReceita, 5.0))
                .periodo("Comparativo")
                .build());

        // Comparativo de agendamentos
        Long agendamentosAtual = atual.getAgendamentos().getTotal();
        Long agendamentosAnterior = anterior.getAgendamentos().getTotal();

        Double percentualAgendamentos = calcularPercentualMudanca(
                BigDecimal.valueOf(agendamentosAtual),
                BigDecimal.valueOf(agendamentosAnterior));

        comparativos.add(DashboardDTO.TendenciaDTO.builder()
                .metrica("Total de Agendamentos")
                .valorAtual(BigDecimal.valueOf(agendamentosAtual))
                .valorAnterior(BigDecimal.valueOf(agendamentosAnterior))
                .percentualMudanca(percentualAgendamentos)
                .tendencia(definirTendencia(percentualAgendamentos, 10.0))
                .periodo("Comparativo")
                .build());

        // Comparativo de novos clientes
        Long novosClientesAtual = atual.getClientes().getNovosClientesEsteMes();
        Long novosClientesAnterior = anterior.getClientes().getNovosClientesEsteMes();

        Double percentualClientes = calcularPercentualMudanca(
                BigDecimal.valueOf(novosClientesAtual),
                BigDecimal.valueOf(novosClientesAnterior));

        comparativos.add(DashboardDTO.TendenciaDTO.builder()
                .metrica("Novos Clientes")
                .valorAtual(BigDecimal.valueOf(novosClientesAtual))
                .valorAnterior(BigDecimal.valueOf(novosClientesAnterior))
                .percentualMudanca(percentualClientes)
                .tendencia(definirTendencia(percentualClientes, 15.0))
                .periodo("Comparativo")
                .build());

        return comparativos;
    }

    private Double calcularPercentualMudanca(BigDecimal valorAtual, BigDecimal valorAnterior) {
        if (valorAnterior == null || valorAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return valorAtual != null && valorAtual.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }

        return valorAtual.subtract(valorAnterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(valorAnterior, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String definirTendencia(Double percentual, Double limite) {
        if (percentual > limite) {
            return "ALTA";
        } else if (percentual < -limite) {
            return "BAIXA";
        } else {
            return "ESTAVEL";
        }
    }
}
