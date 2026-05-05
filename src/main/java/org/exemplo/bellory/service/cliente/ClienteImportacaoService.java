package org.exemplo.bellory.service.cliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.clienteDTO.ImportacaoErroDTO;
import org.exemplo.bellory.model.dto.clienteDTO.ImportacaoResumoDTO;
import org.exemplo.bellory.model.dto.clienteDTO.ImportacaoStatusDTO;
import org.exemplo.bellory.model.entity.importacao.ClienteImportacao;
import org.exemplo.bellory.model.entity.importacao.StatusImportacao;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.importacao.ClienteImportacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Facade da importacao de clientes via CSV.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>{@link #iniciar(MultipartFile)} cria o registro com status PENDENTE,
 *       persiste, e dispara o {@link ClienteImportacaoWorker} em thread async.</li>
 *   <li>O worker atualiza progresso periodico ate concluir.</li>
 *   <li>Frontend faz polling em {@link #getStatus(Long)} ate {@code CONCLUIDO}/{@code FALHA}.</li>
 * </ol>
 */
@Service
@Slf4j
public class ClienteImportacaoService {

    // 10 MB - limite duro para evitar OOM com CSVs absurdos.
    private static final long TAMANHO_MAX_BYTES = 10L * 1024 * 1024;

    private final ClienteImportacaoRepository importacaoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final ClienteImportacaoWorker worker;
    private final ObjectMapper objectMapper;

    public ClienteImportacaoService(ClienteImportacaoRepository importacaoRepository,
                                    OrganizacaoRepository organizacaoRepository,
                                    ClienteImportacaoWorker worker,
                                    ObjectMapper objectMapper) {
        this.importacaoRepository = importacaoRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.worker = worker;
        this.objectMapper = objectMapper;
    }

    /**
     * Cria o registro de importacao e dispara o worker async.
     *
     * <p><b>Sem {@code @Transactional}</b>: o save do {@code JpaRepository}
     * commita por conta propria (tx implicita), garantindo que o registro
     * esteja visivel quando o {@link ClienteImportacaoWorker#processar} rodar
     * em outra thread. Caso contrario, race condition entre commit e dispatch.
     */
    public ImportacaoStatusDTO iniciar(MultipartFile arquivo) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        Long usuarioId = TenantContext.getCurrentUserId();

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV obrigatorio.");
        }
        if (arquivo.getSize() > TAMANHO_MAX_BYTES) {
            throw new IllegalArgumentException(
                    "Arquivo excede o limite de 10 MB (" + arquivo.getSize() + " bytes).");
        }

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Falha ao ler o arquivo: " + e.getMessage());
        }

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organizacao " + organizacaoId + " nao encontrada"));

        ClienteImportacao registro = ClienteImportacao.builder()
                .organizacao(organizacao)
                .usuarioId(usuarioId)
                .nomeArquivo(arquivo.getOriginalFilename())
                .status(StatusImportacao.PENDENTE)
                .totalLinhas(0)
                .processadas(0)
                .importados(0)
                .ignorados(0)
                .erros("[]")
                .dtInicio(LocalDateTime.now())
                .build();

        ClienteImportacao salvo = importacaoRepository.save(registro);
        log.info("[Importacao] Criada importacao {} para org {} (arquivo {}, {} bytes)",
                salvo.getId(), organizacaoId, arquivo.getOriginalFilename(), arquivo.getSize());

        // Dispara worker async. O TenantContextTaskDecorator (em AsyncConfig) propaga
        // organizacaoId/userId para a thread async automaticamente.
        worker.processar(salvo.getId(), conteudo, organizacaoId);

        return toStatusDTO(salvo);
    }

    /**
     * Lista todas as importacoes da organizacao logada, mais recentes primeiro.
     * Retorna um resumo (sem o array de erros, que pode crescer muito) — quando
     * o frontend clicar em uma importacao, faz polling em {@link #getStatus(Long)}
     * para ver os detalhes completos.
     */
    @Transactional(readOnly = true)
    public List<ImportacaoResumoDTO> listarImportacoes() {
        Long organizacaoId = getOrganizacaoIdFromContext();
        return importacaoRepository.findByOrganizacaoIdOrderByDtInicioDesc(organizacaoId).stream()
                .map(this::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportacaoStatusDTO getStatus(Long importacaoId) {
        ClienteImportacao registro = importacaoRepository.findById(importacaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Importacao " + importacaoId + " nao encontrada"));

        Long organizacaoId = getOrganizacaoIdFromContext();
        if (!registro.getOrganizacao().getId().equals(organizacaoId)) {
            throw new IllegalArgumentException(
                    "Importacao " + importacaoId + " nao pertence a esta organizacao");
        }
        return toStatusDTO(registro);
    }

    private ImportacaoResumoDTO toResumoDTO(ClienteImportacao registro) {
        int total = registro.getTotalLinhas() != null ? registro.getTotalLinhas() : 0;
        int processadas = registro.getProcessadas() != null ? registro.getProcessadas() : 0;
        int percentual = total > 0 ? (int) Math.round(processadas * 100.0 / total) : 0;

        return ImportacaoResumoDTO.builder()
                .id(registro.getId())
                .status(registro.getStatus())
                .nomeArquivo(registro.getNomeArquivo())
                .totalLinhas(total)
                .processadas(processadas)
                .importados(registro.getImportados())
                .ignorados(registro.getIgnorados())
                .percentual(percentual)
                .mensagemFalha(registro.getMensagemFalha())
                .dtInicio(registro.getDtInicio())
                .dtFim(registro.getDtFim())
                .build();
    }

    private ImportacaoStatusDTO toStatusDTO(ClienteImportacao registro) {
        int total = registro.getTotalLinhas() != null ? registro.getTotalLinhas() : 0;
        int processadas = registro.getProcessadas() != null ? registro.getProcessadas() : 0;
        int percentual = total > 0 ? (int) Math.round(processadas * 100.0 / total) : 0;

        List<ImportacaoErroDTO> erros = ClienteImportacaoWorker.deserializarErros(
                objectMapper, registro.getErros());

        return ImportacaoStatusDTO.builder()
                .id(registro.getId())
                .status(registro.getStatus())
                .nomeArquivo(registro.getNomeArquivo())
                .totalLinhas(total)
                .processadas(processadas)
                .importados(registro.getImportados())
                .ignorados(registro.getIgnorados())
                .percentual(percentual)
                .erros(erros)
                .mensagemFalha(registro.getMensagemFalha())
                .dtInicio(registro.getDtInicio())
                .dtFim(registro.getDtFim())
                .build();
    }

    /**
     * Conteudo do template CSV oficial (header + linha de exemplo).
     * Inclui BOM UTF-8 para o Excel reconhecer acentos quando o usuario abrir o arquivo.
     */
    public byte[] gerarTemplateCsv() {
        String csv = "﻿nomeCompleto;telefone;email;cpf;dataNascimento\n"
                + "Maria Silva;(32) 99999-9999;maria@exemplo.com;123.456.789-00;1990-05-12\n"
                + "Joao Pereira;(11) 98888-8888;;;\n";
        return csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organizacao nao identificada. Token invalido ou expirado.");
        }
        return organizacaoId;
    }
}
