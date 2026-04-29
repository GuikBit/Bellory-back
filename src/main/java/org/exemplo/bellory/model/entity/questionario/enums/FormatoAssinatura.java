package org.exemplo.bellory.model.entity.questionario.enums;

public enum FormatoAssinatura {
    PNG_BASE64("PNG (Base64)"),
    SVG("SVG");

    private final String descricao;

    FormatoAssinatura(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
