package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.*;
import org.exemplo.bellory.model.entity.financeiro.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.financeiro.*;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaReceberService {

    private final ContaReceberRepository contaReceberRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final ClienteRepository clienteRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final LancamentoFinanceiroService lancamentoService;

    @Transactional
    public ContaReceberResponseDTO criar(ContaReceberCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        ContaReceber conta = new ContaReceber();
        conta.setOrganizacao(organizacao);
        conta.setDescricao(dto.getDescricao());
        conta.setDocumento(dto.getDocumento());
        conta.setNumeroNota(dto.getNumeroNota());
        conta.setValor(dto.getValor());
        conta.setDtEmissao(dto.getDtEmissao());
        conta.setDtVencimento(dto.getDtVencimento());
        conta.setDtCompetencia(dto.getDtCompetencia() != null ? dto.getDtCompetencia() : dto.getDtVencimento());
        conta.setFormaPagamento(dto.getFormaPagamento());
        conta.setObservacoes(dto.getObservacoes());
        conta.setRecorrente(dto.getRecorrente() != null ? dto.getRecorrente() : false);

        if (dto.getValorDesconto() != null) {
            conta.setValorDesconto(dto.getValorDesconto());
        }

        if (dto.getPeriodicidade() != null) {
            conta.setPeriodicidade(ContaPagar.Periodicidade.valueOf(dto.getPeriodicidade()));
        }

        if (dto.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
            conta.setCliente(cliente);
        }

        vincularRelacionamentos(conta, dto.getCategoriaFinanceiraId(), dto.getCentroCustoId(), dto.getContaBancariaId(), orgId);

        // Parcelamento
        if (dto.getTotalParcelas() != null && dto.getTotalParcelas() > 1) {
            return criarParcelamento(conta, dto.getTotalParcelas(), organizacao);
        }

        conta = contaReceberRepository.save(conta);
        return new ContaReceberResponseDTO(conta);
    }

    private ContaReceberResponseDTO criarParcelamento(ContaReceber contaOriginal, int totalParcelas, Organizacao organizacao) {
        BigDecimal valorParcela = contaOriginal.getValor().divide(BigDecimal.valueOf(totalParcelas), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal valorRestante = contaOriginal.getValor();
        List<ContaReceber> parcelas = new ArrayList<>();

        for (int i = 1; i <= totalParcelas; i++) {
            ContaReceber parcela = new ContaReceber();
            parcela.setOrganizacao(organizacao);
            parcela.setDescricao(contaOriginal.getDescricao() + " - Parcela " + i + "/" + totalParcelas);
            parcela.setDocumento(contaOriginal.getDocumento());
            parcela.setNumeroNota(contaOriginal.getNumeroNota());
            parcela.setFormaPagamento(contaOriginal.getFormaPagamento());
            parcela.setObservacoes(contaOriginal.getObservacoes());
            parcela.setCategoriaFinanceira(contaOriginal.getCategoriaFinanceira());
            parcela.setCentroCusto(contaOriginal.getCentroCusto());
            parcela.setContaBancaria(contaOriginal.getContaBancaria());
            parcela.setCliente(contaOriginal.getCliente());
            parcela.setParcelaAtual(i);
            parcela.setTotalParcelas(totalParcelas);
            parcela.setDtCompetencia(contaOriginal.getDtCompetencia());

            if (i == totalParcelas) {
                parcela.setValor(valorRestante);
            } else {
                parcela.setValor(valorParcela);
                valorRestante = valorRestante.subtract(valorParcela);
            }

            parcela.setDtVencimento(contaOriginal.getDtVencimento().plusMonths(i - 1));

            parcelas.add(parcela);
        }

        List<ContaReceber> salvas = contaReceberRepository.saveAll(parcelas);

        ContaReceber primeira = salvas.get(0);
        for (int i = 1; i < salvas.size(); i++) {
            salvas.get(i).setContaReceberOrigem(primeira);
        }
        contaReceberRepository.saveAll(salvas);

        return new ContaReceberResponseDTO(primeira);
    }

    @Transactional
    public ContaReceberResponseDTO atualizar(Long id, ContaReceberUpdateDTO dto) {
        Long orgId = getOrganizacaoId();
        ContaReceber conta = contaReceberRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a receber não encontrada."));

        if (conta.isRecebida()) {
            throw new IllegalStateException("Não é possível alterar uma conta já recebida.");
        }

        if (dto.getDescricao() != null) conta.setDescricao(dto.getDescricao());
        if (dto.getDocumento() != null) conta.setDocumento(dto.getDocumento());
        if (dto.getNumeroNota() != null) conta.setNumeroNota(dto.getNumeroNota());
        if (dto.getValor() != null) conta.setValor(dto.getValor());
        if (dto.getValorDesconto() != null) conta.setValorDesconto(dto.getValorDesconto());
        if (dto.getDtVencimento() != null) conta.setDtVencimento(dto.getDtVencimento());
        if (dto.getDtCompetencia() != null) conta.setDtCompetencia(dto.getDtCompetencia());
        if (dto.getFormaPagamento() != null) conta.setFormaPagamento(dto.getFormaPagamento());
        if (dto.getObservacoes() != null) conta.setObservacoes(dto.getObservacoes());

        if (dto.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
            conta.setCliente(cliente);
        }

        vincularRelacionamentos(conta, dto.getCategoriaFinanceiraId(), dto.getCentroCustoId(), dto.getContaBancariaId(), orgId);

        conta = contaReceberRepository.save(conta);
        return new ContaReceberResponseDTO(conta);
    }

    @Transactional
    public ContaReceberResponseDTO receber(Long id, PagamentoContaDTO dto) {
        Long orgId = getOrganizacaoId();
        ContaReceber conta = contaReceberRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a receber não encontrada."));

        if (conta.isRecebida()) {
            throw new IllegalStateException("Esta conta já foi recebida.");
        }

        if (dto.getValorDesconto() != null) {
            conta.setValorDesconto(conta.getValorDesconto().add(dto.getValorDesconto()));
        }
        if (dto.getValorJuros() != null) {
            conta.setValorJuros(conta.getValorJuros().add(dto.getValorJuros()));
        }
        if (dto.getValorMulta() != null) {
            conta.setValorMulta(conta.getValorMulta().add(dto.getValorMulta()));
        }
        if (dto.getFormaPagamento() != null) {
            conta.setFormaPagamento(dto.getFormaPagamento());
        }

        LocalDate dataRecebimento = dto.getDataPagamento() != null ? dto.getDataPagamento() : LocalDate.now();
        conta.receber(dto.getValor(), dataRecebimento);

        // Atualizar conta bancária
        Long contaBancariaId = dto.getContaBancariaId() != null ? dto.getContaBancariaId() :
                (conta.getContaBancaria() != null ? conta.getContaBancaria().getId() : null);

        if (contaBancariaId != null) {
            ContaBancaria contaBancaria = contaBancariaRepository.findByIdAndOrganizacaoId(contaBancariaId, orgId)
                    .orElse(null);
            if (contaBancaria != null) {
                contaBancaria.creditar(dto.getValor());
                contaBancariaRepository.save(contaBancaria);
                conta.setContaBancaria(contaBancaria);
            }
        }

        conta = contaReceberRepository.save(conta);

        // Criar lançamento financeiro de receita
        LancamentoFinanceiroCreateDTO lancamentoDTO = new LancamentoFinanceiroCreateDTO();
        lancamentoDTO.setTipo("RECEITA");
        lancamentoDTO.setDescricao("Recebimento: " + conta.getDescricao());
        lancamentoDTO.setValor(dto.getValor());
        lancamentoDTO.setDtLancamento(dataRecebimento);
        lancamentoDTO.setDtCompetencia(conta.getDtCompetencia());
        lancamentoDTO.setContaBancariaId(contaBancariaId);
        lancamentoDTO.setContaReceberId(conta.getId());
        lancamentoDTO.setFormaPagamento(conta.getFormaPagamento());
        lancamentoDTO.setStatus("EFETIVADO");
        if (conta.getCategoriaFinanceira() != null) {
            lancamentoDTO.setCategoriaFinanceiraId(conta.getCategoriaFinanceira().getId());
        }
        if (conta.getCentroCusto() != null) {
            lancamentoDTO.setCentroCustoId(conta.getCentroCusto().getId());
        }
        lancamentoService.criar(lancamentoDTO);

        return new ContaReceberResponseDTO(conta);
    }

    @Transactional
    public void cancelar(Long id) {
        Long orgId = getOrganizacaoId();
        ContaReceber conta = contaReceberRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a receber não encontrada."));

        if (conta.isRecebida()) {
            throw new IllegalStateException("Não é possível cancelar uma conta já recebida.");
        }

        conta.cancelar();
        contaReceberRepository.save(conta);
    }

    public ContaReceberResponseDTO buscarPorId(Long id) {
        Long orgId = getOrganizacaoId();
        ContaReceber conta = contaReceberRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a receber não encontrada."));
        return new ContaReceberResponseDTO(conta);
    }

    public List<ContaReceberResponseDTO> listar(FiltroFinanceiroDTO filtro) {
        Long orgId = getOrganizacaoId();
        List<ContaReceber> contas;

        if (filtro != null && filtro.getDataInicio() != null && filtro.getDataFim() != null) {
            if (filtro.getStatus() != null) {
                contas = contaReceberRepository.findByOrganizacaoAndPeriodoAndStatus(
                        orgId, filtro.getDataInicio(), filtro.getDataFim(),
                        ContaReceber.StatusContaReceber.valueOf(filtro.getStatus()));
            } else {
                contas = contaReceberRepository.findByOrganizacaoAndPeriodo(orgId, filtro.getDataInicio(), filtro.getDataFim());
            }
        } else if (filtro != null && filtro.getStatus() != null) {
            contas = contaReceberRepository.findByOrganizacaoIdAndStatus(orgId, ContaReceber.StatusContaReceber.valueOf(filtro.getStatus()));
        } else if (filtro != null && filtro.getClienteId() != null) {
            contas = contaReceberRepository.findByOrganizacaoIdAndClienteId(orgId, filtro.getClienteId());
        } else {
            contas = contaReceberRepository.findByOrganizacaoId(orgId);
        }

        return contas.stream()
                .map(ContaReceberResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<ContaReceberResponseDTO> listarVencidas() {
        Long orgId = getOrganizacaoId();
        return contaReceberRepository.findVencidasByOrganizacao(orgId, LocalDate.now()).stream()
                .map(ContaReceberResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<ContaReceberResponseDTO> listarAVencer(int dias) {
        Long orgId = getOrganizacaoId();
        return contaReceberRepository.findAVencerByOrganizacao(orgId, LocalDate.now(), LocalDate.now().plusDays(dias)).stream()
                .map(ContaReceberResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<ContaReceberResponseDTO> listarPendentesPorCliente(Long clienteId) {
        Long orgId = getOrganizacaoId();
        return contaReceberRepository.findPendentesByCliente(orgId, clienteId).stream()
                .map(ContaReceberResponseDTO::new)
                .collect(Collectors.toList());
    }

    private void vincularRelacionamentos(ContaReceber conta, Long categoriaId, Long centroCustoId, Long contaBancariaId, Long orgId) {
        if (categoriaId != null) {
            CategoriaFinanceira categoria = categoriaRepository.findByIdAndOrganizacaoId(categoriaId, orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria financeira não encontrada."));
            conta.setCategoriaFinanceira(categoria);
        }
        if (centroCustoId != null) {
            CentroCusto centroCusto = centroCustoRepository.findByIdAndOrganizacaoId(centroCustoId, orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));
            conta.setCentroCusto(centroCusto);
        }
        if (contaBancariaId != null) {
            ContaBancaria contaBancaria = contaBancariaRepository.findByIdAndOrganizacaoId(contaBancariaId, orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
            conta.setContaBancaria(contaBancaria);
        }
    }

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new IllegalStateException("Contexto de organização não encontrado.");
        }
        return orgId;
    }
}
