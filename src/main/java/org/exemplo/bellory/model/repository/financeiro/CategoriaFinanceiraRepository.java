package org.exemplo.bellory.model.repository.financeiro;

import org.exemplo.bellory.model.entity.financeiro.CategoriaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, Long> {

    List<CategoriaFinanceira> findByOrganizacaoIdAndAtivoTrue(Long organizacaoId);

    List<CategoriaFinanceira> findByOrganizacaoId(Long organizacaoId);

    List<CategoriaFinanceira> findByOrganizacaoIdAndTipoAndAtivoTrue(
            Long organizacaoId, CategoriaFinanceira.TipoCategoria tipo);

    @Query("SELECT c FROM CategoriaFinanceira c WHERE c.organizacao.id = :orgId AND c.categoriaPai IS NULL AND c.ativo = true ORDER BY c.nome")
    List<CategoriaFinanceira> findCategoriasRaizByOrganizacao(@Param("orgId") Long organizacaoId);

    @Query("SELECT c FROM CategoriaFinanceira c WHERE c.organizacao.id = :orgId AND c.categoriaPai IS NULL AND c.tipo = :tipo AND c.ativo = true ORDER BY c.nome")
    List<CategoriaFinanceira> findCategoriasRaizByOrganizacaoAndTipo(
            @Param("orgId") Long organizacaoId, @Param("tipo") CategoriaFinanceira.TipoCategoria tipo);

    List<CategoriaFinanceira> findByCategoriaPaiId(Long categoriaPaiId);

    Optional<CategoriaFinanceira> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    boolean existsByNomeAndOrganizacaoIdAndTipo(String nome, Long organizacaoId, CategoriaFinanceira.TipoCategoria tipo);
}
