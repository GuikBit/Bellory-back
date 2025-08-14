package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgendamentoFiltroDTO {
    private Long clienteId;
    private List<Long> funcionarioIds;
    private List<String> status;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private List<Long> servicoIds;
    private String nomeCliente;
    private String nomeFuncionario;
    private String nomeServico;
}
