package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.compra.Compra;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.Transacao.CompraRepository;
import org.exemplo.bellory.model.repository.Transacao.PagamentoRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço responsável por gerenciar o ciclo completo de transações:
 * Agendamento/Compra → Cobrança → Pagamento
 */
@Service
public class TransacaoService {

    private final CobrancaRepository cobrancaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final CompraRepository compraRepository;
    private final AgendamentoRepository agendamentoRepository;


    public TransacaoService(CobrancaRepository cobrancaRepository,
                              PagamentoRepository pagamentoRepository,
                              CompraRepository compraRepository,
                              AgendamentoRepository agendamentoRepository) {
        this.cobrancaRepository = cobrancaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.compraRepository = compraRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    /**
     * Cria uma cobrança automaticamente quando um agendamento é criado
     */
    @Transactional
    public Cobranca criarCobrancaParaAgendamento(Agendamento agendamento) {
        // Calcular valor total dos serviços
        BigDecimal valorTotal = agendamento.getServicos().stream()
                .map(Servico::getPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Cobranca cobranca = new Cobranca();
        cobranca.setOrganizacao(agendamento.getOrganizacao());
        cobranca.setCliente(agendamento.getCliente());
        cobranca.setAgendamento(agendamento);
        cobranca.setValor(valorTotal);
        cobranca.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
        cobranca.setTipoCobranca(Cobranca.TipoCobranca.AGENDAMENTO);
        cobranca.setDtVencimento(agendamento.getDtAgendamento().toLocalDate());
        cobranca.setPermiteParcelamento(true);

        return cobrancaRepository.save(cobranca);
    }

    /**
     * Cria uma cobrança automaticamente quando uma compra é finalizada
     */
    @Transactional
    public Cobranca criarCobrancaParaCompra(Compra compra) {
        Cobranca cobranca = new Cobranca();
        cobranca.setOrganizacao(compra.getOrganizacao());
        cobranca.setCliente(compra.getCliente());
        cobranca.setCompra(compra);
        cobranca.setValor(compra.getValorFinal());
        cobranca.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
        cobranca.setTipoCobranca(Cobranca.TipoCobranca.COMPRA);
        cobranca.setDtVencimento(LocalDateTime.now().toLocalDate()); // Vence hoje para compras
        cobranca.setPermiteParcelamento(compra.getValorFinal().compareTo(new BigDecimal("100")) >= 0);

        return cobrancaRepository.save(cobranca);
    }

    /**
     * Processa um pagamento para uma cobrança específica
     */
    @Transactional
    public Pagamento processarPagamento(Long cobrancaId,
                                        Pagamento.MetodoPagamento metodoPagamento,
                                        BigDecimal valorPagamento,
                                        Long cartaoCreditoId) {

        Cobranca cobranca = cobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        if (!cobranca.permiteNovoPagamento()) {
            throw new IllegalStateException("Esta cobrança não permite novos pagamentos");
        }

        if (valorPagamento.compareTo(cobranca.getValorRestante()) > 0) {
            throw new IllegalArgumentException("Valor do pagamento não pode ser maior que o valor restante");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setCobranca(cobranca);
        pagamento.setCliente(cobranca.getCliente());
        pagamento.setValor(valorPagamento);
        pagamento.setMetodoPagamento(metodoPagamento);
        pagamento.setStatusPagamento(Pagamento.StatusPagamento.PENDENTE);

        // Se for cartão de crédito, associar o cartão
        if (metodoPagamento == Pagamento.MetodoPagamento.CARTAO_CREDITO && cartaoCreditoId != null) {
            // Buscar cartão de crédito do cliente - implementar busca no repository
            // pagamento.setCartaoCredito(cartaoCredito);
        }

        Pagamento pagamentoSalvo = pagamentoRepository.save(pagamento);

        // Simular processamento do pagamento
        processarPagamentoAsync(pagamentoSalvo);

        return pagamentoSalvo;
    }

    /**
     * Confirma um pagamento e atualiza todos os status relacionados
     */
    @Transactional
    public void confirmarPagamento(Long pagamentoId) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));

        // Confirmar o pagamento
        pagamento.confirmarPagamento();
        pagamentoRepository.save(pagamento);

        // Atualizar a cobrança
        Cobranca cobranca = pagamento.getCobranca();
        cobranca.recalcularValores();
        cobrancaRepository.save(cobranca);

        // Atualizar status da transação original
        atualizarStatusTransacaoOriginal(cobranca);
    }

    /**
     * Cancela um pagamento
     */
    @Transactional
    public void cancelarPagamento(Long pagamentoId, String motivo) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));

        pagamento.cancelarPagamento();
        pagamento.setObservacoes("Cancelado: " + motivo);
        pagamentoRepository.save(pagamento);

        // Atualizar a cobrança
        Cobranca cobranca = pagamento.getCobranca();
        cobranca.recalcularValores();
        cobrancaRepository.save(cobranca);
    }

    /**
     * Estorna uma cobrança completa
     */
    @Transactional
    public void estornarCobranca(Long cobrancaId, String motivo) {
        Cobranca cobranca = cobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        if (!cobranca.isPaga()) {
            throw new IllegalStateException("Só é possível estornar cobranças pagas");
        }

        // Estornar todos os pagamentos confirmados
        cobranca.getPagamentos().forEach(pagamento -> {
            if (pagamento.isPago()) {
                pagamento.setStatusPagamento(Pagamento.StatusPagamento.ESTORNADO);
                pagamento.setObservacoes("Estornado: " + motivo);
                pagamentoRepository.save(pagamento);
            }
        });

        // Estornar a cobrança
        cobranca.estornar();
        cobrancaRepository.save(cobranca);

        // Atualizar status da transação original
        atualizarStatusTransacaoOriginal(cobranca);
    }

    /**
     * Busca todas as cobranças pendentes de um cliente
     */
    public List<Cobranca> getCobrancasPendentesCliente(Long clienteId) {
        return cobrancaRepository.findByClienteIdAndStatusCobrancaIn(
                clienteId,
                List.of(Cobranca.StatusCobranca.PENDENTE,
                        Cobranca.StatusCobranca.PARCIALMENTE_PAGO,
                        Cobranca.StatusCobranca.VENCIDA)
        );
    }

    /**
     * Busca histórico de pagamentos de um cliente
     */
    public List<Pagamento> getHistoricoPagamentosCliente(Long clienteId) {
        return pagamentoRepository.findByClienteIdOrderByDtCriacaoDesc(clienteId);
    }

    /**
     * Aplica juros e multa em cobranças vencidas
     */
    @Transactional
    public void aplicarJurosMultaVencidas() {
        List<Cobranca> cobrancasVencidas = cobrancaRepository.findVencidas(Cobranca.StatusCobranca.PENDENTE, Cobranca.StatusCobranca.PARCIALMENTE_PAGO);

        cobrancasVencidas.forEach(cobranca -> {
            cobranca.aplicarJurosEMulta();
            cobrancaRepository.save(cobranca);
        });
    }

    // === MÉTODOS PRIVADOS ===

    /**
     * Simula o processamento assíncrono do pagamento
     */
    private void processarPagamentoAsync(Pagamento pagamento) {
        // Em uma implementação real, aqui seria feita a integração com gateway de pagamento
        // Por enquanto, simular aprovação automática para dinheiro e PIX
        if (pagamento.getMetodoPagamento() == Pagamento.MetodoPagamento.DINHEIRO ||
                pagamento.getMetodoPagamento() == Pagamento.MetodoPagamento.PIX) {

            // Simular delay de processamento
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // 2 segundos
                    confirmarPagamento(pagamento.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * Atualiza o status da transação original (Agendamento ou Compra) baseado no status da cobrança
     */
    private void atualizarStatusTransacaoOriginal(Cobranca cobranca) {
        if (cobranca.getAgendamento() != null) {
            Agendamento agendamento = cobranca.getAgendamento();

            if (cobranca.isPaga()) {
                // Não alterar status do agendamento automaticamente
                // O agendamento pode estar pago mas ainda não concluído
            } else if (cobranca.isCancelada()) {
                // agendamento.setStatus(Status.CANCELADO);
                // agendamentoRepository.save(agendamento);
            }

        } else if (cobranca.getCompra() != null) {
            Compra compra = cobranca.getCompra();

            if (cobranca.isPaga()) {
                compra.marcarComoPaga();
                compraRepository.save(compra);
            } else if (cobranca.isCancelada()) {
                compra.cancelarCompra();
                compraRepository.save(compra);
            }
        }
    }
}