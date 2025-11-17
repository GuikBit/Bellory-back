package org.exemplo.bellory.model.entity.funcionario;

import java.time.DayOfWeek;

public enum DiaSemana {
    SEGUNDA("Segunda-feira"),
    TERCA("Terça-feira"),
    QUARTA("Quarta-feira"),
    QUINTA("Quinta-feira"),
    SEXTA("Sexta-feira"),
    SABADO("Sábado"),
    DOMINGO("Domingo");

    private final String descricao;

    DiaSemana(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    // Método para converter DayOfWeek
    public static DiaSemana fromDayOfWeek(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return SEGUNDA;
            case TUESDAY: return TERCA;
            case WEDNESDAY: return QUARTA;
            case THURSDAY: return QUINTA;
            case FRIDAY: return SEXTA;
            case SATURDAY: return SABADO;
            case SUNDAY: return DOMINGO;
            default: throw new IllegalArgumentException("Dia da semana inválido: " + dayOfWeek);
        }
    }

    // --- NOVO MÉTODO: Converter descrição para enum ---
    public static DiaSemana fromDescricao(String descricao) {
        if (descricao == null) {
            throw new IllegalArgumentException("Descrição não pode ser nula");
        }

        // Normaliza a string removendo acentos e hífens, e convertendo para maiúsculas
        String normalizada = descricao.trim()
                .toUpperCase()
                .replace("Ç", "C")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Ó", "O")
                .replace("-", "");

        for (DiaSemana dia : DiaSemana.values()) {
            String descricaoNormalizada = dia.descricao
                    .toUpperCase()
                    .replace("Ç", "C")
                    .replace("Á", "A")
                    .replace("É", "E")
                    .replace("Ó", "O")
                    .replace("-", "");

            if (descricaoNormalizada.equals(normalizada)) {
                return dia;
            }
        }

        throw new IllegalArgumentException("Dia da semana inválido: " + descricao);
    }
}
