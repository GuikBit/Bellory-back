package org.exemplo.bellory.model.entity.servico;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categServico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false, length = 255)
    private String value;

    @Column(nullable = false)
    private boolean ativo = true;
}
