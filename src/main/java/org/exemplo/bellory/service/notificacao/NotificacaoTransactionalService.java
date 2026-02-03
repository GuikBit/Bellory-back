package org.exemplo.bellory.service.notificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.notificacao.NotificacaoPendenteDTO;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.notificacao.NotificacaoEnviada;
import org.exemplo.bellory.model.entity.notificacao.StatusEnvio;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.notificacao.NotificacaoEnviadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;

/**
 * Serviço separado para operações transacionais
 * Cada método abre e fecha sua própria transação (REQUIRES_NEW)
 * Isso garante commit imediato após cada operação
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacaoTransactionalService {

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoEnviadaRepository notificacaoEnviadaRepository;

    /**
     * Registra envio bem-sucedido
     * Transação independente com commit imediato
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEnvioSucesso(NotificacaoPendenteDTO notif, String telefone) {
        try {
            Agendamento agendamento = agendamentoRepository.findById(notif.getAgendamentoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Agendamento não encontrado: " + notif.getAgendamentoId()));

            // Cria registro de notificação
            NotificacaoEnviada registro = NotificacaoEnviada.builder()
                    .agendamento(agendamento)
                    .tipo(notif.getTipo())
                    .horasAntes(notif.getHorasAntes())
                    .dtEnvio(LocalDateTime.now())
                    .status(StatusEnvio.ENVIADO)
                    .telefoneDestino(telefone)
                    .instanceName(notif.getInstanceName())
                    .build();

            notificacaoEnviadaRepository.save(registro);

            // Atualiza status do agendamento apenas se for CONFIRMACAO
            if (notif.getTipo() == org.exemplo.bellory.model.entity.notificacao.TipoNotificacao.CONFIRMACAO) {
                agendamento.setStatus(Status.AGUARDANDO_CONFIRMACAO);
                agendamento.setDtAtualizacao(LocalDateTime.now());
                agendamentoRepository.save(agendamento);
            }

            // Commit automático ao sair do método
            log.debug("Registro de envio salvo com sucesso: ag={}", notif.getAgendamentoId());

        } catch (Exception e) {
            log.error("Erro ao registrar envio: {}", e.getMessage(), e);
            throw e; // Rollback da transação
        }
    }

    /**
     * Registra falha no envio
     * Transação independente com commit imediato
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEnvioFalha(NotificacaoPendenteDTO notif, String erroMsg, String telefone) {
        try {
            Agendamento agendamento = agendamentoRepository.findById(notif.getAgendamentoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Agendamento não encontrado: " + notif.getAgendamentoId()));

            NotificacaoEnviada registro = NotificacaoEnviada.builder()
                    .agendamento(agendamento)
                    .tipo(notif.getTipo())
                    .horasAntes(notif.getHorasAntes())
                    .dtEnvio(LocalDateTime.now())
                    .status(StatusEnvio.FALHA)
                    .erroMensagem(erroMsg)
                    .telefoneDestino(telefone)
                    .instanceName(notif.getInstanceName())
                    .build();

            notificacaoEnviadaRepository.save(registro);

            // Commit automático ao sair do método
            log.debug("Registro de falha salvo: ag={}", notif.getAgendamentoId());

        } catch (Exception e) {
            log.error("Erro ao registrar falha: {}", e.getMessage(), e);
            throw e;
        }
    }
}
