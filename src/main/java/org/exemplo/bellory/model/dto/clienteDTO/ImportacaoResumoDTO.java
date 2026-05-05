package org.exemplo.bellory.model.dto.clienteDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.importacao.StatusImportacao;

import java.time.LocalDateTime;

/**
 * Resumo leve de uma importacao para a tela de listagem.
 *
 * <p>Difere de {@link ImportacaoStatusDTO} por NAO incluir o array de {@code erros}
 * (que pode crescer indefinidamente em CSVs grandes). Quando o frontend clicar
 * em uma importacao, ele faz polling no endpoint de status individual para
 * receber a lista completa de erros.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportacaoResumoDTO {

    private Long id;
    private StatusImportacao status;
    private String nomeArquivo;

    private Integer totalLinhas;
    private Integer processadas;
    private Integer importados;
    private Integer ignorados;

    // Percentual processado (0-100). Se totalLinhas == 0 -> 0.
    private Integer percentual;

    // Preenchido somente se status == FALHA (util para mostrar tooltip na lista).
    private String mensagemFalha;

    private LocalDateTime dtInicio;
    private LocalDateTime dtFim;
}
