package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.exemplo.bellory.model.dto.produto.ProdutoCreateDTO;
import org.exemplo.bellory.model.dto.produto.ProdutoResponseDTO;
import org.exemplo.bellory.model.dto.produto.ProdutoUpdateDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.FuncionarioService;
import org.exemplo.bellory.service.ProdutoService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produto")
@Tag(name = "Produtos", description = "Gerenciamento de produtos, estoque, imagens e relacionados")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final FuncionarioService funcionarioService;

    public ProdutoController(ProdutoService produtoService, FuncionarioService funcionarioService) {
        this.produtoService = produtoService;
        this.funcionarioService = funcionarioService;
    }

    // =============== CRUD BÁSICO ===============

    /**
     * Criar novo produto
     */
    @Operation(summary = "Criar novo produto")
    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> criarProduto(@Valid @RequestBody ProdutoCreateDTO produtoCreateDTO) {
        ProdutoResponseDTO produto = produtoService.criarProduto(produtoCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(produto);
    }

    /**
     * Buscar produto por ID
     */
    @Operation(summary = "Buscar produto por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        ProdutoResponseDTO produto = produtoService.buscarPorId(id);
        return ResponseEntity.ok(produto);
    }

    /**
     * Listar todos os produtos com paginação
     */
//    @GetMapping
//    public ResponseEntity<Page<ProdutoResponseDTO>> listarProdutos(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "nome") String sortBy,
//            @RequestParam(defaultValue = "asc") String sortDir,
//            @RequestParam(required = false) String nome,
//            @RequestParam(required = false) Long categoriaId,
//            @RequestParam(required = false) String marca,
//            @RequestParam(required = false) Boolean ativo,
//            @RequestParam(required = false) Boolean destaque) {
//
//        Page<ProdutoResponseDTO> produtos = produtoService.listarProdutos(
//                page, size, sortBy, sortDir, nome, categoriaId, marca, ativo, destaque);
//        return ResponseEntity.ok(produtos);
//    }

    @Operation(summary = "Listar todos os produtos")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<ProdutoResponseDTO>>> listarProdutos() {

        List<ProdutoResponseDTO> produtoResponseDTOS = produtoService.getProdutos();

        if(produtoResponseDTOS.isEmpty()){
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body(ResponseAPI.<List<ProdutoResponseDTO>>builder()
                            .success(true)
                            .message("Nenhum produtos encontrado.")
                            .dados(produtoResponseDTOS)
                            .build());
        }

        return ResponseEntity
        .status(HttpStatus.OK)
        .body(ResponseAPI.<List<ProdutoResponseDTO>>builder()
                .success(true)
                .message("Lista de produtos recuperada com sucesso.")
                .dados(produtoResponseDTOS)
                .build());
    }

    /**
     * Atualizar produto
     */
    @Operation(summary = "Atualizar produto")
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizarProduto(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoUpdateDTO produtoUpdateDTO) {
        ProdutoResponseDTO produto = produtoService.atualizarProduto(id, produtoUpdateDTO);
        return ResponseEntity.ok(produto);
    }

    /**
     * Deletar produto (soft delete - apenas inativa)
     */
    @Operation(summary = "Deletar produto (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarProduto(@PathVariable Long id) {
        produtoService.deletarProduto(id);
        return ResponseEntity.noContent().build();
    }

    // =============== FUNCIONALIDADES ESPECÍFICAS ===============

    /**
     * Buscar produtos por categoria
     */
    @Operation(summary = "Buscar produtos por categoria")
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<ProdutoResponseDTO>> buscarPorCategoria(@PathVariable Long categoriaId) {
        List<ProdutoResponseDTO> produtos = produtoService.buscarPorCategoria(categoriaId);
        return ResponseEntity.ok(produtos);
    }

    /**
     * Buscar produtos em destaque
     */
    @Operation(summary = "Buscar produtos em destaque")
    @GetMapping("/destaque")
    public ResponseEntity<List<ProdutoResponseDTO>> buscarProdutosDestaque() {
        List<ProdutoResponseDTO> produtos = produtoService.buscarProdutosDestaque();
        return ResponseEntity.ok(produtos);
    }

    /**
     * Buscar produtos por faixa de preço
     */
    @Operation(summary = "Buscar produtos por faixa de preço")
    @GetMapping("/preco")
    public ResponseEntity<List<ProdutoResponseDTO>> buscarPorFaixaPreco(
            @RequestParam BigDecimal precoMinimo,
            @RequestParam BigDecimal precoMaximo) {
        List<ProdutoResponseDTO> produtos = produtoService.buscarPorFaixaPreco(precoMinimo, precoMaximo);
        return ResponseEntity.ok(produtos);
    }

    /**
     * Pesquisar produtos por termo (nome, descrição, marca)
     */
    @Operation(summary = "Pesquisar produtos por termo")
    @GetMapping("/pesquisar")
    public ResponseEntity<Page<ProdutoResponseDTO>> pesquisarProdutos(
            @RequestParam String termo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProdutoResponseDTO> produtos = produtoService.pesquisarProdutos(termo, page, size);
        return ResponseEntity.ok(produtos);
    }

    // =============== GERENCIAMENTO DE ESTOQUE ===============

    /**
     * Adicionar estoque
     */
    @Operation(summary = "Adicionar estoque ao produto")
    @PatchMapping("/{id}/estoque/adicionar")
    public ResponseEntity<ProdutoResponseDTO> adicionarEstoque(
            @PathVariable Long id,
            @RequestParam Integer quantidade) {
        ProdutoResponseDTO produto = produtoService.adicionarEstoque(id, quantidade);
        return ResponseEntity.ok(produto);
    }

    /**
     * Remover estoque
     */
    @Operation(summary = "Remover estoque do produto")
    @PatchMapping("/{id}/estoque/remover")
    public ResponseEntity<ProdutoResponseDTO> removerEstoque(
            @PathVariable Long id,
            @RequestParam Integer quantidade) {
        ProdutoResponseDTO produto = produtoService.removerEstoque(id, quantidade);
        return ResponseEntity.ok(produto);
    }

    /**
     * Produtos com estoque baixo
     */
    @Operation(summary = "Listar produtos com estoque baixo")
    @GetMapping("/estoque/baixo")
    public ResponseEntity<List<ProdutoResponseDTO>> produtosEstoqueBaixo(
            @RequestParam(defaultValue = "10") int limite) {
        List<ProdutoResponseDTO> produtos = produtoService.buscarEstoqueBaixo(limite);
        return ResponseEntity.ok(produtos);
    }

    /**
     * Produtos sem estoque
     */
    @Operation(summary = "Listar produtos sem estoque")
    @GetMapping("/estoque/zerado")
    public ResponseEntity<List<ProdutoResponseDTO>> produtosSemEstoque() {
        List<ProdutoResponseDTO> produtos = produtoService.buscarSemEstoque();
        return ResponseEntity.ok(produtos);
    }

    // =============== GERENCIAMENTO DE IMAGENS ===============

    /**
     * Adicionar imagem ao produto
     */
    @Operation(summary = "Adicionar imagem ao produto")
    @PostMapping("/{id}/imagens")
    public ResponseEntity<ProdutoResponseDTO> adicionarImagem(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String urlImagem = request.get("urlImagem");
        ProdutoResponseDTO produto = produtoService.adicionarImagem(id, urlImagem);
        return ResponseEntity.ok(produto);
    }

    /**
     * Remover imagem do produto
     */
    @Operation(summary = "Remover imagem do produto")
    @DeleteMapping("/{id}/imagens")
    public ResponseEntity<ProdutoResponseDTO> removerImagem(
            @PathVariable Long id,
            @RequestParam String urlImagem) {
        ProdutoResponseDTO produto = produtoService.removerImagem(id, urlImagem);
        return ResponseEntity.ok(produto);
    }

    // =============== AÇÕES DE STATUS ===============

    /**
     * Ativar produto
     */
    @Operation(summary = "Ativar produto")
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<ProdutoResponseDTO> ativarProduto(@PathVariable Long id) {
        ProdutoResponseDTO produto = produtoService.ativarProduto(id);
        return ResponseEntity.ok(produto);
    }

    /**
     * Inativar produto
     */
    @Operation(summary = "Inativar produto")
    @PatchMapping("/{id}/inativar")
    public ResponseEntity<ProdutoResponseDTO> inativarProduto(@PathVariable Long id) {
        ProdutoResponseDTO produto = produtoService.inativarProduto(id);
        return ResponseEntity.ok(produto);
    }

    /**
     * Descontinuar produto
     */
    @Operation(summary = "Descontinuar produto")
    @PatchMapping("/{id}/descontinuar")
    public ResponseEntity<ProdutoResponseDTO> descontinuarProduto(@PathVariable Long id) {
        ProdutoResponseDTO produto = produtoService.descontinuarProduto(id);
        return ResponseEntity.ok(produto);
    }

    /**
     * Marcar/desmarcar como destaque
     */
    @Operation(summary = "Alternar destaque do produto")
    @PatchMapping("/{id}/destaque")
    public ResponseEntity<ProdutoResponseDTO> alternarDestaque(@PathVariable Long id) {
        ProdutoResponseDTO produto = produtoService.alternarDestaque(id);
        return ResponseEntity.ok(produto);
    }

    // =============== PRODUTOS RELACIONADOS ===============

    /**
     * Adicionar produto relacionado
     */
    @Operation(summary = "Adicionar produto relacionado")
    @PostMapping("/{id}/relacionados/{relacionadoId}")
    public ResponseEntity<Void> adicionarProdutoRelacionado(
            @PathVariable Long id,
            @PathVariable Long relacionadoId) {
        produtoService.adicionarProdutoRelacionado(id, relacionadoId);
        return ResponseEntity.ok().build();
    }

    /**
     * Remover produto relacionado
     */
    @Operation(summary = "Remover produto relacionado")
    @DeleteMapping("/{id}/relacionados/{relacionadoId}")
    public ResponseEntity<Void> removerProdutoRelacionado(
            @PathVariable Long id,
            @PathVariable Long relacionadoId) {
        produtoService.removerProdutoRelacionado(id, relacionadoId);
        return ResponseEntity.ok().build();
    }

    /**
     * Buscar produtos relacionados
     */
    @Operation(summary = "Buscar produtos relacionados")
    @GetMapping("/{id}/relacionados")
    public ResponseEntity<List<ProdutoResponseDTO>> buscarProdutosRelacionados(@PathVariable Long id) {
        List<ProdutoResponseDTO> produtos = produtoService.buscarProdutosRelacionados(id);
        return ResponseEntity.ok(produtos);
    }

    // =============== RELATÓRIOS E ESTATÍSTICAS ===============

    /**
     * Dashboard - estatísticas gerais
     */
    @Operation(summary = "Estatísticas gerais de produtos")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        Map<String, Object> stats = produtoService.obterEstatisticas();
        return ResponseEntity.ok(stats);
    }

    /**
     * Produtos mais vendidos (requer integração com sistema de vendas)
     */
    @Operation(summary = "Listar produtos mais vendidos")
    @GetMapping("/mais-vendidos")
    public ResponseEntity<List<ProdutoResponseDTO>> produtosMaisVendidos(
            @RequestParam(defaultValue = "10") int limite) {
        List<ProdutoResponseDTO> produtos = produtoService.buscarMaisVendidos(limite);
        return ResponseEntity.ok(produtos);
    }

    /**
     * Validar código de barras
     */
    @Operation(summary = "Validar código de barras")
    @GetMapping("/validar-codigo-barras")
    public ResponseEntity<Map<String, Boolean>> validarCodigoBarras(@RequestParam String codigoBarras) {
        boolean existe = produtoService.existeCodigoBarras(codigoBarras);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    /**
     * Buscar por código de barras
     */
    @Operation(summary = "Buscar produto por código de barras")
    @GetMapping("/codigo-barras/{codigoBarras}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorCodigoBarras(@PathVariable String codigoBarras) {
        ProdutoResponseDTO produto = produtoService.buscarPorCodigoBarras(codigoBarras);
        return ResponseEntity.ok(produto);
    }

    /**
     * Duplicar produto (criar cópia)
     */
    @Operation(summary = "Duplicar produto")
    @PostMapping("/{id}/duplicar")
    public ResponseEntity<ProdutoResponseDTO> duplicarProduto(@PathVariable Long id) {
        ProdutoResponseDTO produto = produtoService.duplicarProduto(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(produto);
    }
}
