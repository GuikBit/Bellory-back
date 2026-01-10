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

        return this.servicoRepository.findAllByOrganizacao_IdOrderByNomeAsc(organizacaoId);
    }

    public Servico getServicoById(Long id) {

        Servico servico = servicoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + id + " não encontrado."));

        validarOrganizacao(servico.getOrganizacao().getId());

        return servico;
    }

    public List<ServicoAgendamento> getListAgendamentoServicos() {
        Long organizacaoId = getOrganizacaoIdFromContext();
        return servicoRepository.findAllProjectedByOrganizacao_Id(organizacaoId);
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
        if (servicoRepository.existsByNome(dto.getNome())) {
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

        if (dto.getImagens() != null && !dto.getImagens().isEmpty()) {
            List<String> urlsImagensSalvas = new ArrayList<>();

            for (String imagemBase64 : dto.getImagens()) {
                if (imagemBase64 != null && !imagemBase64.isEmpty()) {
                    // Salvar imagem base64 e obter path relativo
                    String relativePath = fileStorageService.storeServiceImageFromBase64(
                            imagemBase64,
                            servicoSalvo.getId(),
                            organizacaoId
                    );

                    // Construir URL completa
                    String fullUrl = fileStorageService.getFileUrl(relativePath);
                    urlsImagensSalvas.add(fullUrl);
                }
            }

            servicoSalvo.setUrlsImagens(urlsImagensSalvas);
            servicoRepository.save(servicoSalvo);
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

        // PROCESSAR IMAGENS COM LOGS DETALHADOS
        System.out.println("🔍 ===== INÍCIO DO PROCESSAMENTO DE IMAGENS =====");
        System.out.println("📦 Total de imagens recebidas no DTO: " + (dto.getImagens() != null ? dto.getImagens().size() : 0));

        if (dto.getImagens() != null) {
            for (int i = 0; i < dto.getImagens().size(); i++) {
                String img = dto.getImagens().get(i);
                System.out.println("📸 Imagem [" + i + "] - Tamanho: " + (img != null ? img.length() : 0) + " chars");
                if (img != null && img.length() > 50) {
                    System.out.println("   Início: " + img.substring(0, 50) + "...");
                }
            }
        }

        if (dto.getImagens() != null && !dto.getImagens().isEmpty()) {
            List<String> urlsFinais = new ArrayList<>();

            for (int i = 0; i < dto.getImagens().size(); i++) {
                String imagem = dto.getImagens().get(i);

                System.out.println("\n🔄 Processando imagem [" + i + "]...");

                if (imagem == null || imagem.isEmpty()) {
                    System.out.println("⚠️ Imagem vazia ou nula, pulando...");
                    continue;
                }

                // Se for URL (começa com http), mantém a URL existente
                if (imagem.startsWith("http://") || imagem.startsWith("https://")) {
                    System.out.println("🔗 É uma URL existente: " + imagem);
                    urlsFinais.add(imagem);
                }
                // Se for base64, salva a nova imagem
                else if (imagem.startsWith("data:image/")) {
                    System.out.println("🖼️ É uma imagem base64, salvando...");
                    try {
                        String relativePath = fileStorageService.storeServiceImageFromBase64(
                                imagem,
                                servicoExistente.getId(),
                                organizacaoId
                        );

                        String fullUrl = fileStorageService.getFileUrl(relativePath);
                        urlsFinais.add(fullUrl);

                        System.out.println("✅ Nova imagem salva com sucesso!");
                        System.out.println("   Relative path: " + relativePath);
                        System.out.println("   Full URL: " + fullUrl);
                    } catch (Exception e) {
                        System.err.println("❌ ERRO ao salvar imagem: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("❓ Formato desconhecido de imagem. Primeiros 100 chars:");
                    System.out.println("   " + imagem.substring(0, Math.min(100, imagem.length())));
                }
            }

            System.out.println("\n📋 RESUMO:");
            System.out.println("   URLs antigas recebidas: " + dto.getImagens().stream().filter(img -> img != null && img.startsWith("http")).count());
            System.out.println("   Base64 novas recebidas: " + dto.getImagens().stream().filter(img -> img != null && img.startsWith("data:image/")).count());
            System.out.println("   Total de URLs finais: " + urlsFinais.size());
            System.out.println("   URLs finais:");
            for (int i = 0; i < urlsFinais.size(); i++) {
                System.out.println("      [" + i + "] " + urlsFinais.get(i));
            }

            servicoExistente.setUrlsImagens(urlsFinais);
        }

        System.out.println("🔍 ===== FIM DO PROCESSAMENTO DE IMAGENS =====\n");

        servicoExistente.setAtivo(dto.isAtivo());
        servicoExistente.setHome(dto.isHome());
        servicoExistente.setAvaliacao(dto.isAvaliacao());
        servicoExistente.setUsuarioAtualizacao(getUserIdFromContext().toString());
        servicoExistente.setDtAtualizacao(LocalDateTime.now());

        Servico servicoAtualizado = servicoRepository.save(servicoExistente);

        System.out.println("💾 Serviço salvo no banco!");
        System.out.println("   URLs de imagens no objeto salvo: " +
                (servicoAtualizado.getUrlsImagens() != null ? servicoAtualizado.getUrlsImagens().size() : 0));

        return servicoAtualizado;
    }

    @Transactional
    public void deleteServico(Long id) {

        Servico servico = getServicoById(id);
        validarOrganizacao(servico.getOrganizacao().getId());
        servico.setAtivo(false); // Soft delete

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
