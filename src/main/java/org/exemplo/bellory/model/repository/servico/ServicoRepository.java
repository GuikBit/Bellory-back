package org.exemplo.bellory.model.repository.servico;

import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<ServicoAgendamento> findAllProjectedBy();

    boolean existsByNome(String nome);

    List<Servico> findAllByOrderByNomeAsc();

    Optional<Servico> findByNomeAndOrganizacao(String nome, Organizacao org);

    List<Servico> findAllByOrganizacao_IdAndIsDeletadoFalseOrderByNomeAsc(Long organizacaoId);

    List<ServicoAgendamento> findAllProjectedByOrganizacao_Id(Long organizacaoId);

    List<Servico> findAllByOrganizacao_IdAndAtivoTrueAndIsDeletadoFalseOrderByNomeAsc(Long organizacaoId);

    /**
     * Busca serviços com fetch dos funcionários e categoria.
     * Evita N+1 queries.
     */
    @Query("SELECT DISTINCT s FROM Servico s " +
            "LEFT JOIN FETCH s.funcionarios f " +
            "LEFT JOIN FETCH s.categoria " +
            "WHERE s.organizacao.id = :orgId " +
            "AND s.ativo = true " +
            "ORDER BY s.nome")
    List<Servico> findAllForPublicSite(@Param("orgId") Long organizacaoId);

    /**
     * Busca serviços marcados para exibir na home.
     */
    List<Servico> findAllByOrganizacao_IdAndAtivoTrueAndIsHomeTrue(Long organizacaoId);

    List<ServicoAgendamento> findAllProjectedByOrganizacao_IdAndIsDeletadoFalse(Long organizacaoId);

    /**
     * Busca serviços ativos com paginação (para site público)
     */
    Page<Servico> findByOrganizacao_IdAndAtivoTrueAndIsDeletadoFalse(Long organizacaoId, Pageable pageable);
}
