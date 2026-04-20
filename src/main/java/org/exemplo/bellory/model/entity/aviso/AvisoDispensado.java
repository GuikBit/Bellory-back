package org.exemplo.bellory.model.entity.aviso;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "aviso_dispensado", schema = "app", indexes = {
        @Index(name = "idx_aviso_disp_org_user", columnList = "organizacao_id, usuario_id"),
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_aviso_dispensado", columnNames = {"organizacao_id", "usuario_id", "aviso_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvisoDispensado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacao_id", nullable = false)
    private Long organizacaoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "aviso_id", nullable = false, length = 100)
    private String avisoId;

    @Column(name = "dt_dispensado", nullable = false)
    private LocalDateTime dtDispensado;

    @PrePersist
    protected void onCreate() {
        dtDispensado = LocalDateTime.now();
    }
}
