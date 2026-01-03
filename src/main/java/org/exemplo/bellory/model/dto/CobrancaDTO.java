package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CobrancaDTO {
    private Long id;
    private BigDecimal valor;
    private BigDecimal valorPago;
    private BigDecimal valorPendente;
    private Cobranca.StatusCobranca statusCobranca;
    private Cobranca.TipoCobranca tipoCobranca;
    private Cobranca.SubtipoCobrancaAgendamento subtipoCobrancaAgendamento;
    private String descricaoSubtipo; // "Sinal/Entrada", "Pagamento Final", etc.
    private LocalDate dtVencimento;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
    private LocalDateTime dtPagamentoCompleto;
    private String numeroCobranca;
    private String observacoes;
    private Boolean permiteParcelamento;
    private BigDecimal percentualSinal;
    private Long cobrancaRelacionadaId; // ID da cobrança relacionada (sinal <-> restante)
    private String gatewayPaymentId;
    private String gatewayPaymentIntentId;
    private boolean isVencida;
    private int diasAtraso;
    private String descricaoTransacao;

    // Informações do cliente
    private Long clienteId;
    private String nomeCliente;

    // Informações de relacionamento
    private Long agendamentoId;
    private Long compraId;

    public CobrancaDTO(Cobranca cobranca) {
        if (cobranca != null) {
            this.id = cobranca.getId();
            this.valor = cobranca.getValor();
            this.valorPago = cobranca.getValorPago();
            this.valorPendente = cobranca.getValorPendente();
            this.statusCobranca = cobranca.getStatusCobranca();
            this.tipoCobranca = cobranca.getTipoCobranca();
            this.subtipoCobrancaAgendamento = cobranca.getSubtipoCobrancaAgendamento();

            // Descrição amigável do subtipo
            if (cobranca.getSubtipoCobrancaAgendamento() != null) {
                this.descricaoSubtipo = cobranca.getSubtipoCobrancaAgendamento().getDescricao();
            }

            this.dtVencimento = cobranca.getDtVencimento();
            this.dtCriacao = cobranca.getDtCriacao();
            this.dtAtualizacao = cobranca.getDtAtualizacao();
            this.dtPagamentoCompleto = cobranca.getDtPagamentoCompleto();
            this.numeroCobranca = cobranca.getNumeroCobranca();
            this.observacoes = cobranca.getObservacoes();
            this.permiteParcelamento = cobranca.getPermiteParcelamento();
            this.percentualSinal = cobranca.getPercentualSinal();

            // Cobrança relacionada
            if (cobranca.getCobrancaRelacionada() != null) {
                this.cobrancaRelacionadaId = cobranca.getCobrancaRelacionada().getId();
            }

            this.gatewayPaymentId = cobranca.getGatewayPaymentId();
            this.gatewayPaymentIntentId = cobranca.getGatewayPaymentIntentId();

            // Informações calculadas
            this.isVencida = cobranca.isVencida();
            this.diasAtraso = cobranca.getDiasAtraso();
            this.descricaoTransacao = cobranca.getDescricaoTransacao();

            // Informações do cliente
            if (cobranca.getCliente() != null) {
                this.clienteId = cobranca.getCliente().getId();
                this.nomeCliente = cobranca.getCliente().getNomeCompleto();
            }

            // Informações de relacionamento
            if (cobranca.getAgendamento() != null) {
                this.agendamentoId = cobranca.getAgendamento().getId();
            }

            if (cobranca.getCompra() != null) {
                this.compraId = cobranca.getCompra().getId();
            }
        }
    }
}
