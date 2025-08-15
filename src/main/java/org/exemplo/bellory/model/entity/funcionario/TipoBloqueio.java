package org.exemplo.bellory.model.entity.funcionario; // No mesmo pacote ou em um pacote de enums

public enum TipoBloqueio {
    AGENDAMENTO("Agendamento de Cliente"),
    ALMOCO("Horário de Almoço"),
    REUNIAO("Reunião"),
    PAUSA("Pausa"),
    FERIAS("Ferias"),
    OUTRO("Outro Bloqueio");

    private final String descricao;

    TipoBloqueio(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
