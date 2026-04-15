package org.exemplo.bellory.model.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateDTO {
    private Long clienteId;
    private Long funcionarioId;
    private List<Long> servicoIds;
    private LocalDate data;
    private LocalTime horario;
    private String observacao;
}
