package org.exemplo.bellory.model.entity.landingpage.enums;

/**
 * Tipos de seções disponíveis para Landing Pages.
 */
public enum SectionType {
    // Estruturais
    HEADER("Header/Menu", "Barra de navegação superior", "menu"),
    FOOTER("Rodapé", "Rodapé com informações de contato", "footer"),

    // Conteúdo Principal
    HERO("Apresentação", "Banner principal de destaque", "presentation"),
    ABOUT("Sobre", "Informações sobre a empresa", "about"),

    // Catálogo
    SERVICES("Serviços", "Lista de serviços oferecidos", "services"),
    PRODUCTS("Produtos", "Lista de produtos", "products"),
    PRICING("Planos/Preços", "Tabela de preços e planos", "pricing"),

    // Social Proof
    TEAM("Equipe", "Membros da equipe", "team"),
    TESTIMONIALS("Depoimentos", "Depoimentos de clientes", "testimonials"),
    GALLERY("Galeria", "Galeria de imagens/vídeos", "gallery"),

    // Conversão
    CTA("Chamada para Ação", "Seção de call-to-action", "cta"),
    BOOKING("Agendamento", "Formulário de agendamento", "booking"),
    CONTACT("Contato", "Formulário de contato", "contact"),
    NEWSLETTER("Newsletter", "Inscrição em newsletter", "newsletter"),

    // Informacional
    FAQ("FAQ", "Perguntas frequentes", "faq"),
    FEATURES("Recursos", "Lista de recursos/funcionalidades", "features"),
    STATS("Estatísticas", "Números e métricas", "stats"),
    TIMELINE("Linha do Tempo", "Histórico/cronologia", "timeline"),

    // Mídia
    VIDEO("Vídeo", "Seção com vídeo", "video"),
    MAP("Mapa", "Mapa de localização", "map"),

    // Especiais
    CAROUSEL("Carrossel", "Carrossel de conteúdo", "carousel"),
    TABS("Abas", "Conteúdo em abas", "tabs"),
    ACCORDION("Acordeão", "Conteúdo expansível", "accordion"),

    // Flexível
    CUSTOM("Personalizado", "Seção totalmente customizada", "custom"),
    HTML("HTML Livre", "HTML/CSS customizado", "html");

    private final String label;
    private final String description;
    private final String icon;

    SectionType(String label, String description, String icon) {
        this.label = label;
        this.description = description;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
}
