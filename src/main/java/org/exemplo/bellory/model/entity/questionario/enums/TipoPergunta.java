package org.exemplo.bellory.model.entity.questionario.enums;

public enum TipoPergunta {
    TEXTO_CURTO("Texto Curto"),
    TEXTO_LONGO("Texto Longo"),
    NUMERO("Número"),
    SELECAO_UNICA("Seleção Única"),
    SELECAO_MULTIPLA("Seleção Múltipla"),
    ESCALA("Escala Linear"),
    DATA("Data"),
    HORA("Hora"),
    AVALIACAO_ESTRELAS("Avaliação por Estrelas"),
    SIM_NAO("Sim/Não");

    private final String descricao;

    TipoPergunta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
