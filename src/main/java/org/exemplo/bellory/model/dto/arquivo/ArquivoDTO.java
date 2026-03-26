package org.exemplo.bellory.model.dto.arquivo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArquivoDTO {

    private Long id;
    private String nomeOriginal;
    private String extensao;
    private String contentType;
    private Long tamanho;
    private String tamanhoFormatado;
    private String url;
    private String caminhoRelativo;
    private Long pastaId;
    private String pastaNome;
    private LocalDateTime dtCriacao;
    private Long criadoPor;

    /**
     * true = arquivo gerenciado pelo sistema (fotos perfil, imagens servico, etc).
     * Arquivos de sistema nao podem ser deletados/movidos/renomeados por este modulo.
     */
    private Boolean sistema;
}
