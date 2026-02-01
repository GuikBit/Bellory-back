package org.exemplo.bellory.service.questionario;

import org.exemplo.bellory.model.dto.questionario.*;
import org.exemplo.bellory.model.entity.questionario.*;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;
import org.exemplo.bellory.model.repository.questionario.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RespostaQuestionarioService {

    private final QuestionarioRepository questionarioRepository;
    private final RespostaQuestionarioRepository respostaQuestionarioRepository;
    private final RespostaPerguntaRepository respostaPerguntaRepository;

    public RespostaQuestionarioService(QuestionarioRepository questionarioRepository,
                                       RespostaQuestionarioRepository respostaQuestionarioRepository,
                                       RespostaPerguntaRepository respostaPerguntaRepository) {
        this.questionarioRepository = questionarioRepository;
        this.respostaQuestionarioRepository = respostaQuestionarioRepository;
        this.respostaPerguntaRepository = respostaPerguntaRepository;
    }

    @Transactional
    public RespostaQuestionarioDTO registrar(RespostaQuestionarioCreateDTO dto, String ipOrigem) {
        Questionario questionario = questionarioRepository.findByIdWithPerguntas(dto.getQuestionarioId())
                .orElseThrow(() -> new IllegalArgumentException("Questionário não encontrado."));

        if (!questionario.getAtivo()) {
            throw new IllegalArgumentException("Este questionário não está mais aceitando respostas.");
        }

        // Verificar duplicidade (se não for anônimo)
        if (!questionario.getAnonimo() && dto.getClienteId() != null) {
            if (respostaQuestionarioRepository.existsByQuestionarioIdAndClienteId(
                    dto.getQuestionarioId(), dto.getClienteId())) {
                throw new IllegalArgumentException("Você já respondeu este questionário.");
            }
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

        // Mapear perguntas
        Map<Long, Pergunta> perguntasMap = questionario.getPerguntas().stream()
                .collect(Collectors.toMap(Pergunta::getId, Function.identity()));

        // Processar respostas
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

            respostaQuestionario.addResposta(respostaPergunta);
        }

        RespostaQuestionario saved = respostaQuestionarioRepository.save(respostaQuestionario);
        return new RespostaQuestionarioDTO(saved);
    }

    @Transactional(readOnly = true)
    public RespostaQuestionarioDTO buscarPorId(Long id) {
        RespostaQuestionario resposta = respostaQuestionarioRepository.findByIdWithRespostas(id)
                .orElseThrow(() -> new IllegalArgumentException("Resposta não encontrada."));
        return new RespostaQuestionarioDTO(resposta);
    }

    @Transactional
    public void deletar(Long id) {
        if (!respostaQuestionarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Resposta não encontrada.");
        }
        respostaQuestionarioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<RespostaQuestionarioDTO> listarPorQuestionario(Long questionarioId, Pageable pageable) {
        return respostaQuestionarioRepository.findByQuestionarioId(questionarioId, pageable)
                .map(RespostaQuestionarioDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<RespostaQuestionarioDTO> listarPorQuestionarioEPeriodo(
            Long questionarioId, LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {
        return respostaQuestionarioRepository.findByQuestionarioIdAndPeriodoPaged(
                questionarioId, inicio, fim, pageable)
                .map(RespostaQuestionarioDTO::new);
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
                .map(RespostaQuestionarioDTO::new)
                .collect(Collectors.toList()));

        return relatorio;
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
                    BigDecimal valorResposta = BigDecimal.valueOf(resposta.getRespostaNumero());
                    if (pergunta.getMinValor() != null &&
                            valorResposta.compareTo(pergunta.getMinValor()) < 0) {
                        throw new IllegalArgumentException("Valor deve ser no mínimo " + pergunta.getMinValor());
                    }
                    if (pergunta.getMaxValor() != null &&
                            valorResposta.compareTo(pergunta.getMaxValor()) > 0) {
                        throw new IllegalArgumentException("Valor deve ser no máximo " + pergunta.getMaxValor());
                    }
                }
                break;

            case ESCALA:
                temResposta = resposta.getRespostaNumero() != null;
                if (temResposta) {
                    int min = pergunta.getEscalaMin() != null ? pergunta.getEscalaMin() : 1;
                    int max = pergunta.getEscalaMax() != null ? pergunta.getEscalaMax() : 10;
                    if (resposta.getRespostaNumero() < min || resposta.getRespostaNumero() > max) {
                        throw new IllegalArgumentException("Valor deve estar entre " + min + " e " + max);
                    }
                }
                break;

            case AVALIACAO_ESTRELAS:
                temResposta = resposta.getRespostaNumero() != null;
                if (temResposta && (resposta.getRespostaNumero() < 1 || resposta.getRespostaNumero() > 5)) {
                    throw new IllegalArgumentException("Avaliação deve ser entre 1 e 5 estrelas.");
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

            Object[] numStats = respostaPerguntaRepository.getEstatisticasNumericas(pergunta.getId());
            if (numStats != null && numStats[0] != null) {
                stats.setMedia((Double) numStats[0]);
                stats.setValorMinimo((Double) numStats[1]);
                stats.setValorMaximo((Double) numStats[2]);
                if (numStats[3] != null) {
                    stats.setDesvioPadrao((Double) numStats[3]);
                }
            }

            // Distribuição de notas
            List<Object[]> distribuicao = respostaPerguntaRepository.getDistribuicaoNotas(pergunta.getId());
            Map<Integer, Long> distribuicaoMap = new HashMap<>();
            for (Object[] row : distribuicao) {
                distribuicaoMap.put((Integer) row[0], (Long) row[1]);
            }
            stats.setDistribuicaoNotas(distribuicaoMap);
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
        }

        return stats;
    }
}
