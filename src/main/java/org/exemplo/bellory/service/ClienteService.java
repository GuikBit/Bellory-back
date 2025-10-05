package org.exemplo.bellory.service;

import jakarta.persistence.criteria.Predicate;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.dto.clienteDTO.*;
import org.exemplo.bellory.model.dto.compra.CompraDTO;
import org.exemplo.bellory.model.dto.compra.CompraFiltroDTO;
import org.exemplo.bellory.model.dto.compra.PagamentoDTO;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final CobrancaRepository cobrancaRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizacaoService organizacaoService;
    private final OrganizacaoRepository organizacaoRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          AgendamentoRepository agendamentoRepository,
                          CobrancaRepository cobrancaRepository,
                          PasswordEncoder passwordEncoder,
                          OrganizacaoService organizacaoService, OrganizacaoRepository organizacaoRepository) {
        this.clienteRepository = clienteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.cobrancaRepository = cobrancaRepository;
        this.passwordEncoder = passwordEncoder;
        this.organizacaoService = organizacaoService;
        this.organizacaoRepository = organizacaoRepository;
    }

    // =============== MÉTODOS EXISTENTES ATUALIZADOS ===============

    public List<ClienteDTO> getListAllCliente() {
        List<Cliente> clientes = this.clienteRepository.findAll();
        return clientes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =============== NOVOS MÉTODOS PARA CRUD ===============

    public List<ClienteDTO> getClientesComFiltros(ClienteFiltroDTO filtro, Sort sort) {
        Specification<Cliente> spec = createClienteSpecification(filtro);
        List<Cliente> clientes = clienteRepository.findAll(spec, sort);

        return clientes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ClienteDetalhadoDTO getClienteDetalhadoById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente com ID " + id + " não encontrado."));

        return convertToDetalhadoDTO(cliente);
    }

    public boolean verificarSeUsernameExiste(String username) {
        String usernameLimpo = username.trim().toLowerCase();
        return clienteRepository.findByUsername(usernameLimpo).isPresent();
    }

    public boolean verificarSeCpfExiste(String cpf) {
        // Importante: sempre limpe o CPF para consultar o banco sem formatação
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        return clienteRepository.findByCpf(cpfLimpo).isPresent();
    }
    @Transactional
    public ClienteDTO createCliente(ClienteCreateDTO dto) {
        // Validações

        // Verificar se existe método findByEmail no repository
         if (clienteRepository.findByEmail(dto.getEmail()).isPresent()) {
             throw new IllegalArgumentException("E-mail já existe.");
         }

         if(clienteRepository.findByCpf(dto.getCpf()).isPresent()){
             throw new IllegalArgumentException("CPF já existe.");
         }

        Cliente cliente = new Cliente();
        cliente.setOrganizacao(organizacaoRepository.findById(1L).orElse(null));
        cliente.setNomeCompleto(dto.getNomeCompleto());
        cliente.setEmail(dto.getEmail());
        cliente.setUsername("usuarioRapido");
        cliente.setPassword(passwordEncoder.encode("102030"));
        cliente.setTelefone(dto.getTelefone());
        cliente.setDataNascimento(dto.getDataNascimento());
        cliente.setCpf(dto.getCpf().replaceAll("[^0-9]", ""));
        cliente.setRole("ROLE_CLIENTE");
        cliente.setAtivo(true);
        cliente.setIsCadastroIncompleto(true);
        cliente.setDtCriacao(LocalDateTime.now());

        Cliente clienteSalvo = clienteRepository.save(cliente);
        return convertToDTO(clienteSalvo);
    }

    @Transactional
    public ClienteDTO updateCliente(Long id, ClienteUpdateDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente com ID " + id + " não encontrado."));

        if (dto.getNomeCompleto() != null) {
            cliente.setNomeCompleto(dto.getNomeCompleto());
        }
        if (dto.getEmail() != null) {
            cliente.setEmail(dto.getEmail());
        }
        if (dto.getTelefone() != null) {
            cliente.setTelefone(dto.getTelefone());
        }
        if (dto.getDataNascimento() != null) {
            cliente.setDataNascimento(dto.getDataNascimento());
        }

        Cliente clienteAtualizado = clienteRepository.save(cliente);
        return convertToDTO(clienteAtualizado);
    }

    @Transactional
    public void desativarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente com ID " + id + " não encontrado."));

        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    // =============== MÉTODOS PARA AGENDAMENTOS ===============

    public List<AgendamentoDTO> getAgendamentosCliente(AgendamentoFiltroDTO filtro) {
        Cliente cliente = clienteRepository.findById(filtro.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        List<Agendamento> agendamentos = agendamentoRepository.findByCliente(cliente);

        // Aplicar filtros manualmente se necessário
        if (filtro.getStatus() != null) {
            Status status = Status.valueOf(filtro.getStatus().stream().findFirst().get());
            agendamentos = agendamentos.stream()
                    .filter(a -> a.getStatus() == status)
                    .collect(Collectors.toList());
        }

        if (filtro.getDataInicio() != null || filtro.getDataFim() != null) {
            agendamentos = agendamentos.stream()
                    .filter(a -> {
                        LocalDate dataAgendamento = a.getDtAgendamento().toLocalDate();
                        boolean afterStart = filtro.getDataInicio() == null ||
                                !dataAgendamento.isBefore(filtro.getDataInicio());
                        boolean beforeEnd = filtro.getDataFim() == null ||
                                !dataAgendamento.isAfter(filtro.getDataFim());
                        return afterStart && beforeEnd;
                    })
                    .collect(Collectors.toList());
        }

        return agendamentos.stream()
                .map(this::convertAgendamentoToDTO)
                .collect(Collectors.toList());
    }

    public List<AgendamentoDTO> getProximosAgendamentosCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        LocalDateTime agora = LocalDateTime.now();
        return agendamentoRepository.findProximosAgendamentosCliente(
                        clienteId, agora, Status.CANCELADO)
                .stream()
                .limit(5)
                .map(this::convertAgendamentoToDTO)
                .collect(Collectors.toList());
    }

    // =============== MÉTODOS PARA COMPRAS ===============

    public List<CompraDTO> getComprasCliente(CompraFiltroDTO filtro) {
        // Implementação placeholder - seria necessário o CompraRepository
        return new ArrayList<>();
    }

    // =============== MÉTODOS PARA COBRANÇAS E PAGAMENTOS ===============

    public List<CobrancaDTO> getCobrancasCliente(Long clienteId, String status) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        List<Cobranca> cobrancas;
        if (status != null && !status.isEmpty()) {
            Status statusEnum = Status.valueOf(status.toUpperCase());
            cobrancas = cobrancaRepository.findByClienteAndStatusCobranca(cliente, statusEnum);
        } else {
            cobrancas = cobrancaRepository.findByCliente(cliente);
        }

        return cobrancas.stream()
                .map(this::convertCobrancaToDTO)
                .collect(Collectors.toList());
    }

    public List<PagamentoDTO> getPagamentosCliente(Long clienteId, String metodo) {
        // Implementação placeholder - seria necessário o PagamentoRepository
        return new ArrayList<>();
    }

    // =============== MÉTODOS PARA HISTÓRICO ===============

    public List<HistoricoClienteDTO> getHistoricoCliente(HistoricoFiltroDTO filtro) {
        // Implementação que combina agendamentos e compras
        List<HistoricoClienteDTO> historico = new ArrayList<>();

        // Buscar agendamentos
        if (filtro.getTipo() == null || filtro.getTipo().equals("TODOS") || filtro.getTipo().equals("AGENDAMENTO")) {
            Cliente cliente = clienteRepository.findById(filtro.getClienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

            List<Agendamento> agendamentos = agendamentoRepository.findByCliente(cliente);

            for (Agendamento agendamento : agendamentos) {
                HistoricoClienteDTO item = HistoricoClienteDTO.builder()
                        .transacaoId(agendamento.getId())
                        .tipoTransacao("AGENDAMENTO")
                        .descricao("Agendamento - " + getDescricaoServicos(agendamento))
                        .valor(calcularValorAgendamento(agendamento))
                        .status(agendamento.getStatus().name())
                        .dataTransacao(agendamento.getDtAgendamento())
                        .detalhes(getDescricaoServicos(agendamento))
                        .build();
                historico.add(item);
            }
        }

        // Aplicar filtros de data
        if (filtro.getDataInicio() != null || filtro.getDataFim() != null) {
            historico = historico.stream()
                    .filter(h -> {
                        LocalDate dataTransacao = h.getDataTransacao().toLocalDate();
                        boolean afterStart = filtro.getDataInicio() == null ||
                                !dataTransacao.isBefore(filtro.getDataInicio());
                        boolean beforeEnd = filtro.getDataFim() == null ||
                                !dataTransacao.isAfter(filtro.getDataFim());
                        return afterStart && beforeEnd;
                    })
                    .collect(Collectors.toList());
        }

        // Ordenar por data decrescente
        historico.sort((a, b) -> b.getDataTransacao().compareTo(a.getDataTransacao()));

        return historico;
    }

    // =============== MÉTODOS PARA ESTATÍSTICAS ===============

    public ResumoFinanceiroClienteDTO getResumoFinanceiroCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        BigDecimal valorTotalGasto = cobrancaRepository.sumByCliente(clienteId, Cobranca.StatusCobranca.PAGO);
        Long totalAgendamentos = agendamentoRepository.countByCliente(clienteId);
        LocalDateTime ultimoAgendamento = agendamentoRepository.findLastAgendamentoByCliente(clienteId);

        return ResumoFinanceiroClienteDTO.builder()
                .clienteId(clienteId)
                .nomeCliente(cliente.getNomeCompleto())
                .valorTotalGasto(valorTotalGasto != null ? valorTotalGasto : BigDecimal.ZERO)
                .valorTotalServicos(valorTotalGasto != null ? valorTotalGasto : BigDecimal.ZERO)
                .valorTotalProdutos(BigDecimal.ZERO) // Placeholder
                .totalAgendamentos(totalAgendamentos)
                .totalCompras(0L) // Placeholder
                .valorPago(valorTotalGasto != null ? valorTotalGasto : BigDecimal.ZERO)
                .valorPendente(BigDecimal.ZERO) // Calcular baseado no status das cobranças
                .ticketMedioServicos(calcularTicketMedio(valorTotalGasto, totalAgendamentos))
                .ultimoAgendamento(ultimoAgendamento)
                .periodoAnalise("Histórico completo")
                .build();
    }

    public List<ServicoEstatisticaDTO> getServicosFavoritosCliente(Long clienteId) {
        // Implementação placeholder - seria necessário analisar os serviços dos agendamentos
        return new ArrayList<>();
    }

    // =============== MÉTODOS PARA BUSCA ===============

    public List<ClienteDTO> buscarClientes(String termo) {
        Specification<Cliente> spec = (root, query, cb) -> {
            String termoBusca = "%" + termo.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nomeCompleto")), termoBusca),
                    cb.like(cb.lower(root.get("email")), termoBusca),
                    cb.like(root.get("telefone"), termoBusca)
            );
        };

        List<Cliente> clientes = clienteRepository.findAll(spec, Sort.by("nomeCompleto").ascending());
        return clientes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =============== MÉTODOS ESPECIAIS ===============

    public List<ClienteAniversarianteDTO> getAniversariantes(Integer mes, Integer ano) {
        LocalDate hoje = LocalDate.now();
        int mesConsulta = mes != null ? mes : hoje.getMonthValue();
        int anoConsulta = ano != null ? ano : hoje.getYear();

        return clienteRepository.findAll().stream()
                .filter(c -> c.getDataNascimento() != null)
                .filter(c -> c.getDataNascimento().getMonthValue() == mesConsulta)
                .map(cliente -> convertToAniversarianteDTO(cliente, anoConsulta))
                .sorted((a, b) -> Integer.compare(a.getDiasParaAniversario(), b.getDiasParaAniversario()))
                .collect(Collectors.toList());
    }

    public List<TopClienteDTO> getTopClientes(int limite) {
        return clienteRepository.findTopClientes().stream()
                .limit(limite)
                .map(this::convertToTopClienteDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClienteDTO alterarStatusCliente(Long id, Boolean ativo) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        cliente.setAtivo(ativo);
        Cliente clienteAtualizado = clienteRepository.save(cliente);
        return convertToDTO(clienteAtualizado);
    }

    public EstatisticasClientesDTO getEstatisticas() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioMes = agora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime inicioAno = agora.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        // Calcular início e fim da semana
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return EstatisticasClientesDTO.builder()
                .totalClientes(clienteRepository.count())
                .clientesAtivos(clienteRepository.countByAtivo(true))
                .clientesInativos(clienteRepository.countByAtivo(false))
                .novosClientesEsseMes(clienteRepository.countByDataCriacaoBetween(inicioMes, agora))
                .novosClientesEsteAno(clienteRepository.countByDataCriacaoBetween(inicioAno, agora))
                .aniversariantesHoje(clienteRepository.countAniversariantesHoje())
                .aniversariantesEstaSemana(clienteRepository.countAniversariantesEstaSemana(inicioSemana, fimSemana))
                .clientesRecorrentes(clienteRepository.countClientesRecorrentes())
                .build();
    }

    // =============== MÉTODOS AUXILIARES ===============

    private Organizacao getOrganizacaoPadrao() {
        // Implementação placeholder - buscar organização padrão
        return organizacaoService.getOrganizacaoPadrao();
    }

    private String getDescricaoServicos(Agendamento agendamento) {
        if (agendamento.getServicos() == null || agendamento.getServicos().isEmpty()) {
            return "Serviços não especificados";
        }

        return agendamento.getServicos().stream()
                .map(servico -> servico.getNome())
                .collect(Collectors.joining(", "));
    }

    private BigDecimal calcularValorAgendamento(Agendamento agendamento) {
        if (agendamento.getCobranca() != null) {
            return agendamento.getCobranca().getValor();
        }

        // Se não houver cobrança, calcular baseado nos serviços
        if (agendamento.getServicos() != null) {
            return agendamento.getServicos().stream()
                    .map(servico -> servico.getPreco())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularTicketMedio(BigDecimal valorTotal, Long quantidade) {
        if (quantidade == null || quantidade == 0 || valorTotal == null) {
            return BigDecimal.ZERO;
        }
        return valorTotal.divide(BigDecimal.valueOf(quantidade), 2, BigDecimal.ROUND_HALF_UP);
    }

    // =============== MÉTODOS DE CONVERSÃO ===============

    private ClienteDTO convertToDTO(Cliente cliente) {
        return ClienteDTO.builder()
                .id(cliente.getId())
                .nomeCompleto(cliente.getNomeCompleto())
                .email(cliente.getEmail())
                .telefone(cliente.getTelefone())
                .dataNascimento(cliente.getDataNascimento())
                .role(cliente.getRole())
                .cpf(cliente.getCpf())
                .ativo(cliente.isAtivo())
                .build();
    }

    private ClienteDetalhadoDTO convertToDetalhadoDTO(Cliente cliente) {
        Long totalAgendamentos = agendamentoRepository.countByCliente(cliente.getId());
        BigDecimal valorTotal = cobrancaRepository.sumByCliente(cliente.getId(), Cobranca.StatusCobranca.PAGO);
        LocalDateTime ultimoAgendamento = agendamentoRepository.findLastAgendamentoByCliente(cliente.getId());

        return ClienteDetalhadoDTO.builder()
                .id(cliente.getId())
                .nomeCompleto(cliente.getNomeCompleto())
                .email(cliente.getEmail())
                .telefone(cliente.getTelefone())
                .dataNascimento(cliente.getDataNascimento())
                .role(cliente.getRole())
                .ativo(cliente.isAtivo())
                .dtCriacao(cliente.getDtCriacao())
                .totalAgendamentos(totalAgendamentos)
                .valorTotalGasto(valorTotal != null ? valorTotal : BigDecimal.ZERO)
                .ultimoAgendamento(ultimoAgendamento)
                .agendamentosPendentes(0L) // Calcular se necessário
                .cobrancasPendentes(0L) // Calcular se necessário
                .build();
    }

    private ClienteAniversarianteDTO convertToAniversarianteDTO(Cliente cliente, int ano) {
        LocalDate hoje = LocalDate.now();
        LocalDate aniversarioEsteAno = cliente.getDataNascimento().withYear(ano);
        int diasPara = (int) ChronoUnit.DAYS.between(hoje, aniversarioEsteAno);

        return ClienteAniversarianteDTO.builder()
                .id(cliente.getId())
                .nomeCompleto(cliente.getNomeCompleto())
                .telefone(cliente.getTelefone())
                .email(cliente.getEmail())
                .dataNascimento(cliente.getDataNascimento())
                .idade(ano - cliente.getDataNascimento().getYear())
                .diasParaAniversario(diasPara)
                .aniversarioHoje(diasPara == 0)
                .build();
    }

    private TopClienteDTO convertToTopClienteDTO(Cliente cliente) {
        BigDecimal valorGasto = cobrancaRepository.sumByCliente(cliente.getId(), Cobranca.StatusCobranca.PAGO);
        Long totalAgendamentos = agendamentoRepository.countByCliente(cliente.getId());
        LocalDateTime ultimaVisita = agendamentoRepository.findLastAgendamentoByCliente(cliente.getId());

        return TopClienteDTO.builder()
                .id(cliente.getId())
                .nomeCompleto(cliente.getNomeCompleto())
                .telefone(cliente.getTelefone())
                .email(cliente.getEmail())
                .valorTotalGasto(valorGasto != null ? valorGasto : BigDecimal.ZERO)
                .totalAgendamentos(totalAgendamentos)
                .ultimaVisita(ultimaVisita)
                .classificacao(determinarClassificacao(valorGasto))
                .build();
    }

    private CobrancaDTO convertCobrancaToDTO(Cobranca cobranca) {
        return CobrancaDTO.builder()
                .id(cobranca.getId())
                .clienteId(cobranca.getCliente().getId())
                .nomeCliente(cobranca.getCliente().getNomeCompleto())
                .agendamentoId(cobranca.getAgendamento() != null ? cobranca.getAgendamento().getId() : null)
                .compraId(cobranca.getCompra() != null ? cobranca.getCompra().getId() : null)
                .valor(cobranca.getValor())
                .statusCobranca(cobranca.getStatusCobranca().name())
                .dtVencimento(cobranca.getDtVencimento())
                .dtCriacao(cobranca.getDtCriacao())
                .tipoTransacao(cobranca.getAgendamento() != null ? "AGENDAMENTO" : "COMPRA")
                .descricaoServicos(cobranca.getAgendamento() != null ?
                        getDescricaoServicos(cobranca.getAgendamento()) : "Compra de produtos")
                .build();
    }

    private AgendamentoDTO convertAgendamentoToDTO(Agendamento agendamento) {
        // Opção 1: Usar o construtor que já existe (RECOMENDADO)
        return new AgendamentoDTO(agendamento);
    }

    private String determinarClassificacao(BigDecimal valorGasto) {
        if (valorGasto == null) return "REGULAR";

        if (valorGasto.compareTo(new BigDecimal("5000")) >= 0) {
            return "VIP";
        } else if (valorGasto.compareTo(new BigDecimal("2000")) >= 0) {
            return "PREMIUM";
        } else {
            return "REGULAR";
        }
    }

    // =============== MÉTODOS DE ESPECIFICAÇÃO ===============

    private Specification<Cliente> createClienteSpecification(ClienteFiltroDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getNome() != null && !filtro.getNome().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("nomeCompleto")),
                        "%" + filtro.getNome().toLowerCase() + "%"));
            }
            if (filtro.getEmail() != null && !filtro.getEmail().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + filtro.getEmail().toLowerCase() + "%"));
            }
            if (filtro.getTelefone() != null && !filtro.getTelefone().trim().isEmpty()) {
                predicates.add(cb.like(root.get("telefone"),
                        "%" + filtro.getTelefone() + "%"));
            }
            if (filtro.getAtivo() != null) {
                predicates.add(cb.equal(root.get("ativo"), filtro.getAtivo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
