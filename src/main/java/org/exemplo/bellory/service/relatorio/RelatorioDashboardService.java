package org.exemplo.bellory.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.relatorio.RelatorioDashboardDTO;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFiltroDTO;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.instancia.InstanceStatus;
import org.exemplo.bellory.model.entity.notificacao.StatusEnvio;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.instance.InstanceRepository;
import org.exemplo.bellory.model.repository.notificacao.NotificacaoEnviadaRepository;
import org.exemplo.bellory.model.repository.produtos.ProdutoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RelatorioDashboardService {

    private final AgendamentoRepository agendamentoRepository;
    private final CobrancaRepository cobrancaRepository;
    private final ClienteRepository clienteRepository;
    private final NotificacaoEnviadaRepository notificacaoRepository;
    private final InstanceRepository instanceRepository;
    private final ProdutoRepository produtoRepository;

    public RelatorioDashboardDTO gerarRelatorio(RelatorioFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // KPIs
        RelatorioDashboardDTO.KpisDTO kpis = montarKpis(organizacaoId, inicioDateTime, fimDateTime);

        // Comparativo
        RelatorioDashboardDTO.ComparativoDTO comparativo = montarComparativo(
                organizacaoId, dataInicio, dataFim);

        // Saude do negocio
        RelatorioDashboardDTO.SaudeNegocioDTO saude = montarSaudeNegocio(
                organizacaoId, inicioDateTime, fimDateTime);

        // Rankings
        RelatorioDashboardDTO.RankingsDTO rankings = montarRankings(organizacaoId, inicioDateTime, fimDateTime);

        return RelatorioDashboardDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .kpis(kpis)
                .comparativo(comparativo)
                .saudeNegocio(saude)
                .rankings(rankings)
                .build();
    }

    private RelatorioDashboardDTO.KpisDTO montarKpis(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        // Faturamento
        BigDecimal faturamento = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(
                inicio, fim, organizacaoId);
        if (faturamento == null) faturamento = BigDecimal.ZERO;

        // Agendamentos
        Long totalAgendamentos = agendamentoRepository.countByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        if (totalAgendamentos == null) totalAgendamentos = 0L;

        // Novos clientes
        Long novosClientes = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicio, fim);

        // Status de agendamentos
        Map<String, Long> porStatus = new LinkedHashMap<>();
        List<Object[]> statusCounts = agendamentoRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        for (Object[] row : statusCounts) {
            Status status = (Status) row[0];
            Long count = (Long) row[1];
            porStatus.put(status.name(), count);
        }

        Long concluidos = porStatus.getOrDefault("CONCLUIDO", 0L);
        Long naoCompareceu = porStatus.getOrDefault("NAO_COMPARECEU", 0L);
        Double taxaConclusao = totalAgendamentos > 0 ? (concluidos * 100.0) / totalAgendamentos : 0.0;
        Double taxaNoShow = totalAgendamentos > 0 ? (naoCompareceu * 100.0) / totalAgendamentos : 0.0;

        // Taxa de ocupacao
        Long agendadosOuConcluidos = concluidos + porStatus.getOrDefault("AGENDADO", 0L)
                + porStatus.getOrDefault("EM_ANDAMENTO", 0L);
        Double taxaOcupacao = totalAgendamentos > 0
                ? (agendadosOuConcluidos * 100.0) / totalAgendamentos : 0.0;

        // Ticket medio
        BigDecimal ticketMedio = concluidos > 0
                ? faturamento.divide(BigDecimal.valueOf(concluidos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Notificacoes
        Long totalConfirmacoes = notificacaoRepository.countByTipoAndOrganizacaoAndPeriodo(
                TipoNotificacao.CONFIRMACAO, organizacaoId, inicio, fim);
        Long totalLembretes = notificacaoRepository.countByTipoAndOrganizacaoAndPeriodo(
                TipoNotificacao.LEMBRETE, organizacaoId, inicio, fim);
        if (totalConfirmacoes == null) totalConfirmacoes = 0L;
        if (totalLembretes == null) totalLembretes = 0L;
        Long totalNotificacoes = totalConfirmacoes + totalLembretes;

        // Taxa de confirmacao
        Double taxaConfirmacao = 0.0;
        if (totalConfirmacoes > 0) {
            List<Object[]> statusNotif = notificacaoRepository.countByTipoAndStatusAndOrganizacaoAndPeriodo(
                    TipoNotificacao.CONFIRMACAO, organizacaoId, inicio, fim);
            Long confirmadas = 0L;
            for (Object[] row : statusNotif) {
                if (row[0] == StatusEnvio.CONFIRMADO) confirmadas = (Long) row[1];
            }
            taxaConfirmacao = (confirmadas * 100.0) / totalConfirmacoes;
        }

        // Valor pendente
        BigDecimal valorPendente = cobrancaRepository.sumValorPendenteByOrganizacao(organizacaoId);
        if (valorPendente == null) valorPendente = BigDecimal.ZERO;

        return RelatorioDashboardDTO.KpisDTO.builder()
                .faturamentoTotal(faturamento)
                .totalAgendamentos(totalAgendamentos)
                .novosClientes(novosClientes)
                .taxaOcupacao(taxaOcupacao)
                .ticketMedio(ticketMedio)
                .taxaConclusaoAgendamentos(taxaConclusao)
                .taxaNoShow(taxaNoShow)
                .totalNotificacoesEnviadas(totalNotificacoes)
                .taxaConfirmacaoNotificacoes(taxaConfirmacao)
                .valorPendente(valorPendente)
                .build();
    }

    private RelatorioDashboardDTO.ComparativoDTO montarComparativo(
            Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {

        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo + 1);
        LocalDate fimAnterior = dataInicio.minusDays(1);
        LocalDateTime inicioAntDT = inicioAnterior.atStartOfDay();
        LocalDateTime fimAntDT = fimAnterior.atTime(23, 59, 59);
        LocalDateTime inicioDT = dataInicio.atStartOfDay();
        LocalDateTime fimDT = dataFim.atTime(23, 59, 59);

        // Faturamento
        BigDecimal fatAtual = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(inicioDT, fimDT, organizacaoId);
        BigDecimal fatAnterior = cobrancaRepository.sumReceitaPagaByPeriodAndOrganizacao(inicioAntDT, fimAntDT, organizacaoId);
        if (fatAtual == null) fatAtual = BigDecimal.ZERO;
        if (fatAnterior == null) fatAnterior = BigDecimal.ZERO;

        // Agendamentos
        Long agendAtual = agendamentoRepository.countByOrganizacaoAndPeriodo(organizacaoId, inicioDT, fimDT);
        Long agendAnterior = agendamentoRepository.countByOrganizacaoAndPeriodo(organizacaoId, inicioAntDT, fimAntDT);
        if (agendAtual == null) agendAtual = 0L;
        if (agendAnterior == null) agendAnterior = 0L;

        // Novos clientes
        Long clientesAtual = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(organizacaoId, inicioDT, fimDT);
        Long clientesAnterior = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(organizacaoId, inicioAntDT, fimAntDT);
        if (clientesAtual == null) clientesAtual = 0L;
        if (clientesAnterior == null) clientesAnterior = 0L;

        // Ticket medio
        BigDecimal ticketAnterior = BigDecimal.ZERO;
        if (agendAnterior > 0 && fatAnterior.compareTo(BigDecimal.ZERO) > 0) {
            ticketAnterior = fatAnterior.divide(BigDecimal.valueOf(agendAnterior), 2, RoundingMode.HALF_UP);
        }
        BigDecimal ticketAtual = BigDecimal.ZERO;
        if (agendAtual > 0 && fatAtual.compareTo(BigDecimal.ZERO) > 0) {
            ticketAtual = fatAtual.divide(BigDecimal.valueOf(agendAtual), 2, RoundingMode.HALF_UP);
        }

        return RelatorioDashboardDTO.ComparativoDTO.builder()
                .periodoAnterior(formatarPeriodo(inicioAnterior, fimAnterior))
                .faturamentoAnterior(fatAnterior)
                .variacaoFaturamento(calcularVariacao(fatAtual, fatAnterior))
                .agendamentosAnterior(agendAnterior)
                .variacaoAgendamentos(agendAnterior > 0
                        ? ((agendAtual - agendAnterior) * 100.0) / agendAnterior : 0.0)
                .novosClientesAnterior(clientesAnterior)
                .variacaoNovosClientes(clientesAnterior > 0
                        ? ((clientesAtual - clientesAnterior) * 100.0) / clientesAnterior : 0.0)
                .ticketMedioAnterior(ticketAnterior)
                .variacaoTicketMedio(calcularVariacao(ticketAtual, ticketAnterior))
                .build();
    }

    private RelatorioDashboardDTO.SaudeNegocioDTO montarSaudeNegocio(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        // Taxa de retencao
        Long clientesDistintos = agendamentoRepository.countClientesDistintosComAgendamentos(
                organizacaoId, inicio, fim);
        Long clientesRecorrentes = agendamentoRepository.countClientesRecorrentesByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        if (clientesDistintos == null) clientesDistintos = 0L;
        if (clientesRecorrentes == null) clientesRecorrentes = 0L;
        Double taxaRetencao = clientesDistintos > 0
                ? (clientesRecorrentes * 100.0) / clientesDistintos : 0.0;

        // Inadimplencia
        BigDecimal valorVencido = cobrancaRepository.sumValorVencidoByOrganizacao(organizacaoId);
        BigDecimal valorTotal = cobrancaRepository.sumValorPendenteByOrganizacao(organizacaoId);
        if (valorVencido == null) valorVencido = BigDecimal.ZERO;
        if (valorTotal == null) valorTotal = BigDecimal.ZERO;
        BigDecimal totalGeral = valorVencido.add(valorTotal);
        Double taxaInadimplencia = totalGeral.compareTo(BigDecimal.ZERO) > 0
                ? valorVencido.multiply(BigDecimal.valueOf(100)).divide(totalGeral, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // Cancelamento
        Long totalAgendamentos = agendamentoRepository.countByOrganizacaoAndPeriodo(organizacaoId, inicio, fim);
        Map<String, Long> porStatus = new LinkedHashMap<>();
        List<Object[]> statusCounts = agendamentoRepository.countByStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        for (Object[] row : statusCounts) {
            Status status = (Status) row[0];
            Long count = (Long) row[1];
            porStatus.put(status.name(), count);
        }
        Long cancelados = porStatus.getOrDefault("CANCELADO", 0L);
        Double taxaCancelamento = totalAgendamentos != null && totalAgendamentos > 0
                ? (cancelados * 100.0) / totalAgendamentos : 0.0;

        // Falha de notificacoes
        Long totalNotif = 0L;
        Long falhas = 0L;
        try {
            Long confTotal = notificacaoRepository.countByTipoAndOrganizacaoAndPeriodo(
                    TipoNotificacao.CONFIRMACAO, organizacaoId, inicio, fim);
            Long lembTotal = notificacaoRepository.countByTipoAndOrganizacaoAndPeriodo(
                    TipoNotificacao.LEMBRETE, organizacaoId, inicio, fim);
            if (confTotal != null) totalNotif += confTotal;
            if (lembTotal != null) totalNotif += lembTotal;

            falhas = (long) notificacaoRepository.findFalhasByOrganizacaoAndPeriodo(
                    organizacaoId, inicio, fim).size();
        } catch (Exception e) {
            // Ignora se nao houver dados
        }
        Double taxaFalhaNotificacoes = totalNotif > 0 ? (falhas * 100.0) / totalNotif : 0.0;

        // Instancias
        Long instanciasAtivas = 0L;
        Long instanciasDesconectadas = 0L;
        try {
            List<Object[]> instData = instanceRepository.countByStatusAndOrganizacao(organizacaoId);
            for (Object[] row : instData) {
                InstanceStatus instStatus = (InstanceStatus) row[0];
                Long count = (Long) row[1];
                if (instStatus == InstanceStatus.CONNECTED || instStatus == InstanceStatus.OPEN) {
                    instanciasAtivas += count;
                } else {
                    instanciasDesconectadas += count;
                }
            }
        } catch (Exception e) {
            // Ignora
        }

        // Estoque baixo
        Long produtosEstoqueBaixo = 0L;
        try {
            produtosEstoqueBaixo = produtoRepository.countProdutosEstoqueBaixoByOrganizacao(organizacaoId);
        } catch (Exception e) {
            // Ignora
        }

        // Receita prevista
        BigDecimal receitaPrevista = BigDecimal.ZERO;
        try {
            var agendamentosFuturos = agendamentoRepository.findAgendamentosFuturosByOrganizacao(
                    organizacaoId, LocalDateTime.now());
            receitaPrevista = agendamentosFuturos.stream()
                    .filter(a -> a.getServicos() != null)
                    .flatMap(a -> a.getServicos().stream())
                    .map(s -> s.getPreco() != null ? s.getPreco() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            // Ignora
        }

        return RelatorioDashboardDTO.SaudeNegocioDTO.builder()
                .taxaRetencaoClientes(taxaRetencao)
                .taxaInadimplencia(taxaInadimplencia)
                .taxaCancelamento(taxaCancelamento)
                .taxaFalhaNotificacoes(taxaFalhaNotificacoes)
                .instanciasAtivas(instanciasAtivas)
                .instanciasDesconectadas(instanciasDesconectadas)
                .produtosEstoqueBaixo(produtosEstoqueBaixo)
                .receitaPrevista(receitaPrevista)
                .build();
    }

    private RelatorioDashboardDTO.RankingsDTO montarRankings(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        // Top servicos
        List<Map<String, Object>> topServicos = new ArrayList<>();
        List<Object[]> servicosData = agendamentoRepository.countServicosMaisVendidosByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        int count = 0;
        for (Object[] row : servicosData) {
            if (count >= 5) break;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("nome", row[1]);
            item.put("quantidade", row[2]);
            topServicos.add(item);
            count++;
        }

        // Top funcionarios
        List<Map<String, Object>> topFuncionarios = new ArrayList<>();
        List<Object[]> funcData = agendamentoRepository.countByFuncionarioAndOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        count = 0;
        for (Object[] row : funcData) {
            if (count >= 5) break;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("nome", row[1]);
            item.put("totalAtendimentos", row[2]);
            topFuncionarios.add(item);
            count++;
        }

        // Top clientes
        List<Map<String, Object>> topClientes = new ArrayList<>();
        List<Object[]> clienteData = clienteRepository.findTopClientesByOrganizacao(organizacaoId);
        count = 0;
        for (Object[] row : clienteData) {
            if (count >= 5) break;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("nome", row[1]);
            item.put("valorGasto", row[2]);
            item.put("totalAgendamentos", row[3]);
            topClientes.add(item);
            count++;
        }

        return RelatorioDashboardDTO.RankingsDTO.builder()
                .topServicos(topServicos)
                .topFuncionarios(topFuncionarios)
                .topClientes(topClientes)
                .build();
    }

    private Double calcularVariacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return atual != null && atual.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return atual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void validarFiltro(RelatorioFiltroDTO filtro) {
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

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }
        return organizacaoId;
    }
}
