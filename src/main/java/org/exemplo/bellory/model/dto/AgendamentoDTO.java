package org.exemplo.bellory.model.dto;

import lombok.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
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
    private CobrancaDTO cobranca;
    private String observacao;
    private Status status;
    private BigDecimal valorTotal;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
    private Long cobrancaId;
    private BigDecimal valorCobranca;
    private String statusCobranca;
    private LocalDate dtVencimentoCobranca;
    private BigDecimal valorPago;
    private BigDecimal valorPendente;
    private boolean cobrancaVencida;

    // Construtor que converte de Agendamento para AgendamentoDTO
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

        this.cobranca = new CobrancaDTO(agendamento.getCobranca());

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

        // Calcular valor total dos serviços
        this.valorTotal = agendamento.getServicos().stream()
                .map(Servico::getPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.dtCriacao = agendamento.getDtCriacao();
        this.dtAtualizacao = agendamento.getDtAtualizacao();
    }
}
