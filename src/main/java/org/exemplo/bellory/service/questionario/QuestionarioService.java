package org.exemplo.bellory.service.questionario;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.questionario.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.questionario.*;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.questionario.QuestionarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionarioService {

    private final QuestionarioRepository questionarioRepository;
    private final OrganizacaoRepository organizacaoRepository;

    public QuestionarioService(QuestionarioRepository questionarioRepository,
                               OrganizacaoRepository organizacaoRepository) {
        this.questionarioRepository = questionarioRepository;
        this.organizacaoRepository = organizacaoRepository;
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
        Long organizacaoId = getOrganizacaoIdFromContext();

        Questionario questionario = questionarioRepository
                .findByIdAndOrganizacaoIdWithPerguntas(id, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));

        validarOrganizacao(questionario.getOrganizacao().getId());

        QuestionarioDTO dto = new QuestionarioDTO(questionario);
        dto.setTotalRespostas(questionarioRepository.countRespostasByQuestionarioId(id));

        return dto;
    }

    @Transactional(readOnly = true)
    public Questionario buscarEntityPorId(Long id) {
        return questionarioRepository.findByIdWithPerguntas(id)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));
    }

    @Transactional(readOnly = true)
    public Page<QuestionarioDTO> listarPorOrganizacao(Pageable pageable) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return questionarioRepository.findByOrganizacao_IdAndIsDeletadoFalse(organizacaoId, pageable)
                .map(q -> {
                    QuestionarioDTO dto = new QuestionarioDTO(q);
                    dto.setTotalRespostas(questionarioRepository.countRespostasByQuestionarioId(q.getId()));
                    return dto;
                });
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

        // Adicionar opções
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
