package org.exemplo.bellory.model.dto.notificacao;

import jakarta.validation.constraints.*;
import lombok.*;
import org.exemplo.bellory.model.entity.notificacao.ConfigNotificacao;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigNotificacaoDTO {

    private Long id;

    @NotNull(message = "Tipo e obrigatorio")
    private TipoNotificacao tipo;

    @NotNull(message = "Horas antes e obrigatorio")
    @Min(value = 1, message = "Minimo 1 hora")
    @Max(value = 48, message = "Maximo 48 horas")
    private Integer horasAntes;

    @Builder.Default
    private Boolean ativo = true;

    private String mensagemTemplate;

    public ConfigNotificacaoDTO(ConfigNotificacao entity) {
        this.id = entity.getId();
        this.tipo = entity.getTipo();
        this.horasAntes = entity.getHorasAntes();
        this.ativo = entity.getAtivo();
        this.mensagemTemplate = entity.getMensagemTemplate();
    }
}
