package org.exemplo.bellory.service.questionario;

import org.exemplo.bellory.model.dto.questionario.*;
import org.exemplo.bellory.model.entity.agendamento.StatusQuestionarioAgendamento;
import org.exemplo.bellory.model.entity.questionario.*;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoQuestionarioRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.questionario.*;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
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

    public RespostaQuestionarioService(QuestionarioRepository questionarioRepository,
                                       RespostaQuestionarioRepository respostaQuestionarioRepository,
                                       RespostaPerguntaRepository respostaPerguntaRepository,
                                       ClienteRepository clienteRepository,
                                       FuncionarioRepository funcionarioRepository,
                                       AgendamentoQuestionarioRepository agendamentoQuestionarioRepository) {
        this.questionarioRepository = questionarioRepository;
        this.respostaQuestionarioRepository = respostaQuestionarioRepository;
        this.respostaPerguntaRepository = respostaPerguntaRepository;
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.agendamentoQuestionarioRepository = agendamentoQuestionarioRepository;
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

        // Tracking: se a resposta veio para um agendamento específico, marca o
        // AgendamentoQuestionario correspondente como RESPONDIDO.
        if (saved.getAgendamentoId() != null) {
            agendamentoQuestionarioRepository
                    .findByAgendamentoIdAndQuestionarioId(saved.getAgendamentoId(), questionario.getId())
                    .ifPresent(aq -> {
                        aq.setStatus(StatusQuestionarioAgendamento.RESPONDIDO);
                        aq.setDtResposta(LocalDateTime.now());
                        aq.setRespostaQuestionarioId(saved.getId());
                        agendamentoQuestionarioRepository.save(aq);
                    });
        }

        return enriquecerComNomes(new RespostaQuestionarioDTO(saved));
    }

    @Transactional(readOnly = true)
    public RespostaQuestionarioDTO buscarPorId(Long id) {
        RespostaQuestionario resposta = respostaQuestionarioRepository.findByIdWithRespostas(id)
                .orElseThrow(() -> new IllegalArgumentException("Resposta não encontrada."));
        return enriquecerComNomes(new RespostaQuestionarioDTO(resposta));
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
        if (!respostaQuestionarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Resposta não encontrada.");
        }
        respostaQuestionarioRepository.deleteById(id);
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
