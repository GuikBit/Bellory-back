package org.exemplo.bellory.model.entity.config;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys", schema = "app",
    indexes = {
        @Index(name = "idx_apikey_org_ativo", columnList = "organizacao_id, ativo"),
        @Index(name = "idx_apikey_user_type_ativo", columnList = "user_id, user_type, ativo")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento polimórfico - armazena informações do usuário
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserType userType; // ADMIN, FUNCIONARIO, CLIENTE

    @Column(name = "username", nullable = false)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(nullable = false, unique = true, length = 128)
    private String keyHash; // Hash SHA-256 da chave

    @Column(nullable = false, unique = true, length = 128)
    private String apikey;

    @Column(nullable = false, length = 100)
    private String name; // Ex: "N8N - Automação Vendas"

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Opcional

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // Enum para tipo de usuário
    public enum UserType {
        ADMIN,
        FUNCIONARIO,
        CLIENTE,
        SISTEMA
    }
}
