package org.exemplo.bellory.model.repository.importacao;

import org.exemplo.bellory.model.entity.importacao.ClienteImportacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteImportacaoRepository extends JpaRepository<ClienteImportacao, Long> {

    /**
     * Lista importacoes da organizacao ordenadas pela mais recente primeiro.
     * Usada na tela de listagem do frontend.
     */
    List<ClienteImportacao> findByOrganizacaoIdOrderByDtInicioDesc(Long organizacaoId);
}
