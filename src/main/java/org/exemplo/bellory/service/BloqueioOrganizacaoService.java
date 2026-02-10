package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.BloqueioOrganizacaoCreateDTO;
import org.exemplo.bellory.model.dto.BloqueioOrganizacaoDTO;
import org.exemplo.bellory.model.dto.BloqueioOrganizacaoUpdateDTO;
import org.exemplo.bellory.model.entity.organizacao.*;
import org.exemplo.bellory.model.repository.organizacao.BloqueioOrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BloqueioOrganizacaoService {

    private final BloqueioOrganizacaoRepository bloqueioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final WebClient webClient;

    public BloqueioOrganizacaoService(BloqueioOrganizacaoRepository bloqueioRepository,
                                       OrganizacaoRepository organizacaoRepository,
                                       WebClient.Builder webClientBuilder) {
        this.bloqueioRepository = bloqueioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.webClient = webClientBuilder.baseUrl("https://brasilapi.com.br").build();
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }
        return organizacaoId;
    }

    /**
     * Lista todos os bloqueios da organização
     */
    public List<BloqueioOrganizacaoDTO> listarTodos() {
        Long orgId = getOrganizacaoIdFromContext();
        return bloqueioRepository.findByOrganizacaoIdOrderByDataInicioAsc(orgId)
                .stream()
                .map(BloqueioOrganizacaoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Lista apenas bloqueios ativos da organização
     */
    public List<BloqueioOrganizacaoDTO> listarAtivos() {
        Long orgId = getOrganizacaoIdFromContext();
        return bloqueioRepository.findByOrganizacaoIdAndAtivoTrueOrderByDataInicioAsc(orgId)
                .stream()
                .map(BloqueioOrganizacaoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Busca um bloqueio pelo ID
     */
    public BloqueioOrganizacaoDTO buscarPorId(Long id) {
        Long orgId = getOrganizacaoIdFromContext();

        BloqueioOrganizacao bloqueio = bloqueioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio com ID " + id + " não encontrado."));

        if (!bloqueio.getOrganizacao().getId().equals(orgId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }

        return new BloqueioOrganizacaoDTO(bloqueio);
    }

    /**
     * Lista bloqueios ativos para um período (usado pelo calendário do front-end)
     */
    public List<BloqueioOrganizacaoDTO> listarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        Long orgId = getOrganizacaoIdFromContext();
        return bloqueioRepository.findBloqueiosAtivosNoPeriodo(orgId, dataInicio, dataFim)
                .stream()
                .map(BloqueioOrganizacaoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Verifica se uma data específica está bloqueada para a organização
     */
    public boolean isDataBloqueada(Long organizacaoId, LocalDate data) {
        List<BloqueioOrganizacao> bloqueios = bloqueioRepository.findBloqueiosAtivosNaData(organizacaoId, data);
        return !bloqueios.isEmpty();
    }

    /**
     * Retorna a lista de bloqueios que impedem uma data específica
     */
    public List<BloqueioOrganizacao> getBloqueiosNaData(Long organizacaoId, LocalDate data) {
        return bloqueioRepository.findBloqueiosAtivosNaData(organizacaoId, data);
    }

    /**
     * Cria um novo bloqueio manual
     */
    @Transactional
    public BloqueioOrganizacaoDTO criar(BloqueioOrganizacaoCreateDTO dto) {
        Long orgId = getOrganizacaoIdFromContext();

        // Validações
        if (dto.getTitulo() == null || dto.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("O título é obrigatório.");
        }
        if (dto.getDataInicio() == null) {
            throw new IllegalArgumentException("A data de início é obrigatória.");
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        BloqueioOrganizacao bloqueio = new BloqueioOrganizacao();
        bloqueio.setOrganizacao(organizacao);
        bloqueio.setTitulo(dto.getTitulo().trim());
        bloqueio.setDataInicio(dto.getDataInicio());
        bloqueio.setDataFim(dto.getDataFim() != null ? dto.getDataFim() : dto.getDataInicio());
        bloqueio.setDescricao(dto.getDescricao());
        bloqueio.setAtivo(true);
        bloqueio.setOrigem(OrigemBloqueio.MANUAL);

        // Definir tipo
        if (dto.getTipo() != null) {
            try {
                bloqueio.setTipo(TipoBloqueioOrganizacao.valueOf(dto.getTipo().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo inválido. Use FERIADO ou BLOQUEIO.");
            }
        } else {
            bloqueio.setTipo(TipoBloqueioOrganizacao.BLOQUEIO);
        }

        // Validar datas
        if (bloqueio.getDataFim().isBefore(bloqueio.getDataInicio())) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início.");
        }

        BloqueioOrganizacao salvo = bloqueioRepository.save(bloqueio);
        return new BloqueioOrganizacaoDTO(salvo);
    }

    /**
     * Atualiza um bloqueio existente
     */
    @Transactional
    public BloqueioOrganizacaoDTO atualizar(Long id, BloqueioOrganizacaoUpdateDTO dto) {
        Long orgId = getOrganizacaoIdFromContext();

        BloqueioOrganizacao bloqueio = bloqueioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio com ID " + id + " não encontrado."));

        if (!bloqueio.getOrganizacao().getId().equals(orgId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }

        if (dto.getTitulo() != null && !dto.getTitulo().trim().isEmpty()) {
            bloqueio.setTitulo(dto.getTitulo().trim());
        }
        if (dto.getDataInicio() != null) {
            bloqueio.setDataInicio(dto.getDataInicio());
        }
        if (dto.getDataFim() != null) {
            bloqueio.setDataFim(dto.getDataFim());
        }
        if (dto.getTipo() != null) {
            try {
                bloqueio.setTipo(TipoBloqueioOrganizacao.valueOf(dto.getTipo().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo inválido. Use FERIADO ou BLOQUEIO.");
            }
        }
        if (dto.getDescricao() != null) {
            bloqueio.setDescricao(dto.getDescricao());
        }
        if (dto.getAtivo() != null) {
            bloqueio.setAtivo(dto.getAtivo());
        }

        // Validar datas
        if (bloqueio.getDataFim().isBefore(bloqueio.getDataInicio())) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início.");
        }

        BloqueioOrganizacao salvo = bloqueioRepository.save(bloqueio);
        return new BloqueioOrganizacaoDTO(salvo);
    }

    /**
     * Alterna o status ativo/inativo de um bloqueio
     */
    @Transactional
    public BloqueioOrganizacaoDTO toggleAtivo(Long id) {
        Long orgId = getOrganizacaoIdFromContext();

        BloqueioOrganizacao bloqueio = bloqueioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio com ID " + id + " não encontrado."));

        if (!bloqueio.getOrganizacao().getId().equals(orgId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }

        bloqueio.setAtivo(!bloqueio.getAtivo());
        BloqueioOrganizacao salvo = bloqueioRepository.save(bloqueio);
        return new BloqueioOrganizacaoDTO(salvo);
    }

    /**
     * Remove um bloqueio
     */
    @Transactional
    public void remover(Long id) {
        Long orgId = getOrganizacaoIdFromContext();

        BloqueioOrganizacao bloqueio = bloqueioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueio com ID " + id + " não encontrado."));

        if (!bloqueio.getOrganizacao().getId().equals(orgId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }

        bloqueioRepository.delete(bloqueio);
    }

    /**
     * Importa feriados nacionais de um ano específico via BrasilAPI.
     * Apenas importa feriados que ainda não existem para a organização.
     *
     * @param ano Ano para importar os feriados (ex: 2025)
     * @return Quantidade de feriados importados
     */
    @Transactional
    public int importarFeriadosNacionais(Integer ano) {
        Long orgId = getOrganizacaoIdFromContext();

        if (ano == null) {
            ano = LocalDate.now().getYear();
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        // Buscar feriados da BrasilAPI
        List<Map> feriados;
        try {
            final int anoFinal = ano;
            feriados = webClient.get()
                    .uri("/api/feriados/v1/{ano}", anoFinal)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar feriados nacionais da API externa: " + e.getMessage());
        }

        if (feriados == null || feriados.isEmpty()) {
            return 0;
        }

        int importados = 0;
        for (Map<String, Object> feriado : feriados) {
            String nome = (String) feriado.get("name");
            String dataStr = (String) feriado.get("date");
            LocalDate data = LocalDate.parse(dataStr);

            // Verificar se já existe
            boolean jaExiste = bloqueioRepository.existsFeriadoNacional(orgId, nome, data, ano);
            if (jaExiste) {
                continue;
            }

            BloqueioOrganizacao bloqueio = new BloqueioOrganizacao();
            bloqueio.setOrganizacao(organizacao);
            bloqueio.setTitulo(nome);
            bloqueio.setDataInicio(data);
            bloqueio.setDataFim(data);
            bloqueio.setTipo(TipoBloqueioOrganizacao.FERIADO);
            bloqueio.setOrigem(OrigemBloqueio.NACIONAL);
            bloqueio.setAnoReferencia(ano);
            bloqueio.setAtivo(true);

            String tipoFeriado = (String) feriado.get("type");
            bloqueio.setDescricao(tipoFeriado != null && tipoFeriado.equals("national")
                    ? "Feriado Nacional" : "Feriado");

            bloqueioRepository.save(bloqueio);
            importados++;
        }

        return importados;
    }
}
