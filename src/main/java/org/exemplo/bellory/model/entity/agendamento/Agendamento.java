package org.exemplo.bellory.model.entity.agendamento;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp; // Importe para @UpdateTimestamp

import java.time.LocalDateTime;
import java.util.List; // Importe para List
import org.exemplo.bellory.model.entity.servico.Servico;


@Entity
@Table(name = "agendamento")
@Getter
@Setter
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "organizacao_id", nullable = false)
    private Long organizacaoId;

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
    private List<Funcionario> funcionarios;


    @Column(name = "data_hora_agendamento", nullable = false)
    private LocalDateTime dataHoraAgendamento;

    @Column(name = "observacao", length = 500)
    private String observacao;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @UpdateTimestamp
    @Column(name = "dt_atualizacao", nullable = false)
    private LocalDateTime dtAtualizacao;

    public Agendamento() {
        this.status = Status.PENDENTE;
    }

    public Agendamento(Long organizacaoId, Cliente cliente, List<Servico> servicos, List<Funcionario> funcionarios, LocalDateTime dataHoraAgendamento, String observacao, Status status, LocalDateTime dtCriacao, LocalDateTime dtAtualizacao) {
        this.organizacaoId = organizacaoId;
        this.cliente = cliente;
        this.servicos = servicos;
        this.funcionarios = funcionarios;
        this.dataHoraAgendamento = dataHoraAgendamento;
        this.observacao = observacao;
        this.status = status;
        this.dtCriacao = dtCriacao;
        this.dtAtualizacao = dtAtualizacao;
    }

    public <E> Agendamento(Long organizacaoId, Cliente cliente, List<Servico> servicos, List<Funcionario> funcionarios, LocalDateTime dataHoraAgendamento, String observacao) {
        this.organizacaoId = organizacaoId;
        this.cliente = cliente;
        this.servicos = servicos;
        this.funcionarios = funcionarios;
        this.dataHoraAgendamento = dataHoraAgendamento;
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
}
