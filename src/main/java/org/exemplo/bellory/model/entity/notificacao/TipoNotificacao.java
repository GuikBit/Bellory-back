package org.exemplo.bellory.model.entity.notificacao;

public enum TipoNotificacao {
    CONFIRMACAO,            // 12/24/36/48h antes - pede confirmacao
    LEMBRETE,               // 1/2/3/4/5/6h antes - apenas lembra
    FILA_ESPERA_OFERTA,     // Surgiu horario antes - cliente aceita adiantar?
    FILA_ESPERA_PERDEU_VEZ  // Cliente respondeu tarde/outro aceitou primeiro
}
