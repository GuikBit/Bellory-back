package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.*;
import org.exemplo.bellory.model.entity.financeiro.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.financeiro.CategoriaFinanceiraRepository;
import org.exemplo.bellory.model.repository.financeiro.CentroCustoRepository;
import org.exemplo.bellory.model.repository.financeiro.ContaBancariaRepository;
import org.exemplo.bellory.model.repository.financeiro.ContaPagarRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaPagarService {

    private final ContaPagarRepository contaPagarRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final LancamentoFinanceiroService lancamentoService;

    @Transactional
    public ContaPagarResponseDTO criar(ContaPagarCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        ContaPagar conta = new ContaPagar();
        conta.setOrganizacao(organizacao);
        conta.setDescricao(dto.getDescricao());
        conta.setFornecedor(dto.getFornecedor());
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

        vincularRelacionamentos(conta, dto.getCategoriaFinanceiraId(), dto.getCentroCustoId(), dto.getContaBancariaId(), orgId);

        // Parcelamento
        if (dto.getTotalParcelas() != null && dto.getTotalParcelas() > 1) {
            return criarParcelamento(conta, dto.getTotalParcelas(), organizacao);
        }

        conta = contaPagarRepository.save(conta);
        return new ContaPagarResponseDTO(conta);
    }

    private ContaPagarResponseDTO criarParcelamento(ContaPagar contaOriginal, int totalParcelas, Organizacao organizacao) {
        BigDecimal valorParcela = contaOriginal.getValor().divide(BigDecimal.valueOf(totalParcelas), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal valorRestante = contaOriginal.getValor();
        List<ContaPagar> parcelas = new ArrayList<>();

        for (int i = 1; i <= totalParcelas; i++) {
            ContaPagar parcela = new ContaPagar();
            parcela.setOrganizacao(organizacao);
            parcela.setDescricao(contaOriginal.getDescricao() + " - Parcela " + i + "/" + totalParcelas);
            parcela.setFornecedor(contaOriginal.getFornecedor());
            parcela.setDocumento(contaOriginal.getDocumento());
            parcela.setNumeroNota(contaOriginal.getNumeroNota());
            parcela.setFormaPagamento(contaOriginal.getFormaPagamento());
            parcela.setObservacoes(contaOriginal.getObservacoes());
            parcela.setCategoriaFinanceira(contaOriginal.getCategoriaFinanceira());
            parcela.setCentroCusto(contaOriginal.getCentroCusto());
            parcela.setContaBancaria(contaOriginal.getContaBancaria());
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

        List<ContaPagar> salvas = contaPagarRepository.saveAll(parcelas);

        // Vincular parcelas à primeira como origem
        ContaPagar primeira = salvas.get(0);
        for (int i = 1; i < salvas.size(); i++) {
            salvas.get(i).setContaPagarOrigem(primeira);
        }
        contaPagarRepository.saveAll(salvas);

        return new ContaPagarResponseDTO(primeira);
    }

    @Transactional
    public ContaPagarResponseDTO atualizar(Long id, ContaPagarUpdateDTO dto) {
        Long orgId = getOrganizacaoId();
        ContaPagar conta = contaPagarRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a pagar não encontrada."));

        if (conta.isPaga()) {
            throw new IllegalStateException("Não é possível alterar uma conta já paga.");
        }

        if (dto.getDescricao() != null) conta.setDescricao(dto.getDescricao());
        if (dto.getFornecedor() != null) conta.setFornecedor(dto.getFornecedor());
        if (dto.getDocumento() != null) conta.setDocumento(dto.getDocumento());
        if (dto.getNumeroNota() != null) conta.setNumeroNota(dto.getNumeroNota());
        if (dto.getValor() != null) conta.setValor(dto.getValor());
        if (dto.getValorDesconto() != null) conta.setValorDesconto(dto.getValorDesconto());
        if (dto.getDtVencimento() != null) conta.setDtVencimento(dto.getDtVencimento());
        if (dto.getDtCompetencia() != null) conta.setDtCompetencia(dto.getDtCompetencia());
        if (dto.getFormaPagamento() != null) conta.setFormaPagamento(dto.getFormaPagamento());
        if (dto.getObservacoes() != null) conta.setObservacoes(dto.getObservacoes());

        vincularRelacionamentos(conta, dto.getCategoriaFinanceiraId(), dto.getCentroCustoId(), dto.getContaBancariaId(), orgId);

        conta = contaPagarRepository.save(conta);
        return new ContaPagarResponseDTO(conta);
    }

    @Transactional
    public ContaPagarResponseDTO pagar(Long id, PagamentoContaDTO dto) {
        Long orgId = getOrganizacaoId();
        ContaPagar conta = contaPagarRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a pagar não encontrada."));

        if (conta.isPaga()) {
            throw new IllegalStateException("Esta conta já foi paga.");
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

        LocalDate dataPagamento = dto.getDataPagamento() != null ? dto.getDataPagamento() : LocalDate.now();
        conta.pagar(dto.getValor(), dataPagamento);

        // Atualizar conta bancária se informada
        Long contaBancariaId = dto.getContaBancariaId() != null ? dto.getContaBancariaId() :
                (conta.getContaBancaria() != null ? conta.getContaBancaria().getId() : null);

        if (contaBancariaId != null) {
            ContaBancaria contaBancaria = contaBancariaRepository.findByIdAndOrganizacaoId(contaBancariaId, orgId)
                    .orElse(null);
            if (contaBancaria != null) {
                contaBancaria.debitar(dto.getValor());
                contaBancariaRepository.save(contaBancaria);
                conta.setContaBancaria(contaBancaria);
            }
        }

        conta = contaPagarRepository.save(conta);

        // Criar lançamento financeiro de despesa
        LancamentoFinanceiroCreateDTO lancamentoDTO = new LancamentoFinanceiroCreateDTO();
        lancamentoDTO.setTipo("DESPESA");
        lancamentoDTO.setDescricao("Pagamento: " + conta.getDescricao());
        lancamentoDTO.setValor(dto.getValor());
        lancamentoDTO.setDtLancamento(dataPagamento);
        lancamentoDTO.setDtCompetencia(conta.getDtCompetencia());
        lancamentoDTO.setContaBancariaId(contaBancariaId);
        lancamentoDTO.setContaPagarId(conta.getId());
        lancamentoDTO.setFormaPagamento(conta.getFormaPagamento());
        lancamentoDTO.setStatus("EFETIVADO");
        if (conta.getCategoriaFinanceira() != null) {
            lancamentoDTO.setCategoriaFinanceiraId(conta.getCategoriaFinanceira().getId());
        }
        if (conta.getCentroCusto() != null) {
            lancamentoDTO.setCentroCustoId(conta.getCentroCusto().getId());
        }
        lancamentoService.criar(lancamentoDTO);

        return new ContaPagarResponseDTO(conta);
    }

    @Transactional
    public void cancelar(Long id) {
        Long orgId = getOrganizacaoId();
        ContaPagar conta = contaPagarRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a pagar não encontrada."));

        if (conta.isPaga()) {
            throw new IllegalStateException("Não é possível cancelar uma conta já paga.");
        }

        conta.cancelar();
        contaPagarRepository.save(conta);
    }

    public ContaPagarResponseDTO buscarPorId(Long id) {
        Long orgId = getOrganizacaoId();
        ContaPagar conta = contaPagarRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a pagar não encontrada."));
        return new ContaPagarResponseDTO(conta);
    }

    public List<ContaPagarResponseDTO> listar(FiltroFinanceiroDTO filtro) {
        Long orgId = getOrganizacaoId();
        List<ContaPagar> contas;

        if (filtro != null && filtro.getDataInicio() != null && filtro.getDataFim() != null) {
            if (filtro.getStatus() != null) {
                contas = contaPagarRepository.findByOrganizacaoAndPeriodoAndStatus(
                        orgId, filtro.getDataInicio(), filtro.getDataFim(),
                        ContaPagar.StatusContaPagar.valueOf(filtro.getStatus()));
            } else {
                contas = contaPagarRepository.findByOrganizacaoAndPeriodo(orgId, filtro.getDataInicio(), filtro.getDataFim());
            }
        } else if (filtro != null && filtro.getStatus() != null) {
            contas = contaPagarRepository.findByOrganizacaoIdAndStatus(orgId, ContaPagar.StatusContaPagar.valueOf(filtro.getStatus()));
        } else {
            contas = contaPagarRepository.findByOrganizacaoId(orgId);
        }

        return contas.stream()
                .map(ContaPagarResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<ContaPagarResponseDTO> listarVencidas() {
        Long orgId = getOrganizacaoId();
        return contaPagarRepository.findVencidasByOrganizacao(orgId, LocalDate.now()).stream()
                .map(ContaPagarResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<ContaPagarResponseDTO> listarAVencer(int dias) {
        Long orgId = getOrganizacaoId();
        return contaPagarRepository.findAVencerByOrganizacao(orgId, LocalDate.now(), LocalDate.now().plusDays(dias)).stream()
                .map(ContaPagarResponseDTO::new)
                .collect(Collectors.toList());
    }

    private void vincularRelacionamentos(ContaPagar conta, Long categoriaId, Long centroCustoId, Long contaBancariaId, Long orgId) {
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
