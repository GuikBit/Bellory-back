package org.exemplo.bellory.model.repository.assinatura;

import org.exemplo.bellory.model.entity.assinatura.Assinatura;
import org.exemplo.bellory.model.entity.assinatura.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
           "WHERE a.status = 'ATIVA' AND a.dtProximoVencimento IS NOT NULL")
    List<Assinatura> findAtivasComVencimento();
}
