package org.exemplo.bellory.model.dto.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.tenent.FuncionarioPublicDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO com detalhes completos de um serviço para a página dedicada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServicoDetalhadoDTO {

    private Long id;
    private String nome;
    private String descricao;
    private String descricaoCompleta;

    // ==================== CATEGORIA ====================
    private Long categoriaId;
    private String categoriaNome;

    // ==================== PREÇOS ====================
    private BigDecimal preco;
    private BigDecimal precoComDesconto;
    private BigDecimal descontoPercentual;

    // ==================== DETALHES ====================
    private String genero;
    private Integer tempoEstimadoMinutos;
    private Boolean disponivel;

    // ==================== IMAGENS ====================
    private List<String> imagens;
    private String imagemPrincipal;

    // ==================== PROFISSIONAIS ====================
    private List<FuncionarioPublicDTO> profissionais;

    // ==================== PRODUTOS UTILIZADOS ====================
    private List<ProdutoResumidoDTO> produtosUtilizados;

    // ==================== AGENDAMENTO ====================
    private Boolean permiteAgendamentoOnline;
    private Boolean requerSinal;
    private BigDecimal percentualSinal;

    // ==================== SEO ====================
    private ProdutoDetalhadoDTO.SeoDataDTO seo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoResumidoDTO {
        private Long id;
        private String nome;
        private String imagemUrl;
    }
}
