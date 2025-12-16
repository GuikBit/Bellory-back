package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServicoPublicDTO {
    private Long id;
    private String nome;
    private String descricao;
    private Long categoriaId;
    private String categoriaNome;
    private String genero;              // "Masculino", "Feminino", "Unissex"
    private Integer tempoEstimadoMinutos;
    private BigDecimal preco;
    private BigDecimal precoComDesconto;
    private Integer descontoPercentual;
    private List<String> imagens;
    private Boolean disponivel;
    private List<Long> funcionarioIds;   // Funcionários que atendem
}
