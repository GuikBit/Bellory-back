package org.exemplo.bellory.service.questionario;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.questionario.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.questionario.*;
import org.exemplo.bellory.model.entity.questionario.enums.FormatoAssinatura;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.questionario.QuestionarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuestionarioService {

    private final QuestionarioRepository questionarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final TemplateContextoBuilder templateContextoBuilder;

    public QuestionarioService(QuestionarioRepository questionarioRepository,
                               OrganizacaoRepository organizacaoRepository,
                               TemplateContextoBuilder templateContextoBuilder) {
        this.questionarioRepository = questionarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.templateContextoBuilder = templateContextoBuilder;
    }

    @Transactional
    public Questionario criar(QuestionarioCreateDTO dto) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        Questionario questionario = new Questionario();
        questionario.setOrganizacao(organizacao);
        questionario.setTitulo(dto.getTitulo());
        questionario.setDescricao(dto.getDescricao());
        questionario.setTipo(dto.getTipo());
        questionario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        questionario.setObrigatorio(dto.getObrigatorio() != null ? dto.getObrigatorio() : false);
        questionario.setAnonimo(dto.getAnonimo() != null ? dto.getAnonimo() : false);
        questionario.setUrlImagem(dto.getUrlImagem());
        questionario.setCorTema(dto.getCorTema());
        questionario.setDtCriacao(LocalDateTime.now());

        validarPerguntasTermoAssinatura(questionario.getAnonimo(), dto.getPerguntas());

        // Adicionar perguntas
        if (dto.getPerguntas() != null && !dto.getPerguntas().isEmpty()) {
            for (int i = 0; i < dto.getPerguntas().size(); i++) {
                PerguntaCreateDTO perguntaDTO = dto.getPerguntas().get(i);
                Pergunta pergunta = criarPergunta(perguntaDTO, i + 1);
                questionario.addPergunta(pergunta);
            }
        }

        return questionarioRepository.save(questionario);
    }

    @Transactional
    public Questionario atualizar(Long id, QuestionarioCreateDTO dto) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Questionario questionario = questionarioRepository
                .findByIdAndOrganizacaoIdWithPerguntas(id, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));
        questionarioRepository.fetchPerguntasComOpcoes(id);

        validarOrganizacao(questionario.getOrganizacao().getId());

        questionario.setTitulo(dto.getTitulo());
        questionario.setDescricao(dto.getDescricao());
        questionario.setTipo(dto.getTipo());
        questionario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : questionario.getAtivo());
        questionario.setObrigatorio(dto.getObrigatorio() != null ? dto.getObrigatorio() : questionario.getObrigatorio());
        questionario.setAnonimo(dto.getAnonimo() != null ? dto.getAnonimo() : questionario.getAnonimo());
        questionario.setUrlImagem(dto.getUrlImagem());
        questionario.setCorTema(dto.getCorTema());
        questionario.setDtAtualizacao(LocalDateTime.now());
        questionario.setUsuarioAtualizacao(getUserIdFromContext().toString());

        validarPerguntasTermoAssinatura(questionario.getAnonimo(), dto.getPerguntas());

        // Limpar perguntas antigas e adicionar novas
        questionario.clearPerguntas();

        if (dto.getPerguntas() != null && !dto.getPerguntas().isEmpty()) {
            for (int i = 0; i < dto.getPerguntas().size(); i++) {
                PerguntaCreateDTO perguntaDTO = dto.getPerguntas().get(i);
                Pergunta pergunta = criarPergunta(perguntaDTO, i + 1);
                questionario.addPergunta(pergunta);
            }
        }

        return questionarioRepository.save(questionario);
    }

    @Transactional
    public void deletar(Long id) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Questionario questionario = questionarioRepository
                .findByIdAndOrganizacao_IdAndIsDeletadoFalse(id, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));

        validarOrganizacao(questionario.getOrganizacao().getId());

        // Soft delete
        questionario.setDeletado(true);
        questionario.setUsuarioDeletado(getUserIdFromContext().toString());
        questionario.setDtDeletado(LocalDateTime.now());

        questionarioRepository.save(questionario);
    }

    @Transactional(readOnly = true)
    public QuestionarioDTO buscarPorId(Long id) {
        return buscarPorId(id, null, null, null);
    }

    /**
     * Busca questionario autenticado, opcionalmente resolvendo placeholders dos termos
     * de consentimento usando os IDs informados. Quando todos os IDs sao null, comportamento
     * permanece identico ao {@link #buscarPorId(Long)} original.
     */
    @Transactional(readOnly = true)
    public QuestionarioDTO buscarPorId(Long id, Long clienteId, Long agendamentoId, Long funcionarioId) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Questionario questionario = questionarioRepository
                .findByIdAndOrganizacaoIdWithPerguntas(id, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));
        questionarioRepository.fetchPerguntasComOpcoes(id);

        validarOrganizacao(questionario.getOrganizacao().getId());

        QuestionarioDTO dto = new QuestionarioDTO(questionario);
        dto.setTotalRespostas(questionarioRepository.countRespostasByQuestionarioId(id));

        aplicarRenderizacaoTermo(dto, organizacaoId, clienteId, agendamentoId, funcionarioId);

        return dto;
    }

    @Transactional(readOnly = true)
    public Questionario buscarEntityPorId(Long id) {
        Questionario questionario = questionarioRepository.findByIdWithPerguntas(id)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));
        questionarioRepository.fetchPerguntasComOpcoes(id);
        return questionario;
    }

    @Transactional(readOnly = true)
    public QuestionarioDTO buscarPublicoPorSlug(Long id, Long organizacaoId) {
        return buscarPublicoPorSlug(id, organizacaoId, null, null, null);
    }

    /**
     * Versao publica do GET de questionario com resolucao opcional de placeholders.
     * Quando os IDs sao informados, valida que cliente/agendamento/funcionario pertencem
     * ao tenant do slug e popula {@code PerguntaDTO.textoTermoRenderizado}.
     */
    @Transactional(readOnly = true)
    public QuestionarioDTO buscarPublicoPorSlug(Long id, Long organizacaoId,
                                                Long clienteId, Long agendamentoId, Long funcionarioId) {
        Questionario questionario = questionarioRepository
                .findByIdAndOrganizacaoIdWithPerguntas(id, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));
        questionarioRepository.fetchPerguntasComOpcoes(id);

        if (!questionario.getAtivo()) {
            throw new IllegalArgumentException("Este questionário não está disponível.");
        }

        QuestionarioDTO dto = new QuestionarioDTO(questionario);
        dto.setTotalRespostas(questionarioRepository.countRespostasByQuestionarioId(id));

        aplicarRenderizacaoTermo(dto, organizacaoId, clienteId, agendamentoId, funcionarioId);

        return dto;
    }

    /**
     * Resolve placeholders {@code {{var}}} em todas as perguntas tipo TERMO_CONSENTIMENTO
     * do DTO, populando {@code textoTermoRenderizado}. No-op quando todos os IDs sao null.
     */
    private void aplicarRenderizacaoTermo(QuestionarioDTO dto, Long organizacaoId,
                                          Long clienteId, Long agendamentoId, Long funcionarioId) {
        if (clienteId == null && agendamentoId == null && funcionarioId == null) {
            return;
        }
        if (dto.getPerguntas() == null || dto.getPerguntas().isEmpty()) {
            return;
        }
        boolean temTermo = dto.getPerguntas().stream().anyMatch(p ->
                p.getTipo() == org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta.TERMO_CONSENTIMENTO);
        if (!temTermo) return;

        Map<String, String> contexto = templateContextoBuilder.construir(
                organizacaoId, clienteId, agendamentoId, funcionarioId);

        for (PerguntaDTO p : dto.getPerguntas()) {
            if (p.getTipo() == org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta.TERMO_CONSENTIMENTO
                    && p.getTextoTermo() != null) {
                p.setTextoTermoRenderizado(templateContextoBuilder.renderizar(p.getTextoTermo(), contexto));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<QuestionarioDTO> listarPorOrganizacao() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return questionarioRepository
                .findByOrganizacao_IdAndIsDeletadoFalseOrderByDtCriacaoDesc(organizacaoId)
                .stream()
                .map(q -> {
                    QuestionarioDTO dto = new QuestionarioDTO(q);
                    dto.setTotalRespostas(questionarioRepository.countRespostasByQuestionarioId(q.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuestionarioDTO> listarAtivosPorOrganizacao() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return questionarioRepository
                .findByOrganizacao_IdAndIsDeletadoFalseAndAtivoTrueOrderByDtCriacaoDesc(organizacaoId)
                .stream()
                .map(q -> {
                    QuestionarioDTO dto = new QuestionarioDTO(q);
                    dto.setTotalRespostas(questionarioRepository.countRespostasByQuestionarioId(q.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuestionarioDTO> listarPorTipo(TipoQuestionario tipo) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return questionarioRepository
                .findByOrganizacao_IdAndTipoAndIsDeletadoFalse(organizacaoId, tipo)
                .stream()
                .map(q -> {
                    QuestionarioDTO dto = new QuestionarioDTO(q);
                    dto.setTotalRespostas(questionarioRepository.countRespostasByQuestionarioId(q.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<QuestionarioDTO> pesquisar(String termo, Pageable pageable) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return questionarioRepository
                .searchByTituloOrDescricao(organizacaoId, termo, pageable)
                .map(q -> {
                    QuestionarioDTO dto = new QuestionarioDTO(q);
                    dto.setTotalRespostas(questionarioRepository.countRespostasByQuestionarioId(q.getId()));
                    return dto;
                });
    }

    @Transactional(readOnly = true)
    public Long contarRespostas(Long questionarioId) {
        return questionarioRepository.countRespostasByQuestionarioId(questionarioId);
    }

    // Métodos auxiliares

    private static final int LARGURA_ASSINATURA_MIN = 200;
    private static final int LARGURA_ASSINATURA_MAX = 1200;
    private static final int LARGURA_ASSINATURA_DEFAULT = 600;
    private static final int ALTURA_ASSINATURA_MIN = 100;
    private static final int ALTURA_ASSINATURA_MAX = 600;
    private static final int ALTURA_ASSINATURA_DEFAULT = 200;

    /**
     * Valida regras de negocio para perguntas tipo TERMO_CONSENTIMENTO e ASSINATURA
     * que dependem do contexto do questionario (ex.: anonimato).
     */
    private void validarPerguntasTermoAssinatura(Boolean anonimo, List<PerguntaCreateDTO> perguntas) {
        if (perguntas == null || perguntas.isEmpty()) {
            return;
        }
        boolean isAnonimo = Boolean.TRUE.equals(anonimo);
        for (PerguntaCreateDTO p : perguntas) {
            TipoPergunta tipo = p.getTipo();
            if (tipo == null) continue;

            boolean ehTermoOuAssinatura = tipo == TipoPergunta.TERMO_CONSENTIMENTO
                    || tipo == TipoPergunta.ASSINATURA;

            if (ehTermoOuAssinatura && isAnonimo) {
                throw new IllegalArgumentException(
                        "Questionário anônimo não pode conter perguntas do tipo TERMO_CONSENTIMENTO ou ASSINATURA.");
            }

            if (ehTermoOuAssinatura && p.getOpcoes() != null && !p.getOpcoes().isEmpty()) {
                throw new IllegalArgumentException(
                        "Perguntas do tipo TERMO_CONSENTIMENTO ou ASSINATURA não aceitam opções de resposta.");
            }

            if (tipo == TipoPergunta.TERMO_CONSENTIMENTO) {
                if (p.getTextoTermo() == null || p.getTextoTermo().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "O campo 'textoTermo' é obrigatório para perguntas do tipo TERMO_CONSENTIMENTO.");
                }
            }

            if (tipo == TipoPergunta.ASSINATURA) {
                Integer largura = p.getLarguraAssinatura();
                if (largura != null && (largura < LARGURA_ASSINATURA_MIN || largura > LARGURA_ASSINATURA_MAX)) {
                    throw new IllegalArgumentException(
                            "Largura da assinatura deve estar entre " + LARGURA_ASSINATURA_MIN
                                    + " e " + LARGURA_ASSINATURA_MAX + " pixels.");
                }
                Integer altura = p.getAlturaAssinatura();
                if (altura != null && (altura < ALTURA_ASSINATURA_MIN || altura > ALTURA_ASSINATURA_MAX)) {
                    throw new IllegalArgumentException(
                            "Altura da assinatura deve estar entre " + ALTURA_ASSINATURA_MIN
                                    + " e " + ALTURA_ASSINATURA_MAX + " pixels.");
                }
            }
        }
    }

    private Pergunta criarPergunta(PerguntaCreateDTO dto, int ordemPadrao) {
        Pergunta pergunta = new Pergunta();
        pergunta.setTexto(dto.getTexto());
        pergunta.setDescricao(dto.getDescricao());
        pergunta.setTipo(dto.getTipo());
        pergunta.setObrigatoria(dto.getObrigatoria() != null ? dto.getObrigatoria() : false);
        pergunta.setOrdem(dto.getOrdem() != null ? dto.getOrdem() : ordemPadrao);
        pergunta.setEscalaMin(dto.getEscalaMin());
        pergunta.setEscalaMax(dto.getEscalaMax());
        pergunta.setLabelMin(dto.getLabelMin());
        pergunta.setLabelMax(dto.getLabelMax());
        pergunta.setMinCaracteres(dto.getMinCaracteres());
        pergunta.setMaxCaracteres(dto.getMaxCaracteres());
        pergunta.setMinValor(dto.getMinValor());
        pergunta.setMaxValor(dto.getMaxValor());

        // Termo de consentimento
        if (dto.getTipo() == TipoPergunta.TERMO_CONSENTIMENTO) {
            pergunta.setTextoTermo(dto.getTextoTermo());
            pergunta.setTemplateTermoId(dto.getTemplateTermoId());
            pergunta.setRequerAceiteExplicito(
                    dto.getRequerAceiteExplicito() != null ? dto.getRequerAceiteExplicito() : false);
        }

        // Assinatura digital
        if (dto.getTipo() == TipoPergunta.ASSINATURA) {
            pergunta.setFormatoAssinatura(
                    dto.getFormatoAssinatura() != null ? dto.getFormatoAssinatura() : FormatoAssinatura.PNG_BASE64);
            pergunta.setLarguraAssinatura(
                    dto.getLarguraAssinatura() != null ? dto.getLarguraAssinatura() : LARGURA_ASSINATURA_DEFAULT);
            pergunta.setAlturaAssinatura(
                    dto.getAlturaAssinatura() != null ? dto.getAlturaAssinatura() : ALTURA_ASSINATURA_DEFAULT);
            pergunta.setExigirAssinaturaProfissional(
                    dto.getExigirAssinaturaProfissional() != null ? dto.getExigirAssinaturaProfissional() : false);
        }

        // Adicionar opções (somente para tipos que aceitam — validação ja garante isso)
        if (dto.getOpcoes() != null && !dto.getOpcoes().isEmpty()) {
            for (int j = 0; j < dto.getOpcoes().size(); j++) {
                OpcaoRespostaCreateDTO opcaoDTO = dto.getOpcoes().get(j);
                OpcaoResposta opcao = criarOpcao(opcaoDTO, j + 1);
                pergunta.addOpcao(opcao);
            }
        }

        return pergunta;
    }

    private OpcaoResposta criarOpcao(OpcaoRespostaCreateDTO dto, int ordemPadrao) {
        OpcaoResposta opcao = new OpcaoResposta();
        opcao.setTexto(dto.getTexto());
        opcao.setValor(dto.getValor());
        opcao.setOrdem(dto.getOrdem() != null ? dto.getOrdem() : ordemPadrao);
        return opcao;
    }

    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }
        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: recurso pertence a outra organização");
        }
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }
        return organizacaoId;
    }

    private Long getUserIdFromContext() {
        Long userId = TenantContext.getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("Usuário não identificado");
        }
        return userId;
    }
}
