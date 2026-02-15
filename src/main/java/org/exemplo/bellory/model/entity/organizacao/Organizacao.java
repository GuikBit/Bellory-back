package org.exemplo.bellory.model.entity.organizacao;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.entity.plano.PlanoLimites;
import org.exemplo.bellory.model.entity.plano.PlanoLimitesBellory;
import org.exemplo.bellory.model.entity.tema.Tema;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizacao", schema = "app", indexes = {
        @Index(name = "idx_organizacao_slug_ativo", columnList = "slug, ativo"),
        @Index(name = "idx_organizacao_plano_id", columnList = "plano_id"),
        @Index(name = "idx_organizacao_ativo", columnList = "ativo"),
        @Index(name = "idx_organizacao_dt_cadastro", columnList = "dt_cadastro")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotBlank(message = "CNPJ é obrigatório")
    @Size(min = 14, max = 18, message = "CNPJ deve ter entre 14 e 18 caracteres")
    @Column(name = "cnpj", unique = true, nullable = false, length = 18)
    private String cnpj;

    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 255, message = "Razão social deve ter no máximo 255 caracteres")
    @Column(name = "razao_social")
    private String razaoSocial;

    @NotBlank(message = "Nome fantasia é obrigatório")
    @Size(max = 255, message = "Nome fantasia deve ter no máximo 255 caracteres")
    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    private String publicoAlvo;

    @Size(max = 50, message = "Inscrição estadual deve ter no máximo 50 caracteres")
    @Column(name = "inscricao_estadual", length = 50)
    private String inscricaoEstadual;

    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    @Column(name = "emailPrincipal")
    private String emailPrincipal;

    @NotBlank(message = "Telefone principal é obrigatório")
    @Column(name = "telefone1", length = 20)
    private String telefone1;

    @Column(name = "telefone2", length = 20)
    private String telefone2;

    @Column(name = "whatsapp", length = 20)
    private String whatsapp;

    // CORRIGIDO: Responsavel é um objeto Embeddable, não uma coluna
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "nome", column = @Column(name = "responsavel_nome")),
            @AttributeOverride(name = "email", column = @Column(name = "responsavel_email")),
            @AttributeOverride(name = "telefone", column = @Column(name = "responsavel_telefone"))
    })
    private Responsavel responsavel;

    // CORRETO: OneToOne com Endereco
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_principal_id", referencedColumnName = "id")
    private Endereco enderecoPrincipal;

    // CORRIGIDO: AcessoAdm é um objeto Embeddable, não uma coluna
//    @Embedded
//    @AttributeOverrides({
//            @AttributeOverride(name = "login", column = @Column(name = "acesso_adm_login")),
//            @AttributeOverride(name = "senha", column = @Column(name = "acesso_adm_senha")),
//            @AttributeOverride(name = "role", column = @Column(name = "acesso_adm_role"))
//    })
//    private AcessoAdm acessoAdm;

    // CORRIGIDO: Se Tema for uma entidade, usar @ManyToOne ou @OneToOne
    // Se for Embeddable, usar @Embedded
    // Assumindo que é uma entidade:
    @Embedded
    private Tema tema;
    // CORRETO: ManyToOne com Plano
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    @JsonIgnore
    private PlanoBellory plano;

    @Column(unique = true, nullable = false)
    private String slug;

    // CORRETO: OneToOne com ConfigSistema
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "config_sistema_id")
    private ConfigSistema configSistema;

    // CORRIGIDO: RedesSociais é um objeto Embeddable, não uma entidade OneToOne
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "instagram", column = @Column(name = "redes_sociais_instagram")),
            @AttributeOverride(name = "facebook", column = @Column(name = "redes_sociais_facebook")),
            @AttributeOverride(name = "whatsapp", column = @Column(name = "redes_sociais_whatsapp")),
            @AttributeOverride(name = "linkedin", column = @Column(name = "redes_sociais_linkedin")),
            @AttributeOverride(name = "messenger", column = @Column(name = "redes_sociais_messenger")),
            @AttributeOverride(name = "site", column = @Column(name = "redes_sociais_site")),
            @AttributeOverride(name = "youtube", column = @Column(name = "redes_sociais_youtube"))
    })
    private RedesSociais redesSociais;

    // REMOVIDO: Este relacionamento precisa ser definido corretamente
    // Se DiaSemanaFuncionamento for uma entidade separada, deveria ser:
    // @OneToMany(mappedBy = "organizacao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<DiaSemanaFuncionamento> horarioFuncionamento;
    //
    // Se for Embeddable, deveria ser:
    // @ElementCollection
    // @CollectionTable(name = "organizacao_horario_funcionamento",
    //                  joinColumns = @JoinColumn(name = "organizacao_id"))
    // private List<DiaSemanaFuncionamento> horarioFuncionamento;

    // CORRETO: OneToOne bidirecional com DadosFaturamentoOrganizacao
    @OneToOne(mappedBy = "organizacao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("organizacao-faturamento")
    private DadosFaturamentoOrganizacao dadosFaturamento;

    // CORRETO: OneToOne com PlanoLimites
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "limites_personalizados_id")
    private PlanoLimitesBellory limitesPersonalizados;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "dt_cadastro", nullable = false, updatable = false)
    private LocalDateTime dtCadastro;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    protected void onCreate() {
        dtCadastro = LocalDateTime.now();
        dtAtualizacao = LocalDateTime.now();
        if (ativo == null) {
            ativo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }
}
