package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.CentroCustoCreateDTO;
import org.exemplo.bellory.model.dto.financeiro.CentroCustoResponseDTO;
import org.exemplo.bellory.model.entity.financeiro.CentroCusto;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.financeiro.CentroCustoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CentroCustoService {

    private final CentroCustoRepository centroCustoRepository;
    private final OrganizacaoRepository organizacaoRepository;

    @Transactional
    public CentroCustoResponseDTO criar(CentroCustoCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setOrganizacao(organizacao);
        centroCusto.setNome(dto.getNome());
        centroCusto.setCodigo(dto.getCodigo());
        centroCusto.setDescricao(dto.getDescricao());

        centroCusto = centroCustoRepository.save(centroCusto);
        return new CentroCustoResponseDTO(centroCusto);
    }

    @Transactional
    public CentroCustoResponseDTO atualizar(Long id, CentroCustoCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        CentroCusto centroCusto = centroCustoRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));

        centroCusto.setNome(dto.getNome());
        centroCusto.setCodigo(dto.getCodigo());
        centroCusto.setDescricao(dto.getDescricao());

        centroCusto = centroCustoRepository.save(centroCusto);
        return new CentroCustoResponseDTO(centroCusto);
    }

    public List<CentroCustoResponseDTO> listarTodos() {
        Long orgId = getOrganizacaoId();
        return centroCustoRepository.findByOrganizacaoIdAndAtivoTrue(orgId).stream()
                .map(CentroCustoResponseDTO::new)
                .collect(Collectors.toList());
    }

    public CentroCustoResponseDTO buscarPorId(Long id) {
        Long orgId = getOrganizacaoId();
        CentroCusto centroCusto = centroCustoRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));
        return new CentroCustoResponseDTO(centroCusto);
    }

    @Transactional
    public void desativar(Long id) {
        Long orgId = getOrganizacaoId();
        CentroCusto centroCusto = centroCustoRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));
        centroCusto.setAtivo(false);
        centroCustoRepository.save(centroCusto);
    }

    @Transactional
    public void ativar(Long id) {
        Long orgId = getOrganizacaoId();
        CentroCusto centroCusto = centroCustoRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo não encontrado."));
        centroCusto.setAtivo(true);
        centroCustoRepository.save(centroCusto);
    }

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new IllegalStateException("Contexto de organização não encontrado.");
        }
        return orgId;
    }
}
