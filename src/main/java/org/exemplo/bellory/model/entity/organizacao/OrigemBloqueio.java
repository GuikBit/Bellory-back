package org.exemplo.bellory.model.entity.organizacao;

public enum OrigemBloqueio {
    NACIONAL("Feriado Nacional"),
    MANUAL("Cadastro Manual");

    private final String descricao;

    OrigemBloqueio(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
