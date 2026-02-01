package org.exemplo.bellory.model.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmacaoPendenteResponse {

    private boolean temConfirmacaoPendente;
    private boolean aguardandoData;
    private boolean aguardandoHorario;

    private Long agendamentoId;
    private Long notificacaoId;
    private String telefone;
    private String instanceName;
    private String clienteNome;
    private Long funcionarioId;
    private List<Long> servicoIds;
    private Long organizacaoId;

    // Para quando está aguardando horário
    private String dataDesejada;
    private List<HorarioDisponivelResponse> horariosDisponiveis;
}
