package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.servico.Servico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List; // Importar List
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class FuncionarioDTO {

    private Long id;
    private Long idOrganizacao;
    private String fotoPerfilUrl;
    private String nomeCompleto; // Mantido de 'nome'
    private String username; // Mantido de 'login'
    private String password;
    private String cpf;
    private String email;
    private String telefone;
    private LocalDate dataNasc;
    private LocalDateTime dataContratacao;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataUpdate;
    private String sexo;
    private Integer nivel;
    private String apelido;
    private String situacao;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String rg;
    private String rgOrgEmissor;
    private String tituloEleitor;
    private String certMilitar;
    private String cnh;
    private String categHabilitacao;
    private String ctps;
    private String ctpsSerie;
    private String pisPasep;
    private String naturalidade;
    private String estadoCivil;
    private String grauInstrucao;
    private String formacao;
    private Cargo cargo;
    private BigDecimal salario;
    private boolean isComissao;
    private String comissao;
    private String nomeMae;
    private String nomePai;
    private String banco;
    private String agencia;
    private String conta;
    private String operacao;
    private String jornadaSemanal;
    private boolean ativo;
    private boolean isVisivelExterno;
    private String role;

    // As listas agora usam os novos DTOs
    private List<JornadaDiaDTO> jornadaDia;
    private List<BloqueioAgendaDTO> bloqueiosAgenda;
    private List<Integer> servicosId;

    public FuncionarioDTO(Funcionario funcionario, List<BloqueioAgendaDTO> bloqueiosDTO, List<JornadaDiaDTO> jornadaDTO, List<Servico> servicos) {
        this.id = funcionario.getId();
        this.idOrganizacao = funcionario.getOrganizacao().getId();
        this.fotoPerfilUrl = funcionario.getFotoPerfil();
        this.nomeCompleto = funcionario.getNomeCompleto();
        this.username = funcionario.getUsername();
        this.cpf = funcionario.getCpf();
        this.email = funcionario.getEmail();
        this.telefone = funcionario.getTelefone();
        this.dataNasc = funcionario.getDataNasc();
        this.dataContratacao = funcionario.getDataContratacao();
        this.dataCriacao = funcionario.getDataCriacao();
        this.dataUpdate = funcionario.getDataUpdate();
        this.sexo = funcionario.getSexo();
        this.nivel = funcionario.getNivel();
        this.apelido = funcionario.getApelido();
        this.situacao = funcionario.getSituacao();
        this.cep = funcionario.getCep();
        this.logradouro = funcionario.getLogradouro();
        this.numero = funcionario.getNumero();
        this.complemento = funcionario.getComplemento();
        this.bairro = funcionario.getBairro();
        this.cidade = funcionario.getCidade();
        this.uf = funcionario.getUf();
        this.rg = funcionario.getRg();
        this.rgOrgEmissor = funcionario.getRgOrgEmissor();
        this.tituloEleitor = funcionario.getTituloEleitor();
        this.certMilitar = funcionario.getCertMilitar();
        this.cnh = funcionario.getCnh();
        this.categHabilitacao = funcionario.getCategHabilitacao();
        this.ctps = funcionario.getCtps();
        this.ctpsSerie = funcionario.getCtpsSerie();
        this.pisPasep = funcionario.getPisPasep();
        this.naturalidade = funcionario.getNaturalidade();
        this.estadoCivil = funcionario.getEstadoCivil();
        this.grauInstrucao = funcionario.getGrauInstrucao();
        this.formacao = funcionario.getFormacao();
        this.cargo = funcionario.getCargo();
        this.salario = funcionario.getSalario();
        this.isComissao = funcionario.isComissao();
        this.comissao = funcionario.getComissao();
        this.nomeMae = funcionario.getNomeMae();
        this.nomePai = funcionario.getNomePai();
        this.banco = funcionario.getBanco();
        this.agencia = funcionario.getAgencia();
        this.conta = funcionario.getConta();
        this.operacao = funcionario.getOperacao();
        this.jornadaSemanal = funcionario.getJornadaSemanal();
        this.ativo = funcionario.isAtivo();
        this.isVisivelExterno = funcionario.isVisivelExterno();

        // Extrai a primeira role encontrada (simplificação)
        this.role = funcionario.getRole();

        // Mapeia as listas de entidades para listas de DTOs
        this.jornadaDia = jornadaDTO;

        this.bloqueiosAgenda = bloqueiosDTO;
        this.servicosId = servicos.stream().map(servico -> servico.getId().intValue()).collect(Collectors.toList());
    }
}
