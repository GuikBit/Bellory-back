package org.exemplo.bellory.model.entity.funcionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "cargo_funcionario")
@Getter
@Setter
public class Cargo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(length = 100, nullable = false)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    @Column
    private boolean ativo;

}
