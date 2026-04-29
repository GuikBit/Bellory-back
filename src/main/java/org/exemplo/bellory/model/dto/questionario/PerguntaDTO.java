package org.exemplo.bellory.model.dto.questionario;

import lombok.*;
import org.exemplo.bellory.model.entity.questionario.Pergunta;
import org.exemplo.bellory.model.entity.questionario.enums.FormatoAssinatura;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;
import org.exemplo.bellory.model.entity.questionario.enums.TipoTemplateTermo;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerguntaDTO {

    private Long id;
    private String texto;
    private String descricao;
    private TipoPergunta tipo;
    private Boolean obrigatoria;
    private Integer ordem;
    private List<OpcaoRespostaDTO> opcoes;

    // Campos para tipo ESCALA
    private Integer escalaMin;
    private Integer escalaMax;
    private String labelMin;
    private String labelMax;

    // Campos para validação
    private Integer minCaracteres;
    private Integer maxCaracteres;
    private BigDecimal minValor;
    private BigDecimal maxValor;

    // Campos para tipo TERMO_CONSENTIMENTO
    private String textoTermo;
    private TipoTemplateTermo templateTermoId;
    private Boolean requerAceiteExplicito;

    /**
     * Texto do termo com placeholders ja substituidos pelo servidor.
     * Populado APENAS quando o GET de questionario recebe os IDs de cliente/
     * agendamento/funcionario na query string. Caso contrario, fica null e o
     * front recebe somente {@code textoTermo} cru.
     */
    private String textoTermoRenderizado;

    // Campos para tipo ASSINATURA
    private FormatoAssinatura formatoAssinatura;
    private Integer larguraAssinatura;
    private Integer alturaAssinatura;
    private Boolean exigirAssinaturaProfissional;

    public PerguntaDTO(Pergunta entity) {
        this.id = entity.getId();
        this.texto = entity.getTexto();
        this.descricao = entity.getDescricao();
        this.tipo = entity.getTipo();
        this.obrigatoria = entity.getObrigatoria();
        this.ordem = entity.getOrdem();
        this.escalaMin = entity.getEscalaMin();
        this.escalaMax = entity.getEscalaMax();
        this.labelMin = entity.getLabelMin();
        this.labelMax = entity.getLabelMax();
        this.minCaracteres = entity.getMinCaracteres();
        this.maxCaracteres = entity.getMaxCaracteres();
        this.minValor = entity.getMinValor();
        this.maxValor = entity.getMaxValor();
        this.textoTermo = entity.getTextoTermo();
        this.templateTermoId = entity.getTemplateTermoId();
        this.requerAceiteExplicito = entity.getRequerAceiteExplicito();
        this.formatoAssinatura = entity.getFormatoAssinatura();
        this.larguraAssinatura = entity.getLarguraAssinatura();
        this.alturaAssinatura = entity.getAlturaAssinatura();
        this.exigirAssinaturaProfissional = entity.getExigirAssinaturaProfissional();

        if (entity.getOpcoes() != null && !entity.getOpcoes().isEmpty()) {
            this.opcoes = entity.getOpcoes().stream()
                    .map(OpcaoRespostaDTO::new)
                    .collect(Collectors.toList());
        }
    }
}
