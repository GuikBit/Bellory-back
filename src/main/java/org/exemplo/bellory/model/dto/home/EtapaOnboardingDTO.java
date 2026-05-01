package org.exemplo.bellory.model.dto.home;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaOnboardingDTO {
    private String chave;
    private String label;
    private boolean concluida;
    private boolean obrigatoria;
    private int ordem;

    public static EtapaOnboardingDTO of(String chave, String label, boolean concluida, boolean obrigatoria, int ordem) {
        return EtapaOnboardingDTO.builder()
                .chave(chave)
                .label(label)
                .concluida(concluida)
                .obrigatoria(obrigatoria)
                .ordem(ordem)
                .build();
    }
}
