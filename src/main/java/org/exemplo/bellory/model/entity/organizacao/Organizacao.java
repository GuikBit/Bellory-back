package org.exemplo.bellory.model.entity.organizacao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.plano.PlanoLimites;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.Cliente;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "organizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String nome;

    @Column(name = "nome_fantasia", length = 255)
    private String nomeFantasia;

    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(name = "nome_responsavel", nullable = false, length = 255)
    private String nomeResponsavel;

    @Column(name = "email_responsavel", nullable = false, unique = true, length = 255)
    private String emailResponsavel;

    @Column(name = "cpf_responsavel", nullable = false, unique = true, length = 14)
    private String cpfResponsavel;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_principal_id", referencedColumnName = "id")
    private Endereco enderecoPrincipal;

    @OneToOne(mappedBy = "organizacao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("organizacao-faturamento")
    private DadosFaturamentoOrganizacao dadosFaturamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    @JsonIgnore
    private Plano plano;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "limites_personalizados_id")
    private PlanoLimites limitesPersonalizados;

//    @OneToOne(mappedBy = "organizacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//    private ConfigLandingPage configLandingpage;

    @OneToOne(mappedBy = "organizacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ConfigSistema configSistema;

    @Column(name = "dt_cadastro", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCadastro;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(nullable = false)
    private boolean ativo = true;

    // Relacionamentos Inversos
    @OneToMany(mappedBy = "organizacao")
    @JsonManagedReference("organizacao-clientes")
    private List<Cliente> clientes;

    @OneToMany(mappedBy = "organizacao")
    @JsonManagedReference("organizacao-funcionarios")
    private List<Funcionario> funcionarios;

    @OneToMany(mappedBy = "organizacao")
    private List<Servico> servicos;

    // Getters e Setters
}
