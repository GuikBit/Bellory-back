package org.exemplo.bellory.model.entity.fila;

public enum StatusFilaTentativa {
    PENDENTE,              // criada, aguardando envio do WhatsApp
    ENVIADO,               // mensagem enviada ao N8N
    AGUARDANDO_RESPOSTA,   // cliente recebeu, aguardando SIM/NAO
    ACEITO,                // cliente aceitou - agendamento foi adiantado
    RECUSADO,              // cliente recusou - proximo da fila e' notificado
    EXPIRADO,              // 30 min sem resposta - proximo da fila e' notificado
    SUPERADO,              // outro cliente aceitou antes desta tentativa ser respondida
    FALHA                  // erro no envio (instancia offline, etc.)
}
