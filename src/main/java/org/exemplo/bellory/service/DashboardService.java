package org.exemplo.bellory.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.dashboard.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Servico;
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
import java.time.temporal.ChronoUnit;
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


    @Transactional
    public DashboardDTO getDashboardGeral(DashboardFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().minusDays(30);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();

        return DashboardDTO.builder()
                .dataConsulta(LocalDate.now())
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .agendamentos(getAgendamentosResumo(dataInicio, dataFim, filtro, organizacaoId))
                .financeiro(getFinanceiroResumo(dataInicio, dataFim, filtro, organizacaoId))
                .clientes(getClientesResumo(dataInicio, dataFim, filtro, organizacaoId))
                .estoque(getEstoqueResumo(filtro, organizacaoId))
                .funcionarios(getFuncionariosResumo(dataInicio, dataFim, filtro, organizacaoId))
                .vendas(getVendasResumo(dataInicio, dataFim, filtro, organizacaoId))
                .graficos(getGraficos(dataInicio, dataFim, filtro, organizacaoId))
                .tendencias(getTendencias(dataInicio, dataFim, filtro, organizacaoId))
                .build();
    }

    public DashboardComparativoDTO getDashboardComparativo(LocalDate inicioAtual, LocalDate fimAtual,
                                                           LocalDate inicioAnterior, LocalDate fimAnterior) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        DashboardFiltroDTO filtroAtual = DashboardFiltroDTO.builder()
                .dataInicio(inicioAtual)
                .dataFim(fimAtual)
                .build();

        DashboardFiltroDTO filtroAnterior = DashboardFiltroDTO.builder()
                .dataInicio(inicioAnterior)
                .dataFim(fimAnterior)
                .build();

        DashboardDTO dashboardAtual = getDashboardGeral(filtroAtual);
        DashboardDTO dashboardAnterior = getDashboardGeral(filtroAnterior);

        return DashboardComparativoDTO.builder()
                .periodoAtual(dashboardAtual)
                .periodoAnterior(dashboardAnterior)
                .comparativos(criarComparativos(dashboardAtual, dashboardAnterior))
                .descricaoPeriodos(formatarPeriodo(inicioAtual, fimAtual) + " vs " + formatarPeriodo(inicioAnterior, fimAnterior))
                .build();
    }

    public FuncionarioMetricasDTO getMetricasFuncionario(Long funcionarioId, LocalDate inicio, LocalDate fim) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        if (!funcionario.getOrganizacao().getId().equals(organizacaoId)) {
            throw new SecurityException("Acesso negado: Este funcionário não pertence à sua organização");
        }

        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime fimDateTime = fim.atTime(23, 59, 59);

        List<Agendamento> agendamentos = agendamentoRepository.findByFuncionarioAndDataRange(
                funcionarioId, inicioDateTime, fimDateTime);

        Long totalAtendimentos = (long) agendamentos.size();
        Long atendimentosConcluidos = agendamentos.stream()
                .filter(a -> a.getStatus() == Status.CONCLUIDO)
                .count();
        Long atendimentosCancelados = agendamentos.stream()
                .filter(a -> a.getStatus() == Status.CANCELADO)
                .count();

        BigDecimal receitaGerada = agendamentos.stream()
                .filter(a -> a.getCobrancas() != null && !a.getCobrancas().isEmpty())
                .flatMap(a -> a.getCobrancas().stream())
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double taxaConclusao = totalAtendimentos > 0 ?
                (atendimentosConcluidos * 100.0) / totalAtendimentos : 0.0;

        return FuncionarioMetricasDTO.builder()
                .funcionarioId(funcionarioId)
                .nomeFuncionario(funcionario.getNomeCompleto())
                .periodo(formatarPeriodo(inicio, fim))
                .totalAtendimentos(totalAtendimentos)
                .atendimentosConcluidos(atendimentosConcluidos)
                .atendimentosCancelados(atendimentosCancelados)
                .receitaGerada(receitaGerada)
                .taxaConclusao(taxaConclusao)
                .mediaAtendimentosPorDia(calcularMediaAtendimentosPorDia(totalAtendimentos, inicio, fim))
                .taxaOcupacao(calcularTaxaOcupacaoFuncionario(funcionarioId, inicio, fim))
                .build();
    }


    // ==================== AGENDAMENTOS ====================
    private DashboardDTO.AgendamentosResumoDTO getAgendamentosResumo(LocalDate dataInicio, LocalDate dataFim,
                                                                      DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Buscar agendamentos com detalhes (evita N+1)
        List<Agendamento> agendamentos;
        if (filtro.getFuncionarioId() != null) {
            validarFuncionarioOrganizacao(filtro.getFuncionarioId(), organizacaoId);
            agendamentos = agendamentoRepository.findByFuncionarioAndDataRange(
                    filtro.getFuncionarioId(), inicioDateTime, fimDateTime);
        } else {
            agendamentos = agendamentoRepository.findByOrganizacaoAndPeriodoWithDetails(
                    organizacaoId, inicioDateTime, fimDateTime);
        }

        // Contadores por status usando query otimizada
        Map<String, Long> porStatus = new HashMap<>();
        List<Object[]> statusCounts = agendamentoRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : statusCounts) {
            Status status = (Status) row[0];
            Long count = (Long) row[1];
            porStatus.put(status.name(), count);
        }

        // Contagens otimizadas
        LocalDateTime hojeInicio = LocalDate.now().atStartOfDay();
        LocalDateTime hojeFim = LocalDate.now().atTime(23, 59, 59);
        Long agendamentosHoje = agendamentoRepository.countByOrganizacaoAndPeriodo(organizacaoId, hojeInicio, hojeFim);

        LocalDate inicioSemana = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = inicioSemana.plusDays(6);
        Long agendamentosEstaSemana = agendamentoRepository.countByOrganizacaoAndPeriodo(
                organizacaoId, inicioSemana.atStartOfDay(), fimSemana.atTime(23, 59, 59));

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        Long agendamentosEsteMes = agendamentoRepository.countByOrganizacaoAndPeriodo(
                organizacaoId, inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59));

        // Taxa de ocupação
        Double taxaOcupacao = calcularTaxaOcupacao(agendamentos);

        // Taxa de cancelamento
        long totalAgendamentos = agendamentos.size();
        long cancelados = porStatus.getOrDefault("CANCELADO", 0L);
        Double taxaCancelamento = totalAgendamentos > 0 ?
                (cancelados * 100.0) / totalAgendamentos : 0.0;

        // Próximos agendamentos (próximos 7 dias)
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime proximaSemana = agora.plusDays(7).withHour(23).withMinute(59).withSecond(59);
        List<Agendamento> proximosAgendamentosList = agendamentoRepository.findProximosAgendamentosByOrganizacaoId(
                agora, proximaSemana, organizacaoId);
        Long proximosAgendamentos = (long) proximosAgendamentosList.size();

        // Agendamentos recentes (últimos 10)
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

    // ==================== FINANCEIRO ====================
    private DashboardDTO.FinanceiroResumoDTO getFinanceiroResumo(LocalDate dataInicio, LocalDate dataFim,
                                                                  DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Receitas usando queries otimizadas (sem filtro em memória)
        BigDecimal receitaTotal = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioDateTime, fimDateTime, organizacaoId);
        if (receitaTotal == null) receitaTotal = BigDecimal.ZERO;

        // Receita hoje
        LocalDateTime hojeInicio = LocalDate.now().atStartOfDay();
        LocalDateTime hojeFim = LocalDate.now().atTime(23, 59, 59);
        BigDecimal receitaHoje = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(hojeInicio, hojeFim, organizacaoId);
        if (receitaHoje == null) receitaHoje = BigDecimal.ZERO;

        // Receita este mês
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        BigDecimal receitaEsteMes = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59), organizacaoId);
        if (receitaEsteMes == null) receitaEsteMes = BigDecimal.ZERO;

        // Receita este ano
        LocalDate inicioAno = LocalDate.now().withDayOfYear(1);
        LocalDate fimAno = inicioAno.plusYears(1).minusDays(1);
        BigDecimal receitaEsteAno = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioAno.atStartOfDay(), fimAno.atTime(23, 59, 59), organizacaoId);
        if (receitaEsteAno == null) receitaEsteAno = BigDecimal.ZERO;

        // Receita prevista (agendamentos futuros)
        BigDecimal receitaPrevista = calcularReceitaPrevista(organizacaoId);

        // Contagem e valores por status
        Map<String, Long> contagemPorStatus = new HashMap<>();
        Map<String, BigDecimal> valorPorStatus = new HashMap<>();

        List<Object[]> contagens = cobrancaRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : contagens) {
            Cobranca.StatusCobranca status = (Cobranca.StatusCobranca) row[0];
            Long count = (Long) row[1];
            contagemPorStatus.put(status.name(), count);
        }

        List<Object[]> valores = cobrancaRepository.sumValorByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : valores) {
            Cobranca.StatusCobranca status = (Cobranca.StatusCobranca) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            valorPorStatus.put(status.name(), valor != null ? valor : BigDecimal.ZERO);
        }

        Long totalCobrancas = contagemPorStatus.values().stream().reduce(0L, Long::sum);
        Long cobrancasPagas = contagemPorStatus.getOrDefault("PAGO", 0L);
        Long cobrancasPendentes = contagemPorStatus.getOrDefault("PENDENTE", 0L) +
                contagemPorStatus.getOrDefault("PARCIALMENTE_PAGO", 0L);
        Long cobrancasVencidas = contagemPorStatus.getOrDefault("VENCIDA", 0L);

        BigDecimal contasReceber = cobrancaRepository.sumValorPendenteByOrganizacao(organizacaoId);
        if (contasReceber == null) contasReceber = BigDecimal.ZERO;

        BigDecimal contasVencidas = cobrancaRepository.sumValorVencidoByOrganizacao(organizacaoId);
        if (contasVencidas == null) contasVencidas = BigDecimal.ZERO;

        // Ticket médio
        BigDecimal ticketMedio = cobrancasPagas > 0 ?
                receitaTotal.divide(BigDecimal.valueOf(cobrancasPagas), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Percentual de recebimento
        BigDecimal valorTotal = valorPorStatus.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Double percentualRecebimento = valorTotal.compareTo(BigDecimal.ZERO) > 0 ?
                receitaTotal.multiply(BigDecimal.valueOf(100)).divide(valorTotal, 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        // Formas de pagamento
        Map<String, Long> formasPagamento = calcularFormasPagamento(organizacaoId, inicioDateTime, fimDateTime);

        // Receita por serviço
        Map<String, BigDecimal> receitaPorServico = new LinkedHashMap<>();
        List<Object[]> receitaServicos = cobrancaRepository.sumReceitaByServicoAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : receitaServicos) {
            String nomeServico = (String) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            receitaPorServico.put(nomeServico, valor != null ? valor : BigDecimal.ZERO);
        }

        // Receita por funcionário
        Map<String, BigDecimal> receitaPorFuncionario = new LinkedHashMap<>();
        List<Object[]> receitaFuncionarios = cobrancaRepository.sumReceitaByFuncionarioAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : receitaFuncionarios) {
            String nomeFuncionario = (String) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            receitaPorFuncionario.put(nomeFuncionario, valor != null ? valor : BigDecimal.ZERO);
        }

        // Receita de serviços vs produtos
        BigDecimal receitaServicos2 = BigDecimal.ZERO;
        BigDecimal receitaProdutos = BigDecimal.ZERO;
        List<Object[]> receitaPorTipo = cobrancaRepository.sumReceitaByTipoAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : receitaPorTipo) {
            Cobranca.TipoCobranca tipo = (Cobranca.TipoCobranca) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            if (tipo == Cobranca.TipoCobranca.AGENDAMENTO) {
                receitaServicos2 = valor != null ? valor : BigDecimal.ZERO;
            } else if (tipo == Cobranca.TipoCobranca.COMPRA) {
                receitaProdutos = valor != null ? valor : BigDecimal.ZERO;
            }
        }

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
                .receitaServicos(receitaServicos2)
                .totalCobrancas(totalCobrancas)
                .cobrancasPagas(cobrancasPagas)
                .cobrancasPendentes(cobrancasPendentes)
                .cobrancasVencidas(cobrancasVencidas)
                .contasReceber(contasReceber)
                .contasVencidas(contasVencidas)
                .valorPendente(contasReceber)
                .valorVencido(contasVencidas)
                .percentualRecebimento(percentualRecebimento)
                .totalTransacoes(totalCobrancas)
                .formasPagamento(formasPagamento)
                .build();
    }

    // ==================== CLIENTES ====================
    private DashboardDTO.ClientesResumoDTO getClientesResumo(LocalDate dataInicio, LocalDate dataFim,
                                                              DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Total de clientes ativos
        Long totalClientes = clienteRepository.countByOrganizacao_IdAndAtivoTrue(organizacaoId);

        // Novos clientes no período
        Long novosClientes = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicioDateTime, fimDateTime);

        // Novos clientes hoje
        LocalDateTime hojeInicio = LocalDate.now().atStartOfDay();
        LocalDateTime hojeFim = LocalDate.now().atTime(23, 59, 59);
        Long novosClientesHoje = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, hojeInicio, hojeFim);

        // Novos clientes este mês
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        Long novosClientesEsteMes = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59));

        // Clientes ativos (com agendamentos no período) - query otimizada
        Long clientesAtivos = agendamentoRepository.countClientesDistintosComAgendamentos(
                organizacaoId, inicioDateTime, fimDateTime);

        // Clientes recorrentes (mais de 1 agendamento no período) - query otimizada
        Long clientesRecorrentes = agendamentoRepository.countClientesRecorrentesByOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);

        // Taxa de retenção
        Double taxaRetencao = clientesAtivos > 0 ?
                (clientesRecorrentes * 100.0) / clientesAtivos : 0.0;

        // Clientes inativos (sem agendamentos há 90 dias) - query otimizada (evita N+1)
        LocalDateTime dataLimiteInatividade = LocalDateTime.now().minusDays(90);
        Long clientesInativos = agendamentoRepository.countClientesInativos(organizacaoId, dataLimiteInatividade);

        // Ticket médio por cliente
        BigDecimal ticketMedio = clienteRepository.findTicketMedioByOrganizacao(organizacaoId);
        Double ticketMedioCliente = ticketMedio != null ? ticketMedio.doubleValue() : 0.0;

        // Aniversariantes
        Long aniversariantesHoje = clienteRepository.countAniversariantesHojeByOrganizacao(organizacaoId);

        // Aniversariantes esta semana
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = inicioSemana.plusDays(6);
        Long aniversariantesEstaSemana = 0L;
        if (inicioSemana.getMonth() == fimSemana.getMonth()) {
            aniversariantesEstaSemana = clienteRepository.countAniversariantesEstaSemanaByOrganizacaoOptimized(
                    organizacaoId, hoje.getMonthValue(), inicioSemana.getDayOfMonth(), fimSemana.getDayOfMonth());
        }

        // Aniversariantes do mês
        Long aniversariantesMes = clienteRepository.countAniversariantesDoMesByOrganizacao(
                organizacaoId, LocalDate.now().getMonthValue());

        // Top clientes
        List<ClienteTopDTO> topClientes = new ArrayList<>();
        List<Object[]> topClientesData = clienteRepository.findTopClientesByOrganizacao(organizacaoId);
        int count = 0;
        for (Object[] row : topClientesData) {
            if (count >= 10) break;
            topClientes.add(ClienteTopDTO.builder()
                    .id((Long) row[0])
                    .nome((String) row[1])
                    .valorGasto((BigDecimal) row[2])
                    .totalAgendamentos((Long) row[3])
                    .build());
            count++;
        }

        return DashboardDTO.ClientesResumoDTO.builder()
                .totalClientes(totalClientes)
                .novosClientes(novosClientes)
                .novosClientesHoje(novosClientesHoje)
                .novosClientesEsteMes(novosClientesEsteMes)
                .clientesAtivos(clientesAtivos)
                .clientesInativos(clientesInativos)
                .clientesRecorrentes(clientesRecorrentes)
                .taxaRetencao(taxaRetencao)
                .ticketMedioCliente(ticketMedioCliente)
                .topClientes(topClientes)
                .clientesAniversarioHoje(aniversariantesHoje)
                .clientesAniversarioEstaSemana(aniversariantesEstaSemana)
                .aniversariantesMes(aniversariantesMes)
                .build();
    }

    // ==================== ESTOQUE ====================
    private DashboardDTO.EstoqueResumoDTO getEstoqueResumo(DashboardFiltroDTO filtro, Long organizacaoId) {
        // Contagens otimizadas
        Long totalProdutos = produtoRepository.countProdutosAtivosByOrganizacao(organizacaoId);
        Long produtosEstoqueBaixo = produtoRepository.countProdutosEstoqueBaixoByOrganizacao(organizacaoId);
        Long produtosSemEstoque = produtoRepository.countProdutosSemEstoqueByOrganizacao(organizacaoId);

        // Valor total do estoque
        BigDecimal valorTotalEstoque = produtoRepository.calcularValorTotalEstoqueByOrganizacao(organizacaoId);
        if (valorTotalEstoque == null) valorTotalEstoque = BigDecimal.ZERO;

        // Produtos com estoque baixo (lista)
        List<Produto> produtosBaixoEstoqueList = produtoRepository.findProdutosEstoqueBaixoByOrganizacao(organizacaoId);
        List<ProdutoEstoqueDTO> produtosBaixoEstoque = produtosBaixoEstoqueList.stream()
                .limit(10)
                .map(p -> ProdutoEstoqueDTO.builder()
                        .id(p.getId())
                        .nome(p.getNome())
                        .quantidadeAtual(p.getQuantidadeEstoque())
                        .estoqueMinimo(p.getEstoqueMinimo())
                        .categoria(p.getCategoria() != null ? p.getCategoria().getLabel() : null)
                        .status(determinarStatusEstoque(p))
                        .build())
                .collect(Collectors.toList());

        // Produtos mais vendidos (últimos 30 dias)
        List<ProdutoTopDTO> produtosMaisVendidos = getProdutosMaisVendidos(organizacaoId, 30, 5);

        // Alertas de estoque
        Long alertasEstoque = produtoRepository.countAlertasEstoqueByOrganizacao(organizacaoId);

        // Valor do estoque parado (produtos sem movimento há 60 dias)
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(60);
        BigDecimal valorEstoqueParado = produtoRepository.calcularValorEstoqueParadoByOrganizacao(organizacaoId, dataLimite);
        if (valorEstoqueParado == null) valorEstoqueParado = BigDecimal.ZERO;

        // Giro de estoque (simplificado)
        Double giroEstoque = 0.0;
        if (valorTotalEstoque.compareTo(BigDecimal.ZERO) > 0) {
            // Custo das vendas / Estoque médio (simplificado)
            giroEstoque = 12.0; // placeholder - implementar cálculo real se necessário
        }

        return DashboardDTO.EstoqueResumoDTO.builder()
                .totalProdutos(totalProdutos)
                .produtosAtivos(totalProdutos)
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

    // ==================== FUNCIONÁRIOS ====================
    private DashboardDTO.FuncionariosResumoDTO getFuncionariosResumo(LocalDate dataInicio, LocalDate dataFim,
                                                                      DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Total de funcionários ativos
        List<Funcionario> funcionarios = funcionarioRepository.findAllByOrganizacao_Id(organizacaoId);
        Long totalFuncionarios = funcionarios.stream().filter(Funcionario::isAtivo).count();
        Long funcionariosAtivos = totalFuncionarios;

        // Atendimentos por funcionário (query otimizada)
        Map<String, Long> agendamentosPorFuncionario = new LinkedHashMap<>();
        List<Object[]> atendimentosPorFunc = agendamentoRepository.countByFuncionarioAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);

        FuncionarioTopDTO funcionarioMaisAtendimentos = null;
        for (Object[] row : atendimentosPorFunc) {
            Long funcId = (Long) row[0];
            String nome = (String) row[1];
            Long total = (Long) row[2];
            agendamentosPorFuncionario.put(nome, total);

            if (funcionarioMaisAtendimentos == null) {
                funcionarioMaisAtendimentos = FuncionarioTopDTO.builder()
                        .id(funcId)
                        .nome(nome)
                        .totalAtendimentos(total)
                        .build();
            }
        }

        // Receita por funcionário
        Map<String, BigDecimal> receitaPorFuncionario = new LinkedHashMap<>();
        List<Object[]> receitaPorFunc = cobrancaRepository.sumReceitaByFuncionarioAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);

        FuncionarioTopDTO funcionarioMaisReceita = null;
        for (Object[] row : receitaPorFunc) {
            String nome = (String) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            receitaPorFuncionario.put(nome, valor != null ? valor : BigDecimal.ZERO);

            if (funcionarioMaisReceita == null && valor != null) {
                Funcionario func = funcionarios.stream()
                        .filter(f -> f.getNomeCompleto().equals(nome))
                        .findFirst().orElse(null);
                if (func != null) {
                    funcionarioMaisReceita = FuncionarioTopDTO.builder()
                            .id(func.getId())
                            .nome(nome)
                            .receitaGerada(valor)
                            .build();
                }
            }
        }

        // Top performers
        List<FuncionarioPerformanceDTO> topPerformers = new ArrayList<>();
        for (Object[] row : atendimentosPorFunc) {
            if (topPerformers.size() >= 5) break;
            Long funcId = (Long) row[0];
            String nome = (String) row[1];
            Long totalAtendimentos = (Long) row[2];
            BigDecimal receita = receitaPorFuncionario.getOrDefault(nome, BigDecimal.ZERO);

            topPerformers.add(FuncionarioPerformanceDTO.builder()
                    .id(funcId)
                    .nome(nome)
                    .totalAgendamentos(totalAtendimentos)
                    .receitaGerada(receita)
                    .status("ATIVO")
                    .build());
        }

        // Taxa média de ocupação
        Double taxaOcupacaoMedia = calcularTaxaOcupacaoMedia(organizacaoId, dataInicio, dataFim);

        // Produtividade média (atendimentos por funcionário)
        Double produtividadeMedia = funcionariosAtivos > 0 ?
                (double) agendamentosPorFuncionario.values().stream().reduce(0L, Long::sum) / funcionariosAtivos : 0.0;

        return DashboardDTO.FuncionariosResumoDTO.builder()
                .totalFuncionarios(totalFuncionarios)
                .funcionariosAtivos(funcionariosAtivos)
                .produtividadeMedia(produtividadeMedia)
                .topPerformers(topPerformers)
                .agendamentosPorFuncionario(agendamentosPorFuncionario)
                .receitaPorFuncionario(receitaPorFuncionario)
                .ocupacaoMediaFuncionarios(taxaOcupacaoMedia)
                .funcionarioMaisAtendimentos(funcionarioMaisAtendimentos)
                .funcionarioMaisReceita(funcionarioMaisReceita)
                .taxaOcupacaoMedia(taxaOcupacaoMedia)
                .build();
    }

    // ==================== VENDAS ====================
    private DashboardDTO.VendasResumoDTO getVendasResumo(LocalDate dataInicio, LocalDate dataFim,
                                                          DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Total de vendas (agendamentos concluídos)
        Long totalVendas = agendamentoRepository.countVendasByOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);

        // Valor total vendido
        BigDecimal valorTotalVendido = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioDateTime, fimDateTime, organizacaoId);
        if (valorTotalVendido == null) valorTotalVendido = BigDecimal.ZERO;

        // Vendas hoje
        LocalDateTime hojeInicio = LocalDate.now().atStartOfDay();
        LocalDateTime hojeFim = LocalDate.now().atTime(23, 59, 59);
        Long vendasHoje = agendamentoRepository.countVendasByOrganizacaoAndPeriodo(
                organizacaoId, hojeInicio, hojeFim);

        BigDecimal valorVendasHoje = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                hojeInicio, hojeFim, organizacaoId);
        if (valorVendasHoje == null) valorVendasHoje = BigDecimal.ZERO;

        // Serviço mais vendido
        ServicoTopDTO servicoMaisVendido = null;
        List<Object[]> servicosVendidos = agendamentoRepository.countServicosMaisVendidosByOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        if (!servicosVendidos.isEmpty()) {
            Object[] top = servicosVendidos.get(0);
            servicoMaisVendido = ServicoTopDTO.builder()
                    .id((Long) top[0])
                    .nome((String) top[1])
                    .quantidadeVendida((Long) top[2])
                    .build();
        }

        // Vendas por categoria
        Map<String, Long> vendasPorCategoria = new LinkedHashMap<>();
        List<Object[]> vendasCat = agendamentoRepository.countVendasByCategoriaAndOrganizacao(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : vendasCat) {
            String categoria = row[0] != null ? (String) row[0] : "Sem categoria";
            Long quantidade = (Long) row[1];
            vendasPorCategoria.put(categoria, quantidade);
        }

        // Ticket médio de venda
        Double ticketMedioVenda = totalVendas > 0 ?
                valorTotalVendido.divide(BigDecimal.valueOf(totalVendas), 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        // Pedidos por status (através de agendamentos)
        List<Object[]> statusCounts = agendamentoRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        Long pedidosPendentes = 0L;
        Long pedidosEntregues = 0L;
        Long pedidosCancelados = 0L;
        for (Object[] row : statusCounts) {
            Status status = (Status) row[0];
            Long count = (Long) row[1];
            if (status == Status.PENDENTE || status == Status.AGENDADO || status == Status.EM_ESPERA) {
                pedidosPendentes += count;
            } else if (status == Status.CONCLUIDO) {
                pedidosEntregues += count;
            } else if (status == Status.CANCELADO) {
                pedidosCancelados += count;
            }
        }

        // Crescimento de vendas (comparado com período anterior)
        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo + 1);
        LocalDate fimAnterior = dataInicio.minusDays(1);

        BigDecimal valorAnterior = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59), organizacaoId);
        if (valorAnterior == null) valorAnterior = BigDecimal.ZERO;

        Double crescimentoVendas = 0.0;
        if (valorAnterior.compareTo(BigDecimal.ZERO) > 0) {
            crescimentoVendas = valorTotalVendido.subtract(valorAnterior)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(valorAnterior, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return DashboardDTO.VendasResumoDTO.builder()
                .totalVendas(totalVendas)
                .valorTotalVendas(valorTotalVendido)
                .vendasHoje(vendasHoje)
                .valorVendasHoje(valorVendasHoje)
                .pedidosPendentes(pedidosPendentes)
                .pedidosEntregues(pedidosEntregues)
                .valorTotalVendido(valorTotalVendido)
                .pedidosCancelados(pedidosCancelados)
                .ticketMedioVenda(ticketMedioVenda)
                .servicosMaisVendidos(servicoMaisVendido)
                .vendasPorCategoria(vendasPorCategoria)
                .crescimentoVendas(crescimentoVendas)
                .build();
    }

    // ==================== GRÁFICOS ====================
    private DashboardDTO.GraficosDTO getGraficos(LocalDate dataInicio, LocalDate dataFim,
                                                  DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Receita por período
        List<GraficoReceitaDTO> receitaPorPeriodo = calcularReceitaPorPeriodo(dataInicio, dataFim, organizacaoId);

        // Agendamentos por status
        Map<String, Long> agendamentosPorStatus = new LinkedHashMap<>();
        List<Object[]> statusCounts = agendamentoRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : statusCounts) {
            Status status = (Status) row[0];
            Long count = (Long) row[1];
            agendamentosPorStatus.put(status.name(), count);
        }

        // Serviços mais procurados
        Map<String, Long> servicosMaisProcurados = new LinkedHashMap<>();
        List<Object[]> servicosVendidos = agendamentoRepository.countServicosMaisVendidosByOrganizacaoAndPeriodo(
                organizacaoId, inicioDateTime, fimDateTime);
        for (Object[] row : servicosVendidos) {
            if (servicosMaisProcurados.size() >= 10) break;
            String nome = (String) row[1];
            Long quantidade = (Long) row[2];
            servicosMaisProcurados.put(nome, quantidade);
        }

        return DashboardDTO.GraficosDTO.builder()
                .receitaPorPeriodo(receitaPorPeriodo)
                .agendamentosPorStatus(agendamentosPorStatus)
                .servicosMaisProcurados(servicosMaisProcurados)
                .build();
    }

    // ==================== TENDÊNCIAS ====================
    private DashboardDTO.TendenciasDTO getTendencias(LocalDate dataInicio, LocalDate dataFim,
                                                      DashboardFiltroDTO filtro, Long organizacaoId) {
        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo + 1);
        LocalDate fimAnterior = dataInicio.minusDays(1);

        List<DashboardDTO.TendenciaDTO> tendencias = new ArrayList<>();

        tendencias.add(criarTendenciaReceita(dataInicio, dataFim, inicioAnterior, fimAnterior, organizacaoId));
        tendencias.add(criarTendenciaAgendamentos(dataInicio, dataFim, inicioAnterior, fimAnterior, organizacaoId));
        tendencias.add(criarTendenciaNovosClientes(dataInicio, dataFim, inicioAnterior, fimAnterior, organizacaoId));

        return DashboardDTO.TendenciasDTO.builder()
                .tendencias(tendencias)
                .build();
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void validarFiltro(DashboardFiltroDTO filtro) {
        if (filtro.getDataInicio() != null && filtro.getDataFim() != null) {
            if (filtro.getDataInicio().isAfter(filtro.getDataFim())) {
                throw new IllegalArgumentException("Data de início não pode ser posterior à data de fim.");
            }
        }
    }

    private void validarFuncionarioOrganizacao(Long funcionarioId, Long organizacaoId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        if (!funcionario.getOrganizacao().getId().equals(organizacaoId)) {
            throw new SecurityException("Acesso negado: Este funcionário não pertence à sua organização");
        }
    }

    private String formatarPeriodo(LocalDate inicio, LocalDate fim) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return inicio.format(formatter) + " a " + fim.format(formatter);
    }

    private AgendamentoRecenteDTO mapToAgendamentoRecente(Agendamento agendamento) {
        String nomeCliente = agendamento.getCliente() != null ?
                agendamento.getCliente().getNomeCompleto() : "Cliente não informado";

        String servicos = agendamento.getServicos() != null ?
                agendamento.getServicos().stream()
                        .map(Servico::getNome)
                        .collect(Collectors.joining(", ")) : "";

        BigDecimal valor = BigDecimal.ZERO;
        if (agendamento.getCobrancas() != null && !agendamento.getCobrancas().isEmpty()) {
            valor = agendamento.getCobrancas().stream()
                    .map(Cobranca::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return AgendamentoRecenteDTO.builder()
                .id(agendamento.getId())
                .clienteNome(nomeCliente)
                .dataHora(agendamento.getDtAgendamento())
                .servicoNome(servicos)
                .status(agendamento.getStatus().name())
                .valor(valor)
                .build();
    }

    private Double calcularTaxaOcupacao(List<Agendamento> agendamentos) {
        long agendadosOuConcluidos = agendamentos.stream()
                .filter(a -> a.getStatus() == Status.AGENDADO || a.getStatus() == Status.CONCLUIDO)
                .count();

        return agendamentos.size() > 0 ?
                (agendadosOuConcluidos * 100.0) / agendamentos.size() : 0.0;
    }

    private Double calcularTaxaOcupacaoFuncionario(Long funcionarioId, LocalDate inicio, LocalDate fim) {
        // Implementação simplificada - calcular baseado em horas trabalhadas vs horas disponíveis
        return 75.0;
    }

    private BigDecimal calcularReceitaPrevista(Long organizacaoId) {
        LocalDateTime agora = LocalDateTime.now();
        List<Agendamento> agendamentosFuturos = agendamentoRepository.findAgendamentosFuturosByOrganizacao(
                organizacaoId, agora);

        return agendamentosFuturos.stream()
                .filter(a -> a.getStatus() != Status.CANCELADO)
                .map(a -> {
                    if (a.getServicos() != null && !a.getServicos().isEmpty()) {
                        return a.getServicos().stream()
                                .map(s -> s.getPreco() != null ? s.getPreco() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Long> calcularFormasPagamento(Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {
        Map<String, Long> formas = new LinkedHashMap<>();
        formas.put("Dinheiro", 0L);
        formas.put("Cartão de Crédito", 0L);
        formas.put("Cartão de Débito", 0L);
        formas.put("PIX", 0L);
        formas.put("Outros", 0L);

        try {
            List<Object[]> formasPagamento = cobrancaRepository.countByFormaPagamentoAndOrganizacao(
                    organizacaoId, inicio, fim);

            for (Object[] row : formasPagamento) {
                if (row[0] != null) {
                    String forma = row[0].toString();
                    Long count = (Long) row[1];
                    formas.put(forma, count);
                }
            }
        } catch (Exception e) {
            // Em caso de erro, retorna valores zerados
        }

        return formas;
    }

    private String determinarStatusEstoque(Produto produto) {
        if (produto.getQuantidadeEstoque() == null || produto.getQuantidadeEstoque() == 0) {
            return "CRITICO";
        }
        if (produto.getEstoqueMinimo() != null && produto.getQuantidadeEstoque() <= produto.getEstoqueMinimo()) {
            return "BAIXO";
        }
        return "OK";
    }

    private List<ProdutoTopDTO> getProdutosMaisVendidos(Long organizacaoId, int dias, int limite) {
        // Implementação baseada em compras de produtos
        // Retorna lista vazia se não houver módulo de vendas de produtos implementado
        return new ArrayList<>();
    }

    private Double calcularTaxaOcupacaoMedia(Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {
        // Implementação simplificada
        return 75.0;
    }

    private List<GraficoReceitaDTO> calcularReceitaPorPeriodo(LocalDate dataInicio, LocalDate dataFim,
                                                               Long organizacaoId) {
        List<GraficoReceitaDTO> grafico = new ArrayList<>();

        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim);

        if (diasPeriodo <= 31) {
            // Gráfico diário
            for (LocalDate data = dataInicio; !data.isAfter(dataFim); data = data.plusDays(1)) {
                BigDecimal receita = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                        data.atStartOfDay(), data.atTime(23, 59, 59), organizacaoId);

                grafico.add(GraficoReceitaDTO.builder()
                        .periodo(data.format(DateTimeFormatter.ofPattern("dd/MM")))
                        .receita(receita != null ? receita : BigDecimal.ZERO)
                        .build());
            }
        } else {
            // Gráfico mensal
            LocalDate mesAtual = dataInicio.withDayOfMonth(1);
            LocalDate ultimoMes = dataFim.withDayOfMonth(1);

            while (!mesAtual.isAfter(ultimoMes)) {
                LocalDate inicioMes = mesAtual;
                LocalDate fimMes = mesAtual.plusMonths(1).minusDays(1);

                BigDecimal receita = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                        inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59), organizacaoId);

                grafico.add(GraficoReceitaDTO.builder()
                        .periodo(mesAtual.format(DateTimeFormatter.ofPattern("MM/yyyy")))
                        .receita(receita != null ? receita : BigDecimal.ZERO)
                        .build());

                mesAtual = mesAtual.plusMonths(1);
            }
        }

        return grafico;
    }

    private DashboardDTO.TendenciaDTO criarTendenciaReceita(LocalDate inicioAtual, LocalDate fimAtual,
                                                            LocalDate inicioAnterior, LocalDate fimAnterior,
                                                            Long organizacaoId) {
        BigDecimal receitaAtual = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioAtual.atStartOfDay(), fimAtual.atTime(23, 59, 59), organizacaoId);
        if (receitaAtual == null) receitaAtual = BigDecimal.ZERO;

        BigDecimal receitaAnterior = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59), organizacaoId);
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
                                                                  LocalDate inicioAnterior, LocalDate fimAnterior,
                                                                  Long organizacaoId) {
        Long totalAtual = agendamentoRepository.countByOrganizacaoAndPeriodo(
                organizacaoId, inicioAtual.atStartOfDay(), fimAtual.atTime(23, 59, 59));
        if (totalAtual == null) totalAtual = 0L;

        Long totalAnterior = agendamentoRepository.countByOrganizacaoAndPeriodo(
                organizacaoId, inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59));
        if (totalAnterior == null) totalAnterior = 0L;

        Double percentualMudanca = 0.0;
        String tendencia = "ESTAVEL";

        if (totalAnterior > 0) {
            percentualMudanca = ((totalAtual - totalAnterior) * 100.0) / totalAnterior;

            if (percentualMudanca > 10) {
                tendencia = "ALTA";
            } else if (percentualMudanca < -10) {
                tendencia = "BAIXA";
            }
        }

        return DashboardDTO.TendenciaDTO.builder()
                .metrica("Agendamentos")
                .valorAtual(BigDecimal.valueOf(totalAtual))
                .valorAnterior(BigDecimal.valueOf(totalAnterior))
                .percentualMudanca(percentualMudanca)
                .tendencia(tendencia)
                .periodo(formatarPeriodo(inicioAtual, fimAtual))
                .build();
    }

    private DashboardDTO.TendenciaDTO criarTendenciaNovosClientes(LocalDate inicioAtual, LocalDate fimAtual,
                                                                   LocalDate inicioAnterior, LocalDate fimAnterior,
                                                                   Long organizacaoId) {
        Long clientesAtual = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicioAtual.atStartOfDay(), fimAtual.atTime(23, 59, 59));

        Long clientesAnterior = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59));

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

    private Double calcularMediaAtendimentosPorDia(Long totalAtendimentos, LocalDate inicio, LocalDate fim) {
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        return dias > 0 ? totalAtendimentos.doubleValue() / dias : 0.0;
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }

        return organizacaoId;
    }
}
