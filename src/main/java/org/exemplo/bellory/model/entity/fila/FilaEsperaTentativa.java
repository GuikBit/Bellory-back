package org.exemplo.bellory.model.entity.fila;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "fila_espera_tentativa", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaEsperaTentativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    // Cliente / agendamento que esta na fila e que recebeu a oferta de adiantamento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private Agendamento agendamento;

    // Agendamento que foi cancelado e abriu o slot (origem da oferta)
    @Column(name = "agendamento_cancelado_id", nullable = false)
    private Long agendamentoCanceladoId;

    @Column(name = "funcionario_id", nullable = false)
    private Long funcionarioId;

    // Notificacao WhatsApp enviada (preenchido apos disparo)
    @Column(name = "notificacao_enviada_id")
    private Long notificacaoEnviadaId;

    // Tentativa de origem na cadeia de cascata (null se primeira)
    @Column(name = "cascata_origem_id")
    private Long cascataOrigemId;

    // Nivel da cascata (1 = primeira, ate fila_max_cascata)
    @Column(name = "cascata_nivel", nullable = false)
    private Integer cascataNivel = 1;

    @Column(name = "slot_inicio", nullable = false)
    private LocalDateTime slotInicio;

    @Column(name = "slot_fim", nullable = false)
    private LocalDateTime slotFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusFilaTentativa status = StatusFilaTentativa.PENDENTE;

    @Column(name = "dt_envio")
    private LocalDateTime dtEnvio;

    @Column(name = "dt_resposta")
    private LocalDateTime dtResposta;

    // now + fila_timeout_minutos. Apos esse instante, scheduler marca EXPIRADO.
    @Column(name = "dt_expira", nullable = false)
    private LocalDateTime dtExpira;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    protected void onCreate() {
        if (dtCriacao == null) {
            dtCriacao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusFilaTentativa.PENDENTE;
        }
        if (cascataNivel == null) {
            cascataNivel = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }

    public boolean isAguardandoResposta() {
        return status == StatusFilaTentativa.AGUARDANDO_RESPOSTA;
    }

    public boolean isFinalizada() {
        return status == StatusFilaTentativa.ACEITO
                || status == StatusFilaTentativa.RECUSADO
                || status == StatusFilaTentativa.EXPIRADO
                || status == StatusFilaTentativa.SUPERADO
                || status == StatusFilaTentativa.FALHA;
    }
}
