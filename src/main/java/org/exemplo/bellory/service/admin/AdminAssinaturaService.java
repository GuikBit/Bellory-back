package org.exemplo.bellory.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.exception.AssasApiException;
import org.exemplo.bellory.model.dto.assinatura.*;
import org.exemplo.bellory.model.entity.assinatura.*;
import org.exemplo.bellory.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.bellory.model.repository.assinatura.CobrancaPlataformaRepository;
import org.exemplo.bellory.model.repository.assinatura.PagamentoPlataformaRepository;
import org.exemplo.bellory.service.assinatura.AssasClient;
import org.exemplo.bellory.service.assinatura.AssinaturaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminAssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final CobrancaPlataformaRepository cobrancaPlataformaRepository;
    private final PagamentoPlataformaRepository pagamentoPlataformaRepository;
    private final AssinaturaService assinaturaService;
    private final AssasClient assasClient;

    public AdminAssinaturaService(AssinaturaRepository assinaturaRepository,
                                  CobrancaPlataformaRepository cobrancaPlataformaRepository,
                                  PagamentoPlataformaRepository pagamentoPlataformaRepository,
                                  AssinaturaService assinaturaService,
                                  AssasClient assasClient) {
        this.assinaturaRepository = assinaturaRepository;
        this.cobrancaPlataformaRepository = cobrancaPlataformaRepository;
        this.pagamentoPlataformaRepository = pagamentoPlataformaRepository;
        this.assinaturaService = assinaturaService;
        this.assasClient = assasClient;
    }

    @Transactional(readOnly = true)
    public List<AssinaturaResponseDTO> listarAssinaturas(AdminAssinaturaFiltroDTO filtro) {
        List<Assinatura> assinaturas;

        if (filtro != null && filtro.getStatus() != null && !filtro.getStatus().isBlank()) {
            StatusAssinatura status = StatusAssinatura.valueOf(filtro.getStatus());
            assinaturas = assinaturaRepository.findByStatus(status);
        } else {
            assinaturas = assinaturaRepository.findAllWithDetails();
        }

        return assinaturas.stream()
                .map(assinaturaService::toAssinaturaResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssinaturaResponseDTO detalharAssinatura(Long id) {
        Assinatura assinatura = assinaturaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura nao encontrada com ID: " + id));
        return assinaturaService.toAssinaturaResponseDTO(assinatura);
    }

    @Transactional(readOnly = true)
    public List<CobrancaPlataformaDTO> listarCobrancasAssinatura(Long assinaturaId) {
        List<CobrancaPlataforma> cobrancas = cobrancaPlataformaRepository.findByAssinaturaId(assinaturaId);
        return cobrancas.stream()
                .map(this::toCobrancaDTO)
                .collect(Collectors.toList());
    }

    // === Buscar assinatura por organizacao ID ===
    @Transactional(readOnly = true)
    public AssinaturaResponseDTO buscarPorOrganizacaoId(Long organizacaoId) {
        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura nao encontrada para a organizacao ID: " + organizacaoId));
        return assinaturaService.toAssinaturaResponseDTO(assinatura);
    }

    // === Listar cobrancas por organizacao ID ===
    @Transactional(readOnly = true)
    public List<CobrancaPlataformaDTO> listarCobrancasPorOrganizacao(Long organizacaoId) {
        List<CobrancaPlataforma> cobrancas = cobrancaPlataformaRepository.findByOrganizacaoId(organizacaoId);
        return cobrancas.stream()
                .map(this::toCobrancaDTO)
                .collect(Collectors.toList());
    }

    // === Listar pagamentos de uma cobranca ===
    @Transactional(readOnly = true)
    public List<PagamentoPlataformaDTO> listarPagamentosCobranca(Long cobrancaId) {
        List<PagamentoPlataforma> pagamentos = pagamentoPlataformaRepository.findByCobrancaId(cobrancaId);
        return pagamentos.stream()
                .map(this::toPagamentoDTO)
                .collect(Collectors.toList());
    }

    // === Listar todos os pagamentos de uma organizacao ===
    @Transactional(readOnly = true)
    public List<PagamentoPlataformaDTO> listarPagamentosPorOrganizacao(Long organizacaoId) {
        List<PagamentoPlataforma> pagamentos = pagamentoPlataformaRepository.findByOrganizacaoId(organizacaoId);
        return pagamentos.stream()
                .map(this::toPagamentoDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssinaturaResponseDTO cancelarAssinatura(Long id) {
        Assinatura assinatura = assinaturaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura nao encontrada com ID: " + id));

        try {
            assasClient.cancelarAssinatura(assinatura.getAssasSubscriptionId());
        } catch (AssasApiException e) {
            log.warn("Erro ao cancelar assinatura no Asaas (admin): {}", e.getMessage());
        }

        assinatura.setStatus(StatusAssinatura.CANCELADA);
        assinatura.setDtCancelamento(LocalDateTime.now());
        assinaturaRepository.save(assinatura);

        log.info("Assinatura ID {} cancelada pelo admin", id);
        return assinaturaService.toAssinaturaResponseDTO(assinatura);
    }

    @Transactional
    public AssinaturaResponseDTO suspenderAssinatura(Long id) {
        Assinatura assinatura = assinaturaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura nao encontrada com ID: " + id));

        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinaturaRepository.save(assinatura);

        log.info("Assinatura ID {} suspensa pelo admin", id);
        return assinaturaService.toAssinaturaResponseDTO(assinatura);
    }

    @Transactional
    public AssinaturaResponseDTO reativarAssinatura(Long id) {
        Assinatura assinatura = assinaturaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura nao encontrada com ID: " + id));

        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDtCancelamento(null);
        assinaturaRepository.save(assinatura);

        log.info("Assinatura ID {} reativada pelo admin", id);
        return assinaturaService.toAssinaturaResponseDTO(assinatura);
    }

    @Transactional(readOnly = true)
    public AdminBillingDashboardDTO getDashboardMetricas() {
        long totalAtivas = assinaturaRepository.countByStatus(StatusAssinatura.ATIVA);
        long totalTrial = assinaturaRepository.countByStatus(StatusAssinatura.TRIAL);
        long totalVencidas = assinaturaRepository.countByStatus(StatusAssinatura.VENCIDA);
        long totalCanceladas = assinaturaRepository.countByStatus(StatusAssinatura.CANCELADA);
        long totalSuspensas = assinaturaRepository.countByStatus(StatusAssinatura.SUSPENSA);

        int mesAtual = LocalDate.now().getMonthValue();
        int anoAtual = LocalDate.now().getYear();
        BigDecimal receitaMesAtual = cobrancaPlataformaRepository.calcularReceitaMes(mesAtual, anoAtual);

        // MRR simplificado: receita do mes atual
        BigDecimal mrr = receitaMesAtual;

        return AdminBillingDashboardDTO.builder()
                .mrr(mrr)
                .totalAtivas(totalAtivas)
                .totalTrial(totalTrial)
                .totalVencidas(totalVencidas)
                .totalCanceladas(totalCanceladas)
                .totalSuspensas(totalSuspensas)
                .receitaMesAtual(receitaMesAtual)
                .build();
    }

    private CobrancaPlataformaDTO toCobrancaDTO(CobrancaPlataforma cobranca) {
        return CobrancaPlataformaDTO.builder()
                .id(cobranca.getId())
                .valor(cobranca.getValor())
                .dtVencimento(cobranca.getDtVencimento())
                .dtPagamento(cobranca.getDtPagamento())
                .status(cobranca.getStatus().name())
                .formaPagamento(cobranca.getFormaPagamento() != null ? cobranca.getFormaPagamento().name() : null)
                .assasInvoiceUrl(cobranca.getAssasInvoiceUrl())
                .assasBankSlipUrl(cobranca.getAssasBankSlipUrl())
                .assasPixQrCode(cobranca.getAssasPixQrCode())
                .assasPixCopiaCola(cobranca.getAssasPixCopiaCola())
                .referenciaMes(cobranca.getReferenciaMes())
                .referenciaAno(cobranca.getReferenciaAno())
                .dtCriacao(cobranca.getDtCriacao())
                .build();
    }

    private PagamentoPlataformaDTO toPagamentoDTO(PagamentoPlataforma pagamento) {
        return PagamentoPlataformaDTO.builder()
                .id(pagamento.getId())
                .cobrancaId(pagamento.getCobranca().getId())
                .valor(pagamento.getValor())
                .status(pagamento.getStatus().name())
                .formaPagamento(pagamento.getFormaPagamento().name())
                .assasPaymentId(pagamento.getAssasPaymentId())
                .dtPagamento(pagamento.getDtPagamento())
                .dtCriacao(pagamento.getDtCriacao())
                .build();
    }
}
