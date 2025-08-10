package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.dto.ServicoCreateDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Servico;
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

    public ServicoService(ServicoRepository servicoRepository, OrganizacaoRepository organizacaoRepository) {
        this.servicoRepository = servicoRepository;
        this.organizacaoRepository = organizacaoRepository;
    }

    public List<Servico> getListAllServicos() {
        return this.servicoRepository.findAllByOrderByNomeAsc();
    }

    public Servico getServicoById(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + id + " não encontrado."));
    }

    public List<ServicoAgendamento> getListAgendamentoServicos() {
        return this.servicoRepository.findAllProjectedBy();
    }

    @Transactional
    public Servico createServico(ServicoCreateDTO dto) {
        // Validação de campos obrigatórios
        if (dto.getOrganizacaoId() == null) {
            throw new IllegalArgumentException("O ID da organização é obrigatório.");
        }
        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do serviço é obrigatório.");
        }
        if (dto.getPreco() == null || dto.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço do serviço é obrigatório e deve ser maior que zero.");
        }
        if (dto.getTempoEstimadoMinutos() == null || dto.getTempoEstimadoMinutos() <= 0) {
            throw new IllegalArgumentException("A duração estimada do serviço é obrigatória.");
        }

        // Validação de unicidade do nome do serviço
        if (servicoRepository.existsByNome(dto.getNome())) {
            throw new IllegalArgumentException("Já existe um serviço com o nome '" + dto.getNome() + "'.");
        }

        Organizacao org = organizacaoRepository.findById(dto.getOrganizacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Organização com ID " + dto.getOrganizacaoId() + " não encontrada."));

        Servico novoServico = new Servico();
        novoServico.setOrganizacao(org);
        novoServico.setNome(dto.getNome());
        novoServico.setCategoria(dto.getCategoria());
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

        if (dto.getNome() != null && !dto.getNome().trim().isEmpty()) {
            servicoExistente.setNome(dto.getNome());
        }
        if (dto.getCategoria() != null) {
            servicoExistente.setCategoria(dto.getCategoria());
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
        servico.setAtivo(false); // Soft delete
        servicoRepository.save(servico);
    }
}
