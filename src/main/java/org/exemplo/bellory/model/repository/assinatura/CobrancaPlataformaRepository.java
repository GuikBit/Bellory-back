package org.exemplo.bellory.model.repository.assinatura;

import org.exemplo.bellory.model.entity.assinatura.CobrancaPlataforma;
import org.exemplo.bellory.model.entity.assinatura.StatusCobrancaPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CobrancaPlataformaRepository extends JpaRepository<CobrancaPlataforma, Long> {

    @Query("SELECT c FROM CobrancaPlataforma c " +
           "LEFT JOIN FETCH c.assinatura " +
           "WHERE c.organizacao.id = :organizacaoId " +
           "ORDER BY c.dtVencimento DESC")
    List<CobrancaPlataforma> findByOrganizacaoId(@Param("organizacaoId") Long organizacaoId);

    @Query("SELECT c FROM CobrancaPlataforma c " +
           "LEFT JOIN FETCH c.assinatura " +
           "WHERE c.organizacao.id = :organizacaoId AND c.status = :status " +
           "ORDER BY c.dtVencimento DESC")
    List<CobrancaPlataforma> findByOrganizacaoIdAndStatus(
            @Param("organizacaoId") Long organizacaoId,
            @Param("status") StatusCobrancaPlataforma status);

    @Query("SELECT c FROM CobrancaPlataforma c " +
           "WHERE c.status = 'PENDENTE' AND c.dtVencimento < :hoje")
    List<CobrancaPlataforma> findCobrancasVencidas(@Param("hoje") LocalDate hoje);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM CobrancaPlataforma c " +
           "WHERE c.status = 'PAGA' AND c.referenciaMes = :mes AND c.referenciaAno = :ano")
    BigDecimal calcularReceitaMes(@Param("mes") int mes, @Param("ano") int ano);

    @Query("SELECT c FROM CobrancaPlataforma c " +
           "LEFT JOIN FETCH c.assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "WHERE c.assinatura.id = :assinaturaId " +
           "ORDER BY c.dtVencimento DESC")
    List<CobrancaPlataforma> findByAssinaturaId(@Param("assinaturaId") Long assinaturaId);

    Optional<CobrancaPlataforma> findByAssasPaymentId(String assasPaymentId);

    boolean existsByAssinaturaIdAndReferenciaMesAndReferenciaAno(Long assinaturaId, Integer mes, Integer ano);
}
