package org.exemplo.bellory.model.dto.notificacao;

import lombok.*;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoPendenteDTO {
    private Long agendamentoId;
    private Long organizacaoId;
    private String nomeOrganizacao;
    private String nomeCliente;
    private String telefoneCliente;
    private String nomeServico;
    private String nomeFuncionario;
    private LocalDateTime dataAgendamento;
    private BigDecimal valor;
    private String endereco;
    private TipoNotificacao tipo;
    private Integer horasAntes;
    private String mensagemTemplate;
    private String InstanceName;
}
