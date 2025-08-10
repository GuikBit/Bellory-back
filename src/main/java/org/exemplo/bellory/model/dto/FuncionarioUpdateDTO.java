package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class FuncionarioUpdateDTO {

    // Dados Pessoais
    private String nomeCompleto;
    private String email;
    private String telefone;
    private LocalDate dataNasc;
    private String sexo;
    private String apelido;

    // Endereço
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;

    // Dados Profissionais
    private String cargo;
    private BigDecimal salario;
    private boolean isComissao;
    private String comissao;
    private String jornadaSemanal;

    // O status (ativo/inativo) pode ser controlado separadamente se necessário
    // private Boolean ativo;
}
