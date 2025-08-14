package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgendamentoCreateDTO {
    private Long organizacaoId;
    private Long clienteId;
    private List<Long> servicoIds;
    private List<Long> funcionarioIds;
    private LocalDateTime dtAgendamento;
    private String observacao;
}
