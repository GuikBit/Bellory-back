package org.exemplo.bellory.service;

import lombok.RequiredArgsConstructor;

import org.exemplo.bellory.model.dto.produto.ProdutoCreateDTO;
import org.exemplo.bellory.model.dto.produto.ProdutoResponseDTO;
import org.exemplo.bellory.model.dto.produto.ProdutoResumoDTO;
import org.exemplo.bellory.model.dto.produto.ProdutoUpdateDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.event.EstoqueBaixoEvent;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.produtos.ProdutoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ApplicationEventPublisher eventPublisher;

    // =============== CRUD BÁSICO ===============

    public ProdutoResponseDTO criarProduto(ProdutoCreateDTO dto) {
        validarDadosCriacao(dto);

        Organizacao organizacao = organizacaoRepository.findById(dto.getOrganizacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        Produto produto = construirProdutoFromCreateDTO(dto, organizacao, categoria);
        produto = produtoRepository.save(produto);

        return mapearParaResponseDTO(produto);
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Long id) {
        Produto produto = buscarProdutoOuFalhar(id);
        return mapearParaResponseDTO(produto);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> listarProdutos(int page, int size, String sortBy, String sortDir,
                                                   String nome, Long categoriaId, String marca,
                                                   Boolean ativo, Boolean destaque) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Produto> produtosPage = produtoRepository.findAllWithFilters(
                nome, categoriaId, marca, ativo, destaque, pageable);

        return produtosPage.map(this::mapearParaResponseDTO);
    }

    @Transactional
    public List<ProdutoResponseDTO> getProdutos(){
        List<Produto> produtos = produtoRepository.findAll();
        return produtos.stream().map(this::mapearParaResponseDTO).collect(Collectors.toList());
    }

    public ProdutoResponseDTO atualizarProduto(Long id, ProdutoUpdateDTO dto) {
        Produto produto = buscarProdutoOuFalhar(id);

        atualizarDadosProduto(produto, dto);
        produto = produtoRepository.save(produto);

        return mapearParaResponseDTO(produto);
    }

    public void deletarProduto(Long id) {
        Produto produto = buscarProdutoOuFalhar(id);
        produto.inativar();
        produtoRepository.save(produto);
    }

    // =============== FUNCIONALIDADES ESPECÍFICAS ===============

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> buscarPorCategoria(Long categoriaId) {
        List<Produto> produtos = produtoRepository.findByCategoriaIdAndAtivo(categoriaId, true);
        return produtos.stream()
                .map(this::mapearParaResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> buscarProdutosDestaque() {
        List<Produto> produtos = produtoRepository.findByDestaqueAndAtivo(true, true);
        return produtos.stream()
                .map(this::mapearParaResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> buscarPorFaixaPreco(BigDecimal precoMinimo, BigDecimal precoMaximo) {
        List<Produto> produtos = produtoRepository.findByPrecoRange(precoMinimo, precoMaximo);
        return produtos.stream()
                .map(this::mapearParaResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> pesquisarProdutos(String termo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Produto> produtos = produtoRepository.searchProducts(termo, pageable);
        return produtos.map(this::mapearParaResponseDTO);
    }

    // =============== GERENCIAMENTO DE ESTOQUE ===============

    public ProdutoResponseDTO adicionarEstoque(Long id, Integer quantidade) {
        Produto produto = buscarProdutoOuFalhar(id);
        produto.adicionarEstoque(quantidade);
        produto = produtoRepository.save(produto);
        return mapearParaResponseDTO(produto);
    }

    public ProdutoResponseDTO removerEstoque(Long id, Integer quantidade) {
        Produto produto = buscarProdutoOuFalhar(id);
        if (!produto.temEstoqueDisponivel(quantidade)) {
            throw new IllegalArgumentException("Estoque insuficiente. Disponível: " + produto.getQuantidadeEstoque());
        }
        produto.removerEstoque(quantidade);
        produto = produtoRepository.save(produto);

        // Publicar evento de estoque baixo se abaixo do minimo
        if (produto.estoqueAbaixoDoMinimo() && produto.getOrganizacao() != null) {
            eventPublisher.publishEvent(new EstoqueBaixoEvent(
                    this,
                    produto.getId(),
                    produto.getNome(),
                    produto.getQuantidadeEstoque(),
                    produto.getOrganizacao().getId()
            ));
        }

        return mapearParaResponseDTO(produto);
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> buscarEstoqueBaixo(int limite) {
        List<Produto> produtos = produtoRepository.findByEstoqueBaixo(limite);
        return produtos.stream()
                .map(this::mapearParaResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> buscarSemEstoque() {
        List<Produto> produtos = produtoRepository.findSemEstoque();
        return produtos.stream()
                .map(this::mapearParaResponseDTO)
                .collect(Collectors.toList());
    }

    // =============== GERENCIAMENTO DE IMAGENS ===============

    public ProdutoResponseDTO adicionarImagem(Long id, String urlImagem) {
        Produto produto = buscarProdutoOuFalhar(id);
        produto.adicionarImagem(urlImagem);
        produto = produtoRepository.save(produto);
        return mapearParaResponseDTO(produto);
    }

    public ProdutoResponseDTO removerImagem(Long id, String urlImagem) {
        Produto produto = buscarProdutoOuFalhar(id);
        produto.removerImagem(urlImagem);
        produto = produtoRepository.save(produto);
        return mapearParaResponseDTO(produto);
    }

    // =============== AÇÕES DE STATUS ===============

    public ProdutoResponseDTO ativarProduto(Long id) {
        Produto produto = buscarProdutoOuFalhar(id);
        produto.ativar();
        produto = produtoRepository.save(produto);
        return mapearParaResponseDTO(produto);
    }

    public ProdutoResponseDTO inativarProduto(Long id) {
        Produto produto = buscarProdutoOuFalhar(id);
        produto.inativar();
        produto = produtoRepository.save(produto);
        return mapearParaResponseDTO(produto);
    }

    public ProdutoResponseDTO descontinuarProduto(Long id) {
        Produto produto = buscarProdutoOuFalhar(id);
        produto.descontinuar();
        produto = produtoRepository.save(produto);
        return mapearParaResponseDTO(produto);
    }

    public ProdutoResponseDTO alternarDestaque(Long id) {
        Produto produto = buscarProdutoOuFalhar(id);
        produto.setDestaque(!produto.isDestaque());
        produto.setDtAtualizacao(LocalDateTime.now());
        produto = produtoRepository.save(produto);
        return mapearParaResponseDTO(produto);
    }

    // =============== PRODUTOS RELACIONADOS ===============

    public void adicionarProdutoRelacionado(Long produtoId, Long relacionadoId) {
        if (produtoId.equals(relacionadoId)) {
            throw new IllegalArgumentException("Um produto não pode ser relacionado a si mesmo");
        }

        Produto produto = buscarProdutoOuFalhar(produtoId);
        Produto relacionado = buscarProdutoOuFalhar(relacionadoId);

        produto.getProdutosRelacionados().add(relacionado);
        produtoRepository.save(produto);
    }

    public void removerProdutoRelacionado(Long produtoId, Long relacionadoId) {
        Produto produto = buscarProdutoOuFalhar(produtoId);
        Produto relacionado = buscarProdutoOuFalhar(relacionadoId);

        produto.getProdutosRelacionados().remove(relacionado);
        produtoRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> buscarProdutosRelacionados(Long id) {
        Produto produto = buscarProdutoOuFalhar(id);
        return produto.getProdutosRelacionados().stream()
                .map(this::mapearParaResponseDTO)
                .collect(Collectors.toList());
    }

    // =============== RELATÓRIOS E ESTATÍSTICAS ===============

    @Transactional(readOnly = true)
    public Map<String, Object> obterEstatisticas() {
        Map<String, Object> stats = new HashMap<>();

        Long totalProdutos = produtoRepository.count();
        Long produtosAtivos = produtoRepository.countByAtivo(true);
        Long produtosInativos = totalProdutos - produtosAtivos;
        Long produtosSemEstoque = produtoRepository.countSemEstoque();
        Long produtosEstoqueBaixo = produtoRepository.countEstoqueBaixo(10);

        BigDecimal valorTotalEstoque = produtoRepository.calcularValorTotalEstoque();

        stats.put("totalProdutos", totalProdutos);
        stats.put("produtosAtivos", produtosAtivos);
        stats.put("produtosInativos", produtosInativos);
        stats.put("produtosSemEstoque", produtosSemEstoque);
        stats.put("produtosEstoqueBaixo", produtosEstoqueBaixo);
        stats.put("valorTotalEstoque", valorTotalEstoque != null ? valorTotalEstoque : BigDecimal.ZERO);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> buscarMaisVendidos(int limite) {
        // Esta funcionalidade requer integração com sistema de vendas
        // Por enquanto, retorna produtos em destaque como placeholder
        List<Produto> produtos = produtoRepository.findTopByDestaqueOrderByTotalAvaliacoesDesc(limite);
        return produtos.stream()
                .map(this::mapearParaResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean existeCodigoBarras(String codigoBarras) {
        return produtoRepository.existsByCodigoBarras(codigoBarras);
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorCodigoBarras(String codigoBarras) {
        Produto produto = produtoRepository.findByCodigoBarras(codigoBarras)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com código de barras: " + codigoBarras));
        return mapearParaResponseDTO(produto);
    }

    public ProdutoResponseDTO duplicarProduto(Long id) {
        Produto original = buscarProdutoOuFalhar(id);

        Produto copia = Produto.builder()
                .organizacao(original.getOrganizacao())
                .nome(original.getNome() + " (Cópia)")
                .categoria(original.getCategoria())
                .genero(original.getGenero())
                .descricao(original.getDescricao())
                .preco(original.getPreco())
                .precoCusto(original.getPrecoCusto())
                .quantidadeEstoque(0)
                .estoqueMinimo(original.getEstoqueMinimo())
                .unidade(original.getUnidade())
                .marca(original.getMarca())
                .modelo(original.getModelo())
                .peso(original.getPeso())
                .descontoPercentual(original.getDescontoPercentual())
                .destaque(false)
                .ativo(false) // Produto duplicado inicia inativo
                .status(Produto.StatusProduto.INATIVO)
                .urlsImagens(new ArrayList<>(original.getUrlsImagens()))
                .ingredientes(original.getIngredientes() != null ? new ArrayList<>(original.getIngredientes()) : null)
                .comoUsar(original.getComoUsar() != null ? new ArrayList<>(original.getComoUsar()) : null)
                .especificacoes(original.getEspecificacoes() != null ? new HashMap<>(original.getEspecificacoes()) : null)
                .dtCriacao(LocalDateTime.now())
                .usuarioCriacao(original.getUsuarioCriacao())
                .build();

        copia = produtoRepository.save(copia);
        return mapearParaResponseDTO(copia);
    }

    // =============== MÉTODOS PRIVADOS DE APOIO ===============

    private Produto buscarProdutoOuFalhar(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com ID: " + id));
    }

    private void validarDadosCriacao(ProdutoCreateDTO dto) {
        if (dto.getCodigoBarras() != null && existeCodigoBarras(dto.getCodigoBarras())) {
            throw new IllegalArgumentException("Já existe um produto com este código de barras");
        }

        if (dto.getCodigoInterno() != null && produtoRepository.existsByCodigoInterno(dto.getCodigoInterno())) {
            throw new IllegalArgumentException("Já existe um produto com este código interno");
        }

        if (dto.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço deve ser maior que zero");
        }
    }

    private Produto construirProdutoFromCreateDTO(ProdutoCreateDTO dto, Organizacao organizacao, Categoria categoria) {
        return Produto.builder()
                .organizacao(organizacao)
                .nome(dto.getNome())
                .categoria(categoria)
                .genero(dto.getGenero())
                .destaque(dto.getDestaque() != null ? dto.getDestaque() : false)
                .descricao(dto.getDescricao())
                .codigoBarras(dto.getCodigoBarras())
                .codigoInterno(dto.getCodigoInterno())
                .preco(dto.getPreco())
                .precoCusto(dto.getPrecoCusto())
                .quantidadeEstoque(dto.getQuantidadeEstoque() != null ? dto.getQuantidadeEstoque() : 0)
                .estoqueMinimo(dto.getEstoqueMinimo() != null ? dto.getEstoqueMinimo() : 0)
                .unidade(dto.getUnidade())
                .marca(dto.getMarca())
                .modelo(dto.getModelo())
                .peso(dto.getPeso())
                .descontoPercentual(dto.getDescontoPercentual())
                .urlsImagens(dto.getUrlsImagens() != null ? new ArrayList<>(dto.getUrlsImagens()) : new ArrayList<>())
                .ingredientes(dto.getIngredientes() != null ? new ArrayList<>(dto.getIngredientes()) : null)
                .comoUsar(dto.getComoUsar() != null ? new ArrayList<>(dto.getComoUsar()) : null)
                .especificacoes(dto.getEspecificacoes() != null ? new HashMap<>(dto.getEspecificacoes()) : null)
                .usuarioCriacao(dto.getUsuarioCriacao())
                .dtCriacao(LocalDateTime.now())
                .ativo(true)
                .status(Produto.StatusProduto.ATIVO)
                .build();
    }

    private void atualizarDadosProduto(Produto produto, ProdutoUpdateDTO dto) {
        if (dto.getNome() != null) produto.setNome(dto.getNome());
        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
            produto.setCategoria(categoria);
        }
        if (dto.getGenero() != null) produto.setGenero(dto.getGenero());
        if (dto.getDestaque() != null) produto.setDestaque(dto.getDestaque());
        if (dto.getDescricao() != null) produto.setDescricao(dto.getDescricao());
        if (dto.getCodigoBarras() != null) {
            if (!dto.getCodigoBarras().equals(produto.getCodigoBarras()) && existeCodigoBarras(dto.getCodigoBarras())) {
                throw new IllegalArgumentException("Já existe um produto com este código de barras");
            }
            produto.setCodigoBarras(dto.getCodigoBarras());
        }
        if (dto.getPreco() != null) produto.setPreco(dto.getPreco());
        if (dto.getPrecoCusto() != null) produto.setPrecoCusto(dto.getPrecoCusto());
        if (dto.getQuantidadeEstoque() != null) produto.setQuantidadeEstoque(dto.getQuantidadeEstoque());
        if (dto.getEstoqueMinimo() != null) produto.setEstoqueMinimo(dto.getEstoqueMinimo());
        if (dto.getUnidade() != null) produto.setUnidade(dto.getUnidade());
        if (dto.getMarca() != null) produto.setMarca(dto.getMarca());
        if (dto.getModelo() != null) produto.setModelo(dto.getModelo());
        if (dto.getPeso() != null) produto.setPeso(dto.getPeso());
        if (dto.getDescontoPercentual() != null) produto.setDescontoPercentual(dto.getDescontoPercentual());
        if (dto.getUrlsImagens() != null) produto.setUrlsImagens(new ArrayList<>(dto.getUrlsImagens()));
        if (dto.getIngredientes() != null) produto.setIngredientes(new ArrayList<>(dto.getIngredientes()));
        if (dto.getComoUsar() != null) produto.setComoUsar(new ArrayList<>(dto.getComoUsar()));
        if (dto.getEspecificacoes() != null) produto.setEspecificacoes(new HashMap<>(dto.getEspecificacoes()));
        if (dto.getUsuarioAtualizacao() != null) produto.setUsuarioAtualizacao(dto.getUsuarioAtualizacao());

        produto.setDtAtualizacao(LocalDateTime.now());
    }

    private ProdutoResponseDTO mapearParaResponseDTO(Produto produto) {
        ProdutoResponseDTO dto = new ProdutoResponseDTO();

        // Campos básicos
        dto.setId(produto.getId());
        dto.setNome(produto.getNome());
        dto.setGenero(produto.getGenero());
        dto.setAvaliacao(produto.getAvaliacao());
        dto.setTotalAvaliacoes(produto.getTotalAvaliacoes());
        dto.setDescontoPercentual(produto.getDescontoPercentual());
        dto.setDestaque(produto.isDestaque());
        dto.setAtivo(produto.isAtivo());
        dto.setDescricao(produto.getDescricao());
        dto.setCodigoBarras(produto.getCodigoBarras());
        dto.setCodigoInterno(produto.getCodigoInterno());
        dto.setPreco(produto.getPreco());
        dto.setPrecoCusto(produto.getPrecoCusto());
        dto.setQuantidadeEstoque(produto.getQuantidadeEstoque());
        dto.setEstoqueMinimo(produto.getEstoqueMinimo());
        dto.setUnidade(produto.getUnidade());
        dto.setStatus(produto.getStatus());
        dto.setStatusDescricao(produto.getStatus().getDescricao());
        dto.setMarca(produto.getMarca());
        dto.setModelo(produto.getModelo());
        dto.setPeso(produto.getPeso());

        // Coleções
        dto.setUrlsImagens(produto.getUrlsImagens());
        dto.setIngredientes(produto.getIngredientes());
        dto.setComoUsar(produto.getComoUsar());
        dto.setEspecificacoes(produto.getEspecificacoes());

        // Datas e usuários
        dto.setDtCriacao(produto.getDtCriacao());
        dto.setDtAtualizacao(produto.getDtAtualizacao());
        dto.setUsuarioCriacao(produto.getUsuarioCriacao());
        dto.setUsuarioAtualizacao(produto.getUsuarioAtualizacao());

        // Relacionamentos
        if (produto.getOrganizacao() != null) {
            dto.setOrganizacaoId(produto.getOrganizacao().getId());
            dto.setNomeOrganizacao(produto.getOrganizacao().getNomeFantasia());
        }

        if (produto.getCategoria() != null) {
            dto.setCategoriaId(produto.getCategoria().getId());
            dto.setNomeCategoria(produto.getCategoria().getLabel());
        }

        // Campos calculados
        dto.setMargemLucro(produto.calcularMargem());
        dto.setTemEstoque(produto.temEstoque());
        dto.setEstoqueAbaixoMinimo(produto.estoqueAbaixoDoMinimo());

        // Preço com desconto
        if (produto.getDescontoPercentual() != null && produto.getDescontoPercentual() > 0) {
            BigDecimal desconto = produto.getPreco()
                    .multiply(BigDecimal.valueOf(produto.getDescontoPercentual()))
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            dto.setPrecoComDesconto(produto.getPreco().subtract(desconto));
        } else {
            dto.setPrecoComDesconto(produto.getPreco());
        }

        // Imagem principal
        if (produto.getUrlsImagens() != null && !produto.getUrlsImagens().isEmpty()) {
            dto.setImagemPrincipal(produto.getUrlsImagens().get(0));
        }

        return dto;
    }

    // =============== MÉTODOS PÚBLICOS ADICIONAIS ===============

    @Transactional(readOnly = true)
    public List<ProdutoResumoDTO> buscarProdutosResumo() {
        List<Produto> produtos = produtoRepository.findByAtivo(true);
        return produtos.stream()
                .map(this::mapearParaResumoDTO)
                .collect(Collectors.toList());
    }

    private ProdutoResumoDTO mapearParaResumoDTO(Produto produto) {
        ProdutoResumoDTO dto = new ProdutoResumoDTO();
        dto.setId(produto.getId());
        dto.setNome(produto.getNome());
        dto.setMarca(produto.getMarca());
        dto.setPreco(produto.getPreco());
        dto.setQuantidadeEstoque(produto.getQuantidadeEstoque());
        dto.setStatus(produto.getStatus().getDescricao());
        dto.setDestaque(produto.isDestaque());

        if (produto.getCategoria() != null) {
            dto.setNomeCategoria(produto.getCategoria().getLabel());
        }

        if (produto.getUrlsImagens() != null && !produto.getUrlsImagens().isEmpty()) {
            dto.setImagemPrincipal(produto.getUrlsImagens().get(0));
        }

        // Calcular preço com desconto
        if (produto.getDescontoPercentual() != null && produto.getDescontoPercentual() > 0) {
            BigDecimal desconto = produto.getPreco()
                    .multiply(BigDecimal.valueOf(produto.getDescontoPercentual()))
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            dto.setPrecoComDesconto(produto.getPreco().subtract(desconto));
        } else {
            dto.setPrecoComDesconto(produto.getPreco());
        }

        return dto;
    }
}
