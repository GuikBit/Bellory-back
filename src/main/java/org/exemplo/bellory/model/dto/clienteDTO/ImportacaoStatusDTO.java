package org.exemplo.bellory.model.dto.clienteDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.importacao.StatusImportacao;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportacaoStatusDTO {

    private Long id;
    private StatusImportacao status;
    private String nomeArquivo;

    private Integer totalLinhas;
    private Integer processadas;
    private Integer importados;
    private Integer ignorados;

    // Percentual processado (0-100). Se totalLinhas == 0 -> 0.
    private Integer percentual;

    // Lista de erros por linha. Populada conforme o processamento avanca.
    private List<ImportacaoErroDTO> erros;

    // Preenchido somente se status == FALHA (erro fatal abortou a importacao).
    private String mensagemFalha;

    private LocalDateTime dtInicio;
    private LocalDateTime dtFim;
}
