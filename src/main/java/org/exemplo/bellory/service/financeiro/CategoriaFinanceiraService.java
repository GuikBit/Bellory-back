package org.exemplo.bellory.service.financeiro;

import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.financeiro.CategoriaFinanceiraCreateDTO;
import org.exemplo.bellory.model.dto.financeiro.CategoriaFinanceiraResponseDTO;
import org.exemplo.bellory.model.entity.financeiro.CategoriaFinanceira;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.financeiro.CategoriaFinanceiraRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaFinanceiraService {

    private final CategoriaFinanceiraRepository categoriaRepository;
    private final OrganizacaoRepository organizacaoRepository;

    @Transactional
    public CategoriaFinanceiraResponseDTO criar(CategoriaFinanceiraCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        CategoriaFinanceira categoria = new CategoriaFinanceira();
        categoria.setOrganizacao(organizacao);
        categoria.setNome(dto.getNome());
        categoria.setDescricao(dto.getDescricao());
        categoria.setTipo(CategoriaFinanceira.TipoCategoria.valueOf(dto.getTipo()));
        categoria.setCor(dto.getCor());
        categoria.setIcone(dto.getIcone());

        if (dto.getCategoriaPaiId() != null) {
            CategoriaFinanceira pai = categoriaRepository.findByIdAndOrganizacaoId(dto.getCategoriaPaiId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria pai não encontrada."));
            categoria.setCategoriaPai(pai);
        }

        categoria = categoriaRepository.save(categoria);
        return new CategoriaFinanceiraResponseDTO(categoria);
    }

    @Transactional
    public CategoriaFinanceiraResponseDTO atualizar(Long id, CategoriaFinanceiraCreateDTO dto) {
        Long orgId = getOrganizacaoId();
        CategoriaFinanceira categoria = categoriaRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria financeira não encontrada."));

        categoria.setNome(dto.getNome());
        categoria.setDescricao(dto.getDescricao());
        if (dto.getTipo() != null) {
            categoria.setTipo(CategoriaFinanceira.TipoCategoria.valueOf(dto.getTipo()));
        }
        categoria.setCor(dto.getCor());
        categoria.setIcone(dto.getIcone());

        if (dto.getCategoriaPaiId() != null) {
            CategoriaFinanceira pai = categoriaRepository.findByIdAndOrganizacaoId(dto.getCategoriaPaiId(), orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria pai não encontrada."));
            categoria.setCategoriaPai(pai);
        } else {
            categoria.setCategoriaPai(null);
        }

        categoria = categoriaRepository.save(categoria);
        return new CategoriaFinanceiraResponseDTO(categoria);
    }

    public List<CategoriaFinanceiraResponseDTO> listarTodas() {
        Long orgId = getOrganizacaoId();
        return categoriaRepository.findByOrganizacaoIdAndAtivoTrue(orgId).stream()
                .map(CategoriaFinanceiraResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<CategoriaFinanceiraResponseDTO> listarPorTipo(String tipo) {
        Long orgId = getOrganizacaoId();
        CategoriaFinanceira.TipoCategoria tipoCategoria = CategoriaFinanceira.TipoCategoria.valueOf(tipo);
        return categoriaRepository.findByOrganizacaoIdAndTipoAndAtivoTrue(orgId, tipoCategoria).stream()
                .map(CategoriaFinanceiraResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<CategoriaFinanceiraResponseDTO> listarArvore() {
        Long orgId = getOrganizacaoId();
        return categoriaRepository.findCategoriasRaizByOrganizacao(orgId).stream()
                .map(CategoriaFinanceiraResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<CategoriaFinanceiraResponseDTO> listarArvorePorTipo(String tipo) {
        Long orgId = getOrganizacaoId();
        CategoriaFinanceira.TipoCategoria tipoCategoria = CategoriaFinanceira.TipoCategoria.valueOf(tipo);
        return categoriaRepository.findCategoriasRaizByOrganizacaoAndTipo(orgId, tipoCategoria).stream()
                .map(CategoriaFinanceiraResponseDTO::new)
                .collect(Collectors.toList());
    }

    public CategoriaFinanceiraResponseDTO buscarPorId(Long id) {
        Long orgId = getOrganizacaoId();
        CategoriaFinanceira categoria = categoriaRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria financeira não encontrada."));
        return new CategoriaFinanceiraResponseDTO(categoria);
    }

    @Transactional
    public void desativar(Long id) {
        Long orgId = getOrganizacaoId();
        CategoriaFinanceira categoria = categoriaRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria financeira não encontrada."));
        categoria.setAtivo(false);
        categoriaRepository.save(categoria);
    }

    @Transactional
    public void ativar(Long id) {
        Long orgId = getOrganizacaoId();
        CategoriaFinanceira categoria = categoriaRepository.findByIdAndOrganizacaoId(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria financeira não encontrada."));
        categoria.setAtivo(true);
        categoriaRepository.save(categoria);
    }

    private Long getOrganizacaoId() {
        Long orgId = TenantContext.getCurrentOrganizacaoId();
        if (orgId == null) {
            throw new IllegalStateException("Contexto de organização não encontrado.");
        }
        return orgId;
    }
}
