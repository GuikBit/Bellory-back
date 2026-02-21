package org.exemplo.bellory.model.repository.push;

import org.exemplo.bellory.model.entity.push.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findAllByUserIdAndUserRoleAndOrganizacao_Id(Long userId, String userRole, Long organizacaoId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByOrganizacao_Id(Long organizacaoId);

    List<PushSubscription> findAllByUserRoleAndOrganizacao_Id(String userRole, Long organizacaoId);

    void deleteByEndpoint(String endpoint);
}
