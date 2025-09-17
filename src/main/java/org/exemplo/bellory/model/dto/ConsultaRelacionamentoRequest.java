package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ConsultaRelacionamentoRequest {
    private List<Long> funcionarioIds;
    private List<Long> servicoIds;
}
