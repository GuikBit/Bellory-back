package org.exemplo.bellory.model.dto.arquivo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PastaArquivoDTO {

    private Long id;
    private String nome;
    private String caminhoCompleto;
    private Long pastaPaiId;
    private Integer totalArquivos;
    private Long tamanhoTotal;
    private String tamanhoTotalFormatado;
    private List<PastaArquivoDTO> subpastas;
    private List<ArquivoDTO> arquivos;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;

    /**
     * true = pasta do sistema (colaboradores, servicos, produtos, organizacao).
     * Pastas de sistema nao podem ser deletadas nem renomeadas.
     */
    private Boolean sistema;
}
