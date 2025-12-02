package org.exemplo.bellory.model.repository.instance;


import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.instancia.InstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long> {

    /**
     * Buscar instância por nome
     */
    Optional<Instance> findByInstanceName(String instanceName);

    /**
     * Buscar todas as instâncias de uma organização
     */
    List<Instance> findByOrganizacaoId(Long organizacaoId);

    /**
     * Buscar instâncias ativas de uma organização
     */
    List<Instance> findByOrganizacaoIdAndIsActiveTrue(Long organizacaoId);

    /**
     * Buscar instâncias por status
     */
    List<Instance> findByStatus(InstanceStatus status);

    /**
     * Buscar instâncias conectadas de uma organização
     */
    List<Instance> findByOrganizacaoIdAndStatusIn(Long organizacaoId, List<InstanceStatus> statuses);

    /**
     * Verificar se existe instância com o nome
     */
    boolean existsByInstanceName(String instanceName);

    /**
     * Buscar instância por número de telefone
     */
    Optional<Instance> findByPhoneNumber(String phoneNumber);

    /**
     * Contar instâncias ativas de uma organização
     */
    long countByOrganizacaoIdAndIsActiveTrue(Long organizacaoId);

    /**
     * Contar instâncias conectadas de uma organização
     */
    @Query("SELECT COUNT(i) FROM Instance i WHERE i.organizacao.id = :organizacaoId " +
            "AND i.status IN ('CONNECTED', 'OPEN')")
    long countConnectedByOrganizacaoId(@Param("organizacaoId") Long organizacaoId);
}


