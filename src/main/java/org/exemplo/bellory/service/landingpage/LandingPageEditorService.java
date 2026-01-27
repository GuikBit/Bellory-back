package org.exemplo.bellory.service.landingpage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.landingpage.*;
import org.exemplo.bellory.model.dto.landingpage.request.*;
import org.exemplo.bellory.model.entity.landingpage.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.landingpage.*;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para gerenciar o editor de Landing Pages.
 * Responsável por CRUD de páginas, seções e elementos.
 */
@Service
@Transactional
public class LandingPageEditorService {

    private final LandingPageRepository landingPageRepository;
    private final LandingPageSectionRepository sectionRepository;
    private final LandingPageVersionRepository versionRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final ObjectMapper objectMapper;

    public LandingPageEditorService(
            LandingPageRepository landingPageRepository,
            LandingPageSectionRepository sectionRepository,
            LandingPageVersionRepository versionRepository,
            OrganizacaoRepository organizacaoRepository,
            ObjectMapper objectMapper) {
        this.landingPageRepository = landingPageRepository;
        this.sectionRepository = sectionRepository;
        this.versionRepository = versionRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== LANDING PAGE CRUD ====================

    /**
     * Lista todas as landing pages da organização.
     */
    @Transactional(readOnly = true)
    public List<LandingPageDTO> listAll() {
        Long orgId = TenantContext.getOrganizacaoId();
        List<LandingPage> pages = landingPageRepository.findByOrganizacaoIdAndAtivoTrueOrderByDtCriacaoDesc(orgId);
        return pages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista landing pages com paginação.
     */
    @Transactional(readOnly = true)
    public Page<LandingPageDTO> listPaginated(Pageable pageable) {
        Long orgId = TenantContext.getOrganizacaoId();
        return landingPageRepository.findByOrganizacaoIdAndAtivoTrue(orgId, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca landing page por ID com todas as seções.
     */
    @Transactional(readOnly = true)
    public Optional<LandingPageDTO> getById(Long id) {
        Long orgId = TenantContext.getOrganizacaoId();
        return landingPageRepository.findByIdWithSections(id)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .map(this::convertToDTOWithSections);
    }

    /**
     * Busca landing page por slug.
     */
    @Transactional(readOnly = true)
    public Optional<LandingPageDTO> getBySlug(String slug) {
        Long orgId = TenantContext.getOrganizacaoId();
        return landingPageRepository.findByOrgAndSlugWithSections(orgId, slug)
                .map(this::convertToDTOWithSections);
    }

    /**
     * Cria uma nova landing page.
     */
    public LandingPageDTO create(CreateLandingPageRequest request) {
        Long orgId = TenantContext.getOrganizacaoId();
        Organizacao org = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada"));

        // Gerar slug se não fornecido
        String slug = request.getSlug();
        if (slug == null || slug.isEmpty()) {
            slug = generateSlug(request.getNome());
        }

        // Verificar se slug já existe
        if (landingPageRepository.existsByOrganizacaoIdAndSlug(orgId, slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        // Se for home, remover flag de outras páginas
        if (Boolean.TRUE.equals(request.getIsHome())) {
            landingPageRepository.findByOrganizacaoIdAndIsHomeTrue(orgId)
                    .ifPresent(existing -> {
                        existing.setIsHome(false);
                        landingPageRepository.save(existing);
                    });
        }

        LandingPage landingPage = LandingPage.builder()
                .organizacao(org)
                .slug(slug)
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .tipo(request.getTipo())
                .isHome(request.getIsHome())
                .status("DRAFT")
                .globalSettings(serializeJson(request.getGlobalSettings()))
                .seoSettings(serializeJson(request.getSeoSettings()))
                .versao(1)
                .ativo(true)
                .build();

        // Se tiver template, copiar seções
        if (request.getTemplateId() != null) {
            copyTemplateToPage(request.getTemplateId(), landingPage);
        } else {
            // Criar seções padrão
            createDefaultSections(landingPage);
        }

        landingPage = landingPageRepository.save(landingPage);

        // Criar versão inicial
        createVersion(landingPage, "Versão inicial", "MANUAL");

        return convertToDTOWithSections(landingPage);
    }

    /**
     * Atualiza uma landing page.
     */
    public LandingPageDTO update(Long id, UpdateLandingPageRequest request) {
        Long orgId = TenantContext.getOrganizacaoId();
        LandingPage landingPage = landingPageRepository.findByIdWithSections(id)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        // Atualizar campos básicos
        if (request.getNome() != null) {
            landingPage.setNome(request.getNome());
        }
        if (request.getSlug() != null && !request.getSlug().equals(landingPage.getSlug())) {
            if (landingPageRepository.existsByOrganizacaoIdAndSlug(orgId, request.getSlug())) {
                throw new RuntimeException("Slug já existe");
            }
            landingPage.setSlug(request.getSlug());
        }
        if (request.getDescricao() != null) {
            landingPage.setDescricao(request.getDescricao());
        }
        if (request.getTipo() != null) {
            landingPage.setTipo(request.getTipo());
        }
        if (request.getIsHome() != null) {
            if (Boolean.TRUE.equals(request.getIsHome())) {
                landingPageRepository.findByOrganizacaoIdAndIsHomeTrue(orgId)
                        .filter(existing -> !existing.getId().equals(id))
                        .ifPresent(existing -> {
                            existing.setIsHome(false);
                            landingPageRepository.save(existing);
                        });
            }
            landingPage.setIsHome(request.getIsHome());
        }
        if (request.getGlobalSettings() != null) {
            landingPage.setGlobalSettings(serializeJson(request.getGlobalSettings()));
        }
        if (request.getSeoSettings() != null) {
            landingPage.setSeoSettings(serializeJson(request.getSeoSettings()));
        }
        if (request.getCustomCss() != null) {
            landingPage.setCustomCss(request.getCustomCss());
        }
        if (request.getCustomJs() != null) {
            landingPage.setCustomJs(request.getCustomJs());
        }
        if (request.getFaviconUrl() != null) {
            landingPage.setFaviconUrl(request.getFaviconUrl());
        }

        // Atualizar seções se fornecidas
        if (request.getSections() != null) {
            updateSections(landingPage, request.getSections());
        }

        landingPage.setVersao(landingPage.getVersao() + 1);
        landingPage = landingPageRepository.save(landingPage);

        // Criar versão se solicitado
        if (Boolean.TRUE.equals(request.getCreateVersion())) {
            String desc = request.getVersionDescription() != null
                    ? request.getVersionDescription()
                    : "Atualização";
            createVersion(landingPage, desc, "MANUAL");
        }

        return convertToDTOWithSections(landingPage);
    }

    /**
     * Publica uma landing page.
     */
    public LandingPageDTO publish(Long id) {
        Long orgId = TenantContext.getOrganizacaoId();
        Long userId = TenantContext.getUserId();

        LandingPage landingPage = landingPageRepository.findByIdWithSections(id)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        landingPage.setStatus("PUBLISHED");
        landingPage.setDtPublicacao(LocalDateTime.now());
        landingPage.setPublicadoPor(userId);
        landingPage.setVersao(landingPage.getVersao() + 1);

        landingPage = landingPageRepository.save(landingPage);

        // Criar versão de publicação
        createVersion(landingPage, "Publicação", "PUBLISH");

        return convertToDTOWithSections(landingPage);
    }

    /**
     * Despublica uma landing page.
     */
    public LandingPageDTO unpublish(Long id) {
        Long orgId = TenantContext.getOrganizacaoId();
        LandingPage landingPage = landingPageRepository.findById(id)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        landingPage.setStatus("DRAFT");
        return convertToDTO(landingPageRepository.save(landingPage));
    }

    /**
     * Duplica uma landing page.
     */
    public LandingPageDTO duplicate(Long id, String novoNome) {
        Long orgId = TenantContext.getOrganizacaoId();
        LandingPage original = landingPageRepository.findByIdWithSections(id)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        String nome = novoNome != null ? novoNome : original.getNome() + " (Cópia)";
        String slug = generateSlug(nome);

        LandingPage copy = LandingPage.builder()
                .organizacao(original.getOrganizacao())
                .slug(slug)
                .nome(nome)
                .descricao(original.getDescricao())
                .tipo(original.getTipo())
                .isHome(false)
                .status("DRAFT")
                .globalSettings(original.getGlobalSettings())
                .seoSettings(original.getSeoSettings())
                .customCss(original.getCustomCss())
                .customJs(original.getCustomJs())
                .faviconUrl(original.getFaviconUrl())
                .versao(1)
                .ativo(true)
                .build();

        // Copiar seções
        for (LandingPageSection section : original.getSections()) {
            LandingPageSection sectionCopy = LandingPageSection.builder()
                    .sectionId(UUID.randomUUID().toString())
                    .tipo(section.getTipo())
                    .nome(section.getNome())
                    .ordem(section.getOrdem())
                    .visivel(section.getVisivel())
                    .template(section.getTemplate())
                    .content(section.getContent())
                    .styles(section.getStyles())
                    .settings(section.getSettings())
                    .animations(section.getAnimations())
                    .visibilityRules(section.getVisibilityRules())
                    .dataSource(section.getDataSource())
                    .locked(false)
                    .ativo(true)
                    .build();
            copy.addSection(sectionCopy);
        }

        copy = landingPageRepository.save(copy);
        createVersion(copy, "Cópia de " + original.getNome(), "MANUAL");

        return convertToDTOWithSections(copy);
    }

    /**
     * Deleta uma landing page (soft delete).
     */
    public void delete(Long id) {
        Long orgId = TenantContext.getOrganizacaoId();
        LandingPage landingPage = landingPageRepository.findById(id)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        landingPage.setAtivo(false);
        sectionRepository.softDeleteAllByLandingPageId(id);
        landingPageRepository.save(landingPage);
    }

    // ==================== SECTION CRUD ====================

    /**
     * Adiciona uma nova seção à landing page.
     */
    public LandingPageSectionDTO addSection(Long landingPageId, AddSectionRequest request) {
        Long orgId = TenantContext.getOrganizacaoId();
        LandingPage landingPage = landingPageRepository.findById(landingPageId)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        // Determinar ordem
        int ordem = request.getPosicao() != null
                ? request.getPosicao()
                : sectionRepository.findMaxOrdem(landingPageId) + 1;

        // Se inserindo no meio, ajustar ordem das outras seções
        if (request.getPosicao() != null) {
            List<LandingPageSection> sections = sectionRepository
                    .findByLandingPageIdAndAtivoTrueOrderByOrdemAsc(landingPageId);
            for (LandingPageSection s : sections) {
                if (s.getOrdem() >= ordem) {
                    s.setOrdem(s.getOrdem() + 1);
                    sectionRepository.save(s);
                }
            }
        }

        LandingPageSection section = LandingPageSection.builder()
                .landingPage(landingPage)
                .sectionId(UUID.randomUUID().toString())
                .tipo(request.getTipo())
                .nome(request.getNome() != null ? request.getNome() : getDefaultSectionName(request.getTipo()))
                .ordem(ordem)
                .visivel(true)
                .template(request.getTemplate())
                .content(serializeJson(request.getContent()))
                .styles(serializeJson(request.getStyles()))
                .settings(serializeJson(request.getSettings()))
                .locked(false)
                .ativo(true)
                .build();

        // Se tiver template de seção, copiar conteúdo
        if (request.getTemplateSectionId() != null) {
            sectionRepository.findById(request.getTemplateSectionId())
                    .ifPresent(template -> {
                        section.setContent(template.getContent());
                        section.setStyles(template.getStyles());
                        section.setSettings(template.getSettings());
                        section.setTemplate(template.getTemplate());
                    });
        } else if (request.getContent() == null) {
            // Criar conteúdo padrão para o tipo
            section.setContent(getDefaultSectionContent(request.getTipo()));
        }

        section = sectionRepository.save(section);
        return convertSectionToDTO(section);
    }

    /**
     * Atualiza uma seção.
     */
    public LandingPageSectionDTO updateSection(Long landingPageId, String sectionId, UpdateSectionRequest request) {
        Long orgId = TenantContext.getOrganizacaoId();

        // Verificar se landing page pertence à organização
        LandingPage landingPage = landingPageRepository.findById(landingPageId)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        LandingPageSection section = sectionRepository.findBySectionIdAndLandingPageId(sectionId, landingPageId)
                .orElseThrow(() -> new RuntimeException("Seção não encontrada"));

        if (Boolean.TRUE.equals(section.getLocked())) {
            throw new RuntimeException("Seção está bloqueada para edição");
        }

        if (request.getNome() != null) {
            section.setNome(request.getNome());
        }
        if (request.getTemplate() != null) {
            section.setTemplate(request.getTemplate());
        }
        if (request.getVisivel() != null) {
            section.setVisivel(request.getVisivel());
        }
        if (request.getOrdem() != null) {
            section.setOrdem(request.getOrdem());
        }
        if (request.getContent() != null) {
            section.setContent(serializeJson(request.getContent()));
        }
        if (request.getStyles() != null) {
            section.setStyles(serializeJson(request.getStyles()));
        }
        if (request.getSettings() != null) {
            section.setSettings(serializeJson(request.getSettings()));
        }
        if (request.getAnimations() != null) {
            section.setAnimations(serializeJson(request.getAnimations()));
        }
        if (request.getVisibilityRules() != null) {
            section.setVisibilityRules(serializeJson(request.getVisibilityRules()));
        }
        if (request.getDataSource() != null) {
            section.setDataSource(serializeJson(request.getDataSource()));
        }

        section = sectionRepository.save(section);
        return convertSectionToDTO(section);
    }

    /**
     * Reordena seções.
     */
    public List<LandingPageSectionDTO> reorderSections(Long landingPageId, ReorderSectionsRequest request) {
        Long orgId = TenantContext.getOrganizacaoId();

        landingPageRepository.findById(landingPageId)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        List<LandingPageSection> sections = sectionRepository
                .findByLandingPageIdAndAtivoTrueOrderByOrdemAsc(landingPageId);

        // Criar mapa de sectionId -> section
        Map<String, LandingPageSection> sectionMap = sections.stream()
                .collect(Collectors.toMap(LandingPageSection::getSectionId, s -> s));

        // Atualizar ordem
        int ordem = 0;
        for (String sectionId : request.getSectionIds()) {
            LandingPageSection section = sectionMap.get(sectionId);
            if (section != null) {
                section.setOrdem(ordem++);
                sectionRepository.save(section);
            }
        }

        return sectionRepository.findByLandingPageIdAndAtivoTrueOrderByOrdemAsc(landingPageId)
                .stream()
                .map(this::convertSectionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Duplica uma seção.
     */
    public LandingPageSectionDTO duplicateSection(Long landingPageId, String sectionId) {
        Long orgId = TenantContext.getOrganizacaoId();

        landingPageRepository.findById(landingPageId)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        LandingPageSection original = sectionRepository.findBySectionIdAndLandingPageId(sectionId, landingPageId)
                .orElseThrow(() -> new RuntimeException("Seção não encontrada"));

        int novaOrdem = original.getOrdem() + 1;

        // Ajustar ordem das seções abaixo
        List<LandingPageSection> sections = sectionRepository
                .findByLandingPageIdAndAtivoTrueOrderByOrdemAsc(landingPageId);
        for (LandingPageSection s : sections) {
            if (s.getOrdem() >= novaOrdem) {
                s.setOrdem(s.getOrdem() + 1);
                sectionRepository.save(s);
            }
        }

        LandingPageSection copy = LandingPageSection.builder()
                .landingPage(original.getLandingPage())
                .sectionId(UUID.randomUUID().toString())
                .tipo(original.getTipo())
                .nome(original.getNome() + " (Cópia)")
                .ordem(novaOrdem)
                .visivel(original.getVisivel())
                .template(original.getTemplate())
                .content(original.getContent())
                .styles(original.getStyles())
                .settings(original.getSettings())
                .animations(original.getAnimations())
                .visibilityRules(original.getVisibilityRules())
                .dataSource(original.getDataSource())
                .locked(false)
                .ativo(true)
                .build();

        copy = sectionRepository.save(copy);
        return convertSectionToDTO(copy);
    }

    /**
     * Deleta uma seção.
     */
    public void deleteSection(Long landingPageId, String sectionId) {
        Long orgId = TenantContext.getOrganizacaoId();

        landingPageRepository.findById(landingPageId)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        LandingPageSection section = sectionRepository.findBySectionIdAndLandingPageId(sectionId, landingPageId)
                .orElseThrow(() -> new RuntimeException("Seção não encontrada"));

        if (Boolean.TRUE.equals(section.getLocked())) {
            throw new RuntimeException("Seção está bloqueada para exclusão");
        }

        section.setAtivo(false);
        sectionRepository.save(section);
    }

    // ==================== VERSION MANAGEMENT ====================

    /**
     * Lista versões de uma landing page.
     */
    @Transactional(readOnly = true)
    public List<LandingPageVersionDTO> listVersions(Long landingPageId) {
        Long orgId = TenantContext.getOrganizacaoId();

        landingPageRepository.findById(landingPageId)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        return versionRepository.findByLandingPageIdOrderByVersaoDesc(landingPageId)
                .stream()
                .map(this::convertVersionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Restaura uma versão anterior.
     */
    public LandingPageDTO restoreVersion(Long landingPageId, Integer versao) {
        Long orgId = TenantContext.getOrganizacaoId();

        LandingPage landingPage = landingPageRepository.findByIdWithSections(landingPageId)
                .filter(lp -> lp.getOrganizacao().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Landing page não encontrada"));

        LandingPageVersion version = versionRepository.findByLandingPageIdAndVersao(landingPageId, versao)
                .orElseThrow(() -> new RuntimeException("Versão não encontrada"));

        // Deserializar snapshot e restaurar
        try {
            LandingPageDTO snapshot = objectMapper.readValue(version.getSnapshot(), LandingPageDTO.class);

            // Restaurar configurações
            landingPage.setGlobalSettings(serializeJson(snapshot.getGlobalSettings()));
            landingPage.setSeoSettings(serializeJson(snapshot.getSeoSettings()));
            landingPage.setCustomCss(snapshot.getCustomCss());
            landingPage.setCustomJs(snapshot.getCustomJs());

            // Restaurar seções
            if (snapshot.getSections() != null) {
                updateSections(landingPage, snapshot.getSections());
            }

            landingPage.setVersao(landingPage.getVersao() + 1);
            landingPage = landingPageRepository.save(landingPage);

            // Criar versão de restauração
            createVersion(landingPage, "Restaurado da versão " + versao, "MANUAL");

            return convertToDTOWithSections(landingPage);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao restaurar versão", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private void createVersion(LandingPage landingPage, String descricao, String tipo) {
        try {
            LandingPageDTO snapshot = convertToDTOWithSections(landingPage);
            String snapshotJson = objectMapper.writeValueAsString(snapshot);

            int novaVersao = versionRepository.findMaxVersao(landingPage.getId()) + 1;

            LandingPageVersion version = LandingPageVersion.builder()
                    .landingPage(landingPage)
                    .versao(novaVersao)
                    .snapshot(snapshotJson)
                    .descricao(descricao)
                    .tipo(tipo)
                    .criadoPor(TenantContext.getUserId())
                    .criadoPorNome(TenantContext.getUsername())
                    .build();

            versionRepository.save(version);

        } catch (JsonProcessingException e) {
            // Log error but don't fail the operation
        }
    }

    private void updateSections(LandingPage landingPage, List<LandingPageSectionDTO> sectionsDTO) {
        // Soft delete seções existentes
        sectionRepository.softDeleteAllByLandingPageId(landingPage.getId());

        // Criar novas seções
        int ordem = 0;
        for (LandingPageSectionDTO dto : sectionsDTO) {
            LandingPageSection section = LandingPageSection.builder()
                    .landingPage(landingPage)
                    .sectionId(dto.getSectionId() != null ? dto.getSectionId() : UUID.randomUUID().toString())
                    .tipo(dto.getTipo())
                    .nome(dto.getNome())
                    .ordem(ordem++)
                    .visivel(dto.getVisivel() != null ? dto.getVisivel() : true)
                    .template(dto.getTemplate())
                    .content(serializeJson(dto.getContent()))
                    .styles(serializeJson(dto.getStyles()))
                    .settings(serializeJson(dto.getSettings()))
                    .animations(serializeJson(dto.getAnimations()))
                    .visibilityRules(serializeJson(dto.getVisibilityRules()))
                    .dataSource(serializeJson(dto.getDataSource()))
                    .locked(dto.getLocked() != null ? dto.getLocked() : false)
                    .ativo(true)
                    .build();
            sectionRepository.save(section);
        }
    }

    private void copyTemplateToPage(Long templateId, LandingPage landingPage) {
        landingPageRepository.findByIdWithSections(templateId)
                .ifPresent(template -> {
                    landingPage.setGlobalSettings(template.getGlobalSettings());
                    for (LandingPageSection section : template.getSections()) {
                        LandingPageSection copy = LandingPageSection.builder()
                                .sectionId(UUID.randomUUID().toString())
                                .tipo(section.getTipo())
                                .nome(section.getNome())
                                .ordem(section.getOrdem())
                                .visivel(section.getVisivel())
                                .template(section.getTemplate())
                                .content(section.getContent())
                                .styles(section.getStyles())
                                .settings(section.getSettings())
                                .animations(section.getAnimations())
                                .visibilityRules(section.getVisibilityRules())
                                .dataSource(section.getDataSource())
                                .locked(false)
                                .ativo(true)
                                .build();
                        landingPage.addSection(copy);
                    }
                });
    }

    private void createDefaultSections(LandingPage landingPage) {
        String[] defaultTypes = {"HEADER", "HERO", "ABOUT", "SERVICES", "TEAM", "BOOKING", "FOOTER"};
        int ordem = 0;

        for (String tipo : defaultTypes) {
            LandingPageSection section = LandingPageSection.builder()
                    .sectionId(UUID.randomUUID().toString())
                    .tipo(tipo)
                    .nome(getDefaultSectionName(tipo))
                    .ordem(ordem++)
                    .visivel(true)
                    .content(getDefaultSectionContent(tipo))
                    .locked(false)
                    .ativo(true)
                    .build();
            landingPage.addSection(section);
        }
    }

    private String getDefaultSectionName(String tipo) {
        return switch (tipo.toUpperCase()) {
            case "HEADER" -> "Menu";
            case "HERO" -> "Apresentação";
            case "ABOUT" -> "Sobre";
            case "SERVICES" -> "Serviços";
            case "PRODUCTS" -> "Produtos";
            case "TEAM" -> "Equipe";
            case "TESTIMONIALS" -> "Depoimentos";
            case "PRICING" -> "Planos";
            case "GALLERY" -> "Galeria";
            case "BOOKING" -> "Agendamento";
            case "CONTACT" -> "Contato";
            case "FAQ" -> "FAQ";
            case "FOOTER" -> "Rodapé";
            default -> tipo;
        };
    }

    private String getDefaultSectionContent(String tipo) {
        // Retorna estrutura JSON padrão para cada tipo de seção
        // Isso pode ser expandido com templates mais elaborados
        return switch (tipo.toUpperCase()) {
            case "HERO" -> """
                {
                    "layout": "centered",
                    "background": {"type": "color", "color": "#1a1a1a"},
                    "elements": [
                        {"id": "hero-title", "type": "heading", "tag": "h1", "content": "Bem-vindo à {empresa}", "styles": {"desktop": {"fontSize": "48px", "fontWeight": "bold", "color": "#ffffff"}}},
                        {"id": "hero-subtitle", "type": "paragraph", "content": "Qualidade e excelência em serviços", "styles": {"desktop": {"fontSize": "20px", "color": "#cccccc"}}},
                        {"id": "hero-cta", "type": "button", "content": "Agendar Agora", "variant": "primary", "action": {"type": "scroll", "href": "#booking"}}
                    ]
                }
                """;
            case "HEADER" -> """
                {
                    "layout": "space-between",
                    "elements": [
                        {"id": "logo", "type": "logo", "props": {"showText": true}},
                        {"id": "nav", "type": "menu", "props": {"items": [{"label": "Início", "href": "#hero"}, {"label": "Serviços", "href": "#services"}, {"label": "Sobre", "href": "#about"}, {"label": "Contato", "href": "#contact"}]}},
                        {"id": "cta", "type": "button", "content": "Agendar", "variant": "primary", "action": {"type": "scroll", "href": "#booking"}}
                    ]
                }
                """;
            case "FOOTER" -> """
                {
                    "layout": "grid",
                    "elements": [
                        {"id": "footer-about", "type": "container", "children": [
                            {"id": "footer-logo", "type": "logo"},
                            {"id": "footer-desc", "type": "paragraph", "content": "{descricao_empresa}"}
                        ]},
                        {"id": "footer-contact", "type": "container", "children": [
                            {"id": "footer-contact-title", "type": "heading", "tag": "h4", "content": "Contato"},
                            {"id": "footer-phone", "type": "text", "content": "{telefone}"},
                            {"id": "footer-email", "type": "text", "content": "{email}"}
                        ]},
                        {"id": "footer-social", "type": "social-links"}
                    ]
                }
                """;
            default -> """
                {"layout": "default", "elements": []}
                """;
        };
    }

    private String generateSlug(String nome) {
        return nome.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private <T> T deserializeJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private <T> T deserializeJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // ==================== CONVERSION METHODS ====================

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

        dto.setGlobalSettings(deserializeJson(entity.getGlobalSettings(),
                LandingPageDTO.GlobalSettingsDTO.class));
        dto.setSeoSettings(deserializeJson(entity.getSeoSettings(),
                LandingPageDTO.SeoSettingsDTO.class));
        dto.setCustomCss(entity.getCustomCss());
        dto.setCustomJs(entity.getCustomJs());
        dto.setFaviconUrl(entity.getFaviconUrl());

        if (entity.getSections() != null) {
            dto.setSections(entity.getSections().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getAtivo()))
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
                .content(deserializeJson(entity.getContent(),
                        LandingPageSectionDTO.SectionContentDTO.class))
                .styles(deserializeJson(entity.getStyles(),
                        LandingPageSectionDTO.SectionStylesDTO.class))
                .settings(deserializeJson(entity.getSettings(),
                        new TypeReference<Map<String, Object>>() {}))
                .animations(deserializeJson(entity.getAnimations(),
                        LandingPageSectionDTO.AnimationDTO.class))
                .visibilityRules(deserializeJson(entity.getVisibilityRules(),
                        LandingPageSectionDTO.VisibilityRulesDTO.class))
                .dataSource(deserializeJson(entity.getDataSource(),
                        LandingPageSectionDTO.DataSourceDTO.class))
                .locked(entity.getLocked())
                .build();
    }

    private LandingPageVersionDTO convertVersionToDTO(LandingPageVersion entity) {
        return LandingPageVersionDTO.builder()
                .id(entity.getId())
                .versao(entity.getVersao())
                .descricao(entity.getDescricao())
                .tipo(entity.getTipo())
                .criadoPor(entity.getCriadoPor())
                .criadoPorNome(entity.getCriadoPorNome())
                .dtCriacao(entity.getDtCriacao())
                .build();
    }
}
