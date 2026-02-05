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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List; // Importe para List
import java.util.Optional;

import org.exemplo.bellory.model.entity.servico.Servico;


@AllArgsConstructor
@Entity
@Table(name = "agendamento", schema = "app")
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
            schema = "app",
            joinColumns = @JoinColumn(name = "agendamento_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<Servico> servicos;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "agendamento_funcionario",
            schema = "app",
            joinColumns = @JoinColumn(name = "agendamento_id"),
            inverseJoinColumns = @JoinColumn(name = "funcionario_id")
    )
    @JsonIgnore
    private List<Funcionario> funcionarios;

    // ALTERADO: Relacionamento OneToMany para suportar múltiplas cobranças (sinal + restante)
    @OneToMany(mappedBy = "agendamento", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Cobranca> cobrancas = new ArrayList<>();

    @Column(name = "dtAgendamento", nullable = false)
    private LocalDateTime dtAgendamento;

    @Column(columnDefinition = "TEXT")
    private String observacao;

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

    // NOVO: Data de confirmação do agendamento (quando sinal é pago)
    @Column(name = "dt_confirmacao")
    private LocalDateTime dtConfirmacao;

    // NOVO: Indica se requer pagamento de sinal
    @Column(name = "requer_sinal")
    private Boolean requerSinal = true;

    // NOVO: Percentual do sinal (ex: 30%)
    @Column(name = "percentual_sinal", precision = 5, scale = 2)
    private BigDecimal percentualSinal;

    public Agendamento() {
        this.status = Status.PENDENTE;
        this.requerSinal = true;
    }

    public Agendamento(Organizacao organizacao, Cliente cliente, List<Servico> servicos,
                       List<Funcionario> funcionarios, LocalDateTime dtAgendamento,
                       String observacao, Status status, LocalDateTime dtCriacao,
                       LocalDateTime dtAtualizacao) {
        this.organizacao = organizacao;
        this.cliente = cliente;
        this.servicos = servicos;
        this.funcionarios = funcionarios;
        this.dtAgendamento = dtAgendamento;
        this.observacao = observacao;
        this.status = status;
        this.dtCriacao = dtCriacao;
        this.dtAtualizacao = dtAtualizacao;
        this.requerSinal = true;
    }

    public <E> Agendamento(Organizacao organizacao, Cliente cliente, List<Servico> servicos,
                           List<Funcionario> funcionarios, LocalDateTime dtAgendamento,
                           String observacao) {
        this.organizacao = organizacao;
        this.cliente = cliente;
        this.servicos = servicos;
        this.funcionarios = funcionarios;
        this.dtAgendamento = dtAgendamento;
        this.observacao = observacao;
        this.requerSinal = true;
    }

    // === MÉTODOS DE MUDANÇA DE STATUS ===

    public void marcarComoAgendado() {
        if (this.status == Status.PENDENTE ||
            this.status == Status.EM_ESPERA ||
            this.status == Status.REAGENDADO) {
            this.status = Status.AGENDADO;
        } else {
            throw new IllegalStateException("Não é possível mudar o status de " + this.status + " para AGENDADO.");
        }
    }

    // NOVO: Confirma agendamento quando sinal é pago
    public void confirmarAgendamento() {
        if (this.status == Status.PENDENTE) {
            this.status = Status.AGENDADO;
            this.dtConfirmacao = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Não é possível confirmar agendamento com status " + this.status + ".");
        }
    }

    public void marcarComoConcluido() {
        if (this.status == Status.AGENDADO ||
            this.status == Status.EM_ESPERA ||
            this.status == Status.EM_ANDAMENTO) {
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
        if (this.status == Status.PENDENTE ||
            this.status == Status.AGENDADO ||
            this.status == Status.CONFIRMADO) {
            this.status = Status.EM_ESPERA;
        } else {
            throw new IllegalStateException("Não é possível colocar em espera um agendamento com status " + this.status + ".");
        }
    }

    // === MÉTODOS AUXILIARES PARA SERVIÇOS ===

    public void adicionarServico(Servico servico) {
        if (this.servicos == null) {
            this.servicos = new ArrayList<>();
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
            this.funcionarios = new ArrayList<>();
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

    // === MÉTODOS AUXILIARES PARA COBRANÇAS ===

    public void adicionarCobranca(Cobranca cobranca) {
        if (this.cobrancas == null) {
            this.cobrancas = new ArrayList<>();
        }
        if (!this.cobrancas.contains(cobranca)) {
            this.cobrancas.add(cobranca);
            cobranca.setAgendamento(this);
        }
    }

    // NOVO: Obtém a cobrança do sinal
    public Optional<Cobranca> getCobrancaSinal() {
        if (this.cobrancas == null) return Optional.empty();
        return this.cobrancas.stream()
                .filter(c -> c.getSubtipoCobrancaAgendamento() == Cobranca.SubtipoCobrancaAgendamento.SINAL)
                .findFirst();
    }

    // NOVO: Obtém a cobrança do restante
    public Optional<Cobranca> getCobrancaRestante() {
        if (this.cobrancas == null) return Optional.empty();
        return this.cobrancas.stream()
                .filter(c -> c.getSubtipoCobrancaAgendamento() == Cobranca.SubtipoCobrancaAgendamento.RESTANTE)
                .findFirst();
    }

    // NOVO: Obtém a cobrança integral
    public Optional<Cobranca> getCobrancaIntegral() {
        if (this.cobrancas == null) return Optional.empty();
        return this.cobrancas.stream()
                .filter(c -> c.getSubtipoCobrancaAgendamento() == Cobranca.SubtipoCobrancaAgendamento.INTEGRAL)
                .findFirst();
    }

    // NOVO: Verifica se o sinal foi pago
    public boolean isSinalPago() {
        return getCobrancaSinal()
                .map(Cobranca::isPaga)
                .orElse(false);
    }

    // NOVO: Verifica se o pagamento está completo
    public boolean isPagamentoCompleto() {
        // Verifica se há pelo menos uma cobrança
        if (cobrancas == null || cobrancas.isEmpty()) {
            return false;
        }

        // Verifica se todas as cobranças estão pagas
        return cobrancas.stream()
                .allMatch(Cobranca::isPaga);
    }

    // NOVO: Verifica se está confirmado (sinal pago ou não requer sinal)
    public boolean isConfirmado() {
        return this.dtConfirmacao != null ||
                !this.requerSinal ||
                isSinalPago();
    }

    // === MÉTODOS AUXILIARES PARA BLOQUEIO ===

    public void setBloqueioAgenda(BloqueioAgenda bloqueio) {
        this.bloqueioAgenda = bloqueio;
        if (bloqueio != null) {
            bloqueio.setAgendamento(this);
        }
    }

    // === CÁLCULOS ===

    // NOVO: Calcula valor total dos serviços
    public BigDecimal calcularValorTotal() {
        if (this.servicos == null || this.servicos.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return this.servicos.stream()
                .map(Servico::getPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
