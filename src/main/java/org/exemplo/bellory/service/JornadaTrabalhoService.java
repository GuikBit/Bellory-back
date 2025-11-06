package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.dto.HorarioTrabalhoDTO;
import org.exemplo.bellory.model.dto.JornadaDiaCreateUpdateDTO;
import org.exemplo.bellory.model.dto.JornadaDiaDTO;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.funcionario.HorarioTrabalhoRepository;
import org.exemplo.bellory.model.repository.funcionario.JornadaDiaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JornadaTrabalhoService {

    private final JornadaDiaRepository jornadaDiaRepository;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final FuncionarioRepository funcionarioRepository;

    public JornadaTrabalhoService(JornadaDiaRepository jornadaDiaRepository,
                                  HorarioTrabalhoRepository horarioTrabalhoRepository,
                                  FuncionarioRepository funcionarioRepository) {
        this.jornadaDiaRepository = jornadaDiaRepository;
        this.horarioTrabalhoRepository = horarioTrabalhoRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    /**
     * Busca todas as jornadas de trabalho de um funcionário
     */
    public List<JornadaDiaDTO> getJornadasByFuncionario(Long funcionarioId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        List<JornadaDia> jornadas = jornadaDiaRepository.findByFuncionarioId(funcionarioId);

        return jornadas.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Cria ou atualiza uma jornada de trabalho para um dia específico
     */
    @Transactional
    public JornadaDiaDTO criarOuAtualizarJornada(Long funcionarioId, JornadaDiaCreateUpdateDTO dto) {
        // Validações
        if (dto.getDiaSemana() == null || dto.getDiaSemana().trim().isEmpty()) {
            throw new IllegalArgumentException("O dia da semana é obrigatório.");
        }

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        DiaSemana diaSemana;
        try {
            // Tenta primeiro pelo nome do enum (SEGUNDA, TERCA, etc.)
            diaSemana = DiaSemana.valueOf(dto.getDiaSemana().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Se falhar, tenta pela descrição (Segunda-feira, Sexta-Feira, etc.)
            diaSemana = DiaSemana.fromDescricao(dto.getDiaSemana());
        }

        // Busca ou cria a jornada do dia
        JornadaDia jornadaDia = jornadaDiaRepository.findByFuncionarioAndDiaSemana(funcionario, diaSemana)
                .orElse(new JornadaDia());

        jornadaDia.setFuncionario(funcionario);
        jornadaDia.setDiaSemana(diaSemana);
        jornadaDia.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        // Remove todos os horários antigos
        jornadaDia.getHorarios().clear();

        // Adiciona os novos horários
        if (dto.getHorarios() != null && !dto.getHorarios().isEmpty()) {
            for (HorarioTrabalhoDTO horarioDTO : dto.getHorarios()) {
                if (horarioDTO.getHoraInicio() != null && horarioDTO.getHoraFim() != null) {
                    HorarioTrabalho horario = new HorarioTrabalho();
                    horario.setId(horarioDTO.getId());
                    horario.setHoraInicio(horarioDTO.getHoraInicio());
                    horario.setHoraFim(horarioDTO.getHoraFim());
                    jornadaDia.addHorario(horario);
                }
            }
        }

        JornadaDia savedJornada = jornadaDiaRepository.save(jornadaDia);
        return converterParaDTO(savedJornada);
    }

    /**
     * Atualiza apenas o status ativo/inativo de um dia
     */
    @Transactional
    public JornadaDiaDTO atualizarStatusDia(Long funcionarioId, String diaSemanaStr, Boolean ativo) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        DiaSemana diaSemana;
        try {
            diaSemana = DiaSemana.valueOf(diaSemanaStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Dia da semana inválido: " + diaSemanaStr);
        }

        JornadaDia jornadaDia = jornadaDiaRepository.findByFuncionarioAndDiaSemana(funcionario, diaSemana)
                .orElseThrow(() -> new IllegalArgumentException("Jornada não encontrada para o dia " + diaSemanaStr));

        jornadaDia.setAtivo(ativo);
        JornadaDia savedJornada = jornadaDiaRepository.save(jornadaDia);

        return converterParaDTO(savedJornada);
    }

    /**
     * Deleta um horário específico dentro de um dia
     */
    @Transactional
    public void deletarHorario(Long funcionarioId, String diaSemanaStr, String horarioId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        DiaSemana diaSemana;
        try {
            diaSemana = DiaSemana.valueOf(diaSemanaStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Dia da semana inválido: " + diaSemanaStr);
        }

        JornadaDia jornadaDia = jornadaDiaRepository.findByFuncionarioAndDiaSemana(funcionario, diaSemana)
                .orElseThrow(() -> new IllegalArgumentException("Jornada não encontrada para o dia " + diaSemanaStr));

        HorarioTrabalho horario = horarioTrabalhoRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horário com ID " + horarioId + " não encontrado."));

        // Verifica se o horário pertence à jornada do dia
        if (!horario.getJornadaDia().getId().equals(jornadaDia.getId())) {
            throw new IllegalArgumentException("O horário não pertence à jornada especificada.");
        }

        jornadaDia.removeHorario(horario);
        horarioTrabalhoRepository.delete(horario);
    }

    /**
     * Deleta uma jornada completa de um dia
     */
    @Transactional
    public void deletarJornadaDia(Long funcionarioId, String diaSemanaStr) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + funcionarioId + " não encontrado."));

        DiaSemana diaSemana;
        try {
            // Tenta primeiro pelo nome do enum (SEGUNDA, TERCA, etc.)
            diaSemana = DiaSemana.valueOf(diaSemanaStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Se falhar, tenta pela descrição (Segunda-feira, Sexta-Feira, etc.)
            diaSemana = DiaSemana.fromDescricao(diaSemanaStr);
        }

        JornadaDia jornadaDia = jornadaDiaRepository.findByFuncionarioAndDiaSemana(funcionario, diaSemana)
                .orElseThrow(() -> new IllegalArgumentException("Jornada não encontrada para o dia " + diaSemanaStr));

        jornadaDiaRepository.delete(jornadaDia);
    }

    /**
     * Converte a entidade JornadaDia para DTO
     */
    private JornadaDiaDTO converterParaDTO(JornadaDia jornadaDia) {
        List<HorarioTrabalhoDTO> horariosDTO = jornadaDia.getHorarios().stream()
                .map(h -> new HorarioTrabalhoDTO(h.getId(), h.getHoraInicio(), h.getHoraFim()))
                .collect(Collectors.toList());

        return new JornadaDiaDTO(
                jornadaDia.getId(),
                jornadaDia.getDiaSemana().getDescricao(),
                jornadaDia.getAtivo(),
                horariosDTO
        );
    }
}
