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


    @Transactional
    public DashboardDTO getDashboardGeral(DashboardFiltroDTO filtro) {
        // Valida e obtém o ID da organização do contexto JWT
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

    public Object getDashboardComparativo(LocalDate inicioAtual, LocalDate fimAtual,
                                          LocalDate inicioAnterior, LocalDate fimAnterior) {
        // Valida e obtém o ID da organização do contexto JWT
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

        Map<String, Object> comparativo = new HashMap<>();
        comparativo.put("periodoAtual", dashboardAtual);
        comparativo.put("periodoAnterior", dashboardAnterior);
        comparativo.put("comparativos", criarComparativos(dashboardAtual, dashboardAnterior));

        return comparativo;
    }

    public Object getMetricasFuncionario(Long funcionarioId, LocalDate inicio, LocalDate fim) {
        // Valida e obtém o ID da organização do contexto JWT
        Long organizacaoId = getOrganizacaoIdFromContext();

        // Valida se o funcionário pertence à organização
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        if (!funcionario.getOrganizacao().getId().equals(organizacaoId)) {
            throw new SecurityException("Acesso negado: Este funcionário não pertence à sua organização");
        }

        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime fimDateTime = fim.atTime(23, 59, 59);

        // Buscar agendamentos do funcionário no período (já filtra por organização via relacionamento)
        List<Agendamento> agendamentos = agendamentoRepository.findByFuncionarioAndDataRange(
                funcionarioId, inicioDateTime, fimDateTime);

        // Calcular métricas
        Long totalAtendimentos = (long) agendamentos.size();
        Long atendimentosConcluidos = agendamentos.stream()
                .filter(a -> a.getStatus() == Status.CONCLUIDO)
                .count();

        BigDecimal receitaGerada = agendamentos.stream()
                .filter(a -> a.getCobrancas() != null && !a.getCobrancas().isEmpty())
                .flatMap(a -> a.getCobrancas().stream())
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double taxaConclusao = totalAtendimentos > 0 ?
                (atendimentosConcluidos * 100.0) / totalAtendimentos : 0.0;

        Map<String, Object> metricas = new HashMap<>();
        metricas.put("funcionarioId", funcionarioId);
        metricas.put("nomeFuncionario", funcionario.getNomeCompleto());
        metricas.put("periodo", formatarPeriodo(inicio, fim));
        metricas.put("totalAtendimentos", totalAtendimentos);
        metricas.put("atendimentosConcluidos", atendimentosConcluidos);
        metricas.put("receitaGerada", receitaGerada);
        metricas.put("taxaConclusao", taxaConclusao);
        metricas.put("mediaAtendimentosPorDia", calcularMediaAtendimentosPorDia(totalAtendimentos, inicio, fim));

        return metricas;
    }


    private DashboardDTO.AgendamentosResumoDTO getAgendamentosResumo(LocalDate dataInicio, LocalDate dataFim,
                                                                     DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Buscar agendamentos filtrando por organização
        List<Agendamento> agendamentos;
        if (filtro.getFuncionarioId() != null) {
            // Validar se o funcionário pertence à organização
            Funcionario funcionario = funcionarioRepository.findById(filtro.getFuncionarioId())
                    .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

            if (!funcionario.getOrganizacao().getId().equals(organizacaoId)) {
                throw new SecurityException("Acesso negado: Este funcionário não pertence à sua organização");
            }

            agendamentos = agendamentoRepository.findByFuncionarioAndDataRange(
                    filtro.getFuncionarioId(), inicioDateTime, fimDateTime);
        } else {
            // Buscar todos os agendamentos da organização no período
            agendamentos = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                    inicioDateTime, fimDateTime, organizacaoId);
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
        List<Agendamento> agendamentosHojeList = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                hojeBaixo, hojeAlto, organizacaoId);
        Long agendamentosHoje = (long) agendamentosHojeList.size();

        // Agendamentos esta semana
        LocalDate inicioSemana = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = inicioSemana.plusDays(6);
        List<Agendamento> agendamentosEstaSemanaList = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                inicioSemana.atStartOfDay(), fimSemana.atTime(23, 59, 59), organizacaoId);
        Long agendamentosEstaSemana = (long) agendamentosEstaSemanaList.size();

        // Agendamentos este mês
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        List<Agendamento> agendamentosEsteMesList = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59), organizacaoId);
        Long agendamentosEsteMes = (long) agendamentosEsteMesList.size();

        // Taxa de ocupação (simulada - você precisará implementar baseado na jornada dos funcionários)
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

    private DashboardDTO.FinanceiroResumoDTO getFinanceiroResumo(LocalDate dataInicio, LocalDate dataFim,
                                                                 DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Buscar cobranças da organização através do relacionamento Cliente -> Organização
        List<Cobranca> cobrancasPeriodo = cobrancaRepository.findByPeriod(inicioDateTime, fimDateTime).stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                .collect(Collectors.toList());

        // Receita total do período (apenas cobranças pagas)
        BigDecimal receitaTotal = cobrancasPeriodo.stream()
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Receita hoje
        LocalDateTime hojeBaixo = LocalDate.now().atStartOfDay();
        LocalDateTime hojeAlto = LocalDate.now().atTime(23, 59, 59);
        List<Cobranca> cobrancasHoje = cobrancaRepository.findByPeriod(hojeBaixo, hojeAlto).stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                .collect(Collectors.toList());
        BigDecimal receitaHoje = cobrancasHoje.stream()
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Receita este mês
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        List<Cobranca> cobrancasMes = cobrancaRepository.findByPeriod(
                        inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59)).stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                .collect(Collectors.toList());
        BigDecimal receitaEsteMes = cobrancasMes.stream()
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Receita este ano
        LocalDate inicioAno = LocalDate.now().withDayOfYear(1);
        LocalDate fimAno = inicioAno.plusYears(1).minusDays(1);
        List<Cobranca> cobrancasAno = cobrancaRepository.findByPeriod(
                        inicioAno.atStartOfDay(), fimAno.atTime(23, 59, 59)).stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                .collect(Collectors.toList());
        BigDecimal receitaEsteAno = cobrancasAno.stream()
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Receita prevista (agendamentos futuros confirmados)
        BigDecimal receitaPrevista = calcularReceitaPrevista(organizacaoId);

        // Contas a receber (cobranças pendentes)
        BigDecimal contasReceber = cobrancasPeriodo.stream()
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PENDENTE)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Contas vencidas
        List<Cobranca> vencidas = cobrancaRepository.findVencidas(
                        Cobranca.StatusCobranca.PENDENTE, Cobranca.StatusCobranca.VENCIDA).stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                .collect(Collectors.toList());
        BigDecimal contasVencidas = vencidas.stream()
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ticket médio
        Long totalAgendamentosPagos = cobrancasPeriodo.stream()
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .count();
        BigDecimal ticketMedio = totalAgendamentosPagos > 0 ?
                receitaTotal.divide(BigDecimal.valueOf(totalAgendamentosPagos), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Formas de pagamento mais usadas
        Map<String, Long> formasPagamento = calcularFormasPagamento(cobrancasPeriodo);

        return DashboardDTO.FinanceiroResumoDTO.builder()
                .receitaTotal(receitaTotal)
                .receitaHoje(receitaHoje)
                .receitaEsteMes(receitaEsteMes)
                .receitaEsteAno(receitaEsteAno)
                .receitaPrevista(receitaPrevista)
                .contasReceber(contasReceber)
                .contasVencidas(contasVencidas)
                .ticketMedio(ticketMedio)
                .totalTransacoes((long) cobrancasPeriodo.size())
                .formasPagamento(formasPagamento)
                .build();
    }

    private DashboardDTO.ClientesResumoDTO getClientesResumo(LocalDate dataInicio, LocalDate dataFim,
                                                             DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Total de clientes ativos da organização
        Long totalClientes = clienteRepository.countByOrganizacao_IdAndAtivoTrue(organizacaoId);

        // Novos clientes no período
        Long novosClientes = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicioDateTime, fimDateTime);

        // Novos clientes hoje
        LocalDateTime hojeBaixo = LocalDate.now().atStartOfDay();
        LocalDateTime hojeAlto = LocalDate.now().atTime(23, 59, 59);
        Long novosClientesHoje = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, hojeBaixo, hojeAlto);

        // Novos clientes este mês
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        Long novosClientesEsteMes = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59));

        // Clientes ativos (com agendamentos no período)
        List<Agendamento> agendamentos = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                inicioDateTime, fimDateTime, organizacaoId);

        Long clientesAtivos = agendamentos.stream()
                .map(a -> a.getCliente().getId())
                .distinct()
                .count();

        // Taxa de retenção (clientes com mais de 1 agendamento no período)
        Map<Long, Long> agendamentosPorCliente = agendamentos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCliente().getId(),
                        Collectors.counting()
                ));

        Long clientesRetornaram = agendamentosPorCliente.values().stream()
                .filter(count -> count > 1)
                .count();

        Double taxaRetencao = clientesAtivos > 0 ?
                (clientesRetornaram * 100.0) / clientesAtivos : 0.0;

        // Clientes inativos (sem agendamentos há 90 dias)
        LocalDateTime dataLimiteInatividade = LocalDateTime.now().minusDays(90);
        Long clientesInativos = calcularClientesInativos(organizacaoId, dataLimiteInatividade);

        // Aniversariantes do mês
        Long aniversariantesMes = clienteRepository.countAniversariantesDoMesByOrganizacao(
                organizacaoId,                     // ✅ organizacaoId primeiro
                LocalDate.now().getMonthValue()    // ✅ mes depois
        );
        return DashboardDTO.ClientesResumoDTO.builder()
                .totalClientes(totalClientes)
                .novosClientes(novosClientes)
                .novosClientesHoje(novosClientesHoje)
                .novosClientesEsteMes(novosClientesEsteMes)
                .clientesAtivos(clientesAtivos)
                .clientesInativos(clientesInativos)
                .taxaRetencao(taxaRetencao)
                .aniversariantesMes(aniversariantesMes)
                .build();
    }

    private DashboardDTO.EstoqueResumoDTO getEstoqueResumo(DashboardFiltroDTO filtro, Long organizacaoId) {
        // Buscar produtos da organização
        List<Produto> produtos = produtoRepository.findAllByOrganizacao_Id(organizacaoId);

        Long totalProdutos = (long) produtos.size();

        // Produtos com estoque baixo (menor que estoque mínimo)
        Long produtosEstoqueBaixo = produtos.stream()
                .filter(p -> p.getQuantidadeEstoque() != null && p.getEstoqueMinimo() != null &&
                        p.getQuantidadeEstoque() < p.getEstoqueMinimo())
                .count();

        // Produtos sem estoque
        Long produtosSemEstoque = produtos.stream()
                .filter(p -> p.getQuantidadeEstoque() == null || p.getQuantidadeEstoque() == 0)
                .count();

        // Valor total do estoque
        BigDecimal valorTotalEstoque = produtos.stream()
                .filter(p -> p.getQuantidadeEstoque() != null && p.getPreco() != null)
                .map(p -> p.getPreco().multiply(BigDecimal.valueOf(p.getQuantidadeEstoque())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Produtos mais vendidos (últimos 30 dias)
        List<ProdutoTopDTO> produtosMaisVendidos = getProdutosMaisVendidos(organizacaoId, 30, 5);

        return DashboardDTO.EstoqueResumoDTO.builder()
                .totalProdutos(totalProdutos)
                .produtosEstoqueBaixo(produtosEstoqueBaixo)
                .produtosSemEstoque(produtosSemEstoque)
                .valorTotalEstoque(valorTotalEstoque)
                .produtosMaisVendidos(produtosMaisVendidos)
                .build();
    }

    private DashboardDTO.FuncionariosResumoDTO getFuncionariosResumo(LocalDate dataInicio, LocalDate dataFim,
                                                                     DashboardFiltroDTO filtro, Long organizacaoId) {
        // Total de funcionários ativos da organização
        List<Funcionario> funcionarios = funcionarioRepository.findAllByOrganizacao_Id(organizacaoId);
        Long totalFuncionarios = funcionarios.stream()
                .filter(Funcionario::isAtivo)
                .count();

        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Buscar agendamentos do período da organização
        List<Agendamento> agendamentos = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                inicioDateTime, fimDateTime, organizacaoId);

        // Funcionário com mais atendimentos
        Map<Long, Long> atendimentosPorFuncionario = new HashMap<>();
        for (Agendamento agendamento : agendamentos) {
            if (agendamento.getFuncionarios() != null) {
                for (Funcionario func : agendamento.getFuncionarios()) {
                    atendimentosPorFuncionario.merge(func.getId(), 1L, Long::sum);
                }
            }
        }

        FuncionarioTopDTO funcionarioMaisAtendimentos = null;
        if (!atendimentosPorFuncionario.isEmpty()) {
            Long funcIdMaisAtendimentos = atendimentosPorFuncionario.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (funcIdMaisAtendimentos != null) {
                Funcionario func = funcionarios.stream()
                        .filter(f -> f.getId().equals(funcIdMaisAtendimentos))
                        .findFirst()
                        .orElse(null);

                if (func != null) {
                    funcionarioMaisAtendimentos = FuncionarioTopDTO.builder()
                            .id(func.getId())
                            .nome(func.getNomeCompleto())
                            .totalAtendimentos(atendimentosPorFuncionario.get(funcIdMaisAtendimentos))
                            .build();
                }
            }
        }

        // Funcionário com mais receita
        FuncionarioTopDTO funcionarioMaisReceita = calcularFuncionarioMaisReceita(agendamentos, funcionarios);

        // Taxa média de ocupação dos funcionários
        Double taxaOcupacaoMedia = calcularTaxaOcupacaoMedia(organizacaoId, dataInicio, dataFim);

        return DashboardDTO.FuncionariosResumoDTO.builder()
                .totalFuncionarios(totalFuncionarios)
                .funcionarioMaisAtendimentos(funcionarioMaisAtendimentos)
                .funcionarioMaisReceita(funcionarioMaisReceita)
                .taxaOcupacaoMedia(taxaOcupacaoMedia)
                .build();
    }

    private DashboardDTO.VendasResumoDTO getVendasResumo(LocalDate dataInicio, LocalDate dataFim,
                                                         DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Buscar agendamentos do período
        List<Agendamento> agendamentos = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                inicioDateTime, fimDateTime, organizacaoId);

        // Serviço mais vendido
        Map<Long, Long> servicosPorQuantidade = new HashMap<>();
        Map<Long, String> servicosNomes = new HashMap<>();

        for (Agendamento agendamento : agendamentos) {
            if (agendamento.getServicos() != null) {
                agendamento.getServicos().forEach(servico -> {
                    servicosPorQuantidade.merge(servico.getId(), 1L, Long::sum);
                    servicosNomes.putIfAbsent(servico.getId(), servico.getNome());
                });
            }
        }

        ServicoTopDTO servicoMaisVendido = null;
        if (!servicosPorQuantidade.isEmpty()) {
            Long servicoIdMaisVendido = servicosPorQuantidade.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (servicoIdMaisVendido != null) {
                servicoMaisVendido = ServicoTopDTO.builder()
                        .id(servicoIdMaisVendido)
                        .nome(servicosNomes.get(servicoIdMaisVendido))
                        .quantidadeVendida(servicosPorQuantidade.get(servicoIdMaisVendido))
                        .build();
            }
        }

        // CORRIGIDO: Total de vendas (agendamentos concluídos E totalmente pagos)
        Long totalVendas = agendamentos.stream()
                .filter(a -> a.getStatus() == Status.CONCLUIDO &&
                        a.getCobrancas() != null &&
                        !a.getCobrancas().isEmpty() &&
                        todasCobrancasPagas(a))
                .count();

        // CORRIGIDO: Valor total vendido (soma de TODAS as cobranças PAGAS)
        BigDecimal valorTotalVendido = agendamentos.stream()
                .filter(a -> a.getCobrancas() != null && !a.getCobrancas().isEmpty())
                .flatMap(a -> a.getCobrancas().stream())
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardDTO.VendasResumoDTO.builder()
                .totalVendas(totalVendas)
                .valorTotalVendido(valorTotalVendido)
                .servicosMaisVendidos(servicoMaisVendido)
                .build();
    }
    private boolean todasCobrancasPagas(Agendamento agendamento) {
        if (agendamento.getCobrancas() == null || agendamento.getCobrancas().isEmpty()) {
            return false;
        }

        return agendamento.getCobrancas().stream()
                .allMatch(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO);
    }


    private DashboardDTO.GraficosDTO getGraficos(LocalDate dataInicio, LocalDate dataFim,
                                                 DashboardFiltroDTO filtro, Long organizacaoId) {
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Receita por período (diária, semanal ou mensal dependendo do range)
        List<GraficoReceitaDTO> receitaPorPeriodo = calcularReceitaPorPeriodo(dataInicio, dataFim, organizacaoId);

        // Agendamentos por status
        List<Agendamento> agendamentos = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                inicioDateTime, fimDateTime, organizacaoId);

        Map<String, Long> agendamentosPorStatus = agendamentos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus().name(),
                        Collectors.counting()
                ));

        // Serviços mais procurados
        Map<String, Long> servicosMaisProcurados = calcularServicosMaisProcurados(agendamentos, 10);

        return DashboardDTO.GraficosDTO.builder()
                .receitaPorPeriodo(receitaPorPeriodo)
                .agendamentosPorStatus(agendamentosPorStatus)
                .servicosMaisProcurados(servicosMaisProcurados)
                .build();
    }

    private DashboardDTO.TendenciasDTO getTendencias(LocalDate dataInicio, LocalDate dataFim,
                                                     DashboardFiltroDTO filtro, Long organizacaoId) {
        // Calcular período anterior do mesmo tamanho para comparação
        long diasPeriodo = java.time.temporal.ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo + 1);
        LocalDate fimAnterior = dataInicio.minusDays(1);

        List<DashboardDTO.TendenciaDTO> tendencias = new ArrayList<>();

        // Tendência de receita
        tendencias.add(criarTendenciaReceita(dataInicio, dataFim, inicioAnterior, fimAnterior, organizacaoId));

        // Tendência de agendamentos
        tendencias.add(criarTendenciaAgendamentos(dataInicio, dataFim, inicioAnterior, fimAnterior, organizacaoId));

        // Tendência de novos clientes
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

        // CORRIGIDO: Calcular valor total de TODAS as cobranças
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
        // Implementação simplificada
        // Você deve calcular baseado na jornada de trabalho dos funcionários
        long agendadosOuConcluidos = agendamentos.stream()
                .filter(a -> a.getStatus() == Status.AGENDADO || a.getStatus() == Status.CONCLUIDO)
                .count();

        return agendamentos.size() > 0 ?
                (agendadosOuConcluidos * 100.0) / agendamentos.size() : 0.0;
    }

    private BigDecimal calcularReceitaPrevista(Long organizacaoId) {
        LocalDateTime agora = LocalDateTime.now();
        List<Agendamento> agendamentosFuturos = agendamentoRepository.findAgendamentosFuturos(agora).stream()
                .filter(a -> a.getCliente() != null &&
                        a.getCliente().getOrganizacao().getId().equals(organizacaoId))
                .collect(Collectors.toList());

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

    private Map<String, Long> calcularFormasPagamento(List<Cobranca> cobrancas) {
        // Esta implementação depende de como você armazena a forma de pagamento
        // Assumindo que você tem essa informação no Pagamento relacionado à cobrança
        Map<String, Long> formas = new HashMap<>();
        formas.put("Dinheiro", 0L);
        formas.put("Cartão", 0L);
        formas.put("PIX", 0L);
        formas.put("Outros", 0L);

        // Você precisará implementar a lógica correta baseado no seu modelo de Pagamento
        return formas;
    }

    private Long calcularClientesInativos(Long organizacaoId, LocalDateTime dataLimite) {
        List<Cliente> todosClientes = clienteRepository.findAllByOrganizacao_Id(organizacaoId);

        long clientesInativos = 0;
        for (Cliente cliente : todosClientes) {
            LocalDateTime ultimoAgendamento = agendamentoRepository.findLastAgendamentoByCliente(cliente.getId());
            if (ultimoAgendamento == null || ultimoAgendamento.isBefore(dataLimite)) {
                clientesInativos++;
            }
        }

        return clientesInativos;
    }

    private List<ProdutoTopDTO> getProdutosMaisVendidos(Long organizacaoId, int dias, int limite) {
        // Implementação simplificada
        // Você deve buscar produtos vendidos através das compras/vendas
        List<ProdutoTopDTO> produtos = new ArrayList<>();

        // TODO: Implementar consulta real aos produtos vendidos

        return produtos;
    }

    private FuncionarioTopDTO calcularFuncionarioMaisReceita(List<Agendamento> agendamentos,
                                                             List<Funcionario> funcionarios) {
        Map<Long, BigDecimal> receitaPorFuncionario = new HashMap<>();

        for (Agendamento agendamento : agendamentos) {
            if (agendamento.getFuncionarios() != null &&
                    agendamento.getCobrancas() != null &&
                    !agendamento.getCobrancas().isEmpty()) {

                // Somar TODAS as cobranças PAGAS deste agendamento
                BigDecimal valorTotalPago = agendamento.getCobrancas().stream()
                        .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                        .map(Cobranca::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Se tem valor pago, dividir entre os funcionários
                if (valorTotalPago.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal valorDividido = valorTotalPago
                            .divide(BigDecimal.valueOf(agendamento.getFuncionarios().size()),
                                    2, RoundingMode.HALF_UP);

                    for (Funcionario func : agendamento.getFuncionarios()) {
                        receitaPorFuncionario.merge(func.getId(), valorDividido, BigDecimal::add);
                    }
                }
            }
        }

        if (receitaPorFuncionario.isEmpty()) {
            return null;
        }

        Long funcIdMaisReceita = receitaPorFuncionario.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (funcIdMaisReceita != null) {
            Funcionario func = funcionarios.stream()
                    .filter(f -> f.getId().equals(funcIdMaisReceita))
                    .findFirst()
                    .orElse(null);

            if (func != null) {
                return FuncionarioTopDTO.builder()
                        .id(func.getId())
                        .nome(func.getNomeCompleto())
                        .receitaGerada(receitaPorFuncionario.get(funcIdMaisReceita))
                        .build();
            }
        }

        return null;
    }

    private Double calcularTaxaOcupacaoMedia(Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {
        // Implementação simplificada
        // Você deve calcular baseado na jornada de trabalho dos funcionários
        return 75.0; // Placeholder
    }

    private List<GraficoReceitaDTO> calcularReceitaPorPeriodo(LocalDate dataInicio, LocalDate dataFim,
                                                              Long organizacaoId) {
        List<GraficoReceitaDTO> grafico = new ArrayList<>();

        long diasPeriodo = java.time.temporal.ChronoUnit.DAYS.between(dataInicio, dataFim);

        if (diasPeriodo <= 31) {
            // Gráfico diário
            for (LocalDate data = dataInicio; !data.isAfter(dataFim); data = data.plusDays(1)) {
                LocalDateTime inicio = data.atStartOfDay();
                LocalDateTime fim = data.atTime(23, 59, 59);

                List<Cobranca> cobrancas = cobrancaRepository.findByPeriod(inicio, fim).stream()
                        .filter(c -> c.getCliente() != null &&
                                c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                        .collect(Collectors.toList());

                BigDecimal receita = cobrancas.stream()
                        .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                        .map(Cobranca::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                grafico.add(GraficoReceitaDTO.builder()
                        .periodo(data.format(DateTimeFormatter.ofPattern("dd/MM")))
                        .receita(receita)
                        .build());
            }
        } else {
            // Gráfico mensal
            LocalDate mesAtual = dataInicio.withDayOfMonth(1);
            LocalDate ultimoMes = dataFim.withDayOfMonth(1);

            while (!mesAtual.isAfter(ultimoMes)) {
                LocalDate inicioMes = mesAtual;
                LocalDate fimMes = mesAtual.plusMonths(1).minusDays(1);

                List<Cobranca> cobrancas = cobrancaRepository.findByPeriod(
                                inicioMes.atStartOfDay(),
                                fimMes.atTime(23, 59, 59)).stream()
                        .filter(c -> c.getCliente() != null &&
                                c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                        .collect(Collectors.toList());

                BigDecimal receita = cobrancas.stream()
                        .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                        .map(Cobranca::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                grafico.add(GraficoReceitaDTO.builder()
                        .periodo(mesAtual.format(DateTimeFormatter.ofPattern("MM/yyyy")))
                        .receita(receita)
                        .build());

                mesAtual = mesAtual.plusMonths(1);
            }
        }

        return grafico;
    }

    private Map<String, Long> calcularServicosMaisProcurados(List<Agendamento> agendamentos, int limite) {
        Map<String, Long> servicosCount = new HashMap<>();

        for (Agendamento agendamento : agendamentos) {
            if (agendamento.getServicos() != null) {
                agendamento.getServicos().forEach(servico -> {
                    servicosCount.merge(servico.getNome(), 1L, Long::sum);
                });
            }
        }

        return servicosCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limite)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private DashboardDTO.TendenciaDTO criarTendenciaReceita(LocalDate inicioAtual, LocalDate fimAtual,
                                                            LocalDate inicioAnterior, LocalDate fimAnterior,
                                                            Long organizacaoId) {
        List<Cobranca> cobrancasAtual = cobrancaRepository.findByPeriod(
                        inicioAtual.atStartOfDay(), fimAtual.atTime(23, 59, 59)).stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                .collect(Collectors.toList());

        List<Cobranca> cobrancasAnterior = cobrancaRepository.findByPeriod(
                        inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59)).stream()
                .filter(c -> c.getCliente() != null && c.getCliente().getOrganizacao().getId().equals(organizacaoId))
                .collect(Collectors.toList());

        BigDecimal receitaAtual = cobrancasAtual.stream()
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal receitaAnterior = cobrancasAnterior.stream()
                .filter(c -> c.getStatusCobranca() == Cobranca.StatusCobranca.PAGO)
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
        List<Agendamento> agendamentosAtual = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                inicioAtual.atStartOfDay(), fimAtual.atTime(23, 59, 59), organizacaoId);

        List<Agendamento> agendamentosAnterior = agendamentoRepository.findByDtAgendamentoBetweenAndClienteOrganizacaoId(
                inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59), organizacaoId);

        Long totalAtual = (long) agendamentosAtual.size();
        Long totalAnterior = (long) agendamentosAnterior.size();

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
        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;
        return dias > 0 ? totalAtendimentos.doubleValue() / dias : 0.0;
    }

    // --------------------
    // Métodos de Validação Multi-Tenant
    // --------------------

    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }

        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }

        return organizacaoId;
    }
}
