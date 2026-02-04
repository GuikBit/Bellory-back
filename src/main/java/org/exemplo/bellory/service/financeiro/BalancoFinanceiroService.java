package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.BalancoFinanceiroDTO;
import org.exemplo.bellory.model.entity.financeiro.ContaBancaria;
import org.exemplo.bellory.model.repository.financeiro.ContaBancariaRepository;
import org.exemplo.bellory.model.repository.financeiro.ContaPagarRepository;
import org.exemplo.bellory.model.repository.financeiro.ContaReceberRepository;
import org.exemplo.bellory.model.repository.financeiro.LancamentoFinanceiroRepository;
import org.exemplo.bellory.model.entity.financeiro.ContaPagar;
import org.exemplo.bellory.model.entity.financeiro.ContaReceber;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalancoFinanceiroService {

    private final ContaBancariaRepository contaBancariaRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final LancamentoFinanceiroRepository lancamentoRepository;

    public BalancoFinanceiroDTO gerarBalanco(LocalDate dataReferencia) {
        Long orgId = getOrganizacaoId();
        LocalDate hoje = dataReferencia != null ? dataReferencia : LocalDate.now();

        // Saldos das contas bancárias
        List<ContaBancaria> contasBancarias = contaBancariaRepository.findByOrganizacaoIdAndAtivoTrue(orgId);
        BigDecimal totalAtivos = contaBancariaRepository.sumSaldoAtualByOrganizacao(orgId);

        List<BalancoFinanceiroDTO.SaldoContaBancariaDTO> saldosContas = contasBancarias.stream()
                .map(c -> BalancoFinanceiroDTO.SaldoContaBancariaDTO.builder()
                        .contaId(c.getId())
                        .contaNome(c.getNome())
                        .tipoConta(c.getTipoConta() != null ? c.getTipoConta().getDescricao() : null)
                        .banco(c.getBanco())
                        .saldoAtual(c.getSaldoAtual())
                        .build())
                .collect(Collectors.toList());

        // Contas a Receber
        BigDecimal crPendentes = contaReceberRepository.sumValorByOrganizacaoAndStatus(orgId, ContaReceber.StatusContaReceber.PENDENTE);
        BigDecimal crParciais = contaReceberRepository.sumValorByOrganizacaoAndStatus(orgId, ContaReceber.StatusContaReceber.PARCIALMENTE_RECEBIDA);
        BigDecimal contasReceberPendentes = crPendentes.add(crParciais);
        BigDecimal contasReceberVencidas = contaReceberRepository.sumValorVencidasByOrganizacao(orgId, hoje);
        BigDecimal contasReceberRecebidas = contaReceberRepository.sumValorByOrganizacaoAndStatus(orgId, ContaReceber.StatusContaReceber.RECEBIDA);
        int qtdCRPendentes = contaReceberRepository.findByOrganizacaoIdAndStatus(orgId, ContaReceber.StatusContaReceber.PENDENTE).size();
        int qtdCRVencidas = contaReceberRepository.countVencidasByOrganizacao(orgId, hoje);

        // Contas a Pagar
        BigDecimal cpPendentes = contaPagarRepository.sumValorByOrganizacaoAndStatus(orgId, ContaPagar.StatusContaPagar.PENDENTE);
        BigDecimal cpParciais = contaPagarRepository.sumValorByOrganizacaoAndStatus(orgId, ContaPagar.StatusContaPagar.PARCIALMENTE_PAGA);
        BigDecimal contasPagarPendentes = cpPendentes.add(cpParciais);
        BigDecimal contasPagarVencidas = contaPagarRepository.sumValorVencidasByOrganizacao(orgId, hoje);
        BigDecimal contasPagarPagas = contaPagarRepository.sumValorByOrganizacaoAndStatus(orgId, ContaPagar.StatusContaPagar.PAGA);
        int qtdCPPendentes = contaPagarRepository.findByOrganizacaoIdAndStatus(orgId, ContaPagar.StatusContaPagar.PENDENTE).size();
        int qtdCPVencidas = contaPagarRepository.countVencidasByOrganizacao(orgId, hoje);

        // Totais
        BigDecimal totalContasReceber = contasReceberPendentes;
        BigDecimal totalContasPagar = contasPagarPendentes;
        BigDecimal saldoLiquido = totalAtivos.add(totalContasReceber).subtract(totalContasPagar);

        // Indicadores
        BigDecimal indiceLiquidez = totalContasPagar.compareTo(BigDecimal.ZERO) > 0
                ? totalAtivos.add(totalContasReceber).divide(totalContasPagar, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Evolução mensal (últimos 6 meses)
        List<BalancoFinanceiroDTO.BalancoMensalDTO> evolucao = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth mesAno = YearMonth.from(hoje.minusMonths(i));
            LocalDate inicioMes = mesAno.atDay(1);
            LocalDate fimMes = mesAno.atEndOfMonth();

            BigDecimal receitas = lancamentoRepository.sumReceitasByPeriodo(orgId, inicioMes, fimMes);
            BigDecimal despesas = lancamentoRepository.sumDespesasByPeriodo(orgId, inicioMes, fimMes);

            evolucao.add(BalancoFinanceiroDTO.BalancoMensalDTO.builder()
                    .mesAno(mesAno.getMonth().name() + "/" + mesAno.getYear())
                    .receitas(receitas)
                    .despesas(despesas)
                    .resultado(receitas.subtract(despesas))
                    .build());
        }

        // Ticket médio e receita média diária
        BigDecimal receitasMesAtual = lancamentoRepository.sumReceitasByPeriodo(orgId,
                YearMonth.from(hoje).atDay(1), hoje);
        BigDecimal despesasMesAtual = lancamentoRepository.sumDespesasByPeriodo(orgId,
                YearMonth.from(hoje).atDay(1), hoje);

        int diasNoMes = hoje.getDayOfMonth();
        BigDecimal receitaMediaDiaria = diasNoMes > 0
                ? receitasMesAtual.divide(BigDecimal.valueOf(diasNoMes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long qtdReceitasMes = lancamentoRepository.countByTipoAndPeriodo(orgId,
                org.exemplo.bellory.model.entity.financeiro.LancamentoFinanceiro.TipoLancamento.RECEITA,
                YearMonth.from(hoje).atDay(1), hoje);
        long qtdDespesasMes = lancamentoRepository.countByTipoAndPeriodo(orgId,
                org.exemplo.bellory.model.entity.financeiro.LancamentoFinanceiro.TipoLancamento.DESPESA,
                YearMonth.from(hoje).atDay(1), hoje);

        BigDecimal ticketMedioReceitas = qtdReceitasMes > 0
                ? receitasMesAtual.divide(BigDecimal.valueOf(qtdReceitasMes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal ticketMedioDespesas = qtdDespesasMes > 0
                ? despesasMesAtual.divide(BigDecimal.valueOf(qtdDespesasMes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return BalancoFinanceiroDTO.builder()
                .dataReferencia(hoje)
                .totalAtivos(totalAtivos)
                .totalContasReceber(totalContasReceber)
                .totalContasPagar(totalContasPagar)
                .saldoLiquido(saldoLiquido)
                .saldosContas(saldosContas)
                .contasReceberPendentes(contasReceberPendentes)
                .contasReceberVencidas(contasReceberVencidas)
                .contasReceberRecebidas(contasReceberRecebidas)
                .qtdContasReceberPendentes(qtdCRPendentes)
                .qtdContasReceberVencidas(qtdCRVencidas)
                .contasPagarPendentes(contasPagarPendentes)
                .contasPagarVencidas(contasPagarVencidas)
                .contasPagarPagas(contasPagarPagas)
                .qtdContasPagarPendentes(qtdCPPendentes)
                .qtdContasPagarVencidas(qtdCPVencidas)
                .indiceLiquidez(indiceLiquidez)
                .ticketMedioReceitas(ticketMedioReceitas)
                .ticketMedioDespesas(ticketMedioDespesas)
                .receitaMediaDiaria(receitaMediaDiaria)
                .evolucaoMensal(evolucao)
                .build();
    }

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new IllegalStateException("Contexto de organização não encontrado.");
        }
        return orgId;
    }
}
