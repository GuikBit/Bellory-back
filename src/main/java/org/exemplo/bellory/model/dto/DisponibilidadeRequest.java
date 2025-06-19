package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadeRequest {
    private Long funcionarioId; // ID do funcionário que o cliente deseja agendar
    private Long organizacaoId; // ID da organização, se o funcionário pertencer a uma específica
    private LocalDate dataDesejada; // O dia que o cliente quer agendar
    private List<Long> servicoIds; // IDs dos serviços que o cliente deseja realizar
}
