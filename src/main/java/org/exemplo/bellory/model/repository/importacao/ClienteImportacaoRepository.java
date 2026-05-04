package org.exemplo.bellory.model.repository.importacao;

import org.exemplo.bellory.model.entity.importacao.ClienteImportacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteImportacaoRepository extends JpaRepository<ClienteImportacao, Long> {
}
