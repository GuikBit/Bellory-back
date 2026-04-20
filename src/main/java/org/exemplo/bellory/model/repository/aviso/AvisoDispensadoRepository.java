package org.exemplo.bellory.model.repository.aviso;

import org.exemplo.bellory.model.entity.aviso.AvisoDispensado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvisoDispensadoRepository extends JpaRepository<AvisoDispensado, Long> {

    @Query("SELECT ad.avisoId FROM AvisoDispensado ad " +
            "WHERE ad.organizacaoId = :organizacaoId AND ad.usuarioId = :usuarioId")
    List<String> findAvisoIdsByOrganizacaoIdAndUsuarioId(
            @Param("organizacaoId") Long organizacaoId,
            @Param("usuarioId") Long usuarioId
    );

    Optional<AvisoDispensado> findByOrganizacaoIdAndUsuarioIdAndAvisoId(
            Long organizacaoId, Long usuarioId, String avisoId
    );
}
