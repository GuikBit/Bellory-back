package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.entity.enums.TipoCategoria;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> findByTipo(TipoCategoria tipo) {
        return categoriaRepository.findByTipo(tipo);
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
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria updateCategoria(Long id, Categoria categoriaDetails) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria com ID " + id + " não encontrada."));

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
        categoria.setAtivo(false);
        categoriaRepository.save(categoria);
    }
}
