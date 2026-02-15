package org.exemplo.bellory.model.repository.categoria;

import org.exemplo.bellory.model.entity.enums.TipoCategoria;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByOrganizacao_IdAndTipo(Long organizacaoId, TipoCategoria tipo);

    List<Categoria> findByOrganizacao_IdAndTipoAndAtivoTrue(Long organizacaoId, TipoCategoria tipo);

    List<Categoria> findByOrganizacao_IdAndAtivoTrue(Long organizacaoId);

    List<Categoria> findByOrganizacao_IdAndTipoAndAtivoTrueOrderByLabelAsc(Long organizacaoId, TipoCategoria tipo);
}
