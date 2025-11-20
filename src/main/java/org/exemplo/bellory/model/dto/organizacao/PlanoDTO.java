package org.exemplo.bellory.model.dto.organizacao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanoDTO {
    // ✅ Aceita String ou converte para String temporariamente
    private String id;
    private String nome;
    private String periodicidade;
    private Double valor;
    private String formaPagamento;
    private DadosCartaoDTO dadosCartao;
}
