package org.exemplo.bellory.service.cliente;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.exemplo.bellory.model.dto.clienteDTO.ImportacaoErroDTO;
import org.exemplo.bellory.model.entity.importacao.ClienteImportacao;
import org.exemplo.bellory.model.entity.importacao.StatusImportacao;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.event.ClientesImportadosEvent;
import org.exemplo.bellory.model.repository.importacao.ClienteImportacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Worker assincrono que consome o conteudo de um CSV e materializa os
 * clientes em transacoes individuais por linha.
 *
 * <p>Separado de {@link ClienteImportacaoService} para evitar self-invocation
 * (chamadas locais nao passam pelo proxy do Spring, ai {@code @Async} nao funciona).
 */
@Component
@Slf4j
public class ClienteImportacaoWorker {

    // Header obrigatorio do CSV. Ordem nao importa, mas todas as colunas devem existir.
    private static final List<String> HEADER_OBRIGATORIO =
            List.of("nomeCompleto", "telefone", "email", "cpf", "dataNascimento");

    private static final DateTimeFormatter FMT_DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SENHA_PADRAO = "cliente_rapido";
    private static final String EMAIL_PADRAO_VAZIO = "cliente_rapido@gmail.com";

    // Granularidade do flush de progresso (UPDATE no banco a cada N linhas).
    private static final int FLUSH_PROGRESSO_A_CADA = 20;
    // Tentativas de regerar username em caso de colisao com usuario existente.
    private static final int MAX_TENTATIVAS_USERNAME = 3;

    private final ClienteImportacaoRepository importacaoRepository;
    private final ClienteRepository clienteRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ClienteImportacaoWorker(ClienteImportacaoRepository importacaoRepository,
                                   ClienteRepository clienteRepository,
                                   OrganizacaoRepository organizacaoRepository,
                                   PasswordEncoder passwordEncoder,
                                   ApplicationEventPublisher eventPublisher,
                                   ObjectMapper objectMapper,
                                   PlatformTransactionManager txManager) {
        this.importacaoRepository = importacaoRepository;
        this.clienteRepository = clienteRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        // Cada linha em sua propria transacao - falha de uma nao derruba as outras.
        this.transactionTemplate = new TransactionTemplate(txManager);
        this.transactionTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async
    public void processar(Long importacaoId, byte[] conteudo, Long organizacaoId) {
        log.info("[Importacao] Iniciando processamento async da importacao {} (org {})",
                importacaoId, organizacaoId);

        ClienteImportacao registro = importacaoRepository.findById(importacaoId).orElse(null);
        if (registro == null) {
            log.error("[Importacao] Registro {} nao encontrado, abortando", importacaoId);
            return;
        }

        try {
            registro.setStatus(StatusImportacao.PROCESSANDO);
            importacaoRepository.save(registro);

            Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Organizacao " + organizacaoId + " nao encontrada"));

            executarImportacao(registro, organizacao, conteudo);

            registro.setStatus(StatusImportacao.CONCLUIDO);
            registro.setDtFim(LocalDateTime.now());
            importacaoRepository.save(registro);

            eventPublisher.publishEvent(new ClientesImportadosEvent(
                    this,
                    registro.getId(),
                    organizacaoId,
                    registro.getNomeArquivo(),
                    registro.getTotalLinhas(),
                    registro.getImportados(),
                    registro.getIgnorados()
            ));

            log.info("[Importacao] {} concluida: {} importados, {} ignorados de {} linhas",
                    importacaoId, registro.getImportados(), registro.getIgnorados(),
                    registro.getTotalLinhas());

        } catch (Exception e) {
            log.error("[Importacao] Falha fatal na importacao {}: {}", importacaoId, e.getMessage(), e);
            registro.setStatus(StatusImportacao.FALHA);
            registro.setMensagemFalha(e.getMessage());
            registro.setDtFim(LocalDateTime.now());
            try {
                importacaoRepository.save(registro);
            } catch (Exception persistEx) {
                log.error("[Importacao] Erro ao persistir status FALHA da importacao {}",
                        importacaoId, persistEx);
            }
        }
    }

    private void executarImportacao(ClienteImportacao registro,
                                    Organizacao organizacao,
                                    byte[] conteudo) throws Exception {
        char delimitador = detectarDelimitador(conteudo);
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimitador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        List<ImportacaoErroDTO> erros = new ArrayList<>();
        Set<String> telefonesNoCsv = new HashSet<>();

        try (Reader reader = new InputStreamReader(
                removerBomSeNecessario(new ByteArrayInputStream(conteudo)),
                StandardCharsets.UTF_8);
             CSVParser parser = formato.parse(reader)) {

            validarHeader(parser.getHeaderNames());

            int processadas = 0;
            int importados = 0;
            int ignorados = 0;

            for (CSVRecord record : parser) {
                // Numero da linha real no arquivo (1-based, considerando header como linha 1)
                int linhaNoArquivo = (int) record.getRecordNumber() + 1;
                processadas++;

                try {
                    importarLinha(record, organizacao, telefonesNoCsv);
                    importados++;
                } catch (Exception e) {
                    ignorados++;
                    erros.add(ImportacaoErroDTO.builder()
                            .linha(linhaNoArquivo)
                            .motivo(e.getMessage())
                            .build());
                    log.debug("[Importacao] Linha {} ignorada: {}", linhaNoArquivo, e.getMessage());
                }

                if (processadas % FLUSH_PROGRESSO_A_CADA == 0) {
                    atualizarProgresso(registro, processadas, importados, ignorados, erros);
                }
            }

            // Flush final (pega o resto que nao caiu no flush periodico).
            registro.setTotalLinhas(processadas);
            registro.setProcessadas(processadas);
            registro.setImportados(importados);
            registro.setIgnorados(ignorados);
            registro.setErros(serializarErros(erros));
        }
    }

    private void atualizarProgresso(ClienteImportacao registro,
                                    int processadas,
                                    int importados,
                                    int ignorados,
                                    List<ImportacaoErroDTO> erros) {
        try {
            registro.setProcessadas(processadas);
            registro.setImportados(importados);
            registro.setIgnorados(ignorados);
            registro.setErros(serializarErros(erros));
            importacaoRepository.save(registro);
        } catch (Exception e) {
            log.warn("[Importacao] Falha ao atualizar progresso da importacao {}: {}",
                    registro.getId(), e.getMessage());
        }
    }

    /**
     * Importa uma linha em transacao propria. Lanca {@link IllegalArgumentException}
     * com a mensagem de erro se a linha for invalida.
     */
    private void importarLinha(CSVRecord record, Organizacao organizacao, Set<String> telefonesNoCsv) {
        String nomeCompleto = obter(record, "nomeCompleto");
        String telefone = obter(record, "telefone");
        String email = obter(record, "email");
        String cpfRaw = obter(record, "cpf");
        String dataNascRaw = obter(record, "dataNascimento");

        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            throw new IllegalArgumentException("Nome obrigatorio");
        }
        if (telefone == null || telefone.isBlank()) {
            throw new IllegalArgumentException("Telefone obrigatorio");
        }

        // Telefone: normaliza so-digitos para deduplicar (mantem original ao salvar).
        String telefoneNorm = telefone.replaceAll("[^0-9]", "");
        if (!telefonesNoCsv.add(telefoneNorm)) {
            throw new IllegalArgumentException("Telefone " + telefone + " repetido no CSV");
        }

        String cpfLimpo = null;
        if (cpfRaw != null && !cpfRaw.isBlank()) {
            cpfLimpo = cpfRaw.replaceAll("[^0-9]", "");
            if (cpfLimpo.length() != 11) {
                throw new IllegalArgumentException("CPF invalido (esperado 11 digitos)");
            }
        }

        LocalDate dataNasc = null;
        if (dataNascRaw != null && !dataNascRaw.isBlank()) {
            dataNasc = parseData(dataNascRaw);
        }

        String emailFinal = (email == null || email.isBlank()) ? null : email.trim();

        Long orgId = organizacao.getId();
        if (clienteRepository.findByTelefoneAndOrganizacao_Id(telefone, orgId).isPresent()) {
            throw new IllegalArgumentException("Telefone " + telefone + " ja cadastrado");
        }
        if (cpfLimpo != null && clienteRepository.findByCpfAndOrganizacao_Id(cpfLimpo, orgId).isPresent()) {
            throw new IllegalArgumentException("CPF ja cadastrado");
        }
        if (emailFinal != null && clienteRepository.findByEmailAndOrganizacao_Id(emailFinal, orgId).isPresent()) {
            throw new IllegalArgumentException("Email " + emailFinal + " ja cadastrado");
        }

        String username = gerarUsernameUnico(orgId);

        // Cada cliente em transacao propria (REQUIRES_NEW).
        final String usernameFinal = username;
        final String emailParaSalvar = emailFinal != null ? emailFinal : EMAIL_PADRAO_VAZIO;
        final String cpfParaSalvar = cpfLimpo;
        final LocalDate dataParaSalvar = dataNasc;

        transactionTemplate.executeWithoutResult(status -> {
            Cliente cliente = new Cliente();
            cliente.setOrganizacao(organizacao);
            cliente.setNomeCompleto(nomeCompleto.trim());
            cliente.setEmail(emailParaSalvar);
            cliente.setCpf(cpfParaSalvar);
            cliente.setUsername(usernameFinal);
            cliente.setPassword(passwordEncoder.encode(SENHA_PADRAO));
            cliente.setTelefone(telefone.trim());
            cliente.setDataNascimento(dataParaSalvar);
            cliente.setRole("ROLE_CLIENTE");
            cliente.setAtivo(true);
            cliente.setIsCadastroIncompleto(true);
            cliente.setDtCriacao(LocalDateTime.now());
            clienteRepository.save(cliente);
        });
    }

    private String gerarUsernameUnico(Long organizacaoId) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS_USERNAME; tentativa++) {
            int random = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
            String candidato = "cliente_rapido_" + random;
            if (clienteRepository.findByUsernameAndOrganizacao_Id(candidato, organizacaoId).isEmpty()) {
                return candidato;
            }
        }
        throw new IllegalArgumentException("Nao foi possivel gerar username unico apos "
                + MAX_TENTATIVAS_USERNAME + " tentativas");
    }

    private LocalDate parseData(String raw) {
        String s = raw.trim();
        try {
            // ISO: 1990-05-12
            return LocalDate.parse(s);
        } catch (Exception ignored) {
        }
        try {
            // BR: 12/05/1990
            return LocalDate.parse(s, FMT_DATA_BR);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Data de nascimento invalida (use yyyy-MM-dd ou dd/MM/yyyy): " + s);
        }
    }

    private void validarHeader(List<String> headerEncontrado) {
        for (String obrigatoria : HEADER_OBRIGATORIO) {
            if (!headerEncontrado.contains(obrigatoria)) {
                throw new IllegalArgumentException(
                        "Header invalido: coluna '" + obrigatoria + "' ausente. "
                                + "Esperado: " + String.join(",", HEADER_OBRIGATORIO));
            }
        }
    }

    /**
     * Detecta o delimitador olhando a primeira linha do conteudo.
     * Aceita {@code ,} ou {@code ;}. Se ambos aparecerem, escolhe o de maior frequencia.
     */
    private char detectarDelimitador(byte[] conteudo) {
        String texto = new String(conteudo, StandardCharsets.UTF_8);
        int fimHeader = texto.indexOf('\n');
        String header = fimHeader < 0 ? texto : texto.substring(0, fimHeader);

        // Remove BOM (U+FEFF) se existir - Excel BR salva com BOM em UTF-8.
        if (!header.isEmpty() && header.charAt(0) == '﻿') {
            header = header.substring(1);
        }

        long virgulas = header.chars().filter(c -> c == ',').count();
        long pontosVirgula = header.chars().filter(c -> c == ';').count();

        if (pontosVirgula == 0 && virgulas == 0) {
            throw new IllegalArgumentException(
                    "CSV nao parece valido: header sem ',' nem ';' como separador");
        }
        return pontosVirgula >= virgulas ? ';' : ',';
    }

    private java.io.InputStream removerBomSeNecessario(java.io.InputStream in) throws java.io.IOException {
        java.io.PushbackInputStream pb = new java.io.PushbackInputStream(in, 3);
        byte[] bom = new byte[3];
        int lidos = pb.read(bom, 0, 3);
        if (lidos == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            return pb; // BOM consumido
        }
        if (lidos > 0) {
            pb.unread(bom, 0, lidos);
        }
        return pb;
    }

    private String obter(CSVRecord record, String coluna) {
        if (!record.isMapped(coluna)) {
            return null;
        }
        String v = record.get(coluna);
        return v == null ? null : v.trim();
    }

    private String serializarErros(List<ImportacaoErroDTO> erros) {
        if (erros == null || erros.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(erros);
        } catch (Exception e) {
            log.error("[Importacao] Falha ao serializar erros: {}", e.getMessage(), e);
            return "[]";
        }
    }

    /**
     * Helper estatico (usado pelo service ao montar o DTO de status) para
     * deserializar o JSONB persistido em lista de DTOs.
     */
    public static List<ImportacaoErroDTO> deserializarErros(ObjectMapper mapper, String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<ImportacaoErroDTO>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
