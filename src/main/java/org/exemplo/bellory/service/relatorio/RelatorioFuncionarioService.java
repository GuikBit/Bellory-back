package org.exemplo.bellory.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFiltroDTO;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFuncionarioDTO;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.funcionario.DiaSemana;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.funcionario.HorarioTrabalho;
import org.exemplo.bellory.model.entity.funcionario.JornadaDia;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
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
public class RelatorioFuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final CobrancaRepository cobrancaRepository;

    public RelatorioFuncionarioDTO gerarRelatorio(RelatorioFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        List<Funcionario> funcionarios = funcionarioRepository.findAllByOrganizacao_Id(organizacaoId);
        Long totalAtivos = funcionarios.stream().filter(Funcionario::isAtivo).count();

        // Produtividade
        List<RelatorioFuncionarioDTO.ProdutividadeDTO> produtividade = montarProdutividade(
                organizacaoId, funcionarios, inicioDateTime, fimDateTime, dataInicio, dataFim);

        // Ocupacao
        List<RelatorioFuncionarioDTO.OcupacaoAgendaDTO> ocupacao = montarOcupacao(
                organizacaoId, funcionarios, inicioDateTime, fimDateTime, dataInicio, dataFim);

        // Comissoes
        List<RelatorioFuncionarioDTO.ComissaoDTO> comissoes = montarComissoes(
                organizacaoId, funcionarios, inicioDateTime, fimDateTime);

        BigDecimal totalComissoes = comissoes.stream()
                .map(RelatorioFuncionarioDTO.ComissaoDTO::getComissaoCalculada)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RelatorioFuncionarioDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .totalFuncionariosAtivos(totalAtivos)
                .rankingProdutividade(produtividade)
                .ocupacaoAgenda(ocupacao)
                .comissoes(comissoes)
                .totalComissoes(totalComissoes)
                .build();
    }

    private List<RelatorioFuncionarioDTO.ProdutividadeDTO> montarProdutividade(
            Long organizacaoId, List<Funcionario> funcionarios,
            LocalDateTime inicio, LocalDateTime fim,
            LocalDate dataInicio, LocalDate dataFim) {

        // Buscar contagem por funcionario e status
        List<Object[]> dados = agendamentoRepository.countByFuncionarioAndStatusAndOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);

        // Receita por funcionario
        Map<String, BigDecimal> receitaPorFunc = new LinkedHashMap<>();
        List<Object[]> receitaData = cobrancaRepository.sumReceitaByFuncionarioAndOrganizacao(
                organizacaoId, inicio, fim);
        for (Object[] row : receitaData) {
            receitaPorFunc.put((String) row[0], row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        // Agregar dados por funcionario
        Map<Long, String> nomes = new LinkedHashMap<>();
        Map<Long, Long> totais = new LinkedHashMap<>();
        Map<Long, Long> concluidosMap = new LinkedHashMap<>();
        Map<Long, Long> canceladosMap = new LinkedHashMap<>();
        Map<Long, Long> noShowMap = new LinkedHashMap<>();

        for (Object[] row : dados) {
            Long funcId = (Long) row[0];
            String nome = (String) row[1];
            Status status = (Status) row[2];
            Long count = (Long) row[3];

            nomes.putIfAbsent(funcId, nome);
            totais.merge(funcId, count, Long::sum);

            if (status == Status.CONCLUIDO) concluidosMap.merge(funcId, count, Long::sum);
            else if (status == Status.CANCELADO) canceladosMap.merge(funcId, count, Long::sum);
            else if (status == Status.NAO_COMPARECEU) noShowMap.merge(funcId, count, Long::sum);
        }

        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;

        List<RelatorioFuncionarioDTO.ProdutividadeDTO> result = new ArrayList<>();
        for (Long funcId : nomes.keySet()) {
            Long total = totais.getOrDefault(funcId, 0L);
            Long conc = concluidosMap.getOrDefault(funcId, 0L);
            Long canc = canceladosMap.getOrDefault(funcId, 0L);
            Long noShow = noShowMap.getOrDefault(funcId, 0L);
            String nome = nomes.get(funcId);
            BigDecimal receita = receitaPorFunc.getOrDefault(nome, BigDecimal.ZERO);
            BigDecimal ticketMedio = conc > 0 ? receita.divide(BigDecimal.valueOf(conc), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            Double taxaConclusao = total > 0 ? (conc * 100.0) / total : 0.0;
            Double taxaCancelamento = total > 0 ? (canc * 100.0) / total : 0.0;

            // Buscar cargo
            String cargo = funcionarios.stream()
                    .filter(f -> f.getId().equals(funcId))
                    .findFirst()
                    .map(f -> f.getCargo() != null ? f.getCargo().getNome() : null)
                    .orElse(null);

            result.add(RelatorioFuncionarioDTO.ProdutividadeDTO.builder()
                    .funcionarioId(funcId)
                    .nome(nome)
                    .cargo(cargo)
                    .totalAtendimentos(total)
                    .atendimentosConcluidos(conc)
                    .atendimentosCancelados(canc)
                    .naoCompareceu(noShow)
                    .taxaConclusao(taxaConclusao)
                    .taxaCancelamento(taxaCancelamento)
                    .faturamentoGerado(receita)
                    .ticketMedio(ticketMedio)
                    .mediaAtendimentosDia(diasPeriodo > 0 ? total.doubleValue() / diasPeriodo : 0.0)
                    .build());
        }

        result.sort((a, b) -> Long.compare(b.getTotalAtendimentos(), a.getTotalAtendimentos()));
        return result;
    }

    private List<RelatorioFuncionarioDTO.OcupacaoAgendaDTO> montarOcupacao(
            Long organizacaoId, List<Funcionario> funcionarios,
            LocalDateTime inicio, LocalDateTime fim,
            LocalDate dataInicio, LocalDate dataFim) {

        List<RelatorioFuncionarioDTO.OcupacaoAgendaDTO> result = new ArrayList<>();
        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;

        for (Funcionario func : funcionarios) {
            if (!func.isAtivo()) continue;

            // Calcular horas disponíveis com base na jornada
            double horasDisponiveisTotal = 0;
            Map<String, Double> ocupacaoPorDia = new LinkedHashMap<>();

            if (func.getJornadasDia() != null) {
                for (JornadaDia jd : func.getJornadasDia()) {
                    if (jd.getAtivo() != null && jd.getAtivo() && jd.getHorarios() != null) {
                        double horasDia = 0;
                        for (HorarioTrabalho ht : jd.getHorarios()) {
                            long minutos = ChronoUnit.MINUTES.between(ht.getHoraInicio(), ht.getHoraFim());
                            horasDia += minutos / 60.0;
                        }
                        // Multiplicar por semanas no periodo
                        long semanas = diasPeriodo / 7;
                        if (semanas < 1) semanas = 1;
                        horasDisponiveisTotal += horasDia * semanas;
                        ocupacaoPorDia.put(jd.getDiaSemana().name(), horasDia);
                    }
                }
            }

            // Calcular horas ocupadas (agendamentos * tempo estimado dos servicos)
            Long agendamentos = agendamentoRepository.countByFuncionarioAndDataRange(
                    func.getId(), inicio, fim);
            if (agendamentos == null) agendamentos = 0L;

            // Estima 1 hora por agendamento se nao tiver tempo estimado dos servicos
            double horasOcupadas = agendamentos * 1.0;

            Double taxaOcupacao = horasDisponiveisTotal > 0
                    ? (horasOcupadas * 100.0) / horasDisponiveisTotal : 0.0;
            if (taxaOcupacao > 100) taxaOcupacao = 100.0;

            result.add(RelatorioFuncionarioDTO.OcupacaoAgendaDTO.builder()
                    .funcionarioId(func.getId())
                    .nome(func.getNomeCompleto())
                    .horasDisponiveis(horasDisponiveisTotal)
                    .horasOcupadas(horasOcupadas)
                    .taxaOcupacao(taxaOcupacao)
                    .ocupacaoPorDiaSemana(ocupacaoPorDia)
                    .build());
        }

        result.sort((a, b) -> Double.compare(b.getTaxaOcupacao(), a.getTaxaOcupacao()));
        return result;
    }

    private List<RelatorioFuncionarioDTO.ComissaoDTO> montarComissoes(
            Long organizacaoId, List<Funcionario> funcionarios,
            LocalDateTime inicio, LocalDateTime fim) {

        List<RelatorioFuncionarioDTO.ComissaoDTO> comissoes = new ArrayList<>();

        // Receita por funcionario
        Map<String, BigDecimal> receitaPorFunc = new LinkedHashMap<>();
        List<Object[]> receitaData = cobrancaRepository.sumReceitaByFuncionarioAndOrganizacao(
                organizacaoId, inicio, fim);
        for (Object[] row : receitaData) {
            receitaPorFunc.put((String) row[0], row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        // Atendimentos por funcionario
        Map<String, Long> atendimentosPorFunc = new LinkedHashMap<>();
        List<Object[]> atendData = agendamentoRepository.countByFuncionarioAndOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        for (Object[] row : atendData) {
            atendimentosPorFunc.put((String) row[1], (Long) row[2]);
        }

        for (Funcionario func : funcionarios) {
            if (!func.isAtivo() || !func.isComissao()) continue;

            BigDecimal receita = receitaPorFunc.getOrDefault(func.getNomeCompleto(), BigDecimal.ZERO);
            Long totalAtend = atendimentosPorFunc.getOrDefault(func.getNomeCompleto(), 0L);

            // Calcular comissao
            BigDecimal comissaoCalculada = BigDecimal.ZERO;
            String tipoComissao = "percentual";
            String valorComissao = func.getComissao();

            if (valorComissao != null && !valorComissao.isEmpty()) {
                try {
                    String valorLimpo = valorComissao.replace("%", "").replace(",", ".").trim();
                    BigDecimal percentual = new BigDecimal(valorLimpo);

                    if (valorComissao.contains("%")) {
                        comissaoCalculada = receita.multiply(percentual).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        tipoComissao = "percentual";
                    } else {
                        comissaoCalculada = percentual.multiply(BigDecimal.valueOf(totalAtend));
                        tipoComissao = "fixo";
                    }
                } catch (NumberFormatException e) {
                    // Ignora valores invalidos
                }
            }

            comissoes.add(RelatorioFuncionarioDTO.ComissaoDTO.builder()
                    .funcionarioId(func.getId())
                    .nome(func.getNomeCompleto())
                    .tipoComissao(tipoComissao)
                    .valorComissao(valorComissao)
                    .faturamentoGerado(receita)
                    .comissaoCalculada(comissaoCalculada)
                    .totalAtendimentos(totalAtend)
                    .build());
        }

        comissoes.sort((a, b) -> b.getComissaoCalculada().compareTo(a.getComissaoCalculada()));
        return comissoes;
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
