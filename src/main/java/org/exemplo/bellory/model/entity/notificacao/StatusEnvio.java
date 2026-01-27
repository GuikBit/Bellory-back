package org.exemplo.bellory.model.entity.notificacao;

public enum StatusEnvio {
    PENDENTE,    // Aguardando envio
    ENVIADO,     // Enviado com sucesso para o n8n
    ENTREGUE,    // Confirmacao de entrega do WhatsApp
    FALHA,       // Erro no envio
    CANCELADO    // Agendamento cancelado antes do envio
}
