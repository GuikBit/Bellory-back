package org.exemplo.bellory.model.entity.landingpage.enums;

/**
 * Tipos de elementos disponíveis para uso nas seções.
 * Cada elemento tem propriedades específicas que serão armazenadas no JSON.
 */
public enum ElementType {

    // ==================== TEXTO ====================
    HEADING("Título", "heading", "Texto de título (h1-h6)"),
    PARAGRAPH("Parágrafo", "paragraph", "Bloco de texto"),
    TEXT("Texto", "text", "Texto inline"),
    RICH_TEXT("Texto Rico", "rich-text", "Texto com formatação HTML"),
    QUOTE("Citação", "quote", "Bloco de citação"),
    LIST("Lista", "list", "Lista ordenada ou não ordenada"),

    // ==================== MÍDIA ====================
    IMAGE("Imagem", "image", "Imagem estática"),
    VIDEO("Vídeo", "video", "Player de vídeo"),
    ICON("Ícone", "icon", "Ícone SVG ou fonte"),
    AVATAR("Avatar", "avatar", "Imagem circular de perfil"),
    LOGO("Logo", "logo", "Logotipo"),
    GALLERY("Galeria", "gallery", "Grade de imagens"),
    BACKGROUND("Background", "background", "Imagem/vídeo de fundo"),

    // ==================== INTERATIVOS ====================
    BUTTON("Botão", "button", "Botão de ação"),
    LINK("Link", "link", "Link de texto"),
    MENU("Menu", "menu", "Menu de navegação"),
    DROPDOWN("Dropdown", "dropdown", "Menu suspenso"),
    TABS("Abas", "tabs", "Navegação em abas"),
    ACCORDION("Acordeão", "accordion", "Conteúdo expansível"),
    MODAL_TRIGGER("Abrir Modal", "modal-trigger", "Botão que abre modal"),

    // ==================== FORMULÁRIO ====================
    FORM("Formulário", "form", "Formulário completo"),
    INPUT("Campo de Texto", "input", "Input de texto"),
    TEXTAREA("Área de Texto", "textarea", "Campo de texto multilinha"),
    SELECT("Seleção", "select", "Campo de seleção"),
    CHECKBOX("Checkbox", "checkbox", "Caixa de seleção"),
    RADIO("Radio", "radio", "Botão de opção"),
    DATE_PICKER("Data", "date-picker", "Seletor de data"),
    TIME_PICKER("Hora", "time-picker", "Seletor de hora"),
    FILE_UPLOAD("Upload", "file-upload", "Upload de arquivo"),

    // ==================== LAYOUT ====================
    CONTAINER("Container", "container", "Container de elementos"),
    ROW("Linha", "row", "Linha flexbox"),
    COLUMN("Coluna", "column", "Coluna do grid"),
    GRID("Grid", "grid", "Layout em grade"),
    FLEX("Flex", "flex", "Layout flexbox"),
    SPACER("Espaçador", "spacer", "Espaço em branco"),
    DIVIDER("Divisor", "divider", "Linha divisória"),
    SECTION("Seção", "section", "Wrapper de seção"),

    // ==================== CARDS ====================
    CARD("Card", "card", "Card genérico"),
    SERVICE_CARD("Card de Serviço", "service-card", "Card para serviço"),
    PRODUCT_CARD("Card de Produto", "product-card", "Card para produto"),
    TEAM_CARD("Card de Membro", "team-card", "Card para membro da equipe"),
    TESTIMONIAL_CARD("Card de Depoimento", "testimonial-card", "Card para depoimento"),
    PRICING_CARD("Card de Plano", "pricing-card", "Card para plano/preço"),
    FEATURE_CARD("Card de Recurso", "feature-card", "Card para feature"),

    // ==================== DADOS DINÂMICOS ====================
    SERVICE_LIST("Lista de Serviços", "service-list", "Lista dinâmica de serviços"),
    PRODUCT_LIST("Lista de Produtos", "product-list", "Lista dinâmica de produtos"),
    TEAM_LIST("Lista de Equipe", "team-list", "Lista dinâmica de membros"),
    TESTIMONIAL_LIST("Lista de Depoimentos", "testimonial-list", "Lista dinâmica de depoimentos"),
    BOOKING_FORM("Formulário de Agendamento", "booking-form", "Formulário de agendamento integrado"),
    CONTACT_FORM("Formulário de Contato", "contact-form", "Formulário de contato integrado"),

    // ==================== WIDGETS ====================
    SOCIAL_LINKS("Redes Sociais", "social-links", "Links de redes sociais"),
    RATING("Avaliação", "rating", "Estrelas de avaliação"),
    BADGE("Badge", "badge", "Etiqueta/selo"),
    TAG("Tag", "tag", "Tag/label"),
    PROGRESS("Progresso", "progress", "Barra de progresso"),
    COUNTER("Contador", "counter", "Número animado"),
    COUNTDOWN("Contagem Regressiva", "countdown", "Timer de contagem"),
    MAP("Mapa", "map", "Mapa integrado"),
    WHATSAPP_BUTTON("WhatsApp", "whatsapp-button", "Botão de WhatsApp"),

    // ==================== ESPECIAIS ====================
    CAROUSEL("Carrossel", "carousel", "Carrossel/slider"),
    SLIDER("Slider", "slider", "Slider de imagens"),
    MARQUEE("Marquee", "marquee", "Texto/imagens em movimento"),
    EMBED("Embed", "embed", "Conteúdo incorporado"),
    HTML("HTML", "html", "Código HTML customizado"),
    SCRIPT("Script", "script", "JavaScript customizado");

    private final String label;
    private final String type;
    private final String description;

    ElementType(String label, String type, String description) {
        this.label = label;
        this.type = type;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getType() { return type; }
    public String getDescription() { return description; }
}
