package org.exemplo.bellory.model.repository.push;

import org.exemplo.bellory.model.entity.push.NotificacaoPush;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacaoPushRepository extends JpaRepository<NotificacaoPush, Long> {

    Page<NotificacaoPush> findAllByUserIdAndUserRoleAndOrganizacao_IdOrderByDtCadastroDesc(
            Long userId, String userRole, Long organizacaoId, Pageable pageable);

    long countByUserIdAndUserRoleAndOrganizacao_IdAndLidoFalse(
            Long userId, String userRole, Long organizacaoId);
}
