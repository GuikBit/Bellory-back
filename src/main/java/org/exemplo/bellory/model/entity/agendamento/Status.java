package org.exemplo.bellory.model.entity.agendamento; // Ajuste o pacote conforme a sua estrutura

public enum Status {
    PENDENTE("Pendente"),
    AGENDADO("Agendado"),
    CONFIRMADO("Confirmado"),
    AGUARDANDO_CONFIRMACAO( "A confirmar"),
    EM_ESPERA("Em Espera"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado"),
    EM_ANDAMENTO("Em Anamento"),
    NAO_COMPARECEU("Nao Compareceu"),
    REAGENDADO("Reagendado"),
    VENCIDA("Vencida"),
    PAGO("Pago");

    private final String descricao;

    Status(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
