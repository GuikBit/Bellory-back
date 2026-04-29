package org.exemplo.bellory.model.entity.questionario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resposta_pergunta", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaPergunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resposta_questionario_id", nullable = false)
    @JsonIgnore
    private RespostaQuestionario respostaQuestionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pergunta_id", nullable = false)
    private Pergunta pergunta;

    @Column(name = "resposta_texto", length = 5000)
    private String respostaTexto;

    @Column(name = "resposta_numero", precision = 15, scale = 4)
    private BigDecimal respostaNumero;

    @ElementCollection
    @CollectionTable(
        name = "resposta_opcoes_selecionadas",
        schema = "app",
        joinColumns = @JoinColumn(name = "resposta_pergunta_id")
    )
    @Column(name = "opcao_id")
    @Builder.Default
    private List<Long> respostaOpcaoIds = new ArrayList<>();

    @Column(name = "resposta_data")
    private LocalDate respostaData;

    @Column(name = "resposta_hora")
    private LocalTime respostaHora;

    // ===== Termo de consentimento =====

    @Column(name = "aceitou_termo")
    private Boolean aceitouTermo;

    /**
     * Setado pelo servidor no momento do POST. Valor enviado pelo cliente e ignorado.
     */
    @Column(name = "data_aceite")
    private LocalDateTime dataAceite;

    /**
     * Snapshot do termo (com placeholders ja substituidos pelo front) no momento do aceite.
     * NUNCA recalculado depois.
     */
    @Column(name = "texto_termo_renderizado", columnDefinition = "TEXT")
    private String textoTermoRenderizado;

    /**
     * SHA-256 do {@code textoTermoRenderizado} calculado pelo servidor para auditoria.
     */
    @Column(name = "hash_termo", length = 64)
    private String hashTermo;

    // ===== Assinatura digital =====

    /**
     * FK logico para {@code app.arquivo} (is_sistema=true) com a assinatura do cliente.
     */
    @Column(name = "arquivo_assinatura_cliente_id")
    private Long arquivoAssinaturaClienteId;

    /**
     * FK logico para {@code app.arquivo} (is_sistema=true) com a assinatura do profissional.
     */
    @Column(name = "arquivo_assinatura_profissional_id")
    private Long arquivoAssinaturaProfissionalId;
}
