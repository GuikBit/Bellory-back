package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.financeiro.ContaBancaria;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContaBancariaResponseDTO {
    private Long id;
    private String nome;
    private String tipoConta;
    private String tipoContaDescricao;
    private String banco;
    private String agencia;
    private String numeroConta;
    private BigDecimal saldoInicial;
    private BigDecimal saldoAtual;
    private Boolean principal;
    private Boolean ativo;
    private String cor;
    private String icone;

    public ContaBancariaResponseDTO(ContaBancaria entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.tipoConta = entity.getTipoConta() != null ? entity.getTipoConta().name() : null;
        this.tipoContaDescricao = entity.getTipoConta() != null ? entity.getTipoConta().getDescricao() : null;
        this.banco = entity.getBanco();
        this.agencia = entity.getAgencia();
        this.numeroConta = entity.getNumeroConta();
        this.saldoInicial = entity.getSaldoInicial();
        this.saldoAtual = entity.getSaldoAtual();
        this.principal = entity.getPrincipal();
        this.ativo = entity.getAtivo();
        this.cor = entity.getCor();
        this.icone = entity.getIcone();
    }
}
