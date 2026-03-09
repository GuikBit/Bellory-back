package org.exemplo.bellory.model.entity.assinatura;

/**
 * Enum semantico para o frontend saber exatamente qual tela/modal exibir.
 * Diferente do StatusAssinatura (estado interno), este enum representa
 * a "situacao" do ponto de vista do usuario.
 */
public enum SituacaoAssinatura {

    /** Trial ativo - mostrar banner com dias restantes */
    TRIAL_ATIVO,

    /** Trial expirado - tela de escolha de plano (bloqueado) */
    TRIAL_EXPIRADO,

    /** Plano gratuito ativo - banner de upgrade */
    PLANO_GRATUITO,

    /** Assinatura ativa com plano pago - acesso normal */
    ATIVA,

    /** Ativa mas com cobranca pendente - alerta de pagamento */
    PAGAMENTO_PENDENTE,

    /** Pagamento atrasado - tela de regularizacao (bloqueado) */
    PAGAMENTO_ATRASADO,

    /** Cancelada mas ainda com acesso (grace period) */
    CANCELADA_COM_ACESSO,

    /** Cancelada sem acesso - tela para reativar (bloqueado) */
    CANCELADA_SEM_ACESSO,

    /** Suspensa pelo admin (bloqueado) */
    SUSPENSA,

    /** Nenhuma assinatura encontrada */
    SEM_ASSINATURA
}
