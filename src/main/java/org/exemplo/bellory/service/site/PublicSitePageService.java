package org.exemplo.bellory.service.site;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.exemplo.bellory.model.dto.site.*;
import org.exemplo.bellory.model.dto.site.request.TransitionConfigRequest;
import org.exemplo.bellory.model.dto.tenent.*;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.funcionario.HorarioTrabalho;
import org.exemplo.bellory.model.entity.funcionario.JornadaDia;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.organizacao.RedesSociais;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.landingpage.LandingPage;
import org.exemplo.bellory.model.entity.landingpage.LandingPageSection;
import org.exemplo.bellory.model.entity.organizacao.BloqueioOrganizacao;
import org.exemplo.bellory.model.entity.organizacao.HorarioFuncionamento;
import org.exemplo.bellory.model.entity.organizacao.PeriodoFuncionamento;
import org.exemplo.bellory.model.entity.site.SitePublicoConfig;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.landingpage.LandingPageRepository;
import org.exemplo.bellory.model.repository.organizacao.BloqueioOrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.HorarioFuncionamentoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.produtos.ProdutoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.site.SitePublicoConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsável por montar as páginas do site público.
 * Agrega dados de múltiplas fontes para construir as respostas da API.
 */
@Service
@Transactional(readOnly = true)
public class PublicSitePageService {

    private final OrganizacaoRepository organizacaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ServicoRepository servicoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final SitePublicoConfigRepository siteConfigRepository;
    private final LandingPageRepository landingPageRepository;
    private final BloqueioOrganizacaoRepository bloqueioOrganizacaoRepository;
    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private final ObjectMapper objectMapper;

    public PublicSitePageService(
            OrganizacaoRepository organizacaoRepository,
            FuncionarioRepository funcionarioRepository,
            ServicoRepository servicoRepository,
            CategoriaRepository categoriaRepository,
            ProdutoRepository produtoRepository,
            SitePublicoConfigRepository siteConfigRepository,
            LandingPageRepository landingPageRepository,
            BloqueioOrganizacaoRepository bloqueioOrganizacaoRepository,
            HorarioFuncionamentoRepository horarioFuncionamentoRepository,
            ObjectMapper objectMapper) {
        this.organizacaoRepository = organizacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.servicoRepository = servicoRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
        this.siteConfigRepository = siteConfigRepository;
        this.landingPageRepository = landingPageRepository;
        this.bloqueioOrganizacaoRepository = bloqueioOrganizacaoRepository;
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== HOME PAGE ====================

    /**
     * Retorna todos os dados necessários para renderizar a home page.
     * Prioriza LandingPage publicada (is_home=true), senão usa SitePublicoConfig.
     */
    public Optional<HomePageDTO> getHomePage(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Organizacao org = orgOpt.get();
        Long orgId = org.getId();

        // Prioridade: LandingPage publicada com is_home=true
        // Se existir, usa os settings das seções da LandingPage como SitePublicoConfig
        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(null);

        Optional<LandingPage> publishedHome = landingPageRepository.findPublishedHomeByOrgSlug(slug);
        if (publishedHome.isPresent() && siteConfig != null) {
            // Merge: settings da LandingPage sobrescrevem o SitePublicoConfig
            mergeLandingPageSettingsIntoConfig(publishedHome.get(), siteConfig);
        }

        if (siteConfig == null) {
            siteConfig = buildDefaultSiteConfig(org);
        }

        // Buscar dados relacionados
        List<Funcionario> funcionarios = funcionarioRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsVisivelExternoTrue(orgId);

        List<Servico> todosServicos = servicoRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsDeletadoFalseOrderByNomeAsc(orgId);

        List<Servico> servicosDestaque = todosServicos.stream()
                .filter(Servico::isHome)
                .limit(siteConfig.getServicesFeaturedLimit() != null ? siteConfig.getServicesFeaturedLimit() : 6)
                .collect(Collectors.toList());

        List<Categoria> categorias = categoriaRepository.findByOrganizacao_IdAndAtivoTrue(orgId);

        List<Produto> produtosDestaque = produtoRepository
                .findByOrganizacao_IdAndAtivoTrueAndDestaqueTrue(orgId);
        if (siteConfig.getProductsFeaturedLimit() != null) {
            produtosDestaque = produtosDestaque.stream()
                    .limit(siteConfig.getProductsFeaturedLimit())
                    .collect(Collectors.toList());
        }

        // Montar resposta
        HomePageDTO homePage = HomePageDTO.builder()
                .organizacao(convertOrganizacao(org))
                .siteConfig(buildSiteConfigDTO(org))
                .header(buildHeaderConfig(org, siteConfig))
                .hero(buildHeroSection(org, siteConfig, funcionarios, todosServicos))
                .about(buildAboutSection(org, siteConfig, false))
                .services(buildServicesSection(siteConfig, servicosDestaque, categorias, todosServicos.size()))
                .products(buildProductsSection(siteConfig, produtosDestaque))
                .team(buildTeamSection(siteConfig, funcionarios))
                .booking(buildBookingSection(org, siteConfig, todosServicos, funcionarios))
                .horariosFuncionamento(buildHorariosFuncionamento(org.getId()))
                .footer(buildFooterConfig(org, siteConfig, funcionarios))
                .sectionsOrder(parseSectionsOrder(siteConfig.getHomeSectionsOrder()))
                .transitions(parseTransitions(siteConfig.getTransitions()))
                .seo(buildSeoMetadata(org))
                .features(buildFeatures(org))
                .customAssets(buildCustomAssets(siteConfig))
                .build();

        return Optional.of(homePage);
    }

    // ==================== HEADER ====================

    public Optional<HeaderConfigDTO> getHeader(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Organizacao org = orgOpt.get();
        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(org));

        return Optional.of(buildHeaderConfig(org, siteConfig));
    }

    // ==================== HERO ====================

    public Optional<HeroSectionDTO> getHero(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Organizacao org = orgOpt.get();
        Long orgId = org.getId();

        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(org));

        List<Funcionario> funcionarios = funcionarioRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsVisivelExternoTrue(orgId);

        List<Servico> servicos = servicoRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsDeletadoFalseOrderByNomeAsc(orgId);

        return Optional.of(buildHeroSection(org, siteConfig, funcionarios, servicos));
    }

    // ==================== ABOUT ====================

    public Optional<AboutSectionDTO> getAbout(String slug, boolean fullVersion) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Organizacao org = orgOpt.get();
        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(org));

        return Optional.of(buildAboutSection(org, siteConfig, fullVersion));
    }

    // ==================== SERVICES ====================

    public Optional<ServicesSectionDTO> getFeaturedServices(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Long orgId = orgOpt.get().getId();

        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(orgOpt.get()));

        List<Servico> todosServicos = servicoRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsDeletadoFalseOrderByNomeAsc(orgId);

        List<Servico> servicosDestaque = todosServicos.stream()
                .filter(Servico::isHome)
                .limit(siteConfig.getServicesFeaturedLimit() != null ? siteConfig.getServicesFeaturedLimit() : 6)
                .collect(Collectors.toList());

        List<Categoria> categorias = categoriaRepository.findByOrganizacao_IdAndAtivoTrue(orgId);

        return Optional.of(buildServicesSection(siteConfig, servicosDestaque, categorias, todosServicos.size()));
    }

    public Optional<ServicesSectionDTO> getAllServices(String slug, int page, int size) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Long orgId = orgOpt.get().getId();

        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(orgOpt.get()));

        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<Servico> servicosPage = servicoRepository
                .findByOrganizacao_IdAndAtivoTrueAndIsDeletadoFalse(orgId, pageable);

        List<Categoria> categorias = categoriaRepository.findByOrganizacao_IdAndAtivoTrue(orgId);

        ServicesSectionDTO result = buildServicesSection(siteConfig,
                servicosPage.getContent(), categorias, (int) servicosPage.getTotalElements());

        return Optional.of(result);
    }

    public Optional<ServicoDetalhadoDTO> getServiceById(String slug, Long servicoId) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Long orgId = orgOpt.get().getId();

        Optional<Servico> servicoOpt = servicoRepository.findById(servicoId);
        if (servicoOpt.isEmpty() || !servicoOpt.get().getOrganizacao().getId().equals(orgId)) {
            return Optional.empty();
        }

        return Optional.of(convertServicoDetalhado(servicoOpt.get(), slug));
    }

    // ==================== PRODUCTS ====================

    public Optional<ProductsSectionDTO> getFeaturedProducts(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Long orgId = orgOpt.get().getId();

        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(orgOpt.get()));

        List<Produto> produtosDestaque = produtoRepository
                .findByOrganizacao_IdAndAtivoTrueAndDestaqueTrue(orgId);

        if (siteConfig.getProductsFeaturedLimit() != null) {
            produtosDestaque = produtosDestaque.stream()
                    .limit(siteConfig.getProductsFeaturedLimit())
                    .collect(Collectors.toList());
        }

        return Optional.of(buildProductsSection(siteConfig, produtosDestaque));
    }

    public Optional<ProductsSectionDTO> getAllProducts(String slug, int page, int size) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Long orgId = orgOpt.get().getId();

        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(orgOpt.get()));

        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<Produto> produtosPage = produtoRepository
                .findByOrganizacao_IdAndAtivoTrue(orgId, pageable);

        List<Categoria> categorias = categoriaRepository.findByOrganizacao_IdAndAtivoTrue(orgId);

        ProductsSectionDTO result = ProductsSectionDTO.builder()
                .title(siteConfig.getProductsSectionTitle())
                .subtitle(siteConfig.getProductsSectionSubtitle())
                .showPrices(siteConfig.getProductsShowPrices())
                .produtos(produtosPage.getContent().stream()
                        .map(this::convertProduto)
                        .collect(Collectors.toList()))
                .categorias(convertCategoriasProdutos(categorias))
                .totalProdutos((int) produtosPage.getTotalElements())
                .build();

        return Optional.of(result);
    }

    public Optional<ProdutoDetalhadoDTO> getProductById(String slug, Long produtoId) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Long orgId = orgOpt.get().getId();

        Optional<Produto> produtoOpt = produtoRepository.findById(produtoId);
        if (produtoOpt.isEmpty() || !produtoOpt.get().getOrganizacao().getId().equals(orgId)) {
            return Optional.empty();
        }

        return Optional.of(convertProdutoDetalhado(produtoOpt.get(), slug));
    }

    // ==================== TEAM ====================

    public Optional<TeamSectionDTO> getTeam(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Long orgId = orgOpt.get().getId();

        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(orgOpt.get()));

        List<Funcionario> funcionarios = funcionarioRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsVisivelExternoTrue(orgId);

        return Optional.of(buildTeamSection(siteConfig, funcionarios));
    }

    // ==================== BOOKING ====================

    // aqui precisa passar mais informacoes, como: dias bloqueados (feriados - bloqueio), se um dia da semana nao tiver horario na organizacao, esse dia da semana deve ser bloqueado,
    // o dia minimo e o dia maximo para agendamento

    public Optional<BookingSectionDTO> getBookingInfo(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Organizacao org = orgOpt.get();
        Long orgId = org.getId();

        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(org));

        List<Servico> servicos = servicoRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsDeletadoFalseOrderByNomeAsc(orgId);

        List<Funcionario> funcionarios = funcionarioRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsVisivelExternoTrue(orgId);

        return Optional.of(buildBookingSection(org, siteConfig, servicos, funcionarios));
    }

    // ==================== FOOTER ====================

    public Optional<FooterConfigDTO> getFooter(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Organizacao org = orgOpt.get();
        Long orgId = org.getId();

        SitePublicoConfig siteConfig = siteConfigRepository
                .findByOrganizacaoSlugAndActive(slug)
                .orElse(buildDefaultSiteConfig(org));

        List<Funcionario> funcionarios = funcionarioRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsVisivelExternoTrue(orgId);

        return Optional.of(buildFooterConfig(org, siteConfig, funcionarios));
    }

    // ==================== LANDING PAGE INTEGRATION ====================

    /**
     * Aplica os settings das seções de uma LandingPage publicada sobre o SitePublicoConfig.
     * Isso permite que edições feitas no editor de landing pages reflitam no site público.
     */
    private void mergeLandingPageSettingsIntoConfig(LandingPage landingPage, SitePublicoConfig config) {
        if (landingPage.getSections() == null) return;

        for (LandingPageSection section : landingPage.getSections()) {
            if (!Boolean.TRUE.equals(section.getAtivo()) || !Boolean.TRUE.equals(section.getVisivel())) {
                continue;
            }
            if (section.getSettings() == null || section.getSettings().isBlank()) {
                continue;
            }

            try {
                Map<String, Object> settings = objectMapper.readValue(
                        section.getSettings(), new TypeReference<Map<String, Object>>() {});

                switch (section.getTipo()) {
                    case "HERO":
                        applyIfPresent(settings, "title", v -> config.setHeroTitle((String) v));
                        applyIfPresent(settings, "subtitle", v -> config.setHeroSubtitle((String) v));
                        applyIfPresent(settings, "backgroundUrl", v -> config.setHeroBackgroundUrl((String) v));
                        applyIfPresent(settings, "backgroundOverlay", v -> config.setHeroBackgroundOverlay(((Number) v).doubleValue()));
                        applyIfPresent(settings, "type", v -> config.setHeroType((String) v));
                        applyIfPresent(settings, "customHtml", v -> config.setHeroCustomHtml((String) v));
                        applyIfPresent(settings, "showBookingForm", v -> config.setHeroShowBookingForm((Boolean) v));
                        if (settings.containsKey("buttons")) {
                            config.setHeroButtons(objectMapper.writeValueAsString(settings.get("buttons")));
                        }
                        break;
                    case "HEADER":
                        applyIfPresent(settings, "logoUrl", v -> config.setHeaderLogoUrl((String) v));
                        applyIfPresent(settings, "logoAlt", v -> config.setHeaderLogoAlt((String) v));
                        applyIfPresent(settings, "showPhone", v -> config.setHeaderShowPhone((Boolean) v));
                        applyIfPresent(settings, "showSocial", v -> config.setHeaderShowSocial((Boolean) v));
                        applyIfPresent(settings, "sticky", v -> config.setHeaderSticky((Boolean) v));
                        if (settings.containsKey("menuItems")) {
                            config.setHeaderMenuItems(objectMapper.writeValueAsString(settings.get("menuItems")));
                        }
                        if (settings.containsKey("actionButtons")) {
                            config.setHeaderActionButtons(objectMapper.writeValueAsString(settings.get("actionButtons")));
                        }
                        break;
                    case "ABOUT":
                        applyIfPresent(settings, "title", v -> config.setAboutTitle((String) v));
                        applyIfPresent(settings, "subtitle", v -> config.setAboutSubtitle((String) v));
                        applyIfPresent(settings, "description", v -> config.setAboutDescription((String) v));
                        applyIfPresent(settings, "fullDescription", v -> config.setAboutFullDescription((String) v));
                        applyIfPresent(settings, "imageUrl", v -> config.setAboutImageUrl((String) v));
                        applyIfPresent(settings, "videoUrl", v -> config.setAboutVideoUrl((String) v));
                        applyIfPresent(settings, "mission", v -> config.setAboutMission((String) v));
                        applyIfPresent(settings, "vision", v -> config.setAboutVision((String) v));
                        applyIfPresent(settings, "values", v -> config.setAboutValues((String) v));
                        if (settings.containsKey("galleryImages")) {
                            config.setAboutGalleryImages(objectMapper.writeValueAsString(settings.get("galleryImages")));
                        }
                        if (settings.containsKey("highlights")) {
                            config.setAboutHighlights(objectMapper.writeValueAsString(settings.get("highlights")));
                        }
                        break;
                    case "SERVICES":
                        applyIfPresent(settings, "sectionTitle", v -> config.setServicesSectionTitle((String) v));
                        applyIfPresent(settings, "sectionSubtitle", v -> config.setServicesSectionSubtitle((String) v));
                        applyIfPresent(settings, "showPrices", v -> config.setServicesShowPrices((Boolean) v));
                        applyIfPresent(settings, "showDuration", v -> config.setServicesShowDuration((Boolean) v));
                        applyIfPresent(settings, "featuredLimit", v -> config.setServicesFeaturedLimit(((Number) v).intValue()));
                        break;
                    case "PRODUCTS":
                        applyIfPresent(settings, "sectionTitle", v -> config.setProductsSectionTitle((String) v));
                        applyIfPresent(settings, "sectionSubtitle", v -> config.setProductsSectionSubtitle((String) v));
                        applyIfPresent(settings, "showPrices", v -> config.setProductsShowPrices((Boolean) v));
                        applyIfPresent(settings, "featuredLimit", v -> config.setProductsFeaturedLimit(((Number) v).intValue()));
                        break;
                    case "TEAM":
                        applyIfPresent(settings, "sectionTitle", v -> config.setTeamSectionTitle((String) v));
                        applyIfPresent(settings, "sectionSubtitle", v -> config.setTeamSectionSubtitle((String) v));
                        applyIfPresent(settings, "showSection", v -> config.setTeamShowSection((Boolean) v));
                        break;
                    case "BOOKING":
                        applyIfPresent(settings, "sectionTitle", v -> config.setBookingSectionTitle((String) v));
                        applyIfPresent(settings, "sectionSubtitle", v -> config.setBookingSectionSubtitle((String) v));
                        applyIfPresent(settings, "enabled", v -> config.setBookingEnabled((Boolean) v));
                        break;
                    case "FOOTER":
                        applyIfPresent(settings, "description", v -> config.setFooterDescription((String) v));
                        applyIfPresent(settings, "logoUrl", v -> config.setFooterLogoUrl((String) v));
                        applyIfPresent(settings, "copyrightText", v -> config.setFooterCopyrightText((String) v));
                        applyIfPresent(settings, "showMap", v -> config.setFooterShowMap((Boolean) v));
                        applyIfPresent(settings, "showHours", v -> config.setFooterShowHours((Boolean) v));
                        applyIfPresent(settings, "showSocial", v -> config.setFooterShowSocial((Boolean) v));
                        applyIfPresent(settings, "showNewsletter", v -> config.setFooterShowNewsletter((Boolean) v));
                        if (settings.containsKey("linkSections")) {
                            config.setFooterLinkSections(objectMapper.writeValueAsString(settings.get("linkSections")));
                        }
                        break;
                }
            } catch (Exception e) {
                // Log e continua - não quebra a renderização por um settings inválido
            }
        }

        // Atualizar sectionsOrder baseado na ordem das seções visíveis da LandingPage
        List<String> sectionsOrder = landingPage.getSections().stream()
                .filter(s -> Boolean.TRUE.equals(s.getAtivo()) && Boolean.TRUE.equals(s.getVisivel()))
                .sorted(Comparator.comparingInt(LandingPageSection::getOrdem))
                .map(LandingPageSection::getTipo)
                .collect(Collectors.toList());
        if (!sectionsOrder.isEmpty()) {
            try {
                config.setHomeSectionsOrder(objectMapper.writeValueAsString(sectionsOrder));
            } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void applyIfPresent(Map<String, Object> settings, String key, java.util.function.Consumer<Object> setter) {
        Object value = settings.get(key);
        if (value != null) {
            setter.accept(value);
        }
    }

    // ==================== PRIVATE BUILD METHODS ====================

    private SitePublicoConfig buildDefaultSiteConfig(Organizacao org) {
        return SitePublicoConfig.builder()
                .organizacao(org)
                .heroTitle("Bem-vindo à " + org.getNomeFantasia())
                .heroSubtitle("Qualidade e excelência em serviços")
                .aboutTitle("Sobre Nós")
                .servicesSectionTitle("Nossos Serviços")
                .productsSectionTitle("Produtos em Destaque")
                .teamSectionTitle("Nossa Equipe")
                .bookingSectionTitle("Agende seu Horário")
                .servicesFeaturedLimit(6)
                .productsFeaturedLimit(8)
                .headerShowPhone(true)
                .headerSticky(true)
                .footerShowMap(true)
                .footerShowHours(true)
                .footerShowSocial(true)
                .bookingEnabled(true)
                .teamShowSection(true)
                .active(true)
                .build();
    }

    private HeaderConfigDTO buildHeaderConfig(Organizacao org, SitePublicoConfig config) {
        HeaderConfigDTO.SocialLinksDTO socialLinks = null;
        if (org.getRedesSociais() != null) {
            RedesSociais rs = org.getRedesSociais();
            socialLinks = HeaderConfigDTO.SocialLinksDTO.builder()
                    .instagram(rs.getInstagram())
                    .facebook(rs.getFacebook())
                    .whatsapp(rs.getWhatsapp())
                    .youtube(rs.getYoutube())
                    .linkedin(rs.getLinkedin())
                    .build();
        }

        List<HeaderConfigDTO.MenuItemDTO> menuItems = parseMenuItems(config.getHeaderMenuItems());
        if (menuItems == null || menuItems.isEmpty()) {
            menuItems = buildDefaultMenuItems();
        }

        List<HeaderConfigDTO.ActionButtonDTO> actionButtons = parseActionButtons(config.getHeaderActionButtons());
        if (actionButtons == null || actionButtons.isEmpty()) {
            actionButtons = List.of(
                    HeaderConfigDTO.ActionButtonDTO.builder()
                            .label("Agendar")
                            .href("#agendar")
                            .type("primary")
                            .build()
            );
        }

        return HeaderConfigDTO.builder()
                .logoUrl(config.getHeaderLogoUrl())
                .logoAlt(config.getHeaderLogoAlt() != null ? config.getHeaderLogoAlt() : org.getNomeFantasia())
                .menuItems(menuItems)
                .actionButtons(actionButtons)
                .showPhone(config.getHeaderShowPhone())
                .phoneNumber(org.getTelefone1())
                .showSocial(config.getHeaderShowSocial())
                .sticky(config.getHeaderSticky())
                .headerLayout(config.getHeaderLayout())
                .menuStyle(config.getHeaderMenuStyle())
                .transparentOnHero(config.getHeaderTransparentOnHero())
                .showCart(config.getHeaderShowCart())
                .logoHeight(config.getHeaderLogoHeight())
                .socialLinks(socialLinks)
                .backgroundColor(config.getHeaderBackgroundColor())
                .backgroundPattern(config.getHeaderBackgroundPattern())
                .patternOpacity(config.getHeaderPatternOpacity())
                .build();
    }

    private HeroSectionDTO buildHeroSection(Organizacao org, SitePublicoConfig config,
                                            List<Funcionario> funcionarios, List<Servico> servicos) {
        List<HeroSectionDTO.HeroButtonDTO> buttons = parseHeroButtons(config.getHeroButtons());
        if (buttons == null || buttons.isEmpty()) {
            buttons = List.of(
                    HeroSectionDTO.HeroButtonDTO.builder()
                            .label("Agendar Agora")
                            .href("#agendar")
                            .type("primary")
                            .build(),
                    HeroSectionDTO.HeroButtonDTO.builder()
                            .label("Ver Serviços")
                            .href("#servicos")
                            .type("secondary")
                            .build()
            );
        }

        HeroSectionDTO.HeroStatsDTO stats = HeroSectionDTO.HeroStatsDTO.builder()
                .servicesCount(servicos.size())
                .teamSize(funcionarios.size())
                .build();

        return HeroSectionDTO.builder()
                .type(config.getHeroType())
                .title(config.getHeroTitle() != null ? config.getHeroTitle() : "Bem-vindo à " + org.getNomeFantasia())
                .subtitle(config.getHeroSubtitle())
                .backgroundUrl(config.getHeroBackgroundUrl())
                .backgroundOverlay(config.getHeroBackgroundOverlay())
                .customHtml(config.getHeroCustomHtml())
                .buttons(buttons)
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
                .statsConfig(parseStatsConfig(config.getHeroStatsConfig()))
                .stats(stats)
                .backgroundColor(config.getHeroBackgroundColor())
                .backgroundPattern(config.getHeroBackgroundPattern())
                .patternOpacity(config.getHeroPatternOpacity())
                .build();
    }

    private AboutSectionDTO buildAboutSection(Organizacao org, SitePublicoConfig config, boolean fullVersion) {
        List<AboutSectionDTO.HighlightDTO> highlights = parseHighlights(config.getAboutHighlights());

        AboutSectionDTO.OrganizationInfoDTO orgInfo = AboutSectionDTO.OrganizationInfoDTO.builder()
                .name(org.getNomeFantasia())
                .phone(org.getTelefone1())
                .email(org.getEmailPrincipal())
                .whatsapp(org.getWhatsapp())
                .address(formatAddress(org.getEnderecoPrincipal()))
                .build();

        AboutSectionDTO.AboutSectionDTOBuilder builder = AboutSectionDTO.builder()
                .title(config.getAboutTitle())
                .subtitle(config.getAboutSubtitle())
                .description(config.getAboutDescription())
                .imageUrl(config.getAboutImageUrl())
                .videoUrl(config.getAboutVideoUrl())
                .galleryImages(parseStringList(config.getAboutGalleryImages()))
                .highlights(highlights)
                .showGallery(config.getAboutShowGallery())
                .showHighlights(config.getAboutShowHighlights())
                .showMVV(config.getAboutShowMVV())
                .layoutStyle(config.getAboutLayoutStyle())
                .yearFounded(config.getAboutYearFounded())
                .teamPhotoUrl(config.getAboutTeamPhotoUrl())
                .backgroundColor(config.getAboutBackgroundColor())
                .backgroundPattern(config.getAboutBackgroundPattern())
                .patternOpacity(config.getAboutPatternOpacity())
                .organizationInfo(orgInfo);

        if (fullVersion) {
            builder.fullDescription(config.getAboutFullDescription())
                    .mission(config.getAboutMission())
                    .vision(config.getAboutVision())
                    .values(config.getAboutValues());
        }

        return builder.build();
    }

    private ServicesSectionDTO buildServicesSection(SitePublicoConfig config, List<Servico> servicos,
                                                    List<Categoria> categorias, int totalServicos) {
        return ServicesSectionDTO.builder()
                .title(config.getServicesSectionTitle())
                .subtitle(config.getServicesSectionSubtitle())
                .showPrices(config.getServicesShowPrices())
                .showDuration(config.getServicesShowDuration())
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
                .servicos(servicos.stream().map(this::convertServico).collect(Collectors.toList()))
                .categorias(convertCategoriasServicos(categorias, servicos))
                .totalServicos(totalServicos)
                .build();
    }

    private ProductsSectionDTO buildProductsSection(SitePublicoConfig config, List<Produto> produtos) {
        return ProductsSectionDTO.builder()
                .title(config.getProductsSectionTitle())
                .subtitle(config.getProductsSectionSubtitle())
                .showPrices(config.getProductsShowPrices())
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
                .produtos(produtos.stream().map(this::convertProduto).collect(Collectors.toList()))
                .totalProdutos(produtos.size())
                .build();
    }

    private TeamSectionDTO buildTeamSection(SitePublicoConfig config, List<Funcionario> funcionarios) {
        return TeamSectionDTO.builder()
                .title(config.getTeamSectionTitle())
                .subtitle(config.getTeamSectionSubtitle())
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
                .membros(funcionarios.stream().map(this::convertFuncionario).collect(Collectors.toList()))
                .totalMembros(funcionarios.size())
                .build();
    }

    private BookingSectionDTO buildBookingSection(Organizacao org, SitePublicoConfig config,
                                                  List<Servico> servicos, List<Funcionario> funcionarios) {
        ConfigSistema configSistema = org.getConfigSistema();

        var configAgendamento = configSistema != null ? configSistema.getConfigAgendamento() : null;

        Integer minDias = configAgendamento != null && configAgendamento.getMinDiasAgendamento() != null
                ? configAgendamento.getMinDiasAgendamento() : 1;
        Integer maxDias = configAgendamento != null && configAgendamento.getMaxDiasAgendamento() != null
                ? configAgendamento.getMaxDiasAgendamento() : 90;
        Boolean cobrarSinal = configAgendamento != null && configAgendamento.getCobrarSinal() != null
                ? configAgendamento.getCobrarSinal() : false;
        Integer porcentSinal = configAgendamento != null ? configAgendamento.getPorcentSinal() : null;

        BookingSectionDTO.BookingConfigDTO bookingConfig = BookingSectionDTO.BookingConfigDTO.builder()
                .requiresDeposit(cobrarSinal)
                .depositPercentage(porcentSinal != null ? porcentSinal.doubleValue() : null)
                .minAdvanceHours(1)
                .minAdvanceDays(minDias)
                .maxAdvanceDays(maxDias)
                .allowMultipleServices(true)
                .requiresLogin(false)
                .build();

        // Bloqueios ativos no período de agendamento (de hoje até maxDias)
        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(maxDias);
        List<BloqueioOrganizacao> bloqueios = bloqueioOrganizacaoRepository
                .findBloqueiosAtivosNoPeriodo(org.getId(), hoje, dataLimite);

        List<BookingSectionDTO.BloqueioDTO> diasBloqueados = bloqueios.stream()
                .map(b -> BookingSectionDTO.BloqueioDTO.builder()
                        .titulo(b.getTitulo())
                        .dataInicio(b.getDataInicio())
                        .dataFim(b.getDataFim())
                        .tipo(b.getTipo().name())
                        .build())
                .collect(Collectors.toList());

        return BookingSectionDTO.builder()
                .title(config.getBookingSectionTitle())
                .subtitle(config.getBookingSectionSubtitle())
                .enabled(config.getBookingEnabled())
                .backgroundColor(config.getBookingBackgroundColor())
                .backgroundPattern(config.getBookingBackgroundPattern())
                .patternOpacity(config.getBookingPatternOpacity())
                .servicosDisponiveis(servicos.stream().map(this::convertServico).collect(Collectors.toList()))
                .profissionaisDisponiveis(funcionarios.stream().map(this::convertFuncionario).collect(Collectors.toList()))
                .config(bookingConfig)
                .diasBloqueados(diasBloqueados)
                .horariosFuncionamento(buildHorariosFuncionamento(org.getId()))
                .build();
    }

    private FooterConfigDTO buildFooterConfig(Organizacao org, SitePublicoConfig config,
                                              List<Funcionario> funcionarios) {
        HeaderConfigDTO.SocialLinksDTO socialLinks = null;
        if (org.getRedesSociais() != null) {
            RedesSociais rs = org.getRedesSociais();
            socialLinks = HeaderConfigDTO.SocialLinksDTO.builder()
                    .instagram(rs.getInstagram())
                    .facebook(rs.getFacebook())
                    .whatsapp(rs.getWhatsapp())
                    .youtube(rs.getYoutube())
                    .linkedin(rs.getLinkedin())
                    .build();
        }

        FooterConfigDTO.ContactInfoDTO contactInfo = FooterConfigDTO.ContactInfoDTO.builder()
                .phone(org.getTelefone1())
                .whatsapp(org.getWhatsapp())
                .email(org.getEmailPrincipal())
                .address(formatAddress(org.getEnderecoPrincipal()))
                .build();

        List<FooterConfigDTO.LinkSectionDTO> linkSections = parseLinkSections(config.getFooterLinkSections());
        if (linkSections == null || linkSections.isEmpty()) {
            linkSections = buildDefaultFooterLinks();
        }

        String copyright = config.getFooterCopyrightText();
        if (copyright == null || copyright.isEmpty()) {
            copyright = String.format("© %d %s. Todos os direitos reservados.",
                    java.time.Year.now().getValue(), org.getNomeFantasia());
        }

        return FooterConfigDTO.builder()
                .logoUrl(config.getFooterLogoUrl() != null ? config.getFooterLogoUrl() : config.getHeaderLogoUrl())
                .description(config.getFooterDescription())
                .copyrightText(copyright)
                .linkSections(linkSections)
                .contactInfo(contactInfo)
                .socialLinks(socialLinks)
                .showMap(config.getFooterShowMap())
                .showHours(config.getFooterShowHours())
                .showSocial(config.getFooterShowSocial())
                .showNewsletter(config.getFooterShowNewsletter())
                .horariosFuncionamento(buildHorariosFuncionamento(org.getId()))
                .endereco(convertEndereco(org.getEnderecoPrincipal()))
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

    // ==================== CONVERSION METHODS ====================

    private OrganizacaoPublicDTO convertOrganizacao(Organizacao org) {
        OrganizacaoPublicDTO.OrganizacaoPublicDTOBuilder builder = OrganizacaoPublicDTO.builder()
                .id(org.getId())
                .nome(org.getNomeFantasia())
                .nomeFantasia(org.getNomeFantasia())
                .slug(org.getSlug())
                .email(org.getEmailPrincipal())
                .telefone(org.getTelefone1())
                .whatsapp(org.getWhatsapp());

        if (org.getEnderecoPrincipal() != null) {
            builder.endereco(convertEndereco(org.getEnderecoPrincipal()));
        }

        return builder.build();
    }

    private EnderecoPublicDTO convertEndereco(Endereco endereco) {
        if (endereco == null) return null;

        return EnderecoPublicDTO.builder()
                .logradouro(endereco.getLogradouro())
                .numero(endereco.getNumero())
                .complemento(endereco.getComplemento())
                .bairro(endereco.getBairro())
                .cidade(endereco.getCidade())
                .uf(endereco.getUf())
                .cep(endereco.getCep())
                .build();
    }

    private ServicoPublicDTO convertServico(Servico servico) {
        BigDecimal precoComDesconto = null;
        if (servico.getDesconto() != null && servico.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal desconto = servico.getPreco()
                    .multiply(servico.getDesconto())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            precoComDesconto = servico.getPreco().subtract(desconto);
        }

        List<Long> funcionarioIds = servico.getFuncionarios() != null
                ? servico.getFuncionarios().stream()
                .filter(Funcionario::isAtivo)
                .filter(Funcionario::isVisivelExterno)
                .map(Funcionario::getId)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return ServicoPublicDTO.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .descricao(servico.getDescricao())
                .categoriaId(servico.getCategoria() != null ? servico.getCategoria().getId() : null)
                .categoriaNome(servico.getCategoria() != null ? servico.getCategoria().getLabel() : null)
                .genero(servico.getGenero())
                .tempoEstimadoMinutos(servico.getTempoEstimadoMinutos())
                .preco(servico.getPreco())
                .precoComDesconto(precoComDesconto)
                .descontoPercentual(servico.getDesconto())
                .imagens(servico.getUrlsImagens())
                .disponivel(servico.isAtivo())
                .funcionarioIds(funcionarioIds)
                .produtos(servico.getProdutos())
                .build();
    }

    private ServicoDetalhadoDTO convertServicoDetalhado(Servico servico, String slug) {
        BigDecimal precoComDesconto = null;
        if (servico.getDesconto() != null && servico.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal desconto = servico.getPreco()
                    .multiply(servico.getDesconto())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            precoComDesconto = servico.getPreco().subtract(desconto);
        }

        List<FuncionarioPublicDTO> profissionais = servico.getFuncionarios() != null
                ? servico.getFuncionarios().stream()
                .filter(Funcionario::isAtivo)
                .filter(Funcionario::isVisivelExterno)
                .map(this::convertFuncionario)
                .collect(Collectors.toList())
                : Collections.emptyList();

        // Buscar produtos utilizados no serviço
        List<ServicoDetalhadoDTO.ProdutoResumidoDTO> produtosUtilizados = Collections.emptyList();
        if (servico.getProdutos() != null && !servico.getProdutos().isEmpty()) {
            Long orgId = servico.getOrganizacao().getId();
            List<Produto> produtos = produtoRepository
                    .findByOrganizacao_IdAndAtivoTrueAndNomeIn(orgId, servico.getProdutos());
            produtosUtilizados = produtos.stream()
                    .map(p -> ServicoDetalhadoDTO.ProdutoResumidoDTO.builder()
                            .id(p.getId())
                            .nome(p.getNome())
                            .imagemUrl(p.getUrlsImagens() != null && !p.getUrlsImagens().isEmpty()
                                    ? p.getUrlsImagens().get(0) : null)
                            .build())
                    .collect(Collectors.toList());
        }

        ProdutoDetalhadoDTO.SeoDataDTO seo = ProdutoDetalhadoDTO.SeoDataDTO.builder()
                .title(servico.getNome() + " | " + servico.getOrganizacao().getNomeFantasia())
                .description(servico.getDescricao())
                .canonicalUrl("https://app.bellory.com.br/" + slug + "/servicos/" + servico.getId())
                .build();

        return ServicoDetalhadoDTO.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .descricao(servico.getDescricao())
                .categoriaId(servico.getCategoria() != null ? servico.getCategoria().getId() : null)
                .categoriaNome(servico.getCategoria() != null ? servico.getCategoria().getLabel() : null)
                .preco(servico.getPreco())
                .precoComDesconto(precoComDesconto)
                .descontoPercentual(servico.getDesconto())
                .genero(servico.getGenero())
                .tempoEstimadoMinutos(servico.getTempoEstimadoMinutos())
                .disponivel(servico.isAtivo())
                .imagens(servico.getUrlsImagens())
                .imagemPrincipal(servico.getUrlsImagens() != null && !servico.getUrlsImagens().isEmpty()
                        ? servico.getUrlsImagens().get(0) : null)
                .profissionais(profissionais)
                .produtosUtilizados(produtosUtilizados)
                .permiteAgendamentoOnline(true)
                .seo(seo)
                .build();
    }

    private ProdutoPublicDTO convertProduto(Produto produto) {
        BigDecimal precoComDesconto = null;
        if (produto.getDescontoPercentual() != null && produto.getDescontoPercentual() > 0) {
            BigDecimal desconto = produto.getPreco()
                    .multiply(BigDecimal.valueOf(produto.getDescontoPercentual()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            precoComDesconto = produto.getPreco().subtract(desconto);
        }

        return ProdutoPublicDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .preco(produto.getPreco())
                .precoComDesconto(precoComDesconto)
                .descontoPercentual(produto.getDescontoPercentual())
                .imagens(produto.getUrlsImagens())
                .categoria(produto.getCategoria() != null ? produto.getCategoria().getLabel() : null)
                .emEstoque(produto.getQuantidadeEstoque() != null && produto.getQuantidadeEstoque() > 0)
                .avaliacao(produto.getAvaliacao())
                .totalAvaliacoes(produto.getTotalAvaliacoes())
                .build();
    }

    private ProdutoDetalhadoDTO convertProdutoDetalhado(Produto produto, String slug) {
        BigDecimal precoComDesconto = null;
        if (produto.getDescontoPercentual() != null && produto.getDescontoPercentual() > 0) {
            BigDecimal desconto = produto.getPreco()
                    .multiply(BigDecimal.valueOf(produto.getDescontoPercentual()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            precoComDesconto = produto.getPreco().subtract(desconto);
        }

        List<ProdutoPublicDTO> relacionados = new ArrayList<>();
        if (produto.getProdutosRelacionados() != null) {
            relacionados = produto.getProdutosRelacionados().stream()
                    .filter(Produto::isAtivo)
                    .limit(4)
                    .map(this::convertProduto)
                    .collect(Collectors.toList());
        }

        ProdutoDetalhadoDTO.SeoDataDTO seo = ProdutoDetalhadoDTO.SeoDataDTO.builder()
                .title(produto.getNome() + " | " + produto.getOrganizacao().getNomeFantasia())
                .description(produto.getDescricao())
                .canonicalUrl("https://app.bellory.com.br/" + slug + "/produtos/" + produto.getId())
                .ogImage(produto.getUrlsImagens() != null && !produto.getUrlsImagens().isEmpty()
                        ? produto.getUrlsImagens().get(0) : null)
                .build();

        return ProdutoDetalhadoDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .preco(produto.getPreco())
                .precoComDesconto(precoComDesconto)
                .descontoPercentual(produto.getDescontoPercentual())
                .imagens(produto.getUrlsImagens())
                .imagemPrincipal(produto.getUrlsImagens() != null && !produto.getUrlsImagens().isEmpty()
                        ? produto.getUrlsImagens().get(0) : null)
                .categoriaId(produto.getCategoria() != null ? produto.getCategoria().getId() : null)
                .categoriaNome(produto.getCategoria() != null ? produto.getCategoria().getLabel() : null)
                .emEstoque(produto.getQuantidadeEstoque() != null && produto.getQuantidadeEstoque() > 0)
                .quantidadeEstoque(produto.getQuantidadeEstoque())
                .unidade(produto.getUnidade())
                .avaliacao(produto.getAvaliacao())
                .totalAvaliacoes(produto.getTotalAvaliacoes())
                .marca(produto.getMarca())
                .modelo(produto.getModelo())
                .peso(produto.getPeso())
                .codigoBarras(produto.getCodigoBarras())
                .ingredientes(produto.getIngredientes())
                .comoUsar(produto.getComoUsar())
                .especificacoes(produto.getEspecificacoes())
                .produtosRelacionados(relacionados)
                .seo(seo)
                .build();
    }

    private FuncionarioPublicDTO convertFuncionario(Funcionario func) {
        List<Long> servicoIds = new ArrayList<>();
        List<String> servicoNomes = new ArrayList<>();

        if (func.getServicos() != null) {
            for (Servico s : func.getServicos()) {
                servicoIds.add(s.getId());
                servicoNomes.add(s.getNome());
            }
        }

        return FuncionarioPublicDTO.builder()
                .id(func.getId())
                .nome(func.getNomeCompleto())
                .apelido(func.getApelido())
                .foto(func.getFotoPerfil())
                .cargo(func.getCargo() != null ? func.getCargo().getNome() : null)
                .servicoIds(servicoIds)
                .servicoNomes(servicoNomes)
                .horarios(convertJornadasDia(func.getJornadasDia()))
                .build();
    }

    private List<JornadaDiaPublicDTO> convertJornadasDia(List<JornadaDia> jornadas) {
        if (jornadas == null) return Collections.emptyList();

        return jornadas.stream()
                .filter(j -> j.getAtivo() != null && j.getAtivo())
                .map(this::convertJornadaDia)
                .collect(Collectors.toList());
    }

    private JornadaDiaPublicDTO convertJornadaDia(JornadaDia jornada) {
        return JornadaDiaPublicDTO.builder()
                .diaSemana(jornada.getDiaSemana().name())
                .diaSemanaLabel(getDiaSemanaLabel(jornada.getDiaSemana().name()))
                .ativo(jornada.getAtivo())
                .horarios(convertHorariosTrabalho(jornada.getHorarios()))
                .build();
    }

    private List<HorarioTrabalhoPublicDTO> convertHorariosTrabalho(List<HorarioTrabalho> horarios) {
        if (horarios == null) return Collections.emptyList();

        return horarios.stream()
                .map(h -> HorarioTrabalhoPublicDTO.builder()
                        .inicio(h.getHoraInicio().toString())
                        .fim(h.getHoraFim().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CategoriaPublicDTO> convertCategoriasServicos(List<Categoria> categorias, List<Servico> servicos) {
        Map<Long, Long> servicosPorCategoria = servicos.stream()
                .filter(s -> s.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getCategoria().getId(),
                        Collectors.counting()
                ));

        return categorias.stream()
                .map(cat -> CategoriaPublicDTO.builder()
                        .id(cat.getId())
                        .label(cat.getLabel())
                        .value(cat.getValue())
                        .quantidadeServicos(servicosPorCategoria.getOrDefault(cat.getId(), 0L).intValue())
                        .build())
                .filter(cat -> cat.getQuantidadeServicos() > 0)
                .collect(Collectors.toList());
    }

    private List<CategoriaPublicDTO> convertCategoriasProdutos(List<Categoria> categorias) {
        return categorias.stream()
                .map(cat -> CategoriaPublicDTO.builder()
                        .id(cat.getId())
                        .label(cat.getLabel())
                        .value(cat.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private SiteConfigDTO buildSiteConfigDTO(Organizacao org) {
        return SiteConfigDTO.builder()
                .tema(org.getTema())
                .build();
    }

    private SeoMetadataDTO buildSeoMetadata(Organizacao org) {
        String nomeDisplay = org.getNomeFantasia() != null ? org.getNomeFantasia() : org.getRazaoSocial();

        return SeoMetadataDTO.builder()
                .title(nomeDisplay + " | Agende seu horário")
                .description("Agende seu horário com " + nomeDisplay + ". Serviços de qualidade com profissionais qualificados.")
                .keywords(nomeDisplay + ", agendamento, beleza, serviços")
                .ogTitle(nomeDisplay)
                .ogDescription("Conheça nossos serviços e agende online!")
                .canonicalUrl("https://app.bellory.com.br/" + org.getSlug())
                .build();
    }

    private FeaturesDTO buildFeatures(Organizacao org) {
        ConfigSistema config = org.getConfigSistema();

        return FeaturesDTO.builder()
                .agendamentoOnline(true)
                .ecommerce(config != null && config.isUsaEcommerce())
                .planosClientes(config != null && config.isUsaPlanosParaClientes())
                .avaliacoes(true)
                .chat(false)
                .notificacoesPush(config != null && config.isDisparaNotificacoesPush())
                .build();
    }

    private HomePageDTO.CustomAssetsDTO buildCustomAssets(SitePublicoConfig config) {
        List<HomePageDTO.ExternalScriptDTO> scripts = parseExternalScripts(config.getExternalScripts());

        return HomePageDTO.CustomAssetsDTO.builder()
                .customCss(config.getCustomCss())
                .customJs(config.getCustomJs())
                .externalScripts(scripts)
                .build();
    }

    private List<HorarioFuncionamentoDTO> buildHorariosFuncionamento(Long orgId) {
        List<HorarioFuncionamento> horarios = horarioFuncionamentoRepository
                .findByOrganizacaoIdWithPeriodos(orgId);

        Map<String, HorarioFuncionamentoDTO> horariosPorDia = new LinkedHashMap<>();

        String[] diasSemana = {"SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA", "SABADO", "DOMINGO"};
        for (String dia : diasSemana) {
            horariosPorDia.put(dia, HorarioFuncionamentoDTO.builder()
                    .diaSemana(dia)
                    .diaSemanaLabel(getDiaSemanaLabel(dia))
                    .aberto(false)
                    .build());
        }

        for (HorarioFuncionamento h : horarios) {
            String dia = h.getDiaSemana().name();
            HorarioFuncionamentoDTO dto = horariosPorDia.get(dia);
            if (dto == null) continue;

            boolean ativo = h.getAtivo() != null && h.getAtivo();
            dto.setAberto(ativo);

            if (ativo && h.getPeriodos() != null && !h.getPeriodos().isEmpty()) {
                String abertura = h.getPeriodos().stream()
                        .map(p -> p.getHoraInicio().toString())
                        .min(String::compareTo)
                        .orElse(null);
                String fechamento = h.getPeriodos().stream()
                        .map(p -> p.getHoraFim().toString())
                        .max(String::compareTo)
                        .orElse(null);
                dto.setHoraAbertura(abertura);
                dto.setHoraFechamento(fechamento);
            }
        }

        return new ArrayList<>(horariosPorDia.values());
    }

    // ==================== PARSE/HELPER METHODS ====================

    private List<HeaderConfigDTO.MenuItemDTO> parseMenuItems(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<HeaderConfigDTO.MenuItemDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<HeaderConfigDTO.ActionButtonDTO> parseActionButtons(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<HeaderConfigDTO.ActionButtonDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<HeroSectionDTO.HeroButtonDTO> parseHeroButtons(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<HeroSectionDTO.HeroButtonDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private HeroSectionDTO.StatsConfigDTO parseStatsConfig(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<HeroSectionDTO.StatsConfigDTO>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<AboutSectionDTO.HighlightDTO> parseHighlights(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<AboutSectionDTO.HighlightDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<FooterConfigDTO.LinkSectionDTO> parseLinkSections(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<FooterConfigDTO.LinkSectionDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseSectionsOrder(String json) {
        if (json == null || json.isEmpty()) {
            return List.of("HERO", "ABOUT", "SERVICES", "PRODUCTS", "TEAM", "BOOKING");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of("HERO", "ABOUT", "SERVICES", "PRODUCTS", "TEAM", "BOOKING");
        }
    }

    private Map<String, TransitionConfigRequest> parseTransitions(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, TransitionConfigRequest>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<HomePageDTO.ExternalScriptDTO> parseExternalScripts(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<HomePageDTO.ExternalScriptDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<HeaderConfigDTO.MenuItemDTO> buildDefaultMenuItems() {
        return List.of(
                HeaderConfigDTO.MenuItemDTO.builder().label("Início").href("/").order(1).build(),
                HeaderConfigDTO.MenuItemDTO.builder().label("Serviços").href("#servicos").order(2).build(),
                HeaderConfigDTO.MenuItemDTO.builder().label("Produtos").href("#produtos").order(3).build(),
                HeaderConfigDTO.MenuItemDTO.builder().label("Equipe").href("#equipe").order(4).build(),
                HeaderConfigDTO.MenuItemDTO.builder().label("Sobre").href("#sobre").order(5).build(),
                HeaderConfigDTO.MenuItemDTO.builder().label("Contato").href("#contato").order(6).build()
        );
    }

    private List<FooterConfigDTO.LinkSectionDTO> buildDefaultFooterLinks() {
        return List.of(
                FooterConfigDTO.LinkSectionDTO.builder()
                        .title("Links Rápidos")
                        .links(List.of(
                                FooterConfigDTO.LinkDTO.builder().label("Início").href("/").build(),
                                FooterConfigDTO.LinkDTO.builder().label("Serviços").href("#servicos").build(),
                                FooterConfigDTO.LinkDTO.builder().label("Produtos").href("#produtos").build(),
                                FooterConfigDTO.LinkDTO.builder().label("Agendar").href("#agendar").build()
                        ))
                        .build(),
                FooterConfigDTO.LinkSectionDTO.builder()
                        .title("Institucional")
                        .links(List.of(
                                FooterConfigDTO.LinkDTO.builder().label("Sobre Nós").href("/sobre").build(),
                                FooterConfigDTO.LinkDTO.builder().label("Nossa Equipe").href("#equipe").build()
                        ))
                        .build()
        );
    }

    private String formatAddress(Endereco endereco) {
        if (endereco == null) return null;

        StringBuilder sb = new StringBuilder();
        if (endereco.getLogradouro() != null) sb.append(endereco.getLogradouro());
        if (endereco.getNumero() != null) sb.append(", ").append(endereco.getNumero());
        if (endereco.getComplemento() != null && !endereco.getComplemento().isEmpty()) {
            sb.append(" - ").append(endereco.getComplemento());
        }
        if (endereco.getBairro() != null) sb.append(", ").append(endereco.getBairro());
        if (endereco.getCidade() != null) sb.append(" - ").append(endereco.getCidade());
        if (endereco.getUf() != null) sb.append("/").append(endereco.getUf());

        return sb.toString();
    }

    private String getDiaSemanaLabel(String dia) {
        return switch (dia) {
            case "SEGUNDA" -> "Segunda-feira";
            case "TERCA" -> "Terça-feira";
            case "QUARTA" -> "Quarta-feira";
            case "QUINTA" -> "Quinta-feira";
            case "SEXTA" -> "Sexta-feira";
            case "SABADO" -> "Sábado";
            case "DOMINGO" -> "Domingo";
            default -> dia;
        };
    }
}
