package org.exemplo.bellory.model.entity.organizacao;

public enum TipoBloqueioOrganizacao {
    FERIADO("Feriado"),
    BLOQUEIO("Bloqueio");

    private final String descricao;

    TipoBloqueioOrganizacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
