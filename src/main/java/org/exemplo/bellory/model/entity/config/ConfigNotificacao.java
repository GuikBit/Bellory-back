package org.exemplo.bellory.model.entity.config;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigNotificacao {
    @Column(name = "enviar_lembrete_whatsapp")
    private Boolean enviarLembreteWhatsapp = false;

    @Column(name = "enviar_lembrete_sms")
    private Boolean enviarLembreteSMS = false;

    @Column(name = "enviar_lembrete_email")
    private Boolean enviarLembreteEmail = false;

    @Column(name = "enviar_confitmacao_fora_horario")
    private Boolean enviarConfirmacaoForaHorario = false;

    @Column(name = "tempo_confirmacao")
    private Integer tempoParaConfirmacao;

    @Column(name = "tempo_lembrete_pos_confirmacao")
    private Integer tempoLembretePosConfirmacao;
}
