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
public class FeaturesDTO {
    private Boolean agendamentoOnline;
    private Boolean ecommerce;
    private Boolean planosClientes;
    private Boolean avaliacoes;
    private Boolean chat;
    private Boolean notificacoesPush;
}
