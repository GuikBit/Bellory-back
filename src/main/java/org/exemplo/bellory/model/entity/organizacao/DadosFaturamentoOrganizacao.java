package org.exemplo.bellory.model.entity.organizacao;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dados_faturamento_organizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DadosFaturamentoOrganizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false, unique = true)
    @JsonBackReference("organizacao-faturamento")
    private Organizacao organizacao;

    @Column(name = "cpf_cnpj", length = 18)
    private String cpfCnpj;

    @Column(length = 10)
    private String agencia;

    @Column(name = "conta_numero", length = 20)
    private String contaNumero;

    @Column(name = "conta_verificador", length = 2)
    private String contaVerificador;

    @Column(name = "numero_cartao", length = 19)
    private String numeroCartao;

    @Column(name = "dt_validade_cartao")
    private LocalDate dtValidadeCartao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "dt_cadastro", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCadastro;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // Getters e Setters
}
