package org.exemplo.bellory.model.repository.assinatura;

import org.exemplo.bellory.model.entity.assinatura.PagamentoPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagamentoPlataformaRepository extends JpaRepository<PagamentoPlataforma, Long> {

    @Query("SELECT p FROM PagamentoPlataforma p " +
           "LEFT JOIN FETCH p.cobranca " +
           "WHERE p.cobranca.id = :cobrancaId " +
           "ORDER BY p.dtCriacao DESC")
    List<PagamentoPlataforma> findByCobrancaId(@Param("cobrancaId") Long cobrancaId);

    @Query("SELECT p FROM PagamentoPlataforma p " +
           "LEFT JOIN FETCH p.cobranca c " +
           "WHERE c.organizacao.id = :organizacaoId " +
           "ORDER BY p.dtCriacao DESC")
    List<PagamentoPlataforma> findByOrganizacaoId(@Param("organizacaoId") Long organizacaoId);

    Optional<PagamentoPlataforma> findByAssasPaymentId(String assasPaymentId);
}
