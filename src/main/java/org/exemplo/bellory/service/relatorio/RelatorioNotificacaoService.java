package org.exemplo.bellory.service.relatorio;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.relatorio.RelatorioFiltroDTO;
import org.exemplo.bellory.model.dto.relatorio.RelatorioNotificacaoDTO;
import org.exemplo.bellory.model.entity.notificacao.NotificacaoEnviada;
import org.exemplo.bellory.model.entity.notificacao.StatusEnvio;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.notificacao.NotificacaoEnviadaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioNotificacaoService {

    private final NotificacaoEnviadaRepository notificacaoRepository;
    private final AgendamentoRepository agendamentoRepository;

    public RelatorioNotificacaoDTO gerarRelatorio(RelatorioFiltroDTO filtro) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        validarFiltro(filtro);

        LocalDate dataInicio = filtro.getDataInicio() != null ? filtro.getDataInicio() : LocalDate.now().withDayOfMonth(1);
        LocalDate dataFim = filtro.getDataFim() != null ? filtro.getDataFim() : LocalDate.now();
        LocalDateTime inicioDateTime = dataInicio.atStartOfDay();
        LocalDateTime fimDateTime = dataFim.atTime(23, 59, 59);

        // Confirmacoes
        RelatorioNotificacaoDTO.ConfirmacoesResumoDTO confirmacoes = montarConfirmacoes(
                organizacaoId, inicioDateTime, fimDateTime);

        // Lembretes
        RelatorioNotificacaoDTO.LembretesResumoDTO lembretes = montarLembretes(
                organizacaoId, inicioDateTime, fimDateTime);

        // Falhas
        RelatorioNotificacaoDTO.FalhasResumoDTO falhas = montarFalhas(
                organizacaoId, inicioDateTime, fimDateTime);

        // Efetividade
        RelatorioNotificacaoDTO.EfetividadeDTO efetividade = montarEfetividade(
                organizacaoId, inicioDateTime, fimDateTime);

        // Evolucao
        List<RelatorioNotificacaoDTO.NotificacaoPeriodoDTO> evolucao = montarEvolucao(
                organizacaoId, inicioDateTime, fimDateTime);

        return RelatorioNotificacaoDTO.builder()
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .periodoConsulta(formatarPeriodo(dataInicio, dataFim))
                .confirmacoes(confirmacoes)
                .lembretes(lembretes)
                .falhas(falhas)
                .efetividade(efetividade)
                .evolucao(evolucao)
                .build();
    }

    private RelatorioNotificacaoDTO.ConfirmacoesResumoDTO montarConfirmacoes(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        Long totalEnviadas = notificacaoRepository.countByTipoAndOrganizacaoAndPeriodo(
                TipoNotificacao.CONFIRMACAO, organizacaoId, inicio, fim);
        if (totalEnviadas == null) totalEnviadas = 0L;

        Map<String, Long> porStatus = new LinkedHashMap<>();
        List<Object[]> statusData = notificacaoRepository.countByTipoAndStatusAndOrganizacaoAndPeriodo(
                TipoNotificacao.CONFIRMACAO, organizacaoId, inicio, fim);

        Long confirmadas = 0L;
        Long canceladasCliente = 0L;
        Long reagendadas = 0L;
        Long aguardando = 0L;
        Long expiradas = 0L;

        for (Object[] row : statusData) {
            StatusEnvio status = (StatusEnvio) row[0];
            Long count = (Long) row[1];
            porStatus.put(status.name(), count);

            switch (status) {
                case CONFIRMADO -> confirmadas = count;
                case CANCELADO_CLIENTE -> canceladasCliente = count;
                case REAGENDADO -> reagendadas = count;
                case AGUARDANDO_RESPOSTA, AGUARDANDO_DATA, AGUARDANDO_HORARIO -> aguardando += count;
                case EXPIRADO -> expiradas = count;
                default -> {}
            }
        }

        Long respondidas = confirmadas + canceladasCliente + reagendadas;
        Double taxaResposta = totalEnviadas > 0 ? (respondidas * 100.0) / totalEnviadas : 0.0;
        Double taxaConfirmacao = totalEnviadas > 0 ? (confirmadas * 100.0) / totalEnviadas : 0.0;
        Double taxaCancelamento = totalEnviadas > 0 ? (canceladasCliente * 100.0) / totalEnviadas : 0.0;
        Double taxaReagendamento = totalEnviadas > 0 ? (reagendadas * 100.0) / totalEnviadas : 0.0;

        return RelatorioNotificacaoDTO.ConfirmacoesResumoDTO.builder()
                .totalEnviadas(totalEnviadas)
                .confirmadas(confirmadas)
                .canceladasPeloCliente(canceladasCliente)
                .reagendadas(reagendadas)
                .aguardandoResposta(aguardando)
                .expiradas(expiradas)
                .taxaResposta(taxaResposta)
                .taxaConfirmacao(taxaConfirmacao)
                .taxaCancelamento(taxaCancelamento)
                .taxaReagendamento(taxaReagendamento)
                .porStatus(porStatus)
                .build();
    }

    private RelatorioNotificacaoDTO.LembretesResumoDTO montarLembretes(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        Long totalEnviados = notificacaoRepository.countByTipoAndOrganizacaoAndPeriodo(
                TipoNotificacao.LEMBRETE, organizacaoId, inicio, fim);
        if (totalEnviados == null) totalEnviados = 0L;

        Map<String, Long> porStatus = new LinkedHashMap<>();
        List<Object[]> statusData = notificacaoRepository.countByTipoAndStatusAndOrganizacaoAndPeriodo(
                TipoNotificacao.LEMBRETE, organizacaoId, inicio, fim);

        Long entregues = 0L;
        Long falhas = 0L;

        for (Object[] row : statusData) {
            StatusEnvio status = (StatusEnvio) row[0];
            Long count = (Long) row[1];
            porStatus.put(status.name(), count);

            if (status == StatusEnvio.ENVIADO || status == StatusEnvio.ENTREGUE) {
                entregues += count;
            } else if (status == StatusEnvio.FALHA) {
                falhas = count;
            }
        }

        Double taxaEntrega = totalEnviados > 0 ? (entregues * 100.0) / totalEnviados : 0.0;

        return RelatorioNotificacaoDTO.LembretesResumoDTO.builder()
                .totalEnviados(totalEnviados)
                .entregues(entregues)
                .falhas(falhas)
                .taxaEntrega(taxaEntrega)
                .porStatus(porStatus)
                .build();
    }

    private RelatorioNotificacaoDTO.FalhasResumoDTO montarFalhas(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        List<NotificacaoEnviada> falhasRecentes = notificacaoRepository.findFalhasByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);

        Long totalFalhas = (long) falhasRecentes.size();

        // Total geral para calcular taxa
        Long totalConfirmacoes = notificacaoRepository.countByTipoAndOrganizacaoAndPeriodo(
                TipoNotificacao.CONFIRMACAO, organizacaoId, inicio, fim);
        Long totalLembretes = notificacaoRepository.countByTipoAndOrganizacaoAndPeriodo(
                TipoNotificacao.LEMBRETE, organizacaoId, inicio, fim);
        if (totalConfirmacoes == null) totalConfirmacoes = 0L;
        if (totalLembretes == null) totalLembretes = 0L;
        Long totalGeral = totalConfirmacoes + totalLembretes;

        Double taxaFalha = totalGeral > 0 ? (totalFalhas * 100.0) / totalGeral : 0.0;

        // Erros mais comuns
        Map<String, Long> errosMaisComuns = new LinkedHashMap<>();
        List<Object[]> errosData = notificacaoRepository.countErrosByMensagemAndOrganizacao(
                organizacaoId, inicio, fim);
        for (Object[] row : errosData) {
            String erro = (String) row[0];
            Long count = (Long) row[1];
            errosMaisComuns.put(erro != null ? erro : "Erro desconhecido", count);
        }

        // Detalhes das falhas recentes (limite 20)
        List<RelatorioNotificacaoDTO.FalhaDetalheDTO> detalhes = falhasRecentes.stream()
                .limit(20)
                .map(ne -> RelatorioNotificacaoDTO.FalhaDetalheDTO.builder()
                        .id(ne.getId())
                        .tipo(ne.getTipo().name())
                        .telefoneDestino(ne.getTelefoneDestino())
                        .erroMensagem(ne.getErroMensagem())
                        .dtEnvio(ne.getDtEnvio() != null
                                ? ne.getDtEnvio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : null)
                        .build())
                .collect(Collectors.toList());

        return RelatorioNotificacaoDTO.FalhasResumoDTO.builder()
                .totalFalhas(totalFalhas)
                .taxaFalha(taxaFalha)
                .errosMaisComuns(errosMaisComuns)
                .falhasRecentes(detalhes)
                .build();
    }

    private RelatorioNotificacaoDTO.EfetividadeDTO montarEfetividade(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        // IDs de agendamentos que receberam notificação
        List<Long> agendamentoIdsComNotificacao = notificacaoRepository.findAgendamentoIdsComNotificacao(
                organizacaoId, inicio, fim);

        Long totalNoShow = agendamentoRepository.countNoShowByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        if (totalNoShow == null) totalNoShow = 0L;

        Long totalAgendamentos = agendamentoRepository.countByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        if (totalAgendamentos == null) totalAgendamentos = 0L;

        Long agendamentosComNotificacao = (long) agendamentoIdsComNotificacao.size();
        Long agendamentosSemNotificacao = totalAgendamentos - agendamentosComNotificacao;

        Long noShowComNotificacao = 0L;
        if (!agendamentoIdsComNotificacao.isEmpty()) {
            noShowComNotificacao = agendamentoRepository.countNoShowByAgendamentoIds(
                    organizacaoId, inicio, fim, agendamentoIdsComNotificacao);
            if (noShowComNotificacao == null) noShowComNotificacao = 0L;
        }

        Long noShowSemNotificacao = totalNoShow - noShowComNotificacao;

        Double taxaNoShowCom = agendamentosComNotificacao > 0
                ? (noShowComNotificacao * 100.0) / agendamentosComNotificacao : 0.0;
        Double taxaNoShowSem = agendamentosSemNotificacao > 0
                ? (noShowSemNotificacao * 100.0) / agendamentosSemNotificacao : 0.0;
        Double reducao = taxaNoShowSem - taxaNoShowCom;

        return RelatorioNotificacaoDTO.EfetividadeDTO.builder()
                .taxaNoShowComConfirmacao(taxaNoShowCom)
                .taxaNoShowSemConfirmacao(taxaNoShowSem)
                .reducaoNoShow(reducao)
                .agendamentosComNotificacao(agendamentosComNotificacao)
                .agendamentosSemNotificacao(agendamentosSemNotificacao)
                .noShowComNotificacao(noShowComNotificacao)
                .noShowSemNotificacao(noShowSemNotificacao)
                .build();
    }

    private List<RelatorioNotificacaoDTO.NotificacaoPeriodoDTO> montarEvolucao(
            Long organizacaoId, LocalDateTime inicio, LocalDateTime fim) {

        List<Object[]> dados = notificacaoRepository.countByDataAndTipoAndOrganizacao(
                organizacaoId, inicio, fim);

        Map<String, Long> confirmacoesPorData = new LinkedHashMap<>();
        Map<String, Long> lembretesPorData = new LinkedHashMap<>();

        for (Object[] row : dados) {
            LocalDate data = (LocalDate) row[0];
            TipoNotificacao tipo = (TipoNotificacao) row[1];
            Long count = (Long) row[2];
            String dataFormatada = data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            if (tipo == TipoNotificacao.CONFIRMACAO) {
                confirmacoesPorData.merge(dataFormatada, count, Long::sum);
            } else {
                lembretesPorData.merge(dataFormatada, count, Long::sum);
            }
        }

        Set<String> todasDatas = new TreeSet<>();
        todasDatas.addAll(confirmacoesPorData.keySet());
        todasDatas.addAll(lembretesPorData.keySet());

        // Contar falhas por data usando dados já coletados
        List<NotificacaoEnviada> falhas = notificacaoRepository.findFalhasByOrganizacaoAndPeriodo(
                organizacaoId, inicio, fim);
        Map<String, Long> falhasPorData = falhas.stream()
                .collect(Collectors.groupingBy(
                        ne -> ne.getDtEnvio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        Collectors.counting()));

        return todasDatas.stream()
                .map(data -> RelatorioNotificacaoDTO.NotificacaoPeriodoDTO.builder()
                        .periodo(data)
                        .confirmacoesEnviadas(confirmacoesPorData.getOrDefault(data, 0L))
                        .lembretesEnviados(lembretesPorData.getOrDefault(data, 0L))
                        .falhas(falhasPorData.getOrDefault(data, 0L))
                        .build())
                .collect(Collectors.toList());
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
