package org.exemplo.bellory.model.repository.arquivo;

import org.exemplo.bellory.model.entity.arquivo.PastaArquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PastaArquivoRepository extends JpaRepository<PastaArquivo, Long> {

    List<PastaArquivo> findAllByOrganizacao_IdAndPastaPaiIsNullOrderByNomeAsc(Long organizacaoId);

    List<PastaArquivo> findAllByOrganizacao_IdAndPastaPai_IdOrderByNomeAsc(Long organizacaoId, Long pastaPaiId);

    Optional<PastaArquivo> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);

    boolean existsByOrganizacao_IdAndCaminhoCompleto(Long organizacaoId, String caminhoCompleto);

    @Query("SELECT COUNT(p) FROM PastaArquivo p WHERE p.organizacao.id = :orgId")
    Integer contarPastas(@Param("orgId") Long organizacaoId);

    Optional<PastaArquivo> findByOrganizacao_IdAndCaminhoCompleto(Long organizacaoId, String caminhoCompleto);
}
