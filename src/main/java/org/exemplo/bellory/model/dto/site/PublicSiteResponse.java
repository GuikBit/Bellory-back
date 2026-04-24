package org.exemplo.bellory.model.dto.site;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicSiteResponse<T> {

    private boolean siteAtivo;
    private ModoSite modo;
    private T conteudo;

    public static <T> PublicSiteResponse<T> ativo(ModoSite modo, T conteudo) {
        return PublicSiteResponse.<T>builder()
                .siteAtivo(true)
                .modo(modo)
                .conteudo(conteudo)
                .build();
    }

    public static <T> PublicSiteResponse<T> inativo() {
        return PublicSiteResponse.<T>builder()
                .siteAtivo(false)
                .modo(null)
                .conteudo(null)
                .build();
    }
}
