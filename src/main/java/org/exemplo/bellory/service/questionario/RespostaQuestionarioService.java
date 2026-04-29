package org.exemplo.bellory.service.questionario;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.questionario.*;
import org.exemplo.bellory.model.entity.agendamento.AgendamentoQuestionario;
import org.exemplo.bellory.model.entity.agendamento.StatusAssinatura;
import org.exemplo.bellory.model.entity.agendamento.StatusQuestionarioAgendamento;
import org.exemplo.bellory.model.entity.arquivo.Arquivo;
import org.exemplo.bellory.model.entity.questionario.*;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoQuestionarioRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.questionario.*;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.service.ArquivoStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RespostaQuestionarioService {

    private static final Set<String> STOPWORDS_PT = Set.of(
            "a", "o", "e", "é", "de", "do", "da", "dos", "das", "no", "na", "nos", "nas",
            "um", "uma", "uns", "umas", "que", "se", "para", "por", "com", "sem", "sob",
            "mas", "ou", "como", "muito", "mais", "menos", "já", "também", "só", "ser",
            "ter", "estar", "foi", "era", "são", "eu", "tu", "ele", "ela", "nós", "vós",
            "eles", "elas", "meu", "minha", "seu", "sua", "este", "esta", "esse", "essa",
            "isto", "isso", "aquele", "aquela", "ao", "aos", "à", "às", "pelo", "pela",
            "em", "qual", "tudo", "nada", "algo", "sim", "não", "lhe", "te", "me");

    private static final int TOP_PALAVRAS = 10;
    private static final int TAMANHO_MIN_PALAVRA = 3;


    private final QuestionarioRepository questionarioRepository;
    private final RespostaQuestionarioRepository respostaQuestionarioRepository;
    private final RespostaPerguntaRepository respostaPerguntaRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final AgendamentoQuestionarioRepository agendamentoQuestionarioRepository;
    private final ArquivoStorageService arquivoStorageService;

    public RespostaQuestionarioService(QuestionarioRepository questionarioRepository,
                                       RespostaQuestionarioRepository respostaQuestionarioRepository,
                                       RespostaPerguntaRepository respostaPerguntaRepository,
                                       ClienteRepository clienteRepository,
                                       FuncionarioRepository funcionarioRepository,
                                       AgendamentoQuestionarioRepository agendamentoQuestionarioRepository,
                                       ArquivoStorageService arquivoStorageService) {
        this.questionarioRepository = questionarioRepository;
        this.respostaQuestionarioRepository = respostaQuestionarioRepository;
        this.respostaPerguntaRepository = respostaPerguntaRepository;
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.agendamentoQuestionarioRepository = agendamentoQuestionarioRepository;
        this.arquivoStorageService = arquivoStorageService;
    }

    @Transactional
    public RespostaQuestionarioDTO registrarPublico(Long organizacaoId, RespostaQuestionarioCreateDTO dto, String ipOrigem) {
        Questionario questionario = questionarioRepository
                .findByIdAndOrganizacaoIdWithPerguntas(dto.getQuestionarioId(), organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));
        questionarioRepository.fetchPerguntasComOpcoes(dto.getQuestionarioId());
        return registrarInterno(dto, ipOrigem, questionario);
    }

    @Transactional
    public RespostaQuestionarioDTO registrar(RespostaQuestionarioCreateDTO dto, String ipOrigem) {
        Questionario questionario = questionarioRepository.findByIdWithPerguntas(dto.getQuestionarioId())
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));
        return registrarInterno(dto, ipOrigem, questionario);
    }

    private RespostaQuestionarioDTO registrarInterno(RespostaQuestionarioCreateDTO dto, String ipOrigem, Questionario questionario) {

        if (!questionario.getAtivo()) {
            throw new IllegalArgumentException("Este questionário não está mais aceitando respostas.");
        }

        // Verificar se agendamento já foi avaliado
        if (dto.getAgendamentoId() != null) {
            if (respostaQuestionarioRepository.findByQuestionarioIdAndAgendamentoId(
                    dto.getQuestionarioId(), dto.getAgendamentoId()).isPresent()) {
                throw new IllegalArgumentException("Este agendamento já foi avaliado.");
            }
        }

        // Validar respostas
        validarRespostas(dto, questionario);

        Long organizacaoId = questionario.getOrganizacao() != null
                ? questionario.getOrganizacao().getId() : null;
        Long criadoPor = TenantContext.getCurrentUserId(); // pode ser null no fluxo publico

        // Criar entidade
        RespostaQuestionario respostaQuestionario = new RespostaQuestionario();
        respostaQuestionario.setQuestionario(questionario);
        respostaQuestionario.setClienteId(dto.getClienteId());
        respostaQuestionario.setColaboradorId(dto.getColaboradorId());
        respostaQuestionario.setAgendamentoId(dto.getAgendamentoId());
        respostaQuestionario.setIpOrigem(ipOrigem);
        respostaQuestionario.setDispositivo(dto.getDispositivo());
        respostaQuestionario.setTempoPreenchimentoSegundos(dto.getTempoPreenchimentoSegundos());
        respostaQuestionario.setUserAgent(dto.getUserAgent());
        respostaQuestionario.setDtResposta(LocalDateTime.now());

        // Mapear perguntas e DTOs por id de pergunta
        Map<Long, Pergunta> perguntasMap = questionario.getPerguntas().stream()
                .collect(Collectors.toMap(Pergunta::getId, Function.identity()));
        Map<Long, RespostaPerguntaCreateDTO> dtoPorPerguntaId = dto.getRespostas().stream()
                .collect(Collectors.toMap(RespostaPerguntaCreateDTO::getPerguntaId, Function.identity()));

        // Processar respostas (campos basicos + termo; assinatura e gravada apos primeiro flush)
        LocalDateTime agora = LocalDateTime.now();
        for (RespostaPerguntaCreateDTO respostaDTO : dto.getRespostas()) {
            Pergunta pergunta = perguntasMap.get(respostaDTO.getPerguntaId());

            RespostaPergunta respostaPergunta = new RespostaPergunta();
            respostaPergunta.setRespostaQuestionario(respostaQuestionario);
            respostaPergunta.setPergunta(pergunta);
            respostaPergunta.setRespostaTexto(respostaDTO.getRespostaTexto());
            respostaPergunta.setRespostaNumero(respostaDTO.getRespostaNumero());
            respostaPergunta.setRespostaOpcaoIds(respostaDTO.getRespostaOpcaoIds() != null ?
                    new ArrayList<>(respostaDTO.getRespostaOpcaoIds()) : new ArrayList<>());
            respostaPergunta.setRespostaData(respostaDTO.getRespostaData());
            respostaPergunta.setRespostaHora(respostaDTO.getRespostaHora());

            // Termo de consentimento: dataAceite e hash sao SEMPRE gerados pelo servidor
            if (pergunta.getTipo() == TipoPergunta.TERMO_CONSENTIMENTO) {
                respostaPergunta.setAceitouTermo(respostaDTO.getAceitouTermo());
                if (respostaDTO.getTextoTermoRenderizado() != null
                        && !respostaDTO.getTextoTermoRenderizado().isBlank()) {
                    respostaPergunta.setTextoTermoRenderizado(respostaDTO.getTextoTermoRenderizado());
                    respostaPergunta.setHashTermo(sha256Hex(respostaDTO.getTextoTermoRenderizado()));
                }
                if (Boolean.TRUE.equals(respostaDTO.getAceitouTermo())) {
                    respostaPergunta.setDataAceite(agora);
                }
            }

            respostaQuestionario.addResposta(respostaPergunta);
        }

        // Primeiro save: gera IDs das RespostaPergunta para usar como sub-pasta da assinatura
        RespostaQuestionario saved = respostaQuestionarioRepository.saveAndFlush(respostaQuestionario);

        // Processar assinaturas (precisa do ID gerado de cada RespostaPergunta)
        boolean temPerguntaAssinatura = false;
        boolean todasObrigatoriasCapturadas = true;
        boolean algumaAssinaturaCapturada = false;

        for (RespostaPergunta rp : saved.getRespostas()) {
            Pergunta p = rp.getPergunta();
            if (p.getTipo() != TipoPergunta.ASSINATURA) continue;

            temPerguntaAssinatura = true;
            RespostaPerguntaCreateDTO respostaDTO = dtoPorPerguntaId.get(p.getId());
            if (respostaDTO == null) {
                if (Boolean.TRUE.equals(p.getObrigatoria())) todasObrigatoriasCapturadas = false;
                continue;
            }

            String b64Cliente = respostaDTO.getAssinaturaClienteBase64();
            if (b64Cliente != null && !b64Cliente.isBlank()) {
                AssinaturaImagemValidator.Resultado res = AssinaturaImagemValidator.decodificarEValidar(b64Cliente);
                Arquivo arquivo = arquivoStorageService.salvarAssinatura(
                        res.getBytes(),
                        res.getFormato().getExtensao(),
                        res.getFormato().getContentType(),
                        organizacaoId,
                        rp.getId(),
                        false,
                        criadoPor);
                rp.setArquivoAssinaturaClienteId(arquivo.getId());
                algumaAssinaturaCapturada = true;
            } else if (Boolean.TRUE.equals(p.getObrigatoria())) {
                todasObrigatoriasCapturadas = false;
            }

            if (Boolean.TRUE.equals(p.getExigirAssinaturaProfissional())) {
                String b64Prof = respostaDTO.getAssinaturaProfissionalBase64();
                if (b64Prof != null && !b64Prof.isBlank()) {
                    AssinaturaImagemValidator.Resultado res = AssinaturaImagemValidator.decodificarEValidar(b64Prof);
                    Arquivo arquivo = arquivoStorageService.salvarAssinatura(
                            res.getBytes(),
                            res.getFormato().getExtensao(),
                            res.getFormato().getContentType(),
                            organizacaoId,
                            rp.getId(),
                            true,
                            criadoPor);
                    rp.setArquivoAssinaturaProfissionalId(arquivo.getId());
                    algumaAssinaturaCapturada = true;
                } else {
                    todasObrigatoriasCapturadas = false;
                }
            }
        }

        if (temPerguntaAssinatura) {
            saved = respostaQuestionarioRepository.save(saved);
        }

        // Tracking: status duplo no AgendamentoQuestionario
        if (saved.getAgendamentoId() != null) {
            StatusAssinatura statusAssinatura = !temPerguntaAssinatura
                    ? StatusAssinatura.NAO_REQUERIDA
                    : (algumaAssinaturaCapturada && todasObrigatoriasCapturadas
                        ? StatusAssinatura.ASSINADA
                        : StatusAssinatura.PENDENTE);

            final RespostaQuestionario savedFinal = saved;
            final boolean assinou = statusAssinatura == StatusAssinatura.ASSINADA;
            final StatusAssinatura statusFinal = statusAssinatura;
            agendamentoQuestionarioRepository
                    .findByAgendamentoIdAndQuestionarioId(savedFinal.getAgendamentoId(), questionario.getId())
                    .ifPresent(aq -> {
                        aq.setStatus(StatusQuestionarioAgendamento.RESPONDIDO);
                        aq.setDtResposta(LocalDateTime.now());
                        aq.setRespostaQuestionarioId(savedFinal.getId());
                        aq.setStatusAssinatura(statusFinal);
                        if (assinou) {
                            aq.setDtAssinatura(LocalDateTime.now());
                        }
                        agendamentoQuestionarioRepository.save(aq);
                    });
        }

        return enriquecerComNomes(new RespostaQuestionarioDTO(saved));
    }

    /**
     * Calcula SHA-256 hex do conteudo. Usado para congelar o termo aceito e detectar
     * adulteracao posterior (via endpoint de auditoria).
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }

    @Transactional(readOnly = true)
    public RespostaQuestionarioDTO buscarPorId(Long id) {
        RespostaQuestionario resposta = respostaQuestionarioRepository.findByIdWithRespostas(id)
                .orElseThrow(() -> new IllegalArgumentException("Resposta não encontrada."));
        return enriquecerComNomes(new RespostaQuestionarioDTO(resposta));
    }

    /**
     * Monta o relatorio de auditoria de termos/assinaturas de uma resposta.
     * Recalcula o SHA-256 do {@code textoTermoRenderizado} e compara com {@code hashTermo}
     * armazenado para detectar adulteracao.
     *
     * Retorna respostas mesmo quando soft-deleted (uso legal).
     */
    @Transactional(readOnly = true)
    public AuditoriaTermoDTO obterAuditoria(Long respostaQuestionarioId) {
        RespostaQuestionario resposta = respostaQuestionarioRepository.findByIdWithRespostas(respostaQuestionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Resposta não encontrada."));

        List<AuditoriaTermoDTO.TermoAceito> termos = new ArrayList<>();
        List<AuditoriaTermoDTO.AssinaturaCapturada> assinaturas = new ArrayList<>();

        for (RespostaPergunta rp : resposta.getRespostas()) {
            Pergunta p = rp.getPergunta();
            if (p == null) continue;
            TipoPergunta tipo = p.getTipo();

            if (tipo == TipoPergunta.TERMO_CONSENTIMENTO) {
                String hashRecalculado = rp.getTextoTermoRenderizado() != null
                        ? sha256Hex(rp.getTextoTermoRenderizado()) : null;
                boolean integridadeOk = hashRecalculado != null
                        && hashRecalculado.equals(rp.getHashTermo());
                termos.add(AuditoriaTermoDTO.TermoAceito.builder()
                        .respostaPerguntaId(rp.getId())
                        .perguntaId(p.getId())
                        .perguntaTexto(p.getTexto())
                        .aceitouTermo(rp.getAceitouTermo())
                        .dataAceite(rp.getDataAceite())
                        .textoTermoRenderizado(rp.getTextoTermoRenderizado())
                        .hashTermoEsperado(rp.getHashTermo())
                        .hashTermoCalculado(hashRecalculado)
                        .integridadeOk(integridadeOk)
                        .build());
            }

            if (tipo == TipoPergunta.ASSINATURA) {
                String urlBase = "/api/v1/resposta-questionario/" + resposta.getId() + "/assinatura/";
                String urlCliente = rp.getArquivoAssinaturaClienteId() != null
                        ? urlBase + "cliente?perguntaId=" + p.getId() : null;
                String urlProf = rp.getArquivoAssinaturaProfissionalId() != null
                        ? urlBase + "profissional?perguntaId=" + p.getId() : null;
                assinaturas.add(AuditoriaTermoDTO.AssinaturaCapturada.builder()
                        .respostaPerguntaId(rp.getId())
                        .perguntaId(p.getId())
                        .perguntaTexto(p.getTexto())
                        .arquivoAssinaturaClienteId(rp.getArquivoAssinaturaClienteId())
                        .arquivoAssinaturaProfissionalId(rp.getArquivoAssinaturaProfissionalId())
                        .urlAssinaturaCliente(urlCliente)
                        .urlAssinaturaProfissional(urlProf)
                        .build());
            }
        }

        return AuditoriaTermoDTO.builder()
                .respostaQuestionarioId(resposta.getId())
                .questionarioId(resposta.getQuestionario() != null ? resposta.getQuestionario().getId() : null)
                .questionarioTitulo(resposta.getQuestionario() != null ? resposta.getQuestionario().getTitulo() : null)
                .clienteId(resposta.getClienteId())
                .agendamentoId(resposta.getAgendamentoId())
                .dtResposta(resposta.getDtResposta())
                .ipOrigem(resposta.getIpOrigem())
                .userAgent(resposta.getUserAgent())
                .dispositivo(resposta.getDispositivo())
                .deletado(resposta.isDeletado())
                .dtDeletado(resposta.getDtDeletado())
                .termos(termos)
                .assinaturas(assinaturas)
                .build();
    }

    /**
     * Recupera os bytes da assinatura associada a uma pergunta dentro de uma resposta,
     * validando que a pergunta pertence a essa resposta e que o arquivo eh de sistema.
     *
     * @param respostaQuestionarioId id da resposta
     * @param perguntaId id da pergunta tipo ASSINATURA
     * @param profissional true para devolver a assinatura do profissional, false para cliente
     * @return bytes brutos da imagem (PNG/SVG)
     */
    @Transactional(readOnly = true)
    public AssinaturaDownload buscarBytesAssinatura(Long respostaQuestionarioId, Long perguntaId, boolean profissional) {
        RespostaQuestionario resposta = respostaQuestionarioRepository.findByIdWithRespostas(respostaQuestionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Resposta não encontrada."));

        RespostaPergunta rp = resposta.getRespostas().stream()
                .filter(r -> r.getPergunta() != null && perguntaId.equals(r.getPergunta().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pergunta não pertence a esta resposta."));

        if (rp.getPergunta().getTipo() != TipoPergunta.ASSINATURA) {
            throw new IllegalArgumentException("Pergunta não é do tipo ASSINATURA.");
        }

        Long arquivoId = profissional
                ? rp.getArquivoAssinaturaProfissionalId()
                : rp.getArquivoAssinaturaClienteId();
        if (arquivoId == null) {
            throw new IllegalArgumentException("Assinatura não encontrada para este registro.");
        }

        Long organizacaoId = resposta.getQuestionario() != null && resposta.getQuestionario().getOrganizacao() != null
                ? resposta.getQuestionario().getOrganizacao().getId() : null;
        if (organizacaoId == null) {
            throw new IllegalArgumentException("Organização da resposta não identificada.");
        }

        byte[] bytes = arquivoStorageService.lerAssinatura(arquivoId, organizacaoId);
        return new AssinaturaDownload(bytes, resposta.getClienteId(), organizacaoId);
    }

    /**
     * Tupla simples para devolver bytes + ownership a quem chama (para o controller validar tenant/cliente).
     */
    public record AssinaturaDownload(byte[] bytes, Long clienteId, Long organizacaoId) {}

    /**
     * Carrega a resposta com perguntas/organizacao para o gerador de PDF.
     * Inclui respostas soft-deleted (auditoria/comprovante eh acessivel mesmo apos delete).
     */
    @Transactional(readOnly = true)
    public RespostaQuestionario buscarParaComprovante(Long id) {
        RespostaQuestionario resposta = respostaQuestionarioRepository.findByIdWithRespostas(id)
                .orElseThrow(() -> new IllegalArgumentException("Resposta não encontrada."));
        // toca os campos LAZY que o PDF vai consumir (organizacao do questionario)
        if (resposta.getQuestionario() != null && resposta.getQuestionario().getOrganizacao() != null) {
            resposta.getQuestionario().getOrganizacao().getNomeFantasia();
        }
        return resposta;
    }

    @Transactional(readOnly = true)
    public List<RespostaQuestionarioDTO> buscarHistoricoPorCliente(Long questionarioId, Long clienteId) {
        return respostaQuestionarioRepository
                .findHistoricoByQuestionarioIdAndClienteId(questionarioId, clienteId)
                .stream()
                .map(r -> enriquecerComNomes(new RespostaQuestionarioDTO(r)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletar(Long id) {
        deletar(id, TenantContext.getCurrentUsername());
    }

    /**
     * Deleta uma resposta de questionario.
     *
     * Quando a resposta contem ao menos uma {@code RespostaPergunta} com termo de
     * consentimento aceito ou assinatura digital, aplica-se SOFT-DELETE para preservar
     * o registro (LGPD Art. 16, II — retencao para cumprimento de obrigacao legal).
     *
     * Caso contrario, mantem-se o hard-delete original (comportamento back-compat).
     */
    @Transactional
    public void deletar(Long id, String usuarioDeletado) {
        RespostaQuestionario resposta = respostaQuestionarioRepository.findByIdWithRespostas(id)
                .orElseThrow(() -> new IllegalArgumentException("Resposta não encontrada."));

        if (resposta.isDeletado()) {
            return; // idempotente — ja foi soft-deleted
        }

        boolean temProvaLegal = resposta.getRespostas() != null
                && resposta.getRespostas().stream().anyMatch(rp ->
                        Boolean.TRUE.equals(rp.getAceitouTermo())
                                || rp.getArquivoAssinaturaClienteId() != null
                                || rp.getArquivoAssinaturaProfissionalId() != null);

        if (temProvaLegal) {
            resposta.setDeletado(true);
            resposta.setUsuarioDeletado(usuarioDeletado);
            resposta.setDtDeletado(LocalDateTime.now());
            respostaQuestionarioRepository.save(resposta);
        } else {
            respostaQuestionarioRepository.delete(resposta);
        }
    }

    @Transactional(readOnly = true)
    public Page<RespostaQuestionarioDTO> listarPorQuestionario(Long questionarioId, Pageable pageable) {
        return respostaQuestionarioRepository.findByQuestionarioId(questionarioId, pageable)
                .map(r -> enriquecerComNomes(new RespostaQuestionarioDTO(r)));
    }

    @Transactional(readOnly = true)
    public Page<RespostaQuestionarioDTO> listarPorQuestionarioEPeriodo(
            Long questionarioId, LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {
        return respostaQuestionarioRepository.findByQuestionarioIdAndPeriodoPaged(
                questionarioId, inicio, fim, pageable)
                .map(r -> enriquecerComNomes(new RespostaQuestionarioDTO(r)));
    }

    @Transactional(readOnly = true)
    public boolean clienteJaRespondeu(Long questionarioId, Long clienteId) {
        return respostaQuestionarioRepository.existsByQuestionarioIdAndClienteId(questionarioId, clienteId);
    }

    @Transactional(readOnly = true)
    public boolean agendamentoJaAvaliado(Long questionarioId, Long agendamentoId) {
        return respostaQuestionarioRepository.findByQuestionarioIdAndAgendamentoId(
                questionarioId, agendamentoId).isPresent();
    }

    @Transactional(readOnly = true)
    public EstatisticasQuestionarioDTO obterEstatisticas(Long questionarioId) {
        Questionario questionario = questionarioRepository.findByIdWithPerguntas(questionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));

        LocalDateTime agora = LocalDateTime.now();

        EstatisticasQuestionarioDTO stats = new EstatisticasQuestionarioDTO();
        stats.setQuestionarioId(questionarioId);
        stats.setQuestionarioTitulo(questionario.getTitulo());

        // Contagens
        stats.setTotalRespostas(respostaQuestionarioRepository.countByQuestionarioId(questionarioId));
        stats.setRespostasHoje(respostaQuestionarioRepository.countByQuestionarioIdAndDtRespostaAfter(
                questionarioId, agora.toLocalDate().atStartOfDay()));
        stats.setRespostasUltimos7Dias(respostaQuestionarioRepository.countByQuestionarioIdAndDtRespostaAfter(
                questionarioId, agora.minusDays(7)));
        stats.setRespostasUltimos30Dias(respostaQuestionarioRepository.countByQuestionarioIdAndDtRespostaAfter(
                questionarioId, agora.minusDays(30)));

        // Tempo médio
        stats.setMediaTempoPreenchimentoSegundos(
                respostaQuestionarioRepository.avgTempoPreenchimento(questionarioId));

        // Período
        stats.setPrimeiraResposta(respostaQuestionarioRepository.findPrimeiraResposta(questionarioId));
        stats.setUltimaResposta(respostaQuestionarioRepository.findUltimaResposta(questionarioId));

        // Estatísticas por pergunta
        List<EstatisticasPerguntaDTO> estatisticasPerguntas = new ArrayList<>();
        for (Pergunta pergunta : questionario.getPerguntas()) {
            estatisticasPerguntas.add(calcularEstatisticasPergunta(pergunta));
        }
        stats.setEstatisticasPerguntas(estatisticasPerguntas);

        // Taxa de conclusão = respostas preenchidas / (totalRespostas * totalPerguntas)
        long totalPerguntas = questionario.getPerguntas().size();
        if (stats.getTotalRespostas() != null && stats.getTotalRespostas() > 0 && totalPerguntas > 0) {
            Long preenchidas = respostaPerguntaRepository
                    .countRespostasPreenchidasByQuestionario(questionarioId);
            long denominador = stats.getTotalRespostas() * totalPerguntas;
            stats.setTaxaConclusao(preenchidas != null ? (preenchidas * 100.0) / denominador : 0.0);
        }

        // Distribuição temporal — últimos 30 dias por dia
        List<Object[]> porDia = respostaQuestionarioRepository
                .countByQuestionarioIdGroupByDay(questionarioId, agora.minusDays(30));
        Map<String, Long> mapPorDia = new LinkedHashMap<>();
        for (Object[] row : porDia) {
            String data = row[0] != null ? row[0].toString() : null;
            Long count = ((Number) row[1]).longValue();
            if (data != null) mapPorDia.put(data, count);
        }
        stats.setRespostasPorDia(mapPorDia);

        // Distribuição por hora do dia (00–23)
        List<Object[]> porHora = respostaQuestionarioRepository
                .countByQuestionarioIdGroupByHour(questionarioId);
        Map<String, Long> mapPorHora = new LinkedHashMap<>();
        for (Object[] row : porHora) {
            Integer hora = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            mapPorHora.put(String.format("%02d", hora), count);
        }
        stats.setRespostasPorHora(mapPorHora);

        return stats;
    }

    @Transactional(readOnly = true)
    public RelatorioRespostasDTO gerarRelatorio(Long questionarioId, LocalDateTime inicio, LocalDateTime fim) {
        Questionario questionario = questionarioRepository.findById(questionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));

        RelatorioRespostasDTO relatorio = new RelatorioRespostasDTO();
        relatorio.setQuestionarioId(questionarioId);
        relatorio.setQuestionarioTitulo(questionario.getTitulo());
        relatorio.setDataGeracao(LocalDateTime.now());
        relatorio.setPeriodoInicio(inicio);
        relatorio.setPeriodoFim(fim);

        // Contagens
        relatorio.setTotalRespostas(respostaQuestionarioRepository.countByQuestionarioId(questionarioId));
        relatorio.setTotalClientes(respostaQuestionarioRepository.countDistinctClientesByQuestionarioId(questionarioId));
        relatorio.setTotalAgendamentosAvaliados(
                respostaQuestionarioRepository.countDistinctAgendamentosByQuestionarioId(questionarioId));

        // Estatísticas
        relatorio.setEstatisticas(obterEstatisticas(questionarioId));

        // Respostas do período
        List<RespostaQuestionario> respostas = respostaQuestionarioRepository
                .findByQuestionarioIdAndPeriodo(questionarioId, inicio, fim);
        relatorio.setRespostas(respostas.stream()
                .map(r -> enriquecerComNomes(new RespostaQuestionarioDTO(r)))
                .collect(Collectors.toList()));

        return relatorio;
    }

    private RespostaQuestionarioDTO enriquecerComNomes(RespostaQuestionarioDTO dto) {
        if (dto.getClienteId() != null) {
            clienteRepository.findById(dto.getClienteId())
                    .ifPresent(c -> dto.setClienteNome(c.getNomeCompleto()));
        }
        if (dto.getColaboradorId() != null) {
            funcionarioRepository.findById(dto.getColaboradorId())
                    .ifPresent(f -> dto.setColaboradorNome(f.getNomeCompleto()));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public Double calcularNPS(Long questionarioId, Long perguntaId) {
        List<Object[]> distribuicao = respostaPerguntaRepository.getDistribuicaoNotas(perguntaId);

        long promotores = 0;
        long neutros = 0;
        long detratores = 0;
        long total = 0;

        for (Object[] row : distribuicao) {
            Integer nota = (Integer) row[0];
            Long count = (Long) row[1];
            total += count;

            if (nota >= 9) {
                promotores += count;
            } else if (nota >= 7) {
                neutros += count;
            } else {
                detratores += count;
            }
        }

        if (total == 0) return null;

        return ((promotores * 100.0 / total) - (detratores * 100.0 / total));
    }

    // Métodos auxiliares

    private void validarRespostas(RespostaQuestionarioCreateDTO dto, Questionario questionario) {
        Map<Long, Pergunta> perguntasMap = questionario.getPerguntas().stream()
                .collect(Collectors.toMap(Pergunta::getId, Function.identity()));

        Set<Long> perguntasRespondidas = new HashSet<>();

        for (RespostaPerguntaCreateDTO respostaDTO : dto.getRespostas()) {
            Pergunta pergunta = perguntasMap.get(respostaDTO.getPerguntaId());

            if (pergunta == null) {
                throw new IllegalArgumentException(
                        "Pergunta com ID " + respostaDTO.getPerguntaId() + " não pertence a este questionário.");
            }

            validarRespostaPorTipo(respostaDTO, pergunta);
            perguntasRespondidas.add(respostaDTO.getPerguntaId());
        }

        // Verificar obrigatórias
        for (Pergunta pergunta : questionario.getPerguntas()) {
            if (pergunta.getObrigatoria() && !perguntasRespondidas.contains(pergunta.getId())) {
                throw new IllegalArgumentException(
                        "A pergunta '" + pergunta.getTexto() + "' é obrigatória.");
            }
        }
    }

    private void validarRespostaPorTipo(RespostaPerguntaCreateDTO resposta, Pergunta pergunta) {
        boolean temResposta = false;

        switch (pergunta.getTipo()) {
            case TEXTO_CURTO:
            case TEXTO_LONGO:
                temResposta = resposta.getRespostaTexto() != null &&
                        !resposta.getRespostaTexto().trim().isEmpty();
                if (temResposta) {
                    if (pergunta.getMinCaracteres() != null &&
                            resposta.getRespostaTexto().length() < pergunta.getMinCaracteres()) {
                        throw new IllegalArgumentException(
                                "Resposta deve ter no mínimo " + pergunta.getMinCaracteres() + " caracteres.");
                    }
                    if (pergunta.getMaxCaracteres() != null &&
                            resposta.getRespostaTexto().length() > pergunta.getMaxCaracteres()) {
                        throw new IllegalArgumentException(
                                "Resposta deve ter no máximo " + pergunta.getMaxCaracteres() + " caracteres.");
                    }
                }
                break;

            case NUMERO:
                temResposta = resposta.getRespostaNumero() != null;
                if (temResposta) {
                    if (pergunta.getMinValor() != null &&
                            resposta.getRespostaNumero().compareTo(pergunta.getMinValor()) < 0) {
                        throw new IllegalArgumentException("Valor deve ser no mínimo " + pergunta.getMinValor());
                    }
                    if (pergunta.getMaxValor() != null &&
                            resposta.getRespostaNumero().compareTo(pergunta.getMaxValor()) > 0) {
                        throw new IllegalArgumentException("Valor deve ser no máximo " + pergunta.getMaxValor());
                    }
                }
                break;

            case ESCALA:
                temResposta = resposta.getRespostaNumero() != null;
                if (temResposta) {
                    int min = pergunta.getEscalaMin() != null ? pergunta.getEscalaMin() : 1;
                    int max = pergunta.getEscalaMax() != null ? pergunta.getEscalaMax() : 10;
                    int valor = resposta.getRespostaNumero().intValue();
                    if (valor < min || valor > max) {
                        throw new IllegalArgumentException("Valor deve estar entre " + min + " e " + max);
                    }
                }
                break;

            case AVALIACAO_ESTRELAS:
                temResposta = resposta.getRespostaNumero() != null;
                if (temResposta) {
                    int estrelas = resposta.getRespostaNumero().intValue();
                    if (estrelas < 1 || estrelas > 5) {
                        throw new IllegalArgumentException("Avaliação deve ser entre 1 e 5 estrelas.");
                    }
                }
                break;

            case SELECAO_UNICA:
                temResposta = resposta.getRespostaOpcaoIds() != null &&
                        !resposta.getRespostaOpcaoIds().isEmpty();
                if (temResposta && resposta.getRespostaOpcaoIds().size() > 1) {
                    throw new IllegalArgumentException(
                            "Selecione apenas uma opção para a pergunta '" + pergunta.getTexto() + "'.");
                }
                if (temResposta) {
                    validarOpcoesExistentes(resposta.getRespostaOpcaoIds(), pergunta);
                }
                break;

            case SELECAO_MULTIPLA:
                temResposta = resposta.getRespostaOpcaoIds() != null &&
                        !resposta.getRespostaOpcaoIds().isEmpty();
                if (temResposta) {
                    validarOpcoesExistentes(resposta.getRespostaOpcaoIds(), pergunta);
                }
                break;

            case DATA:
                temResposta = resposta.getRespostaData() != null;
                break;

            case HORA:
                temResposta = resposta.getRespostaHora() != null;
                break;

            case SIM_NAO:
                temResposta = resposta.getRespostaTexto() != null &&
                        (resposta.getRespostaTexto().equalsIgnoreCase("Sim") ||
                                resposta.getRespostaTexto().equalsIgnoreCase("Não"));
                if (resposta.getRespostaTexto() != null && !temResposta) {
                    throw new IllegalArgumentException("Resposta deve ser 'Sim' ou 'Não'.");
                }
                break;

            case TERMO_CONSENTIMENTO:
                temResposta = Boolean.TRUE.equals(resposta.getAceitouTermo())
                        && resposta.getTextoTermoRenderizado() != null
                        && !resposta.getTextoTermoRenderizado().isBlank();

                if (Boolean.TRUE.equals(pergunta.getRequerAceiteExplicito())
                        && !Boolean.TRUE.equals(resposta.getAceitouTermo())) {
                    throw new IllegalArgumentException(
                            "Aceite obrigatório do termo: '" + pergunta.getTexto() + "'.");
                }
                if (Boolean.TRUE.equals(resposta.getAceitouTermo())
                        && (resposta.getTextoTermoRenderizado() == null
                            || resposta.getTextoTermoRenderizado().isBlank())) {
                    throw new IllegalArgumentException(
                            "textoTermoRenderizado é obrigatório quando o termo for aceito ('"
                                    + pergunta.getTexto() + "').");
                }
                break;

            case ASSINATURA:
                boolean temAssinaturaCliente = resposta.getAssinaturaClienteBase64() != null
                        && !resposta.getAssinaturaClienteBase64().isBlank();
                boolean temAssinaturaProfissional = resposta.getAssinaturaProfissionalBase64() != null
                        && !resposta.getAssinaturaProfissionalBase64().isBlank();
                temResposta = temAssinaturaCliente;

                if (Boolean.TRUE.equals(pergunta.getExigirAssinaturaProfissional())
                        && !temAssinaturaProfissional) {
                    if (Boolean.TRUE.equals(pergunta.getObrigatoria()) || temAssinaturaCliente) {
                        throw new IllegalArgumentException(
                                "Assinatura do profissional é obrigatória para '"
                                        + pergunta.getTexto() + "'.");
                    }
                }
                break;
        }

        if (pergunta.getObrigatoria() && !temResposta) {
            throw new IllegalArgumentException("A pergunta '" + pergunta.getTexto() + "' é obrigatória.");
        }
    }

    private void validarOpcoesExistentes(List<Long> opcaoIds, Pergunta pergunta) {
        Set<Long> opcoesValidas = pergunta.getOpcoes().stream()
                .map(OpcaoResposta::getId)
                .collect(Collectors.toSet());

        for (Long opcaoId : opcaoIds) {
            if (!opcoesValidas.contains(opcaoId)) {
                throw new IllegalArgumentException("Opção selecionada inválida: " + opcaoId);
            }
        }
    }

    private EstatisticasPerguntaDTO calcularEstatisticasPergunta(Pergunta pergunta) {
        EstatisticasPerguntaDTO stats = new EstatisticasPerguntaDTO();
        stats.setPerguntaId(pergunta.getId());
        stats.setPerguntaTexto(pergunta.getTexto());
        stats.setTipo(pergunta.getTipo());

        Long totalRespostas = respostaPerguntaRepository.countByPerguntaId(pergunta.getId());
        stats.setTotalRespostas(totalRespostas);
        stats.setRespostasEmBranco(respostaPerguntaRepository.countRespostasEmBranco(pergunta.getId()));

        // Estatísticas numéricas
        if (pergunta.getTipo() == TipoPergunta.NUMERO ||
                pergunta.getTipo() == TipoPergunta.ESCALA ||
                pergunta.getTipo() == TipoPergunta.AVALIACAO_ESTRELAS) {

            List<Object[]> numStatsResult = respostaPerguntaRepository.getEstatisticasNumericas(pergunta.getId());
            if (!numStatsResult.isEmpty()) {
                Object[] numStats = numStatsResult.get(0);
                if (numStats != null && numStats[0] != null) {
                    stats.setMedia(((Number) numStats[0]).doubleValue());
                    if (numStats[1] != null) stats.setValorMinimo(((Number) numStats[1]).doubleValue());
                    if (numStats[2] != null) stats.setValorMaximo(((Number) numStats[2]).doubleValue());
                    if (numStats[3] != null) stats.setDesvioPadrao(((Number) numStats[3]).doubleValue());
                }
            }

            // Distribuição de notas
            List<Object[]> distribuicao = respostaPerguntaRepository.getDistribuicaoNotas(pergunta.getId());
            Map<Integer, Long> distribuicaoMap = new HashMap<>();
            for (Object[] row : distribuicao) {
                distribuicaoMap.put((Integer) row[0], (Long) row[1]);
            }
            stats.setDistribuicaoNotas(distribuicaoMap);

            // Mediana e moda
            List<BigDecimal> ordenados = respostaPerguntaRepository
                    .findRespostasNumericasOrdenadas(pergunta.getId());
            if (!ordenados.isEmpty()) {
                stats.setMediana(calcularMediana(ordenados));
                stats.setModa(calcularModa(distribuicaoMap));
            }
        }

        // Estatísticas de seleção
        if (pergunta.getTipo() == TipoPergunta.SELECAO_UNICA ||
                pergunta.getTipo() == TipoPergunta.SELECAO_MULTIPLA) {

            List<Object[]> opcoesCounts = respostaPerguntaRepository.countOpcoesSelecionadas(pergunta.getId());
            List<EstatisticaOpcaoDTO> estatisticasOpcoes = new ArrayList<>();

            Map<Long, Long> contagemMap = new HashMap<>();
            long totalSelecoes = 0;

            for (Object[] row : opcoesCounts) {
                Long opcaoId = ((Number) row[0]).longValue();
                Long count = ((Number) row[1]).longValue();
                contagemMap.put(opcaoId, count);
                totalSelecoes += count;
            }

            for (OpcaoResposta opcao : pergunta.getOpcoes()) {
                Long count = contagemMap.getOrDefault(opcao.getId(), 0L);
                double percentual = totalSelecoes > 0 ? (count * 100.0 / totalSelecoes) : 0;

                estatisticasOpcoes.add(EstatisticaOpcaoDTO.builder()
                        .opcaoId(opcao.getId())
                        .opcaoTexto(opcao.getTexto())
                        .totalSelecoes(count)
                        .percentual(percentual)
                        .build());
            }

            stats.setEstatisticasOpcoes(estatisticasOpcoes);
        }

        // Estatísticas Sim/Não
        if (pergunta.getTipo() == TipoPergunta.SIM_NAO) {
            List<Object[]> simNaoCounts = respostaPerguntaRepository.countSimNao(pergunta.getId());
            for (Object[] row : simNaoCounts) {
                String resposta = (String) row[0];
                Long count = (Long) row[1];
                if ("Sim".equalsIgnoreCase(resposta)) {
                    stats.setTotalSim(count);
                } else {
                    stats.setTotalNao(count);
                }
            }
            long total = (stats.getTotalSim() != null ? stats.getTotalSim() : 0) +
                    (stats.getTotalNao() != null ? stats.getTotalNao() : 0);
            if (total > 0 && stats.getTotalSim() != null) {
                stats.setPercentualSim((stats.getTotalSim() * 100.0) / total);
            }
        }

        // Estatísticas de texto
        if (pergunta.getTipo() == TipoPergunta.TEXTO_CURTO ||
                pergunta.getTipo() == TipoPergunta.TEXTO_LONGO) {
            stats.setMediaCaracteres(respostaPerguntaRepository.avgCaracteresTexto(pergunta.getId()));
            List<String> textos = respostaPerguntaRepository.findRespostasTexto(pergunta.getId());
            stats.setPalavrasFrequentes(extrairPalavrasFrequentes(textos));
        }

        return stats;
    }

    private Double calcularMediana(List<BigDecimal> ordenados) {
        int n = ordenados.size();
        if (n == 0) return null;
        if (n % 2 == 1) {
            return ordenados.get(n / 2).doubleValue();
        }
        BigDecimal a = ordenados.get(n / 2 - 1);
        BigDecimal b = ordenados.get(n / 2);
        return a.add(b).doubleValue() / 2.0;
    }

    private Double calcularModa(Map<Integer, Long> distribuicao) {
        return distribuicao.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().doubleValue())
                .orElse(null);
    }

    private List<String> extrairPalavrasFrequentes(List<String> textos) {
        if (textos == null || textos.isEmpty()) return Collections.emptyList();

        Map<String, Long> contagem = new HashMap<>();
        for (String texto : textos) {
            String[] palavras = texto.toLowerCase()
                    .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                    .split("\\s+");
            for (String p : palavras) {
                if (p.length() >= TAMANHO_MIN_PALAVRA && !STOPWORDS_PT.contains(p)) {
                    contagem.merge(p, 1L, Long::sum);
                }
            }
        }

        return contagem.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_PALAVRAS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
