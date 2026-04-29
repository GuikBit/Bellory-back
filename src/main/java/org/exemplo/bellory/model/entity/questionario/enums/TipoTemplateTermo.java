package org.exemplo.bellory.model.entity.questionario.enums;

public enum TipoTemplateTermo {
    PADRAO_BELLORY("Padrão Bellory"),
    PADRAO_PROCEDIMENTO("Padrão Procedimento"),
    PADRAO_PROCEDIMENTO_QUIMICO("Padrão Procedimento Químico"),
    PADRAO_USO_IMAGEM("Padrão Uso de Imagem"),
    CUSTOM("Customizado");

    private final String descricao;

    TipoTemplateTermo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
