package org.exemplo.bellory.model.entity.landingPage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.users.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_landing_pages")
@Getter
@Setter
public class LandingPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug; // Ex: "promo-verao-2025"

    @Column(nullable = false)
    private String internalTitle; // Um título para organização interna

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "landingPage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC") // Garante que as secções vêm sempre ordenadas
    private List<Section> sections = new ArrayList<>();
}
