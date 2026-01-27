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

    // Construtor para JPQL projection
    public NotificacaoPendenteDTO(Long agendamentoId, LocalDateTime dtAgendamento,
            String nomeCliente, String telefoneCliente, Long organizacaoId,
            String organizacaoNome, TipoNotificacao tipo, Integer horasAntes,
            String mensagemTemplate, String instanceName) {
        this.agendamentoId = agendamentoId;
        this.dtAgendamento = dtAgendamento;
        this.nomeCliente = nomeCliente;
        this.telefoneCliente = telefoneCliente;
        this.organizacaoId = organizacaoId;
        this.organizacaoNome = organizacaoNome;
        this.tipo = tipo;
        this.horasAntes = horasAntes;
        this.mensagemTemplate = mensagemTemplate;
        this.instanceName = instanceName;
    }
}
