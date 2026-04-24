package org.exemplo.bellory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.site.AboutSectionDTO;
import org.exemplo.bellory.model.dto.site.FooterConfigDTO;
import org.exemplo.bellory.model.dto.site.HeaderConfigDTO;
import org.exemplo.bellory.model.dto.site.HeroSectionDTO;
import org.exemplo.bellory.model.dto.site.HomePageDTO;
import org.exemplo.bellory.model.dto.site.SitePublicoConfigDTO;
import org.exemplo.bellory.model.dto.site.request.*;
import org.exemplo.bellory.model.dto.site.request.TransitionConfigRequest;
import org.exemplo.bellory.model.entity.landingpage.LandingPage;
import org.exemplo.bellory.model.entity.landingpage.LandingPageSection;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.site.SitePublicoConfig;
import org.exemplo.bellory.model.repository.landingpage.LandingPageRepository;
import org.exemplo.bellory.model.repository.landingpage.LandingPageSectionRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.site.SitePublicoConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteConfigService {

    private final SitePublicoConfigRepository siteConfigRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final LandingPageRepository landingPageRepository;
    private final LandingPageSectionRepository sectionRepository;
    private final ObjectMapper objectMapper;

    // ==================== GET ====================

    @Transactional(readOnly = true)
    public SitePublicoConfigDTO buscarConfig() {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = siteConfigRepository.findByOrganizacaoId(orgId)
                .orElse(null);

        if (config == null) {
            return SitePublicoConfigDTO.builder()
                    .organizacaoId(orgId)
                    .active(false)
                    .build();
        }

        return convertToDTO(config);
    }

    // ==================== SAVE FULL ====================

    @Transactional
    public SitePublicoConfigDTO salvarConfigCompleta(SitePublicoConfigRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);

        if (dto.getHero() != null) {
            aplicarHero(config, dto.getHero());
            syncSectionToLandingPage(orgId, "HERO", dto.getHero());
        }
        if (dto.getHeader() != null) {
            aplicarHeader(config, dto.getHeader());
            syncSectionToLandingPage(orgId, "HEADER", dto.getHeader());
        }
        if (dto.getAbout() != null) {
            aplicarAbout(config, dto.getAbout());
            syncSectionToLandingPage(orgId, "ABOUT", dto.getAbout());
        }
        if (dto.getFooter() != null) {
            aplicarFooter(config, dto.getFooter());
            syncSectionToLandingPage(orgId, "FOOTER", dto.getFooter());
        }
        if (dto.getServices() != null) {
            aplicarServices(config, dto.getServices());
            syncSectionToLandingPage(orgId, "SERVICES", dto.getServices());
        }
        if (dto.getProducts() != null) {
            aplicarProducts(config, dto.getProducts());
            syncSectionToLandingPage(orgId, "PRODUCTS", dto.getProducts());
        }
        if (dto.getTeam() != null) {
            aplicarTeam(config, dto.getTeam());
            syncSectionToLandingPage(orgId, "TEAM", dto.getTeam());
        }
        if (dto.getBooking() != null) {
            aplicarBooking(config, dto.getBooking());
            syncSectionToLandingPage(orgId, "BOOKING", dto.getBooking());
        }
        if (dto.getGeneral() != null) {
            aplicarGeneral(config, dto.getGeneral());
        }

        SitePublicoConfig saved = siteConfigRepository.save(config);
        return convertToDTO(saved);
    }

    // ==================== PATCH PER SECTION ====================

    @Transactional
    public HeroSectionRequest atualizarHero(HeroSectionRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarHero(config, dto);
        siteConfigRepository.save(config);
        syncSectionToLandingPage(orgId, "HERO", dto);
        return extrairHero(config);
    }

    @Transactional
    public HeaderConfigRequest atualizarHeader(HeaderConfigRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarHeader(config, dto);
        siteConfigRepository.save(config);
        syncSectionToLandingPage(orgId, "HEADER", dto);
        return extrairHeader(config);
    }

    @Transactional
    public AboutSectionRequest atualizarAbout(AboutSectionRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarAbout(config, dto);
        siteConfigRepository.save(config);
        syncSectionToLandingPage(orgId, "ABOUT", dto);
        return extrairAbout(config);
    }

    @Transactional
    public FooterConfigRequest atualizarFooter(FooterConfigRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarFooter(config, dto);
        siteConfigRepository.save(config);
        syncSectionToLandingPage(orgId, "FOOTER", dto);
        return extrairFooter(config);
    }

    @Transactional
    public ServicesSectionRequest atualizarServices(ServicesSectionRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarServices(config, dto);
        siteConfigRepository.save(config);
        syncSectionToLandingPage(orgId, "SERVICES", dto);
        return extrairServices(config);
    }

    @Transactional
    public ProductsSectionRequest atualizarProducts(ProductsSectionRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarProducts(config, dto);
        siteConfigRepository.save(config);
        syncSectionToLandingPage(orgId, "PRODUCTS", dto);
        return extrairProducts(config);
    }

    @Transactional
    public TeamSectionRequest atualizarTeam(TeamSectionRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarTeam(config, dto);
        siteConfigRepository.save(config);
        syncSectionToLandingPage(orgId, "TEAM", dto);
        return extrairTeam(config);
    }

    @Transactional
    public BookingSectionRequest atualizarBooking(BookingSectionRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarBooking(config, dto);
        siteConfigRepository.save(config);
        syncSectionToLandingPage(orgId, "BOOKING", dto);
        return extrairBooking(config);
    }

    @Transactional
    public GeneralSettingsRequest atualizarGeneral(GeneralSettingsRequest dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        aplicarGeneral(config, dto);
        siteConfigRepository.save(config);
        // General settings sync: update sectionsOrder on the LandingPage level
        syncGeneralToLandingPage(orgId, dto);
        return extrairGeneral(config);
    }

    @Transactional
    public SitePublicoConfigDTO alterarStatus(Boolean active) {
        if (active == null) {
            throw new IllegalArgumentException("O campo 'active' é obrigatório.");
        }
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);
        config.setActive(active);
        SitePublicoConfig saved = siteConfigRepository.save(config);
        return convertToDTO(saved);
    }

    @Transactional
    public Map<String, TransitionConfigRequest> atualizarTransitions(Map<String, TransitionConfigRequest> dto) {
        Long orgId = getOrganizacaoId();
        SitePublicoConfig config = findOrCreate(orgId);

        // Merge: carrega o mapa existente e sobrescreve apenas as chaves enviadas
        Map<String, TransitionConfigRequest> existing = extrairTransitions(config);
        if (existing == null) {
            existing = new java.util.HashMap<>();
        }
        existing.putAll(dto);

        config.setTransitions(toJson(existing));
        siteConfigRepository.save(config);
        return existing;
    }

    // ==================== APLICAR (DTO -> Entity) ====================

    /**
     * Para campos String nullable no PATCH parcial:
     * - null  → não enviou, não altera
     * - ""    → quer limpar, seta null no banco
     * - "abc" → valor normal
     */
    private String blankToNull(String value) {
        return (value != null && value.isBlank()) ? null : value;
    }

    private void aplicarHero(SitePublicoConfig config, HeroSectionRequest dto) {
        if (dto.getType() != null) config.setHeroType(blankToNull(dto.getType()));
        if (dto.getTitle() != null) config.setHeroTitle(blankToNull(dto.getTitle()));
        if (dto.getSubtitle() != null) config.setHeroSubtitle(blankToNull(dto.getSubtitle()));
        if (dto.getBackgroundUrl() != null) config.setHeroBackgroundUrl(blankToNull(dto.getBackgroundUrl()));
        if (dto.getBackgroundOverlay() != null) config.setHeroBackgroundOverlay(dto.getBackgroundOverlay());
        if (dto.getCustomHtml() != null) config.setHeroCustomHtml(blankToNull(dto.getCustomHtml()));
        if (dto.getButtons() != null) config.setHeroButtons(toJson(dto.getButtons()));
        if (dto.getShowBookingForm() != null) config.setHeroShowBookingForm(dto.getShowBookingForm());
        if (dto.getContentLayout() != null) config.setHeroContentLayout(blankToNull(dto.getContentLayout()));
        if (dto.getTitleSize() != null) config.setHeroTitleSize(blankToNull(dto.getTitleSize()));
        if (dto.getHeroHeight() != null) config.setHeroHeight(blankToNull(dto.getHeroHeight()));
        if (dto.getOverlayStyle() != null) config.setHeroOverlayStyle(blankToNull(dto.getOverlayStyle()));
        if (dto.getBadgeText() != null) config.setHeroBadgeText(blankToNull(dto.getBadgeText()));
        if (dto.getTitleHighlight() != null) config.setHeroTitleHighlight(blankToNull(dto.getTitleHighlight()));
        if (dto.getShowParticles() != null) config.setHeroShowParticles(dto.getShowParticles());
        if (dto.getVideoUrl() != null) config.setHeroVideoUrl(blankToNull(dto.getVideoUrl()));
        if (dto.getSideImageUrl() != null) config.setHeroSideImageUrl(blankToNull(dto.getSideImageUrl()));
        if (dto.getStatsConfig() != null) config.setHeroStatsConfig(toJson(dto.getStatsConfig()));
        if (dto.getBackgroundColor() != null) config.setHeroBackgroundColor(blankToNull(dto.getBackgroundColor()));
        if (dto.getBackgroundPattern() != null) config.setHeroBackgroundPattern(blankToNull(dto.getBackgroundPattern()));
        if (dto.getPatternOpacity() != null) config.setHeroPatternOpacity(dto.getPatternOpacity());
    }

    private void aplicarHeader(SitePublicoConfig config, HeaderConfigRequest dto) {
        if (dto.getLogoUrl() != null) config.setHeaderLogoUrl(blankToNull(dto.getLogoUrl()));
        if (dto.getLogoAlt() != null) config.setHeaderLogoAlt(blankToNull(dto.getLogoAlt()));
        if (dto.getMenuItems() != null) config.setHeaderMenuItems(toJson(dto.getMenuItems()));
        if (dto.getActionButtons() != null) config.setHeaderActionButtons(toJson(dto.getActionButtons()));
        if (dto.getShowPhone() != null) config.setHeaderShowPhone(dto.getShowPhone());
        if (dto.getShowSocial() != null) config.setHeaderShowSocial(dto.getShowSocial());
        if (dto.getSticky() != null) config.setHeaderSticky(dto.getSticky());
        if (dto.getHeaderLayout() != null) config.setHeaderLayout(blankToNull(dto.getHeaderLayout()));
        if (dto.getMenuStyle() != null) config.setHeaderMenuStyle(blankToNull(dto.getMenuStyle()));
        if (dto.getTransparentOnHero() != null) config.setHeaderTransparentOnHero(dto.getTransparentOnHero());
        if (dto.getShowCart() != null) config.setHeaderShowCart(dto.getShowCart());
        if (dto.getLogoHeight() != null) config.setHeaderLogoHeight(dto.getLogoHeight());
        if (dto.getBackgroundColor() != null) config.setHeaderBackgroundColor(blankToNull(dto.getBackgroundColor()));
        if (dto.getBackgroundPattern() != null) config.setHeaderBackgroundPattern(blankToNull(dto.getBackgroundPattern()));
        if (dto.getPatternOpacity() != null) config.setHeaderPatternOpacity(dto.getPatternOpacity());
    }

    private void aplicarAbout(SitePublicoConfig config, AboutSectionRequest dto) {
        if (dto.getTitle() != null) config.setAboutTitle(blankToNull(dto.getTitle()));
        if (dto.getSubtitle() != null) config.setAboutSubtitle(blankToNull(dto.getSubtitle()));
        if (dto.getDescription() != null) config.setAboutDescription(blankToNull(dto.getDescription()));
        if (dto.getFullDescription() != null) config.setAboutFullDescription(blankToNull(dto.getFullDescription()));
        if (dto.getImageUrl() != null) config.setAboutImageUrl(blankToNull(dto.getImageUrl()));
        if (dto.getGalleryImages() != null) config.setAboutGalleryImages(toJson(dto.getGalleryImages()));
        if (dto.getVideoUrl() != null) config.setAboutVideoUrl(blankToNull(dto.getVideoUrl()));
        if (dto.getHighlights() != null) config.setAboutHighlights(toJson(dto.getHighlights()));
        if (dto.getMission() != null) config.setAboutMission(blankToNull(dto.getMission()));
        if (dto.getVision() != null) config.setAboutVision(blankToNull(dto.getVision()));
        if (dto.getValues() != null) config.setAboutValues(blankToNull(dto.getValues()));
        if (dto.getShowGallery() != null) config.setAboutShowGallery(dto.getShowGallery());
        if (dto.getShowHighlights() != null) config.setAboutShowHighlights(dto.getShowHighlights());
        if (dto.getShowMVV() != null) config.setAboutShowMVV(dto.getShowMVV());
        if (dto.getLayoutStyle() != null) config.setAboutLayoutStyle(blankToNull(dto.getLayoutStyle()));
        if (dto.getYearFounded() != null) config.setAboutYearFounded(dto.getYearFounded());
        if (dto.getTeamPhotoUrl() != null) config.setAboutTeamPhotoUrl(blankToNull(dto.getTeamPhotoUrl()));
        if (dto.getBackgroundColor() != null) config.setAboutBackgroundColor(blankToNull(dto.getBackgroundColor()));
        if (dto.getBackgroundPattern() != null) config.setAboutBackgroundPattern(blankToNull(dto.getBackgroundPattern()));
        if (dto.getPatternOpacity() != null) config.setAboutPatternOpacity(dto.getPatternOpacity());
    }

    private void aplicarFooter(SitePublicoConfig config, FooterConfigRequest dto) {
        if (dto.getDescription() != null) config.setFooterDescription(blankToNull(dto.getDescription()));
        if (dto.getLogoUrl() != null) config.setFooterLogoUrl(blankToNull(dto.getLogoUrl()));
        if (dto.getLinkSections() != null) config.setFooterLinkSections(toJson(dto.getLinkSections()));
        if (dto.getCopyrightText() != null) config.setFooterCopyrightText(blankToNull(dto.getCopyrightText()));
        if (dto.getShowMap() != null) config.setFooterShowMap(dto.getShowMap());
        if (dto.getShowHours() != null) config.setFooterShowHours(dto.getShowHours());
        if (dto.getShowSocial() != null) config.setFooterShowSocial(dto.getShowSocial());
        if (dto.getShowNewsletter() != null) config.setFooterShowNewsletter(dto.getShowNewsletter());
        if (dto.getLayout() != null) config.setFooterLayout(blankToNull(dto.getLayout()));
        if (dto.getLogoHeight() != null) config.setFooterLogoHeight(dto.getLogoHeight());
        if (dto.getShowLogo() != null) config.setFooterShowLogo(dto.getShowLogo());
        if (dto.getSocialStyle() != null) config.setFooterSocialStyle(blankToNull(dto.getSocialStyle()));
        if (dto.getDividerStyle() != null) config.setFooterDividerStyle(blankToNull(dto.getDividerStyle()));
        if (dto.getShowContact() != null) config.setFooterShowContact(dto.getShowContact());
        if (dto.getShowBackToTop() != null) config.setFooterShowBackToTop(dto.getShowBackToTop());
        if (dto.getNewsletterTitle() != null) config.setFooterNewsletterTitle(blankToNull(dto.getNewsletterTitle()));
        if (dto.getNewsletterPlaceholder() != null) config.setFooterNewsletterPlaceholder(blankToNull(dto.getNewsletterPlaceholder()));
        if (dto.getColumns() != null) config.setFooterColumns(dto.getColumns());
        if (dto.getCompactHours() != null) config.setFooterCompactHours(dto.getCompactHours());
        if (dto.getBackgroundColor() != null) config.setFooterBackgroundColor(blankToNull(dto.getBackgroundColor()));
        if (dto.getBackgroundPattern() != null) config.setFooterBackgroundPattern(blankToNull(dto.getBackgroundPattern()));
        if (dto.getPatternOpacity() != null) config.setFooterPatternOpacity(dto.getPatternOpacity());
    }

    private void aplicarServices(SitePublicoConfig config, ServicesSectionRequest dto) {
        if (dto.getSectionTitle() != null) config.setServicesSectionTitle(blankToNull(dto.getSectionTitle()));
        if (dto.getSectionSubtitle() != null) config.setServicesSectionSubtitle(blankToNull(dto.getSectionSubtitle()));
        if (dto.getShowPrices() != null) config.setServicesShowPrices(dto.getShowPrices());
        if (dto.getShowDuration() != null) config.setServicesShowDuration(dto.getShowDuration());
        if (dto.getFeaturedLimit() != null) config.setServicesFeaturedLimit(dto.getFeaturedLimit());
        if (dto.getCardStyle() != null) config.setServicesCardStyle(blankToNull(dto.getCardStyle()));
        if (dto.getShowCategory() != null) config.setServicesShowCategory(dto.getShowCategory());
        if (dto.getShowDescription() != null) config.setServicesShowDescription(dto.getShowDescription());
        if (dto.getShowImage() != null) config.setServicesShowImage(dto.getShowImage());
        if (dto.getShowDiscount() != null) config.setServicesShowDiscount(dto.getShowDiscount());
        if (dto.getCardImageHeight() != null) config.setServicesCardImageHeight(dto.getCardImageHeight());
        if (dto.getShowCategoryFilter() != null) config.setServicesShowCategoryFilter(dto.getShowCategoryFilter());
        if (dto.getColumns() != null) config.setServicesColumns(dto.getColumns());
        if (dto.getBackgroundColor() != null) config.setServicesBackgroundColor(blankToNull(dto.getBackgroundColor()));
        if (dto.getBackgroundPattern() != null) config.setServicesBackgroundPattern(blankToNull(dto.getBackgroundPattern()));
        if (dto.getPatternOpacity() != null) config.setServicesPatternOpacity(dto.getPatternOpacity());
    }

    private void aplicarProducts(SitePublicoConfig config, ProductsSectionRequest dto) {
        if (dto.getSectionTitle() != null) config.setProductsSectionTitle(blankToNull(dto.getSectionTitle()));
        if (dto.getSectionSubtitle() != null) config.setProductsSectionSubtitle(blankToNull(dto.getSectionSubtitle()));
        if (dto.getShowPrices() != null) config.setProductsShowPrices(dto.getShowPrices());
        if (dto.getFeaturedLimit() != null) config.setProductsFeaturedLimit(dto.getFeaturedLimit());
        if (dto.getLayout() != null) config.setProductsLayout(blankToNull(dto.getLayout()));
        if (dto.getCardStyle() != null) config.setProductsCardStyle(blankToNull(dto.getCardStyle()));
        if (dto.getColumns() != null) config.setProductsColumns(dto.getColumns());
        if (dto.getCardImageHeight() != null) config.setProductsCardImageHeight(dto.getCardImageHeight());
        if (dto.getShowRating() != null) config.setProductsShowRating(dto.getShowRating());
        if (dto.getShowCategory() != null) config.setProductsShowCategory(dto.getShowCategory());
        if (dto.getShowDescription() != null) config.setProductsShowDescription(dto.getShowDescription());
        if (dto.getShowDiscount() != null) config.setProductsShowDiscount(dto.getShowDiscount());
        if (dto.getShowStock() != null) config.setProductsShowStock(dto.getShowStock());
        if (dto.getShowAddToCart() != null) config.setProductsShowAddToCart(dto.getShowAddToCart());
        if (dto.getHoverEffect() != null) config.setProductsHoverEffect(blankToNull(dto.getHoverEffect()));
        if (dto.getBadgeStyle() != null) config.setProductsBadgeStyle(blankToNull(dto.getBadgeStyle()));
        if (dto.getAutoPlay() != null) config.setProductsAutoPlay(dto.getAutoPlay());
        if (dto.getAutoPlaySpeed() != null) config.setProductsAutoPlaySpeed(dto.getAutoPlaySpeed());
        if (dto.getBackgroundColor() != null) config.setProductsBackgroundColor(blankToNull(dto.getBackgroundColor()));
        if (dto.getBackgroundPattern() != null) config.setProductsBackgroundPattern(blankToNull(dto.getBackgroundPattern()));
        if (dto.getPatternOpacity() != null) config.setProductsPatternOpacity(dto.getPatternOpacity());
    }

    private void aplicarTeam(SitePublicoConfig config, TeamSectionRequest dto) {
        if (dto.getSectionTitle() != null) config.setTeamSectionTitle(blankToNull(dto.getSectionTitle()));
        if (dto.getSectionSubtitle() != null) config.setTeamSectionSubtitle(blankToNull(dto.getSectionSubtitle()));
        if (dto.getShowSection() != null) config.setTeamShowSection(dto.getShowSection());
        if (dto.getLayout() != null) config.setTeamLayout(blankToNull(dto.getLayout()));
        if (dto.getCardStyle() != null) config.setTeamCardStyle(blankToNull(dto.getCardStyle()));
        if (dto.getPhotoShape() != null) config.setTeamPhotoShape(blankToNull(dto.getPhotoShape()));
        if (dto.getPhotoHeight() != null) config.setTeamPhotoHeight(dto.getPhotoHeight());
        if (dto.getShowBio() != null) config.setTeamShowBio(dto.getShowBio());
        if (dto.getShowServices() != null) config.setTeamShowServices(dto.getShowServices());
        if (dto.getShowSchedule() != null) config.setTeamShowSchedule(dto.getShowSchedule());
        if (dto.getCarouselAutoPlay() != null) config.setTeamCarouselAutoPlay(dto.getCarouselAutoPlay());
        if (dto.getCarouselSpeed() != null) config.setTeamCarouselSpeed(dto.getCarouselSpeed());
        if (dto.getBackgroundColor() != null) config.setTeamBackgroundColor(blankToNull(dto.getBackgroundColor()));
        if (dto.getBackgroundPattern() != null) config.setTeamBackgroundPattern(blankToNull(dto.getBackgroundPattern()));
        if (dto.getPatternOpacity() != null) config.setTeamPatternOpacity(dto.getPatternOpacity());
    }

    private void aplicarBooking(SitePublicoConfig config, BookingSectionRequest dto) {
        if (dto.getSectionTitle() != null) config.setBookingSectionTitle(blankToNull(dto.getSectionTitle()));
        if (dto.getSectionSubtitle() != null) config.setBookingSectionSubtitle(blankToNull(dto.getSectionSubtitle()));
        if (dto.getEnabled() != null) config.setBookingEnabled(dto.getEnabled());
        if (dto.getBackgroundColor() != null) config.setBookingBackgroundColor(blankToNull(dto.getBackgroundColor()));
        if (dto.getBackgroundPattern() != null) config.setBookingBackgroundPattern(blankToNull(dto.getBackgroundPattern()));
        if (dto.getPatternOpacity() != null) config.setBookingPatternOpacity(dto.getPatternOpacity());
    }

    private void aplicarGeneral(SitePublicoConfig config, GeneralSettingsRequest dto) {
        if (dto.getHomeSectionsOrder() != null) config.setHomeSectionsOrder(toJson(dto.getHomeSectionsOrder()));
        if (dto.getCustomCss() != null) config.setCustomCss(blankToNull(dto.getCustomCss()));
        if (dto.getCustomJs() != null) config.setCustomJs(blankToNull(dto.getCustomJs()));
        if (dto.getExternalScripts() != null) config.setExternalScripts(toJson(dto.getExternalScripts()));
        if (dto.getActive() != null) config.setActive(dto.getActive());
    }

    // ==================== EXTRAIR (Entity -> Section DTO) ====================

    private HeroSectionRequest extrairHero(SitePublicoConfig config) {
        return HeroSectionRequest.builder()
                .type(config.getHeroType())
                .title(config.getHeroTitle())
                .subtitle(config.getHeroSubtitle())
                .backgroundUrl(config.getHeroBackgroundUrl())
                .backgroundOverlay(config.getHeroBackgroundOverlay())
                .customHtml(config.getHeroCustomHtml())
                .buttons(fromJson(config.getHeroButtons(), new TypeReference<>() {}))
                .showBookingForm(config.getHeroShowBookingForm())
                .contentLayout(config.getHeroContentLayout())
                .titleSize(config.getHeroTitleSize())
                .heroHeight(config.getHeroHeight())
                .overlayStyle(config.getHeroOverlayStyle())
                .badgeText(config.getHeroBadgeText())
                .titleHighlight(config.getHeroTitleHighlight())
                .showParticles(config.getHeroShowParticles())
                .videoUrl(config.getHeroVideoUrl())
                .sideImageUrl(config.getHeroSideImageUrl())
                .statsConfig(fromJson(config.getHeroStatsConfig(), new TypeReference<>() {}))
                .backgroundColor(config.getHeroBackgroundColor())
                .backgroundPattern(config.getHeroBackgroundPattern())
                .patternOpacity(config.getHeroPatternOpacity())
                .build();
    }

    private HeaderConfigRequest extrairHeader(SitePublicoConfig config) {
        return HeaderConfigRequest.builder()
                .logoUrl(config.getHeaderLogoUrl())
                .logoAlt(config.getHeaderLogoAlt())
                .menuItems(fromJson(config.getHeaderMenuItems(), new TypeReference<>() {}))
                .actionButtons(fromJson(config.getHeaderActionButtons(), new TypeReference<>() {}))
                .showPhone(config.getHeaderShowPhone())
                .showSocial(config.getHeaderShowSocial())
                .sticky(config.getHeaderSticky())
                .headerLayout(config.getHeaderLayout())
                .menuStyle(config.getHeaderMenuStyle())
                .transparentOnHero(config.getHeaderTransparentOnHero())
                .showCart(config.getHeaderShowCart())
                .logoHeight(config.getHeaderLogoHeight())
                .backgroundColor(config.getHeaderBackgroundColor())
                .backgroundPattern(config.getHeaderBackgroundPattern())
                .patternOpacity(config.getHeaderPatternOpacity())
                .build();
    }

    private AboutSectionRequest extrairAbout(SitePublicoConfig config) {
        return AboutSectionRequest.builder()
                .title(config.getAboutTitle())
                .subtitle(config.getAboutSubtitle())
                .description(config.getAboutDescription())
                .fullDescription(config.getAboutFullDescription())
                .imageUrl(config.getAboutImageUrl())
                .galleryImages(fromJson(config.getAboutGalleryImages(), new TypeReference<>() {}))
                .videoUrl(config.getAboutVideoUrl())
                .highlights(fromJson(config.getAboutHighlights(), new TypeReference<>() {}))
                .mission(config.getAboutMission())
                .vision(config.getAboutVision())
                .values(config.getAboutValues())
                .showGallery(config.getAboutShowGallery())
                .showHighlights(config.getAboutShowHighlights())
                .showMVV(config.getAboutShowMVV())
                .layoutStyle(config.getAboutLayoutStyle())
                .yearFounded(config.getAboutYearFounded())
                .teamPhotoUrl(config.getAboutTeamPhotoUrl())
                .backgroundColor(config.getAboutBackgroundColor())
                .backgroundPattern(config.getAboutBackgroundPattern())
                .patternOpacity(config.getAboutPatternOpacity())
                .build();
    }

    private FooterConfigRequest extrairFooter(SitePublicoConfig config) {
        return FooterConfigRequest.builder()
                .description(config.getFooterDescription())
                .logoUrl(config.getFooterLogoUrl())
                .linkSections(fromJson(config.getFooterLinkSections(), new TypeReference<>() {}))
                .copyrightText(config.getFooterCopyrightText())
                .showMap(config.getFooterShowMap())
                .showHours(config.getFooterShowHours())
                .showSocial(config.getFooterShowSocial())
                .showNewsletter(config.getFooterShowNewsletter())
                .layout(config.getFooterLayout())
                .logoHeight(config.getFooterLogoHeight())
                .showLogo(config.getFooterShowLogo())
                .socialStyle(config.getFooterSocialStyle())
                .dividerStyle(config.getFooterDividerStyle())
                .showContact(config.getFooterShowContact())
                .showBackToTop(config.getFooterShowBackToTop())
                .newsletterTitle(config.getFooterNewsletterTitle())
                .newsletterPlaceholder(config.getFooterNewsletterPlaceholder())
                .columns(config.getFooterColumns())
                .compactHours(config.getFooterCompactHours())
                .backgroundColor(config.getFooterBackgroundColor())
                .backgroundPattern(config.getFooterBackgroundPattern())
                .patternOpacity(config.getFooterPatternOpacity())
                .build();
    }

    private ServicesSectionRequest extrairServices(SitePublicoConfig config) {
        return ServicesSectionRequest.builder()
                .sectionTitle(config.getServicesSectionTitle())
                .sectionSubtitle(config.getServicesSectionSubtitle())
                .showPrices(config.getServicesShowPrices())
                .showDuration(config.getServicesShowDuration())
                .featuredLimit(config.getServicesFeaturedLimit())
                .cardStyle(config.getServicesCardStyle())
                .showCategory(config.getServicesShowCategory())
                .showDescription(config.getServicesShowDescription())
                .showImage(config.getServicesShowImage())
                .showDiscount(config.getServicesShowDiscount())
                .cardImageHeight(config.getServicesCardImageHeight())
                .showCategoryFilter(config.getServicesShowCategoryFilter())
                .columns(config.getServicesColumns())
                .backgroundColor(config.getServicesBackgroundColor())
                .backgroundPattern(config.getServicesBackgroundPattern())
                .patternOpacity(config.getServicesPatternOpacity())
                .build();
    }

    private ProductsSectionRequest extrairProducts(SitePublicoConfig config) {
        return ProductsSectionRequest.builder()
                .sectionTitle(config.getProductsSectionTitle())
                .sectionSubtitle(config.getProductsSectionSubtitle())
                .showPrices(config.getProductsShowPrices())
                .featuredLimit(config.getProductsFeaturedLimit())
                .layout(config.getProductsLayout())
                .cardStyle(config.getProductsCardStyle())
                .columns(config.getProductsColumns())
                .cardImageHeight(config.getProductsCardImageHeight())
                .showRating(config.getProductsShowRating())
                .showCategory(config.getProductsShowCategory())
                .showDescription(config.getProductsShowDescription())
                .showDiscount(config.getProductsShowDiscount())
                .showStock(config.getProductsShowStock())
                .showAddToCart(config.getProductsShowAddToCart())
                .hoverEffect(config.getProductsHoverEffect())
                .badgeStyle(config.getProductsBadgeStyle())
                .autoPlay(config.getProductsAutoPlay())
                .autoPlaySpeed(config.getProductsAutoPlaySpeed())
                .backgroundColor(config.getProductsBackgroundColor())
                .backgroundPattern(config.getProductsBackgroundPattern())
                .patternOpacity(config.getProductsPatternOpacity())
                .build();
    }

    private TeamSectionRequest extrairTeam(SitePublicoConfig config) {
        return TeamSectionRequest.builder()
                .sectionTitle(config.getTeamSectionTitle())
                .sectionSubtitle(config.getTeamSectionSubtitle())
                .showSection(config.getTeamShowSection())
                .layout(config.getTeamLayout())
                .cardStyle(config.getTeamCardStyle())
                .photoShape(config.getTeamPhotoShape())
                .photoHeight(config.getTeamPhotoHeight())
                .showBio(config.getTeamShowBio())
                .showServices(config.getTeamShowServices())
                .showSchedule(config.getTeamShowSchedule())
                .carouselAutoPlay(config.getTeamCarouselAutoPlay())
                .carouselSpeed(config.getTeamCarouselSpeed())
                .backgroundColor(config.getTeamBackgroundColor())
                .backgroundPattern(config.getTeamBackgroundPattern())
                .patternOpacity(config.getTeamPatternOpacity())
                .build();
    }

    private BookingSectionRequest extrairBooking(SitePublicoConfig config) {
        return BookingSectionRequest.builder()
                .sectionTitle(config.getBookingSectionTitle())
                .sectionSubtitle(config.getBookingSectionSubtitle())
                .enabled(config.getBookingEnabled())
                .backgroundColor(config.getBookingBackgroundColor())
                .backgroundPattern(config.getBookingBackgroundPattern())
                .patternOpacity(config.getBookingPatternOpacity())
                .build();
    }

    private GeneralSettingsRequest extrairGeneral(SitePublicoConfig config) {
        return GeneralSettingsRequest.builder()
                .homeSectionsOrder(fromJson(config.getHomeSectionsOrder(), new TypeReference<>() {}))
                .customCss(config.getCustomCss())
                .customJs(config.getCustomJs())
                .externalScripts(fromJson(config.getExternalScripts(), new TypeReference<>() {}))
                .active(config.getActive())
                .build();
    }

    public Map<String, TransitionConfigRequest> extrairTransitions(SitePublicoConfig config) {
        return fromJson(config.getTransitions(), new TypeReference<>() {});
    }

    // ==================== CONVERT FULL DTO ====================

    private SitePublicoConfigDTO convertToDTO(SitePublicoConfig config) {
        return SitePublicoConfigDTO.builder()
                .id(config.getId())
                .organizacaoId(config.getOrganizacao().getId())
                .hero(extrairHero(config))
                .header(extrairHeader(config))
                .about(extrairAbout(config))
                .footer(extrairFooter(config))
                .services(extrairServices(config))
                .products(extrairProducts(config))
                .team(extrairTeam(config))
                .booking(extrairBooking(config))
                .general(extrairGeneral(config))
                .transitions(extrairTransitions(config))
                .active(config.getActive())
                .dtCriacao(config.getDtCriacao())
                .dtAtualizacao(config.getDtAtualizacao())
                .build();
    }

    // ==================== HELPERS ====================

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new IllegalArgumentException("Contexto de organização não encontrado");
        }
        return orgId;
    }

    private SitePublicoConfig findOrCreate(Long orgId) {
        return siteConfigRepository.findByOrganizacaoId(orgId)
                .orElseGet(() -> criarNovaConfig(orgId));
    }

    private SitePublicoConfig criarNovaConfig(Long orgId) {
        Organizacao org = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada: " + orgId));

        return SitePublicoConfig.builder()
                .organizacao(org)
                .active(true)
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar JSON: {}", e.getMessage());
            throw new RuntimeException("Erro ao processar dados JSON", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao desserializar JSON: {}", e.getMessage());
            return null;
        }
    }

    // ==================== LANDING PAGE SYNC (Dual-Write) ====================

    private static final Map<String, String> SECTION_DEFAULT_NAMES = Map.of(
            "HEADER", "Menu de Navegação",
            "HERO", "Banner Principal",
            "ABOUT", "Sobre Nós",
            "SERVICES", "Nossos Serviços",
            "PRODUCTS", "Produtos em Destaque",
            "TEAM", "Nossa Equipe",
            "BOOKING", "Agendamento",
            "FOOTER", "Rodapé"
    );

    private static final List<String> DEFAULT_SECTIONS_ORDER = List.of(
            "HEADER", "HERO", "ABOUT", "SERVICES", "PRODUCTS", "TEAM", "BOOKING", "FOOTER"
    );

    /**
     * Sincroniza a edição de uma seção do site-config para a LandingPage home.
     * Cria a LandingPage e a seção se não existirem.
     */
    private void syncSectionToLandingPage(Long orgId, String sectionType, Object settingsDto) {
        try {
            LandingPage homePage = findOrCreateHomeLandingPage(orgId);
            LandingPageSection section = findOrCreateSection(homePage, sectionType);
            section.setSettings(toJson(settingsDto));
            sectionRepository.save(section);
        } catch (Exception e) {
            log.warn("Erro ao sincronizar seção {} com LandingPage: {}", sectionType, e.getMessage());
            // Não propaga o erro - o SitePublicoConfig já foi salvo com sucesso
        }
    }

    /**
     * Sincroniza configurações gerais para a LandingPage home.
     */
    private void syncGeneralToLandingPage(Long orgId, GeneralSettingsRequest dto) {
        try {
            LandingPage homePage = findOrCreateHomeLandingPage(orgId);

            if (dto.getCustomCss() != null) homePage.setCustomCss(dto.getCustomCss());
            if (dto.getCustomJs() != null) homePage.setCustomJs(dto.getCustomJs());

            // Reordenar seções da landing page baseado no homeSectionsOrder
            if (dto.getHomeSectionsOrder() != null) {
                List<String> order = dto.getHomeSectionsOrder();
                for (int i = 0; i < order.size(); i++) {
                    String tipo = order.get(i);
                    LandingPageSection section = findOrCreateSection(homePage, tipo);
                    section.setOrdem(i);
                    section.setVisivel(true);
                }
                // Seções não listadas ficam ocultas
                for (LandingPageSection s : homePage.getSections()) {
                    if (Boolean.TRUE.equals(s.getAtivo()) && !order.contains(s.getTipo())) {
                        s.setVisivel(false);
                    }
                }
            }

            landingPageRepository.save(homePage);
        } catch (Exception e) {
            log.warn("Erro ao sincronizar general com LandingPage: {}", e.getMessage());
        }
    }

    private LandingPage findOrCreateHomeLandingPage(Long orgId) {
        // 1) Busca home ativa com seções
        Optional<LandingPage> home = landingPageRepository.findHomeWithSections(orgId);
        if (home.isPresent()) {
            return home.get();
        }

        // 2) Fallback: busca pelo slug "home" (pode estar com isHome=false ou ativo=false)
        Optional<LandingPage> bySlug = landingPageRepository.findByOrganizacaoIdAndSlug(orgId, "home");
        if (bySlug.isPresent()) {
            LandingPage lp = bySlug.get();
            lp.setIsHome(true);
            lp.setAtivo(true);
            return landingPageRepository.save(lp);
        }

        // 3) Não existe — cria nova
        Organizacao org = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada: " + orgId));

        LandingPage lp = LandingPage.builder()
                .organizacao(org)
                .slug("home")
                .nome("Página Inicial")
                .tipo("HOME")
                .isHome(true)
                .status("DRAFT")
                .versao(1)
                .ativo(true)
                .sections(new ArrayList<>())
                .build();

        // Criar seções padrão
        for (int i = 0; i < DEFAULT_SECTIONS_ORDER.size(); i++) {
            String tipo = DEFAULT_SECTIONS_ORDER.get(i);
            LandingPageSection section = LandingPageSection.builder()
                    .sectionId(UUID.randomUUID().toString())
                    .tipo(tipo)
                    .nome(SECTION_DEFAULT_NAMES.getOrDefault(tipo, tipo))
                    .ordem(i)
                    .visivel(true)
                    .locked(false)
                    .ativo(true)
                    .build();
            lp.addSection(section);
        }

        return landingPageRepository.save(lp);
    }

    private LandingPageSection findOrCreateSection(LandingPage lp, String sectionType) {
        return lp.getSections().stream()
                .filter(s -> sectionType.equals(s.getTipo()) && Boolean.TRUE.equals(s.getAtivo()))
                .findFirst()
                .orElseGet(() -> {
                    int maxOrdem = lp.getSections().stream()
                            .filter(s -> Boolean.TRUE.equals(s.getAtivo()))
                            .mapToInt(LandingPageSection::getOrdem)
                            .max().orElse(-1);

                    LandingPageSection section = LandingPageSection.builder()
                            .sectionId(UUID.randomUUID().toString())
                            .tipo(sectionType)
                            .nome(SECTION_DEFAULT_NAMES.getOrDefault(sectionType, sectionType))
                            .ordem(maxOrdem + 1)
                            .visivel(true)
                            .locked(false)
                            .ativo(true)
                            .build();
                    lp.addSection(section);
                    return section;
                });
    }
}
