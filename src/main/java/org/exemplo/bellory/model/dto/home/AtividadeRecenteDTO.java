package org.exemplo.bellory.model.dto.home;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtividadeRecenteDTO {
    private String tipo;        // AGENDAMENTO, CLIENTE, COBRANCA
    private String descricao;
    private LocalDateTime data;
    private String icone;       // Hint pro frontend
}
