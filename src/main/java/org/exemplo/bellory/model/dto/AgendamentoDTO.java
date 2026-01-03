package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.servico.Servico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgendamentoDTO {
    private Long id;
    private Long organizacaoId;
    private ClienteResumoDTO cliente;
    private Long clienteId;
    private List<ServicoResumoDTO> servicos;
    private List<FuncionarioResumoDTO> funcionarios;
    private LocalDateTime dtAgendamento;
    private List<CobrancaDTO> cobrancas; // Plural para deixar claro que são múltiplas
    private String observacao;
    private Status status;
    private BigDecimal valorTotal;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
    private LocalDateTime dtConfirmacao;

    // Novos campos relacionados ao sinal
    private Boolean requerSinal;
    private BigDecimal percentualSinal;
    private Boolean isSinalPago;
    private Boolean isPagamentoCompleto;
    private Boolean isConfirmado;

    // Informações resumidas de cobrança
    private CobrancaResumoDTO resumoCobranca;

    public AgendamentoDTO(Agendamento agendamento) {
        this.id = agendamento.getId();
        this.organizacaoId = agendamento.getOrganizacao().getId();
        this.clienteId = agendamento.getCliente().getId();

        // Cliente resumo
        this.cliente = new ClienteResumoDTO(
                agendamento.getCliente().getId(),
                agendamento.getCliente().getNomeCompleto(),
                agendamento.getCliente().getEmail(),
                agendamento.getCliente().getTelefone()
        );

        // Cobranças completas
        this.cobrancas = agendamento.getCobrancas().stream()
                .map(CobrancaDTO::new)
                .collect(Collectors.toList());

        // Serviços resumo
        this.servicos = agendamento.getServicos().stream()
                .map(servico -> new ServicoResumoDTO(
                        servico.getId(),
                        servico.getNome(),
                        servico.getPreco(),
                        servico.getTempoEstimadoMinutos()
                ))
                .collect(Collectors.toList());

        // Funcionários resumo
        this.funcionarios = agendamento.getFuncionarios().stream()
                .map(funcionario -> new FuncionarioResumoDTO(
                        funcionario.getId(),
                        funcionario.getNomeCompleto(),
                        funcionario.getEmail(),
                        funcionario.getCargo().getNome()
                ))
                .collect(Collectors.toList());

        this.dtAgendamento = agendamento.getDtAgendamento();
        this.observacao = agendamento.getObservacao();
        this.status = agendamento.getStatus();
        this.valorTotal = agendamento.calcularValorTotal();
        this.dtCriacao = agendamento.getDtCriacao();
        this.dtAtualizacao = agendamento.getDtAtualizacao();
        this.dtConfirmacao = agendamento.getDtConfirmacao();

        // Informações do sinal
        this.requerSinal = agendamento.getRequerSinal();
        this.percentualSinal = agendamento.getPercentualSinal();
        this.isSinalPago = agendamento.isSinalPago();
        this.isPagamentoCompleto = agendamento.isPagamentoCompleto();
        this.isConfirmado = agendamento.isConfirmado();

        // Resumo consolidado das cobranças
        this.resumoCobranca = construirResumoCobranca(agendamento);
    }

    private CobrancaResumoDTO construirResumoCobranca(Agendamento agendamento) {
        if (agendamento.getCobrancas() == null || agendamento.getCobrancas().isEmpty()) {
            return null;
        }

        BigDecimal valorTotalCobrancas = BigDecimal.ZERO;
        BigDecimal valorTotalPago = BigDecimal.ZERO;
        BigDecimal valorTotalPendente = BigDecimal.ZERO;
        boolean temCobrancaVencida = false;

        CobrancaDTO cobrancaSinal = null;
        CobrancaDTO cobrancaRestante = null;
        CobrancaDTO cobrancaIntegral = null;

        for (Cobranca cobranca : agendamento.getCobrancas()) {
            valorTotalCobrancas = valorTotalCobrancas.add(cobranca.getValor());
            valorTotalPago = valorTotalPago.add(cobranca.getValorPago());
            valorTotalPendente = valorTotalPendente.add(cobranca.getValorPendente());

            if (cobranca.isVencida()) {
                temCobrancaVencida = true;
            }

            // Separar as cobranças por tipo
            if (cobranca.isSinal()) {
                cobrancaSinal = new CobrancaDTO(cobranca);
            } else if (cobranca.isRestante()) {
                cobrancaRestante = new CobrancaDTO(cobranca);
            } else if (cobranca.isIntegral()) {
                cobrancaIntegral = new CobrancaDTO(cobranca);
            }
        }

        return CobrancaResumoDTO.builder()
                .valorTotal(valorTotalCobrancas)
                .valorPago(valorTotalPago)
                .valorPendente(valorTotalPendente)
                .temCobrancaVencida(temCobrancaVencida)
                .cobrancaSinal(cobrancaSinal)
                .cobrancaRestante(cobrancaRestante)
                .cobrancaIntegral(cobrancaIntegral)
                .build();
    }
}
