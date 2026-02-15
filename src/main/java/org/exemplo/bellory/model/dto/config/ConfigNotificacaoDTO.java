package org.exemplo.bellory.model.dto.config;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigNotificacaoDTO {
    private Boolean enviarLembreteWhatsapp ;
    private Boolean enviarLembreteSMS ;
    private Boolean enviarLembreteEmail ;
    private Boolean enviarConfirmacaoForaHorario;
    private Integer tempoParaConfirmacao;
    private Integer tempoLembretePosConfirmacao;
}
