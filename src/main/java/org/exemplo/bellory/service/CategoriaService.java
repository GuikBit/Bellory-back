package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.entity.enums.TipoCategoria;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final OrganizacaoRepository organizacaoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, OrganizacaoRepository organizacaoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.organizacaoRepository = organizacaoRepository;
    }

    public List<Categoria> findByTipo(TipoCategoria tipo) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return categoriaRepository.findByOrganizacao_IdAndTipoAndAtivoTrue(organizacaoId, tipo);
    }

    @Transactional
    public Categoria createCategoria(Categoria categoria) {
        if (categoria.getLabel() == null || categoria.getLabel().trim().isEmpty()) {
            throw new IllegalArgumentException("O campo 'label' da categoria é obrigatório.");
        }
        if (categoria.getValue() == null || categoria.getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("O campo 'value' da categoria é obrigatório.");
        }
        if (categoria.getTipo() == null) {
            throw new IllegalArgumentException("O campo 'tipo' da categoria é obrigatório.");
        }

        Long organizacaoId = getOrganizacaoIdFromContext();

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização com ID " + organizacaoId + " não encontrada."));

        categoria.setOrganizacao(organizacao);
        categoria.setAtivo(true);

        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria updateCategoria(Long id, Categoria categoriaDetails) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria com ID " + id + " não encontrada."));

        validarOrganizacao(categoria.getOrganizacao().getId());

        if (categoriaDetails.getLabel() != null) {
            categoria.setLabel(categoriaDetails.getLabel());
        }
        if (categoriaDetails.getValue() != null) {
            categoria.setValue(categoriaDetails.getValue());
        }
        if (categoriaDetails.getTipo() != null) {
            categoria.setTipo(categoriaDetails.getTipo());
        }
        categoria.setAtivo(categoriaDetails.isAtivo());

        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void deleteCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria com ID " + id + " não encontrada."));

        validarOrganizacao(categoria.getOrganizacao().getId());

        categoria.setAtivo(false);
        categoriaRepository.save(categoria);
    }

    // --------------------
    // Métodos de Validação
    // --------------------

    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }

        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }

        return organizacaoId;
    }
}
