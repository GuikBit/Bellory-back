package org.exemplo.bellory.model.entity.agendamento;

/**
 * Estado da captura de assinatura digital em um questionario vinculado a um agendamento.
 * Independente do {@link StatusQuestionarioAgendamento} (que controla a resposta).
 *
 * Permite combinacoes como "respondido mas ainda nao assinou" ou
 * "respondido e assinado".
 */
public enum StatusAssinatura {

    /** O questionario nao tem pergunta tipo ASSINATURA - assinatura nao se aplica. */
    NAO_REQUERIDA,

    /** Existe pergunta tipo ASSINATURA mas a captura ainda nao foi recebida. */
    PENDENTE,

    /** Assinatura(s) foram capturadas com sucesso. */
    ASSINADA
}
