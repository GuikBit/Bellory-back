package org.exemplo.bellory.service.questionario;

import org.exemplo.bellory.model.dto.questionario.TemplateTermoDTO;
import org.exemplo.bellory.model.entity.questionario.enums.TipoTemplateTermo;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Catalogo estatico de templates de termo de consentimento.
 *
 * Em fase 1 nao ha tabela de templates: o catalogo e populado in-memory na construcao
 * do componente. Customizacao por organizacao (CRUD) fica para fase 2.
 *
 * Os templates sao escritos em Markdown com placeholders {{variavel}} que devem ser
 * substituidos pelo front antes do submit. O servidor congela o resultado em
 * {@code RespostaPergunta.textoTermoRenderizado} e calcula o {@code hashTermo}.
 */
@Component
public class TemplateTermoCatalog {

    private final Map<TipoTemplateTermo, TemplateTermoDTO> templates;

    public TemplateTermoCatalog() {
        Map<TipoTemplateTermo, TemplateTermoDTO> map = new LinkedHashMap<>();

        map.put(TipoTemplateTermo.PADRAO_BELLORY, TemplateTermoDTO.builder()
                .id(TipoTemplateTermo.PADRAO_BELLORY)
                .nome("Termo Padrão Bellory")
                .descricao("Termo genérico de aceite e ciência fornecido pela plataforma")
                .conteudo(conteudoPadraoBellory())
                .variaveis(List.of("nomeCliente", "cpfCliente", "nomeEstabelecimento"))
                .editavel(false)
                .build());

        map.put(TipoTemplateTermo.PADRAO_PROCEDIMENTO, TemplateTermoDTO.builder()
                .id(TipoTemplateTermo.PADRAO_PROCEDIMENTO)
                .nome("Termo de Responsabilidade e Consentimento (Procedimento)")
                .descricao("Template padrão para procedimentos estéticos")
                .conteudo(conteudoPadraoProcedimento())
                .variaveis(List.of(
                        "nomeCliente", "cpfCliente", "nomeProfissional",
                        "nomeEstabelecimento", "nomeProcedimento", "dataAtendimento"))
                .editavel(false)
                .build());

        map.put(TipoTemplateTermo.PADRAO_PROCEDIMENTO_QUIMICO, TemplateTermoDTO.builder()
                .id(TipoTemplateTermo.PADRAO_PROCEDIMENTO_QUIMICO)
                .nome("Termo de Ciência de Riscos (Procedimento Químico)")
                .descricao("Template para procedimentos químicos com declaração de ciência de riscos")
                .conteudo(conteudoPadraoProcedimentoQuimico())
                .variaveis(List.of(
                        "nomeCliente", "cpfCliente", "nomeProfissional",
                        "nomeEstabelecimento", "nomeProcedimento", "dataAtendimento"))
                .editavel(false)
                .build());

        map.put(TipoTemplateTermo.PADRAO_USO_IMAGEM, TemplateTermoDTO.builder()
                .id(TipoTemplateTermo.PADRAO_USO_IMAGEM)
                .nome("Autorização de Uso de Imagem")
                .descricao("Opt-in para uso de fotos antes/depois em redes sociais e materiais de divulgação")
                .conteudo(conteudoPadraoUsoImagem())
                .variaveis(List.of("nomeCliente", "cpfCliente", "nomeEstabelecimento"))
                .editavel(false)
                .build());

        this.templates = Collections.unmodifiableMap(map);
    }

    public List<TemplateTermoDTO> listar() {
        return templates.values().stream().collect(Collectors.toList());
    }

    public Optional<TemplateTermoDTO> buscarPorId(TipoTemplateTermo id) {
        return Optional.ofNullable(templates.get(id));
    }

    private String conteudoPadraoBellory() {
        return """
                **TERMO DE ACEITE E CIÊNCIA**

                Eu, **{{nomeCliente}}**, portador(a) do CPF **{{cpfCliente}}**, declaro estar ciente e de acordo com os termos descritos abaixo, prestados pelo estabelecimento **{{nomeEstabelecimento}}**.

                Confirmo que as informações fornecidas são verdadeiras e que estou de acordo com as condições de atendimento.

                Ao assinar, declaro ter lido e compreendido todas as informações deste termo.
                """;
    }

    private String conteudoPadraoProcedimento() {
        return """
                **TERMO DE RESPONSABILIDADE E CONSENTIMENTO**

                Eu, **{{nomeCliente}}**, portador(a) do CPF **{{cpfCliente}}**, autorizo a realização do procedimento **{{nomeProcedimento}}** pelo(a) profissional **{{nomeProfissional}}** no estabelecimento **{{nomeEstabelecimento}}** na data **{{dataAtendimento}}**.

                Declaro que:

                1. Fui devidamente informado(a) sobre o procedimento, suas etapas, cuidados pré e pós-atendimento.
                2. Tive a oportunidade de esclarecer todas as minhas dúvidas antes de assinar este termo.
                3. As informações que prestei sobre meu histórico de saúde, alergias e medicamentos em uso são verdadeiras.
                4. Comprometo-me a seguir as orientações pós-procedimento fornecidas pelo(a) profissional.
                5. Estou ciente de que resultados podem variar de pessoa para pessoa.

                Assumo a responsabilidade pelas informações prestadas e autorizo a realização do procedimento.
                """;
    }

    private String conteudoPadraoProcedimentoQuimico() {
        return """
                **TERMO DE CIÊNCIA DE RISCOS — PROCEDIMENTO QUÍMICO**

                Eu, **{{nomeCliente}}**, portador(a) do CPF **{{cpfCliente}}**, autorizo a realização do procedimento químico **{{nomeProcedimento}}** pelo(a) profissional **{{nomeProfissional}}** no estabelecimento **{{nomeEstabelecimento}}** na data **{{dataAtendimento}}**.

                **Declaração de ciência de riscos:**

                Declaro estar ciente de que procedimentos químicos podem apresentar reações adversas, incluindo (mas não se limitando a):

                - Irritação, vermelhidão ou ardência na área aplicada
                - Reações alérgicas
                - Alterações temporárias ou permanentes na cor, textura ou aspecto
                - Necessidade de cuidados específicos pós-procedimento

                **Declaro também que:**

                1. Informei corretamente meu histórico de saúde, alergias e medicamentos em uso.
                2. Não estou gestante, lactante ou em condição que contraindique este procedimento (quando aplicável).
                3. Tive a oportunidade de esclarecer todas as minhas dúvidas.
                4. Comprometo-me a seguir rigorosamente as orientações pós-procedimento.
                5. Isento o(a) profissional e o estabelecimento de responsabilidade por reações decorrentes de informações omitidas ou incorretas que eu tenha prestado.

                Assino este termo de livre e espontânea vontade, ciente das informações acima.
                """;
    }

    private String conteudoPadraoUsoImagem() {
        return """
                **AUTORIZAÇÃO DE USO DE IMAGEM**

                Eu, **{{nomeCliente}}**, portador(a) do CPF **{{cpfCliente}}**, **autorizo** o estabelecimento **{{nomeEstabelecimento}}** a utilizar fotografias, vídeos e demais imagens do antes/durante/depois do meu atendimento para os seguintes fins:

                - Divulgação em redes sociais (Instagram, Facebook, TikTok, etc.)
                - Materiais de divulgação impressos e digitais
                - Portfólio do(a) profissional e do estabelecimento
                - Site institucional

                **Declaro que:**

                1. Esta autorização é concedida sem qualquer ônus ou contraprestação financeira.
                2. As imagens não serão utilizadas para fins difamatórios, discriminatórios ou que firam minha honra.
                3. Posso revogar esta autorização a qualquer momento, mediante solicitação por escrito ao estabelecimento.

                Esta autorização é facultativa e não condiciona a realização do atendimento.
                """;
    }
}
