package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicSiteResponseDTO {

    // ========== INFORMAÇÕES DA ORGANIZAÇÃO ==========
    private OrganizacaoPublicDTO organizacao;

    // ========== CONFIGURAÇÕES DO SITE ==========
    private SiteConfigDTO siteConfig;

    // ========== EQUIPE / FUNCIONÁRIOS ==========
    private List<FuncionarioPublicDTO> equipe;

    // ========== SERVIÇOS ==========
    private List<ServicoPublicDTO> servicos;

    // ========== CATEGORIAS DE SERVIÇOS ==========
    private List<CategoriaPublicDTO> categorias;

    // ========== PRODUTOS (se habilitado) ==========
    private List<ProdutoPublicDTO> produtosDestaque;

    // ========== HORÁRIOS DE FUNCIONAMENTO ==========
    private List<HorarioFuncionamentoDTO> horariosFuncionamento;

    // ========== REDES SOCIAIS ==========
    private RedesSociaisDTO redesSociais;

    // ========== METADATA SEO ==========
    private SeoMetadataDTO seo;

    // ========== FLAGS DE FUNCIONALIDADES ==========
    private FeaturesDTO features;
}



