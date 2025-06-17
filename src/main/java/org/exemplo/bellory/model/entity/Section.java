package org.exemplo.bellory.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_sections")
@Getter
@Setter
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_page_id", nullable = false)
    private LandingPage landingPage;

    @Column(nullable = false)
    private String sectionType; // Ex: "HERO", "FEATURES_GRID", "TESTIMONIALS"

    @Column(nullable = false)
    private int displayOrder; // Ordem da secção na página (0, 1, 2...)

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ContentBlock> contentBlocks = new ArrayList<>();
}
