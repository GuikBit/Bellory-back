package org.exemplo.bellory.model.entity.landingpage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa uma versão salva de uma Landing Page.
 * Usado para histórico, rollback e comparação de versões.
 */
@Entity
@Table(name = "landing_page_version", schema = "app", indexes = {
        @Index(name = "idx_version_landing_page", columnList = "landing_page_id"),
        @Index(name = "idx_version_number", columnList = "landing_page_id, versao")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingPageVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_page_id", nullable = false)
    @JsonIgnore
    private LandingPage landingPage;

    /**
     * Número da versão.
     */
    @Column(name = "versao", nullable = false)
    private Integer versao;

    /**
     * Snapshot completo da página em JSON.
     * Contém todas as seções e configurações.
     */
    @Column(name = "snapshot", columnDefinition = "TEXT", nullable = false)
    private String snapshot;

    /**
     * Descrição da alteração (changelog).
     */
    @Column(name = "descricao", length = 500)
    private String descricao;

    /**
     * Tipo: AUTO_SAVE, MANUAL, PUBLISH
     */
    @Column(name = "tipo", length = 20)
    @Builder.Default
    private String tipo = "MANUAL";

    /**
     * ID do usuário que criou a versão.
     */
    @Column(name = "criado_por")
    private Long criadoPor;

    /**
     * Nome do usuário que criou a versão.
     */
    @Column(name = "criado_por_nome", length = 200)
    private String criadoPorNome;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
    }
}
