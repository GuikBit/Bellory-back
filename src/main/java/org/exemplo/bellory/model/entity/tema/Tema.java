package org.exemplo.bellory.model.entity.tema;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Tema {
    // Removido o campo 'id' - Embeddables não devem ter ID próprio
    // O ID da organização já identifica o tema

    @Column(name = "tema_nome", length = 100)
    private String nome;

    @Column(name = "tema_tipo", length = 50)
    private String tipo;

    // Cores aninhadas - precisa de AttributeOverrides para evitar conflitos
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "primary", column = @Column(name = "tema_cor_primary")),
            @AttributeOverride(name = "secondary", column = @Column(name = "tema_cor_secondary")),
            @AttributeOverride(name = "accent", column = @Column(name = "tema_cor_accent")),
            @AttributeOverride(name = "background", column = @Column(name = "tema_cor_background")),
            @AttributeOverride(name = "text", column = @Column(name = "tema_cor_text")),
            @AttributeOverride(name = "textSecondary", column = @Column(name = "tema_cor_text_secondary")),
            @AttributeOverride(name = "cardBackground", column = @Column(name = "tema_cor_card_background")),
            @AttributeOverride(name = "cardBackgroundSecondary", column = @Column(name = "tema_cor_card_background_secondary")),
            @AttributeOverride(name = "buttonText", column = @Column(name = "tema_cor_button_text")),
            @AttributeOverride(name = "backgroundLinear", column = @Column(name = "tema_cor_background_linear")),
            @AttributeOverride(name = "success", column = @Column(name = "tema_cor_success")),
            @AttributeOverride(name = "warning", column = @Column(name = "tema_cor_warning")),
            @AttributeOverride(name = "error", column = @Column(name = "tema_cor_error")),
            @AttributeOverride(name = "info", column = @Column(name = "tema_cor_info")),
            @AttributeOverride(name = "border", column = @Column(name = "tema_cor_border")),
            @AttributeOverride(name = "borderLight", column = @Column(name = "tema_cor_border_light")),
            @AttributeOverride(name = "divider", column = @Column(name = "tema_cor_divider")),
            @AttributeOverride(name = "overlay", column = @Column(name = "tema_cor_overlay")),
            @AttributeOverride(name = "modalBackground", column = @Column(name = "tema_cor_modal_background")),
            @AttributeOverride(name = "inputBackground", column = @Column(name = "tema_cor_input_background")),
            @AttributeOverride(name = "inputBorder", column = @Column(name = "tema_cor_input_border")),
            @AttributeOverride(name = "inputFocus", column = @Column(name = "tema_cor_input_focus")),
            @AttributeOverride(name = "placeholder", column = @Column(name = "tema_cor_placeholder")),
            @AttributeOverride(name = "navBackground", column = @Column(name = "tema_cor_nav_background")),
            @AttributeOverride(name = "navHover", column = @Column(name = "tema_cor_nav_hover")),
            @AttributeOverride(name = "navActive", column = @Column(name = "tema_cor_nav_active")),
            @AttributeOverride(name = "online", column = @Column(name = "tema_cor_online")),
            @AttributeOverride(name = "offline", column = @Column(name = "tema_cor_offline")),
            @AttributeOverride(name = "away", column = @Column(name = "tema_cor_away")),
            @AttributeOverride(name = "busy", column = @Column(name = "tema_cor_busy"))
    })
    private Cores cores;

    // Fonts aninhadas
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "heading", column = @Column(name = "tema_font_heading")),
            @AttributeOverride(name = "body", column = @Column(name = "tema_font_body")),
            @AttributeOverride(name = "mono", column = @Column(name = "tema_font_mono"))
    })
    private Fonts fonts;

    // BorderRadius aninhado
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "small", column = @Column(name = "tema_border_radius_small")),
            @AttributeOverride(name = "medium", column = @Column(name = "tema_border_radius_medium")),
            @AttributeOverride(name = "large", column = @Column(name = "tema_border_radius_large")),
            @AttributeOverride(name = "xl", column = @Column(name = "tema_border_radius_xl")),
            @AttributeOverride(name = "full", column = @Column(name = "tema_border_radius_full"))
    })
    private BorderRadius borderRadius;

    // Shadows aninhadas
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "tema_shadow_base")),
            @AttributeOverride(name = "md", column = @Column(name = "tema_shadow_md")),
            @AttributeOverride(name = "lg", column = @Column(name = "tema_shadow_lg")),
            @AttributeOverride(name = "primaryGlow", column = @Column(name = "tema_shadow_primary_glow")),
            @AttributeOverride(name = "accentGlow", column = @Column(name = "tema_shadow_accent_glow"))
    })
    private Shadows shadows;
}
