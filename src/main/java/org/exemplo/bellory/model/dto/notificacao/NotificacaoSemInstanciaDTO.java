package org.exemplo.bellory.model.dto.notificacao;

import lombok.*;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoSemInstanciaDTO {
    private Long agendamentoId;
    private Long organizacaoId;
    private String organizacaoNome;
    private String organizacaoEmail;
    private TipoNotificacao tipo;
    private Integer horasAntes;
    private LocalDateTime dtAgendamento;
}
