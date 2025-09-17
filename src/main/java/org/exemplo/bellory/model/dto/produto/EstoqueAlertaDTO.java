package org.exemplo.bellory.model.dto.produto;

import lombok.Data;

@Data
public class EstoqueAlertaDTO {
    private Long produtoId;
    private String nomeProduto;
    private String marca;
    private Integer quantidadeAtual;
    private Integer estoqueMinimo;
    private String nivel; // CRITICO, BAIXO, ZERADO
    private String imagemPrincipal;
}
