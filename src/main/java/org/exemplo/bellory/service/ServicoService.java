package org.exemplo.bellory.service;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.dto.ServicoCreateDTO;
import org.exemplo.bellory.model.dto.ServicoDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final CategoriaRepository categoriaRepository; // Adicionado

    public ServicoService(ServicoRepository servicoRepository, OrganizacaoRepository organizacaoRepository, CategoriaRepository categoriaRepository) {
        this.servicoRepository = servicoRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.categoriaRepository = categoriaRepository; // Adicionado
    }

    public List<Servico> getListAllServicos() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return this.servicoRepository.findAllByOrganizacao_IdOrderByNomeAsc(organizacaoId);
    }

    public Servico getServicoById(Long id) {

        Servico servico = servicoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + id + " não encontrado."));

        validarOrganizacao(servico.getOrganizacao().getId());

        return servico;
    }

    public List<ServicoAgendamento> getListAgendamentoServicos() {
        Long organizacaoId = getOrganizacaoIdFromContext();
        return servicoRepository.findAllProjectedByOrganizacao_Id(organizacaoId);
    }

    @Transactional
    public Servico createServico(ServicoCreateDTO dto) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        // Validação de campos obrigatórios
//        if (dto.getOrganizacaoId() == null) {
//            throw new IllegalArgumentException("O ID da organização é obrigatório.");
//        }
        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do serviço é obrigatório.");
        }
        if (dto.getPreco() == null || dto.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço do serviço é obrigatório e deve ser maior que zero.");
        }
        if (dto.getTempoEstimadoMinutos() == null || dto.getTempoEstimadoMinutos() <= 0) {
            throw new IllegalArgumentException("A duração estimada do serviço é obrigatória.");
        }
        if (dto.getCategoriaId() == null) {
            throw new IllegalArgumentException("O ID da categoria é obrigatório.");
        }

        // Validação de unicidade do nome do serviço
        if (servicoRepository.existsByNome(dto.getNome())) {
            throw new IllegalArgumentException("Já existe um serviço com o nome '" + dto.getNome() + "'.");
        }

        Organizacao org = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização com ID " + dto.getOrganizacaoId() + " não encontrada."));

        validarOrganizacao(org.getId());

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria com ID " + dto.getCategoriaId() + " não encontrada."));

        validarOrganizacao(categoria.getOrganizacao().getId());

        Servico novoServico = new Servico();
        novoServico.setOrganizacao(org);
        novoServico.setNome(dto.getNome());
        novoServico.setCategoria(categoria);
        novoServico.setGenero(dto.getGenero());
        novoServico.setDescricao(dto.getDescricao());
        novoServico.setTempoEstimadoMinutos(dto.getTempoEstimadoMinutos());
        novoServico.setPreco(dto.getPreco());
        novoServico.setUrlsImagens(dto.getUrlsImagens());
        novoServico.setAtivo(true);
        novoServico.setDtCriacao(LocalDateTime.now());

        return servicoRepository.save(novoServico);
    }

    @Transactional
    public Servico updateServico(Long id, ServicoCreateDTO dto) {
        Servico servicoExistente = getServicoById(id);

        validarOrganizacao(servicoExistente.getOrganizacao().getId());

        if (dto.getNome() != null && !dto.getNome().trim().isEmpty()) {
            servicoExistente.setNome(dto.getNome());
        }
        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria com ID " + dto.getCategoriaId() + " não encontrada."));
            servicoExistente.setCategoria(categoria);
        }
        if (dto.getGenero() != null) {
            servicoExistente.setGenero(dto.getGenero());
        }
        if (dto.getDescricao() != null) {
            servicoExistente.setDescricao(dto.getDescricao());
        }
        if (dto.getTempoEstimadoMinutos() != null && dto.getTempoEstimadoMinutos() > 0) {
            servicoExistente.setTempoEstimadoMinutos(dto.getTempoEstimadoMinutos());
        }
        if (dto.getPreco() != null && dto.getPreco().compareTo(BigDecimal.ZERO) > 0) {
            servicoExistente.setPreco(dto.getPreco());
        }
        if (dto.getUrlsImagens() != null) {
            servicoExistente.setUrlsImagens(dto.getUrlsImagens());
        }

        servicoExistente.setDtAtualizacao(LocalDateTime.now());
        return servicoRepository.save(servicoExistente);
    }

    @Transactional
    public void deleteServico(Long id) {

        Servico servico = getServicoById(id);
        validarOrganizacao(servico.getOrganizacao().getId());
        servico.setAtivo(false); // Soft delete

        servicoRepository.save(servico);
    }

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
