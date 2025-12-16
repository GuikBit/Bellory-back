package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuncionarioPublicDTO {
    private Long id;
    private String nome;
    private String apelido;
    private String foto;
    private String cargo;
    private String descricao;
    private List<Long> servicoIds;          // IDs dos serviços que atende
    private List<String> servicoNomes;       // Nomes dos serviços que atende
    private List<JornadaDiaPublicDTO> horarios;
}
