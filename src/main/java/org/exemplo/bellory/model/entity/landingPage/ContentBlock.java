package org.exemplo.bellory.model.entity.landingPage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_content_blocks")
@Getter
@Setter
public class ContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private String contentKey; // Ex: "title", "subtitle", "imageUrl", "buttonText"

    @Lob // Large Object: ideal para armazenar textos longos, URLs ou até mesmo JSON
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentValue;
}
