package org.exemplo.bellory.model.entity.instancia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.converter.StringListConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "instance_webhook")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url = "https://auto.bellory.com.br/webhook/webhook/whatsapp";

    @Column(nullable = false)
    private String token = "0626f19f09bd356cc21037164c7c3ca51752fef8";

    @Column(nullable = false)
    private Boolean enabled = true; // Melhor nome que "status"

    @Column(name = "by_events")
    private Boolean byEvents = false;

    @Column(name = "base64")
    private Boolean base64 = false;

    // Armazena os eventos como JSON ou texto separado por vírgula
    @Column(name = "events", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> events = new ArrayList<>();

    // Relacionamento bidirecional (opcional)
    @OneToOne(mappedBy = "webhookConfig", fetch = FetchType.LAZY)
    @JsonIgnore
    private Instance instance;

    @PrePersist
    @PreUpdate
    private void ensureConfigurations() {
        if (this.events == null) {
            this.events = new ArrayList<>();
            this.events.add("MESSAGES_UPSERT");
        } else if (this.events.isEmpty()) {
            this.events.add("MESSAGES_UPSERT");
        }
    }
}
