package org.exemplo.bellory.service.notificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.notificacao.NotificacaoSemInstanciaDTO;
import org.exemplo.bellory.service.EmailService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacaoAlertService {

    private final EmailService emailService;

    public void alertarInstanciasDesconectadas(List<NotificacaoSemInstanciaDTO> lista) {
        Map<Long, List<NotificacaoSemInstanciaDTO>> porOrg = lista.stream()
            .collect(Collectors.groupingBy(NotificacaoSemInstanciaDTO::getOrganizacaoId));

        porOrg.forEach((orgId, notifs) -> {
            NotificacaoSemInstanciaDTO primeiro = notifs.get(0);
            log.warn("Org {} ({}) tem {} notificacoes pendentes mas WhatsApp desconectado!",
                orgId, primeiro.getOrganizacaoNome(), notifs.size());

            if (primeiro.getOrganizacaoEmail() != null) {
                enviarEmailAlerta(primeiro, notifs.size());
            }
        });
    }

    private void enviarEmailAlerta(NotificacaoSemInstanciaDTO org, int qtdPendentes) {
        try {
            String assunto = "WhatsApp Desconectado - " + qtdPendentes + " notificacoes pendentes";
            String corpo = String.format("""
                <html>
                <body>
                <h2>Ola!</h2>

                <p>Sua instancia do WhatsApp esta desconectada e existem <strong>%d</strong>
                notificacoes pendentes para seus clientes.</p>

                <p>Acesse o painel Bellory e reconecte seu WhatsApp para que as notificacoes
                de agendamento sejam enviadas corretamente.</p>

                <br>
                <p>Equipe Bellory</p>
                </body>
                </html>
                """, qtdPendentes);

            emailService.enviarEmailSimples(org.getOrganizacaoEmail(), assunto, corpo);
            log.info("Email de alerta enviado para org {}: {}", org.getOrganizacaoId(), org.getOrganizacaoEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar email para org {}: {}", org.getOrganizacaoId(), e.getMessage());
        }
    }
}
