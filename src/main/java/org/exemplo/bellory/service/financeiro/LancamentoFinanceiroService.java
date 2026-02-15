package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.*;
import org.exemplo.bellory.model.entity.financeiro.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.financeiro.*;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LancamentoFinanceiroService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final CategoriaFinanceiraRepository categoriaRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final OrganizacaoRepository organizacaoRepository;

    @Transactional
    public LancamentoFinanceiroResponseDTO criar(LancamentoFinanceiroCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        LancamentoFinanceiro lancamento = new LancamentoFinanceiro();
        lancamento.setOrganizacao(organizacao);
        lancamento.setTipo(LancamentoFinanceiro.TipoLancamento.valueOf(dto.getTipo()));
        lancamento.setDescricao(dto.getDescricao());
        lancamento.setValor(dto.getValor());
        lancamento.setDtLancamento(dto.getDtLancamento() != null ? dto.getDtLancamento() : LocalDate.now());
        lancamento.setDtCompetencia(dto.getDtCompetencia() != null ? dto.getDtCompetencia() : lancamento.getDtLancamento());
        lancamento.setFormaPagamento(dto.getFormaPagamento());
        lancamento.setDocumento(dto.getDocumento());
        lancamento.setNumeroNota(dto.getNumeroNota());
        lancamento.setObservacoes(dto.getObservacoes());

        if (dto.getStatus() != null) {
            lancamento.setStatus(LancamentoFinanceiro.StatusLancamento.valueOf(dto.getStatus()));
        }

        // Vincular categoria
        if (dto.getCategoriaFinanceiraId() != null) {
            CategoriaFinanceira categoria = categoriaRepository.findByIdAndOrganizacaoId(dto.getCategoriaFinanceiraId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria financeira não encontrada."));
            lancamento.setCategoriaFinanceira(categoria);
        }

        // Vincular centro de custo
        if (dto.getCentroCustoId() != null) {
            CentroCusto centroCusto = centroCustoRepository.findByIdAndOrganizacaoId(dto.getCentroCustoId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));
            lancamento.setCentroCusto(centroCusto);
        }

        // Vincular conta bancária
        if (dto.getContaBancariaId() != null) {
            ContaBancaria contaBancaria = contaBancariaRepository.findByIdAndOrganizacaoId(dto.getContaBancariaId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
            lancamento.setContaBancaria(contaBancaria);

            // Atualizar saldo se efetivado
            if (lancamento.isEfetivado()) {
                if (lancamento.isReceita()) {
                    contaBancaria.creditar(dto.getValor());
                } else if (lancamento.isDespesa()) {
                    contaBancaria.debitar(dto.getValor());
                }
                contaBancariaRepository.save(contaBancaria);
            }
        }

        // Transferência entre contas
        if (lancamento.isTransferencia() && dto.getContaBancariaDestinoId() != null) {
            ContaBancaria contaDestino = contaBancariaRepository.findByIdAndOrganizacaoId(dto.getContaBancariaDestinoId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta bancária de destino não encontrada."));
            lancamento.setContaBancariaDestino(contaDestino);

            if (lancamento.isEfetivado() && lancamento.getContaBancaria() != null) {
                lancamento.getContaBancaria().debitar(dto.getValor());
                contaDestino.creditar(dto.getValor());
                contaBancariaRepository.save(lancamento.getContaBancaria());
                contaBancariaRepository.save(contaDestino);
            }
        }

        // Vincular conta a pagar
        if (dto.getContaPagarId() != null) {
            ContaPagar contaPagar = contaPagarRepository.findByIdAndOrganizacaoId(dto.getContaPagarId(), orgId)
                    .orElse(null);
            lancamento.setContaPagar(contaPagar);
        }

        // Vincular conta a receber
        if (dto.getContaReceberId() != null) {
            ContaReceber contaReceber = contaReceberRepository.findByIdAndOrganizacaoId(dto.getContaReceberId(), orgId)
                    .orElse(null);
            lancamento.setContaReceber(contaReceber);
        }

        lancamento = lancamentoRepository.save(lancamento);
        return new LancamentoFinanceiroResponseDTO(lancamento);
    }

    @Transactional
    public LancamentoFinanceiroResponseDTO atualizar(Long id, LancamentoFinanceiroUpdateDTO dto) {
        Long orgId = getOrganizacaoId();
        LancamentoFinanceiro lancamento = lancamentoRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento financeiro não encontrado."));

        if (lancamento.getStatus() == LancamentoFinanceiro.StatusLancamento.CANCELADO) {
            throw new IllegalStateException("Não é possível alterar um lançamento cancelado.");
        }

        if (dto.getDescricao() != null) lancamento.setDescricao(dto.getDescricao());
        if (dto.getValor() != null) lancamento.setValor(dto.getValor());
        if (dto.getDtLancamento() != null) lancamento.setDtLancamento(dto.getDtLancamento());
        if (dto.getDtCompetencia() != null) lancamento.setDtCompetencia(dto.getDtCompetencia());
        if (dto.getFormaPagamento() != null) lancamento.setFormaPagamento(dto.getFormaPagamento());
        if (dto.getDocumento() != null) lancamento.setDocumento(dto.getDocumento());
        if (dto.getNumeroNota() != null) lancamento.setNumeroNota(dto.getNumeroNota());
        if (dto.getObservacoes() != null) lancamento.setObservacoes(dto.getObservacoes());

        if (dto.getCategoriaFinanceiraId() != null) {
            CategoriaFinanceira categoria = categoriaRepository.findByIdAndOrganizacaoId(dto.getCategoriaFinanceiraId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria financeira não encontrada."));
            lancamento.setCategoriaFinanceira(categoria);
        }

        if (dto.getCentroCustoId() != null) {
            CentroCusto centroCusto = centroCustoRepository.findByIdAndOrganizacaoId(dto.getCentroCustoId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));
            lancamento.setCentroCusto(centroCusto);
        }

        lancamento = lancamentoRepository.save(lancamento);
        return new LancamentoFinanceiroResponseDTO(lancamento);
    }

    @Transactional
    public void cancelar(Long id) {
        Long orgId = getOrganizacaoId();
        LancamentoFinanceiro lancamento = lancamentoRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento financeiro não encontrado."));

        if (lancamento.getStatus() == LancamentoFinanceiro.StatusLancamento.CANCELADO) {
            throw new IllegalStateException("Lançamento já está cancelado.");
        }

        // Reverter saldo se estava efetivado
        if (lancamento.isEfetivado() && lancamento.getContaBancaria() != null) {
            if (lancamento.isReceita()) {
                lancamento.getContaBancaria().debitar(lancamento.getValor());
            } else if (lancamento.isDespesa()) {
                lancamento.getContaBancaria().creditar(lancamento.getValor());
            } else if (lancamento.isTransferencia()) {
                lancamento.getContaBancaria().creditar(lancamento.getValor());
                if (lancamento.getContaBancariaDestino() != null) {
                    lancamento.getContaBancariaDestino().debitar(lancamento.getValor());
                    contaBancariaRepository.save(lancamento.getContaBancariaDestino());
                }
            }
            contaBancariaRepository.save(lancamento.getContaBancaria());
        }

        lancamento.cancelar();
        lancamentoRepository.save(lancamento);
    }

    @Transactional
    public LancamentoFinanceiroResponseDTO efetivar(Long id) {
        Long orgId = getOrganizacaoId();
        LancamentoFinanceiro lancamento = lancamentoRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento financeiro não encontrado."));

        if (lancamento.isEfetivado()) {
            throw new IllegalStateException("Lançamento já está efetivado.");
        }

        lancamento.efetivar();

        // Atualizar saldo da conta
        if (lancamento.getContaBancaria() != null) {
            if (lancamento.isReceita()) {
                lancamento.getContaBancaria().creditar(lancamento.getValor());
            } else if (lancamento.isDespesa()) {
                lancamento.getContaBancaria().debitar(lancamento.getValor());
            } else if (lancamento.isTransferencia()) {
                lancamento.getContaBancaria().debitar(lancamento.getValor());
                if (lancamento.getContaBancariaDestino() != null) {
                    lancamento.getContaBancariaDestino().creditar(lancamento.getValor());
                    contaBancariaRepository.save(lancamento.getContaBancariaDestino());
                }
            }
            contaBancariaRepository.save(lancamento.getContaBancaria());
        }

        lancamento = lancamentoRepository.save(lancamento);
        return new LancamentoFinanceiroResponseDTO(lancamento);
    }

    public LancamentoFinanceiroResponseDTO buscarPorId(Long id) {
        Long orgId = getOrganizacaoId();
        LancamentoFinanceiro lancamento = lancamentoRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento financeiro não encontrado."));
        return new LancamentoFinanceiroResponseDTO(lancamento);
    }

    public List<LancamentoFinanceiroResponseDTO> listar(FiltroFinanceiroDTO filtro) {
        Long orgId = getOrganizacaoId();
        List<LancamentoFinanceiro> lancamentos;

        if (filtro != null && filtro.getDataInicio() != null && filtro.getDataFim() != null) {
            if (filtro.getTipo() != null) {
                lancamentos = lancamentoRepository.findByTipoAndPeriodo(orgId,
                        LancamentoFinanceiro.TipoLancamento.valueOf(filtro.getTipo()),
                        filtro.getDataInicio(), filtro.getDataFim());
            } else if (filtro.getCategoriaFinanceiraId() != null) {
                lancamentos = lancamentoRepository.findByCategoriaAndPeriodo(orgId,
                        filtro.getCategoriaFinanceiraId(), filtro.getDataInicio(), filtro.getDataFim());
            } else if (filtro.getCentroCustoId() != null) {
                lancamentos = lancamentoRepository.findByCentroCustoAndPeriodo(orgId,
                        filtro.getCentroCustoId(), filtro.getDataInicio(), filtro.getDataFim());
            } else if (filtro.getContaBancariaId() != null) {
                lancamentos = lancamentoRepository.findByContaBancariaAndPeriodo(orgId,
                        filtro.getContaBancariaId(), filtro.getDataInicio(), filtro.getDataFim());
            } else {
                lancamentos = lancamentoRepository.findByOrganizacaoAndPeriodo(orgId,
                        filtro.getDataInicio(), filtro.getDataFim());
            }
        } else if (filtro != null && filtro.getTipo() != null) {
            lancamentos = lancamentoRepository.findByOrganizacaoIdAndTipo(orgId,
                    LancamentoFinanceiro.TipoLancamento.valueOf(filtro.getTipo()));
        } else {
            lancamentos = lancamentoRepository.findByOrganizacaoId(orgId);
        }

        return lancamentos.stream()
                .map(LancamentoFinanceiroResponseDTO::new)
                .collect(Collectors.toList());
    }

    public BigDecimal getReceitasPeriodo(LocalDate inicio, LocalDate fim) {
        Long orgId = getOrganizacaoId();
        return lancamentoRepository.sumReceitasByPeriodo(orgId, inicio, fim);
    }

    public BigDecimal getDespesasPeriodo(LocalDate inicio, LocalDate fim) {
        Long orgId = getOrganizacaoId();
        return lancamentoRepository.sumDespesasByPeriodo(orgId, inicio, fim);
    }

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new IllegalStateException("Contexto de organização não encontrado.");
        }
        return orgId;
    }
}
