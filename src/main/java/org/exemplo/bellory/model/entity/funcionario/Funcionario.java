package org.exemplo.bellory.model.entity.funcionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.Role;
import org.exemplo.bellory.model.entity.users.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "funcionario", schema = "app")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Funcionario extends User {

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(length = 14)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(name = "dataNasc")
    private LocalDate dataNasc;

    @Column(name = "dataContratacao")
    private LocalDateTime dataContratacao;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    @Column(name = "dataUpdate")
    private LocalDateTime dataUpdate;

    @Column(length = 20)
    private String sexo;

    private Integer nivel; // Ex: Nível de acesso ou hierárquico

    @Column(length = 100)
    private String apelido;

    @Column(length = 50)
    private String situacao; // "Ativo", "Inativo", etc.

    @Column(length = 10)
    private String cep;

    @Column(length = 255)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 255)
    private String complemento;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(length = 20)
    private String rg;

    @Column(name = "rg_org_emissor", length = 50)
    private String rgOrgEmissor;

    @Column(name = "titulo_eleitor", length = 30)
    private String tituloEleitor;

    @Column(name = "cert_militar", length = 30)
    private String certMilitar;

    @Column(length = 20)
    private String cnh;

    @Column(name = "categ_habilitacao", length = 10)
    private String categHabilitacao;

    @Column(length = 30)
    private String ctps;

    @Column(name = "ctps_serie", length = 20)
    private String ctpsSerie;

    @Column(name = "pis_pasep", length = 30)
    private String pisPasep;

    @Column(length = 100)
    private String naturalidade;

    @Column(name = "estado_civil", length = 50)
    private String estadoCivil;

    @Column(name = "grau_instrucao", length = 100)
    private String grauInstrucao;

    @Column(columnDefinition = "TEXT")
    private String formacao;

    @Column(precision = 10, scale = 2)
    private BigDecimal salario;

    @Column(name = "is_comissao")
    private boolean isComissao = false;

    @Column(name = "isVisivelExterno")
    private boolean isVisivelExterno;

    @Column(name= "isPrimeiroAcesso")
    private boolean isPrimeiroAcesso;

    // A comissão pode ser um percentual ou valor fixo, por isso String
    @Column(length = 50)
    private String comissao;

    @Column(name = "nome_mae", length = 255)
    private String nomeMae;

    @Column(name = "nome_pai", length = 255)
    private String nomePai;

    @Column(length = 100)
    private String banco;

    @Column(length = 20)
    private String agencia;

    @Column(length = 30)
    private String conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @Column(length = 20)
    private String operacao; // Para contas como Poupança na CEF

    @Column(name = "jornada_semanal", length = 50)
    private String jornadaSemanal; // Ex: "44 horas"

    // --- CAMPO DE ROLE COMO STRING ---
    private String role;

    // NOVO RELACIONAMENTO: JornadaDia ao invés de JornadaTrabalho
    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JornadaDia> jornadasDia = new ArrayList<>();

    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BloqueioAgenda> bloqueiosAgenda = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "funcionario_servico",
            schema = "app",
            joinColumns = @JoinColumn(name = "funcionario_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<Servico> servicos;

    // Métodos para ajudar a gerenciar a sincronia dos relacionamentos
    public void addJornadaDia(JornadaDia jornadaDia) {
        jornadasDia.add(jornadaDia);
        jornadaDia.setFuncionario(this);
    }

    public void removeJornadaDia(JornadaDia jornadaDia) {
        jornadasDia.remove(jornadaDia);
        jornadaDia.setFuncionario(null);
    }

    public void addBloqueio(BloqueioAgenda bloqueio) {
        bloqueiosAgenda.add(bloqueio);
        bloqueio.setFuncionario(this);
    }
}
