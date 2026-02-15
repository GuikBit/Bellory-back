package org.exemplo.bellory.model.entity.questionario.enums;

public enum TipoQuestionario {
    CLIENTE("Cadastro de Cliente"),
    COLABORADOR("Cadastro de Colaborador"),
    AVALIACAO_DESEMPENHO("Avaliação de Desempenho"),
    FEEDBACK_ATENDIMENTO("Feedback de Atendimento"),
    FEEDBACK_AGENDAMENTO("Feedback de Agendamento"),
    FEEDBACK_BOT("Feedback do Bot"),
    FEEDBACK_GERAL("Feedback Geral"),
    PESQUISA_SATISFACAO("Pesquisa de Satisfação"),
    OUTRO("Outro");

    private final String descricao;

    TipoQuestionario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
