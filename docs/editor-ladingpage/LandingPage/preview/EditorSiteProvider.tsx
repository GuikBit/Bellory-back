import { useMemo, useCallback, useEffect, useRef, type ReactNode } from 'react';
import { SiteContext } from '../../../../../global/SiteContext';
import type { SiteContextData } from '../../../../../global/SiteContext';
import { useTheme } from '../../../../../global/Theme-context';
import { useEditorSiteData } from '../../../../../hooks/useEditorSiteData';
import { useEditorState } from '../../../../../stores/editorStore';
import { CartProvider } from '../../../../Externo/Carrinho/CartContext';
import type {
  Servico,
  Produto,
  MembroEquipe,
  Categoria,
  SectionType,
} from '../../../../../types/site.types';
import type {
  HeroSectionSettings,
  AboutSectionSettings,
  ServicesSectionSettings,
  ProductsSectionSettings,
  TeamSectionSettings,
  HeaderSectionSettings,
  FooterSectionSettings,
  BookingSectionSettings,
} from '../../../../../types/section-settings.types';

// Mapeamento de tema name → ID (mesmo do SiteContext)
const themeNameToId: Record<string, string> = {
  "Tema Elegante": "femininoElegante",
  "Masculino Default": "masculine_default",
  "Masculino Moderno": "masculinoModerno",
  "Masculino Classico": "masculinoClassico",
  "Masculino Clássico": "masculinoClassico",
  "Feminino Elegante": "femininoElegante",
  "Feminino Moderno": "femininoModerno",
  "Bellory Elegante": "belloryElegante",
};

const validThemeIds = new Set([
  "masculine_default",
  "masculinoModerno",
  "masculinoClassico",
  "femininoElegante",
  "femininoModerno",
  "belloryElegante",
]);

function resolveThemeId(themeNameOrId: string): string {
  if (validThemeIds.has(themeNameOrId)) return themeNameOrId;
  return themeNameToId[themeNameOrId] ?? "femininoElegante";
}

interface EditorSiteProviderProps {
  children: ReactNode;
}

export function EditorSiteProvider({ children }: EditorSiteProviderProps) {
  const { homeData, isLoading, isError, error } = useEditorSiteData();
  const { landingPage } = useEditorState();
  const { setTheme, applyCustomColors, currentTheme } = useTheme();
  const previousThemeRef = useRef<string | null>(null);

  // Aplica tema do site no mount e restaura tema admin no unmount
  useEffect(() => {
    if (!homeData?.siteConfig?.tema?.nome) return;

    // Salva tema atual do admin
    previousThemeRef.current = currentTheme.id;

    const themeId = resolveThemeId(homeData.siteConfig.tema.nome);
    setTheme(themeId);

    if (homeData.siteConfig.tema.cores) {
      applyCustomColors(homeData.siteConfig.tema.cores as unknown as Record<string, string>);
    }

    return () => {
      // Restaura tema do admin
      if (previousThemeRef.current) {
        setTheme(previousThemeRef.current);
      }
    };
  }, [homeData?.siteConfig?.tema]);

  // Helper para buscar settings de uma seção pelo tipo
  const getSectionSettings = useCallback((tipo: string): Record<string, unknown> => {
    if (!landingPage?.sections) return {};
    const section = landingPage.sections.find(s => s.tipo === tipo && s.visible !== false);
    return (section?.settings as Record<string, unknown>) ?? {};
  }, [landingPage?.sections]);

  // === Helpers ===
  const getServico = useCallback((id: number): Servico | undefined => {
    return homeData?.services?.servicos?.find(s => s.id === id);
  }, [homeData]);

  const getProduto = useCallback((id: number): Produto | undefined => {
    return homeData?.products?.produtos?.find(p => p.id === id);
  }, [homeData]);

  const getMembro = useCallback((id: number): MembroEquipe | undefined => {
    return homeData?.team?.membros?.find(m => m.id === id);
  }, [homeData]);

  const getCategoria = useCallback((id: number): Categoria | undefined => {
    return homeData?.services?.categorias?.find(c => c.id === id);
  }, [homeData]);

  const getServicosByCategoria = useCallback((categoriaId: number): Servico[] => {
    return homeData?.services?.servicos?.filter(s => s.categoriaId === categoriaId) ?? [];
  }, [homeData]);

  const getProdutosByCategoria = useCallback((categoria: string): Produto[] => {
    return homeData?.products?.produtos?.filter(p => p.categoria === categoria) ?? [];
  }, [homeData]);

  // No-op navigation stubs (editor context)
  const navigateTo = useCallback((_path: string) => {
    // No-op inside editor
  }, []);

  const scrollToSection = useCallback((_sectionId: string) => {
    // No-op inside editor
  }, []);

  // Determina sectionsOrder a partir das seções visíveis e ordenadas do editor
  const sectionsOrder = useMemo<SectionType[]>(() => {
    if (!landingPage?.sections) {
      return homeData?.sectionsOrder ?? ['HERO', 'ABOUT', 'SERVICES', 'PRODUCTS', 'TEAM', 'BOOKING'];
    }
    return [...landingPage.sections]
      .filter(s => s.visible)
      .sort((a, b) => a.order - b.order)
      .map(s => s.tipo as SectionType);
  }, [landingPage?.sections, homeData?.sectionsOrder]);

  // Merge de dados reais + overrides do editor
  const contextValue = useMemo<SiteContextData>(() => {
    if (!homeData) {
      return {
        isLoading,
        isError,
        error,
        slug: '',
        organizacao: null,
        siteConfig: null,
        header: null,
        hero: null,
        about: null,
        services: null,
        products: null,
        team: null,
        booking: null,
        footer: null,
        sectionsOrder,
        seo: null,
        features: null,
        getServico,
        getProduto,
        getMembro,
        getCategoria,
        getServicosByCategoria,
        getProdutosByCategoria,
        navigateTo,
        scrollToSection,
        cores: null,
      };
    }

    // Merge hero settings
    const heroSettings = getSectionSettings('HERO') as HeroSectionSettings;
    const mergedHero = homeData.hero ? {
      ...homeData.hero,
      ...(heroSettings.titulo && { title: heroSettings.titulo }),
      ...(heroSettings.subtitulo && { subtitle: heroSettings.subtitulo }),
      ...(heroSettings.backgroundImage && { backgroundImage: heroSettings.backgroundImage }),
      ...(heroSettings.backgroundOverlay !== undefined && { backgroundOverlay: heroSettings.backgroundOverlay }),
      ...(heroSettings.buttons && { buttons: heroSettings.buttons }),
      ...(heroSettings.showStats !== undefined && { showBookingForm: heroSettings.showStats }),
      ...(heroSettings.stats && {
        stats: { ...homeData.hero.stats, ...heroSettings.stats },
      }),
    } : null;

    // Merge about settings
    const aboutSettings = getSectionSettings('ABOUT') as AboutSectionSettings;
    const mergedAbout = homeData.about ? {
      ...homeData.about,
      ...(aboutSettings.titulo && { title: aboutSettings.titulo }),
      ...(aboutSettings.descricao && { description: aboutSettings.descricao }),
      ...(aboutSettings.imagem && { image: aboutSettings.imagem }),
      ...(aboutSettings.videoUrl && { videoUrl: aboutSettings.videoUrl }),
      ...(aboutSettings.highlights && { highlights: aboutSettings.highlights }),
      ...(aboutSettings.missao && { mission: aboutSettings.missao }),
      ...(aboutSettings.visao && { vision: aboutSettings.visao }),
      ...(aboutSettings.valores && { values: aboutSettings.valores }),
    } : null;

    // Merge services settings
    const servicesSettings = getSectionSettings('SERVICES') as ServicesSectionSettings;
    const mergedServices = homeData.services ? (() => {
      let servicos = homeData.services.servicos;
      if (servicesSettings.categoryFilter?.length) {
        servicos = servicos.filter(s => servicesSettings.categoryFilter!.includes(s.categoriaId));
      }
      if (servicesSettings.maxItems) {
        servicos = servicos.slice(0, servicesSettings.maxItems);
      }
      return {
        ...homeData.services,
        ...(servicesSettings.titulo && { title: servicesSettings.titulo }),
        ...(servicesSettings.subtitulo && { subtitle: servicesSettings.subtitulo }),
        ...(servicesSettings.showPrices !== undefined && { showPrices: servicesSettings.showPrices }),
        ...(servicesSettings.showDuration !== undefined && { showDuration: servicesSettings.showDuration }),
        servicos,
      };
    })() : null;

    // Merge products settings
    const productsSettings = getSectionSettings('PRODUCTS') as ProductsSectionSettings;
    const mergedProducts = homeData.products ? (() => {
      let produtos = homeData.products.produtos;
      if (productsSettings.maxItems) {
        produtos = produtos.slice(0, productsSettings.maxItems);
      }
      return {
        ...homeData.products,
        ...(productsSettings.titulo && { title: productsSettings.titulo }),
        ...(productsSettings.subtitulo && { subtitle: productsSettings.subtitulo }),
        ...(productsSettings.showPrices !== undefined && { showPrices: productsSettings.showPrices }),
        produtos,
      };
    })() : null;

    // Merge team settings
    const teamSettings = getSectionSettings('TEAM') as TeamSectionSettings;
    const mergedTeam = homeData.team ? (() => {
      let membros = homeData.team.membros;
      if (teamSettings.memberIds?.length) {
        membros = membros.filter(m => teamSettings.memberIds!.includes(m.id));
      }
      return {
        ...homeData.team,
        ...(teamSettings.titulo && { title: teamSettings.titulo }),
        ...(teamSettings.subtitulo && { subtitle: teamSettings.subtitulo }),
        membros,
      };
    })() : null;

    // Merge header settings
    const headerSettings = getSectionSettings('HEADER') as HeaderSectionSettings;
    const mergedHeader = homeData.header ? {
      ...homeData.header,
      ...(headerSettings.logoUrl && { logoUrl: headerSettings.logoUrl }),
      ...(headerSettings.menuItems && { menuItems: headerSettings.menuItems }),
      ...(headerSettings.actionButtons && { actionButtons: headerSettings.actionButtons }),
      ...(headerSettings.showPhone !== undefined && { showPhone: headerSettings.showPhone }),
      ...(headerSettings.showSocial !== undefined && { showSocial: headerSettings.showSocial }),
      ...(headerSettings.socialLinks && { socialLinks: headerSettings.socialLinks }),
      ...(headerSettings.sticky !== undefined && { sticky: headerSettings.sticky }),
    } : null;

    // Merge footer settings
    const footerSettings = getSectionSettings('FOOTER') as FooterSectionSettings;
    const mergedFooter = homeData.footer ? {
      ...homeData.footer,
      ...(footerSettings.copyrightText && { copyrightText: footerSettings.copyrightText }),
      ...(footerSettings.showHours !== undefined && { showHours: footerSettings.showHours }),
      ...(footerSettings.showSocial !== undefined && { showSocial: footerSettings.showSocial }),
      ...(footerSettings.showNewsletter !== undefined && { showNewsletter: footerSettings.showNewsletter }),
      ...(footerSettings.linkSections && { linkSections: footerSettings.linkSections }),
    } : null;

    // Merge booking settings
    const bookingSettings = getSectionSettings('BOOKING') as BookingSectionSettings;
    const mergedBooking = homeData.booking ? {
      ...homeData.booking,
      ...(bookingSettings.titulo && { title: bookingSettings.titulo }),
      ...(bookingSettings.subtitulo && { subtitle: bookingSettings.subtitulo }),
      ...(bookingSettings.enabled !== undefined && { enabled: bookingSettings.enabled }),
    } : null;

    return {
      isLoading: false,
      isError: false,
      error: null,
      slug: homeData.organizacao.slug,
      organizacao: homeData.organizacao,
      siteConfig: homeData.siteConfig,
      header: mergedHeader,
      hero: mergedHero,
      about: mergedAbout,
      services: mergedServices,
      products: mergedProducts,
      team: mergedTeam,
      booking: mergedBooking,
      footer: mergedFooter,
      sectionsOrder,
      seo: homeData.seo,
      features: homeData.features,
      getServico,
      getProduto,
      getMembro,
      getCategoria,
      getServicosByCategoria,
      getProdutosByCategoria,
      navigateTo,
      scrollToSection,
      cores: homeData.siteConfig?.tema?.cores ?? null,
    };
  }, [
    homeData,
    isLoading,
    isError,
    error,
    sectionsOrder,
    getSectionSettings,
    getServico,
    getProduto,
    getMembro,
    getCategoria,
    getServicosByCategoria,
    getProdutosByCategoria,
    navigateTo,
    scrollToSection,
  ]);

  return (
    <SiteContext.Provider value={contextValue}>
      <CartProvider>
        {children}
      </CartProvider>
    </SiteContext.Provider>
  );
}
