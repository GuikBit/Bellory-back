package org.exemplo.bellory.model.entity.servico;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.enums.TipoCategoria;

@Entity
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false, length = 255)
    private String value;

    @Enumerated(EnumType.STRING) // Armazena o nome do enum no banco
    @Column(nullable = false, length = 50)
    private TipoCategoria tipo;

    @Column(nullable = false)
    private boolean ativo = true;
}

