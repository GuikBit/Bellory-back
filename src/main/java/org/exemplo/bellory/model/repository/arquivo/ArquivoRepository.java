package org.exemplo.bellory.model.repository.arquivo;

import org.exemplo.bellory.model.entity.arquivo.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {

    List<Arquivo> findAllByOrganizacao_IdAndPasta_IdOrderByDtCriacaoDesc(Long organizacaoId, Long pastaId);

    List<Arquivo> findAllByOrganizacao_IdAndPastaIsNullOrderByDtCriacaoDesc(Long organizacaoId);

    Optional<Arquivo> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);

    @Query("SELECT COALESCE(SUM(a.tamanho), 0) FROM Arquivo a WHERE a.organizacao.id = :orgId")
    Long calcularStorageUsado(@Param("orgId") Long organizacaoId);

    @Query("SELECT COUNT(a) FROM Arquivo a WHERE a.organizacao.id = :orgId")
    Integer contarArquivos(@Param("orgId") Long organizacaoId);

    @Query("SELECT COALESCE(SUM(a.tamanho), 0) FROM Arquivo a WHERE a.pasta.id = :pastaId")
    Long calcularTamanhoPasta(@Param("pastaId") Long pastaId);

    @Query("SELECT COUNT(a) FROM Arquivo a WHERE a.pasta.id = :pastaId")
    Integer contarArquivosPorPasta(@Param("pastaId") Long pastaId);

    boolean existsByNomeOriginalAndOrganizacao_IdAndPasta_Id(String nomeOriginal, Long organizacaoId, Long pastaId);

    @Query("SELECT a FROM Arquivo a WHERE a.organizacao.id = :orgId ORDER BY a.dtCriacao DESC")
    List<Arquivo> findAllByOrganizacao(@Param("orgId") Long organizacaoId);
}
