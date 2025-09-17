package org.exemplo.bellory.model.repository.produtos;

import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
//    Optional<Object> findByNomeAndOrganizacao(String nome, Organizacao organizacao);
//
//    List<Produto> findByAtivo(boolean ativo);
//
//    Long countByAtivo(boolean ativo);
//
//    @Query("SELECT p FROM Produto p WHERE p.quantidadeEstoque <= :limite AND p.ativo = true ORDER BY p.quantidadeEstoque ASC")
//    List<Produto> findByEstoqueBaixo(int limite);
//
//    @Query("SELECT p FROM Produto p WHERE p.quantidadeEstoque = 0 AND p.ativo = true")
//    List<Produto> findSemEstoque();

    // =============== CONSULTAS BÁSICAS ===============

    Optional<Object> findByNomeAndOrganizacao(String nome, Organizacao organizacao);

    List<Produto> findByAtivo(boolean ativo);

    Long countByAtivo(boolean ativo);

    List<Produto> findByCategoriaIdAndAtivo(Long categoriaId, boolean ativo);

    List<Produto> findByDestaqueAndAtivo(boolean destaque, boolean ativo);

    Optional<Produto> findByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoInterno(String codigoInterno);

    // =============== CONSULTAS DE ESTOQUE ===============

    @Query("SELECT p FROM Produto p WHERE p.quantidadeEstoque <= :limite AND p.ativo = true ORDER BY p.quantidadeEstoque ASC")
    List<Produto> findByEstoqueBaixo(@Param("limite") int limite);

    @Query("SELECT p FROM Produto p WHERE p.quantidadeEstoque = 0 AND p.ativo = true")
    List<Produto> findSemEstoque();

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.quantidadeEstoque = 0 AND p.ativo = true")
    Long countSemEstoque();

    @Query("SELECT COUNT(p) FROM Produto p WHERE p.quantidadeEstoque <= :limite AND p.ativo = true")
    Long countEstoqueBaixo(@Param("limite") int limite);

    @Query("SELECT COALESCE(SUM(p.preco * p.quantidadeEstoque), 0) FROM Produto p WHERE p.ativo = true")
    BigDecimal calcularValorTotalEstoque();

    // =============== CONSULTAS DE PREÇO ===============

    @Query("SELECT p FROM Produto p WHERE p.preco BETWEEN :precoMinimo AND :precoMaximo AND p.ativo = true")
    List<Produto> findByPrecoRange(@Param("precoMinimo") BigDecimal precoMinimo, @Param("precoMaximo") BigDecimal precoMaximo);

    @Query("SELECT p FROM Produto p WHERE p.ativo = true ORDER BY p.preco ASC")
    List<Produto> findByPrecoAsc();

    @Query("SELECT p FROM Produto p WHERE p.ativo = true ORDER BY p.preco DESC")
    List<Produto> findByPrecoDesc();

    // =============== CONSULTAS DE PESQUISA ===============

    @Query("SELECT p FROM Produto p WHERE " +
            "(LOWER(p.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(p.descricao) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(p.marca) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "p.codigoBarras LIKE CONCAT('%', :termo, '%') OR " +
            "p.codigoInterno LIKE CONCAT('%', :termo, '%')) " +
            "AND p.ativo = true")
    Page<Produto> searchProducts(@Param("termo") String termo, Pageable pageable);

    @Query("SELECT p FROM Produto p WHERE " +
            "(:nome IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:categoriaId IS NULL OR p.categoria.id = :categoriaId) AND " +
            "(:marca IS NULL OR LOWER(p.marca) LIKE LOWER(CONCAT('%', :marca, '%'))) AND " +
            "(:ativo IS NULL OR p.ativo = :ativo) AND " +
            "(:destaque IS NULL OR p.destaque = :destaque)")
    Page<Produto> findAllWithFilters(@Param("nome") String nome,
                                     @Param("categoriaId") Long categoriaId,
                                     @Param("marca") String marca,
                                     @Param("ativo") Boolean ativo,
                                     @Param("destaque") Boolean destaque,
                                     Pageable pageable);

    // =============== CONSULTAS PARA RELATÓRIOS ===============

    @Query("SELECT p FROM Produto p WHERE p.destaque = true AND p.ativo = true ORDER BY p.totalAvaliacoes DESC")
    List<Produto> findTopByDestaqueOrderByTotalAvaliacoesDesc(Pageable pageable);

    default List<Produto> findTopByDestaqueOrderByTotalAvaliacoesDesc(int limite) {
        return findTopByDestaqueOrderByTotalAvaliacoesDesc(Pageable.ofSize(limite));
    }

    @Query("SELECT p FROM Produto p WHERE p.ativo = true ORDER BY p.dtCriacao DESC")
    List<Produto> findRecentProducts(Pageable pageable);

    default List<Produto> findRecentProducts(int limite) {
        return findRecentProducts(Pageable.ofSize(limite));
    }

    @Query("SELECT p.marca, COUNT(p) FROM Produto p WHERE p.ativo = true GROUP BY p.marca ORDER BY COUNT(p) DESC")
    List<Object[]> countByMarca();

    @Query("SELECT c.label, COUNT(p) FROM Produto p JOIN p.categoria c WHERE p.ativo = true GROUP BY c.label ORDER BY COUNT(p) DESC")
    List<Object[]> countByCategoria();

    // =============== CONSULTAS ESPECÍFICAS DE NEGÓCIO ===============

    @Query("SELECT p FROM Produto p WHERE p.organizacao.id = :organizacaoId AND p.ativo = true")
    List<Produto> findByOrganizacaoId(@Param("organizacaoId") Long organizacaoId);

    @Query("SELECT p FROM Produto p WHERE p.organizacao.id = :organizacaoId AND p.categoria.id = :categoriaId AND p.ativo = true")
    List<Produto> findByOrganizacaoAndCategoria(@Param("organizacaoId") Long organizacaoId, @Param("categoriaId") Long categoriaId);

    @Query("SELECT p FROM Produto p WHERE p.quantidadeEstoque <= p.estoqueMinimo AND p.ativo = true")
    List<Produto> findProdutosAbaixoEstoqueMinimo();

    @Query("SELECT p FROM Produto p WHERE p.status = 'SEM_ESTOQUE' AND p.ativo = true")
    List<Produto> findProdutosSemEstoqueByStatus();

    @Query("SELECT p FROM Produto p WHERE p.descontoPercentual > 0 AND p.ativo = true ORDER BY p.descontoPercentual DESC")
    List<Produto> findProdutosComDesconto();

    @Query("SELECT p FROM Produto p WHERE p.avaliacao >= :avaliacaoMinima AND p.totalAvaliacoes >= :minimoAvaliacoes AND p.ativo = true ORDER BY p.avaliacao DESC, p.totalAvaliacoes DESC")
    List<Produto> findBestRatedProducts(@Param("avaliacaoMinima") BigDecimal avaliacaoMinima, @Param("minimoAvaliacoes") int minimoAvaliacoes);

    // =============== CONSULTAS DE ANÁLISE ===============

    @Query("SELECT " +
            "COALESCE(AVG(p.preco), 0) as precoMedio, " +
            "COALESCE(MIN(p.preco), 0) as precoMinimo, " +
            "COALESCE(MAX(p.preco), 0) as precoMaximo, " +
            "COALESCE(SUM(p.quantidadeEstoque), 0) as estoqueTotal " +
            "FROM Produto p WHERE p.ativo = true")
    Object[] getAnalisePrecos();

    @Query("SELECT " +
            "COUNT(CASE WHEN p.status = 'ATIVO' THEN 1 END) as ativos, " +
            "COUNT(CASE WHEN p.status = 'INATIVO' THEN 1 END) as inativos, " +
            "COUNT(CASE WHEN p.status = 'DESCONTINUADO' THEN 1 END) as descontinuados, " +
            "COUNT(CASE WHEN p.status = 'SEM_ESTOQUE' THEN 1 END) as semEstoque " +
            "FROM Produto p")
    Object[] getEstatisticasStatus();

    @Query(value = "SELECT * FROM produto p " +
            "WHERE p.dt_criacao >= CURRENT_DATE - INTERVAL :dias DAY " +
            "AND p.ativo = true " +
            "ORDER BY p.dt_criacao DESC",
            nativeQuery = true)
    List<Produto> findRecentlyCreated(@Param("dias") int dias);

    @Query(value = "SELECT * FROM produto p " +
            "WHERE p.dt_atualizacao >= CURRENT_DATE - INTERVAL :dias DAY " +
            "AND p.ativo = true " +
            "ORDER BY p.dt_atualizacao DESC",
            nativeQuery = true)
    List<Produto> findRecentlyUpdated(@Param("dias") int dias);

    // =============== CONSULTAS PARA MOBILE/API ===============

    @Query("SELECT new map(" +
            "p.id as id, " +
            "p.nome as nome, " +
            "p.preco as preco, " +
            "p.marca as marca, " +
            "c.label as categoria, " +
            "p.quantidadeEstoque as estoque, " +
            "p.status as status" +
            ") FROM Produto p JOIN p.categoria c WHERE p.ativo = true")
    List<Object> findProdutosResumo();

    @Query("SELECT p FROM Produto p WHERE " +
            "SIZE(p.urlsImagens) > 0 AND " +
            "p.ativo = true " +
            "ORDER BY p.dtCriacao DESC")
    List<Produto> findProdutosComImagens(Pageable pageable);

    default List<Produto> findProdutosComImagens(int limite) {
        return findProdutosComImagens(Pageable.ofSize(limite));
    }
}
