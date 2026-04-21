package org.exemplo.bellory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.client.payment.dto.CachedCustomerStatus;
import org.exemplo.bellory.client.payment.dto.PaymentPlanLimitType;
import org.exemplo.bellory.client.payment.dto.PlanLimitDto;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.arquivo.ArquivoDTO;
import org.exemplo.bellory.model.dto.arquivo.PastaArquivoDTO;
import org.exemplo.bellory.model.dto.arquivo.StorageUsageDTO;
import org.exemplo.bellory.model.entity.arquivo.Arquivo;
import org.exemplo.bellory.model.entity.arquivo.PastaArquivo;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.arquivo.ArquivoRepository;
import org.exemplo.bellory.model.repository.arquivo.PastaArquivoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.service.assinatura.AssinaturaCacheService;
import org.exemplo.bellory.service.plano.LimiteValidatorService;
import org.exemplo.bellory.service.plano.LimiteValidatorService.TipoLimite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArquivoStorageService {

    private final ArquivoRepository arquivoRepository;
    private final PastaArquivoRepository pastaArquivoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final LimiteValidatorService limiteValidator;
    private final AssinaturaCacheService assinaturaCacheService;

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    private static final String ARQUIVOS_DIR = "arquivos";
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    /**
     * Pastas do sistema que aparecem na raiz do file explorer.
     * Nao podem ser deletadas, renomeadas ou ter arquivos movidos/deletados por este modulo.
     * A pasta "clientes" e excluida propositalmente.
     */
    private static final Map<String, String> PASTAS_SISTEMA = new LinkedHashMap<>();
    static {
        PASTAS_SISTEMA.put("colaboradores", "Colaboradores");
        PASTAS_SISTEMA.put("servicos", "Serviços");
        PASTAS_SISTEMA.put("produtos", "Produtos");
        PASTAS_SISTEMA.put("organizacao", "Organização");
        PASTAS_SISTEMA.put(ARQUIVOS_DIR, "Arquivos");
    }

    /**
     * Pastas protegidas = todas do sistema MENOS "arquivos" (onde o usuario cria conteudo).
     */
    private static final Set<String> PASTAS_PROTEGIDAS = Set.of(
            "colaboradores", "servicos", "produtos", "organizacao"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            // Imagens
            "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "ico",
            // Documentos
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
            // Texto
            "txt", "csv", "rtf", "md",
            // Compactados
            "zip", "rar", "7z", "tar", "gz",
            // Outros
            "json", "xml"
    );

    // ==================== ARQUIVOS ====================

    @Transactional
    public List<ArquivoDTO> uploadArquivos(List<MultipartFile> files, Long pastaId) {
        Long organizacaoId = getOrganizacaoId();
        Long userId = TenantContext.getCurrentUserId();

        // Verifica se o plano permite uso do modulo de arquivos
        limiteValidator.validarFeatureHabilitada(organizacaoId, TipoLimite.ARQUIVO);

        verificarLimiteStorage(organizacaoId, files);

        PastaArquivo pasta = null;
        if (pastaId != null) {
            pasta = pastaArquivoRepository.findByIdAndOrganizacao_Id(pastaId, organizacaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Pasta não encontrada."));
        }

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        List<ArquivoDTO> resultados = new ArrayList<>();

        for (MultipartFile file : files) {
            validarArquivo(file);
            ArquivoDTO dto = salvarArquivo(file, organizacao, pasta, userId);
            resultados.add(dto);
        }

        return resultados;
    }

    public List<ArquivoDTO> listarArquivos(Long pastaId) {
        Long organizacaoId = getOrganizacaoId();

        List<Arquivo> arquivos;
        if (pastaId != null) {
            pastaArquivoRepository.findByIdAndOrganizacao_Id(pastaId, organizacaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Pasta não encontrada."));
            arquivos = arquivoRepository.findAllByOrganizacao_IdAndPasta_IdOrderByDtCriacaoDesc(organizacaoId, pastaId);
        } else {
            arquivos = arquivoRepository.findAllByOrganizacao_IdAndPastaIsNullOrderByDtCriacaoDesc(organizacaoId);
        }

        return arquivos.stream().map(this::toArquivoDTO).toList();
    }

    public List<ArquivoDTO> listarTodosArquivos() {
        Long organizacaoId = getOrganizacaoId();
        List<Arquivo> arquivos = arquivoRepository.findAllByOrganizacao(organizacaoId);
        return arquivos.stream().map(this::toArquivoDTO).toList();
    }

    @Transactional
    public void deletarArquivo(Long arquivoId) {
        Long organizacaoId = getOrganizacaoId();

        Arquivo arquivo = arquivoRepository.findByIdAndOrganizacao_Id(arquivoId, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Arquivo não encontrado."));

        deletarArquivoFisico(arquivo.getCaminhoRelativo());
        arquivoRepository.delete(arquivo);
    }

    @Transactional
    public ArquivoDTO moverArquivo(Long arquivoId, Long novaPastaId) {
        Long organizacaoId = getOrganizacaoId();

        Arquivo arquivo = arquivoRepository.findByIdAndOrganizacao_Id(arquivoId, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Arquivo não encontrado."));

        PastaArquivo novaPasta = null;
        String novaPastaPath = "";
        if (novaPastaId != null) {
            novaPasta = pastaArquivoRepository.findByIdAndOrganizacao_Id(novaPastaId, organizacaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Pasta destino não encontrada."));
            novaPastaPath = novaPasta.getCaminhoCompleto() + "/";
        }

        String antigoCaminho = arquivo.getCaminhoRelativo();
        String novoCaminho = organizacaoId + "/" + ARQUIVOS_DIR + "/"
                + novaPastaPath + arquivo.getNomeArmazenado();

        try {
            Path origem = Paths.get(uploadDir, antigoCaminho).normalize();
            Path destino = Paths.get(uploadDir, novoCaminho).normalize();
            Files.createDirectories(destino.getParent());
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao mover arquivo: " + e.getMessage(), e);
        }

        arquivo.setPasta(novaPasta);
        arquivo.setCaminhoRelativo(novoCaminho);
        arquivoRepository.save(arquivo);

        return toArquivoDTO(arquivo);
    }

    @Transactional
    public ArquivoDTO renomearArquivo(Long arquivoId, String novoNome) {
        Long organizacaoId = getOrganizacaoId();

        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo é obrigatório.");
        }

        Arquivo arquivo = arquivoRepository.findByIdAndOrganizacao_Id(arquivoId, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Arquivo não encontrado."));

        String extensao = arquivo.getExtensao();
        String nomeBase = novoNome.contains(".") ? novoNome.substring(0, novoNome.lastIndexOf(".")) : novoNome;
        String novoNomeCompleto = sanitizeFilename(nomeBase) + "." + extensao;

        arquivo.setNomeOriginal(novoNomeCompleto);
        arquivoRepository.save(arquivo);

        return toArquivoDTO(arquivo);
    }

    // ==================== PASTAS ====================

    @Transactional
    public PastaArquivoDTO criarPasta(String nome, Long pastaPaiId) {
        Long organizacaoId = getOrganizacaoId();
        Long userId = TenantContext.getCurrentUserId();

        // Verifica se o plano permite uso do modulo de arquivos
        limiteValidator.validarFeatureHabilitada(organizacaoId, TipoLimite.ARQUIVO);

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da pasta é obrigatório.");
        }

        String nomeSanitizado = sanitizeFolderName(nome);

        // Nao permitir criar pasta com nome de pasta do sistema
        if (pastaPaiId == null && PASTAS_SISTEMA.containsKey(nomeSanitizado.toLowerCase())) {
            throw new IllegalArgumentException("Não é permitido criar pasta com este nome. Nome reservado pelo sistema.");
        }

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        PastaArquivo pastaPai = null;
        String caminhoCompleto;

        if (pastaPaiId != null) {
            pastaPai = pastaArquivoRepository.findByIdAndOrganizacao_Id(pastaPaiId, organizacaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Pasta pai não encontrada."));
            caminhoCompleto = pastaPai.getCaminhoCompleto() + "/" + nomeSanitizado;
        } else {
            caminhoCompleto = nomeSanitizado;
        }

        if (pastaArquivoRepository.existsByOrganizacao_IdAndCaminhoCompleto(organizacaoId, caminhoCompleto)) {
            throw new IllegalArgumentException("Já existe uma pasta com este nome neste local.");
        }

        Path pastaFisica = Paths.get(uploadDir, organizacaoId.toString(), ARQUIVOS_DIR, caminhoCompleto);
        try {
            Files.createDirectories(pastaFisica);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta no disco: " + e.getMessage(), e);
        }

        PastaArquivo pasta = PastaArquivo.builder()
                .organizacao(organizacao)
                .pastaPai(pastaPai)
                .nome(nomeSanitizado)
                .caminhoCompleto(caminhoCompleto)
                .criadoPor(userId)
                .build();

        pasta = pastaArquivoRepository.save(pasta);

        return toPastaDTO(pasta, false);
    }

    public List<PastaArquivoDTO> listarPastas(Long pastaPaiId) {
        Long organizacaoId = getOrganizacaoId();

        List<PastaArquivo> pastas;
        if (pastaPaiId != null) {
            pastas = pastaArquivoRepository.findAllByOrganizacao_IdAndPastaPai_IdOrderByNomeAsc(organizacaoId, pastaPaiId);
        } else {
            pastas = pastaArquivoRepository.findAllByOrganizacao_IdAndPastaPaiIsNullOrderByNomeAsc(organizacaoId);
        }

        return pastas.stream().map(p -> toPastaDTO(p, false)).toList();
    }

    public PastaArquivoDTO obterPasta(Long pastaId) {
        Long organizacaoId = getOrganizacaoId();

        PastaArquivo pasta = pastaArquivoRepository.findByIdAndOrganizacao_Id(pastaId, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Pasta não encontrada."));

        return toPastaDTO(pasta, true);
    }

    @Transactional
    public PastaArquivoDTO renomearPasta(Long pastaId, String novoNome) {
        Long organizacaoId = getOrganizacaoId();

        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException("Nome da pasta é obrigatório.");
        }

        PastaArquivo pasta = pastaArquivoRepository.findByIdAndOrganizacao_Id(pastaId, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Pasta não encontrada."));

        String novoNomeSanitizado = sanitizeFolderName(novoNome);

        String novoCaminho;
        if (pasta.getPastaPai() != null) {
            novoCaminho = pasta.getPastaPai().getCaminhoCompleto() + "/" + novoNomeSanitizado;
        } else {
            novoCaminho = novoNomeSanitizado;
        }

        if (pastaArquivoRepository.existsByOrganizacao_IdAndCaminhoCompleto(organizacaoId, novoCaminho)) {
            throw new IllegalArgumentException("Já existe uma pasta com este nome neste local.");
        }

        Path antigaPasta = Paths.get(uploadDir, organizacaoId.toString(), ARQUIVOS_DIR, pasta.getCaminhoCompleto());
        Path novaPasta = Paths.get(uploadDir, organizacaoId.toString(), ARQUIVOS_DIR, novoCaminho);
        try {
            if (Files.exists(antigaPasta)) {
                Files.move(antigaPasta, novaPasta, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao renomear pasta: " + e.getMessage(), e);
        }

        String caminhoAntigo = pasta.getCaminhoCompleto();
        pasta.setNome(novoNomeSanitizado);
        pasta.setCaminhoCompleto(novoCaminho);
        pastaArquivoRepository.save(pasta);

        atualizarCaminhosRecursivamente(pasta, caminhoAntigo, novoCaminho, organizacaoId);

        return toPastaDTO(pasta, false);
    }

    @Transactional
    public void deletarPasta(Long pastaId) {
        Long organizacaoId = getOrganizacaoId();

        PastaArquivo pasta = pastaArquivoRepository.findByIdAndOrganizacao_Id(pastaId, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Pasta não encontrada."));

        Path pastaFisica = Paths.get(uploadDir, organizacaoId.toString(), ARQUIVOS_DIR, pasta.getCaminhoCompleto());
        deletarDiretorioRecursivo(pastaFisica);

        pastaArquivoRepository.delete(pasta);
    }

    // ==================== STORAGE (calcula TODOS os arquivos no disco) ====================

    public StorageUsageDTO obterUsoStorage() {
        Long organizacaoId = getOrganizacaoId();

        // Calcular uso REAL do disco (todas as pastas, incluindo sistema)
        Path orgDir = Paths.get(uploadDir, organizacaoId.toString());
        long totalBytesDisco = calcularTamanhoDiretorio(orgDir);

        // Contar arquivos no disco (excluindo clientes)
        int totalArquivosDisco = contarArquivosDiretorio(orgDir);

        // Contar pastas do modulo (DB)
        Integer totalPastasModulo = pastaArquivoRepository.contarPastas(organizacaoId);

        // Contar pastas do sistema que existem no disco
        int totalPastasSistema = 0;
        for (String pastaSistema : PASTAS_SISTEMA.keySet()) {
            Path pastaSistemaPath = orgDir.resolve(pastaSistema);
            if (Files.exists(pastaSistemaPath) && Files.isDirectory(pastaSistemaPath)) {
                totalPastasSistema++;
            }
        }

        // Buscar limite do plano
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        Integer limiteMb = obterLimiteStorageMb(organizacao);

        Double percentualUsado = null;
        Boolean limiteAtingido = false;
        if (limiteMb != null) {
            long limiteBytes = (long) limiteMb * 1024 * 1024;
            percentualUsado = Math.round(((double) totalBytesDisco / limiteBytes) * 10000.0) / 100.0;
            limiteAtingido = totalBytesDisco >= limiteBytes;
        }

        return StorageUsageDTO.builder()
                .organizacaoId(organizacaoId)
                .totalBytes(totalBytesDisco)
                .totalFormatado(formatarTamanho(totalBytesDisco))
                .totalArquivos(totalArquivosDisco)
                .totalPastas(totalPastasModulo + totalPastasSistema)
                .limiteMb(limiteMb != null ? limiteMb.longValue() : null)
                .limiteFormatado(limiteMb != null ? formatarTamanho((long) limiteMb * 1024 * 1024) : "Ilimitado")
                .percentualUsado(percentualUsado)
                .limiteAtingido(limiteAtingido)
                .build();
    }

    // ==================== NAVEGACAO ====================

    /**
     * Navega pelo file explorer.
     *
     * pastaId = null          -> Raiz: mostra pastas do sistema + pasta "arquivos"
     * pastaSistema = "colaboradores" etc -> Lista arquivos do disco (somente leitura)
     * pastaId = Long          -> Pasta do modulo (DB), lista subpastas + arquivos
     */
    public PastaArquivoDTO navegarPasta(Long pastaId) {
        Long organizacaoId = getOrganizacaoId();

        if (pastaId == null) {
            return navegarRaiz(organizacaoId);
        }

        PastaArquivo pasta = pastaArquivoRepository.findByIdAndOrganizacao_Id(pastaId, organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Pasta não encontrada."));

        return toPastaDTO(pasta, true);
    }

    /**
     * Navega dentro de uma pasta do sistema (colaboradores, servicos, etc).
     * Leitura direta do disco. Arquivos retornados com sistema=true.
     */
    public PastaArquivoDTO navegarPastaSistema(String nomePasta) {
        Long organizacaoId = getOrganizacaoId();

        if (!PASTAS_SISTEMA.containsKey(nomePasta)) {
            throw new IllegalArgumentException("Pasta do sistema não encontrada: " + nomePasta);
        }

        // A pasta "arquivos" e tratada como raiz do modulo do usuario
        if (ARQUIVOS_DIR.equals(nomePasta)) {
            return navegarPastaArquivos(organizacaoId);
        }

        Path pastaFisica = Paths.get(uploadDir, organizacaoId.toString(), nomePasta);
        boolean isSistema = PASTAS_PROTEGIDAS.contains(nomePasta);

        List<ArquivoDTO> arquivos = listarArquivosDoDisco(pastaFisica, organizacaoId, nomePasta, isSistema);
        List<PastaArquivoDTO> subpastas = listarSubpastasDoDisco(pastaFisica, organizacaoId, nomePasta, isSistema);

        long tamanhoTotal = calcularTamanhoDiretorio(pastaFisica);

        return PastaArquivoDTO.builder()
                .id(null)
                .nome(PASTAS_SISTEMA.get(nomePasta))
                .caminhoCompleto(nomePasta)
                .pastaPaiId(null)
                .totalArquivos(arquivos.size())
                .tamanhoTotal(tamanhoTotal)
                .tamanhoTotalFormatado(formatarTamanho(tamanhoTotal))
                .subpastas(subpastas)
                .arquivos(arquivos)
                .sistema(isSistema)
                .build();
    }

    // ==================== METODOS PRIVADOS - NAVEGACAO ====================

    private PastaArquivoDTO navegarRaiz(Long organizacaoId) {
        Path orgDir = Paths.get(uploadDir, organizacaoId.toString());

        List<PastaArquivoDTO> pastasRaiz = new ArrayList<>();

        // Adicionar pastas do sistema que existem no disco
        for (Map.Entry<String, String> entry : PASTAS_SISTEMA.entrySet()) {
            String nomeDisco = entry.getKey();
            String nomeExibicao = entry.getValue();
            Path pastaPath = orgDir.resolve(nomeDisco);

            boolean isSistema = PASTAS_PROTEGIDAS.contains(nomeDisco);
            long tamanho = calcularTamanhoDiretorio(pastaPath);
            int totalArqs = contarArquivosDiretorio(pastaPath);

            // Pasta "arquivos" conta subpastas do DB + arquivos do DB
            if (ARQUIVOS_DIR.equals(nomeDisco)) {
                Integer totalArquivosDB = arquivoRepository.contarArquivos(organizacaoId);
                pastasRaiz.add(PastaArquivoDTO.builder()
                        .id(null)
                        .nome(nomeExibicao)
                        .caminhoCompleto(nomeDisco)
                        .pastaPaiId(null)
                        .totalArquivos(totalArquivosDB != null ? totalArquivosDB : 0)
                        .tamanhoTotal(tamanho)
                        .tamanhoTotalFormatado(formatarTamanho(tamanho))
                        .sistema(false)
                        .build());
            } else {
                pastasRaiz.add(PastaArquivoDTO.builder()
                        .id(null)
                        .nome(nomeExibicao)
                        .caminhoCompleto(nomeDisco)
                        .pastaPaiId(null)
                        .totalArquivos(totalArqs)
                        .tamanhoTotal(tamanho)
                        .tamanhoTotalFormatado(formatarTamanho(tamanho))
                        .sistema(isSistema)
                        .build());
            }
        }

        long tamanhoTotal = calcularTamanhoDiretorio(orgDir);

        return PastaArquivoDTO.builder()
                .id(null)
                .nome("Raiz")
                .caminhoCompleto("")
                .pastaPaiId(null)
                .totalArquivos(0)
                .tamanhoTotal(tamanhoTotal)
                .tamanhoTotalFormatado(formatarTamanho(tamanhoTotal))
                .subpastas(pastasRaiz)
                .arquivos(List.of())
                .sistema(false)
                .build();
    }

    /**
     * Navega dentro da pasta "arquivos" - mistura pastas do DB (usuario) com arquivos do DB na raiz.
     */
    private PastaArquivoDTO navegarPastaArquivos(Long organizacaoId) {
        List<PastaArquivo> pastasDB = pastaArquivoRepository
                .findAllByOrganizacao_IdAndPastaPaiIsNullOrderByNomeAsc(organizacaoId);
        List<Arquivo> arquivosDB = arquivoRepository
                .findAllByOrganizacao_IdAndPastaIsNullOrderByDtCriacaoDesc(organizacaoId);

        Path pastaFisica = Paths.get(uploadDir, organizacaoId.toString(), ARQUIVOS_DIR);
        long tamanhoTotal = calcularTamanhoDiretorio(pastaFisica);

        return PastaArquivoDTO.builder()
                .id(null)
                .nome("Arquivos")
                .caminhoCompleto(ARQUIVOS_DIR)
                .pastaPaiId(null)
                .totalArquivos(arquivosDB.size())
                .tamanhoTotal(tamanhoTotal)
                .tamanhoTotalFormatado(formatarTamanho(tamanhoTotal))
                .subpastas(pastasDB.stream().map(p -> toPastaDTO(p, false)).toList())
                .arquivos(arquivosDB.stream().map(this::toArquivoDTO).toList())
                .sistema(false)
                .build();
    }

    /**
     * Lista arquivos fisicos de um diretorio do disco (para pastas do sistema).
     */
    private List<ArquivoDTO> listarArquivosDoDisco(Path diretorio, Long organizacaoId, String pastaBase, boolean isSistema) {
        List<ArquivoDTO> arquivos = new ArrayList<>();

        if (!Files.exists(diretorio) || !Files.isDirectory(diretorio)) {
            return arquivos;
        }

        try (Stream<Path> paths = Files.list(diretorio)) {
            paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> {
                        try { return Files.getLastModifiedTime(p).toInstant(); }
                        catch (IOException e) { return java.time.Instant.MIN; }
                    }, Comparator.reverseOrder()))
                    .forEach(p -> {
                        try {
                            String nome = p.getFileName().toString();
                            String extensao = getFileExtension(nome);
                            long tamanho = Files.size(p);
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            LocalDateTime dtCriacao = LocalDateTime.ofInstant(
                                    attrs.creationTime().toInstant(), ZoneId.systemDefault());

                            String caminhoRelativo = organizacaoId + "/" + pastaBase + "/" + nome;

                            arquivos.add(ArquivoDTO.builder()
                                    .id(null)
                                    .nomeOriginal(nome)
                                    .extensao(extensao)
                                    .contentType(detectContentType(extensao))
                                    .tamanho(tamanho)
                                    .tamanhoFormatado(formatarTamanho(tamanho))
                                    .url(baseUrl + "/" + caminhoRelativo)
                                    .caminhoRelativo(caminhoRelativo)
                                    .dtCriacao(dtCriacao)
                                    .sistema(isSistema)
                                    .build());
                        } catch (IOException e) {
                            // ignora arquivo com erro
                        }
                    });
        } catch (IOException e) {
            // diretorio nao existe ou erro de leitura
        }

        return arquivos;
    }

    /**
     * Lista subpastas fisicas de um diretorio do disco (para pastas do sistema como organizacao/logo, organizacao/banner).
     */
    private List<PastaArquivoDTO> listarSubpastasDoDisco(Path diretorio, Long organizacaoId, String pastaBase, boolean isSistema) {
        List<PastaArquivoDTO> subpastas = new ArrayList<>();

        if (!Files.exists(diretorio) || !Files.isDirectory(diretorio)) {
            return subpastas;
        }

        try (Stream<Path> paths = Files.list(diretorio)) {
            paths.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(p -> {
                        String nome = p.getFileName().toString();
                        long tamanho = calcularTamanhoDiretorio(p);
                        int totalArqs = contarArquivosDiretorio(p);

                        subpastas.add(PastaArquivoDTO.builder()
                                .id(null)
                                .nome(nome)
                                .caminhoCompleto(pastaBase + "/" + nome)
                                .pastaPaiId(null)
                                .totalArquivos(totalArqs)
                                .tamanhoTotal(tamanho)
                                .tamanhoTotalFormatado(formatarTamanho(tamanho))
                                .sistema(isSistema)
                                .build());
                    });
        } catch (IOException e) {
            // ignora
        }

        return subpastas;
    }

    /**
     * Navega em subpasta de pasta do sistema (ex: organizacao/logo).
     */
    public PastaArquivoDTO navegarSubpastaSistema(String caminhoCompleto) {
        Long organizacaoId = getOrganizacaoId();

        // Validar que o caminho comeca com uma pasta do sistema
        String pastaPrincipal = caminhoCompleto.contains("/")
                ? caminhoCompleto.substring(0, caminhoCompleto.indexOf("/"))
                : caminhoCompleto;

        if (!PASTAS_SISTEMA.containsKey(pastaPrincipal)) {
            throw new IllegalArgumentException("Caminho inválido.");
        }

        boolean isSistema = PASTAS_PROTEGIDAS.contains(pastaPrincipal);

        Path pastaFisica = Paths.get(uploadDir, organizacaoId.toString(), caminhoCompleto);

        if (!Files.exists(pastaFisica) || !Files.isDirectory(pastaFisica)) {
            throw new IllegalArgumentException("Pasta não encontrada.");
        }

        String nomePasta = Paths.get(caminhoCompleto).getFileName().toString();
        List<ArquivoDTO> arquivos = listarArquivosDoDisco(pastaFisica, organizacaoId, caminhoCompleto, isSistema);
        List<PastaArquivoDTO> subpastas = listarSubpastasDoDisco(pastaFisica, organizacaoId, caminhoCompleto, isSistema);
        long tamanhoTotal = calcularTamanhoDiretorio(pastaFisica);

        return PastaArquivoDTO.builder()
                .id(null)
                .nome(nomePasta)
                .caminhoCompleto(caminhoCompleto)
                .pastaPaiId(null)
                .totalArquivos(arquivos.size())
                .tamanhoTotal(tamanhoTotal)
                .tamanhoTotalFormatado(formatarTamanho(tamanhoTotal))
                .subpastas(subpastas)
                .arquivos(arquivos)
                .sistema(isSistema)
                .build();
    }

    // ==================== METODOS PRIVADOS - UPLOAD/STORAGE ====================

    private ArquivoDTO salvarArquivo(MultipartFile file, Organizacao organizacao, PastaArquivo pasta, Long userId) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String contentType = file.getContentType();

            String nomeArmazenado = String.format("%d_%d.%s",
                    organizacao.getId(),
                    System.currentTimeMillis(),
                    extension);

            String pastaPath = "";
            if (pasta != null) {
                pastaPath = pasta.getCaminhoCompleto() + "/";
            }

            Path targetDirectory = Paths.get(uploadDir,
                    organizacao.getId().toString(),
                    ARQUIVOS_DIR,
                    pastaPath.isEmpty() ? "" : pastaPath);
            Files.createDirectories(targetDirectory);

            Path targetLocation = targetDirectory.resolve(nomeArmazenado);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String caminhoRelativo = organizacao.getId() + "/" + ARQUIVOS_DIR + "/" + pastaPath + nomeArmazenado;

            Arquivo arquivo = Arquivo.builder()
                    .organizacao(organizacao)
                    .pasta(pasta)
                    .nomeOriginal(originalFilename)
                    .nomeArmazenado(nomeArmazenado)
                    .caminhoRelativo(caminhoRelativo)
                    .extensao(extension)
                    .contentType(contentType)
                    .tamanho(file.getSize())
                    .criadoPor(userId)
                    .build();

            arquivo = arquivoRepository.save(arquivo);

            return toArquivoDTO(arquivo);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo: " + e.getMessage(), e);
        }
    }

    private void verificarLimiteStorage(Long organizacaoId, List<MultipartFile> files) {
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        Integer limiteMb = obterLimiteStorageMb(organizacao);

        if (limiteMb != null) {
            long limiteBytes = (long) limiteMb * 1024 * 1024;
            // Uso real do disco (todas as pastas)
            Path orgDir = Paths.get(uploadDir, organizacaoId.toString());
            long usoAtual = calcularTamanhoDiretorio(orgDir);
            long tamanhoNovosArquivos = files.stream().mapToLong(MultipartFile::getSize).sum();

            if (usoAtual + tamanhoNovosArquivos > limiteBytes) {
                String usoFormatado = formatarTamanho(usoAtual);
                String limiteFormatado = formatarTamanho(limiteBytes);
                throw new IllegalArgumentException(
                        String.format("Limite de armazenamento atingido. Uso atual: %s / Limite: %s",
                                usoFormatado, limiteFormatado));
            }
        }
    }

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo: 20MB.");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Formato não permitido: ." + extension
                    + ". Formatos aceitos: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private Integer obterLimiteStorageMb(Organizacao organizacao) {
        try {
            CachedCustomerStatus status = assinaturaCacheService.getCachedByOrganizacao(organizacao.getId());
            if (status == null || status.getLimits() == null) {
                log.debug("Sem cache de plano para org={}, storage sem limite", organizacao.getId());
                return null;
            }

            // Busca a key "arquivo" nos limites do plano
            return status.getLimits().stream()
                    .filter(l -> "arquivo".equalsIgnoreCase(l.getKey()))
                    .findFirst()
                    .map(limit -> {
                        if (limit.getType() == PaymentPlanLimitType.BOOLEAN) {
                            // BOOLEAN false = modulo desabilitado (0MB)
                            // BOOLEAN true = sem limite de storage definido
                            return Boolean.TRUE.equals(limit.getEnabled()) ? null : 0;
                        }
                        if (limit.getType() == PaymentPlanLimitType.UNLIMITED) {
                            return null; // sem limite
                        }
                        if (limit.getType() == PaymentPlanLimitType.NUMBER) {
                            // value = limite em MB
                            return limit.getValue() != null ? limit.getValue().intValue() : null;
                        }
                        return null;
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Erro ao consultar limite de storage para org={}: {}", organizacao.getId(), e.getMessage());
            return null; // fail-open
        }
    }

    // ==================== METODOS PRIVADOS - DISCO ====================

    /**
     * Calcula o tamanho total de um diretorio recursivamente.
     * Exclui a pasta "clientes" do calculo.
     */
    private long calcularTamanhoDiretorio(Path directory) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(p -> {
                        // Excluir pasta "clientes" do calculo
                        String rel = directory.relativize(p).toString().replace("\\", "/");
                        return !rel.startsWith("clientes/") && !rel.equals("clientes");
                    })
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); }
                        catch (IOException e) { return 0; }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Conta total de arquivos em um diretorio recursivamente.
     * Exclui a pasta "clientes".
     */
    private int contarArquivosDiretorio(Path directory) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            return (int) paths
                    .filter(p -> {
                        String rel = directory.relativize(p).toString().replace("\\", "/");
                        return !rel.startsWith("clientes/") && !rel.equals("clientes");
                    })
                    .filter(Files::isRegularFile)
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private void atualizarCaminhosRecursivamente(PastaArquivo pasta, String caminhoAntigo, String caminhoNovo, Long organizacaoId) {
        List<Arquivo> arquivos = arquivoRepository.findAllByOrganizacao_IdAndPasta_IdOrderByDtCriacaoDesc(organizacaoId, pasta.getId());
        for (Arquivo arquivo : arquivos) {
            String novoCaminho = arquivo.getCaminhoRelativo().replace(
                    ARQUIVOS_DIR + "/" + caminhoAntigo,
                    ARQUIVOS_DIR + "/" + caminhoNovo);
            arquivo.setCaminhoRelativo(novoCaminho);
            arquivoRepository.save(arquivo);
        }

        List<PastaArquivo> subpastas = pastaArquivoRepository
                .findAllByOrganizacao_IdAndPastaPai_IdOrderByNomeAsc(organizacaoId, pasta.getId());
        for (PastaArquivo subpasta : subpastas) {
            String antigoCaminhoSub = subpasta.getCaminhoCompleto();
            String novoCaminhoSub = antigoCaminhoSub.replace(caminhoAntigo, caminhoNovo);
            subpasta.setCaminhoCompleto(novoCaminhoSub);
            pastaArquivoRepository.save(subpasta);
            atualizarCaminhosRecursivamente(subpasta, antigoCaminhoSub, novoCaminhoSub, organizacaoId);
        }
    }

    private void deletarArquivoFisico(String caminhoRelativo) {
        try {
            Path filePath = Paths.get(uploadDir, caminhoRelativo).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log mas nao falha
        }
    }

    private void deletarDiretorioRecursivo(Path directory) {
        try {
            if (!Files.exists(directory)) return;
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); }
                            catch (IOException e) { /* ignora */ }
                        });
            }
        } catch (IOException e) {
            // Log mas nao falha
        }
    }

    // ==================== CONVERSORES ====================

    private ArquivoDTO toArquivoDTO(Arquivo arquivo) {
        return ArquivoDTO.builder()
                .id(arquivo.getId())
                .nomeOriginal(arquivo.getNomeOriginal())
                .extensao(arquivo.getExtensao())
                .contentType(arquivo.getContentType())
                .tamanho(arquivo.getTamanho())
                .tamanhoFormatado(formatarTamanho(arquivo.getTamanho()))
                .url(baseUrl + "/" + arquivo.getCaminhoRelativo())
                .caminhoRelativo(arquivo.getCaminhoRelativo())
                .pastaId(arquivo.getPasta() != null ? arquivo.getPasta().getId() : null)
                .pastaNome(arquivo.getPasta() != null ? arquivo.getPasta().getNome() : null)
                .dtCriacao(arquivo.getDtCriacao())
                .criadoPor(arquivo.getCriadoPor())
                .sistema(false)
                .build();
    }

    private PastaArquivoDTO toPastaDTO(PastaArquivo pasta, boolean incluirConteudo) {
        PastaArquivoDTO.PastaArquivoDTOBuilder builder = PastaArquivoDTO.builder()
                .id(pasta.getId())
                .nome(pasta.getNome())
                .caminhoCompleto(pasta.getCaminhoCompleto())
                .pastaPaiId(pasta.getPastaPai() != null ? pasta.getPastaPai().getId() : null)
                .totalArquivos(arquivoRepository.contarArquivosPorPasta(pasta.getId()))
                .tamanhoTotal(arquivoRepository.calcularTamanhoPasta(pasta.getId()))
                .tamanhoTotalFormatado(formatarTamanho(arquivoRepository.calcularTamanhoPasta(pasta.getId())))
                .dtCriacao(pasta.getDtCriacao())
                .dtAtualizacao(pasta.getDtAtualizacao())
                .sistema(false);

        if (incluirConteudo) {
            Long organizacaoId = getOrganizacaoId();
            List<PastaArquivo> subpastas = pastaArquivoRepository
                    .findAllByOrganizacao_IdAndPastaPai_IdOrderByNomeAsc(organizacaoId, pasta.getId());
            List<Arquivo> arquivos = arquivoRepository
                    .findAllByOrganizacao_IdAndPasta_IdOrderByDtCriacaoDesc(organizacaoId, pasta.getId());

            builder.subpastas(subpastas.stream().map(p -> toPastaDTO(p, false)).toList());
            builder.arquivos(arquivos.stream().map(this::toArquivoDTO).toList());
        }

        return builder.build();
    }

    // ==================== UTILITARIOS ====================

    private Long getOrganizacaoId() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no contexto.");
        }
        return organizacaoId;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "arquivo";
        return filename.replaceAll("[^a-zA-Z0-9._\\-\\s]", "_").trim();
    }

    private String sanitizeFolderName(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9_\\-\\s]", "_").trim();
    }

    private String detectContentType(String extension) {
        if (extension == null) return "application/octet-stream";
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    static String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
