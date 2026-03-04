package org.exemplo.bellory.service.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.cupom.*;
import org.exemplo.bellory.model.entity.assinatura.CupomDesconto;
import org.exemplo.bellory.model.entity.assinatura.CupomUtilizacao;
import org.exemplo.bellory.model.entity.assinatura.TipoDesconto;
import org.exemplo.bellory.model.repository.assinatura.CupomDescontoRepository;
import org.exemplo.bellory.model.repository.assinatura.CupomUtilizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCupomDescontoService {

    private final CupomDescontoRepository cupomDescontoRepository;
    private final CupomUtilizacaoRepository cupomUtilizacaoRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CupomDescontoResponseDTO> listarTodos() {
        return cupomDescontoRepository.findAllByOrderByDtCriacaoDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CupomDescontoResponseDTO> listarVigentes() {
        return cupomDescontoRepository.findVigentes()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CupomDescontoResponseDTO buscarPorId(Long id) {
        CupomDesconto cupom = cupomDescontoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupom nao encontrado: " + id));
        return toResponseDTO(cupom);
    }

    @Transactional
    public CupomDescontoResponseDTO criar(CupomDescontoCreateDTO dto) {
        String codigo = dto.getCodigo().toUpperCase().trim();

        if (cupomDescontoRepository.findByCodigo(codigo).isPresent()) {
            throw new RuntimeException("Ja existe um cupom com o codigo: " + codigo);
        }

        TipoDesconto tipo = TipoDesconto.valueOf(dto.getTipoDesconto());

        CupomDesconto entity = CupomDesconto.builder()
                .codigo(codigo)
                .descricao(dto.getDescricao())
                .tipoDesconto(tipo)
                .valorDesconto(dto.getValorDesconto())
                .dtInicio(dto.getDtInicio())
                .dtFim(dto.getDtFim())
                .maxUtilizacoes(dto.getMaxUtilizacoes())
                .maxUtilizacoesPorOrg(dto.getMaxUtilizacoesPorOrg())
                .planosPermitidos(toJson(dto.getPlanosPermitidos()))
                .segmentosPermitidos(toJson(dto.getSegmentosPermitidos()))
                .organizacoesPermitidas(toJson(dto.getOrganizacoesPermitidas()))
                .cicloCobranca(dto.getCicloCobranca())
                .userCriacao(TenantContext.getCurrentUserId())
                .build();

        CupomDesconto salvo = cupomDescontoRepository.save(entity);
        log.info("Cupom criado: {} ({})", salvo.getCodigo(), salvo.getId());
        return toResponseDTO(salvo);
    }

    @Transactional
    public CupomDescontoResponseDTO atualizar(Long id, CupomDescontoUpdateDTO dto) {
        CupomDesconto entity = cupomDescontoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupom nao encontrado: " + id));

        if (dto.getCodigo() != null) {
            String novoCodigo = dto.getCodigo().toUpperCase().trim();
            if (!novoCodigo.equals(entity.getCodigo()) && cupomDescontoRepository.existsByCodigoAndIdNot(novoCodigo, id)) {
                throw new RuntimeException("Ja existe um cupom com o codigo: " + novoCodigo);
            }
            entity.setCodigo(novoCodigo);
        }
        if (dto.getDescricao() != null) entity.setDescricao(dto.getDescricao());
        if (dto.getTipoDesconto() != null) entity.setTipoDesconto(TipoDesconto.valueOf(dto.getTipoDesconto()));
        if (dto.getValorDesconto() != null) entity.setValorDesconto(dto.getValorDesconto());
        if (dto.getDtInicio() != null) entity.setDtInicio(dto.getDtInicio());
        if (dto.getDtFim() != null) entity.setDtFim(dto.getDtFim());
        if (dto.getMaxUtilizacoes() != null) entity.setMaxUtilizacoes(dto.getMaxUtilizacoes());
        if (dto.getMaxUtilizacoesPorOrg() != null) entity.setMaxUtilizacoesPorOrg(dto.getMaxUtilizacoesPorOrg());
        if (dto.getPlanosPermitidos() != null) entity.setPlanosPermitidos(toJson(dto.getPlanosPermitidos()));
        if (dto.getSegmentosPermitidos() != null) entity.setSegmentosPermitidos(toJson(dto.getSegmentosPermitidos()));
        if (dto.getOrganizacoesPermitidas() != null) entity.setOrganizacoesPermitidas(toJson(dto.getOrganizacoesPermitidas()));
        if (dto.getCicloCobranca() != null) entity.setCicloCobranca(dto.getCicloCobranca());

        entity.setUserAtualizacao(TenantContext.getCurrentUserId());

        CupomDesconto salvo = cupomDescontoRepository.save(entity);
        log.info("Cupom atualizado: {} ({})", salvo.getCodigo(), salvo.getId());
        return toResponseDTO(salvo);
    }

    @Transactional
    public void desativar(Long id) {
        CupomDesconto entity = cupomDescontoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupom nao encontrado: " + id));
        entity.setAtivo(false);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());
        cupomDescontoRepository.save(entity);
        log.info("Cupom desativado: {} ({})", entity.getCodigo(), entity.getId());
    }

    @Transactional
    public CupomDescontoResponseDTO ativar(Long id) {
        CupomDesconto entity = cupomDescontoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupom nao encontrado: " + id));
        entity.setAtivo(true);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());
        CupomDesconto salvo = cupomDescontoRepository.save(entity);
        log.info("Cupom ativado: {} ({})", entity.getCodigo(), entity.getId());
        return toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<CupomUtilizacaoDTO> listarUtilizacoes(Long cupomId) {
        cupomDescontoRepository.findById(cupomId)
                .orElseThrow(() -> new RuntimeException("Cupom nao encontrado: " + cupomId));

        return cupomUtilizacaoRepository.findByCupomIdOrderByDtUtilizacaoDesc(cupomId)
                .stream()
                .map(this::toUtilizacaoDTO)
                .collect(Collectors.toList());
    }

    // ==================== CONVERSORES ====================

    private CupomDescontoResponseDTO toResponseDTO(CupomDesconto entity) {
        return CupomDescontoResponseDTO.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .descricao(entity.getDescricao())
                .tipoDesconto(entity.getTipoDesconto().name())
                .valorDesconto(entity.getValorDesconto())
                .dtInicio(entity.getDtInicio())
                .dtFim(entity.getDtFim())
                .maxUtilizacoes(entity.getMaxUtilizacoes())
                .maxUtilizacoesPorOrg(entity.getMaxUtilizacoesPorOrg())
                .totalUtilizado(entity.getTotalUtilizado())
                .planosPermitidos(fromJson(entity.getPlanosPermitidos(), new TypeReference<List<String>>() {}))
                .segmentosPermitidos(fromJson(entity.getSegmentosPermitidos(), new TypeReference<List<String>>() {}))
                .organizacoesPermitidas(fromJson(entity.getOrganizacoesPermitidas(), new TypeReference<List<Long>>() {}))
                .cicloCobranca(entity.getCicloCobranca())
                .ativo(entity.getAtivo())
                .vigente(entity.isVigente())
                .dtCriacao(entity.getDtCriacao())
                .dtAtualizacao(entity.getDtAtualizacao())
                .build();
    }

    private CupomUtilizacaoDTO toUtilizacaoDTO(CupomUtilizacao entity) {
        return CupomUtilizacaoDTO.builder()
                .id(entity.getId())
                .cupomId(entity.getCupom().getId())
                .cupomCodigo(entity.getCupom().getCodigo())
                .organizacaoId(entity.getOrganizacaoId())
                .assinaturaId(entity.getAssinaturaId())
                .cobrancaId(entity.getCobrancaId())
                .valorOriginal(entity.getValorOriginal())
                .valorDesconto(entity.getValorDesconto())
                .valorFinal(entity.getValorFinal())
                .planoCodigo(entity.getPlanoCodigo())
                .cicloCobranca(entity.getCicloCobranca())
                .dtUtilizacao(entity.getDtUtilizacao())
                .build();
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar para JSON: {}", e.getMessage());
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("Erro ao parsear JSON: {}", e.getMessage());
            return null;
        }
    }
}
