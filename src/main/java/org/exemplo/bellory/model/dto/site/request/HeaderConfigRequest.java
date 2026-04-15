package org.exemplo.bellory.model.dto.site.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.site.HeaderConfigDTO;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeaderConfigRequest {

    private String logoUrl;
    private String logoAlt;
    private List<HeaderConfigDTO.MenuItemDTO> menuItems;
    private List<HeaderConfigDTO.ActionButtonDTO> actionButtons;
    private Boolean showPhone;
    private Boolean showSocial;
    private Boolean sticky;
    private String headerLayout;
    private String menuStyle;
    private Boolean transparentOnHero;
    private Boolean showCart;
    private Integer logoHeight;
    private String backgroundColor;
    private String backgroundPattern;
    private Double patternOpacity;
}
