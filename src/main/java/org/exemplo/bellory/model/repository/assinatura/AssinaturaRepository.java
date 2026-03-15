package org.exemplo.bellory.model.repository.assinatura;

import org.exemplo.bellory.model.entity.assinatura.Assinatura;
import org.exemplo.bellory.model.entity.assinatura.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "WHERE a.organizacao.id = :organizacaoId")
    Optional<Assinatura> findByOrganizacaoId(@Param("organizacaoId") Long organizacaoId);

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "WHERE a.id = :id")
    Optional<Assinatura> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "WHERE a.status = 'TRIAL' AND a.dtFimTrial < :agora")
    List<Assinatura> findTrialsExpirados(@Param("agora") LocalDateTime agora);

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory")
    List<Assinatura> findAllWithDetails();

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "WHERE a.status = :status")
    List<Assinatura> findByStatus(@Param("status") StatusAssinatura status);

    long countByStatus(StatusAssinatura status);

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "LEFT JOIN FETCH a.cupom " +
           "WHERE a.status = 'ATIVA' AND a.dtProximoVencimento IS NOT NULL")
    List<Assinatura> findAtivasComVencimento();

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "WHERE a.status = 'TRIAL' AND a.dtFimTrial BETWEEN :inicio AND :fim")
    List<Assinatura> findTrialsExpirandoEntre(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "LEFT JOIN FETCH a.cupom " +
           "WHERE a.status = 'ATIVA' AND a.cicloCobranca = 'ANUAL' " +
           "AND a.dtProximoVencimento IS NOT NULL AND a.dtProximoVencimento <= :limite")
    List<Assinatura> findAnuaisParaRenovacao(@Param("limite") LocalDateTime limite);

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "LEFT JOIN FETCH a.cupom " +
           "WHERE a.status = 'ATIVA' AND a.cicloCobranca = 'MENSAL' " +
           "AND a.dtProximoVencimento IS NOT NULL")
    List<Assinatura> findMensaisAtivas();

    @Query("SELECT a FROM Assinatura a " +
           "LEFT JOIN FETCH a.organizacao " +
           "LEFT JOIN FETCH a.planoBellory " +
           "WHERE a.assasSubscriptionId IS NOT NULL " +
           "AND a.status IN :statuses")
    List<Assinatura> findByAssasSubscriptionIdNotNullAndStatusIn(@Param("statuses") List<StatusAssinatura> statuses);

    Optional<Assinatura> findByAssasSubscriptionId(String assasSubscriptionId);

    List<Assinatura> findByAssasSubscriptionIdInAndStatus(Collection<String> subscriptionIds, StatusAssinatura status);
}
