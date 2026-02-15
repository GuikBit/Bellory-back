package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.organizacao.HorarioFuncionamentoCreateDTO;
import org.exemplo.bellory.model.dto.organizacao.HorarioFuncionamentoResponseDTO;
import org.exemplo.bellory.model.dto.organizacao.PeriodoFuncionamentoDTO;
import org.exemplo.bellory.model.entity.funcionario.DiaSemana;
import org.exemplo.bellory.model.entity.organizacao.HorarioFuncionamento;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.organizacao.PeriodoFuncionamento;
import org.exemplo.bellory.model.repository.organizacao.HorarioFuncionamentoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HorarioFuncionamentoService {

    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private final OrganizacaoRepository organizacaoRepository;

    public HorarioFuncionamentoService(HorarioFuncionamentoRepository horarioFuncionamentoRepository,
                                       OrganizacaoRepository organizacaoRepository) {
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
        this.organizacaoRepository = organizacaoRepository;
    }

    /**
     * Lista os 7 dias da semana com seus períodos.
     * Se não existirem registros, cria automaticamente os 7 dias (todos inativos, sem períodos).
     */
    @Transactional
    public List<HorarioFuncionamentoResponseDTO> listar() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        List<HorarioFuncionamento> existentes = horarioFuncionamentoRepository.findByOrganizacaoIdWithPeriodos(organizacaoId);

        if (existentes.isEmpty()) {
            existentes = criarDiasPadrao(organizacaoId);
        }

        return existentes.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza um dia específico (upsert): limpa períodos antigos e adiciona novos.
     */
    @Transactional
    public HorarioFuncionamentoResponseDTO atualizarDia(String diaSemanaStr, HorarioFuncionamentoCreateDTO dto) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        DiaSemana diaSemana = parseDiaSemana(diaSemanaStr);

        HorarioFuncionamento horario = horarioFuncionamentoRepository
                .findByOrganizacaoIdAndDiaSemana(organizacaoId, diaSemana)
                .orElseGet(() -> {
                    Organizacao org = organizacaoRepository.findById(organizacaoId)
                            .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));
                    HorarioFuncionamento novo = new HorarioFuncionamento();
                    novo.setOrganizacao(org);
                    novo.setDiaSemana(diaSemana);
                    return novo;
                });

        horario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        // Limpa períodos antigos e flush para que o orphanRemoval
        // execute os DELETEs antes dos INSERTs dos novos períodos
        horario.getPeriodos().clear();
        if (horario.getId() != null) {
            horarioFuncionamentoRepository.saveAndFlush(horario);
        }

        // Adiciona novos períodos
        if (dto.getPeriodos() != null) {
            for (PeriodoFuncionamentoDTO periodoDTO : dto.getPeriodos()) {
                if (periodoDTO.getHoraInicio() != null && periodoDTO.getHoraFim() != null) {
                    PeriodoFuncionamento periodo = new PeriodoFuncionamento();
                    periodo.setHoraInicio(periodoDTO.getHoraInicio());
                    periodo.setHoraFim(periodoDTO.getHoraFim());
                    horario.addPeriodo(periodo);
                }
            }
        }

        HorarioFuncionamento saved = horarioFuncionamentoRepository.saveAndFlush(horario);
        return converterParaDTO(saved);
    }

    /**
     * Bulk update de todos os 7 dias.
     */
    @Transactional
    public List<HorarioFuncionamentoResponseDTO> atualizarTodos(List<HorarioFuncionamentoCreateDTO> dtos) {
        List<HorarioFuncionamentoResponseDTO> resultado = new ArrayList<>();
        for (HorarioFuncionamentoCreateDTO dto : dtos) {
            resultado.add(atualizarDia(dto.getDiaSemana(), dto));
        }
        return resultado;
    }

    /**
     * Toggle ativo/inativo de um dia.
     */
    @Transactional
    public HorarioFuncionamentoResponseDTO toggleDia(String diaSemanaStr, Boolean ativo) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        DiaSemana diaSemana = parseDiaSemana(diaSemanaStr);

        HorarioFuncionamento horario = horarioFuncionamentoRepository
                .findByOrganizacaoIdAndDiaSemana(organizacaoId, diaSemana)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Horário de funcionamento não encontrado para o dia " + diaSemanaStr));

        horario.setAtivo(ativo);
        HorarioFuncionamento saved = horarioFuncionamentoRepository.save(horario);
        return converterParaDTO(saved);
    }

    /**
     * Busca horários de funcionamento por organizacaoId (para uso público, sem TenantContext).
     */
    public List<HorarioFuncionamento> findByOrganizacaoId(Long organizacaoId) {
        return horarioFuncionamentoRepository.findByOrganizacaoIdWithPeriodos(organizacaoId);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private List<HorarioFuncionamento> criarDiasPadrao(Long organizacaoId) {
        Organizacao org = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        List<HorarioFuncionamento> dias = new ArrayList<>();
        for (DiaSemana dia : DiaSemana.values()) {
            HorarioFuncionamento horario = new HorarioFuncionamento();
            horario.setOrganizacao(org);
            horario.setDiaSemana(dia);
            horario.setAtivo(false);
            dias.add(horario);
        }

        return horarioFuncionamentoRepository.saveAll(dias);
    }

    private DiaSemana parseDiaSemana(String diaSemanaStr) {
        try {
            return DiaSemana.valueOf(diaSemanaStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            try {
                return DiaSemana.fromDescricao(diaSemanaStr);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Dia da semana inválido: " + diaSemanaStr);
            }
        }
    }

    private HorarioFuncionamentoResponseDTO converterParaDTO(HorarioFuncionamento horario) {
        List<PeriodoFuncionamentoDTO> periodosDTO = horario.getPeriodos().stream()
                .map(p -> new PeriodoFuncionamentoDTO(p.getHoraInicio(), p.getHoraFim()))
                .collect(Collectors.toList());

        return HorarioFuncionamentoResponseDTO.builder()
                .id(horario.getId())
                .diaSemana(horario.getDiaSemana().name())
                .diaSemanaLabel(horario.getDiaSemana().getDescricao())
                .ativo(horario.getAtivo())
                .periodos(periodosDTO)
                .build();
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }
        return organizacaoId;
    }
}
