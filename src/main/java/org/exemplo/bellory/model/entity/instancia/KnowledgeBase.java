package org.exemplo.bellory.model.entity.instancia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "instance_knowledgebase", schema = "app",
    indexes = {
        @Index(name = "idx_kb_instance_id", columnList = "instance_id"),
        @Index(name = "idx_kb_instance_type", columnList = "instance_id, type")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private KnowledgeType type; // TEXT ou FILE

    @Column(columnDefinition = "TEXT")
    private String content; // Para tipo TEXT

    @Column(length = 255)
    private String fileName; // Para tipo FILE

    @Column
    private Long fileSize; // Tamanho do arquivo em bytes

    @Column(length = 100)
    private String fileType; // Tipo MIME do arquivo

    @Column(length = 500)
    private String fileUrl; // URL onde o arquivo está armazenado

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    @JsonIgnore
    private Instance instance;

    // Enum para tipo de conhecimento
    public enum KnowledgeType {
        TEXT,
        FILE
    }
}
