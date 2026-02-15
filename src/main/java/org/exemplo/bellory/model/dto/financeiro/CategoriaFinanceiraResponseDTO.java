package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.financeiro.CategoriaFinanceira;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaFinanceiraResponseDTO {
    private Long id;
    private Long categoriaPaiId;
    private String categoriaPaiNome;
    private String nome;
    private String descricao;
    private String tipo;
    private String tipoDescricao;
    private String cor;
    private String icone;
    private Boolean ativo;
    private List<CategoriaFinanceiraResponseDTO> subcategorias = new ArrayList<>();

    public CategoriaFinanceiraResponseDTO(CategoriaFinanceira entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.tipo = entity.getTipo() != null ? entity.getTipo().name() : null;
        this.tipoDescricao = entity.getTipo() != null ? entity.getTipo().getDescricao() : null;
        this.cor = entity.getCor();
        this.icone = entity.getIcone();
        this.ativo = entity.getAtivo();

        if (entity.getCategoriaPai() != null) {
            this.categoriaPaiId = entity.getCategoriaPai().getId();
            this.categoriaPaiNome = entity.getCategoriaPai().getNome();
        }

        if (entity.getSubcategorias() != null && !entity.getSubcategorias().isEmpty()) {
            this.subcategorias = entity.getSubcategorias().stream()
                    .map(CategoriaFinanceiraResponseDTO::new)
                    .collect(Collectors.toList());
        }
    }
}
