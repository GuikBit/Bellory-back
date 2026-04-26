package org.exemplo.bellory.model.entity.agendamento;

public enum StatusQuestionarioAgendamento {
    PENDENTE,    // criado, aguardando disparo (instância offline ou ainda não tentou)
    ENVIADO,     // mensagem WhatsApp enviada com sucesso, aguardando resposta do cliente
    RESPONDIDO,  // cliente respondeu o questionário
    FALHOU       // falha permanente no envio
}
