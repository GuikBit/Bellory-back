package org.exemplo.bellory.model.repository;

import org.exemplo.bellory.model.entity.config.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHashAndAtivoTrue(String keyHash);

    List<ApiKey> findByUserIdAndUserTypeAndAtivoTrue(Long userId, ApiKey.UserType userType);

    List<ApiKey> findByOrganizacaoIdAndAtivoTrue(Long organizacaoId);
}