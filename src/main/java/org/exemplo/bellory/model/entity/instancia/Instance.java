package org.exemplo.bellory.model.entity.instancia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma instância do WhatsApp integrada com Evolution API
 * Cada organização pode ter múltiplas instâncias (diferentes números de WhatsApp)
 */
@Entity
@Table(name = "instance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String instanceId;

    @Column(nullable = false, unique = true, length = 100)
    private String instanceName;

    @Column(nullable = false)
    private String integration;

    @Column(columnDefinition = "TEXT")
    private String qrcode;

    /**
     * Status da conexão: DISCONNECTED, CONNECTING, CONNECTED, OPEN
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstanceStatus status = InstanceStatus.DISCONNECTED;

    /**
     * Número de telefone conectado (formato: 5511999999999)
     */
    @Column(length = 20)
    private String phoneNumber;

    /**
     * URL da foto de perfil do WhatsApp
     */
    @Column(length = 500)
    private String profilePictureUrl;

    /**
     * Nome do perfil do WhatsApp
     */
    @Column(length = 100)
    private String profileName;

    /**
     * URL do webhook para receber eventos
     */
    @Column(length = 500)
    private String webhookUrl;

    /**
     * Flag para habilitar/desabilitar webhook
     */
    @Column(nullable = false)
    private Boolean webhookEnabled = false;

    /**
     * Eventos que serão enviados para o webhook (JSON array)
     * Ex: ["MESSAGES_UPSERT", "CONNECTION_UPDATE", "QRCODE_UPDATED"]
     */
    @Column(columnDefinition = "TEXT")
    private String webhookEvents;

    /**
     * Rejeitar chamadas automaticamente
     */
    @Column(nullable = false)
    private Boolean rejectCall = false;

    /**
     * Mensagem enviada quando uma chamada é rejeitada
     */
    @Column(length = 500)
    private String msgCall;

    /**
     * Ignorar mensagens de grupos
     */
    @Column(nullable = false)
    private Boolean groupsIgnore = true;

    /**
     * Sempre aparecer online
     */
    @Column(nullable = false)
    private Boolean alwaysOnline = false;

    /**
     * Marcar mensagens como lidas automaticamente
     */
    @Column(nullable = false)
    private Boolean readMessages = false;

    /**
     * Marcar status como lidos automaticamente
     */
    @Column(nullable = false)
    private Boolean readStatus = false;

    /**
     * Relacionamento com a Organização (Barbearia/Salão)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    /**
     * Flag para ativar/desativar a instância
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * Data de criação do registro
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data da última atualização
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Descrição ou observações sobre a instância
     */
    @Column(columnDefinition = "TEXT")
    private String description;
}
