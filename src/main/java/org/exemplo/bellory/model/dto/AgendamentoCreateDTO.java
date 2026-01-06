package org.exemplo.bellory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.dto.clienteDTO.AgentamentoClienteDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgendamentoCreateDTO {
    private Long organizacaoId;
    private Long clienteId;
    private List<Long> servicoIds;
    private List<Long> funcionarioIds;
    private LocalDateTime dtAgendamento;
    private String observacao;

    // NOVO: Indica se o agendamento requer pagamento de sinal
    private Boolean requerSinal = true;

    // NOVO: Percentual do sinal (se não informado, usa padrão da organização)
    // Ex: 30.00 para 30%
    private BigDecimal percentualSinal;

    // NOVO: Data de vencimento do sinal (se não informado, usa data do agendamento)
    private LocalDate dtVencimentoSinal;

    // NOVO: Data de vencimento do pagamento final
    private LocalDate dtVencimentoRestante;
}
