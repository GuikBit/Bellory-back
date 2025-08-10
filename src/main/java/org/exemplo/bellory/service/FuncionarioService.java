package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.funcionario.JornadaTrabalho;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors; // Importar

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
    public Funcionario postNewFuncionario(FuncionarioCreateDTO dto) { // <-- MUDANÇA AQUI
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

        // --- VERIFICAÇÃO DE UNICIDADE ---
        funcionarioRepository.findByUsername(dto.getUsername()).ifPresent(f -> {
            throw new IllegalArgumentException("O login '" + dto.getUsername() + "' já está em uso.");
        });

        // --- BUSCA DA ORGANIZAÇÃO ---
        Organizacao org = organizacaoRepository.findById(dto.getIdOrganizacao())
                .orElseThrow(() -> new IllegalArgumentException("Organização com ID " + dto.getIdOrganizacao() + " não encontrada."));

        // --- CRIAÇÃO E MAPEAMENTO DA ENTIDADE ---
        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setOrganizacao(org);
        novoFuncionario.setUsername(dto.getUsername());
        novoFuncionario.setNomeCompleto(dto.getNomeCompleto());
        novoFuncionario.setEmail(dto.getEmail());
        novoFuncionario.setPassword(passwordEncoder.encode(dto.getPassword())); // Criptografa a senha
        novoFuncionario.setCargo(dto.getCargo());
        novoFuncionario.setNivel(dto.getNivel());
        novoFuncionario.setAtivo(true); // Define como ativo por padrão
        novoFuncionario.setRole("ROLE_FUNCIONARIO"); // Define uma role padrão
        novoFuncionario.setDataContratacao(LocalDateTime.now());
        novoFuncionario.setDataCriacao(LocalDateTime.now());

        return funcionarioRepository.save(novoFuncionario);
    }

    // O método agora retorna uma lista de DTOs
    public List<FuncionarioDTO> getListAllFuncionarios() {
        List<Funcionario> funcionarios = this.funcionarioRepository.findAll();

        // Lógica de mapeamento completa
        return funcionarios.stream()
                .map(funcionario -> {
                    // Converte o Set<JornadaTrabalho> em List<JornadaTrabalhoDTO>
                    List<JornadaTrabalhoDTO> jornadaDTO = funcionario.getJornadaDeTrabalho().stream()
                            .map(jornada -> new JornadaTrabalhoDTO(
                                    jornada.getDiaSemana().name(),
                                    jornada.getHoraInicio(),
                                    jornada.getHoraFim(),
                                    jornada.getAtivo()
                            ))
                            .collect(Collectors.toList());

                    // Converte o Set<BloqueioAgenda> em List<BloqueioAgendaDTO>
                    List<BloqueioAgendaDTO> bloqueiosDTO = funcionario.getBloqueiosAgenda().stream()
                            .map(bloqueio -> new BloqueioAgendaDTO(
                                    bloqueio.getInicioBloqueio(),
                                    bloqueio.getFimBloqueio(),
                                    bloqueio.getDescricao(),
                                    bloqueio.getTipoBloqueio().name()
                            ))
                            .collect(Collectors.toList());

                    // Cria o DTO final do funcionário com as listas de DTOs
                    return new FuncionarioDTO( funcionario, bloqueiosDTO, jornadaDTO
                    );
                })
                .collect(Collectors.toList());
    }

    public List<FuncionarioAgendamento> getListAllFuncionariosAgendamento() {
        return this.funcionarioRepository.findAllProjectedBy();
    }

}
