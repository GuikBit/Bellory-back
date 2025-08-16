package org.exemplo.bellory.model.dto.clienteDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstatisticasClientesDTO {
    // Contadores gerais
    private Long totalClientes;
    private Long clientesAtivos;
    private Long clientesInativos;
    private Long novosClientesEsseMes;
    private Long novosClientesEsteAno;

    // Aniversariantes
    private Long aniversariantesHoje;
    private Long aniversariantesEstaSemana;
    private Long aniversariantesEsteMes;

    // Estatísticas financeiras
    private BigDecimal receitaTotalClientes;
    private BigDecimal ticketMedio;
    private BigDecimal maiorTicket;
    private BigDecimal menorTicket;

    // Frequência
    private Long clientesRecorrentes;
    private Long clientesEsporadicos;
    private Double mediaAgendamentosPorCliente;

    // Distribuição por gênero (se disponível)
    private Long clientesMasculinos;
    private Long clientesFemininos;
    private Long clientesOutros;

    // Faixas etárias
    private Long clientesAte25;
    private Long clientes26a35;
    private Long clientes36a45;
    private Long clientes46a55;
    private Long clientesAcima55;
}
