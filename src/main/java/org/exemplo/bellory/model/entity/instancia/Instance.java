package org.exemplo.bellory.model.entity.instancia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "instance", schema = "app")
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

    private String personality;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InstanceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "tool_id")
    @JsonIgnore
    private Tools tools;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "webhook_id")
    @JsonIgnore
    private WebhookConfig webhookConfig;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "settings_id")
    @JsonIgnore
    private Settings settings;

    // Relacionamento OneToMany com KnowledgeBase
    @OneToMany(
            mappedBy = "instance",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<KnowledgeBase> knowledgeBase = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void ensureConfigurations() {
        if (this.tools == null) {
            this.tools = new Tools();
        }
        if (this.webhookConfig == null) {
            this.webhookConfig = new WebhookConfig();
        }

        if(this.settings == null) {
            this.settings = new Settings();
        }
    }

    public void addKnowledgeBase(KnowledgeBase knowledge) {
        knowledgeBase.add(knowledge);
        knowledge.setInstance(this);
    }

    public void removeKnowledgeBase(KnowledgeBase knowledge) {
        knowledgeBase.remove(knowledge);
        knowledge.setInstance(null);
    }

}
