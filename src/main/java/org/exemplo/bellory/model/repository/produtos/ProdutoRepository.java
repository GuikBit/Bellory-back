package org.exemplo.bellory.model.repository.produtos;

import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Optional<Object> findByNomeAndOrganizacao(String nome, Organizacao organizacao);

    List<Produto> findByAtivo(boolean ativo);

    Long countByAtivo(boolean ativo);

    @Query("SELECT p FROM Produto p WHERE p.qtdEstoque <= :limite AND p.ativo = true ORDER BY p.qtdEstoque ASC")
    List<Produto> findByEstoqueBaixo(int limite);

    @Query("SELECT p FROM Produto p WHERE p.qtdEstoque = 0 AND p.ativo = true")
    List<Produto> findSemEstoque();
}
