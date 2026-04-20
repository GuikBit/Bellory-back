package org.exemplo.bellory.model.dto.home;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvisoDTO {
    private String id;
    private String tipo;       // COBRANCA, LIMITE, PLANO, SISTEMA
    private String severidade;  // INFO, WARNING, ERROR
    private String titulo;
    private String mensagem;
    private String cta;         // Call to action (ex: "Ver cobranças")
    private boolean dispensavel;
}
