package org.exemplo.bellory.model.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.plano.*;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.entity.plano.PlanoLimitesBellory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanoBelloryMapper {

    private final ObjectMapper objectMapper;

    // ===================== Entity -> Response DTO =====================

    public PlanoBelloryResponseDTO toResponseDTO(PlanoBellory entity, Long totalOrganizacoes) {
        if (entity == null) return null;

        return PlanoBelloryResponseDTO.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .nome(entity.getNome())
                .tagline(entity.getTagline())
                .descricaoCompleta(entity.getDescricaoCompleta())
                .ativo(entity.isAtivo())
                .popular(entity.isPopular())
                .cta(entity.getCta())
                .badge(entity.getBadge())
                .icone(entity.getIcone())
                .cor(entity.getCor())
                .gradiente(entity.getGradiente())
                .precoMensal(entity.getPrecoMensal())
                .precoAnual(entity.getPrecoAnual())
                .descontoPercentualAnual(entity.getDescontoPercentualAnual())
                .features(parseFeatures(entity.getFeatures()))
                .limites(toLimitesDTO(entity.getLimites()))
                .ordemExibicao(entity.getOrdemExibicao())
                .dtCriacao(entity.getDtCriacao())
                .dtAtualizacao(entity.getDtAtualizacao())
                .userCriacao(entity.getUserCriacao())
                .userAtualizacao(entity.getUserAtualizacao())
                .totalOrganizacoesUsando(totalOrganizacoes)
                .build();
    }

    // ===================== Entity -> Public DTO =====================

    public PlanoBelloryPublicDTO toPublicDTO(PlanoBellory entity) {
        if (entity == null) return null;

        return PlanoBelloryPublicDTO.builder()
                .id(entity.getCodigo())
                .name(entity.getNome())
                .tagline(entity.getTagline())
                .description(entity.getDescricaoCompleta())
                .popular(entity.isPopular())
                .cta(entity.getCta())
                .badge(entity.getBadge())
                .icon(entity.getIcone())
                .color(entity.getCor())
                .gradient(entity.getGradiente())
                .price(entity.getPrecoMensal())
                .yearlyPrice(entity.getPrecoAnual())
                .yearlyDiscount(entity.getDescontoPercentualAnual())
                .features(parseFeatures(entity.getFeatures()))
                .limits(toLimitesDTO(entity.getLimites()))
                .build();
    }

    // ===================== Create DTO -> Entity =====================

    public PlanoBellory toEntity(PlanoBelloryCreateDTO dto) {
        if (dto == null) return null;

        PlanoBellory entity = new PlanoBellory();
        entity.setCodigo(dto.getCodigo());
        entity.setNome(dto.getNome());
        entity.setTagline(dto.getTagline());
        entity.setDescricaoCompleta(dto.getDescricaoCompleta());
        entity.setPopular(dto.isPopular());
        entity.setCta(dto.getCta());
        entity.setBadge(dto.getBadge());
        entity.setIcone(dto.getIcone());
        entity.setCor(dto.getCor());
        entity.setGradiente(dto.getGradiente());
        entity.setPrecoMensal(dto.getPrecoMensal());
        entity.setPrecoAnual(dto.getPrecoAnual());
        entity.setDescontoPercentualAnual(dto.getDescontoPercentualAnual());
        entity.setFeatures(serializeFeatures(dto.getFeatures()));
        entity.setOrdemExibicao(dto.getOrdemExibicao());
        entity.setAtivo(true);

        if (dto.getLimites() != null) {
            PlanoLimitesBellory limites = toLimitesEntity(dto.getLimites());
            limites.setPlano(entity);
            entity.setLimites(limites);
        }

        return entity;
    }

    // ===================== Update DTO -> Entity (partial) =====================

    public void updateEntity(PlanoBellory entity, PlanoBelloryUpdateDTO dto) {
        if (dto == null || entity == null) return;

        if (dto.getCodigo() != null) entity.setCodigo(dto.getCodigo());
        if (dto.getNome() != null) entity.setNome(dto.getNome());
        if (dto.getTagline() != null) entity.setTagline(dto.getTagline());
        if (dto.getDescricaoCompleta() != null) entity.setDescricaoCompleta(dto.getDescricaoCompleta());
        if (dto.getPopular() != null) entity.setPopular(dto.getPopular());
        if (dto.getCta() != null) entity.setCta(dto.getCta());
        if (dto.getBadge() != null) entity.setBadge(dto.getBadge());
        if (dto.getIcone() != null) entity.setIcone(dto.getIcone());
        if (dto.getCor() != null) entity.setCor(dto.getCor());
        if (dto.getGradiente() != null) entity.setGradiente(dto.getGradiente());
        if (dto.getPrecoMensal() != null) entity.setPrecoMensal(dto.getPrecoMensal());
        if (dto.getPrecoAnual() != null) entity.setPrecoAnual(dto.getPrecoAnual());
        if (dto.getDescontoPercentualAnual() != null) entity.setDescontoPercentualAnual(dto.getDescontoPercentualAnual());
        if (dto.getFeatures() != null) entity.setFeatures(serializeFeatures(dto.getFeatures()));
        if (dto.getOrdemExibicao() != null) entity.setOrdemExibicao(dto.getOrdemExibicao());

        if (dto.getLimites() != null) {
            if (entity.getLimites() != null) {
                updateLimitesEntity(entity.getLimites(), dto.getLimites());
            } else {
                PlanoLimitesBellory limites = toLimitesEntity(dto.getLimites());
                limites.setPlano(entity);
                entity.setLimites(limites);
            }
        }
    }

    // ===================== Limites Mapping =====================

    public PlanoLimitesDTO toLimitesDTO(PlanoLimitesBellory entity) {
        if (entity == null) return null;

        return PlanoLimitesDTO.builder()
                .maxAgendamentosMes(entity.getMaxAgendamentosMes())
                .maxUsuarios(entity.getMaxUsuarios())
                .maxClientes(entity.getMaxClientes())
                .maxServicos(entity.getMaxServicos())
                .maxUnidades(entity.getMaxUnidades())
                .permiteAgendamentoOnline(entity.isPermiteAgendamentoOnline())
                .permiteWhatsapp(entity.isPermiteWhatsapp())
                .permiteSite(entity.isPermiteSite())
                .permiteEcommerce(entity.isPermiteEcommerce())
                .permiteRelatoriosAvancados(entity.isPermiteRelatoriosAvancados())
                .permiteApi(entity.isPermiteApi())
                .permiteIntegracaoPersonalizada(entity.isPermiteIntegracaoPersonalizada())
                .suportePrioritario(entity.isSuportePrioritario())
                .suporte24x7(entity.isSuporte24x7())
                .build();
    }

    private PlanoLimitesBellory toLimitesEntity(PlanoLimitesDTO dto) {
        if (dto == null) return null;

        return PlanoLimitesBellory.builder()
                .maxAgendamentosMes(dto.getMaxAgendamentosMes())
                .maxUsuarios(dto.getMaxUsuarios())
                .maxClientes(dto.getMaxClientes())
                .maxServicos(dto.getMaxServicos())
                .maxUnidades(dto.getMaxUnidades())
                .permiteAgendamentoOnline(dto.isPermiteAgendamentoOnline())
                .permiteWhatsapp(dto.isPermiteWhatsapp())
                .permiteSite(dto.isPermiteSite())
                .permiteEcommerce(dto.isPermiteEcommerce())
                .permiteRelatoriosAvancados(dto.isPermiteRelatoriosAvancados())
                .permiteApi(dto.isPermiteApi())
                .permiteIntegracaoPersonalizada(dto.isPermiteIntegracaoPersonalizada())
                .suportePrioritario(dto.isSuportePrioritario())
                .suporte24x7(dto.isSuporte24x7())
                .build();
    }

    private void updateLimitesEntity(PlanoLimitesBellory entity, PlanoLimitesDTO dto) {
        entity.setMaxAgendamentosMes(dto.getMaxAgendamentosMes());
        entity.setMaxUsuarios(dto.getMaxUsuarios());
        entity.setMaxClientes(dto.getMaxClientes());
        entity.setMaxServicos(dto.getMaxServicos());
        entity.setMaxUnidades(dto.getMaxUnidades());
        entity.setPermiteAgendamentoOnline(dto.isPermiteAgendamentoOnline());
        entity.setPermiteWhatsapp(dto.isPermiteWhatsapp());
        entity.setPermiteSite(dto.isPermiteSite());
        entity.setPermiteEcommerce(dto.isPermiteEcommerce());
        entity.setPermiteRelatoriosAvancados(dto.isPermiteRelatoriosAvancados());
        entity.setPermiteApi(dto.isPermiteApi());
        entity.setPermiteIntegracaoPersonalizada(dto.isPermiteIntegracaoPersonalizada());
        entity.setSuportePrioritario(dto.isSuportePrioritario());
        entity.setSuporte24x7(dto.isSuporte24x7());
    }

    // ===================== Features JSON helpers =====================

    public List<PlanoFeatureDTO> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(featuresJson, new TypeReference<List<PlanoFeatureDTO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Erro ao fazer parse das features do plano: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public String serializeFeatures(List<PlanoFeatureDTO> features) {
        if (features == null || features.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(features);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar features do plano: {}", e.getMessage());
            return null;
        }
    }
}
