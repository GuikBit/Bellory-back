package org.exemplo.bellory.model.entity.landingPage;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "config_landingpage")
public class ConfigLandingPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false, unique = true)
    @JsonBackReference("organizacao-landingpage")
    private Organizacao organizacao;

    @Column(name = "template_id")
    private Integer templateId;

    @Column(name = "cor_principal", length = 7)
    private String corPrincipal = "#000000";

    @Column(name = "cor_secundaria", length = 7)
    private String corSecundaria = "#FFFFFF";

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "mostrar_apresentacao")
    private boolean mostrarApresentacao = true;

    @Column(name = "texto_apresentacao", columnDefinition = "TEXT")
    private String textoApresentacao;

    @Column(name = "mostrar_sobre")
    private boolean mostrarSobre = true;

    @Column(name = "texto_sobre", columnDefinition = "TEXT")
    private String textoSobre;

    @Column(name = "mostrar_servicos")
    private boolean mostrarServicos = true;

    @Column(name = "mostrar_produtos")
    private boolean mostrarProdutos = true;

    @Column(name = "mostrar_equipe")
    private boolean mostrarEquipe = true;

    @Column(name = "mostrar_contato")
    private boolean mostrarContato = true;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // Getters e Setters
}