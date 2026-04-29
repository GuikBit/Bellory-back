package org.exemplo.bellory.model.dto.questionario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta do endpoint de auditoria de termos. Consolida prova legal por resposta:
 * snapshot do termo, hash esperado vs recalculado e metadados de captura.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaTermoDTO {

    private Long respostaQuestionarioId;
    private Long questionarioId;
    private String questionarioTitulo;
    private Long clienteId;
    private Long agendamentoId;
    private LocalDateTime dtResposta;
    private String ipOrigem;
    private String userAgent;
    private String dispositivo;
    private boolean deletado;
    private LocalDateTime dtDeletado;

    private List<TermoAceito> termos;
    private List<AssinaturaCapturada> assinaturas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TermoAceito {
        private Long respostaPerguntaId;
        private Long perguntaId;
        private String perguntaTexto;
        private Boolean aceitouTermo;
        private LocalDateTime dataAceite;
        private String textoTermoRenderizado;
        private String hashTermoEsperado;
        private String hashTermoCalculado;
        private boolean integridadeOk;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssinaturaCapturada {
        private Long respostaPerguntaId;
        private Long perguntaId;
        private String perguntaTexto;
        private Long arquivoAssinaturaClienteId;
        private Long arquivoAssinaturaProfissionalId;
        /** URL relativa para download via {@code GET /api/v1/resposta-questionario/{id}/assinatura/cliente}. */
        private String urlAssinaturaCliente;
        /** URL relativa para download via {@code GET /api/v1/resposta-questionario/{id}/assinatura/profissional}. */
        private String urlAssinaturaProfissional;
    }
}
