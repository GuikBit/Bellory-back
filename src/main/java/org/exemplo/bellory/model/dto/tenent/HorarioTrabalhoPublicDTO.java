package org.exemplo.bellory.model.dto.tenent;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HorarioTrabalhoPublicDTO {
    private String inicio;              // "09:00"
    private String fim;                 // "18:00"
}
