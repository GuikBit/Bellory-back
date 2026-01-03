package org.exemplo.bellory.model.dto.compra;

import lombok.*;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class CobrancaDTO {
    private Long id;
    private Long clienteId;
    private String clienteNome;
    private Long agendamentoId;
    private BigDecimal valor;
    private BigDecimal valorPago;
    private BigDecimal valorPendente;
    private Cobranca.StatusCobranca statusCobranca;
    private Cobranca.TipoCobranca tipoCobranca;
    private Cobranca.SubtipoCobrancaAgendamento subtipoCobrancaAgendamento;
    private LocalDate dtVencimento;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtPagamentoCompleto;
    private String numeroCobranca;
    private String observacoes;
    private Boolean permiteParcelamento;
    private BigDecimal percentualSinal;
    private Long cobrancaRelacionadaId;
    private List<PagamentoDTO> pagamentos;
    private Integer diasAtraso;
    private String descricaoTransacao;

    public CobrancaDTO(Cobranca cobranca) {
        this.id = cobranca.getId();
        this.clienteId = cobranca.getCliente() != null ? cobranca.getCliente().getId() : null;
        this.clienteNome = cobranca.getCliente() != null ? cobranca.getCliente().getNomeCompleto() : null;
        this.agendamentoId = cobranca.getAgendamento() != null ? cobranca.getAgendamento().getId() : null;
        this.valor = cobranca.getValor();
        this.valorPago = cobranca.getValorPago();
        this.valorPendente = cobranca.getValorPendente();
        this.statusCobranca = cobranca.getStatusCobranca();
        this.tipoCobranca = cobranca.getTipoCobranca();
        this.subtipoCobrancaAgendamento = cobranca.getSubtipoCobrancaAgendamento();
        this.dtVencimento = cobranca.getDtVencimento();
        this.dtCriacao = cobranca.getDtCriacao();
        this.dtPagamentoCompleto = cobranca.getDtPagamentoCompleto();
        this.numeroCobranca = cobranca.getNumeroCobranca();
        this.observacoes = cobranca.getObservacoes();
        this.permiteParcelamento = cobranca.getPermiteParcelamento();
        this.percentualSinal = cobranca.getPercentualSinal();
        this.cobrancaRelacionadaId = cobranca.getCobrancaRelacionada() != null ?
                cobranca.getCobrancaRelacionada().getId() : null;
        this.diasAtraso = cobranca.getDiasAtraso();
        this.descricaoTransacao = cobranca.getDescricaoTransacao();

        if (cobranca.getPagamentos() != null && !cobranca.getPagamentos().isEmpty()) {
            this.pagamentos = cobranca.getPagamentos().stream()
                    .map(PagamentoDTO::new)
                    .collect(Collectors.toList());
        }
    }
}
