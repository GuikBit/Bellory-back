package org.exemplo.bellory.exception;

import lombok.Getter;

/**
 * Lancada quando uma operacao de criacao (cliente, agendamento, funcionario, etc.)
 * excederia o limite do plano contratado na Payment API.
 */
@Getter
public class LimitePlanoExcedidoException extends RuntimeException {

    private final String limitKey;
    private final Long limiteMaximo;
    private final Integer usoAtual;

    public LimitePlanoExcedidoException(String message, String limitKey, Long limiteMaximo, Integer usoAtual) {
        super(message);
        this.limitKey = limitKey;
        this.limiteMaximo = limiteMaximo;
        this.usoAtual = usoAtual;
    }

    public LimitePlanoExcedidoException(String message, String limitKey) {
        this(message, limitKey, null, null);
    }
}
