package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class FuncionarioUpdateDTO {

    // Dados Pessoais
    private String nomeCompleto;
    private String email;
    private String telefone;
    private String cpf;
    private LocalDate dataNasc;
    private String sexo;
    private String apelido;
    private String naturalidade;
    private String estadoCivil;
    private String nomeMae;
    private String nomePai;

    // Endereco
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;

    // Documentos
    private String rg;
    private String rgOrgEmissor;
    private String tituloEleitor;
    private String certMilitar;
    private String cnh;
    private String categHabilitacao;
    private String ctps;
    private String ctpsSerie;
    private String pisPasep;

    // Dados Profissionais
    private Long cargoId;
    private BigDecimal salario;
    private Boolean isComissao;
    private String comissao;
    private String jornadaSemanal;
    private Integer nivel;
    private String situacao;
    private String formacao;
    private String grauInstrucao;
    private LocalDateTime dataContratacao;
    private Boolean isVisivelExterno;

    // Dados Bancarios
    private String banco;
    private String agencia;
    private String conta;
    private String operacao;
}
