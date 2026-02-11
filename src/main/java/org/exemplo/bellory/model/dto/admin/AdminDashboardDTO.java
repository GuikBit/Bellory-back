package org.exemplo.bellory.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDTO {
    private Long totalOrganizacoes;
    private Long organizacoesAtivas;
    private Long organizacoesInativas;
    private Long totalAgendamentos;
    private Long totalClientes;
    private Long totalFuncionarios;
    private Long totalServicos;
    private Long totalInstancias;
    private Long instanciasConectadas;
    private Long instanciasDesconectadas;
    private BigDecimal faturamentoTotal;
    private Long totalCobrancas;
    private Long cobrancasPendentes;
    private Long cobrancasPagas;
    private DistribuicaoPlanos distribuicaoPlanos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistribuicaoPlanos {
        private Long gratuito;
        private Long basico;
        private Long plus;
        private Long premium;
    }
}
