package org.exemplo.bellory.model.dto.notificacao;

import lombok.*;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoPendenteDTO {
    private Long agendamentoId;
    private LocalDateTime dtAgendamento;
    private String nomeCliente;
    private String telefoneCliente;
    private Long organizacaoId;
    private String organizacaoNome;
    private TipoNotificacao tipo;
    private Integer horasAntes;
    private String mensagemTemplate;
    private String instanceName;

}
