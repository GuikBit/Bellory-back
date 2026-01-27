package org.exemplo.bellory.model.repository.notificacao;

import org.exemplo.bellory.model.entity.notificacao.ConfigNotificacao;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigNotificacaoRepository extends JpaRepository<ConfigNotificacao, Long> {

    List<ConfigNotificacao> findByOrganizacaoIdAndAtivoTrue(Long organizacaoId);

    List<ConfigNotificacao> findByOrganizacaoId(Long organizacaoId);

    Optional<ConfigNotificacao> findByOrganizacaoIdAndTipoAndHorasAntes(
        Long organizacaoId, TipoNotificacao tipo, Integer horasAntes);

    boolean existsByOrganizacaoIdAndTipoAndHorasAntes(
        Long organizacaoId, TipoNotificacao tipo, Integer horasAntes);

    @Query("""
        SELECT cn FROM ConfigNotificacao cn
        WHERE cn.organizacao.id = :orgId AND cn.ativo = true
        ORDER BY cn.tipo, cn.horasAntes DESC
        """)
    List<ConfigNotificacao> findConfiguracoesAtivasOrdenadas(@Param("orgId") Long organizacaoId);
}
