package org.exemplo.bellory.model.entity.agendamento; // Ajuste o pacote conforme a sua estrutura

public enum Status {
    PENDENTE("Pendente"),
    AGENDADO("Agendado"),
    CONFIRMADO("Confirmado"),
    EM_ESPERA("Em Espera"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado"),
    PAGO("Pago"),
    EM_ANDAMENTO("Em Anamento"),
    NAO_COMPARECEU("Nao Compareceu"),
    REAGENDADO("Reagendado"),
    VENCIDA("Vencida");


    private final String descricao;

    Status(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
