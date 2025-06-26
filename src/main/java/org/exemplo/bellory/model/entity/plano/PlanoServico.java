package org.exemplo.bellory.model.entity.plano;

import jakarta.persistence.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plano_servico")
public class PlanoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "validade_dias")
    private Integer validadeDias;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(nullable = false)
    private boolean ativo = true;

    // Getters e Setters
}
