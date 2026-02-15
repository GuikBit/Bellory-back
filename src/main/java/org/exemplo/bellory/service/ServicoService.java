package org.exemplo.bellory.service;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.dto.ServicoCreateDTO;
import org.exemplo.bellory.model.dto.ServicoDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final CategoriaRepository categoriaRepository; // Adicionado
    private final FileStorageService fileStorageService;

    public ServicoService(ServicoRepository servicoRepository, OrganizacaoRepository organizacaoRepository, CategoriaRepository categoriaRepository,FileStorageService fileStorageService) {
        this.servicoRepository = servicoRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.categoriaRepository = categoriaRepository; // Adicionado
        this.fileStorageService = fileStorageService;
    }

    public List<Servico> getListAllServicos() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return this.servicoRepository.findAllByOrganizacao_IdAndIsDeletadoFalseOrderByNomeAsc(organizacaoId);
    }

    public Servico getServicoById(Long id) {

        Servico servico = servicoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + id + " não encontrado."));

        validarOrganizacao(servico.getOrganizacao().getId());

        return servico;
    }

    public List<ServicoAgendamento> getListAgendamentoServicos() {
        Long organizacaoId = getOrganizacaoIdFromContext();
        return servicoRepository.findAllProjectedByOrganizacao_IdAndIsDeletadoFalse(organizacaoId);
    }

    @Transactional
    public Servico createServico(ServicoCreateDTO dto) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        // Validação de campos obrigatórios
//        if (dto.getOrganizacaoId() == null) {
//            throw new IllegalArgumentException("O ID da organização é obrigatório.");
//        }
        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do serviço é obrigatório.");
        }
        if (dto.getPreco() == null || dto.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço do serviço é obrigatório e deve ser maior que zero.");
        }
        if (dto.getTempoEstimadoMinutos() == null || dto.getTempoEstimadoMinutos() <= 0) {
            throw new IllegalArgumentException("A duração estimada do serviço é obrigatória.");
        }
        if (dto.getCategoria() == null) {
            throw new IllegalArgumentException("O ID da categoria é obrigatório.");
        }

        // Validação de unicidade do nome do serviço
        if (servicoRepository.existsByNomeAndOrganizacao_Id(dto.getNome(), dto.getOrganizacaoId())) {
            throw new IllegalArgumentException("Já existe um serviço com o nome '" + dto.getNome() + "'.");
        }

        Organizacao org = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização com ID " + dto.getOrganizacaoId() + " não encontrada."));

        validarOrganizacao(org.getId());

        Categoria categoria = categoriaRepository.findById(dto.getCategoria().getId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria com ID " + dto.getCategoria().getId() + " não encontrada."));

        validarOrganizacao(categoria.getOrganizacao().getId());

        Servico novoServico = new Servico();
        novoServico.setOrganizacao(org);
        novoServico.setNome(dto.getNome());
        novoServico.setCategoria(categoria);
        novoServico.setGenero(dto.getGenero());
        novoServico.setDescricao(dto.getDescricao());
        novoServico.setTempoEstimadoMinutos(dto.getTempoEstimadoMinutos());
        novoServico.setPreco(dto.getPreco());
        novoServico.setDesconto(dto.getDesconto());
        novoServico.setPreco(dto.getPreco());
        novoServico.setDesconto(dto.getDesconto());

// Calcula o valor do desconto: preco * (desconto / 100)
        BigDecimal valorDesconto = dto.getPreco()
                .multiply(dto.getDesconto())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal precoFinal = dto.getPreco().subtract(valorDesconto);
        novoServico.setPrecoFinal(precoFinal);
        novoServico.setProdutos(dto.getProdutos());
        novoServico.setAtivo(true);
        novoServico.setDtCriacao(LocalDateTime.now());

        Servico servicoSalvo = servicoRepository.save(novoServico);

        if (dto.getUrlsImagens() != null && !dto.getUrlsImagens().isEmpty()) {
            List<String> urlsImagensSalvas = new ArrayList<>();

            for (String imagem : dto.getUrlsImagens()) {
                if (imagem == null || imagem.isEmpty()) {
                    continue;
                }

                // Se for URL existente, mantém
                if (imagem.startsWith("http://") || imagem.startsWith("https://")) {
                    urlsImagensSalvas.add(imagem);
                }
                // Se for base64, salva a nova imagem
                else if (imagem.startsWith("data:image/") || !imagem.contains("://")) {
                    String relativePath = fileStorageService.storeServiceImageFromBase64(
                            imagem,
                            servicoSalvo.getId(),
                            organizacaoId
                    );
                    String fullUrl = fileStorageService.getFileUrl(relativePath);
                    urlsImagensSalvas.add(fullUrl);
                }
            }

            servicoSalvo.setUrlsImagens(urlsImagensSalvas);
            servicoSalvo = servicoRepository.save(servicoSalvo);
        }

        return servicoSalvo;
    }

    @Transactional
    public Map<String, Object> uploadImagensServico(Long servicoId, List<MultipartFile> imagens) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        // Verificar se serviço existe e pertence à organização
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + servicoId + " não encontrado."));

        validarOrganizacao(servico.getOrganizacao().getId());

        List<String> novasUrls = new ArrayList<>();
        List<String> urlsAtuais = servico.getUrlsImagens() != null ?
                new ArrayList<>(servico.getUrlsImagens()) : new ArrayList<>();

        // Processar novas imagens
        for (MultipartFile imagem : imagens) {
            if (!imagem.isEmpty()) {
                String relativePath = fileStorageService.storeServiceImage(
                        imagem,
                        servicoId,
                        organizacaoId
                );
                String fullUrl = fileStorageService.getFileUrl(relativePath);
                novasUrls.add(fullUrl);
                urlsAtuais.add(fullUrl);
            }
        }

        // Atualizar serviço
        servico.setUrlsImagens(urlsAtuais);
        servico.setDtCriacao(LocalDateTime.now()); // ou setDtUpdate se tiver
        servicoRepository.save(servico);

        Map<String, Object> response = new HashMap<>();
        response.put("servicoId", servicoId);
        response.put("novasImagens", novasUrls);
        response.put("totalImagens", urlsAtuais.size());

        return response;
    }

    @Transactional
    public void deleteImagemServico(Long servicoId, String imageUrl) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + servicoId + " não encontrado."));

        validarOrganizacao(servico.getOrganizacao().getId());

        List<String> urlsAtuais = servico.getUrlsImagens();
        if (urlsAtuais != null && urlsAtuais.contains(imageUrl)) {
            // Remover da lista
            urlsAtuais.remove(imageUrl);

            // Deletar arquivo físico
            String relativePath = fileStorageService.getRelativePathFromUrl(imageUrl);
            fileStorageService.deleteFile(relativePath, organizacaoId);

            // Atualizar serviço
            servico.setUrlsImagens(urlsAtuais);
            servicoRepository.save(servico);
        }
    }

    @Transactional
    public Servico updateServico(Long id, ServicoCreateDTO dto) {
        Servico servicoExistente = getServicoById(id);
        Long organizacaoId = servicoExistente.getOrganizacao().getId();

        validarOrganizacao(organizacaoId);

        if (dto.getNome() != null && !dto.getNome().trim().isEmpty()) {
            servicoExistente.setNome(dto.getNome());
        }
        if (dto.getCategoria() != null && dto.getCategoria().getId() != null) {
            Categoria categoria = categoriaRepository.findById(dto.getCategoria().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
            servicoExistente.setCategoria(categoria);
        }
        if (dto.getGenero() != null) {
            servicoExistente.setGenero(dto.getGenero());
        }
        if (dto.getDescricao() != null) {
            servicoExistente.setDescricao(dto.getDescricao());
        }
        if (dto.getTempoEstimadoMinutos() != null && dto.getTempoEstimadoMinutos() > 0) {
            servicoExistente.setTempoEstimadoMinutos(dto.getTempoEstimadoMinutos());
        }

        // Atualizar preço e desconto
        if (dto.getPreco() != null && dto.getPreco().compareTo(BigDecimal.ZERO) > 0) {
            servicoExistente.setPreco(dto.getPreco());
        }
        if (dto.getDesconto() != null) {
            servicoExistente.setDesconto(dto.getDesconto());

            // Recalcular preço final
            BigDecimal valorDesconto = servicoExistente.getPreco()
                    .multiply(dto.getDesconto())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal precoFinal = servicoExistente.getPreco().subtract(valorDesconto);
            servicoExistente.setPrecoFinal(precoFinal);
        }

        servicoExistente.setProdutos(dto.getProdutos());

        // Processar imagens
        if (dto.getUrlsImagens() != null) {
            if (dto.getUrlsImagens().isEmpty()) {
                // Lista vazia = usuario removeu todas as imagens
                servicoExistente.setUrlsImagens(new ArrayList<>());
            } else {
                List<String> urlsFinais = new ArrayList<>();

                for (int i = 0; i < dto.getUrlsImagens().size(); i++) {
                    String imagem = dto.getUrlsImagens().get(i);

                    if (imagem == null || imagem.isEmpty()) {
                        continue;
                    }

                    // Se for URL (começa com http), mantém a URL existente
                    if (imagem.startsWith("http://") || imagem.startsWith("https://")) {
                        urlsFinais.add(imagem);
                    }
                    // Se for base64, salva a nova imagem
                    else if (imagem.startsWith("data:image/") || !imagem.contains("://")) {
                        try {
                            String relativePath = fileStorageService.storeServiceImageFromBase64(
                                    imagem,
                                    servicoExistente.getId(),
                                    organizacaoId
                            );

                            String fullUrl = fileStorageService.getFileUrl(relativePath);
                            urlsFinais.add(fullUrl);
                        } catch (Exception e) {
                            System.err.println("Erro ao salvar imagem [" + i + "]: " + e.getMessage());
                        }
                    }
                }

                servicoExistente.setUrlsImagens(urlsFinais);
            }
        }

        servicoExistente.setAtivo(dto.isAtivo());
        servicoExistente.setHome(dto.isHome());
        servicoExistente.setAvaliacao(dto.isAvaliacao());
        servicoExistente.setUsuarioAtualizacao(getUserIdFromContext().toString());
        servicoExistente.setDtAtualizacao(LocalDateTime.now());

        return servicoRepository.save(servicoExistente);
    }

    @Transactional
    public void deleteServico(Long id) {

        Servico servico = getServicoById(id);
        validarOrganizacao(servico.getOrganizacao().getId());

        servico.setDeletado(true); // Soft delete
        servico.setUsuarioDeletado(getUserIdFromContext().toString());
        servico.setDtDeletado(LocalDateTime.now());

        servicoRepository.save(servico);
    }

    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }

        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
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
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }

        return userId;
    }
}
