package org.exemplo.bellory.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.plano.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.mapper.PlanoBelloryMapper;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoBelloryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPlanoBelloryService {

    private final PlanoBelloryRepository planoBelloryRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PlanoBelloryMapper mapper;

    // ===================== CRUD Admin =====================

    @Transactional(readOnly = true)
    public List<PlanoBelloryResponseDTO> listarTodos() {
        return planoBelloryRepository.findAllByOrderByOrdemExibicaoAsc()
                .stream()
                .map(plano -> {
                    Long total = planoBelloryRepository.countOrganizacoesByPlanoId(plano.getId());
                    return mapper.toResponseDTO(plano, total);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanoBelloryResponseDTO buscarPorId(Long id) {
        PlanoBellory plano = planoBelloryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano nao encontrado: " + id));
        Long total = planoBelloryRepository.countOrganizacoesByPlanoId(plano.getId());
        return mapper.toResponseDTO(plano, total);
    }

    @Transactional
    public PlanoBelloryResponseDTO criar(PlanoBelloryCreateDTO dto) {
        // Validar codigo unico
        if (planoBelloryRepository.findByCodigo(dto.getCodigo()).isPresent()) {
            throw new RuntimeException("Ja existe um plano com o codigo: " + dto.getCodigo());
        }

        PlanoBellory entity = mapper.toEntity(dto);
        entity.setUserCriacao(TenantContext.getCurrentUserId());

        PlanoBellory salvo = planoBelloryRepository.save(entity);
        log.info("Plano criado: {} ({})", salvo.getNome(), salvo.getCodigo());

        return mapper.toResponseDTO(salvo, 0L);
    }

    @Transactional
    public PlanoBelloryResponseDTO atualizar(Long id, PlanoBelloryUpdateDTO dto) {
        PlanoBellory entity = planoBelloryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano nao encontrado: " + id));

        // Validar codigo unico (se estiver sendo alterado)
        if (dto.getCodigo() != null && !dto.getCodigo().equals(entity.getCodigo())) {
            if (planoBelloryRepository.existsByCodigoAndIdNot(dto.getCodigo(), id)) {
                throw new RuntimeException("Ja existe um plano com o codigo: " + dto.getCodigo());
            }
        }

        mapper.updateEntity(entity, dto);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());

        PlanoBellory salvo = planoBelloryRepository.save(entity);
        Long total = planoBelloryRepository.countOrganizacoesByPlanoId(salvo.getId());
        log.info("Plano atualizado: {} ({})", salvo.getNome(), salvo.getCodigo());

        return mapper.toResponseDTO(salvo, total);
    }

    @Transactional
    public void desativar(Long id) {
        PlanoBellory entity = planoBelloryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano nao encontrado: " + id));

        Long orgsUsando = planoBelloryRepository.countOrganizacoesByPlanoId(id);
        if (orgsUsando > 0) {
            throw new RuntimeException(
                    "Não é possível desativar o plano '" + entity.getNome() +
                    "'. Existem " + orgsUsando + " organizacao(oes) usando este plano.");
        }

        entity.setAtivo(false);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());
        planoBelloryRepository.save(entity);
        log.info("Plano desativado: {} ({})", entity.getNome(), entity.getCodigo());
    }

    @Transactional
    public PlanoBelloryResponseDTO ativar(Long id) {
        PlanoBellory entity = planoBelloryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano nao encontrado: " + id));

        entity.setAtivo(true);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());
        PlanoBellory salvo = planoBelloryRepository.save(entity);
        Long total = planoBelloryRepository.countOrganizacoesByPlanoId(salvo.getId());
        log.info("Plano ativado: {} ({})", entity.getNome(), entity.getCodigo());

        return mapper.toResponseDTO(salvo, total);
    }

    @Transactional
    public void reordenar(ReordenarPlanosDTO dto) {
        for (ReordenarPlanosDTO.PlanoOrdemDTO item : dto.getPlanos()) {
            PlanoBellory plano = planoBelloryRepository.findById(item.getId())
                    .orElseThrow(() -> new RuntimeException("Plano nao encontrado: " + item.getId()));
            plano.setOrdemExibicao(item.getOrdemExibicao());
            planoBelloryRepository.save(plano);
        }
        log.info("Planos reordenados: {} itens", dto.getPlanos().size());
    }

    // ===================== Listagem Publica =====================

    @Transactional(readOnly = true)
    public List<PlanoBelloryPublicDTO> listarPlanosPublicos() {
        return planoBelloryRepository.findByAtivoTrueOrderByOrdemExibicaoAsc()
                .stream()
                .map(mapper::toPublicDTO)
                .collect(Collectors.toList());
    }

    // ===================== Listagem Interna (org autenticada) =====================

    @Transactional(readOnly = true)
    public PlanoOrganizacaoDTO listarPlanosOrganizacao() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Organizacao org = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada: " + organizacaoId));

        PlanoBellory planoAtual = org.getPlano();

        List<PlanoBelloryPublicDTO> planosDisponiveis = planoBelloryRepository
                .findByAtivoTrueOrderByOrdemExibicaoAsc()
                .stream()
                .map(mapper::toPublicDTO)
                .collect(Collectors.toList());

        return PlanoOrganizacaoDTO.builder()
                .planoAtualCodigo(planoAtual != null ? planoAtual.getCodigo() : null)
                .planoAtualNome(planoAtual != null ? planoAtual.getNome() : null)
                .planosDisponiveis(planosDisponiveis)
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
