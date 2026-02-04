package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.ContaBancariaCreateDTO;
import org.exemplo.bellory.model.dto.financeiro.ContaBancariaResponseDTO;
import org.exemplo.bellory.model.entity.financeiro.ContaBancaria;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.financeiro.ContaBancariaRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaBancariaService {

    private final ContaBancariaRepository contaBancariaRepository;
    private final OrganizacaoRepository organizacaoRepository;

    @Transactional
    public ContaBancariaResponseDTO criar(ContaBancariaCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        ContaBancaria conta = new ContaBancaria();
        conta.setOrganizacao(organizacao);
        conta.setNome(dto.getNome());
        conta.setTipoConta(ContaBancaria.TipoConta.valueOf(dto.getTipoConta()));
        conta.setBanco(dto.getBanco());
        conta.setAgencia(dto.getAgencia());
        conta.setNumeroConta(dto.getNumeroConta());
        conta.setSaldoInicial(dto.getSaldoInicial() != null ? dto.getSaldoInicial() : BigDecimal.ZERO);
        conta.setSaldoAtual(conta.getSaldoInicial());
        conta.setPrincipal(dto.getPrincipal() != null ? dto.getPrincipal() : false);
        conta.setCor(dto.getCor());
        conta.setIcone(dto.getIcone());

        // Se marcada como principal, desmarcar outras
        if (Boolean.TRUE.equals(conta.getPrincipal())) {
            contaBancariaRepository.findByOrganizacaoIdAndPrincipalTrue(orgId)
                    .ifPresent(outra -> {
                        outra.setPrincipal(false);
                        contaBancariaRepository.save(outra);
                    });
        }

        conta = contaBancariaRepository.save(conta);
        return new ContaBancariaResponseDTO(conta);
    }

    @Transactional
    public ContaBancariaResponseDTO atualizar(Long id, ContaBancariaCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        ContaBancaria conta = contaBancariaRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));

        conta.setNome(dto.getNome());
        if (dto.getTipoConta() != null) {
            conta.setTipoConta(ContaBancaria.TipoConta.valueOf(dto.getTipoConta()));
        }
        conta.setBanco(dto.getBanco());
        conta.setAgencia(dto.getAgencia());
        conta.setNumeroConta(dto.getNumeroConta());
        conta.setCor(dto.getCor());
        conta.setIcone(dto.getIcone());

        if (dto.getPrincipal() != null && dto.getPrincipal() && !Boolean.TRUE.equals(conta.getPrincipal())) {
            contaBancariaRepository.findByOrganizacaoIdAndPrincipalTrue(orgId)
                    .ifPresent(outra -> {
                        if (!outra.getId().equals(id)) {
                            outra.setPrincipal(false);
                            contaBancariaRepository.save(outra);
                        }
                    });
            conta.setPrincipal(true);
        }

        conta = contaBancariaRepository.save(conta);
        return new ContaBancariaResponseDTO(conta);
    }

    public List<ContaBancariaResponseDTO> listarTodas() {
        Long orgId = getOrganizacaoId();
        return contaBancariaRepository.findByOrganizacaoIdAndAtivoTrue(orgId).stream()
                .map(ContaBancariaResponseDTO::new)
                .collect(Collectors.toList());
    }

    public ContaBancariaResponseDTO buscarPorId(Long id) {
        Long orgId = getOrganizacaoId();
        ContaBancaria conta = contaBancariaRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
        return new ContaBancariaResponseDTO(conta);
    }

    public BigDecimal getSaldoTotal() {
        Long orgId = getOrganizacaoId();
        return contaBancariaRepository.sumSaldoAtualByOrganizacao(orgId);
    }

    @Transactional
    public void desativar(Long id) {
        Long orgId = getOrganizacaoId();
        ContaBancaria conta = contaBancariaRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
        conta.setAtivo(false);
        contaBancariaRepository.save(conta);
    }

    @Transactional
    public void ativar(Long id) {
        Long orgId = getOrganizacaoId();
        ContaBancaria conta = contaBancariaRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
        conta.setAtivo(true);
        contaBancariaRepository.save(conta);
    }

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new IllegalStateException("Contexto de organização não encontrado.");
        }
        return orgId;
    }
}
