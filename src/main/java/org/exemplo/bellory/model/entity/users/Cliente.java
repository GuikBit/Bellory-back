package org.exemplo.bellory.model.entity.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.pagamento.CartaoCredito;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.compra.Compra;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente", schema = "app")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Cliente extends User {

    @Column(length = 15)
    private String telefone;

    private LocalDate dataNascimento;

    private Boolean isCadastroIncompleto;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Endereco> enderecos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartaoCredito> cartoesCredito = new ArrayList<>();

    // === NOVOS RELACIONAMENTOS ===
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Agendamento> agendamentos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Compra> compras = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Cobranca> cobrancas = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pagamento> pagamentos = new ArrayList<>();

    @Column(length = 14)
    private String cpf;

    // --- CAMPO DE ROLE COMO STRING ---
    private String role = "ROLE_CLIENTE";

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    // --- MÉTODO PARA IMPLEMENTAR getRole() da classe User ---
    @Override
    public String getRole() {
        return this.role;
    }

    // === MÉTODOS DE CONVENIÊNCIA PARA ENDEREÇOS ===
    public void adicionarEndereco(Endereco endereco) {
        endereco.setCliente(this);
        this.enderecos.add(endereco);
    }

    public void removerEndereco(Endereco endereco) {
        endereco.setCliente(null);
        this.enderecos.remove(endereco);
    }

    public Endereco getEnderecoPrincipal() {
        return this.enderecos.stream()
                .filter(Endereco::isPrincipal)
                .filter(Endereco::isAtivo)
                .findFirst()
                .orElse(null);
    }

    // === MÉTODOS DE CONVENIÊNCIA PARA CARTÕES DE CRÉDITO ===
    public void adicionarCartaoCredito(CartaoCredito cartao) {
        cartao.setCliente(this);
        this.cartoesCredito.add(cartao);
    }

    public void removerCartaoCredito(CartaoCredito cartao) {
        cartao.setCliente(null);
        this.cartoesCredito.remove(cartao);
    }

    public CartaoCredito getCartaoPrincipal() {
        return this.cartoesCredito.stream()
                .filter(CartaoCredito::isPrincipal)
                .filter(CartaoCredito::isAtivo)
                .findFirst()
                .orElse(null);
    }

    public List<CartaoCredito> getCartoesAtivos() {
        return this.cartoesCredito.stream()
                .filter(CartaoCredito::isAtivo)
                .toList();
    }

    // === NOVOS MÉTODOS DE CONVENIÊNCIA PARA AGENDAMENTOS ===
    public void adicionarAgendamento(Agendamento agendamento) {
        agendamento.setCliente(this);
        this.agendamentos.add(agendamento);
    }

    public List<Agendamento> getAgendamentosAtivos() {
        return this.agendamentos.stream()
                .filter(a -> a.getStatus() != null &&
                        !a.getStatus().name().equals("CANCELADO") &&
                        !a.getStatus().name().equals("CONCLUIDO"))
                .toList();
    }

    // === NOVOS MÉTODOS DE CONVENIÊNCIA PARA COMPRAS ===
    public void adicionarCompra(Compra compra) {
        compra.setCliente(this);
        this.compras.add(compra);
    }

    public List<Compra> getComprasFinalizadas() {
        return this.compras.stream()
                .filter(c -> !c.getStatusCompra().equals("CARRINHO") &&
                        !c.getStatusCompra().equals("CANCELADA"))
                .toList();
    }

    // === NOVOS MÉTODOS DE CONVENIÊNCIA PARA COBRANÇAS ===
    public void adicionarCobranca(Cobranca cobranca) {
        cobranca.setCliente(this);
        this.cobrancas.add(cobranca);
    }

    public List<Cobranca> getCobrancasPendentes() {
        return this.cobrancas.stream()
                .filter(c -> c.getStatusCobranca() != null &&
                        c.getStatusCobranca().name().equals("PENDENTE"))
                .toList();
    }

    // === NOVOS MÉTODOS DE CONVENIÊNCIA PARA PAGAMENTOS ===
    public void adicionarPagamento(Pagamento pagamento) {
        pagamento.setCliente(this);
        this.pagamentos.add(pagamento);
    }

    public List<Pagamento> getPagamentosConfirmados() {
        return this.pagamentos.stream()
                .filter(p -> p.getStatusPagamento().equals("CONFIRMADO"))
                .toList();
    }
}
