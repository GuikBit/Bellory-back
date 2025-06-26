package org.exemplo.bellory.model.entity.pagamento;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobranca_id", nullable = false)
    @JsonBackReference("cobranca-pagamentos")
    private Cobranca cobranca;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    @Column(name = "metodo_pagamento", nullable = false, length = 50)
    private String metodoPagamento; // "Cartao de Credito", "Dinheiro", "Pix"

    @Column(name = "status_pagamento", nullable = false, length = 20)
    private String statusPagamento; // "Pendente", "Confirmado", "Recusado"

    @Column(name = "transacao_id", length = 255)
    private String transacaoId;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    // Getters e Setters
}