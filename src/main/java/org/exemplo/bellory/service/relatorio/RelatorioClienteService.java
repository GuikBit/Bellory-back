package org.exemplo.bellory.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.relatorio.RelatorioClienteDTO;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFiltroDTO;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
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
public class RelatorioClienteService {

    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final CobrancaRepository cobrancaRepository;

    public RelatorioClienteDTO gerarRelatorio(RelatorioFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Cadastros
        RelatorioClienteDTO.CadastrosResumoDTO cadastros = montarCadastros(
                organizacaoId, inicioDateTime, fimDateTime, dataInicio, dataFim);

        // Frequencia
        RelatorioClienteDTO.FrequenciaResumoDTO frequencia = montarFrequencia(
                organizacaoId, inicioDateTime, fimDateTime);

        // Valor (LTV)
        List<RelatorioClienteDTO.ClienteValorDTO> rankingValor = montarRankingValor(organizacaoId);
        BigDecimal ltvMedio = calcularLtvMedio(rankingValor);
        BigDecimal ticketMedio = clienteRepository.findTicketMedioByOrganizacao(organizacaoId);
        if (ticketMedio == null) ticketMedio = BigDecimal.ZERO;

        // Evolucao de cadastros
        List<RelatorioClienteDTO.CadastroPeriodoDTO> evolucaoCadastros = montarEvolucaoCadastros(
                organizacaoId, inicioDateTime, fimDateTime);

        return RelatorioClienteDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .cadastros(cadastros)
                .frequencia(frequencia)
                .rankingValor(rankingValor)
                .ltvMedio(ltvMedio)
                .ticketMedio(ticketMedio)
                .evolucaoCadastros(evolucaoCadastros)
                .build();
    }

    private RelatorioClienteDTO.CadastrosResumoDTO montarCadastros(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim,
            LocalDate dataInicio, LocalDate dataFim) {

        Long totalClientes = clienteRepository.countByOrganizacao_Id(organizacaoId);
        Long clientesAtivos = clienteRepository.countByOrganizacao_IdAndAtivoTrue(organizacaoId);
        Long clientesInativos = totalClientes - clientesAtivos;
        Long novosCadastros = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicio, fim);

        Long cadastrosIncompletos = clienteRepository.countCadastrosIncompletos(organizacaoId);
        if (cadastrosIncompletos == null) cadastrosIncompletos = 0L;
        Long cadastrosCompletos = totalClientes - cadastrosIncompletos;

        Double taxaCadastroCompleto = totalClientes > 0
                ? (cadastrosCompletos * 100.0) / totalClientes : 0.0;

        // Crescimento comparado com periodo anterior
        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate inicioAnterior = dataInicio.minusDays(diasPeriodo + 1);
        LocalDate fimAnterior = dataInicio.minusDays(1);
        Long novosAnterior = clienteRepository.countByOrganizacao_IdAndDtCriacaoBetween(
                organizacaoId, inicioAnterior.atStartOfDay(), fimAnterior.atTime(23, 59, 59));
        if (novosAnterior == null) novosAnterior = 0L;

        Double crescimento = novosAnterior > 0
                ? ((novosCadastros - novosAnterior) * 100.0) / novosAnterior : 0.0;

        return RelatorioClienteDTO.CadastrosResumoDTO.builder()
                .totalClientes(totalClientes)
                .clientesAtivos(clientesAtivos)
                .clientesInativos(clientesInativos)
                .novosCadastros(novosCadastros)
                .cadastrosCompletos(cadastrosCompletos)
                .cadastrosIncompletos(cadastrosIncompletos)
                .taxaCadastroCompleto(taxaCadastroCompleto)
                .crescimentoPercentual(crescimento)
                .build();
    }

    private RelatorioClienteDTO.FrequenciaResumoDTO montarFrequencia(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        List<Object[]> dados = agendamentoRepository.findClientesComFrequenciaByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);

        Long clientesFrequentes = 0L;
        Long clientesOcasionais = 0L;
        List<RelatorioClienteDTO.ClienteFrequenciaDTO> detalhamento = new ArrayList<>();

        LocalDateTime agora = LocalDateTime.now();

        for (Object[] row : dados) {
            Long clienteId = (Long) row[0];
            String nome = (String) row[1];
            String telefone = (String) row[2];
            Long totalAgendamentos = (Long) row[3];
            LocalDateTime ultimoAgendamento = (LocalDateTime) row[4];

            long diasSemAgendar = ChronoUnit.DAYS.between(ultimoAgendamento, agora);
            String classificacao;
            if (totalAgendamentos >= 3) {
                classificacao = "FREQUENTE";
                clientesFrequentes++;
            } else {
                classificacao = "OCASIONAL";
                clientesOcasionais++;
            }

            detalhamento.add(RelatorioClienteDTO.ClienteFrequenciaDTO.builder()
                    .clienteId(clienteId)
                    .nome(nome)
                    .telefone(telefone)
                    .totalAgendamentos(totalAgendamentos)
                    .ultimoAgendamento(ultimoAgendamento)
                    .diasSemAgendar(diasSemAgendar)
                    .classificacao(classificacao)
                    .build());
        }

        // Clientes inativos (sem agendamentos ha 90 dias)
        LocalDateTime dataLimiteInatividade = agora.minusDays(90);
        Long clientesInativos = agendamentoRepository.countClientesInativos(organizacaoId, dataLimiteInatividade);
        if (clientesInativos == null) clientesInativos = 0L;

        // Taxa de retencao
        Long clientesDistintos = agendamentoRepository.countClientesDistintosComAgendamentos(
                organizacaoId, inicio, fim);
        Long clientesRecorrentes = agendamentoRepository.countClientesRecorrentesByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        if (clientesDistintos == null) clientesDistintos = 0L;
        if (clientesRecorrentes == null) clientesRecorrentes = 0L;

        Double taxaRetencao = clientesDistintos > 0
                ? (clientesRecorrentes * 100.0) / clientesDistintos : 0.0;

        return RelatorioClienteDTO.FrequenciaResumoDTO.builder()
                .clientesFrequentes(clientesFrequentes)
                .clientesOcasionais(clientesOcasionais)
                .clientesInativos(clientesInativos)
                .taxaRetencao(taxaRetencao)
                .clientesRecorrentes(clientesRecorrentes)
                .detalhamento(detalhamento.size() > 50 ? detalhamento.subList(0, 50) : detalhamento)
                .build();
    }

    private List<RelatorioClienteDTO.ClienteValorDTO> montarRankingValor(Long organizacaoId) {
        List<Object[]> dados = cobrancaRepository.findClientesLtvByOrganizacao(organizacaoId);
        List<RelatorioClienteDTO.ClienteValorDTO> ranking = new ArrayList<>();

        for (Object[] row : dados) {
            Long clienteId = (Long) row[0];
            String nome = (String) row[1];
            String telefone = (String) row[2];
            BigDecimal valorTotal = (BigDecimal) row[3];
            Long totalAgendamentos = (Long) row[4];

            BigDecimal ticketMedio = totalAgendamentos > 0
                    ? valorTotal.divide(BigDecimal.valueOf(totalAgendamentos), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            ranking.add(RelatorioClienteDTO.ClienteValorDTO.builder()
                    .clienteId(clienteId)
                    .nome(nome)
                    .telefone(telefone)
                    .valorTotal(valorTotal)
                    .totalAgendamentos(totalAgendamentos)
                    .ticketMedio(ticketMedio)
                    .build());

            if (ranking.size() >= 50) break;
        }

        return ranking;
    }

    private BigDecimal calcularLtvMedio(List<RelatorioClienteDTO.ClienteValorDTO> ranking) {
        if (ranking.isEmpty()) return BigDecimal.ZERO;
        BigDecimal soma = ranking.stream()
                .map(RelatorioClienteDTO.ClienteValorDTO::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(ranking.size()), 2, RoundingMode.HALF_UP);
    }

    private List<RelatorioClienteDTO.CadastroPeriodoDTO> montarEvolucaoCadastros(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        List<Object[]> dados = clienteRepository.countNovosCadastrosByDataAndOrganizacao(
                organizacaoId, inicio, fim);

        List<RelatorioClienteDTO.CadastroPeriodoDTO> evolucao = new ArrayList<>();
        long acumulado = 0;

        for (Object[] row : dados) {
            java.sql.Date data = (java.sql.Date) row[0];
            Long novos = ((Number) row[1]).longValue();
            acumulado += novos;

            evolucao.add(RelatorioClienteDTO.CadastroPeriodoDTO.builder()
                    .periodo(data.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .novosCadastros(novos)
                    .acumulado(acumulado)
                    .build());
        }

        return evolucao;
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
