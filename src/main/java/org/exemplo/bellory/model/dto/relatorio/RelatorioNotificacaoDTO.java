package org.exemplo.bellory.model.dto.relatorio;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioNotificacaoDTO {

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String periodoConsulta;

    // Confirmacoes
    private ConfirmacoesResumoDTO confirmacoes;

    // Lembretes
    private LembretesResumoDTO lembretes;

    // Falhas
    private FalhasResumoDTO falhas;

    // Efetividade
    private EfetividadeDTO efetividade;

    // Evolucao no periodo
    private List<NotificacaoPeriodoDTO> evolucao;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfirmacoesResumoDTO {
        private Long totalEnviadas;
        private Long confirmadas;
        private Long canceladasPeloCliente;
        private Long reagendadas;
        private Long aguardandoResposta;
        private Long expiradas;
        private Double taxaResposta;
        private Double taxaConfirmacao;
        private Double taxaCancelamento;
        private Double taxaReagendamento;
        private Map<String, Long> porStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LembretesResumoDTO {
        private Long totalEnviados;
        private Long entregues;
        private Long falhas;
        private Double taxaEntrega;
        private Map<String, Long> porStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FalhasResumoDTO {
        private Long totalFalhas;
        private Double taxaFalha;
        private Map<String, Long> errosMaisComuns;
        private List<FalhaDetalheDTO> falhasRecentes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FalhaDetalheDTO {
        private Long id;
        private String tipo;
        private String telefoneDestino;
        private String erroMensagem;
        private String dtEnvio;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EfetividadeDTO {
        private Double taxaNoShowComConfirmacao;
        private Double taxaNoShowSemConfirmacao;
        private Double reducaoNoShow;
        private Long agendamentosComNotificacao;
        private Long agendamentosSemNotificacao;
        private Long noShowComNotificacao;
        private Long noShowSemNotificacao;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificacaoPeriodoDTO {
        private String periodo;
        private Long confirmacoesEnviadas;
        private Long lembretesEnviados;
        private Long falhas;
    }
}
