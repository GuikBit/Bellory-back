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
     * Buscar todas as instâncias de uma organização
     */
    List<Instance> findByOrganizacaoId(Long organizacaoId);

    /**
     * Verificar se existe instância com o nome
     */
    boolean existsByInstanceName(String instanceName);



}


