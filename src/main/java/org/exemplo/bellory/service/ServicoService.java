package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }


    public List<Servico> getListAllServicos() {

        return this.servicoRepository.findAllByOrderByNomeAsc();
    }

    public List<ServicoAgendamento> getListAgendamentoServicos() {

        return this.servicoRepository.findAllProjectedBy();
    }

    public Servico createServico(Servico servico) {
        // Validação de campos obrigatórios e básicos
        if (servico.getOrganizacao_id() <= 0) {
            throw new IllegalArgumentException("O ID da organização é obrigatório e deve ser maior que zero.");
        }
        if (servico.getNome() == null || servico.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do serviço é obrigatório.");
        }
        if (servico.getCategoria() == null || servico.getCategoria().trim().isEmpty()) {
            throw new IllegalArgumentException("A categoria do serviço é obrigatória.");
        }
        if (servico.getPreco() == null || servico.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço do serviço é obrigatório e deve ser maior que zero.");
        }
        if (servico.getDuracaoEstimadaMinutos() == null || servico.getDuracaoEstimadaMinutos() <= 0) {
            throw new IllegalArgumentException("A duração estimada do serviço é obrigatória e deve ser maior que zero.");
        }

        // Validação de unicidade do nome do serviço (opcional, mas recomendado)
        if (servicoRepository.existsByNome(servico.getNome())) {
            throw new IllegalArgumentException("Já existe um serviço com o nome '" + servico.getNome() + "'.");
        }

        // Definir valores padrão se necessário (ex: um novo serviço é ativo por padrão)
        // Se o campo 'ativo' não for fornecido ou for nulo, define como true
//        if (servico.getAtivo() == null) {
//            servico.setAtivo(true);
//        }
        // Exemplo: definir usuário de criação/atualização se não for feito por JWT ou outro filtro
        // servico.setUsuarioAtualizacao("API_CREATE");

        // Salva o serviço no banco de dados
        return servicoRepository.save(servico);
    }
    // NOVO MÉTODO: Para atualizar um serviço existente
    public Servico updateServico(Long id, Servico servicoDetalhes) {
        // 1. Verificar se o serviço existe
        Servico servicoExistente = servicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serviço com ID " + id + " não encontrado para atualização."));

        // 2. Aplicar as atualizações nos campos
        // Você pode escolher quais campos podem ser atualizados
        if (servicoDetalhes.getOrganizacao_id() > 0) { // Exemplo: só atualiza se for um ID válido
            servicoExistente.setOrganizacao_id(servicoDetalhes.getOrganizacao_id());
        }
        if (servicoDetalhes.getNome() != null && !servicoDetalhes.getNome().trim().isEmpty()) {
            // Verifique se o novo nome já existe e não é o nome do próprio serviço que está sendo atualizado
            if (!servicoExistente.getNome().equals(servicoDetalhes.getNome()) && servicoRepository.existsByNome(servicoDetalhes.getNome())) {
                throw new IllegalArgumentException("Já existe outro serviço com o nome '" + servicoDetalhes.getNome() + "'.");
            }
            servicoExistente.setNome(servicoDetalhes.getNome());
        }
        if (servicoDetalhes.getCategoria() != null && !servicoDetalhes.getCategoria().trim().isEmpty()) {
            servicoExistente.setCategoria(servicoDetalhes.getCategoria());
        }
        if (servicoDetalhes.getGenero() != null) { // Gênero pode ser nulo ou vazio
            servicoExistente.setGenero(servicoDetalhes.getGenero());
        }
        if (servicoDetalhes.getDescricao() != null) { // Descrição pode ser nula ou vazia
            servicoExistente.setDescricao(servicoDetalhes.getDescricao());
        }
        if (servicoDetalhes.getDuracaoEstimadaMinutos() != null && servicoDetalhes.getDuracaoEstimadaMinutos() > 0) {
            servicoExistente.setDuracaoEstimadaMinutos(servicoDetalhes.getDuracaoEstimadaMinutos());
        }
        if (servicoDetalhes.getPreco() != null && servicoDetalhes.getPreco().compareTo(BigDecimal.ZERO) > 0) {
            servicoExistente.setPreco(servicoDetalhes.getPreco());
        }
        // Para listas (produtos, urlsImagens), você pode optar por substituir a lista ou adicionar/remover itens.
        // Aqui, estou optando por substituir se uma nova lista for fornecida.
        if (servicoDetalhes.getProdutos() != null) {
            servicoExistente.setProdutos(servicoDetalhes.getProdutos());
        }
        if (servicoDetalhes.getUrlsImagens() != null) {
            servicoExistente.setUrlsImagens(servicoDetalhes.getUrlsImagens());
        }
        // Ativo pode ser atualizado para true ou false
//        if (servicoDetalhes.getAtivo() != null) {
//            servicoExistente.setAtivo(servicoDetalhes.getAtivo());
//        }
        // O campo dtAtualizacao será gerenciado pelo @UpdateTimestamp

        // 3. Validações adicionais (se houver, como dependências entre campos, etc.)
        // As validações básicas de formato já foram feitas ao copiar os dados válidos.

        // 4. Salvar o serviço atualizado
        return servicoRepository.save(servicoExistente);
    }
}
