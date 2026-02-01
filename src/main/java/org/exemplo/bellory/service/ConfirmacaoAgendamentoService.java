package org.exemplo.bellory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.dto.ConfirmacaoPendenteResponse;
import org.exemplo.bellory.model.dto.HorarioDisponivelResponse;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.notificacao.NotificacaoEnviada;
import org.exemplo.bellory.model.entity.notificacao.StatusEnvio;
import org.exemplo.bellory.model.repository.notificacao.NotificacaoEnviadaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConfirmacaoAgendamentoService {

    private final NotificacaoEnviadaRepository notificacaoRepository;
    private final ObjectMapper objectMapper;

    public ConfirmacaoAgendamentoService(NotificacaoEnviadaRepository notificacaoRepository,
                                         ObjectMapper objectMapper) {
        this.notificacaoRepository = notificacaoRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Verifica se existe uma confirmação pendente (aguardando SIM/NAO/REAGENDAR)
     */
    public ConfirmacaoPendenteResponse verificarConfirmacaoPendente(String telefone, String instanceName) {
        String telefoneLimpo = limparTelefone(telefone);

        Optional<NotificacaoEnviada> notificacaoOpt = notificacaoRepository
                .findConfirmacaoPendenteByTelefone(telefoneLimpo, instanceName);

        if (notificacaoOpt.isEmpty()) {
            return ConfirmacaoPendenteResponse.builder()
                    .temConfirmacaoPendente(false)
                    .build();
        }

        NotificacaoEnviada notificacao = notificacaoOpt.get();
        Agendamento agendamento = notificacao.getAgendamento();

        return ConfirmacaoPendenteResponse.builder()
                .temConfirmacaoPendente(true)
                .agendamentoId(agendamento.getId())
                .notificacaoId(notificacao.getId())
                .telefone(telefoneLimpo)
                .instanceName(instanceName)
                .clienteNome(agendamento.getCliente().getNomeCompleto())
                .funcionarioId(agendamento.getFuncionarios().isEmpty() ? null :
                        agendamento.getFuncionarios().get(0).getId())
                .servicoIds(agendamento.getServicos().stream()
                        .map(s -> s.getId())
                        .collect(Collectors.toList()))
                .organizacaoId(agendamento.getOrganizacao().getId())
                .build();
    }

    /**
     * Verifica se o cliente está aguardando informar data de reagendamento
     */
    public ConfirmacaoPendenteResponse verificarAguardandoData(String telefone, String instanceName) {
        String telefoneLimpo = limparTelefone(telefone);

        Optional<NotificacaoEnviada> notificacaoOpt = notificacaoRepository
                .findAguardandoDataByTelefone(telefoneLimpo, instanceName);

        if (notificacaoOpt.isEmpty()) {
            return ConfirmacaoPendenteResponse.builder()
                    .aguardandoData(false)
                    .build();
        }

        NotificacaoEnviada notificacao = notificacaoOpt.get();
        Agendamento agendamento = notificacao.getAgendamento();

        return ConfirmacaoPendenteResponse.builder()
                .aguardandoData(true)
                .agendamentoId(agendamento.getId())
                .notificacaoId(notificacao.getId())
                .telefone(telefoneLimpo)
                .instanceName(instanceName)
                .funcionarioId(agendamento.getFuncionarios().isEmpty() ? null :
                        agendamento.getFuncionarios().get(0).getId())
                .servicoIds(agendamento.getServicos().stream()
                        .map(s -> s.getId())
                        .collect(Collectors.toList()))
                .organizacaoId(agendamento.getOrganizacao().getId())
                .build();
    }

    /**
     * Verifica se o cliente está aguardando selecionar horário
     */
    public ConfirmacaoPendenteResponse verificarAguardandoHorario(String telefone, String instanceName) {
        String telefoneLimpo = limparTelefone(telefone);

        Optional<NotificacaoEnviada> notificacaoOpt = notificacaoRepository
                .findAguardandoHorarioByTelefone(telefoneLimpo, instanceName);

        if (notificacaoOpt.isEmpty()) {
            return ConfirmacaoPendenteResponse.builder()
                    .aguardandoHorario(false)
                    .build();
        }

        NotificacaoEnviada notificacao = notificacaoOpt.get();
        Agendamento agendamento = notificacao.getAgendamento();

        // Parse dos horários disponíveis salvos
        List<HorarioDisponivelResponse> horarios = parseHorariosDisponiveis(
                notificacao.getHorariosDisponiveis());

        return ConfirmacaoPendenteResponse.builder()
                .aguardandoHorario(true)
                .agendamentoId(agendamento.getId())
                .notificacaoId(notificacao.getId())
                .telefone(telefoneLimpo)
                .instanceName(instanceName)
                .funcionarioId(agendamento.getFuncionarios().isEmpty() ? null :
                        agendamento.getFuncionarios().get(0).getId())
                .servicoIds(agendamento.getServicos().stream()
                        .map(s -> s.getId())
                        .collect(Collectors.toList()))
                .organizacaoId(agendamento.getOrganizacao().getId())
                .dataDesejada(notificacao.getDataDesejadaReagendamento() != null ?
                        notificacao.getDataDesejadaReagendamento().toString() : null)
                .horariosDisponiveis(horarios)
                .build();
    }

    /**
     * Marca notificação como aguardando data (cliente respondeu REAGENDAR)
     */
    @Transactional
    public void marcarAguardandoData(Long notificacaoId) {
        NotificacaoEnviada notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notificação não encontrada: " + notificacaoId));

        notificacao.setStatus(StatusEnvio.AGUARDANDO_DATA);
        notificacao.setRespostaCliente("REAGENDAR");
        notificacao.setDtResposta(LocalDateTime.now());

        notificacaoRepository.save(notificacao);
    }

    /**
     * Marca notificação como aguardando horário e salva os horários disponíveis
     */
    @Transactional
    public void marcarAguardandoHorario(Long notificacaoId, String dataDesejada,
                                        List<HorarioDisponivelResponse> horarios) {
        NotificacaoEnviada notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notificação não encontrada: " + notificacaoId));

        notificacao.setStatus(StatusEnvio.AGUARDANDO_HORARIO);
        notificacao.setDataDesejadaReagendamento(LocalDate.parse(dataDesejada));

        // Salvar horários como JSON
        try {
            String horariosJson = objectMapper.writeValueAsString(horarios);
            notificacao.setHorariosDisponiveis(horariosJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar horários", e);
        }

        notificacaoRepository.save(notificacao);
    }

    /**
     * Marca notificação como concluída (reagendamento finalizado)
     */
    @Transactional
    public void marcarConcluida(Long notificacaoId) {
        NotificacaoEnviada notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notificação não encontrada: " + notificacaoId));

        // Determinar o status final baseado na resposta do cliente
        if ("REAGENDAR".equals(notificacao.getRespostaCliente())) {
            notificacao.setStatus(StatusEnvio.REAGENDADO);
        } else if ("SIM".equals(notificacao.getRespostaCliente())) {
            notificacao.setStatus(StatusEnvio.CONFIRMADO);
        } else if ("NAO".equals(notificacao.getRespostaCliente())) {
            notificacao.setStatus(StatusEnvio.CANCELADO_CLIENTE);
        }

        notificacaoRepository.save(notificacao);
    }

    /**
     * Registra a resposta do cliente
     */
    @Transactional
    public void registrarResposta(Long notificacaoId, String resposta) {
        NotificacaoEnviada notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notificação não encontrada: " + notificacaoId));

        String respostaNormalizada = resposta.toUpperCase().trim();

        if (!respostaNormalizada.equals("SIM") &&
            !respostaNormalizada.equals("NAO") &&
            !respostaNormalizada.equals("REAGENDAR")) {
            throw new IllegalArgumentException("Resposta inválida: " + resposta);
        }

        notificacao.setRespostaCliente(respostaNormalizada);
        notificacao.setDtResposta(LocalDateTime.now());

        // Atualizar status baseado na resposta
        switch (respostaNormalizada) {
            case "SIM":
                notificacao.setStatus(StatusEnvio.CONFIRMADO);
                break;
            case "NAO":
                notificacao.setStatus(StatusEnvio.CANCELADO_CLIENTE);
                break;
            case "REAGENDAR":
                notificacao.setStatus(StatusEnvio.AGUARDANDO_DATA);
                break;
        }

        notificacaoRepository.save(notificacao);
    }

    /**
     * Limpa o telefone removendo caracteres especiais
     */
    private String limparTelefone(String telefone) {
        if (telefone == null) return "";
        return telefone.replaceAll("[^0-9]", "");
    }

    /**
     * Parse dos horários disponíveis salvos como JSON
     */
    private List<HorarioDisponivelResponse> parseHorariosDisponiveis(String horariosJson) {
        if (horariosJson == null || horariosJson.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(horariosJson,
                    new TypeReference<List<HorarioDisponivelResponse>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
