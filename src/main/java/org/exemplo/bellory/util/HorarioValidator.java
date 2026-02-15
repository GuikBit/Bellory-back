package org.exemplo.bellory.util;

import org.exemplo.bellory.model.dto.HorarioTrabalhoDTO;
import org.exemplo.bellory.model.dto.JornadaDiaDTO;
import org.springframework.stereotype.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class HorarioValidator {

    public boolean isHorarioValido(String horario) {
        if (horario == null || horario.trim().isEmpty()) {
            return false;
        }

        try {
            LocalTime.parse(horario, DateTimeFormatter.ofPattern("HH:mm"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public boolean isIntervaloValido(String horaInicio, String horaFim) {
        if (!isHorarioValido(horaInicio) || !isHorarioValido(horaFim)) {
            return false;
        }

        LocalTime inicio = LocalTime.parse(horaInicio, DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime fim = LocalTime.parse(horaFim, DateTimeFormatter.ofPattern("HH:mm"));

        return fim.isAfter(inicio);
    }

    public boolean hasHorariosSobrepostos(List<HorarioTrabalhoDTO> horarios) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < horarios.size(); i++) {
            for (int j = i + 1; j < horarios.size(); j++) {
                HorarioTrabalhoDTO h1 = horarios.get(i);
                HorarioTrabalhoDTO h2 = horarios.get(j);

                LocalTime h1Inicio = h1.getHoraInicio();
                LocalTime h1Fim =h1.getHoraFim();
                LocalTime h2Inicio = h2.getHoraInicio();
                LocalTime h2Fim = h2.getHoraFim();

                // Verifica sobreposição
                if ((h1Inicio.isBefore(h2Fim) && h1Fim.isAfter(h2Inicio)) ||
                        (h2Inicio.isBefore(h1Fim) && h2Fim.isAfter(h1Inicio))) {
                    return true;
                }
            }
        }
        return false;
    }

    public String validarPlanoHorarios(List<JornadaDiaDTO> planoHorarios) {
        if (planoHorarios == null || planoHorarios.isEmpty()) {
            return null; // Plano de horários é opcional
        }

        for (JornadaDiaDTO dia : planoHorarios) {
            // Valida dia da semana
            if (dia.getDiaSemana() == null || dia.getDiaSemana().trim().isEmpty()) {
                return "Dia da semana não pode estar vazio";
            }

            // Valida se tem horários quando ativo
            if (dia.getAtivo() && (dia.getHorarios() == null || dia.getHorarios().isEmpty())) {
                return "Dia " + dia.getDiaSemana() + " está ativo mas não possui horários definidos";
            }

            // Valida cada horário
            if (dia.getHorarios() != null) {
                for (HorarioTrabalhoDTO horario : dia.getHorarios()) {
                    // Valida se os campos estão preenchidos
                    if (horario.getHoraInicio() == null) {
                        return "Dia " + dia.getDiaSemana() + ": Hora de início não preenchida";
                    }

                    if (horario.getHoraFim() == null ) {
                        return "Dia " + dia.getDiaSemana() + ": Hora de fim não preenchida";
                    }

                    // Valida formato
                    if (!isHorarioValido(horario.getHoraInicio().toString())) {
                        return "Dia " + dia.getDiaSemana() + ": Hora de início inválida (" + horario.getHoraInicio() + ")";
                    }

                    if (!isHorarioValido(horario.getHoraFim().toString())) {
                        return "Dia " + dia.getDiaSemana() + ": Hora de fim inválida (" + horario.getHoraFim() + ")";
                    }

                    // Valida intervalo
                    if (!isIntervaloValido(horario.getHoraInicio().toString(), horario.getHoraFim().toString())) {
                        return "Dia " + dia.getDiaSemana() + ": Hora de fim deve ser maior que hora de início";
                    }
                }

                // Valida sobreposição
                if (hasHorariosSobrepostos(dia.getHorarios())) {
                    return "Dia " + dia.getDiaSemana() + ": Há sobreposição entre os horários definidos";
                }
            }
        }

        return null; // Sem erros
    }
}
