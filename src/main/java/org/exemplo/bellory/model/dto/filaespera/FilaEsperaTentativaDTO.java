package org.exemplo.bellory.model.dto.filaespera;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.fila.FilaEsperaTentativa;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaEsperaTentativaDTO {
    private Long id;
    private Long agendamentoId;
    private Long clienteId;
    private String nomeCliente;
    private String telefoneCliente;
    private Long funcionarioId;
    private LocalDateTime slotInicio;
    private LocalDateTime slotFim;
    private LocalDateTime dtAgendamentoOriginal;
    private String status;
    private LocalDateTime dtEnvio;
    private LocalDateTime dtResposta;
    private LocalDateTime dtExpira;
    private Integer cascataNivel;
    private boolean finalizada;

    public static FilaEsperaTentativaDTO from(FilaEsperaTentativa t) {
        return FilaEsperaTentativaDTO.builder()
                .id(t.getId())
                .agendamentoId(t.getAgendamento() != null ? t.getAgendamento().getId() : null)
                .clienteId(t.getAgendamento() != null && t.getAgendamento().getCliente() != null
                        ? t.getAgendamento().getCliente().getId() : null)
                .nomeCliente(t.getAgendamento() != null && t.getAgendamento().getCliente() != null
                        ? t.getAgendamento().getCliente().getNomeCompleto() : null)
                .telefoneCliente(t.getAgendamento() != null && t.getAgendamento().getCliente() != null
                        ? t.getAgendamento().getCliente().getTelefone() : null)
                .funcionarioId(t.getFuncionarioId())
                .slotInicio(t.getSlotInicio())
                .slotFim(t.getSlotFim())
                .dtAgendamentoOriginal(t.getAgendamento() != null ? t.getAgendamento().getDtAgendamento() : null)
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .dtEnvio(t.getDtEnvio())
                .dtResposta(t.getDtResposta())
                .dtExpira(t.getDtExpira())
                .cascataNivel(t.getCascataNivel())
                .finalizada(t.isFinalizada())
                .build();
    }
}
