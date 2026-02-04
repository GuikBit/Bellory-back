package org.exemplo.bellory.model.dto.financeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContaBancariaCreateDTO {
    private String nome;
    private String tipoConta; // CONTA_CORRENTE, POUPANCA, CAIXA, CARTEIRA_DIGITAL
    private String banco;
    private String agencia;
    private String numeroConta;
    private BigDecimal saldoInicial;
    private Boolean principal;
    private String cor;
    private String icone;
}
