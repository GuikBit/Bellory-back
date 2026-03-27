package org.exemplo.bellory.model.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponseDTO {
    private Long id;
    private String status;
    private LocalDateTime dtAgendamento;
    private BigDecimal valorTotal;
    private String profissional;
    private List<String> servicos;
    private Boolean requerSinal;
    private BigDecimal percentualSinal;
}
