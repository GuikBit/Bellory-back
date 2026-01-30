package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    // Percentual de sinal padrão (pode ser configurável por organização)
    private static final BigDecimal PERCENTUAL_SINAL_PADRAO = new BigDecimal("30.00");

    public TransacaoService(CobrancaRepository cobrancaRepository,
                            PagamentoRepository pagamentoRepository,
                            CompraRepository compraRepository,
                            AgendamentoRepository agendamentoRepository) {
        this.cobrancaRepository = cobrancaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.compraRepository = compraRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    // ========================================================================
    // MÉTODOS PARA CRIAR COBRANÇAS DE AGENDAMENTO (COM SINAL)
    // ========================================================================

    /**
     * Cria cobranças para agendamento (sinal + restante ou integral)
     * @param agendamento Agendamento para o qual criar as cobranças
     * @param requerSinal Se requer pagamento de sinal
     * @param percentualSinal Percentual do sinal (ex: 30 para 30%)
     * @param dtVencimentoSinal Data de vencimento do sinal
     * @param dtVencimentoRestante Data de vencimento do restante
     * @return Lista de cobranças criadas
     */
    @Transactional
    public List<Cobranca> criarCobrancasParaAgendamento(
            Agendamento agendamento,
            Boolean requerSinal,
            BigDecimal percentualSinal,
            LocalDate dtVencimentoSinal,
            LocalDate dtVencimentoRestante) {

        List<Cobranca> cobrancas = new ArrayList<>();

        // Calcular valor total dos serviços
        BigDecimal valorTotal = agendamento.getServicos().stream()
                .map(Servico::getPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Validar valor total
        if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor total dos serviços deve ser maior que zero.");
        }

        // Definir percentual de sinal
        BigDecimal percentual = percentualSinal != null ? percentualSinal : PERCENTUAL_SINAL_PADRAO;

        // Atualizar agendamento
        agendamento.setRequerSinal(requerSinal != null ? requerSinal : true);
        agendamento.setPercentualSinal(percentual);

        // Caso 1: Agendamento SEM sinal (pagamento integral)
        if (!agendamento.getRequerSinal()) {
            Cobranca cobrancaIntegral = criarCobrancaIntegral(
                    agendamento,
                    valorTotal,
                    dtVencimentoRestante != null ? dtVencimentoRestante : agendamento.getDtAgendamento().toLocalDate()
            );
            cobrancas.add(cobrancaIntegral);
        }
        // Caso 2: Agendamento COM sinal
        else {
            // Validar percentual
            if (percentual.compareTo(BigDecimal.ZERO) <= 0 || percentual.compareTo(new BigDecimal("100")) >= 100) {
                throw new IllegalArgumentException("O percentual do sinal deve estar entre 0 e 100.");
            }

            // Calcular valores
            BigDecimal valorSinal = valorTotal
                    .multiply(percentual)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal valorRestante = valorTotal.subtract(valorSinal);

            // Criar cobrança do sinal
            Cobranca cobrancaSinal = criarCobrancaSinal(
                    agendamento,
                    valorSinal,
                    percentual,
                    dtVencimentoSinal != null ? dtVencimentoSinal : LocalDate.now().plusDays(2)
            );
            cobrancas.add(cobrancaSinal);

            // Criar cobrança do restante
            Cobranca cobrancaRestante = criarCobrancaRestante(
                    agendamento,
                    valorRestante,
                    dtVencimentoRestante != null ? dtVencimentoRestante : agendamento.getDtAgendamento().toLocalDate()
            );
            cobrancas.add(cobrancaRestante);

            // Estabelecer relacionamento entre as cobranças
            cobrancaSinal.setCobrancaRelacionada(cobrancaRestante);
            cobrancaRestante.setCobrancaRelacionada(cobrancaSinal);

            cobrancaRepository.save(cobrancaSinal);
            cobrancaRepository.save(cobrancaRestante);
        }

        return cobrancas;
    }

    /**
     * MÉTODO LEGADO: Mantido para compatibilidade com código antigo
     * Cria uma cobrança integral automaticamente quando um agendamento é criado
     */
    @Transactional
    public Cobranca criarCobrancaParaAgendamento(Agendamento agendamento) {
        // Usar o novo método com pagamento integral (sem sinal)
        List<Cobranca> cobrancas = criarCobrancasParaAgendamento(
                agendamento,
                false, // Sem sinal
                null,
                null,
                agendamento.getDtAgendamento().toLocalDate()
        );
        return cobrancas.get(0);
    }

    /**
     * Cria cobrança de sinal
     */
    private Cobranca criarCobrancaSinal(Agendamento agendamento, BigDecimal valor,
                                        BigDecimal percentual, LocalDate dtVencimento) {
        Cobranca cobranca = new Cobranca();
        cobranca.setOrganizacao(agendamento.getOrganizacao());
        cobranca.setCliente(agendamento.getCliente());
        cobranca.setAgendamento(agendamento);
        cobranca.setValor(valor);
        cobranca.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
        cobranca.setTipoCobranca(Cobranca.TipoCobranca.AGENDAMENTO);
        cobranca.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.SINAL);
        cobranca.setDtVencimento(dtVencimento);
        cobranca.setPercentualSinal(percentual);
        cobranca.setPermiteParcelamento(false); // Sinal normalmente não permite parcelamento
        cobranca.setObservacoes("Sinal para confirmação do agendamento (" + percentual + "%)");

        return cobrancaRepository.save(cobranca);
    }

    @Transactional
    public void criarSinal(Long agendamentoId, Long cobrancaId, BigDecimal porcentagem) {
        // Buscar entidades
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        Cobranca cobrancaOriginal = cobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada"));

        // Validações
//        if (cobrancaOriginal.getSubtipoCobrancaAgendamento() != null) {
//            throw new IllegalArgumentException("Esta cobrança já foi dividida em sinal/restante");
//        }

        if (porcentagem.compareTo(BigDecimal.ZERO) <= 0 || porcentagem.compareTo(new BigDecimal("100")) >= 100) {
            throw new IllegalArgumentException("O percentual do sinal deve estar entre 0 e 100");
        }

        // Calcular valores
        BigDecimal valorSinal = cobrancaOriginal.getValor()
                .multiply(porcentagem)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal valorRestante = cobrancaOriginal.getValor().subtract(valorSinal);

        // Criar cobrança do sinal
        Cobranca cobrancaSinal = new Cobranca();
        cobrancaSinal.setOrganizacao(cobrancaOriginal.getOrganizacao());
        cobrancaSinal.setCliente(cobrancaOriginal.getCliente());
        cobrancaSinal.setAgendamento(agendamento);
        cobrancaSinal.setValor(valorSinal);
        cobrancaSinal.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
        cobrancaSinal.setTipoCobranca(Cobranca.TipoCobranca.AGENDAMENTO);
        cobrancaSinal.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.SINAL);
        cobrancaSinal.setDtVencimento(cobrancaOriginal.getDtVencimento());
        cobrancaSinal.setPercentualSinal(porcentagem);
        cobrancaSinal.setPermiteParcelamento(false);
        cobrancaSinal.setObservacoes("Sinal para confirmação do agendamento (" + porcentagem + "%)");

        // Criar cobrança do restante
//        Cobranca cobrancaRestante = new Cobranca();
//        cobrancaRestante.setOrganizacao(cobrancaOriginal.getOrganizacao());
//        cobrancaRestante.setCliente(cobrancaOriginal.getCliente());
//        cobrancaRestante.setAgendamento(agendamento);
//        cobrancaRestante.setValor(valorRestante);
//        cobrancaRestante.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
//        cobrancaRestante.setTipoCobranca(Cobranca.TipoCobranca.AGENDAMENTO);
//        cobrancaRestante.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.RESTANTE);
//        cobrancaRestante.setDtVencimento(agendamento.getDtAgendamento().toLocalDate());
//        cobrancaRestante.setPermiteParcelamento(true);
//        cobrancaRestante.setObservacoes("Pagamento final do agendamento");
        cobrancaOriginal.setValor(valorRestante);
        cobrancaOriginal.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.RESTANTE);
        cobrancaOriginal.setPermiteParcelamento(false);

        // Estabelecer relacionamento entre as cobranças
        cobrancaSinal.setCobrancaRelacionada(cobrancaOriginal);
        cobrancaOriginal.setCobrancaRelacionada(cobrancaSinal);

        // Salvar novas cobranças
        cobrancaRepository.save(cobrancaSinal);
        cobrancaRepository.save(cobrancaOriginal);

        // Inativar/remover cobrança original
        // cobrancaRepository.delete(cobrancaOriginal);
        // OU se preferir manter histórico:
        // cobrancaOriginal.setStatusCobranca(Cobranca.StatusCobranca.CANCELADA);
        // cobrancaRepository.save(cobrancaOriginal);

        // Atualizar agendamento
        agendamento.setRequerSinal(false);
        agendamento.setPercentualSinal(porcentagem);
        agendamentoRepository.save(agendamento);

//        return Arrays.asList(cobrancaSinal, cobrancaOriginal);
    }
    /**
     * Cria cobrança do valor restante
     */
    private Cobranca criarCobrancaRestante(Agendamento agendamento, BigDecimal valor,
                                           LocalDate dtVencimento) {
        Cobranca cobranca = new Cobranca();
        cobranca.setOrganizacao(agendamento.getOrganizacao());
        cobranca.setCliente(agendamento.getCliente());
        cobranca.setAgendamento(agendamento);
        cobranca.setValor(valor);
        cobranca.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
        cobranca.setTipoCobranca(Cobranca.TipoCobranca.AGENDAMENTO);
        cobranca.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.RESTANTE);
        cobranca.setDtVencimento(dtVencimento);
        cobranca.setPermiteParcelamento(true); // Restante pode permitir parcelamento
        cobranca.setObservacoes("Pagamento final do agendamento");

        return cobrancaRepository.save(cobranca);
    }

    /**
     * Cria cobrança integral (sem sinal)
     */
    private Cobranca criarCobrancaIntegral(Agendamento agendamento, BigDecimal valor,
                                           LocalDate dtVencimento) {
        Cobranca cobranca = new Cobranca();
        cobranca.setOrganizacao(agendamento.getOrganizacao());
        cobranca.setCliente(agendamento.getCliente());
        cobranca.setAgendamento(agendamento);
        cobranca.setValor(valor);
        cobranca.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
        cobranca.setTipoCobranca(Cobranca.TipoCobranca.AGENDAMENTO);
        cobranca.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.INTEGRAL);
        cobranca.setDtVencimento(dtVencimento);
        cobranca.setPermiteParcelamento(true);
        cobranca.setObservacoes("Pagamento integral do agendamento");

        return cobrancaRepository.save(cobranca);
    }

    // ========================================================================
    // MÉTODOS PARA CRIAR COBRANÇAS DE COMPRA
    // ========================================================================

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
        cobranca.setDtVencimento(LocalDateTime.now().toLocalDate());
        cobranca.setPermiteParcelamento(compra.getValorFinal().compareTo(new BigDecimal("100")) >= 0);

        return cobrancaRepository.save(cobranca);
    }

    // ========================================================================
    // MÉTODOS PARA PROCESSAR PAGAMENTOS
    // ========================================================================

    /**
     * Processa pagamento de uma cobrança
     * @param cobrancaId ID da cobrança
     * @param valorPagamento Valor do pagamento
     * @param formaPagamento Forma de pagamento
     * @return Pagamento criado
     */
    @Transactional
    public Pagamento processarPagamento(Long cobrancaId, BigDecimal valorPagamento, Pagamento.FormaPagamento formaPagamento) {

        Cobranca cobranca = cobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada."));

        // Validações
        if (!cobranca.permiteNovoPagamento()) {
            throw new IllegalStateException("Esta cobrança não permite novos pagamentos.");
        }

        if (valorPagamento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero.");
        }

        BigDecimal valorRestante = cobranca.getValorRestante();
        if (valorPagamento.compareTo(valorRestante) > 0) {
            throw new IllegalArgumentException(
                    "Valor do pagamento (" + valorPagamento + ") é maior que o valor restante (" + valorRestante + ")."
            );
        }

        // Criar pagamento
        Pagamento pagamento = new Pagamento();
        pagamento.setCobranca(cobranca);
        pagamento.setCliente(cobranca.getCliente());
        pagamento.setOrganizacao(cobranca.getOrganizacao());
        pagamento.setValor(valorPagamento);
        pagamento.setFormaPagamento(formaPagamento);
        pagamento.setStatusPagamento(Pagamento.StatusPagamento.CONFIRMADO);
        pagamento.setDtPagamento(LocalDateTime.now());

        Pagamento pagamentoSalvo = pagamentoRepository.save(pagamento);

        // Adicionar pagamento à cobrança
        cobranca.adicionarPagamento(pagamentoSalvo);
        cobrancaRepository.save(cobranca);

        // Se for sinal e foi totalmente pago, confirmar agendamento
        if (cobranca.isSinal() && cobranca.isPaga()) {
            confirmarAgendamentoPorPagamentoSinal(cobranca.getAgendamento());
        }

        atualizarStatusPagamentoAgendamento(cobranca.getAgendamento().getId());

        return pagamentoSalvo;
    }

    private void atualizarStatusPagamentoAgendamento(long agendamentoId){
        Optional<Agendamento> agendamento = agendamentoRepository.findById(agendamentoId);



    }

    /**
     * MÉTODO LEGADO: Mantido para compatibilidade
     */
//    @Transactional
//    public Pagamento processarPagamento(Long cobrancaId,
//                                        Pagamento.MetodoPagamento metodoPagamento,
//                                        BigDecimal valorPagamento,
//                                        Long cartaoCreditoId) {
//
//        // Converter MetodoPagamento para FormaPagamento
//        Pagamento.FormaPagamento formaPagamento = converterMetodoParaForma(metodoPagamento);
//
//        return processarPagamento(cobrancaId, valorPagamento, formaPagamento);
//    }

    /**
     * Confirma agendamento quando o sinal é pago
     */
    @Transactional
    public void confirmarAgendamentoPorPagamentoSinal(Agendamento agendamento) {
        if (agendamento.getStatus() == Status.PENDENTE) {
            agendamento.confirmarAgendamento();
            agendamentoRepository.save(agendamento);
        }
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

        // Se for sinal, confirmar agendamento
        if (cobranca.isSinal() && cobranca.isPaga()) {
            confirmarAgendamentoPorPagamentoSinal(cobranca.getAgendamento());
        }

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
     * Cancela cobrança e estorna pagamentos se necessário
     */
    @Transactional
    public void cancelarCobranca(Long cobrancaId, String motivo) {
        Cobranca cobranca = cobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada."));

        if (cobranca.isPaga()) {
            throw new IllegalStateException("Não é possível cancelar uma cobrança já paga. Use estorno.");
        }

        cobranca.cancelar();
        cobranca.setObservacoes(cobranca.getObservacoes() + "\nCANCELADO: " + motivo);
        cobrancaRepository.save(cobranca);

        // Se cancelar sinal, cancelar também o restante
        if (cobranca.isSinal() && cobranca.getCobrancaRelacionada() != null) {
            Cobranca cobrancaRestante = cobranca.getCobrancaRelacionada();
            cobrancaRestante.cancelar();
            cobrancaRestante.setObservacoes(
                    cobrancaRestante.getObservacoes() + "\nCANCELADO: Sinal cancelado"
            );
            cobrancaRepository.save(cobrancaRestante);
        }
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

    // ========================================================================
    // MÉTODOS DE CONSULTA
    // ========================================================================

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
        List<Cobranca> cobrancasVencidas = cobrancaRepository.findVencidas(
                Cobranca.StatusCobranca.PENDENTE,
                Cobranca.StatusCobranca.PARCIALMENTE_PAGO
        );

        cobrancasVencidas.forEach(cobranca -> {
            cobranca.aplicarJurosEMulta();
            cobrancaRepository.save(cobranca);
        });
    }

    // === MÉTODOS PRIVADOS ===

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
                // Se necessário, cancelar agendamento
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

    // Método auxiliar para converter MetodoPagamento para FormaPagamento
    private Pagamento.FormaPagamento converterMetodoParaForma(Pagamento.MetodoPagamento metodo) {
        return switch (metodo) {
            case CARTAO_CREDITO -> Pagamento.FormaPagamento.CARTAO_CREDITO;
            case CARTAO_DEBITO -> Pagamento.FormaPagamento.CARTAO_DEBITO;
            case DINHEIRO -> Pagamento.FormaPagamento.DINHEIRO;
            case PIX -> Pagamento.FormaPagamento.PIX;
            case TRANSFERENCIA_BANCARIA -> Pagamento.FormaPagamento.TRANSFERENCIA;
            case BOLETO -> Pagamento.FormaPagamento.BOLETO;
        };
    }
}
