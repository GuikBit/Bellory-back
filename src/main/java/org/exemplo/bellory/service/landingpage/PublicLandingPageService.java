package org.exemplo.bellory.service.landingpage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.landingpage.LandingPageDTO;
import org.exemplo.bellory.model.dto.landingpage.LandingPageSectionDTO;
import org.exemplo.bellory.model.entity.landingpage.LandingPage;
import org.exemplo.bellory.model.entity.landingpage.LandingPageSection;
import org.exemplo.bellory.model.repository.landingpage.LandingPageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para acesso público (sem autenticação) a landing pages publicadas.
 * Resolve a organização pelo slug da URL, não pelo TenantContext.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PublicLandingPageService {

    private final LandingPageRepository landingPageRepository;
    private final ObjectMapper objectMapper;

    /**
     * Lista páginas publicadas de uma organização (sem seções).
     */
    public List<LandingPageDTO> listPublishedPages(String orgSlug) {
        return landingPageRepository.findPublishedByOrganizacaoSlug(orgSlug)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retorna uma página publicada com todas as seções visíveis.
     */
    public Optional<LandingPageDTO> getPublishedPage(String orgSlug, String pageSlug) {
        return landingPageRepository.findPublishedByOrgSlugAndPageSlug(orgSlug, pageSlug)
                .map(this::convertToDTOWithSections);
    }

    /**
     * Retorna a home page publicada com todas as seções visíveis.
     */
    public Optional<LandingPageDTO> getPublishedHomePage(String orgSlug) {
        return landingPageRepository.findPublishedHomeByOrgSlug(orgSlug)
                .map(this::convertToDTOWithSections);
    }

    // ==================== CONVERSION ====================

    private LandingPageDTO convertToDTO(LandingPage entity) {
        return LandingPageDTO.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .nome(entity.getNome())
                .descricao(entity.getDescricao())
                .tipo(entity.getTipo())
                .isHome(entity.getIsHome())
                .status(entity.getStatus())
                .versao(entity.getVersao())
                .dtPublicacao(entity.getDtPublicacao())
                .dtCriacao(entity.getDtCriacao())
                .dtAtualizacao(entity.getDtAtualizacao())
                .build();
    }

    private LandingPageDTO convertToDTOWithSections(LandingPage entity) {
        LandingPageDTO dto = convertToDTO(entity);

        dto.setGlobalSettings(deserialize(entity.getGlobalSettings(),
                LandingPageDTO.GlobalSettingsDTO.class));
        dto.setSeoSettings(deserialize(entity.getSeoSettings(),
                LandingPageDTO.SeoSettingsDTO.class));
        dto.setCustomCss(entity.getCustomCss());
        dto.setCustomJs(entity.getCustomJs());
        dto.setFaviconUrl(entity.getFaviconUrl());

        if (entity.getSections() != null) {
            dto.setSections(entity.getSections().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getAtivo()))
                    .filter(s -> Boolean.TRUE.equals(s.getVisivel()))
                    .sorted(Comparator.comparingInt(LandingPageSection::getOrdem))
                    .map(this::convertSectionToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private LandingPageSectionDTO convertSectionToDTO(LandingPageSection entity) {
        return LandingPageSectionDTO.builder()
                .id(entity.getId())
                .sectionId(entity.getSectionId())
                .tipo(entity.getTipo())
                .nome(entity.getNome())
                .ordem(entity.getOrdem())
                .visivel(entity.getVisivel())
                .template(entity.getTemplate())
                .content(deserialize(entity.getContent(),
                        LandingPageSectionDTO.SectionContentDTO.class))
                .styles(deserialize(entity.getStyles(),
                        LandingPageSectionDTO.SectionStylesDTO.class))
                .settings(deserialize(entity.getSettings(),
                        new TypeReference<Map<String, Object>>() {}))
                .animations(deserialize(entity.getAnimations(),
                        LandingPageSectionDTO.AnimationDTO.class))
                .build();
    }

    // ==================== JSON HELPERS ====================

    private <T> T deserialize(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Erro ao desserializar JSON: {}", e.getMessage());
            return null;
        }
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("Erro ao desserializar JSON: {}", e.getMessage());
            return null;
        }
    }
}
