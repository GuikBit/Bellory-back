package org.exemplo.bellory.model.repository.config;

import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigSistemaRepository extends JpaRepository<ConfigSistema, Long> {
    Optional<ConfigSistema> findByOrganizacaoId(Long organizacaoId);
    Optional<ConfigSistema> findByTenantId(String tenantId);
}
