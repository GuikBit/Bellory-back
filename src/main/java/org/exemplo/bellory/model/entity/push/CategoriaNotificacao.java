package org.exemplo.bellory.model.entity.push;

import lombok.Getter;

@Getter
public enum CategoriaNotificacao {
    AGENDAMENTO("Calendar"),
    CLIENTE("Users"),
    FINANCEIRO("CreditCard"),
    PAGAMENTO("DollarSign"),
    AGENTE_VIRTUAL("Bot"),
    SISTEMA("Monitor"),
    AVISO("AlertTriangle");

    private final String icone;

    CategoriaNotificacao(String icone) {
        this.icone = icone;
    }
}
