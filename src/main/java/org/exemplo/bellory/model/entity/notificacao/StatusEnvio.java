package org.exemplo.bellory.model.entity.notificacao;

public enum StatusEnvio {
    PENDENTE,              // Aguardando envio
    ENVIADO,               // Enviado com sucesso para o n8n
    ENTREGUE,              // Confirmacao de entrega do WhatsApp
    AGUARDANDO_RESPOSTA,   // Mensagem enviada, aguardando SIM/NAO/REAGENDAR
    AGUARDANDO_DATA,       // Aguardando cliente informar nova data para reagendamento
    AGUARDANDO_HORARIO,    // Aguardando cliente selecionar horario
    CONFIRMADO,            // Cliente respondeu SIM - agendamento confirmado
    CANCELADO_CLIENTE,     // Cliente respondeu NAO - agendamento cancelado
    REAGENDADO,            // Cliente reagendou com sucesso
    FALHA,                 // Erro no envio
    CANCELADO,             // Agendamento cancelado antes do envio
    EXPIRADO               // Tempo de resposta expirou
}
