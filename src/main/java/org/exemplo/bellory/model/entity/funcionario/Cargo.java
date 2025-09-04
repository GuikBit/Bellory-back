package org.exemplo.bellory.model.entity.funcionario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cargo_funcionario")
@Getter
@Setter
public class Cargo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

}
