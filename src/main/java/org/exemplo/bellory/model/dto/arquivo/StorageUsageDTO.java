package org.exemplo.bellory.model.dto.arquivo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageUsageDTO {

    private Long organizacaoId;
    private Long totalBytes;
    private String totalFormatado;
    private Integer totalArquivos;
    private Integer totalPastas;
    private Long limiteMb;
    private String limiteFormatado;
    private Double percentualUsado;
    private Boolean limiteAtingido;
}
