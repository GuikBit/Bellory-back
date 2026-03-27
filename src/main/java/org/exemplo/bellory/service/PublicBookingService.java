package org.exemplo.bellory.service;

import jakarta.persistence.EntityManager;
import org.exemplo.bellory.model.dto.booking.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.config.ConfigAgendamento;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.organizacao.HorarioFuncionamento;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.organizacao.PeriodoFuncionamento;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.BloqueioAgendaRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.funcionario.JornadaDiaRepository;
import org.exemplo.bellory.model.repository.organizacao.HorarioFuncionamentoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PublicBookingService {

    private static final int SLOT_INTERVAL_MINUTES = 30;

    private final OrganizacaoRepository organizacaoRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final JornadaDiaRepository jornadaDiaRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private final EntityManager entityManager;

    public PublicBookingService(OrganizacaoRepository organizacaoRepository,
                                ClienteRepository clienteRepository,
                                FuncionarioRepository funcionarioRepository,
                                ServicoRepository servicoRepository,
                                AgendamentoRepository agendamentoRepository,
                                JornadaDiaRepository jornadaDiaRepository,
                                BloqueioAgendaRepository bloqueioAgendaRepository,
                                HorarioFuncionamentoRepository horarioFuncionamentoRepository,
                                EntityManager entityManager) {
        this.organizacaoRepository = organizacaoRepository;
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.jornadaDiaRepository = jornadaDiaRepository;
        this.bloqueioAgendaRepository = bloqueioAgendaRepository;
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
        this.entityManager = entityManager;
    }

    // ==================== 1. BUSCAR CLIENTE ====================

    public Optional<ClientePublicDTO> buscarClientePorTelefone(String slug, String telefone) {
        Organizacao org = findOrganizacaoBySlug(slug);
        if (org == null) return null; // slug não encontrado

        String digits = sanitizeTelefone(telefone);
        if (!isValidTelefone(digits)) {
            throw new IllegalArgumentException("Telefone inválido. Deve conter entre 10 e 11 dígitos.");
        }

        Optional<Cliente> cliente = clienteRepository.findByTelefoneAndOrganizacao_Id(digits, org.getId());

        if (cliente.isEmpty() || !cliente.get().isAtivo()) {
            return Optional.empty();
        }

        Cliente c = cliente.get();
        return Optional.of(ClientePublicDTO.builder()
                .id(c.getId())
                .nome(c.getNomeCompleto())
                .telefone(c.getTelefone())
                .email(c.getEmail())
                .build());
    }

    // ==================== 2. CRIAR CLIENTE ====================

    @Transactional
    public ClientePublicDTO criarCliente(String slug, ClienteCreatePublicDTO dto) {
        Organizacao org = findOrganizacaoBySlug(slug);
        if (org == null) {
            throw new IllegalArgumentException("Organização não encontrada.");
        }

        // Validar nome
        String nome = dto.getNome() != null ? dto.getNome().trim() : "";
        if (nome.length() < 3 || nome.length() > 100) {
            throw new IllegalArgumentException("Nome deve ter entre 3 e 100 caracteres.");
        }
        if (!nome.matches("^[a-zA-ZÀ-ÿ\\s]+$")) {
            throw new IllegalArgumentException("Nome deve conter apenas letras e espaços.");
        }

        // Validar telefone
        String digits = sanitizeTelefone(dto.getTelefone());
        if (!isValidTelefone(digits)) {
            throw new IllegalArgumentException("Telefone inválido. Deve conter entre 10 e 11 dígitos.");
        }

        // Validar email (opcional)
        String email = dto.getEmail();
        if (email != null && !email.isBlank()) {
            email = email.trim();
            if (!email.matches("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                throw new IllegalArgumentException("Email inválido.");
            }
        } else {
            email = null;
        }

        // Verificar duplicata
        Optional<Cliente> existente = clienteRepository.findByTelefoneAndOrganizacao_Id(digits, org.getId());
        if (existente.isPresent()) {
            Cliente c = existente.get();
            if (c.isAtivo()) {
                throw new ClienteJaCadastradoException("Cliente já cadastrado com este telefone");
            }
            // Reativar cliente inativo
            c.setAtivo(true);
            c.setNomeCompleto(nome);
            c.setTelefone(digits);
            if (email != null) c.setEmail(email);
            clienteRepository.save(c);
            return toClientePublicDTO(c);
        }

        // Criar novo cliente
        Cliente novo = new Cliente();
        novo.setOrganizacao(org);
        novo.setNomeCompleto(nome);
        novo.setTelefone(digits);
        novo.setEmail(email);
        novo.setAtivo(true);
        novo.setRole("ROLE_CLIENTE");
        novo.setUsername(digits); // usar telefone como username temporário
        novo.setPassword(""); // sem credenciais de acesso
        novo.setIsCadastroIncompleto(true);
        novo.setDtCriacao(LocalDateTime.now());
        clienteRepository.save(novo);

        return toClientePublicDTO(novo);
    }

    // ==================== 3. HORÁRIOS DISPONÍVEIS ====================

    @Transactional(readOnly = true)
    public List<String> buscarHorariosDisponiveis(String slug, Long funcionarioId, LocalDate data, List<Long> servicoIds) {
        Organizacao org = findOrganizacaoBySlug(slug);
        if (org == null) {
            throw new IllegalArgumentException("Organização não encontrada.");
        }

        ConfigAgendamento config = getConfigAgendamento(org);
        int maxAdvanceDays = config.getMaxDiasAgendamento() != null ? config.getMaxDiasAgendamento() : 90;
        int minAdvanceMinutes = config.getToleranciaAgendamento() != null ? config.getToleranciaAgendamento() : 15;

        // Validar data
        LocalDate hoje = LocalDate.now();
        if (data.isBefore(hoje)) {
            throw new IllegalArgumentException("Data não pode ser no passado.");
        }
        if (data.isAfter(hoje.plusDays(maxAdvanceDays))) {
            throw new IllegalArgumentException("Data excede o limite máximo de " + maxAdvanceDays + " dias.");
        }

        // Validar funcionário
        Funcionario funcionario = validarFuncionario(funcionarioId, org.getId());

        // Validar serviços
        List<Servico> servicos = validarServicos(servicoIds, org.getId());

        // Calcular duração total
        int duracaoTotal = servicos.stream()
                .mapToInt(Servico::getTempoEstimadoMinutos)
                .sum();

        // Verificar horário de funcionamento da organização
        DiaSemana diaSemana = DiaSemana.fromDayOfWeek(data.getDayOfWeek());
        Optional<HorarioFuncionamento> horarioOrg = horarioFuncionamentoRepository
                .findByOrganizacaoIdAndDiaSemana(org.getId(), diaSemana);

        if (horarioOrg.isEmpty() || !Boolean.TRUE.equals(horarioOrg.get().getAtivo())) {
            return Collections.emptyList(); // organização fechada neste dia
        }

        // Buscar jornada do funcionário para o dia
        Optional<JornadaDia> jornadaOpt = jornadaDiaRepository.findByFuncionarioAndDiaSemana(funcionario, diaSemana);
        if (jornadaOpt.isEmpty() || !Boolean.TRUE.equals(jornadaOpt.get().getAtivo())) {
            return Collections.emptyList(); // funcionário não trabalha neste dia
        }

        JornadaDia jornada = jornadaOpt.get();
        List<HorarioTrabalho> horarios = jornada.getHorarios();
        if (horarios == null || horarios.isEmpty()) {
            return Collections.emptyList();
        }

        // Gerar slots a partir dos períodos de trabalho do funcionário
        List<LocalTime> slots = new ArrayList<>();
        for (HorarioTrabalho ht : horarios) {
            LocalTime slotInicio = ht.getHoraInicio();
            while (slotInicio.plusMinutes(duracaoTotal).compareTo(ht.getHoraFim()) <= 0) {
                slots.add(slotInicio);
                slotInicio = slotInicio.plusMinutes(SLOT_INTERVAL_MINUTES);
            }
        }

        // Limitar slots ao horário de funcionamento da organização
        List<PeriodoFuncionamento> periodosOrg = horarioOrg.get().getPeriodos();
        if (periodosOrg != null && !periodosOrg.isEmpty()) {
            slots = slots.stream().filter(slot -> {
                LocalTime slotFim = slot.plusMinutes(duracaoTotal);
                return periodosOrg.stream().anyMatch(p ->
                        !slot.isBefore(p.getHoraInicio()) && !slotFim.isAfter(p.getHoraFim()));
            }).collect(Collectors.toList());
        }

        // Remover slots com bloqueios
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(23, 59, 59);
        List<BloqueioAgenda> bloqueios = bloqueioAgendaRepository.findBloqueiosSobrepostos(
                funcionarioId, inicioDia, fimDia);

        slots = slots.stream().filter(slot -> {
            LocalDateTime slotInicioDt = data.atTime(slot);
            LocalDateTime slotFimDt = slotInicioDt.plusMinutes(duracaoTotal);
            return bloqueios.stream().noneMatch(b ->
                    slotInicioDt.isBefore(b.getFimBloqueio()) && slotFimDt.isAfter(b.getInicioBloqueio()));
        }).collect(Collectors.toList());

        // Remover slots ocupados por agendamentos existentes
        List<Agendamento> agendamentos = agendamentoRepository
                .findAtivosByFuncionarioAndOrganizacaoAndPeriodo(funcionarioId, org.getId(), inicioDia, fimDia);

        slots = slots.stream().filter(slot -> {
            LocalDateTime slotInicioDt = data.atTime(slot);
            LocalDateTime slotFimDt = slotInicioDt.plusMinutes(duracaoTotal);
            return agendamentos.stream().noneMatch(ag -> {
                LocalDateTime agInicio = ag.getDtAgendamento();
                int agDuracao = ag.getServicos() != null
                        ? ag.getServicos().stream().mapToInt(Servico::getTempoEstimadoMinutos).sum()
                        : 30;
                LocalDateTime agFim = agInicio.plusMinutes(agDuracao);
                return slotInicioDt.isBefore(agFim) && slotFimDt.isAfter(agInicio);
            });
        }).collect(Collectors.toList());

        // Respeitar antecedência mínima (se data = hoje)
        if (data.equals(hoje)) {
            LocalTime limiteMinimo = LocalTime.now().plusMinutes(minAdvanceMinutes);
            slots = slots.stream()
                    .filter(slot -> !slot.isBefore(limiteMinimo))
                    .collect(Collectors.toList());
        }

        // Formatar como HH:mm
        return slots.stream()
                .sorted()
                .map(slot -> String.format("%02d:%02d", slot.getHour(), slot.getMinute()))
                .collect(Collectors.toList());
    }

    // ==================== 4. DIAS DISPONÍVEIS ====================

    @Transactional(readOnly = true)
    public List<DiaDisponivelDTO> buscarDiasDisponiveis(String slug, Long funcionarioId, YearMonth mes) {
        Organizacao org = findOrganizacaoBySlug(slug);
        if (org == null) {
            throw new IllegalArgumentException("Organização não encontrada.");
        }

        ConfigAgendamento config = getConfigAgendamento(org);
        int maxAdvanceDays = config.getMaxDiasAgendamento() != null ? config.getMaxDiasAgendamento() : 90;

        // Validar mês
        YearMonth mesAtual = YearMonth.now();
        if (mes.isBefore(mesAtual)) {
            throw new IllegalArgumentException("Mês não pode ser no passado.");
        }

        // Validar funcionário
        Funcionario funcionario = validarFuncionario(funcionarioId, org.getId());

        // Buscar jornadas ativas do funcionário
        List<JornadaDia> jornadas = jornadaDiaRepository.findByFuncionarioId(funcionarioId);
        Set<DiaSemana> diasAtivos = jornadas.stream()
                .filter(j -> Boolean.TRUE.equals(j.getAtivo()))
                .map(JornadaDia::getDiaSemana)
                .collect(Collectors.toSet());

        // Buscar horários de funcionamento da organização
        List<HorarioFuncionamento> horariosOrg = horarioFuncionamentoRepository
                .findByOrganizacaoIdWithPeriodos(org.getId());
        Set<DiaSemana> diasOrgAbertos = horariosOrg.stream()
                .filter(h -> Boolean.TRUE.equals(h.getAtivo()))
                .map(HorarioFuncionamento::getDiaSemana)
                .collect(Collectors.toSet());

        LocalDate hoje = LocalDate.now();
        LocalDate limiteMax = hoje.plusDays(maxAdvanceDays);
        LocalDate primeiroDia = mes.atDay(1);
        LocalDate ultimoDia = mes.atEndOfMonth();

        List<DiaDisponivelDTO> resultado = new ArrayList<>();

        for (LocalDate dia = primeiroDia; !dia.isAfter(ultimoDia); dia = dia.plusDays(1)) {
            // Excluir dias passados
            if (dia.isBefore(hoje)) {
                resultado.add(DiaDisponivelDTO.builder().data(dia).disponivel(false).build());
                continue;
            }

            // Excluir dias além do limite máximo
            if (dia.isAfter(limiteMax)) {
                resultado.add(DiaDisponivelDTO.builder().data(dia).disponivel(false).build());
                continue;
            }

            DiaSemana diaSemana = DiaSemana.fromDayOfWeek(dia.getDayOfWeek());

            // Verificar se organização está aberta
            if (!diasOrgAbertos.contains(diaSemana)) {
                resultado.add(DiaDisponivelDTO.builder().data(dia).disponivel(false).build());
                continue;
            }

            // Verificar se funcionário trabalha neste dia
            if (!diasAtivos.contains(diaSemana)) {
                resultado.add(DiaDisponivelDTO.builder().data(dia).disponivel(false).build());
                continue;
            }

            // Verificar bloqueio integral (férias/folga)
            LocalDateTime inicioDia = dia.atStartOfDay();
            LocalDateTime fimDia = dia.atTime(23, 59, 59);
            boolean bloqueioIntegral = bloqueioAgendaRepository.existsBloqueioIntegral(
                    funcionarioId, inicioDia, fimDia);

            resultado.add(DiaDisponivelDTO.builder()
                    .data(dia)
                    .disponivel(!bloqueioIntegral)
                    .build());
        }

        return resultado;
    }

    // ==================== 5. CRIAR AGENDAMENTO ====================

    @Transactional
    public BookingResponseDTO criarAgendamento(String slug, BookingCreateDTO dto) {
        Organizacao org = findOrganizacaoBySlug(slug);
        if (org == null) {
            throw new IllegalArgumentException("Organização não encontrada.");
        }

        ConfigAgendamento config = getConfigAgendamento(org);

        // Validar cliente
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .filter(c -> c.isAtivo() && c.getOrganizacao().getId().equals(org.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado ou inativo."));

        // Validar serviços
        if (dto.getServicoIds() == null || dto.getServicoIds().isEmpty()) {
            throw new IllegalArgumentException("Pelo menos um serviço deve ser selecionado.");
        }
        List<Servico> servicos = validarServicos(dto.getServicoIds(), org.getId());

        // Validar funcionário
        Funcionario funcionario = validarFuncionario(dto.getFuncionarioId(), org.getId());

        // Verificar se funcionário atende os serviços
        Set<Long> servicosFuncionario = funcionario.getServicos().stream()
                .map(Servico::getId)
                .collect(Collectors.toSet());
        for (Servico s : servicos) {
            if (!servicosFuncionario.contains(s.getId())) {
                throw new IllegalArgumentException(
                        "O profissional não atende o serviço: " + s.getNome());
            }
        }

        // Validar data
        LocalDate data = dto.getData();
        LocalDate hoje = LocalDate.now();
        int maxAdvanceDays = config.getMaxDiasAgendamento() != null ? config.getMaxDiasAgendamento() : 90;
        if (data.isBefore(hoje)) {
            throw new IllegalArgumentException("Data não pode ser no passado.");
        }
        if (data.isAfter(hoje.plusDays(maxAdvanceDays))) {
            throw new IllegalArgumentException("Data excede o limite máximo.");
        }

        // Validar horário
        LocalTime horario = dto.getHorario();
        if (horario == null) {
            throw new IllegalArgumentException("Horário é obrigatório.");
        }

        // Re-validar disponibilidade com lock pessimista para evitar race conditions
        LocalDateTime dtAgendamento = LocalDateTime.of(data, horario);
        int duracaoTotal = servicos.stream().mapToInt(Servico::getTempoEstimadoMinutos).sum();

        // Buscar agendamentos ativos do funcionário no dia com lock
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(23, 59, 59);

        List<Agendamento> agendamentos = agendamentoRepository
                .findAtivosByFuncionarioAndOrganizacaoAndPeriodo(
                        dto.getFuncionarioId(), org.getId(), inicioDia, fimDia);

        // Verificar conflito
        LocalDateTime slotFim = dtAgendamento.plusMinutes(duracaoTotal);
        boolean conflito = agendamentos.stream().anyMatch(ag -> {
            LocalDateTime agInicio = ag.getDtAgendamento();
            int agDuracao = ag.getServicos() != null
                    ? ag.getServicos().stream().mapToInt(Servico::getTempoEstimadoMinutos).sum()
                    : 30;
            LocalDateTime agFim = agInicio.plusMinutes(agDuracao);
            return dtAgendamento.isBefore(agFim) && slotFim.isAfter(agInicio);
        });

        if (conflito) {
            throw new HorarioIndisponivelException("Horário não está mais disponível");
        }

        // Verificar bloqueios
        boolean temBloqueio = bloqueioAgendaRepository.existsBloqueioSobreposto(
                dto.getFuncionarioId(), dtAgendamento, slotFim);
        if (temBloqueio) {
            throw new HorarioIndisponivelException("Horário não está mais disponível");
        }

        // Calcular valor total
        BigDecimal valorTotal = servicos.stream()
                .map(s -> s.getPrecoFinal() != null && s.getPrecoFinal().compareTo(BigDecimal.ZERO) > 0
                        ? s.getPrecoFinal()
                        : s.getPreco())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Criar agendamento
        Agendamento agendamento = new Agendamento();
        agendamento.setOrganizacao(org);
        agendamento.setCliente(cliente);
        agendamento.setServicos(servicos);
        agendamento.setFuncionarios(List.of(funcionario));
        agendamento.setDtAgendamento(dtAgendamento);
        agendamento.setObservacao(dto.getObservacao() != null ? dto.getObservacao().trim() : "");
        agendamento.setStatus(Status.PENDENTE);
        agendamento.setDtCriacao(LocalDateTime.now());

        // Configuração de sinal/depósito
        boolean cobrarSinal = config.getCobrarSinal() != null && config.getCobrarSinal();
        agendamento.setRequerSinal(cobrarSinal);
        if (cobrarSinal && config.getPorcentSinal() != null) {
            agendamento.setPercentualSinal(BigDecimal.valueOf(config.getPorcentSinal()));
        }

        agendamentoRepository.save(agendamento);

        // Criar bloqueio de agenda para o funcionário
        BloqueioAgenda bloqueio = new BloqueioAgenda(
                funcionario,
                dtAgendamento,
                slotFim,
                "Agendamento #" + agendamento.getId(),
                TipoBloqueio.AGENDAMENTO,
                agendamento
        );
        bloqueioAgendaRepository.save(bloqueio);

        return BookingResponseDTO.builder()
                .id(agendamento.getId())
                .status(agendamento.getStatus().name())
                .dtAgendamento(dtAgendamento)
                .valorTotal(valorTotal)
                .profissional(funcionario.getNomeCompleto())
                .servicos(servicos.stream().map(Servico::getNome).collect(Collectors.toList()))
                .requerSinal(cobrarSinal)
                .percentualSinal(cobrarSinal && config.getPorcentSinal() != null
                        ? BigDecimal.valueOf(config.getPorcentSinal()) : null)
                .build();
    }

    // ==================== HELPERS ====================

    public Organizacao findOrganizacaoBySlug(String slug) {
        return organizacaoRepository.findBySlugAndAtivoTrue(slug).orElse(null);
    }

    private Funcionario validarFuncionario(Long funcionarioId, Long orgId) {
        Funcionario func = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado."));

        if (!func.isAtivo() || func.isDeletado()) {
            throw new IllegalArgumentException("Profissional não está disponível.");
        }
        if (!func.getOrganizacao().getId().equals(orgId)) {
            throw new IllegalArgumentException("Profissional não pertence a esta organização.");
        }
        if (!func.isVisivelExterno()) {
            throw new IllegalArgumentException("Profissional não está disponível para agendamento externo.");
        }
        return func;
    }

    private List<Servico> validarServicos(List<Long> servicoIds, Long orgId) {
        List<Servico> servicos = servicoRepository.findAllById(servicoIds);
        if (servicos.size() != servicoIds.size()) {
            throw new IllegalArgumentException("Um ou mais serviços não foram encontrados.");
        }
        for (Servico s : servicos) {
            if (!s.isAtivo() || s.isDeletado()) {
                throw new IllegalArgumentException("Serviço '" + s.getNome() + "' não está disponível.");
            }
            if (!s.getOrganizacao().getId().equals(orgId)) {
                throw new IllegalArgumentException("Serviço '" + s.getNome() + "' não pertence a esta organização.");
            }
        }
        return servicos;
    }

    private ConfigAgendamento getConfigAgendamento(Organizacao org) {
        ConfigSistema configSistema = org.getConfigSistema();
        if (configSistema != null && configSistema.getConfigAgendamento() != null) {
            return configSistema.getConfigAgendamento();
        }
        return new ConfigAgendamento(); // defaults
    }

    private String sanitizeTelefone(String telefone) {
        if (telefone == null) return "";
        return telefone.replaceAll("[^0-9]", "");
    }

    private boolean isValidTelefone(String digits) {
        return digits.length() >= 10 && digits.length() <= 11;
    }

    private ClientePublicDTO toClientePublicDTO(Cliente c) {
        return ClientePublicDTO.builder()
                .id(c.getId())
                .nome(c.getNomeCompleto())
                .telefone(c.getTelefone())
                .email(c.getEmail())
                .build();
    }

    // ==================== EXCEÇÕES CUSTOMIZADAS ====================

    public static class ClienteJaCadastradoException extends RuntimeException {
        public ClienteJaCadastradoException(String message) {
            super(message);
        }
    }

    public static class HorarioIndisponivelException extends RuntimeException {
        public HorarioIndisponivelException(String message) {
            super(message);
        }
    }
}
