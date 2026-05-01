package org.exemplo.bellory.service.questionario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.questionario.Pergunta;
import org.exemplo.bellory.model.entity.questionario.Questionario;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;
import org.exemplo.bellory.model.repository.questionario.QuestionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Materializa registros sistemicos de questionario para uma organizacao. Cada registro
 * vive com {@code is_sistema=true} + {@code chave_sistema} unica por org — nao pode ser
 * deletado nem desativado, apenas as perguntas podem ser editadas pelo admin.
 *
 * Hoje so a {@code ANAMNESE_PADRAO} e materializada. Para adicionar outros sistemicos,
 * basta adicionar uma nova chamada em {@link #materializarPadroes(Organizacao)}; a
 * idempotencia evita duplicacao em caso de re-execucao.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionarioSistemaService {

    public static final String CHAVE_ANAMNESE_PADRAO = "ANAMNESE_PADRAO";

    private final QuestionarioRepository questionarioRepository;

    @Transactional
    public void materializarPadroes(Organizacao organizacao) {
        if (organizacao == null || organizacao.getId() == null) {
            throw new IllegalArgumentException("Organização inválida para materializar registros sistêmicos.");
        }
        materializarAnamnesePadrao(organizacao);
    }

    private void materializarAnamnesePadrao(Organizacao organizacao) {
        if (questionarioRepository.existsByOrganizacao_IdAndChaveSistema(organizacao.getId(), CHAVE_ANAMNESE_PADRAO)) {
            log.debug("Anamnese padrão já existe para org={}, ignorando.", organizacao.getId());
            return;
        }

        Questionario questionario = new Questionario();
        questionario.setOrganizacao(organizacao);
        questionario.setTitulo("Anamnese Padrão");
        questionario.setDescricao("Questionário de anamnese padrão para procedimentos estéticos. "
                + "Preencha com atenção antes do atendimento — essas informações ajudam o profissional "
                + "a garantir sua segurança.");
        questionario.setTipo(TipoQuestionario.CLIENTE);
        questionario.setAtivo(true);
        questionario.setObrigatorio(true);
        questionario.setAnonimo(false);
        questionario.setDeletado(false);
        questionario.setSistema(true);
        questionario.setChaveSistema(CHAVE_ANAMNESE_PADRAO);
        questionario.setDtCriacao(LocalDateTime.now());

        for (PerguntaSeed seed : perguntasAnamnesePadrao()) {
            Pergunta p = new Pergunta();
            p.setTexto(seed.texto);
            p.setTipo(seed.tipo);
            p.setObrigatoria(seed.obrigatoria);
            p.setOrdem(seed.ordem);
            questionario.addPergunta(p);
        }

        questionarioRepository.save(questionario);
        log.info("Anamnese padrão materializada para org={}", organizacao.getId());
    }

    private List<PerguntaSeed> perguntasAnamnesePadrao() {
        return List.of(
                new PerguntaSeed(1, TipoPergunta.TEXTO_LONGO, true,
                        "Possui alguma alergia conhecida (medicamentos, cosméticos, esmalte, henna, látex, etc.)? Se sim, descreva."),
                new PerguntaSeed(2, TipoPergunta.TEXTO_LONGO, true,
                        "Faz uso de medicamentos contínuos? Se sim, quais?"),
                new PerguntaSeed(3, TipoPergunta.TEXTO_LONGO, true,
                        "Possui alguma condição de saúde relevante (diabetes, hipertensão, problemas cardíacos, doenças autoimunes, distúrbios de coagulação)?"),
                new PerguntaSeed(4, TipoPergunta.SIM_NAO, true,
                        "Está gestante ou amamentando?"),
                new PerguntaSeed(5, TipoPergunta.TEXTO_LONGO, false,
                        "Possui sensibilidade ou irritação na pele (eczema, dermatite, psoríase, rosácea)?"),
                new PerguntaSeed(6, TipoPergunta.TEXTO_LONGO, false,
                        "Já passou por algum procedimento estético recente? Qual e há quanto tempo?"),
                new PerguntaSeed(7, TipoPergunta.TEXTO_LONGO, false,
                        "Tem cicatrizes, manchas, ferimentos ativos ou tatuagens na área a ser tratada?"),
                new PerguntaSeed(8, TipoPergunta.TEXTO_LONGO, false,
                        "Observações adicionais que julgue importante informar ao profissional.")
        );
    }

    private static final class PerguntaSeed {
        final int ordem;
        final TipoPergunta tipo;
        final boolean obrigatoria;
        final String texto;

        PerguntaSeed(int ordem, TipoPergunta tipo, boolean obrigatoria, String texto) {
            this.ordem = ordem;
            this.tipo = tipo;
            this.obrigatoria = obrigatoria;
            this.texto = texto;
        }
    }
}
