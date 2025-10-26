package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, OrganizacaoRepository organizacaoRepository, PasswordEncoder passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Funcionario postNewFuncionario(FuncionarioCreateDTO dto) {
        // --- VALIDAÇÕES ESSENCIAIS ---
        if (dto.getIdOrganizacao() == null) {
            throw new IllegalArgumentException("O ID da organização é obrigatório.");
        }
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("O login é obrigatório.");
        }
        if (dto.getNomeCompleto() == null || dto.getNomeCompleto().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome completo é obrigatório.");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
        if (dto.getCargo() == null || dto.getCargo().trim().isEmpty()){
            throw new IllegalArgumentException("O cargo é obrigatório.");
        }
        if (dto.getNivel() == null ){
            throw new IllegalArgumentException("O nível é obrigatório.");
        }
        if (dto.getRole() == null || dto.getRole().trim().isEmpty()){
            throw new IllegalArgumentException("O perfil de acesso é obrigatório.");
        }

        // Validar que o ID da organização no DTO corresponde ao contexto
        Long organizacaoId = getOrganizacaoIdFromContext();
        if (!organizacaoId.equals(dto.getIdOrganizacao())) {
            throw new SecurityException("Acesso negado: Você não tem permissão para criar funcionário nesta organização");
        }

        // --- VERIFICAÇÃO DE UNICIDADE ---
        funcionarioRepository.findByUsername(dto.getUsername()).ifPresent(f -> {
            throw new IllegalArgumentException("O login '" + dto.getUsername() + "' já está em uso.");
        });

        // --- BUSCA DA ORGANIZAÇÃO ---
        Organizacao org = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização com ID " + organizacaoId + " não encontrada."));

        // --- CRIAÇÃO E MAPEAMENTO DA ENTIDADE ---
        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setOrganizacao(org);
        novoFuncionario.setUsername(dto.getUsername());
        novoFuncionario.setNomeCompleto(dto.getNomeCompleto());
        novoFuncionario.setEmail(dto.getEmail());
        novoFuncionario.setPassword(passwordEncoder.encode(dto.getPassword()));
        novoFuncionario.setCargo(dto.getCargo());
        novoFuncionario.setTelefone(dto.getTelefone());
        novoFuncionario.setNivel(dto.getNivel());
        novoFuncionario.setVisivelExterno(dto.isVisibleExterno());
        novoFuncionario.setAtivo(true);
        novoFuncionario.setRole(dto.getRole());
        novoFuncionario.setDataContratacao(LocalDateTime.now());
        novoFuncionario.setDataCriacao(LocalDateTime.now());

        return funcionarioRepository.save(novoFuncionario);
    }

    @Transactional
    public Funcionario updateFuncionario(Long id, FuncionarioUpdateDTO dto) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado."));

        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        // Atualiza apenas os campos fornecidos
        if (dto.getNomeCompleto() != null && !dto.getNomeCompleto().trim().isEmpty()) {
            funcionario.setNomeCompleto(dto.getNomeCompleto());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            funcionario.setEmail(dto.getEmail());
        }
        if (dto.getTelefone() != null) {
            funcionario.setTelefone(dto.getTelefone());
        }
        if (dto.getDataNasc() != null) {
            funcionario.setDataNasc(dto.getDataNasc());
        }
        if (dto.getCargo() != null && !dto.getCargo().trim().isEmpty()) {
            funcionario.setCargo(dto.getCargo());
        }
        if (dto.getSalario() != null) {
            funcionario.setSalario(dto.getSalario());
        }

        funcionario.setComissao(dto.isComissao());
        funcionario.setDataUpdate(LocalDateTime.now());

        return funcionarioRepository.save(funcionario);
    }

    @Transactional
    public void deleteFuncionario(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado para exclusão."));

        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
    }

    public FuncionarioDTO getFuncionarioById(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado."));

        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        List<JornadaTrabalhoDTO> jornadaDTO = funcionario.getJornadaDeTrabalho().stream()
                .map(jornada -> new JornadaTrabalhoDTO(
                        jornada.getDiaSemana().name(),
                        jornada.getHoraInicio(),
                        jornada.getHoraFim(),
                        jornada.getAtivo()
                ))
                .collect(Collectors.toList());

        List<BloqueioAgendaDTO> bloqueiosDTO = funcionario.getBloqueiosAgenda().stream()
                .map(bloqueio -> new BloqueioAgendaDTO(
                        bloqueio.getInicioBloqueio(),
                        bloqueio.getFimBloqueio(),
                        bloqueio.getDescricao(),
                        bloqueio.getTipoBloqueio().name()
                ))
                .collect(Collectors.toList());

        return new FuncionarioDTO(funcionario, bloqueiosDTO, jornadaDTO);
    }

    public List<FuncionarioDTO> getListAllFuncionarios() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        List<Funcionario> funcionarios = funcionarioRepository.findAllByOrganizacao_Id(organizacaoId);

        return funcionarios.stream()
                .map(funcionario -> {
                    List<JornadaTrabalhoDTO> jornadaDTO = funcionario.getJornadaDeTrabalho().stream()
                            .map(jornada -> new JornadaTrabalhoDTO(
                                    jornada.getDiaSemana().name(),
                                    jornada.getHoraInicio(),
                                    jornada.getHoraFim(),
                                    jornada.getAtivo()
                            ))
                            .collect(Collectors.toList());

                    List<BloqueioAgendaDTO> bloqueiosDTO = funcionario.getBloqueiosAgenda().stream()
                            .map(bloqueio -> new BloqueioAgendaDTO(
                                    bloqueio.getInicioBloqueio(),
                                    bloqueio.getFimBloqueio(),
                                    bloqueio.getDescricao(),
                                    bloqueio.getTipoBloqueio().name()
                            ))
                            .collect(Collectors.toList());

                    return new FuncionarioDTO(funcionario, bloqueiosDTO, jornadaDTO);
                })
                .collect(Collectors.toList());
    }

    public List<FuncionarioAgendamento> getListAllFuncionariosAgendamento() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return funcionarioRepository.findAllProjectedByOrganizacao_Id(organizacaoId);
    }

    public boolean existeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username não pode ser nulo ou vazio.");
        }

        Long organizacaoId = getOrganizacaoIdFromContext();

        return funcionarioRepository.findByUsernameAndOrganizacao_Id(username.trim(), organizacaoId).isPresent();
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
