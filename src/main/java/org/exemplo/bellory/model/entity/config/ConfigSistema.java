package org.exemplo.bellory.model.entity.config;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "config-sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false, unique = true)
    @JsonBackReference("organizacao-configsistema")
    private Organizacao organizacao;

    @Column(name = "usa_ecommerce", nullable = false)
    private boolean usaEcommerce = true;

    @Column(name = "usa_gestao_produtos", nullable = false)
    private boolean usaGestaoProdutos = true;

    @Column(name = "usa_planos_para_clientes", nullable = false)
    private boolean usaPlanosParaClientes = false;

    @Column(name = "dispara_notificacoes_push", nullable = false)
    private boolean disparaNotificacoesPush = true;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;


}
