package org.exemplo.bellory.model.entity.notificacao;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao_enviada", schema = "app",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"agendamento_id", "tipo", "horas_antes"}
    ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacaoEnviada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private Agendamento agendamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoNotificacao tipo;

    @Column(name = "horas_antes", nullable = false)
    private Integer horasAntes;

    @Column(name = "dt_envio", nullable = false)
    private LocalDateTime dtEnvio;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusEnvio status = StatusEnvio.PENDENTE;

    @Column(name = "erro_mensagem", columnDefinition = "TEXT")
    private String erroMensagem;

    @Column(name = "whatsapp_message_id", length = 100)
    private String whatsappMessageId;

    @Column(name = "telefone_destino", length = 20)
    private String telefoneDestino;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
        if (dtEnvio == null) {
            dtEnvio = LocalDateTime.now();
        }
    }
}
