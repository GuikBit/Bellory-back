package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.notificacao.ConfigNotificacao;
import org.exemplo.bellory.model.entity.notificacao.NotificacaoEnviada;
import org.exemplo.bellory.model.entity.notificacao.StatusEnvio;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacaoEnviadaResumoDTO {

    private Long id;
    private TipoNotificacao tipo;
    private Integer horasAntes;
    private StatusEnvio status;
    private LocalDateTime dtEnvio;
    private LocalDateTime dtResposta;
    private String respostaCliente;
    private String telefoneDestino;
    private String whatsappMessageId;
    private String erroMensagem;
    private LocalDate dataDesejadaReagendamento;
    private LocalDateTime dtCriacao;
    private String mensagemTemplate;

    public NotificacaoEnviadaResumoDTO(NotificacaoEnviada n) {
        this(n, null);
    }

    public NotificacaoEnviadaResumoDTO(NotificacaoEnviada n, ConfigNotificacao config) {
        this.id = n.getId();
        this.tipo = n.getTipo();
        this.horasAntes = n.getHorasAntes();
        this.status = n.getStatus();
        this.dtEnvio = n.getDtEnvio();
        this.dtResposta = n.getDtResposta();
        this.respostaCliente = n.getRespostaCliente();
        this.telefoneDestino = n.getTelefoneDestino();
        this.whatsappMessageId = n.getWhatsappMessageId();
        this.erroMensagem = n.getErroMensagem();
        this.dataDesejadaReagendamento = n.getDataDesejadaReagendamento();
        this.dtCriacao = n.getDtCriacao();
        this.mensagemTemplate = config != null ? config.getMensagemTemplate() : null;
    }
}
