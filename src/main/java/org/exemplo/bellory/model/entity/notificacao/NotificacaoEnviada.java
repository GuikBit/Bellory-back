package org.exemplo.bellory.model.entity.notificacao;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;

import java.time.LocalDate;
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
    @Column(name = "status", nullable = false, length = 30)
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

    // Campos para rastrear o fluxo de reagendamento
    @Column(name = "data_desejada_reagendamento")
    private LocalDate dataDesejadaReagendamento;

    @Column(name = "horarios_disponiveis", columnDefinition = "TEXT")
    private String horariosDisponiveis; // JSON com os horários disponíveis

    @Column(name = "dt_resposta")
    private LocalDateTime dtResposta;

    @Column(name = "resposta_cliente", length = 50)
    private String respostaCliente; // SIM, NAO, REAGENDAR

    @Column(name = "instance_name", length = 100)
    private String instanceName;

    @PrePersist
    protected void onCreate() {
        dtCriacao = LocalDateTime.now();
        if (dtEnvio == null) {
            dtEnvio = LocalDateTime.now();
        }
    }

    // Métodos auxiliares para verificar estados
    public boolean isAguardandoResposta() {
        return status == StatusEnvio.AGUARDANDO_RESPOSTA;
    }

    public boolean isAguardandoData() {
        return status == StatusEnvio.AGUARDANDO_DATA;
    }

    public boolean isAguardandoHorario() {
        return status == StatusEnvio.AGUARDANDO_HORARIO;
    }

    public boolean isFinalizado() {
        return status == StatusEnvio.CONFIRMADO ||
               status == StatusEnvio.CANCELADO_CLIENTE ||
               status == StatusEnvio.REAGENDADO ||
               status == StatusEnvio.CANCELADO ||
               status == StatusEnvio.EXPIRADO;
    }
}
