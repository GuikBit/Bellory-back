package org.exemplo.bellory.model.entity.agendamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;

import java.time.LocalDateTime;
import java.util.List; // Importe para List
import org.exemplo.bellory.model.entity.servico.Servico;


@AllArgsConstructor
@Entity
@Table(name = "agendamento")
@Getter
@Setter
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "agendamento_servico",
            joinColumns = @JoinColumn(name = "agendamento_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<Servico> servicos;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "agendamento_funcionario",
            joinColumns = @JoinColumn(name = "agendamento_id"),
            inverseJoinColumns = @JoinColumn(name = "funcionario_id")
    )
    @JsonIgnore
    private List<Funcionario> funcionarios;

    @OneToOne(mappedBy = "agendamento")
    private Cobranca cobranca;


    @Column(name = "dtAgendamento", nullable = false)
    private LocalDateTime dtAgendamento;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    // --- NOVO RELACIONAMENTO BIDIRECIONAL ---
    // Mapeia a relação com BloqueioAgenda, onde BloqueioAgenda possui a chave estrangeira (agendamento_id).
    // Cascade.ALL garante que ao salvar um Agendamento, o Bloqueio associado também será salvo.
    @OneToOne(mappedBy = "agendamento", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private BloqueioAgenda bloqueioAgenda;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "dtCriacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dtAtualizacao")
    private LocalDateTime dtAtualizacao;

    public Agendamento() {
        this.status = Status.PENDENTE;
    }

    public Agendamento(Organizacao organizacao, Cliente cliente, List<Servico> servicos, List<Funcionario> funcionarios, LocalDateTime dtAgendamento, String observacao, Status status, LocalDateTime dtCriacao, LocalDateTime dtAtualizacao) {
        this.organizacao = organizacao;
        this.cliente = cliente;
        this.servicos = servicos;
        this.funcionarios = funcionarios;
        this.dtAgendamento = dtAgendamento;
        this.observacao = observacao;
        this.status = status;
        this.dtCriacao = dtCriacao;
        this.dtAtualizacao = dtAtualizacao;
    }

    public <E> Agendamento(Organizacao organizacao, Cliente cliente, List<Servico> servicos, List<Funcionario> funcionarios, LocalDateTime dtAgendamento, String observacao) {
        this.organizacao = organizacao;
        this.cliente = cliente;
        this.servicos = servicos;
        this.funcionarios = funcionarios;
        this.dtAgendamento = dtAgendamento;
        this.observacao = observacao;

    }

    public void marcarComoAgendado() {
        if (this.status == Status.PENDENTE || this.status == Status.EM_ESPERA) {
            this.status = Status.AGENDADO;
        } else {
            throw new IllegalStateException("Não é possível mudar o status de " + this.status + " para AGENDADO.");
        }
    }

    public void marcarComoConcluido() {
        if (this.status == Status.AGENDADO || this.status == Status.EM_ESPERA) {
            this.status = Status.CONCLUIDO;
        } else {
            throw new IllegalStateException("Não é possível mudar o status de " + this.status + " para CONCLUÍDO.");
        }
    }

    public void cancelarAgendamento() {
        if (this.status != Status.CONCLUIDO && this.status != Status.CANCELADO) {
            this.status = Status.CANCELADO;
        } else {
            throw new IllegalStateException("Não é possível cancelar um agendamento com status " + this.status + ".");
        }
    }

    public void colocarEmEspera() {
        if (this.status == Status.PENDENTE || this.status == Status.AGENDADO) {
            this.status = Status.EM_ESPERA;
        } else {
            throw new IllegalStateException("Não é possível colocar em espera um agendamento com status " + this.status + ".");
        }
    }

    public void adicionarServico(Servico servico) {
        if (this.servicos == null) {
            this.servicos = new java.util.ArrayList<>();
        }
        if (!this.servicos.contains(servico)) {
            this.servicos.add(servico);
        }
    }

    public void removerServico(Servico servico) {
        if (this.servicos != null) {
            this.servicos.remove(servico);
        }
    }

    public void adicionarFuncionario(Funcionario funcionario) {
        if (this.funcionarios == null) {
            this.funcionarios = new java.util.ArrayList<>();
        }
        if (!this.funcionarios.contains(funcionario)) {
            this.funcionarios.add(funcionario);
        }
    }

    public void removerFuncionario(Funcionario funcionario) {
        if (this.funcionarios != null) {
            this.funcionarios.remove(funcionario);
        }
    }

    // Método auxiliar para manter a sincronia
    public void setBloqueioAgenda(BloqueioAgenda bloqueio) {
        this.bloqueioAgenda = bloqueio;
        if (bloqueio != null) {
            bloqueio.setAgendamento(this);
        }
    }
}
